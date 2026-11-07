package com.fm.module.device.service;

import com.fm.module.device.dto.DeviceQueryDTO;
import com.fm.module.device.vo.DeviceImportResultVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 设备导入导出：模板、单次部分成功导入与按筛选条件导出。
 */
public interface DeviceExcelService {

    void exportTemplate(HttpServletResponse response) throws IOException;

    DeviceImportResultVO importDevices(MultipartFile file);

    void export(DeviceQueryDTO query, HttpServletResponse response) throws IOException;
}
