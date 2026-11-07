<template>
  <div class="screen-grid">
    <div class="screen-col">
      <ScreenPanel title="本月隐患">
        <div class="screen-kpi-grid screen-kpi-grid--2">
          <ScreenKpi label="隐患总数" :value="data?.monthTotal ?? 0" unit="条" />
          <ScreenKpi label="整改完成" :value="data?.monthDone ?? 0" unit="条" tone="success" />
          <ScreenKpi label="未完成" :value="data?.monthUnfinished ?? 0" unit="条" tone="orange" />
          <ScreenKpi label="逾期未处置" :value="data?.monthOverdue ?? 0" unit="条" tone="danger" />
        </div>
      </ScreenPanel>

      <ScreenPanel title="隐患趋势" grow>
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
        <FmLineChart :series="trendSeries" dark :smooth="false" height="100%" empty-text="暂无隐患数据" />
      </ScreenPanel>

      <ScreenPanel title="隐患类型排行" grow scroll>
        <div v-if="!data?.typeRanking.length" class="screen-empty">暂无隐患数据</div>
        <div v-else class="screen-rank">
          <div v-for="(item, index) in data.typeRanking" :key="item.name" class="screen-rank-row">
            <span class="screen-rank-index">{{ index + 1 }}</span>
            <div>
              <div>{{ item.name }}</div>
              <div class="screen-rank-track">
                <div class="screen-rank-fill" :style="{ width: `${rankPercent(item.value)}%` }"></div>
              </div>
            </div>
            <span class="screen-rank-value">{{ item.value }}</span>
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
      <ScreenPanel title="巡检计划">
        <div class="screen-kpi-grid screen-kpi-grid--3">
          <ScreenKpi label="本月计划" :value="data?.planCount ?? 0" unit="个" />
          <ScreenKpi label="已结束" :value="data?.planDone ?? 0" unit="个" tone="success" />
          <ScreenKpi label="巡查发现" :value="data?.planDanger ?? 0" unit="条" tone="orange" />
        </div>
      </ScreenPanel>

      <ScreenPanel title="巡检详情" grow scroll>
        <div v-if="!patrolPlans.length" class="screen-empty">暂无巡查记录</div>
        <div v-else class="screen-list">
          <article
            v-for="item in patrolPlans"
            :key="item.id"
            class="screen-list-item"
            :class="{ 'screen-list-item--warning': item.dangerCount > 0 }"
          >
            <div class="screen-item-top">
              <span class="screen-item-title">{{ item.name }}</span>
              <span class="screen-item-time">{{ item.startDate }} ~ {{ item.endDate }}</span>
            </div>
            <div class="screen-item-sub">
              {{ item.workAreaName || '未知工作区' }} · 已巡 {{ item.finishCount }}/{{ item.merchantTotal }}
            </div>
            <div class="screen-item-tags">
              <span class="screen-tag" :class="planTagClass(item.status)">
                {{ planStatusLabel(item.status) }}
              </span>
              <span class="screen-tag" :class="patrolTagClass(item.dangerCount)">
                发现隐患 {{ item.dangerCount }}
              </span>
            </div>
          </article>
        </div>
      </ScreenPanel>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { getDangerScreen } from '@/api/dashboard';
import { pageInspectionPlans } from '@/api/inspection';
import FmLineChart from '@/components/chart/FmLineChart.vue';
import { planStatusLabel } from '@/utils/format';
import ScreenKpi from '../components/ScreenKpi.vue';
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

const recentPatrolLimit = 5;
const range = ref('week');
const { data, load } = useScreenLoader(() => getDangerScreen(range.value));
const { data: patrolPage, load: loadPatrols } = useScreenLoader(() =>
  pageInspectionPlans({ pageNum: 1, pageSize: recentPatrolLimit }),
);
const tick = useScreenTick();

const patrolPlans = computed(() => patrolPage.value?.records ?? []);
const merchantPoints = computed(() => data.value?.merchantPoints ?? []);
const trendSeries = computed(() => toTrendSeries(data.value?.trend ?? [], '隐患数量', '条'));
const rankingMax = computed(() =>
  (data.value?.typeRanking ?? []).reduce((max, item) => Math.max(max, item.value), 0),
);

function rankPercent(value: number): number {
  return rankingMax.value > 0 ? Math.round((value * 100) / rankingMax.value) : 0;
}

function planTagClass(status: number): string {
  if (status === 2) {
    return 'screen-tag--normal';
  }
  if (status === 1) {
    return 'screen-tag--info';
  }
  return 'screen-tag--offline';
}

function patrolTagClass(dangerCount: number): string {
  return dangerCount > 0 ? 'screen-tag--warning' : 'screen-tag--normal';
}

function setRange(value: string): void {
  range.value = value;
  load();
}

function loadAll(): void {
  load();
  loadPatrols();
}

onMounted(loadAll);
watch(tick, loadAll);
</script>
