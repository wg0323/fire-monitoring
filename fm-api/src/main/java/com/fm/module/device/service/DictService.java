package com.fm.module.device.service;

import com.fm.module.device.entity.DictAdcode;
import com.fm.module.device.entity.DictAlarmType;
import com.fm.module.device.entity.DictDangerType;
import com.fm.module.device.entity.DictDeviceMaker;
import com.fm.module.device.entity.DictDeviceType;
import com.fm.module.device.entity.DictMerchantType;
import com.fm.module.device.dto.DictItemDTO;
import com.fm.module.device.vo.DictItemVO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 字典查询编排服务（非实体业务编排，纯接口风格）。
 */
public interface DictService {

    List<DictDeviceType> deviceTypes();

    /** 按类型代码批量取字典名称，未命中的代码不出现在结果中 */
    Map<String, String> deviceTypeNames(Collection<String> codes);

    Map<String, String> alarmTypeNames(Collection<String> codes);

    Map<String, String> dangerTypeNames(Collection<String> codes);

    Map<String, String> merchantTypeNames(Collection<String> codes);

    List<DictDeviceMaker> deviceMakers();

    List<DictAlarmType> alarmTypes();

    List<DictDangerType> dangerTypes();

    List<DictMerchantType> merchantTypes();

    List<DictAdcode> adcodes(String keyword);

    List<DictItemVO> adminList(String type);

    Long saveItem(String type, DictItemDTO request);

    void deleteItem(String type, Long id);
}
