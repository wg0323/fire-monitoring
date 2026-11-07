package com.fm.module.device.service.impl;

import com.alibaba.excel.EasyExcel;
import com.fm.common.exception.BizException;
import com.fm.module.device.dto.DeviceImportRow;
import com.fm.module.device.dto.DeviceQueryDTO;
import com.fm.module.device.dto.DeviceSaveDTO;
import com.fm.module.device.entity.DictDeviceType;
import com.fm.module.device.mapper.DictDeviceMakerMapper;
import com.fm.module.device.mapper.DictDeviceTypeMapper;
import com.fm.module.device.mapper.MerchantBuildingMapper;
import com.fm.module.device.mapper.MerchantFloorMapper;
import com.fm.module.device.mapper.MerchantMapper;
import com.fm.module.device.service.DeviceService;
import com.fm.module.device.vo.DeviceImportResultVO;
import com.fm.module.system.service.DataScopeService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 设备导入：单次请求内成功行入库、失败行返回原因。
 */
class DeviceExcelServiceImplTest {

    private final DeviceService deviceService = mock(DeviceService.class);
    private final DictDeviceTypeMapper deviceTypeMapper = mock(DictDeviceTypeMapper.class);
    private final DataScopeService dataScopeService = mock(DataScopeService.class);
    private final DeviceExcelServiceImpl service = new DeviceExcelServiceImpl(deviceService, deviceTypeMapper,
            mock(DictDeviceMakerMapper.class), mock(MerchantMapper.class),
            mock(MerchantBuildingMapper.class), mock(MerchantFloorMapper.class), dataScopeService);

    @Test
    void platformCannotExport() {
        when(dataScopeService.isPlatform()).thenReturn(true);

        assertThrows(BizException.class,
                () -> service.export(new DeviceQueryDTO(), new MockHttpServletResponse()));
    }

    @Test
    void importReturnsPartialSuccess() {
        DictDeviceType type = new DictDeviceType();
        type.setCode("dh-sa2ga-a");
        type.setName("大华感烟");
        when(deviceTypeMapper.selectList(any())).thenReturn(Arrays.asList(type));
        when(deviceService.create(any(DeviceSaveDTO.class)))
                .thenReturn(1L)
                .thenThrow(new BizException("设备 SN 已存在"));

        DeviceImportResultVO result = service.importDevices(fileWithTwoRows());

        assertEquals(2, result.getTotal());
        assertEquals(1, result.getSuccess());
        assertEquals(1, result.getFailed());
        assertEquals(1, result.getErrors().size());
        assertEquals(3, result.getErrors().get(0).getRowNum());
        assertEquals("SN-TEST-002", result.getErrors().get(0).getSn());
        assertEquals("设备 SN 已存在", result.getErrors().get(0).getMessage());
    }

    private MockMultipartFile fileWithTwoRows() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        EasyExcel.write(output, DeviceImportRow.class).sheet("设备")
                .doWrite(Arrays.asList(row("SN-TEST-001"), row("SN-TEST-002")));
        return new MockMultipartFile("file", "devices.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", output.toByteArray());
    }

    private DeviceImportRow row(String sn) {
        DeviceImportRow row = new DeviceImportRow();
        row.setSn(sn);
        row.setName("导入测试设备");
        row.setTypeName("大华感烟");
        row.setLocationTypeName("未设置");
        row.setStatusName("启用");
        return row;
    }
}
