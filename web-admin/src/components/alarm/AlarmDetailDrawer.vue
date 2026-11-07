<template>
  <el-drawer
    :model-value="modelValue"
    title="报警详情"
    size="720px"
    destroy-on-close
    @update:model-value="close"
    @open="load"
  >
    <div v-loading="loading" class="alarm-detail">
      <template v-if="alarm">
        <div class="alarm-detail__head">
          <AlarmLevelTag :level="alarm.alarmLevel" />
          <span class="alarm-detail__sn">{{ alarm.sn }}</span>
          <span class="alarm-detail__merchant">{{ alarm.merchantName || '-' }}</span>
          <el-tag v-if="pendingReview" type="danger" effect="light">待复核</el-tag>
          <el-tag v-else-if="alarm.status === 1" type="success" effect="plain">已处理</el-tag>
          <el-tag v-else type="warning" effect="plain">未处理</el-tag>
        </div>

        <h3>报警信息</h3>
        <el-table :data="infoRows" border size="small" :show-header="false" class="alarm-detail__table">
          <el-table-column prop="label" width="120" />
          <el-table-column prop="value" show-overflow-tooltip />
        </el-table>

        <h3>处置时间线（报警追踪表）</h3>
        <el-empty v-if="timeline.length === 0" description="暂无处置记录" />
        <el-timeline v-else>
          <el-timeline-item
            v-for="item in timeline"
            :key="item.id"
            :timestamp="formatDateTime(item.createTime)"
            placement="top"
          >
            <div class="timeline-content">
              <el-tag size="small" effect="plain">{{ traceTypeLabel(item.traceType) }}</el-tag>
              <span>{{ traceResultLabel(item.result) }}</span>
              <span>{{ item.content || '-' }}</span>
            </div>
          </el-timeline-item>
        </el-timeline>
      </template>
      <el-empty v-else-if="!loading" description="报警不存在" />
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import AlarmLevelTag from '@/components/alarm/AlarmLevelTag.vue';
import { getAlarm, getAlarmTimeline } from '@/api/alarm';
import type { AlarmItem, AlarmTraceItem } from '@/types/api';
import {
  alarmLevelLabel,
  alarmResultLabel,
  alarmStatusLabel,
  formatDateTime,
  handleSourceLabel,
  traceResultLabel,
  traceTypeLabel,
  verifyStatusLabel,
  workOrderStatusLabel,
} from '@/utils/format';

interface InfoRow {
  label: string;
  value: string;
}

const TRACE_DISPATCH = 4;

function isDispatched(item: AlarmItem): boolean {
  return Boolean(item.workOrderId) || item.lastTraceType === TRACE_DISPATCH;
}

const props = defineProps<{ modelValue: boolean; alarmId: string; readonly?: boolean }>();
const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void;
  (event: 'refresh'): void;
}>();

const loading = ref(false);
const alarm = ref<AlarmItem | null>(null);
const timeline = ref<AlarmTraceItem[]>([]);

const pendingReview = computed(() =>
  alarm.value?.status === 0
  && alarm.value?.verifyStatus === 1
  && alarm.value?.handleSource === 'MERCHANT');

const verifyText = computed(() => {
  if (!alarm.value) {
    return '-';
  }
  return pendingReview.value ? '待复核' : verifyStatusLabel(alarm.value.verifyStatus);
});

const infoRows = computed<InfoRow[]>(() => {
  const item = alarm.value;
  if (!item) {
    return [];
  }
  return [
    { label: '上报时间', value: formatDateTime(item.alarmTime) },
    { label: '设备编号', value: item.sn },
    { label: '设备类型', value: item.deviceTypeName || item.deviceType || '-' },
    { label: '商户', value: item.merchantName || '-' },
    { label: '楼层', value: item.floorName || '未绑定楼层' },
    { label: '楼层图', value: item.floorPlanUrl ? '已上传' : '未上传' },
    { label: '位置', value: item.position || '-' },
    { label: '报警类型', value: alarmLevelLabel(item.alarmLevel) },
    { label: '处理状态', value: alarmStatusLabel(item.status) },
    ...(isDispatched(item)
      ? []
      : [{ label: '电话告警', value: item.phoneAlerted ? '已告警' : '未告警' }]),
    { label: '核警状态', value: verifyText.value },
    { label: '处置结论', value: alarmResultLabel(item.result) },
    { label: '处置来源', value: handleSourceLabel(item.handleSource) },
    { label: '收口人', value: item.handlerName || '-' },
    { label: '收口时间', value: formatDateTime(item.finalizeTime) },
    { label: '派单工作区', value: item.workAreaName || '-' },
    { label: '工单状态', value: workOrderStatusLabel(item.workOrderStatus) },
    { label: '报警抓拍', value: item.snapshotUrl ? '已上传' : '无' },
    { label: '补充说明', value: item.addRemark || '-' },
  ];
});

async function load(): Promise<void> {
  if (!props.alarmId) {
    return;
  }
  loading.value = true;
  try {
    const [detail, traces] = await Promise.all([
      getAlarm(props.alarmId),
      getAlarmTimeline(props.alarmId),
    ]);
    alarm.value = detail;
    timeline.value = traces;
  } finally {
    loading.value = false;
  }
}

function close(value: boolean): void {
  emit('update:modelValue', value);
}
</script>

<style scoped>
.alarm-detail {
  min-height: 240px;
}

.alarm-detail h3 {
  margin: var(--fm-space-5) 0 var(--fm-space-3);
  color: var(--fm-text);
  font-size: 14px;
  font-weight: 600;
}

.alarm-detail__head {
  display: flex;
  align-items: center;
  gap: var(--fm-space-3);
}

.alarm-detail__sn {
  font-family: var(--fm-font-number);
  font-size: 16px;
  font-weight: 600;
}

.alarm-detail__merchant {
  color: var(--fm-text-secondary);
}

.alarm-detail__table {
  width: 100%;
}

.timeline-content {
  display: flex;
  align-items: center;
  gap: var(--fm-space-2);
  color: var(--fm-text-secondary);
}
</style>
