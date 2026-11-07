<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1 class="page-title">{{ title }}</h1>
        <p class="page-subtitle">{{ subtitle }}</p>
      </div>
      <el-tag effect="plain" type="info">共 {{ deviceCount }} 台设备</el-tag>
    </div>

    <div class="archive-layout">
      <aside class="tree-panel">
        <el-input v-model="keyword" placeholder="搜索商户 / 楼宇 / 楼层 / 设备" clearable />
        <div v-loading="treeLoading" class="tree-body">
          <el-tree
            v-if="treeData.length"
            ref="treeRef"
            :data="treeData"
            node-key="key"
            :props="{ label: 'label', children: 'children' }"
            :filter-node-method="filterNode"
            :expand-on-click-node="false"
            highlight-current
            default-expand-all
            @node-click="handleNodeClick"
          />
          <div v-if="!treeLoading && !treeData.length" class="empty-block">暂无档案设备</div>
        </div>
      </aside>

      <div class="cards-panel">
        <div v-if="!devices.length" class="empty-block">该节点暂无设备</div>
        <div v-else class="device-grid">
          <article
            v-for="device in devices"
            :key="device.deviceId"
            class="device-card"
            role="button"
            tabindex="0"
            @click="openTrend(device)"
            @keyup.enter="openTrend(device)"
          >
            <header class="device-head">
              <div class="device-title">
                <span class="device-name">{{ device.name || device.sn }}</span>
                <span class="device-sn">{{ device.sn }}</span>
              </div>
              <el-tag size="small" :type="device.onlineStatus === 1 ? 'success' : 'info'" effect="plain">
                {{ device.onlineStatus === 1 ? '在线' : '离线' }}
              </el-tag>
            </header>

            <div v-if="metrics(device).length" class="metric-grid">
              <div v-for="metric in metrics(device)" :key="metric.label" class="metric-item">
                <div class="metric-label">{{ metric.label }}</div>
                <div class="metric-value" :class="{ 'is-abnormal': metric.abnormal }">
                  {{ metric.value }}<span v-if="metric.unit" class="metric-unit">{{ metric.unit }}</span>
                </div>
              </div>
            </div>
            <div v-else class="card-empty">暂无实时数据</div>

            <footer class="device-foot">
              <span class="device-position">{{ device.position || '未设置位置' }}</span>
              <span class="device-time">{{ formatTime(device.measureTime) }}</span>
            </footer>
          </article>
        </div>
      </div>
    </div>

    <el-dialog v-model="trendVisible" :title="trendTitle" width="720px" destroy-on-close>
      <div class="trend-head">
        <span class="trend-device">{{ trendDevice?.sn }}</span>
        <el-radio-group v-model="range" size="small" @change="loadTrend">
          <el-radio-button value="week">周</el-radio-button>
          <el-radio-button value="month">月</el-radio-button>
          <el-radio-button value="year">年</el-radio-button>
        </el-radio-group>
      </div>
      <div v-if="trends.length > 1" class="metric-tabs">
        <button
          v-for="item in trends"
          :key="item.metric"
          type="button"
          :class="{ 'is-active': activeMetric === item.metric }"
          @click="activeMetric = item.metric"
        >
          {{ item.label }}
        </button>
      </div>
      <FmLineChart :series="visibleTrends" :loading="trendLoading" height="360px" empty-text="暂无历史趋势数据" />
      <div v-if="visibleTrends.length" class="trend-stats">
        <div v-for="item in visibleTrends" :key="item.metric" class="trend-stat">
          <div class="trend-stat-label">{{ item.label }}<span v-if="item.unit">（{{ item.unit }}）</span></div>
          <div class="trend-stat-values">
            <span>最大 {{ formatNumber(item.max) }}</span>
            <span>最小 {{ formatNumber(item.min) }}</span>
            <span>平均 {{ formatNumber(item.avg) }}</span>
          </div>
        </div>
      </div>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue';
