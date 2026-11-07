<template>
  <section v-loading="loading" class="page-card">
    <div class="detail-header">
      <div>
        <el-button class="back-link" link type="primary" @click="emit('back')">
          <svg
            width="14"
            height="14"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            aria-hidden="true"
          >
            <path d="M15 18l-6-6 6-6" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
          返回列表
        </el-button>
        <h1 class="page-title">{{ detail?.name || detail?.sn || '设备详情' }}</h1>
        <p class="page-subtitle">
          {{ detail?.sn }}
          <el-tag
            v-if="detail"
            size="small"
            :type="detail.onlineStatus === 1 ? 'success' : 'info'"
            effect="plain"
          >
            {{ detail.onlineStatus === 1 ? '在线' : '离线' }}
          </el-tag>
        </p>
      </div>
      <el-button v-if="canEdit" type="primary" @click="emit('edit')">编辑设备</el-button>
    </div>

    <template v-if="detail">
      <el-descriptions :column="3" border class="summary">
        <el-descriptions-item label="设备类型">
          {{ detail.typeName || detail.type || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="所属商户">
          {{ detail.merchantName || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="定位方式">
          {{ locationTypeText(detail.locationType) }}
        </el-descriptions-item>
        <el-descriptions-item label="位置">
          {{ detail.locationText || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="激活日期">
          {{ formatDate(detail.activeDate) }}
        </el-descriptions-item>
        <el-descriptions-item label="最后通讯">
          {{ formatTime(detail.lastTime) }}
        </el-descriptions-item>
      </el-descriptions>

      <div class="detail-grid">
        <div class="panel">
          <div class="panel-title">实时快照</div>
          <div v-if="detail.snapshot.length" class="snapshot-grid">
            <div v-for="item in detail.snapshot" :key="item.label" class="snapshot-item">
              <div class="snapshot-label">{{ item.label }}</div>
              <div class="snapshot-value">
                {{ item.value }}<span v-if="item.unit" class="snapshot-unit">{{ item.unit }}</span>
              </div>
              <div class="snapshot-time">{{ formatTime(item.time) }}</div>
            </div>
          </div>
          <div v-else class="empty-block">暂无实时数据</div>
        </div>

        <div class="panel">
          <div class="panel-title">最近报警</div>
          <el-table
            v-if="detail.recentAlarms.length"
            :data="detail.recentAlarms"
            size="small"
            empty-text="暂无报警"
          >
            <el-table-column label="时间" min-width="140">
              <template #default="{ row }">{{ formatTime(row.alarmTime) }}</template>
            </el-table-column>
            <el-table-column label="类型" width="80">
              <template #default="{ row }">{{ row.alarmTypeName || row.alarmType || '-' }}</template>
            </el-table-column>
            <el-table-column label="级别" width="76">
              <template #default="{ row }">
                <el-tag size="small" :type="alarmLevelType(row.alarmLevel)" effect="plain">
                  {{ alarmLevelText(row.alarmLevel) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                {{ row.status === 1 ? '已处理' : '未处理' }}
              </template>
            </el-table-column>
            <el-table-column label="结论" width="76">
              <template #default="{ row }">{{ resultText(row.result) }}</template>
            </el-table-column>
          </el-table>
          <div v-else class="empty-block">暂无报警</div>
        </div>
      </div>

      <div class="panel trend-panel">
        <div class="panel-head">
          <span class="panel-title">历史趋势</span>
          <el-radio-group v-model="range" size="small" @change="loadTrend">
            <el-radio-button value="week">周</el-radio-button>
            <el-radio-button value="month">月</el-radio-button>
            <el-radio-button value="year">年</el-radio-button>
          </el-radio-group>
        </div>
        <FmLineChart
          :series="trends"
          :loading="trendLoading"
          height="340px"
          empty-text="该设备类型暂无历史趋势数据（水压/液位/电气火灾设备支持）"
        />
      </div>
    </template>
  </section>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import dayjs from 'dayjs';
import { getDeviceTrend } from '@/api/device';
import type { DeviceDetail, DeviceTrendSeries } from '@/types/api';
import FmLineChart from '@/components/chart/FmLineChart.vue';

const props = defineProps<{
  detail: DeviceDetail | null;
  loading: boolean;
  canEdit: boolean;
}>();

const emit = defineEmits<{
  (event: 'back'): void;
  (event: 'edit'): void;
}>();

const range = ref('week');
const trends = ref<DeviceTrendSeries[]>([]);
const trendLoading = ref(false);

watch(
  () => props.detail,
  (detail) => {
    range.value = 'week';
    trends.value = detail?.trends ?? [];
  },
  { immediate: true },
);

async function loadTrend(): Promise<void> {
  if (!props.detail) {
    return;
  }
  trendLoading.value = true;
  try {
    trends.value = await getDeviceTrend(props.detail.id, range.value);
  } finally {
    trendLoading.value = false;
  }
}

function locationTypeText(type?: number | null): string {
  if (type === 1) {
    return '室外 GPS';
  }
  if (type === 2) {
    return '室内楼层图';
  }
  return '未设置';
}

function formatTime(value?: string | null): string {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-';
}

function formatDate(value?: string | null): string {
  return value ? dayjs(value).format('YYYY-MM-DD') : '-';
}

function alarmLevelText(level?: number | null): string {
  if (level === 1) {
    return '火警';
  }
  return '故障';
}

function alarmLevelType(level?: number | null): 'danger' | 'warning' | 'info' {
  if (level === 1) {
    return 'danger';
  }
  if (level === 2) {
    return 'warning';
  }
  return 'info';
}

function resultText(result?: number | null): string {
  if (result === 1) {
    return '真警';
  }
  if (result === 2) {
    return '故障';
  }
  if (result === 0) {
    return '误报';
  }
  return '-';
}
</script>

<style scoped>
.detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--fm-space-4);
  margin-bottom: var(--fm-space-4);
}

.back-link {
  height: auto;
  padding: 0;
  margin-bottom: var(--fm-space-2);
  font-size: 13px;
}

.back-link svg {
  margin-right: var(--fm-space-1);
}

.summary {
  margin-bottom: var(--fm-space-4);
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.2fr);
  gap: var(--fm-space-4);
  margin-bottom: var(--fm-space-4);
}

.panel {
  padding: var(--fm-space-4);
  background: var(--fm-card-bg);
  border: 1px solid var(--fm-border);
  border-radius: var(--fm-radius-md);
}

.panel-title {
  color: var(--fm-text);
  font-size: 14px;
  font-weight: 600;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--fm-space-3);
}

.snapshot-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: var(--fm-space-3);
  margin-top: var(--fm-space-3);
}

.snapshot-item {
  padding: var(--fm-space-3);
  background: #f9fafb;
  border-radius: var(--fm-radius-sm);
}

.snapshot-label {
  color: var(--fm-text-secondary);
  font-size: 12px;
}

.snapshot-value {
  margin-top: var(--fm-space-1);
  color: var(--fm-text);
  font-family: var(--fm-font-number);
  font-size: 22px;
  font-weight: 600;
}

.snapshot-unit {
  margin-left: 4px;
  color: var(--fm-text-muted);
  font-size: 12px;
  font-weight: 400;
}

.snapshot-time {
  margin-top: var(--fm-space-1);
  color: var(--fm-text-muted);
  font-size: 12px;
}

.trend-panel {
  margin-top: var(--fm-space-2);
}
</style>
