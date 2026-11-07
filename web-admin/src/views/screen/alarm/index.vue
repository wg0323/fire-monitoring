<template>
  <div class="screen-grid screen-grid--two">
    <div class="screen-col">
      <ScreenPanel title="实时报警" grow scroll>
        <div v-if="!data?.alarms.length" class="screen-empty">暂无报警</div>
        <div v-else class="screen-list">
          <article
            v-for="item in data.alarms"
            :key="item.id"
            class="screen-list-item"
            :class="levelClass(item.alarmLevel)"
          >
            <div class="screen-item-top">
              <span class="screen-item-title">{{ item.merchantName || '未知商户' }}</span>
              <span class="screen-item-time">{{ formatTime(item.alarmTime) }}</span>
            </div>
            <div class="screen-item-sub">{{ item.position || '未设置位置' }}</div>
            <div class="screen-item-tags">
              <span class="screen-tag" :class="levelTagClass(item.alarmLevel)">
                {{ levelText(item.alarmLevel) }}
              </span>
              <span class="screen-tag" :class="item.status === 1 ? 'screen-tag--normal' : 'screen-tag--warning'">
                {{ item.status === 1 ? '已处理' : '未处理' }}
              </span>
              <span v-if="item.result !== null && item.result !== undefined" class="screen-tag screen-tag--offline">
                {{ resultText(item.result) }}
              </span>
            </div>
          </article>
        </div>
      </ScreenPanel>
    </div>

    <div class="screen-col">
      <ScreenPanel title="商户状态地图" grow>
        <ScreenMerchantMap :points="merchantPoints" />
      </ScreenPanel>

      <ScreenPanel class="screen-trend-panel" title="报警趋势">
        <template #extra>
          <div class="screen-range">
            <button
              v-for="item in ranges"
              :key="item.value"
              type="button"
              :class="{ 'is-active': range === item.value }"
              @click="setRange(item.value)"
            >
              {{ item.label }}
            </button>
          </div>
        </template>
        <FmLineChart :series="trendSeries" dark :smooth="false" height="100%" empty-text="暂无报警数据" />
      </ScreenPanel>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import dayjs from 'dayjs';
import { getAlarmScreen } from '@/api/dashboard';
import FmLineChart from '@/components/chart/FmLineChart.vue';
import ScreenMerchantMap from '../components/ScreenMerchantMap.vue';
import ScreenPanel from '../components/ScreenPanel.vue';
import { toTrendSeries } from '../screenData';
import { useScreenLoader } from '../useScreenLoader';
import { useScreenTick } from '../useScreenTick';

const ranges = [
  { value: 'week', label: '周' },
  { value: 'month', label: '月' },
  { value: 'year', label: '年' },
];

const range = ref('week');
const { data, load } = useScreenLoader(() => getAlarmScreen(range.value));
const tick = useScreenTick();

const merchantPoints = computed(() => data.value?.merchantPoints ?? []);
const trendSeries = computed(() => toTrendSeries(data.value?.trend ?? [], '报警数量', '条'));

function setRange(value: string): void {
  range.value = value;
  load();
}

function levelText(level?: number | null): string {
  if (level === 1) {
    return '火警';
  }
  return '故障';
}

function levelClass(level?: number | null): string {
  return level === 1 ? 'screen-list-item--fire' : 'screen-list-item--warning';
}

function levelTagClass(level?: number | null): string {
  return level === 1 ? 'screen-tag--fire' : 'screen-tag--warning';
}

function resultText(result: number): string {
  if (result === 1) {
    return '真警';
  }
  if (result === 2) {
    return '故障';
  }
  return '误报';
}

function formatTime(value?: string | null): string {
  return value ? dayjs(value).format('HH:mm:ss') : '-';
}

onMounted(load);
watch(tick, load);
</script>

<style scoped>
.screen-trend-panel {
  flex: 0 0 300px;
}
</style>
