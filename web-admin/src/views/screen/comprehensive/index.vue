<template>
  <div class="screen-grid">
    <div class="screen-col">
      <ScreenPanel title="今日警情与隐患" grow>
        <div class="screen-section-label">今日火警</div>
        <div class="screen-kpi-grid screen-kpi-grid--3">
          <ScreenKpi label="火警总量" :value="data?.fireTotal ?? 0" unit="起" tone="danger" />
          <ScreenKpi label="已处理" :value="data?.fireHandled ?? 0" unit="起" tone="success" />
          <ScreenKpi label="真警" :value="data?.fireTrue ?? 0" unit="起" tone="orange" />
        </div>
        <div class="screen-section-label">隐患处置</div>
        <div class="screen-kpi-grid screen-kpi-grid--3">
          <ScreenKpi label="未处理" :value="data?.dangerUnhandled ?? 0" unit="条" tone="orange" />
          <ScreenKpi label="当日新增" :value="data?.dangerToday ?? 0" unit="条" />
          <ScreenKpi label="逾期" :value="data?.dangerOverdue ?? 0" unit="条" tone="danger" />
        </div>
        <div class="screen-section-label">巡查情况</div>
        <div class="screen-kpi-grid screen-kpi-grid--2">
          <ScreenKpi label="当日巡查商户" :value="data?.patrolMerchants ?? 0" unit="家" />
          <ScreenKpi label="巡查占比" :value="percentText(data?.patrolPercent)" />
        </div>
      </ScreenPanel>
    </div>

    <div class="screen-col">
      <ScreenPanel title="商户状态地图" grow>
        <ScreenMerchantMap :points="merchantPoints" />
      </ScreenPanel>
    </div>

    <div class="screen-col">
      <ScreenPanel title="设备总览">
        <div class="screen-kpi-grid screen-kpi-grid--2">
          <ScreenKpi label="接入商户" :value="data?.accessMerchants ?? 0" unit="家" />
          <ScreenKpi label="接入设备" :value="data?.accessDevices ?? 0" unit="台" />
          <ScreenKpi label="在线设备" :value="deviceOnline" unit="台" />
          <ScreenKpi label="离线设备" :value="data?.deviceOffline ?? 0" unit="台" tone="offline" />
        </div>
      </ScreenPanel>
      <ScreenPanel title="设备状态占比" grow>
        <div v-for="bar in deviceBars" :key="bar.label" class="screen-bar-row">
          <div class="screen-bar-head">
            <span>{{ bar.label }}</span>
            <span class="screen-bar-value">{{ bar.count }} <em>{{ bar.percent }}%</em></span>
          </div>
          <div class="screen-bar-track">
            <div class="screen-bar-fill" :style="{ width: `${bar.percent}%`, background: bar.color }"></div>
          </div>
        </div>
      </ScreenPanel>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { getComprehensiveScreen } from '@/api/dashboard';
import ScreenKpi from '../components/ScreenKpi.vue';
import ScreenMerchantMap from '../components/ScreenMerchantMap.vue';
import ScreenPanel from '../components/ScreenPanel.vue';
import { useScreenLoader } from '../useScreenLoader';
import { useScreenTick } from '../useScreenTick';

const { data, load } = useScreenLoader(getComprehensiveScreen);
const tick = useScreenTick();

const merchantPoints = computed(() => data.value?.merchantPoints ?? []);

// 与大屏设备状态口径一致：除“离线”外的正常/火警/故障设备均计入在线
const deviceOnline = computed(() => (data.value?.deviceTotal ?? 0) - (data.value?.deviceOffline ?? 0));

const deviceBars = computed(() => [
  { label: '正常', count: data.value?.deviceNormal ?? 0, percent: data.value?.normalPercent ?? 0, color: '#12b76a' },
  { label: '火警', count: data.value?.deviceAlarm ?? 0, percent: data.value?.alarmPercent ?? 0, color: '#d92d20' },
  { label: '故障', count: data.value?.deviceFault ?? 0, percent: data.value?.faultPercent ?? 0, color: '#f79009' },
  { label: '离线', count: data.value?.deviceOffline ?? 0, percent: data.value?.offlinePercent ?? 0, color: '#98a2b3' },
]);

function percentText(value?: number): string {
  return `${value ?? 0}%`;
}

onMounted(load);
watch(tick, load);
</script>

<style scoped>
.screen-bar-value em {
  margin-left: 4px;
  color: var(--screen-text-secondary);
  font-style: normal;
  font-size: 12px;
}
</style>
