<template>
  <FmMap :points="mapPoints" height="100%" :searchable="false">
    <template #overlay>
      <div class="map-legend">
        <button
          v-for="item in legend"
          :key="item.status"
          type="button"
          class="map-legend__item"
          :class="{ 'is-off': hiddenStatuses.includes(item.status) }"
          :title="`点击${hiddenStatuses.includes(item.status) ? '显示' : '隐藏'}${item.label}`"
          @click="toggleStatus(item.status)"
        >
          <span class="map-legend__dot" :style="{ background: item.color }"></span>
          <span>{{ item.label }}</span>
          <span class="map-legend__value">{{ item.count }}</span>
        </button>
        <span class="map-legend__divider"></span>
        <button
          type="button"
          class="map-legend__item map-legend__item--filter"
          :class="{ 'is-active': typePanelOpen || selectedTypes.length > 0 }"
          :aria-expanded="typePanelOpen"
          :title="typePanelOpen ? '收起类型筛选' : '展开类型筛选'"
          @click="typePanelOpen = !typePanelOpen"
        >
          <span>类型筛选</span>
          <span v-if="selectedTypes.length > 0" class="map-legend__badge">
            {{ selectedTypes.length }}
          </span>
        </button>
      </div>
      <div v-if="typePanelOpen" class="map-type-panel">
        <div class="map-type-panel__head">
          <span>商户类型</span>
          <button type="button" class="map-type-panel__reset" @click="resetTypes">全部</button>
        </div>
        <div class="map-type-panel__grid">
          <button
            v-for="item in typeOptions"
            :key="item.code"
            type="button"
            class="map-type-panel__item"
            :class="{ 'is-active': selectedTypes.includes(item.code) }"
            :aria-pressed="selectedTypes.includes(item.code)"
            @click="toggleType(item.code)"
          >
            <svg class="map-type-panel__icon" viewBox="0 0 24 24" aria-hidden="true">
              <use :href="`#fm-type-${merchantTypeSymbol(item.code)}`" />
            </svg>
            <span class="map-type-panel__name">{{ item.name }}</span>
            <span class="map-type-panel__count">{{ item.count }}</span>
          </button>
        </div>
      </div>
    </template>
  </FmMap>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import FmMap from '@/components/map/FmMap.vue';
import { merchantTypeSymbol } from '@/components/map/merchantTypeIcon';
import type { MerchantStatusPoint } from '@/types/api';
import { toMapPoints } from '../screenData';

const props = defineProps<{
  points: MerchantStatusPoint[];
}>();

const STATUS_LEGEND = [
  { label: '正常', status: 0, color: '#12b76a' },
  { label: '火警', status: 1, color: '#d92d20' },
  { label: '故障', status: 2, color: '#f79009' },
  { label: '离线', status: 3, color: '#98a2b3' },
  { label: '未接入', status: 4, color: '#64748b' },
];

interface MerchantTypeOption {
  code: string;
  name: string;
  count: number;
}

const hiddenStatuses = ref<number[]>([]);
const selectedTypes = ref<string[]>([]);
const typePanelOpen = ref(false);

const typeOptions = computed<MerchantTypeOption[]>(() => {
  const groups = new Map<string, MerchantTypeOption>();
  for (const point of props.points) {
    const code = point.type ?? '';
    const group = groups.get(code);
    if (group) {
      group.count += 1;
    } else {
      groups.set(code, {
        code,
        name: point.typeName || point.type || '未分类',
        count: 1,
      });
    }
  }
  return [...groups.values()].sort((left, right) => right.count - left.count);
});

// 状态 3 与 4 在 toMapPoints 中同为灰色离线点，筛选基于原始状态值
const mapPoints = computed(() =>
  toMapPoints(
    props.points.filter(
      (point) => !hiddenStatuses.value.includes(point.status)
        && (selectedTypes.value.length === 0 || selectedTypes.value.includes(point.type ?? '')),
    ),
  ),
);

const legend = computed(() =>
  STATUS_LEGEND.map((item) => ({
    ...item,
    count: props.points.filter((point) => point.status === item.status).length,
  })),
);

function toggleStatus(status: number): void {
  hiddenStatuses.value = hiddenStatuses.value.includes(status)
    ? hiddenStatuses.value.filter((value) => value !== status)
    : [...hiddenStatuses.value, status];
}

function toggleType(code: string): void {
  selectedTypes.value = selectedTypes.value.includes(code)
    ? selectedTypes.value.filter((value) => value !== code)
    : [...selectedTypes.value, code];
}

function resetTypes(): void {
  selectedTypes.value = [];
}
</script>

<style scoped>
.map-legend {
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 4px 6px;
  background: rgb(5 11 20 / 78%);
  border: 1px solid var(--screen-border);
  border-radius: 4px;
  backdrop-filter: blur(6px);
}

.map-legend__item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 3px 8px;
  color: var(--screen-text-secondary);
  background: transparent;
  border: none;
  border-radius: 3px;
  font-size: 12px;
  cursor: pointer;
}

.map-legend__item:hover {
  color: var(--screen-text);
  background: rgb(56 189 248 / 12%);
}

.map-legend__item.is-off {
  opacity: 0.35;
}

.map-legend__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.map-legend__value {
  color: var(--screen-text);
  font-family: var(--fm-font-number, 'DIN', sans-serif);
}

.map-legend__divider {
  width: 1px;
  height: 12px;
  margin: 0 2px;
  background: var(--screen-border);
}

.map-legend__item.is-active {
  color: var(--screen-cyan);
  background: rgb(34 211 238 / 12%);
}

.map-legend__badge {
  min-width: 14px;
  padding: 0 4px;
  color: var(--screen-bg);
  background: var(--screen-cyan);
  border-radius: 999px;
  font-size: 10px;
  line-height: 14px;
  text-align: center;
}

.map-type-panel {
  width: 300px;
  margin-top: 8px;
  padding: 6px;
  background: rgb(5 11 20 / 88%);
  border: 1px solid var(--screen-border);
  border-radius: 4px;
  backdrop-filter: blur(6px);
}

.map-type-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 2px 6px 8px;
  margin-bottom: 4px;
  border-bottom: 1px solid var(--screen-border);
  color: var(--screen-text-secondary);
  font-size: 11px;
}

.map-type-panel__reset {
  padding: 0;
  color: var(--screen-cyan);
  background: transparent;
  border: none;
  font-size: 11px;
  cursor: pointer;
}

.map-type-panel__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 2px;
}

.map-type-panel__item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 8px;
  color: var(--screen-text-secondary);
  background: transparent;
  border: none;
  border-radius: 3px;
  font-size: 12px;
  text-align: left;
  cursor: pointer;
}

.map-type-panel__item:hover {
  color: var(--screen-text);
  background: rgb(56 189 248 / 12%);
}

.map-type-panel__item.is-active {
  color: var(--screen-text);
  background: rgb(34 211 238 / 16%);
}

.map-type-panel__icon {
  flex: none;
  width: 14px;
  height: 14px;
}

.map-type-panel__name {
  overflow: hidden;
  flex: 1;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.map-type-panel__count {
  color: var(--screen-text);
  font-family: var(--fm-font-number, 'DIN', sans-serif);
}
</style>
