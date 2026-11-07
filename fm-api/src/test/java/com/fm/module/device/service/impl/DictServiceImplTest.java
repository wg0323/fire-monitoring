package com.fm.module.device.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fm.common.exception.BizException;
import com.fm.module.alarm.mapper.FireAlarmMapper;
import com.fm.module.danger.mapper.DangerMapper;
import com.fm.module.device.dto.DictItemDTO;
import com.fm.module.device.entity.DictDeviceType;
import com.fm.module.device.mapper.DeviceMapper;
import com.fm.module.device.mapper.DictAdcodeMapper;
import com.fm.module.device.mapper.DictAlarmTypeMapper;
import com.fm.module.device.mapper.DictDangerTypeMapper;
import com.fm.module.device.mapper.DictDeviceMakerMapper;
import com.fm.module.device.mapper.DictDeviceTypeMapper;
import com.fm.module.device.mapper.DictMerchantTypeMapper;
import com.fm.module.device.mapper.MerchantMapper;
import com.fm.module.device.service.DictService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 字典名称解析与维护规则测试。
 */
class DictServiceImplTest {

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, DictDeviceType.class);
    }

    private final DictDeviceTypeMapper deviceTypeMapper = mock(DictDeviceTypeMapper.class);
    private final DeviceMapper deviceMapper = mock(DeviceMapper.class);
    private final DictService service = new DictServiceImpl(deviceTypeMapper,
            mock(DictDeviceMakerMapper.class), mock(DictAlarmTypeMapper.class),
            mock(DictDangerTypeMapper.class), mock(DictMerchantTypeMapper.class),
            mock(DictAdcodeMapper.class), deviceMapper, mock(MerchantMapper.class),
            mock(FireAlarmMapper.class), mock(DangerMapper.class));

    @Test
    void should_return_code_to_name_map_when_codes_exist() {
        DictDeviceType smoke = new DictDeviceType();
        smoke.setCode("dh-sa2ga-a");
        smoke.setName("大华感烟");
        when(deviceTypeMapper.selectList(any())).thenReturn(Collections.singletonList(smoke));

        Map<String, String> names = service.deviceTypeNames(Arrays.asList("dh-sa2ga-a", "dh-wt2ga"));

        assertEquals("大华感烟", names.get("dh-sa2ga-a"));
        assertFalse(names.containsKey("dh-wt2ga"));
        ArgumentCaptor<LambdaQueryWrapper<DictDeviceType>> captor = wrapperCaptor();
        verify(deviceTypeMapper).selectList(captor.capture());
        LambdaQueryWrapper<DictDeviceType> wrapper = captor.getValue();
        assertTrue(wrapper.getSqlSegment().contains(" IN "));
        Map<String, Object> params = wrapper.getParamNameValuePairs();
        assertTrue(params.containsValue("dh-sa2ga-a"), params.toString());
        assertTrue(params.containsValue("dh-wt2ga"), params.toString());
    }

    @Test
    void should_return_empty_map_when_codes_is_null() {
        Map<String, String> names = service.deviceTypeNames(null);

        assertEquals(Collections.emptyMap(), names);
        verify(deviceTypeMapper, never()).selectList(any());
    }

    @Test
    void should_not_query_dict_when_codes_are_blank() {
        Map<String, String> names = service.deviceTypeNames(Arrays.asList(null, " ", ""));

        assertEquals(Collections.emptyMap(), names);
        verify(deviceTypeMapper, never()).selectList(any());
    }

    @Test
    void should_keep_original_code_when_update() {
        DictDeviceType existing = new DictDeviceType();
        existing.setId(9L);
        existing.setCode("dh-sa2ga-a");
        existing.setName("旧名称");
        when(deviceTypeMapper.selectById(9L)).thenReturn(existing);

        DictItemDTO request = new DictItemDTO();
        request.setId(9L);
        request.setCode("changed-code");
        request.setName("新名称");
        request.setStatus(1);
        service.saveItem("device-type", request);

        ArgumentCaptor<DictDeviceType> captor = ArgumentCaptor.forClass(DictDeviceType.class);
        verify(deviceTypeMapper).updateById(captor.capture());
        assertEquals("dh-sa2ga-a", captor.getValue().getCode());
        assertEquals("新名称", captor.getValue().getName());
    }

    @Test
    void should_reject_create_when_code_exists_in_tenant() {
        when(deviceTypeMapper.selectCount(any())).thenReturn(1L);
        DictItemDTO request = new DictItemDTO();
        request.setCode("dh-sa2ga-a");
        request.setName("重复类型");

        BizException error = assertThrows(BizException.class, () -> service.saveItem("device-type", request));

        assertEquals("字典编码已存在", error.getMessage());
        verify(deviceTypeMapper, never()).insert(any(DictDeviceType.class));
    }

    @Test
    void should_reject_delete_when_device_referenced() {
        DictDeviceType existing = new DictDeviceType();
        existing.setId(9L);
        existing.setCode("dh-sa2ga-a");
        when(deviceTypeMapper.selectById(9L)).thenReturn(existing);
        when(deviceMapper.selectCount(any())).thenReturn(1L);

        BizException error = assertThrows(BizException.class, () -> service.deleteItem("device-type", 9L));

        assertEquals("该字典项已被业务数据引用，不能删除", error.getMessage());
        verify(deviceTypeMapper, never()).deleteById(anyLong());
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<LambdaQueryWrapper<DictDeviceType>> wrapperCaptor() {
        return ArgumentCaptor.forClass(LambdaQueryWrapper.class);
    }
}
