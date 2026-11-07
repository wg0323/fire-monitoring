<template>
  <div v-loading="loading" class="fm-line-chart" :style="{ height }">
    <div v-if="hasData" ref="chartRef" class="chart-canvas"></div>
    <div v-else class="chart-empty">{{ emptyText }}</div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import * as echarts from 'echarts/core';
import { LineChart } from 'echarts/charts';
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import type { EChartsOption } from 'echarts';
import type { DeviceTrendSeries } from '@/types/api';

echarts.use([LineChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer]);

const COLORS = ['#17395a', '#f26430', '#12b76a', '#f79009', '#d92d20', '#5d748b'];
const DARK_COLORS = ['#22d3ee', '#ff7a45', '#12b76a', '#f79009', '#d92d20', '#7c93ae'];

const props = withDefaults(
  defineProps<{
    series: DeviceTrendSeries[];
    height?: string;
    loading?: boolean;
    emptyText?: string;
    dark?: boolean;
    smooth?: boolean;
  }>(),
  {
    height: '320px',
    loading: false,
    emptyText: '暂无趋势数据',
    dark: false,
    smooth: true,
  },
);

const chartRef = ref<HTMLDivElement | null>(null);
let chart: ReturnType<typeof echarts.init> | null = null;

const hasData = computed(() =>
  props.series.some((item) => item.hasData && item.points.some((point) => point.value !== null)),
);

function render(): void {
  if (!hasData.value || !chartRef.value) {
    return;
  }
  if (!chart) {
    chart = echarts.init(chartRef.value);
  }
  const instance = chart;
  const times = props.series[0]?.points.map((point) => point.time) ?? [];
  const option: EChartsOption = {
    backgroundColor: 'transparent',
    color: props.dark ? DARK_COLORS : COLORS,
    tooltip: { trigger: 'axis' },
    legend: {
      top: 0,
      itemWidth: 12,
      itemHeight: 8,
      textStyle: { color: props.dark ? '#7c93ae' : '#475467', fontSize: 12 },
    },
    grid: { left: 24, right: 48, top: 36, bottom: 12, containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: times,
      axisLine: { lineStyle: { color: props.dark ? 'rgba(56, 189, 248, .28)' : '#e4e7ec' } },
      axisTick: { show: false },
      axisLabel: {
        color: props.dark ? '#7c93ae' : '#98a2b3',
        fontSize: 12,
        hideOverlap: true,
      },
    },
    yAxis: {
      type: 'value',
      splitLine: {
        lineStyle: { color: props.dark ? 'rgba(56, 189, 248, .12)' : '#e4e7ec', type: 'dashed' },
      },
      axisLabel: { color: props.dark ? '#7c93ae' : '#98a2b3', fontSize: 12 },
    },
    series: props.series
      .filter((item) => item.hasData)
      .map((item) => ({
        name: item.unit ? `${item.label}（${item.unit}）` : item.label,
        type: 'line',
        smooth: props.smooth,
        symbol: 'none',
        lineStyle: { width: 2 },
        data: item.points.map((point) => point.value ?? null),
      })),
  };
  instance.setOption(option, true);
  instance.resize();
}

function resize(): void {
  chart?.resize();
}

watch(
  () => props.series,
  async () => {
    await nextTick();
    render();
  },
  { deep: true },
);

watch(hasData, async (visible) => {
  if (!visible) {
    chart?.dispose();
    chart = null;
    return;
  }
  await nextTick();
  render();
});

onMounted(async () => {
  window.addEventListener('resize', resize);
  await nextTick();
  render();
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize);
  chart?.dispose();
  chart = null;
});
</script>

<style scoped>
.fm-line-chart {
  width: 100%;
}

.chart-canvas {
  width: 100%;
  height: 100%;
}

.chart-empty {
  display: grid;
  place-items: center;
  height: 100%;
  color: var(--fm-text-muted);
  font-size: 13px;
}
</style>
