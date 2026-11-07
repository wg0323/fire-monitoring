<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1 class="page-title">综合分析</h1>
        <p class="page-subtitle">按日期段汇总报警、故障、隐患、真警与通话，排行取前 10。</p>
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
        <el-button type="primary" :loading="loading" @click="load">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
    </div>

    <div class="kpi-grid">
      <StatKpiCard label="报警总数" :value="summary.alarmTotal" hint="火警级别" tone="danger" />
      <StatKpiCard label="故障总数" :value="summary.faultTotal" hint="设备故障与故障结论" tone="warning" />
      <StatKpiCard label="隐患总数" :value="summary.dangerTotal" hint="区间新增" />
      <StatKpiCard label="真警总数" :value="summary.fireTotal" hint="核警结论真实火警" tone="danger" />
      <StatKpiCard label="通话总数" :value="summary.callTotal" hint="外呼记录" tone="success" />
    </div>

    <div class="chart-grid">
      <div class="chart-card">
        <h3 class="chart-title">报警趋势</h3>
        <FmLineChart :series="trendSeries" :loading="loading" height="300px" />
      </div>
      <div class="chart-card">
        <h3 class="chart-title">隐患类型排行</h3>
        <FmBarChart :items="summary.dangerTypeRanking" :loading="loading" height="300px" />
      </div>
      <div class="chart-card">
        <h3 class="chart-title">故障商户排行</h3>
        <FmBarChart :items="summary.faultMerchantRanking" :loading="loading" height="300px" />
      </div>
      <div class="chart-card">
        <h3 class="chart-title">通话商户排行</h3>
        <FmBarChart :items="summary.callMerchantRanking" :loading="loading" height="300px" />
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import dayjs from 'dayjs';
import { getStatComprehensive } from '@/api/stat';
import type { DeviceTrendSeries, StatComprehensive } from '@/types/api';
import FmLineChart from '@/components/chart/FmLineChart.vue';
import FmBarChart from '@/components/chart/FmBarChart.vue';
import StatKpiCard from '../components/StatKpiCard.vue';

const loading = ref(false);
const range = ref<[string, string] | null>(defaultRange());
const summary = ref<StatComprehensive>({
  alarmTotal: 0,
  faultTotal: 0,
  dangerTotal: 0,
  fireTotal: 0,
  callTotal: 0,
  alarmTrend: [],
  dangerTypeRanking: [],
  faultMerchantRanking: [],
  callMerchantRanking: [],
});

const trendSeries = computed<DeviceTrendSeries[]>(() => [
  {
    metric: 'alarm',
    label: '报警',
    hasData: summary.value.alarmTrend.some((point) => point.value > 0),
    points: summary.value.alarmTrend.map((point) => ({ time: point.time, value: point.value })),
  },
]);

function defaultRange(): [string, string] {
  return [dayjs().subtract(6, 'day').format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')];
}

async function load(): Promise<void> {
  const [start, end] = range.value ?? defaultRange();
  loading.value = true;
  try {
    summary.value = await getStatComprehensive({
      startTime: `${start} 00:00:00`,
      endTime: `${end} 23:59:59`,
    });
  } catch (error) {
    // 失败已由请求拦截器提示，此处仅记录，避免 onMounted/事件处理器产生未处理的 rejection
    console.error('[stat/comprehensive] 加载失败', error);
  } finally {
    loading.value = false;
  }
}

function reset(): void {
  range.value = defaultRange();
  load();
}

onMounted(load);
</script>

<style scoped>
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: var(--fm-space-4);
  margin-bottom: var(--fm-space-4);
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--fm-space-4);
}

.chart-card {
  padding: var(--fm-space-4);
  background: var(--fm-card-bg);
  border: 1px solid var(--fm-border);
  border-radius: var(--fm-radius-md);
}

.chart-title {
  margin-bottom: var(--fm-space-3);
  font-size: 14px;
  font-weight: 600;
}

@media (max-width: 1439px) {
  .chart-grid {
    grid-template-columns: 1fr;
  }
}
</style>
