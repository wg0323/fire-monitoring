<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1 class="page-title">故障分析</h1>
        <p class="page-subtitle">按日期段查看设备故障趋势与处置明细。</p>
      </div>
    </div>

    <div class="filter-bar">
      <el-date-picker
        v-model="range"
        type="daterange"
        value-format="YYYY-MM-DD"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        style="width: 260px"
      />
      <div class="filter-actions">
        <el-button type="primary" :loading="loading" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
    </div>

    <div class="chart-card">
      <h3 class="chart-title">故障趋势</h3>
      <FmLineChart :series="trendSeries" :loading="loading" height="300px" />
    </div>

    <el-table v-loading="loading" :data="rows" border row-key="id">
      <el-table-column prop="sn" label="设备编号" min-width="130" />
      <el-table-column label="派单来源" width="110">
        <template #default="{ row }">{{ handleSourceLabel(row.handleSource) }}</template>
      </el-table-column>
      <el-table-column label="上报时间" width="165">
        <template #default="{ row }">{{ formatDateTime(row.alarmTime) }}</template>
      </el-table-column>
      <el-table-column label="报警描述" min-width="120">
        <template #default="{ row }">{{ row.alarmTypeName || '-' }}</template>
      </el-table-column>
      <el-table-column label="商户名称" min-width="150">
        <template #default="{ row }">{{ row.merchantName || '-' }}</template>
      </el-table-column>
      <el-table-column label="地址" min-width="160">
        <template #default="{ row }">{{ row.position || '-' }}</template>
      </el-table-column>
      <el-table-column label="结果" width="110">
        <template #default="{ row }">{{ alarmResultLabel(row.result) }}</template>
      </el-table-column>
      <el-table-column label="处置状态" width="100">
        <template #default="{ row }">{{ alarmStatusLabel(row.status) }}</template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        background
        layout="total, sizes, prev, pager, next"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        @current-change="load"
        @size-change="handleSizeChange"
      />
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import dayjs from 'dayjs';
import { getStatFault } from '@/api/stat';
import type { DeviceTrendSeries, StatFaultItem, StatTrendPoint } from '@/types/api';
import FmLineChart from '@/components/chart/FmLineChart.vue';
import { alarmResultLabel, alarmStatusLabel, formatDateTime, handleSourceLabel } from '@/utils/format';

const loading = ref(false);
const rows = ref<StatFaultItem[]>([]);
const total = ref(0);
const trend = ref<StatTrendPoint[]>([]);
const range = ref<[string, string] | null>(defaultRange());
const query = reactive({ pageNum: 1, pageSize: 20 });

const trendSeries = computed<DeviceTrendSeries[]>(() => [
  {
    metric: 'fault',
    label: '故障',
    hasData: trend.value.some((point) => point.value > 0),
    points: trend.value.map((point) => ({ time: point.time, value: point.value })),
  },
]);

function defaultRange(): [string, string] {
  return [dayjs().subtract(6, 'day').format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')];
}

async function load(): Promise<void> {
  const [start, end] = range.value ?? defaultRange();
  loading.value = true;
  try {
    const result = await getStatFault({
      startTime: `${start} 00:00:00`,
      endTime: `${end} 23:59:59`,
      pageNum: query.pageNum,
      pageSize: query.pageSize,
    });
    rows.value = result.page.records;
    total.value = result.page.total;
    trend.value = result.trend;
  } catch (error) {
    // 失败已由请求拦截器提示，此处仅记录，避免 onMounted/事件处理器产生未处理的 rejection
    console.error('[stat/fault] 加载失败', error);
  } finally {
    loading.value = false;
  }
}

function search(): void {
  query.pageNum = 1;
  load();
}

function reset(): void {
  range.value = defaultRange();
  search();
}

function handleSizeChange(): void {
  query.pageNum = 1;
  load();
}

onMounted(load);
</script>

<style scoped>
.chart-card {
  padding: var(--fm-space-4);
  margin-bottom: var(--fm-space-4);
  background: var(--fm-card-bg);
  border: 1px solid var(--fm-border);
  border-radius: var(--fm-radius-md);
}

.chart-title {
  margin-bottom: var(--fm-space-3);
  font-size: 14px;
  font-weight: 600;
}
</style>
