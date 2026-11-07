package com.fm.module.workorder.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fm.common.context.TenantContext;
import com.fm.common.exception.BizException;
import com.fm.module.alarm.entity.FireAlarm;
import com.fm.module.alarm.mapper.FireAlarmMapper;
import com.fm.module.alarm.service.AlarmDomainService;
import com.fm.module.device.mapper.MerchantMapper;
import com.fm.module.system.entity.SysRole;
import com.fm.module.system.mapper.SysUserMapper;
import com.fm.module.system.mapper.SysWorkAreaMapper;
import com.fm.module.system.service.DataScopeService;
import com.fm.module.workorder.dto.WorkOrderFinishDTO;
import com.fm.module.workorder.entity.WorkOrder;
import com.fm.module.workorder.mapper.WorkOrderMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 工单测试：完成时收口报警、处理人校验与派单幂等。
 */
class WorkOrderServiceImplTest {

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, WorkOrder.class);
        TableInfoHelper.initTableInfo(assistant, FireAlarm.class);
    }

    private final SysUserMapper userMapper = mock(SysUserMapper.class);
    private final SysWorkAreaMapper workAreaMapper = mock(SysWorkAreaMapper.class);
    private final MerchantMapper merchantMapper = mock(MerchantMapper.class);
    private final FireAlarmMapper alarmMapper = mock(FireAlarmMapper.class);
    private final AlarmDomainService alarmDomainService = mock(AlarmDomainService.class);
    private final DataScopeService dataScopeService = mock(DataScopeService.class);
    private final WorkOrderMapper workOrderMapper = mock(WorkOrderMapper.class);
    private TestService service;

    @BeforeEach
    void setUp() {
        service = new TestService(userMapper, workAreaMapper, merchantMapper, alarmMapper,
                alarmDomainService, dataScopeService);
        ReflectionTestUtils.setField(service, "baseMapper", workOrderMapper);
        when(dataScopeService.canAccess(1L)).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void finishFinalizesAlarmWithFieldSource() {
        WorkOrder order = processingOrder(100L);
        FireAlarm alarm = pendingAlarm();
        when(workOrderMapper.selectById(21L)).thenReturn(order);
        when(alarmMapper.selectById(5L)).thenReturn(alarm);
        when(workOrderMapper.update(isNull(), any())).thenReturn(1);

        service.finish(finishRequest(FireAlarm.RESULT_TRUE));

        verify(alarmDomainService).finalizeAlarm(eq(5L), eq(FireAlarm.RESULT_TRUE),
                eq(FireAlarm.SOURCE_FIELD), eq(100L), isNull(), any(), any(), any(), any());
        ArgumentCaptor<LambdaUpdateWrapper<WorkOrder>> captor = wrapperCaptor();
        verify(workOrderMapper).update(isNull(), captor.capture());
        assertTrue(captor.getValue().getSqlSet().contains("finish_time"));
    }

    @Test
    void finishRejectsFaultResult() {
        BizException error = assertThrows(BizException.class,
                () -> service.finish(finishRequest(FireAlarm.RESULT_FAULT)));

        assertEquals("处理结论只能是误报或真实火警", error.getMessage());
        verify(alarmDomainService, never()).finalizeAlarm(any(), any(), any(), any(), any(),
                any(), any(), any(), any());
    }

    @Test
    void finishKeepsFaultAlarmForDangerFlow() {
        WorkOrder order = processingOrder(100L);
        FireAlarm alarm = pendingAlarm();
        alarm.setAlarmLevel(3);
        alarm.setDangerConvertDeadline(LocalDateTime.now().plusHours(1));
        when(workOrderMapper.selectById(21L)).thenReturn(order);
        when(alarmMapper.selectById(5L)).thenReturn(alarm);
        when(workOrderMapper.update(isNull(), any())).thenReturn(1);

        service.finish(finishRequest(FireAlarm.RESULT_FALSE));

        verify(alarmDomainService, never()).finalizeAlarm(any(), any(), any(), any(), any(),
                any(), any(), any(), any());
        ArgumentCaptor<LambdaUpdateWrapper<WorkOrder>> captor = wrapperCaptor();
        verify(workOrderMapper).update(isNull(), captor.capture());
        assertTrue(captor.getValue().getSqlSet().contains("finish_time"));
    }

    @Test
    void finishRejectsOtherHandler() {
        WorkOrder order = processingOrder(200L);
        when(workOrderMapper.selectById(21L)).thenReturn(order);

        BizException error = assertThrows(BizException.class,
                () -> service.finish(finishRequest(FireAlarm.RESULT_TRUE)));

        assertEquals("只有工单处理人可以完成", error.getMessage());
        verify(alarmDomainService, never()).finalizeAlarm(any(), any(), anyString(), any(), any(),
                any(), any(), any(), any());
    }

    @Test
    void finishRejectsFinishedOrder() {
        WorkOrder order = processingOrder(100L);
        order.setStatus(WorkOrder.STATUS_FINISHED);
        when(workOrderMapper.selectById(21L)).thenReturn(order);

        BizException error = assertThrows(BizException.class,
                () -> service.finish(finishRequest(FireAlarm.RESULT_FALSE)));

        assertEquals("只有处理中的工单可以完成", error.getMessage());
    }

    @Test
    void finishToleratesAlarmAlreadyClosedElsewhere() {
        WorkOrder order = processingOrder(100L);
        when(workOrderMapper.selectById(21L)).thenReturn(order);
        when(alarmMapper.selectById(5L)).thenReturn(pendingAlarm());
        doThrow(new BizException("该报警已处理")).when(alarmDomainService).finalizeAlarm(
                any(), any(), anyString(), any(), any(), any(), any(), any(), any());
        when(workOrderMapper.update(isNull(), any())).thenReturn(1);

        service.finish(finishRequest(FireAlarm.RESULT_TRUE));

        verify(workOrderMapper).update(isNull(), any());
    }

    @Test
    void createFromAlarmIsIdempotent() {
        when(workOrderMapper.selectCount(any())).thenReturn(1L);

        service.createFromAlarm(1L, 10L, 5L, 3L, "报警工单", "内容", 1, 9L, "张师傅");

        verify(workOrderMapper, never()).insert(any(WorkOrder.class));
    }

    @Test
    void createFromAlarmWritesDispatchSourceAndWorkArea() {
        when(workOrderMapper.selectCount(any())).thenReturn(0L);
        when(workOrderMapper.insert(any(WorkOrder.class))).thenAnswer(invocation -> {
            WorkOrder order = invocation.getArgument(0);
            order.setId(21L);
            return 1;
        });

        service.createFromAlarm(1L, 10L, 5L, 3L, "报警工单", "内容", 2, 9L, "张师傅");

        ArgumentCaptor<WorkOrder> captor = ArgumentCaptor.forClass(WorkOrder.class);
        verify(workOrderMapper).insert(captor.capture());
        assertEquals(WorkOrder.SOURCE_DISPATCH, captor.getValue().getSource());
        assertEquals(3L, captor.getValue().getWorkAreaId());
        assertEquals(WorkOrder.STATUS_PENDING, captor.getValue().getStatus());
    }

    @Test
    void countMpTodoOnlyCountsOwnOrdersForFieldUser() {
        TenantContext.set(1L, "TENANT", SysRole.SCOPE_WORK_AREA, false);
        when(workOrderMapper.selectCount(any())).thenReturn(2L);

        assertEquals(2L, service.countMpTodo());

        ArgumentCaptor<LambdaQueryWrapper<WorkOrder>> captor = queryCaptor();
        verify(workOrderMapper).selectCount(captor.capture());
        assertTrue(captor.getValue().getSqlSegment().contains("handler_id"));
        assertTrue(captor.getValue().getParamNameValuePairs().containsValue(WorkOrder.STATUS_PENDING));
        assertTrue(captor.getValue().getParamNameValuePairs().containsValue(WorkOrder.STATUS_PROCESSING));
    }

    @Test
    void countMpTodoSkipsMerchantAccount() {
        TenantContext.set(1L, "MERCHANT", SysRole.SCOPE_MERCHANT, false);

        assertEquals(0L, service.countMpTodo());

        verify(workOrderMapper, never()).selectCount(any());
    }

    private WorkOrder processingOrder(Long handlerId) {
        WorkOrder order = new WorkOrder();
        order.setId(21L);
        order.setTenantId(1L);
        order.setMerchantId(10L);
        order.setAlarmId(5L);
        order.setWorkAreaId(3L);
        order.setHandlerId(handlerId);
        order.setStatus(WorkOrder.STATUS_PROCESSING);
        return order;
    }

    private FireAlarm pendingAlarm() {
        FireAlarm alarm = new FireAlarm();
        alarm.setId(5L);
        alarm.setTenantId(1L);
        alarm.setMerchantId(10L);
        alarm.setStatus(FireAlarm.STATUS_PENDING);
        alarm.setAlarmTime(LocalDateTime.now().minusMinutes(10));
        return alarm;
    }

    private WorkOrderFinishDTO finishRequest(int result) {
        WorkOrderFinishDTO request = new WorkOrderFinishDTO();
        request.setId(21L);
        request.setResult(result);
        request.setRemark("现场确认");
        return request;
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<LambdaUpdateWrapper<WorkOrder>> wrapperCaptor() {
        return ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<LambdaQueryWrapper<WorkOrder>> queryCaptor() {
        return ArgumentCaptor.forClass(LambdaQueryWrapper.class);
    }

    /**
     * 固定当前登录人，避免依赖 Sa-Token 请求上下文。
     */
    private static final class TestService extends WorkOrderServiceImpl {

        private TestService(SysUserMapper userMapper, SysWorkAreaMapper workAreaMapper,
                            MerchantMapper merchantMapper, FireAlarmMapper alarmMapper,
                            AlarmDomainService alarmDomainService, DataScopeService dataScopeService) {
            super(userMapper, workAreaMapper, merchantMapper, alarmMapper, alarmDomainService, dataScopeService);
        }

        @Override
        protected Long currentUserId() {
            return 100L;
        }
    }
}
