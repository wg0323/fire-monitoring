package com.fm.module.device.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.PageReadListener;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fm.common.exception.BizException;
import com.fm.module.device.converter.DeviceConverter;
import com.fm.module.device.converter.DeviceImportRowConverter;
import com.fm.module.device.dto.DeviceImportRow;
import com.fm.module.device.dto.DeviceQueryDTO;
import com.fm.module.device.dto.DeviceSaveDTO;
import com.fm.module.device.entity.Device;
import com.fm.module.device.entity.DictDeviceMaker;
import com.fm.module.device.entity.DictDeviceType;
import com.fm.module.device.entity.Merchant;
import com.fm.module.device.entity.MerchantBuilding;
import com.fm.module.device.entity.MerchantFloor;
import com.fm.module.device.mapper.DictDeviceMakerMapper;
import com.fm.module.device.mapper.DictDeviceTypeMapper;
import com.fm.module.device.mapper.MerchantBuildingMapper;
import com.fm.module.device.mapper.MerchantFloorMapper;
import com.fm.module.device.mapper.MerchantMapper;
import com.fm.module.device.service.DeviceExcelService;
import com.fm.module.device.service.DeviceService;
import com.fm.module.device.vo.DeviceImportResultVO;
import com.fm.module.device.vo.DeviceVO;
import com.fm.module.system.service.DataScopeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * EasyExcel 导入导出：逐行导入，成功行入库，失败行返回原因。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceExcelServiceImpl implements DeviceExcelService {

    private static final int MAX_IMPORT_ROWS = 2000;
    private static final int MAX_EXPORT_ROWS = 10000;
    private static final int MAX_ERROR_ITEMS = 100;
    private static final int STATUS_ENABLED = 1;
    private static final int STATUS_DISABLED = 0;
    private static final String LOCATION_NONE = "未设置";
    private static final String LOCATION_GPS = "室外GPS";
    private static final String LOCATION_FLOOR = "室内楼层图";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-M-d");
    private static final DateTimeFormatter DATE_SLASH_FORMAT = DateTimeFormatter.ofPattern("yyyy/M/d");
    private static final DateTimeFormatter FILE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final DeviceService deviceService;
    private final DictDeviceTypeMapper deviceTypeMapper;
    private final DictDeviceMakerMapper deviceMakerMapper;
    private final MerchantMapper merchantMapper;
    private final MerchantBuildingMapper buildingMapper;
    private final MerchantFloorMapper floorMapper;
    private final DataScopeService dataScopeService;

    @Override
    public void exportTemplate(HttpServletResponse response) throws IOException {
        writeWorkbook(response, Collections.emptyList(), "设备导入模板.xlsx");
    }

    /**
     * 逐行容错导入：每行独立提交，单行失败仅记录原因并继续处理后续行。
     * 因此这里刻意不加事务注解，一旦加类级/方法级事务，失败行会连同已成功的行一起回滚，
     * 与「成功行入库、失败行返回原因」的既定语义冲突。
     */
    @Override
    public DeviceImportResultVO importDevices(MultipartFile file) {
        List<DeviceImportRow> rows = readRows(file);
        ImportCatalog catalog = loadCatalog();
        DeviceImportResultVO result = new DeviceImportResultVO();
        for (int i = 0; i < rows.size(); i++) {
            DeviceImportRow row = rows.get(i);
            if (isBlankRow(row)) {
                continue;
            }
            result.setTotal(result.getTotal() + 1);
            int rowNum = i + 2;
            try {
                deviceService.create(toSaveDTO(row, catalog));
                result.setSuccess(result.getSuccess() + 1);
            } catch (Exception ex) {
                result.setFailed(result.getFailed() + 1);
                if (result.getErrors().size() < MAX_ERROR_ITEMS) {
                    result.getErrors().add(DeviceConverter.toImportErrorVO(rowNum, trim(row.getSn()), errorMessage(ex)));
                }
                log.warn("设备导入第 {} 行失败，sn={}，原因={}",
                        rowNum, trim(row.getSn()), errorMessage(ex), ex);
            }
        }
        return result;
    }

    @Override
    public void export(DeviceQueryDTO query, HttpServletResponse response) throws IOException {
        if (dataScopeService.isPlatform()) {
            throw new BizException(403, "平台管理员不支持导出设备");
        }
        List<DeviceVO> devices = deviceService.listForExport(query);
        if (devices.size() > MAX_EXPORT_ROWS) {
            throw new BizException("导出数据超过 " + MAX_EXPORT_ROWS + " 条，请缩小筛选范围");
        }
        List<DeviceImportRow> rows = new ArrayList<>();
        for (DeviceVO device : devices) {
            rows.add(DeviceImportRowConverter.toImportRow(device));
        }
        String fileName = "设备列表_" + LocalDateTime.now().format(FILE_TIME_FORMAT) + ".xlsx";
        writeWorkbook(response, rows, fileName);
    }

    private List<DeviceImportRow> readRows(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择导入文件");
        }
        String fileName = file.getOriginalFilename();
        if (fileName != null) {
            String lower = fileName.toLowerCase();
            if (!lower.endsWith(".xlsx") && !lower.endsWith(".xls")) {
                throw new BizException("仅支持 xlsx 或 xls 文件");
            }
        }
        List<DeviceImportRow> rows = new ArrayList<>();
        try {
            // 分批回调流式读取：达到行数上限立即抛业务异常中断，避免 doReadSync 全量解析后才判断。
            EasyExcel.read(file.getInputStream(), DeviceImportRow.class,
                    new PageReadListener<DeviceImportRow>(batch -> {
                        for (DeviceImportRow row : batch) {
                            if (rows.size() >= MAX_IMPORT_ROWS) {
                                throw new BizException("单次最多导入 " + MAX_IMPORT_ROWS + " 行");
                            }
                            rows.add(row);
                        }
                    })).sheet().doRead();
        } catch (BizException ex) {
            // 行数超限是明确的业务提示，直接透传，不要被包装成通用解析失败。
            throw ex;
        } catch (IOException | RuntimeException ex) {
            log.warn("设备导入文件解析失败，file={}", fileName, ex);
            throw new BizException("Excel 解析失败，请使用导入模板");
        }
        if (rows.isEmpty()) {
            throw new BizException("导入文件没有数据");
        }
        return rows;
    }

    /**
     * 循环前一次性预加载字典/商户/楼宇/楼层并建索引，循环内只查 Map，
     * 避免每行多次 selectOne 造成的 N+1 查询。
     */
    private ImportCatalog loadCatalog() {
        Map<String, String> typeCodes = new HashMap<>();
        for (DictDeviceType type : deviceTypeMapper.selectList(
                new LambdaQueryWrapper<DictDeviceType>().eq(DictDeviceType::getStatus, STATUS_ENABLED))) {
            typeCodes.putIfAbsent(type.getName(), type.getCode());
            typeCodes.putIfAbsent(type.getCode(), type.getCode());
        }
        Map<String, String> makerCodes = new HashMap<>();
        for (DictDeviceMaker maker : deviceMakerMapper.selectList(
                new LambdaQueryWrapper<DictDeviceMaker>().eq(DictDeviceMaker::getStatus, STATUS_ENABLED))) {
            makerCodes.putIfAbsent(maker.getName(), maker.getCode());
            makerCodes.putIfAbsent(maker.getCode(), maker.getCode());
        }
        Map<String, Long> merchantIds = new HashMap<>();
        for (Merchant merchant : merchantMapper.selectList(new LambdaQueryWrapper<Merchant>())) {
            merchantIds.putIfAbsent(merchant.getName(), merchant.getId());
        }
        Map<String, Long> buildingIds = new HashMap<>();
        for (MerchantBuilding building : buildingMapper.selectList(new LambdaQueryWrapper<MerchantBuilding>())) {
            buildingIds.putIfAbsent(catalogKey(building.getMerchantId(), building.getName()), building.getId());
        }
        Map<String, Long> floorIds = new HashMap<>();
        for (MerchantFloor floor : floorMapper.selectList(new LambdaQueryWrapper<MerchantFloor>())) {
            floorIds.putIfAbsent(catalogKey(floor.getBuildingId(), floor.getName()), floor.getId());
        }
        return new ImportCatalog(typeCodes, makerCodes, merchantIds, buildingIds, floorIds);
    }

    private static String catalogKey(Long parentId, String name) {
        return parentId + "|" + name;
    }

    private DeviceSaveDTO toSaveDTO(DeviceImportRow row, ImportCatalog catalog) {
        DeviceSaveDTO dto = new DeviceSaveDTO();
        String sn = trim(row.getSn());
        if (!StringUtils.hasText(sn)) {
            throw new BizException("设备编号不能为空");
        }
        dto.setSn(sn);
        dto.setName(trim(row.getName()));
        dto.setType(resolveType(trim(row.getTypeName()), catalog));
        Long merchantId = resolveMerchant(trim(row.getMerchantName()), catalog);
        dto.setMerchantId(merchantId);
        dto.setMaker(resolveMaker(trim(row.getMakerName()), catalog));
        int locationType = parseLocationType(trim(row.getLocationTypeName()));
        dto.setLocationType(locationType);
        if (locationType == Device.LOCATION_GPS) {
            dto.setLng(parseDouble(row.getLng(), "经度"));
            dto.setLat(parseDouble(row.getLat(), "纬度"));
        } else if (locationType == Device.LOCATION_FLOOR) {
            if (merchantId == null) {
                throw new BizException("楼层定位必须填写所属商户");
            }
            Long buildingId = resolveBuilding(merchantId, trim(row.getBuildingName()), catalog);
            dto.setBuildingId(buildingId);
            dto.setFloorId(resolveFloor(buildingId, trim(row.getFloorName()), catalog));
            dto.setFloorX(parseDecimal(row.getFloorX(), "楼层X坐标"));
            dto.setFloorY(parseDecimal(row.getFloorY(), "楼层Y坐标"));
        }
        dto.setPosition(trim(row.getPosition()));
        dto.setSim(trim(row.getSim()));
        dto.setCardNo(trim(row.getCardNo()));
        Integer channelNum = parseInteger(row.getChannelNum(), "通道数");
        dto.setChannelNum(channelNum == null ? 1 : channelNum);
        dto.setActiveDate(parseDate(row.getActiveDate(), "激活日期"));
        dto.setInstallDate(parseDate(row.getInstallDate(), "安装日期"));
        dto.setStatus(parseStatus(trim(row.getStatusName())));
        return dto;
    }

    private void writeWorkbook(HttpServletResponse response, List<DeviceImportRow> rows, String fileName)
            throws IOException {
        String encoded = URLEncoder.encode(fileName, "UTF-8").replace("+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
        EasyExcel.write(response.getOutputStream(), DeviceImportRow.class)
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .sheet("设备")
                .doWrite(rows);
    }

    private String resolveType(String text, ImportCatalog catalog) {
        if (!StringUtils.hasText(text)) {
            throw new BizException("设备类型不能为空");
        }
        String code = catalog.typeCodes.get(text);
        if (code == null) {
            throw new BizException("设备类型不存在或已禁用：" + text);
        }
        return code;
    }

    private String resolveMaker(String text, ImportCatalog catalog) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String code = catalog.makerCodes.get(text);
        if (code == null) {
            throw new BizException("制造商不存在或已禁用：" + text);
        }
        return code;
    }

    private Long resolveMerchant(String text, ImportCatalog catalog) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        Long id = catalog.merchantIds.get(text);
        if (id == null) {
            throw new BizException("商户不存在：" + text);
        }
        return id;
    }

    private Long resolveBuilding(Long merchantId, String text, ImportCatalog catalog) {
        if (!StringUtils.hasText(text)) {
            throw new BizException("楼层定位必须填写楼宇");
        }
        Long id = catalog.buildingIds.get(catalogKey(merchantId, text));
        if (id == null) {
            throw new BizException("楼宇不存在：" + text);
        }
        return id;
    }

    private Long resolveFloor(Long buildingId, String text, ImportCatalog catalog) {
        if (!StringUtils.hasText(text)) {
            throw new BizException("楼层定位必须填写楼层");
        }
        Long id = catalog.floorIds.get(catalogKey(buildingId, text));
        if (id == null) {
            throw new BizException("楼层不存在：" + text);
        }
        return id;
    }

    private int parseLocationType(String text) {
        if (!StringUtils.hasText(text) || LOCATION_NONE.equals(text)) {
            return Device.LOCATION_NONE;
        }
        if (LOCATION_GPS.equals(text) || "GPS".equalsIgnoreCase(text) || "室外 GPS".equals(text)) {
            return Device.LOCATION_GPS;
        }
        if (LOCATION_FLOOR.equals(text) || "楼层图".equals(text)) {
            return Device.LOCATION_FLOOR;
        }
        throw new BizException("定位方式不合法：" + text);
    }

    private int parseStatus(String text) {
        if (!StringUtils.hasText(text) || "启用".equals(text) || "正常".equals(text)) {
            return STATUS_ENABLED;
        }
        if ("禁用".equals(text)) {
            return STATUS_DISABLED;
        }
        throw new BizException("状态不合法：" + text);
    }

    private BigDecimal parseDecimal(String text, String field) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return new BigDecimal(text.trim());
        } catch (NumberFormatException ex) {
            throw new BizException(field + "必须是数字：" + text);
        }
    }

    private Double parseDouble(String text, String field) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return Double.valueOf(text.trim());
        } catch (NumberFormatException ex) {
            throw new BizException(field + "必须是数字：" + text);
        }
    }

    private Integer parseInteger(String text, String field) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return Integer.valueOf(text.trim());
        } catch (NumberFormatException ex) {
            throw new BizException(field + "必须是整数：" + text);
        }
    }

    private LocalDateTime parseDate(String text, String field) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String value = text.trim();
        String datePart = value.length() > 10 ? value.substring(0, 10) : value;
        try {
            return LocalDate.parse(datePart, DATE_FORMAT).atStartOfDay();
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDate.parse(datePart, DATE_SLASH_FORMAT).atStartOfDay();
            } catch (DateTimeParseException ex) {
                throw new BizException(field + "格式错误，应为 yyyy-MM-dd：" + value);
            }
        }
    }

    private boolean isBlankRow(DeviceImportRow row) {
        return row == null || (!StringUtils.hasText(row.getSn()) && !StringUtils.hasText(row.getName())
                && !StringUtils.hasText(row.getTypeName()) && !StringUtils.hasText(row.getMerchantName()));
    }

    private String errorMessage(Exception ex) {
        return StringUtils.hasText(ex.getMessage()) ? ex.getMessage() : "导入失败";
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    /**
     * 导入所需的名称到 ID/编码索引，循环内只读不查库。
     */
    private static final class ImportCatalog {

        private final Map<String, String> typeCodes;
        private final Map<String, String> makerCodes;
        private final Map<String, Long> merchantIds;
        private final Map<String, Long> buildingIds;
        private final Map<String, Long> floorIds;

        private ImportCatalog(Map<String, String> typeCodes, Map<String, String> makerCodes,
                              Map<String, Long> merchantIds, Map<String, Long> buildingIds,
                              Map<String, Long> floorIds) {
            this.typeCodes = typeCodes;
            this.makerCodes = makerCodes;
            this.merchantIds = merchantIds;
            this.buildingIds = buildingIds;
            this.floorIds = floorIds;
        }
    }
}
