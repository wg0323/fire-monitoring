<template>
  <div v-loading="loading" class="fm-bar-chart" :style="{ height }">
    <div v-if="hasData" ref="chartRef" class="chart-canvas"></div>
    <div v-else class="chart-empty">{{ emptyText }}</div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import * as echarts from 'echarts/core';
import { BarChart } from 'echarts/charts';
import { GridComponent, TooltipComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import type { EChartsOption } from 'echarts';
import type { StatRanking } from '@/types/api';

echarts.use([BarChart, GridComponent, TooltipComponent, CanvasRenderer]);

const props = withDefaults(
  defineProps<{
    items: StatRanking[];
    height?: string;
    loading?: boolean;
    emptyText?: string;
  }>(),
  {
    height: '280px',
    loading: false,
    emptyText: '暂无排行数据',
  },
);

const chartRef = ref<HTMLDivElement | null>(null);
let chart: ReturnType<typeof echarts.init> | null = null;

const hasData = computed(() => props.items.some((item) => item.value > 0));

function render(): void {
  if (!hasData.value || !chartRef.value) {
    return;
  }
  if (!chart) {
    chart = echarts.init(chartRef.value);
  }
  const option: EChartsOption = {
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 8, right: 24, top: 16, bottom: 8, containLabel: true },
    xAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#e4e7ec', type: 'dashed' } },
      axisLabel: { color: '#98a2b3', fontSize: 12 },
    },
    yAxis: {
      type: 'category',
      inverse: true,
      data: props.items.map((item) => item.name),
      axisLine: { lineStyle: { color: '#e4e7ec' } },
      axisTick: { show: false },
      axisLabel: { color: '#475467', fontSize: 12, width: 110, overflow: 'truncate' },
    },
    series: [
      {
        type: 'bar',
        barMaxWidth: 18,
        itemStyle: { color: '#17395a', borderRadius: [0, 4, 4, 0] },
        data: props.items.map((item) => item.value),
      },
    ],
  };
  chart.setOption(option, true);
  chart.resize();
}

function resize(): void {
  chart?.resize();
}

watch(
  () => props.items,
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
.fm-bar-chart {
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
