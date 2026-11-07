package com.fm.module.device.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.fm.module.device.dto.DeviceQueryDTO;
import com.fm.module.device.dto.DeviceSaveDTO;
import com.fm.module.device.dto.FloorPointsSaveDTO;
import com.fm.module.device.entity.Device;
import com.fm.module.device.vo.DeviceVO;

import java.io.Serializable;
import java.util.List;

/**
 * 设备服务。
 */
public interface DeviceService extends IService<Device> {

    Page<DeviceVO> page(DeviceQueryDTO query);

    DeviceVO getVOById(Serializable id);

    List<DeviceVO> listForExport(DeviceQueryDTO query);

    Long create(DeviceSaveDTO request);

    void update(DeviceSaveDTO request);

    void delete(Long id);

    List<Device> floorPoints(Long floorId);

    List<Device> floorCandidates(Long floorId);

    void saveFloorPoints(Long floorId, FloorPointsSaveDTO request);
}