import { ElTree } from 'element-plus';
import dayjs from 'dayjs';
import { archiveTree, archiveTrend, type ArchiveKind } from '@/api/archive';
import type {
  ArchiveBuildingNode,
  ArchiveDeviceItem,
  ArchiveFloorNode,
  ArchiveMerchantNode,
  DeviceTrendSeries,
} from '@/types/api';
import FmLineChart from '@/components/chart/FmLineChart.vue';

const props = defineProps<{
  kind: ArchiveKind;
  title: string;
  subtitle: string;
}>();

type TreeNodeType = 'merchant' | 'building' | 'floor' | 'device';

interface TreeNode {
  key: string;
  label: string;
  type: TreeNodeType;
  device?: ArchiveDeviceItem;
  children?: TreeNode[];
}

interface Metric {
  label: string;
  value: string;
  unit?: string;
  abnormal?: boolean;
}

interface ThresholdRange {
  min: number;
  max: number;
}

/** 设备出厂阈值接入前的系统临时阈值，接入后由设备出厂设置覆盖 */
const WATER_PRESSURE_RANGE: ThresholdRange = { min: 0.05, max: 0.8 };
const WATER_LEVEL_RANGE: ThresholdRange = { min: 10, max: 200 };
const VOLTAGE_RANGE: ThresholdRange = { min: 198, max: 242 };
const CURRENT_RANGE: ThresholdRange = { min: 0, max: 100 };
const RESIDUAL_CURRENT_RANGE: ThresholdRange = { min: 0, max: 0.5 };
const TEMPERATURE_RANGE: ThresholdRange = { min: -10, max: 60 };

const keyword = ref('');
const treeRef = ref<InstanceType<typeof ElTree> | null>(null);
const treeLoading = ref(false);
const treeData = ref<TreeNode[]>([]);
const selected = ref<TreeNode | null>(null);
const deviceCount = ref(0);

const devices = computed(() => (selected.value ? collectDevices(selected.value) : []));

const trendVisible = ref(false);
const trendDevice = ref<ArchiveDeviceItem | null>(null);
const range = ref('week');
const trends = ref<DeviceTrendSeries[]>([]);
const activeMetric = ref('');
const trendLoading = ref(false);
const trendTitle = computed(() =>
  trendDevice.value ? `${trendDevice.value.name || trendDevice.value.sn} · 历史趋势` : '历史趋势',
);
const visibleTrends = computed(() => {
  if (!trends.value.length) {
    return [];
  }
  const active = trends.value.find((item) => item.metric === activeMetric.value);
  return active ? [active] : [trends.value[0]];
});

watch(keyword, (value) => {
  treeRef.value?.filter(value);
});

onMounted(loadTree);

async function loadTree(): Promise<void> {
  treeLoading.value = true;
  try {
    const data = await archiveTree(props.kind);
    treeData.value = buildTree(data);
    deviceCount.value = data.reduce((sum, merchant) => sum + countMerchant(merchant), 0);
    await nextTick();
    const first = treeData.value[0];
    if (first) {
      treeRef.value?.setCurrentKey(first.key);
      selected.value = first;
    }
  } catch (error) {
    // 失败已由请求拦截器提示，此处仅记录，避免 onMounted 产生未处理的 rejection
    console.error('[archive] 加载设备树失败', error);
  } finally {
    treeLoading.value = false;
  }
}

function buildTree(merchants: ArchiveMerchantNode[]): TreeNode[] {
  return merchants.map((merchant) => ({
    key: `m-${merchant.merchantId}`,
    label: merchant.merchantName || '未命名商户',
    type: 'merchant',
    children: [
      ...(merchant.buildings ?? []).map(buildBuilding),
      ...(merchant.devices ?? []).map(buildDevice),
    ],
  }));
}

function buildBuilding(building: ArchiveBuildingNode): TreeNode {
  return {
    key: `b-${building.buildingId}`,
    label: building.buildingName || '未命名楼宇',
    type: 'building',
    children: [
      ...(building.floors ?? []).map(buildFloor),
      ...(building.devices ?? []).map(buildDevice),
    ],
  };
}

