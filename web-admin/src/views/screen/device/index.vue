<template>
  <div class="screen-grid">
    <div class="screen-col">
      <ScreenPanel title="设备类型分布" grow scroll>
        <template #extra>
          <span class="screen-type-total">共 {{ totalDevices }} 台</span>
        </template>
        <div v-if="!data?.types.length" class="screen-empty">暂无设备数据</div>
        <div v-else>
          <div v-for="item in data.types" :key="item.type" class="screen-type-row">
            <div class="screen-type-head">
              <span>{{ item.typeName }}</span>
              <span class="screen-type-total">{{ item.total }} 台</span>
            </div>
            <div class="screen-type-status">
              <span>正常 {{ item.normal }}</span>
              <span>报警 {{ item.alarm }}</span>
              <span>故障 {{ item.fault }}</span>
              <span>离线 {{ item.offline }}</span>
            </div>
            <div class="screen-type-bar">
              <span :style="{ width: `${item.normalPercent}%`, background: '#12b76a' }"></span>
              <span :style="{ width: `${item.alarmPercent}%`, background: '#d92d20' }"></span>
              <span :style="{ width: `${item.faultPercent}%`, background: '#f79009' }"></span>
              <span :style="{ width: `${item.offlinePercent}%`, background: '#98a2b3' }"></span>
            </div>
          </div>
        </div>
      </ScreenPanel>
    </div>

    <div class="screen-col">
      <ScreenPanel title="商户状态地图" grow>
        <ScreenMerchantMap :points="merchantPoints" />
      </ScreenPanel>
    </div>

    <div class="screen-col">
      <ScreenPanel class="screen-trend-panel" title="故障趋势">
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
        <FmLineChart :series="trendSeries" dark :smooth="false" height="100%" empty-text="暂无故障数据" />
      </ScreenPanel>

      <ScreenPanel title="故障滚动" grow scroll>
        <div v-if="!data?.faults.length" class="screen-empty">暂无故障</div>
        <div v-else class="screen-list">
          <article v-for="item in data.faults" :key="item.id" class="screen-list-item screen-list-item--warning">
            <div class="screen-item-top">
              <span class="screen-item-title">{{ item.deviceName || item.deviceSn || '未知设备' }}</span>
              <span class="screen-item-time">{{ formatTime(item.alarmTime) }}</span>
            </div>
            <div class="screen-item-sub">{{ item.merchantName || '未知商户' }} · {{ item.position || '未设置位置' }}</div>
            <div class="screen-item-tags">
              <span class="screen-tag" :class="item.recoverTime ? 'screen-tag--normal' : 'screen-tag--warning'">
                {{ item.recoverTime ? '已恢复' : '未恢复' }}
              </span>
              <span class="screen-tag screen-tag--offline">{{ item.deviceSn || '-' }}</span>
            </div>
          </article>
        </div>
      </ScreenPanel>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import dayjs from 'dayjs';
import { getDeviceScreen } from '@/api/dashboard';
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
const { data, load } = useScreenLoader(() => getDeviceScreen(range.value));
const tick = useScreenTick();

const totalDevices = computed(() => (data.value?.types ?? []).reduce((sum, item) => sum + item.total, 0));
const trendSeries = computed(() => toTrendSeries(data.value?.faultTrend ?? [], '故障数量', '条'));
const merchantPoints = computed(() => data.value?.merchantPoints ?? []);

function setRange(value: string): void {
  range.value = value;
  load();
}

function formatTime(value?: string | null): string {
  return value ? dayjs(value).format('MM-DD HH:mm') : '-';
}

onMounted(load);
watch(tick, load);
</script>

<style scoped>
.screen-trend-panel {
  flex: 0 0 300px;
}
</style>