function buildFloor(floor: ArchiveFloorNode): TreeNode {
  return {
    key: `f-${floor.floorId}`,
    label: floor.floorName || '未命名楼层',
    type: 'floor',
    children: (floor.devices ?? []).map(buildDevice),
  };
}

function buildDevice(device: ArchiveDeviceItem): TreeNode {
  return {
    key: `d-${device.deviceId}`,
    label: device.name || device.sn,
    type: 'device',
    device,
  };
}

function countMerchant(merchant: ArchiveMerchantNode): number {
  const direct = merchant.devices?.length ?? 0;
  return (merchant.buildings ?? []).reduce((sum, building) => {
    const buildingDirect = building.devices?.length ?? 0;
    const floors = (building.floors ?? []).reduce((floorSum, floor) => floorSum + (floor.devices?.length ?? 0), 0);
    return sum + buildingDirect + floors;
  }, direct);
}

function collectDevices(node: TreeNode): ArchiveDeviceItem[] {
  if (node.device) {
    return [node.device];
  }
  return (node.children ?? []).reduce<ArchiveDeviceItem[]>(
    (list, child) => list.concat(collectDevices(child)),
    [],
  );
}

function filterNode(value: string, data: unknown): boolean {
  const label = (data as TreeNode).label;
  return !value || label.includes(value);
}

function handleNodeClick(node: TreeNode): void {
  selected.value = node;
}

function metrics(device: ArchiveDeviceItem): Metric[] {
  if (props.kind === 'water') {
    if (device.measureValue === null || device.measureValue === undefined) {
      return [];
    }
    const pressure = device.measureType === null || device.measureType === undefined || device.measureType === 1;
    const value = pressure ? device.measureValue / 100 : device.measureValue;
    return [
      {
        label: pressure ? '水压' : '液位',
        value: pressure ? value.toFixed(2) : String(value),
        unit: pressure ? 'MPa' : 'cm',
        abnormal: isAbnormal(value, pressure ? WATER_PRESSURE_RANGE : WATER_LEVEL_RANGE),
      },
    ];
  }
  const result: Metric[] = [];
  pushMetric(result, '电压 A', device.voltageA, 1, 0, 'V', VOLTAGE_RANGE);
  pushMetric(result, '电流 A', device.currentA, 0.1, 1, 'A', CURRENT_RANGE);
  pushMetric(result, '剩余电流', device.residualCurrent, 0.1, 1, 'A', RESIDUAL_CURRENT_RANGE);
  pushMetric(result, '温度', device.temperature, 0.1, 1, '℃', TEMPERATURE_RANGE);
  return result;
}

function pushMetric(
  list: Metric[],
  label: string,
  raw: number | null | undefined,
  factor: number,
  digits: number,
  unit: string,
  range: ThresholdRange,
): void {
  if (raw === null || raw === undefined) {
    return;
  }
  const value = raw * factor;
  list.push({ label, value: value.toFixed(digits), unit, abnormal: isAbnormal(value, range) });
}

function isAbnormal(value: number, range: ThresholdRange): boolean {
  return value < range.min || value > range.max;
}

async function openTrend(device: ArchiveDeviceItem): Promise<void> {
  trendDevice.value = device;
  range.value = 'week';
  trends.value = [];
  activeMetric.value = '';
  trendVisible.value = true;
  await loadTrend();
}

async function loadTrend(): Promise<void> {
  if (!trendDevice.value) {
    return;
  }
  trendLoading.value = true;
  try {
    trends.value = await archiveTrend(props.kind, trendDevice.value.deviceId, range.value);
    if (!trends.value.some((item) => item.metric === activeMetric.value)) {
      activeMetric.value = trends.value[0]?.metric ?? '';
    }
  } catch (error) {
    // 失败已由请求拦截器提示，此处仅记录，避免事件处理器产生未处理的 rejection
    console.error('[archive] 加载历史趋势失败', error);
  } finally {
    trendLoading.value = false;
  }
}

function formatTime(value?: string | null): string {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '暂无数据';
}

function formatNumber(value?: number | null): string {
  return value === null || value === undefined ? '-' : String(value);
}
</script>

<style scoped>
.archive-layout {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: var(--fm-space-4);
  min-height: 520px;
}

.tree-panel {
  display: flex;
  flex-direction: column;
  gap: var(--fm-space-3);
  padding: var(--fm-space-4);
  border: 1px solid var(--fm-border);
  border-radius: var(--fm-radius-md);
}

.tree-body {
  flex: 1;
  min-height: 420px;
  overflow: auto;
}

.cards-panel {
  min-width: 0;
}

.device-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: var(--fm-space-4);
}

.device-card {
  display: flex;
  flex-direction: column;
  gap: var(--fm-space-3);
  padding: var(--fm-space-4);
  background: var(--fm-card-bg);
  border: 1px solid var(--fm-border);
  border-radius: var(--fm-radius-md);
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.device-card:hover,
.device-card:focus-visible {
  border-color: var(--fm-primary);
  box-shadow: var(--fm-shadow-card);
  outline: none;
}

.device-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--fm-space-2);
}

.device-title {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.device-name {
  overflow: hidden;
  color: var(--fm-text);
  font-size: 14px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.device-sn {
  margin-top: 2px;
  color: var(--fm-text-muted);
  font-size: 12px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--fm-space-2);
}

.metric-item {
  padding: var(--fm-space-2) var(--fm-space-3);
  background: #f9fafb;
  border-radius: var(--fm-radius-sm);
}

.metric-label {
  color: var(--fm-text-secondary);
  font-size: 12px;
}

.metric-value {
  margin-top: 2px;
  color: var(--fm-text);
  font-family: var(--fm-font-number);
  font-size: 20px;
  font-weight: 600;
}

.metric-value.is-abnormal {
  color: var(--fm-danger);
}

.metric-unit {
  margin-left: 3px;
  color: var(--fm-text-muted);
  font-size: 12px;
  font-weight: 400;
}

.card-empty {
  padding: var(--fm-space-4) 0;
  color: var(--fm-text-muted);
  font-size: 13px;
  text-align: center;
}

.device-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--fm-space-2);
  margin-top: auto;
  color: var(--fm-text-muted);
  font-size: 12px;
}

.device-position {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.device-time {
  flex-shrink: 0;
}

.trend-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--fm-space-3);
  margin-bottom: var(--fm-space-3);
}

.metric-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: var(--fm-space-2);
  margin-bottom: var(--fm-space-3);
}

.metric-tabs button {
  padding: 4px 12px;
  color: var(--fm-text-secondary);
  background: transparent;
  border: 1px solid var(--fm-border);
  border-radius: var(--fm-radius-sm);
  font-size: 12px;
  cursor: pointer;
}

.metric-tabs button.is-active {
  color: #fff;
  background: var(--fm-primary);
  border-color: var(--fm-primary);
}

.trend-device {
  color: var(--fm-text-secondary);
  font-size: 13px;
}

.trend-stats {
  display: grid;
  gap: var(--fm-space-2);
  margin-top: var(--fm-space-3);
}

.trend-stat {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--fm-space-3);
  padding: var(--fm-space-2) var(--fm-space-3);
  background: #f9fafb;
  border-radius: var(--fm-radius-sm);
}

.trend-stat-label {
  color: var(--fm-text);
  font-size: 13px;
  font-weight: 600;
}

.trend-stat-values {
  display: flex;
  gap: var(--fm-space-4);
  color: var(--fm-text-secondary);
  font-size: 12px;
}

@media (max-width: 1279px) {
  .archive-layout {
    grid-template-columns: 240px minmax(0, 1fr);
  }
}
</style>
