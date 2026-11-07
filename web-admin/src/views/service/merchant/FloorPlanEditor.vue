<template>
  <el-dialog
    v-model="dialogVisible"
    :title="readonly ? '平面图查看' : '平面图布点'"
    width="1180px"
    top="4vh"
    destroy-on-close
    :before-close="beforeClose"
  >
    <div v-if="floors.length === 0" class="empty-block">请先在商户详情中维护楼宇与楼层</div>
    <div v-else class="plan-layout" :class="{ 'is-readonly': readonly }">
      <aside class="plan-tree">
        <div v-for="building in buildings" :key="building.id" class="tree-group">
          <div class="tree-building">{{ building.name }}</div>
          <button
            v-for="floor in building.floors"
            :key="floor.id"
            type="button"
            class="tree-item"
            :class="{ 'is-active': floor.id === selectedFloorId }"
            @click="changeFloor(floor.id)"
          >
            <span>{{ floor.name }}</span>
            <el-tag v-if="floor.floorPlanUrl" size="small" type="success" effect="plain">图</el-tag>
          </button>
        </div>
      </aside>

      <section class="plan-main">
        <div class="plan-toolbar">
          <div class="plan-toolbar__left">
            <strong>{{ currentFloor?.name || '-' }}</strong>
            <span class="muted">{{ currentFloor?.floorPlanUrl ? '平面图已上传' : '尚未上传平面图' }}</span>
          </div>
          <div class="plan-toolbar__right">
            <div class="zoom-group">
              <el-button size="small" :disabled="zoom <= 50" @click="changeZoom(-25)">-</el-button>
              <span>{{ zoom }}%</span>
              <el-button size="small" :disabled="zoom >= 400" @click="changeZoom(25)">+</el-button>
            </div>
            <el-upload
              v-if="!readonly"
              :show-file-list="false"
              accept="image/jpeg,image/png"
              :http-request="handlePlanUpload"
            >
              <el-button type="primary" plain :loading="uploading">
                {{ plan ? '更换平面图' : '上传平面图' }}
              </el-button>
            </el-upload>
            <el-button
              v-if="!readonly"
              type="primary"
              :loading="saving"
              :disabled="!plan || !dirty"
              @click="savePoints"
            >
              保存点位
            </el-button>
          </div>
        </div>

        <div v-loading="loading" class="plan-stage">
          <div v-if="plan" class="plan-canvas" :style="{ zoom: zoom / 100 }">
            <div
              class="plan-inner"
              :style="{ aspectRatio: `${plan.width} / ${plan.height}` }"
              @dragover.prevent
              @drop="handleDrop"
              @pointermove="handlePointerMove"
              @pointerup="handlePointerUp"
              @pointerleave="handlePointerUp"
            >
              <img
                :src="plan.url"
                class="plan-image"
                alt="楼层平面图"
                draggable="false"
              />
              <div
                v-for="point in points"
                :key="point.deviceId"
                class="plan-point"
                :class="{
                  'is-selected': point.deviceId === selectedPointId,
                  'is-offline': point.onlineStatus !== 1,
                  'is-readonly': readonly,
                }"
                :style="{ left: `${point.x}%`, top: `${point.y}%` }"
                :title="`${point.name} (${point.sn})`"
                @pointerdown.prevent="startDrag($event, point)"
                @click.stop="selectedPointId = point.deviceId"
              >
                <span>{{ typeLabel(point.type) }}</span>
              </div>
            </div>
          </div>
          <el-empty v-else description="上传平面图后可拖入设备布点" />
        </div>

        <div class="plan-footer">
          <span class="muted">
            {{ readonly ? '只读模式：可查看点位，不能修改。' : '拖拽点位调整位置；点击点位可选中并删除。' }}
          </span>
          <div v-if="selectedPoint" class="selected-actions">
            <span>已选中：{{ selectedPoint.name }}</span>
            <el-button
              v-if="!readonly"
              link
              type="danger"
              @click="removePoint(selectedPoint.deviceId)"
            >
              删除点位
            </el-button>
          </div>
        </div>
      </section>

      <aside v-if="!readonly" class="plan-unplaced">
        <div class="unplaced-head">
          <strong>未布点设备</strong>
          <span class="muted">{{ candidates.length }}</span>
        </div>
        <div v-if="candidates.length === 0" class="empty-block">没有可布点设备</div>
        <div
          v-for="device in candidates"
          :key="device.id"
          class="unplaced-item"
          draggable="true"
          @dragstart="handleDragStart($event, device)"
          @dragend="draggingDevice = null"
        >
          <div class="unplaced-item__main">
            <span class="unplaced-item__name">{{ device.name || device.sn }}</span>
            <span class="muted">{{ device.sn }}</span>
          </div>
          <el-button size="small" plain :disabled="!plan" @click="placeDevice(device)">
            放入
          </el-button>
        </div>
      </aside>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { UploadRequestOptions } from 'element-plus';
import {
  listFloorCandidates,
  listFloorPoints,
  saveFloorPoints,
  updateFloorPlan,
} from '@/api/device';
import { uploadImage } from '@/api/upload';
import type { BuildingNode, DevicePointItem, FloorNode } from '@/types/api';

interface EditorPoint {
  deviceId: string;
  name: string;
  sn: string;
  type: string;
  onlineStatus: number | null;
  x: number;
  y: number;
}

interface PlanState {
  url: string;
  width: number;
  height: number;
}

const props = defineProps<{
  visible: boolean;
  buildings: BuildingNode[];
  initialFloorId?: string;
  readonly?: boolean;
}>();

const TYPE_LABELS: Record<string, string> = {
  'dh-sa2ga-a': '烟',
  'dh-gb2ga-a': '气',
  'dh-wi2na': '浸',
  'dh-wt2ga': '温',
  'dh-video': '视',
  'dh-cms12gb': '终',
  'dh-wsm20gb': '水',
  'gb26875-3': '标',
  'dh-sa1ga-a': '接',
  'dh-level': '液',
  'dh-elec': '电',
};

const emit = defineEmits<{
  (event: 'update:visible', value: boolean): void;
  (event: 'saved'): void;
}>();

const selectedFloorId = ref('');
const points = ref<EditorPoint[]>([]);
const candidates = ref<DevicePointItem[]>([]);
const removedIds = ref<string[]>([]);
const originalIds = ref<Set<string>>(new Set());
const plan = ref<PlanState | null>(null);
const zoom = ref(100);
const loading = ref(false);
const saving = ref(false);
const uploading = ref(false);
const dirty = ref(false);
const selectedPointId = ref('');
const draggingId = ref('');
const draggingDevice = ref<DevicePointItem | null>(null);

const floors = computed<FloorNode[]>(() =>
  props.buildings.flatMap((building) => building.floors));

const currentFloor = computed<FloorNode | null>(() =>
  floors.value.find((floor) => floor.id === selectedFloorId.value) ?? null);

const selectedPoint = computed(() =>
  points.value.find((point) => point.deviceId === selectedPointId.value) ?? null);

const dialogVisible = computed({
  get: () => props.visible,
  set: (value: boolean) => emit('update:visible', value),
});

watch(
  () => props.visible,
  (visible) => {
    if (!visible) {
      return;
    }
    dirty.value = false;
    removedIds.value = [];
    selectedPointId.value = '';
    const target = props.initialFloorId && floors.value.some((floor) => floor.id === props.initialFloorId)
      ? props.initialFloorId
      : floors.value.find((floor) => floor.floorPlanUrl)?.id ?? floors.value[0]?.id ?? '';
    selectFloor(target);
  },
);

async function selectFloor(floorId: string): Promise<void> {
  if (!floorId) {
    return;
  }
  selectedFloorId.value = floorId;
  const floor = floors.value.find((item) => item.id === floorId);
  plan.value = floor?.floorPlanUrl
    ? {
      url: floor.floorPlanUrl,
      width: floor.planWidth ?? 1600,
      height: floor.planHeight ?? 1000,
    }
    : null;
  zoom.value = 100;
  dirty.value = false;
  removedIds.value = [];
  selectedPointId.value = '';
  await loadFloorData(floorId);
}

async function changeFloor(floorId: string): Promise<void> {
  if (floorId === selectedFloorId.value) {
    return;
  }
  if (dirty.value) {
    const confirmed = await ElMessageBox.confirm('当前楼层有未保存的点位修改，确认切换？', '未保存提示', {
      type: 'warning',
    }).then(() => true).catch(() => false);
    if (!confirmed) {
      return;
    }
  }
  await selectFloor(floorId);
}

async function loadFloorData(floorId: string): Promise<void> {
  loading.value = true;
  try {
    const [placed, unplaced] = await Promise.all([
      listFloorPoints(floorId),
      listFloorCandidates(floorId),
    ]);
    points.value = placed.map((device) => ({
      deviceId: device.id,
      name: device.name || device.sn,
      sn: device.sn,
      type: device.type ?? '',
      onlineStatus: device.onlineStatus ?? null,
      x: Number(device.floorX ?? 50),
      y: Number(device.floorY ?? 50),
    }));
    originalIds.value = new Set(points.value.map((point) => point.deviceId));
    candidates.value = unplaced;
  } finally {
    loading.value = false;
  }
}

function changeZoom(step: number): void {
  zoom.value = Math.min(400, Math.max(50, zoom.value + step));
}

function startDrag(event: PointerEvent, point: EditorPoint): void {
  if (props.readonly) {
    return;
  }
  draggingId.value = point.deviceId;
  selectedPointId.value = point.deviceId;
  const target = event.currentTarget as HTMLElement | null;
  target?.setPointerCapture?.(event.pointerId);
}

function handlePointerMove(event: PointerEvent): void {
  if (props.readonly || !draggingId.value) {
    return;
  }
  const point = points.value.find((item) => item.deviceId === draggingId.value);
  if (!point) {
    return;
  }
  const coords = resolveCoordinates(event);
  point.x = coords.x;
  point.y = coords.y;
  dirty.value = true;
}

function handlePointerUp(): void {
  draggingId.value = '';
}

function handleDragStart(event: DragEvent, device: DevicePointItem): void {
  draggingDevice.value = device;
  event.dataTransfer?.setData('text/plain', device.id);
}

function handleDrop(event: DragEvent): void {
  if (props.readonly) {
    return;
  }
  const device = draggingDevice.value;
  if (!device || !plan.value) {
    return;
  }
  const coords = resolveCoordinates(event);
  addOrMovePoint(device, coords.x, coords.y);
  draggingDevice.value = null;
}

function placeDevice(device: DevicePointItem): void {
  if (props.readonly) {
    return;
  }
  if (!plan.value) {
    ElMessage.warning('请先上传平面图');
    return;
  }
  addOrMovePoint(device, 50, 50);
}

function addOrMovePoint(device: DevicePointItem, x: number, y: number): void {
  const existing = points.value.find((point) => point.deviceId === device.id);
  if (existing) {
    existing.x = x;
    existing.y = y;
  } else {
    points.value.push({
      deviceId: device.id,
      name: device.name || device.sn,
      sn: device.sn,
      type: device.type ?? '',
      onlineStatus: device.onlineStatus ?? null,
      x,
      y,
    });
    candidates.value = candidates.value.filter((item) => item.id !== device.id);
  }
  removedIds.value = removedIds.value.filter((id) => id !== device.id);
  selectedPointId.value = device.id;
  dirty.value = true;
}

function removePoint(deviceId: string): void {
  if (props.readonly) {
    return;
  }
  const removed = points.value.find((point) => point.deviceId === deviceId);
  points.value = points.value.filter((point) => point.deviceId !== deviceId);
  if (originalIds.value.has(deviceId)) {
    removedIds.value = Array.from(new Set([...removedIds.value, deviceId]));
  }
  if (removed && !candidates.value.some((item) => item.id === deviceId)) {
    candidates.value = [...candidates.value, {
      id: removed.deviceId,
      name: removed.name,
      sn: removed.sn,
      type: removed.type,
      onlineStatus: removed.onlineStatus,
    }];
  }
  selectedPointId.value = '';
  dirty.value = true;
}

function resolveCoordinates(event: PointerEvent | DragEvent): { x: number; y: number } {
  const inner = event.currentTarget as HTMLElement | null;
  if (!inner) {
    return { x: 50, y: 50 };
  }
  const rect = inner.getBoundingClientRect();
  const x = ((event.clientX - rect.left) / rect.width) * 100;
  const y = ((event.clientY - rect.top) / rect.height) * 100;
  return {
    x: Number(Math.min(100, Math.max(0, x)).toFixed(2)),
    y: Number(Math.min(100, Math.max(0, y)).toFixed(2)),
  };
}

function typeLabel(type: string): string {
  return TYPE_LABELS[type] ?? '设';
}

async function handlePlanUpload(options: UploadRequestOptions): Promise<void> {
  if (props.readonly) {
    return;
  }
  const floor = currentFloor.value;
  if (!floor) {
    return;
  }
  if (floor.floorPlanUrl) {
    const confirmed = await ElMessageBox.confirm(
      '替换后该楼层旧点位会清空，需要重新拖点，确认替换？',
      '替换平面图',
      { type: 'warning', confirmButtonText: '替换', cancelButtonText: '取消' },
    ).then(() => true).catch(() => false);
    if (!confirmed) {
      return;
    }
  }
  const file = options.file;
  uploading.value = true;
  try {
    const dimensions = await readImageSize(file);
    const result = await uploadImage(file);
    await updateFloorPlan(floor.id, {
      planUrl: result.url,
      planWidth: dimensions.width,
      planHeight: dimensions.height,
    });
    plan.value = { url: result.url, width: dimensions.width, height: dimensions.height };
    await loadFloorData(floor.id);
    dirty.value = false;
    removedIds.value = [];
    selectedPointId.value = '';
    ElMessage.success('平面图已上传，旧点位已清空');
    emit('saved');
  } finally {
    uploading.value = false;
  }
}

function readImageSize(file: File): Promise<{ width: number; height: number }> {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file);
    const image = new Image();
    image.onload = () => {
      const size = { width: image.naturalWidth, height: image.naturalHeight };
      URL.revokeObjectURL(url);
      resolve(size);
    };
    image.onerror = () => {
      URL.revokeObjectURL(url);
      reject(new Error('图片读取失败'));
    };
    image.src = url;
  });
}

async function savePoints(): Promise<void> {
  if (props.readonly) {
    return;
  }
  const floor = currentFloor.value;
  if (!floor || !plan.value) {
    return;
  }
  saving.value = true;
  try {
    await saveFloorPoints(floor.id, {
      points: points.value.map((point) => ({
        deviceId: point.deviceId,
        x: point.x,
        y: point.y,
      })),
      removedDeviceIds: removedIds.value,
    });
    ElMessage.success('点位已保存');
    dirty.value = false;
    await loadFloorData(floor.id);
    emit('saved');
  } finally {
    saving.value = false;
  }
}

async function beforeClose(done: () => void): Promise<void> {
  if (!dirty.value) {
    done();
    return;
  }
  try {
    await ElMessageBox.confirm('有未保存的点位修改，确认关闭？', '未保存提示', { type: 'warning' });
    dirty.value = false;
    done();
  } catch {
    // 留在编辑器
  }
}
</script>

<style scoped>
.plan-layout {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr) 220px;
  gap: var(--fm-space-3);
  min-height: 560px;
}

.plan-layout.is-readonly {
  grid-template-columns: 220px minmax(0, 1fr);
}

.plan-tree,
.plan-unplaced {
  overflow: auto;
  padding: var(--fm-space-3);
  border: 1px solid var(--fm-border);
  border-radius: var(--fm-radius-md);
  background: #f9fafb;
}

.tree-group + .tree-group {
  margin-top: var(--fm-space-3);
}

.tree-building {
  margin-bottom: var(--fm-space-1);
  color: var(--fm-text-secondary);
  font-size: 12px;
  font-weight: 600;
}

.tree-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  height: 34px;
  padding: 0 10px;
  border: none;
  border-radius: var(--fm-radius-sm);
  background: transparent;
  color: var(--fm-text-secondary);
  font-size: 13px;
  cursor: pointer;
}

.tree-item:hover {
  background: #fff;
}

.tree-item.is-active {
  background: var(--fm-primary-soft);
  color: var(--fm-primary);
  font-weight: 600;
}

.plan-main {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.plan-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--fm-space-3);
  padding: 8px 12px;
  margin-bottom: var(--fm-space-2);
  border: 1px solid var(--fm-border);
  border-radius: var(--fm-radius-md);
  background: #f9fafb;
}

.plan-toolbar__left {
  display: flex;
  align-items: center;
  gap: var(--fm-space-2);
  font-size: 13px;
}

.plan-toolbar__right {
  display: flex;
  align-items: center;
  gap: var(--fm-space-2);
}

.zoom-group {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--fm-text-secondary);
  font-size: 12px;
}

.plan-stage {
  overflow: auto;
  flex: 1;
  height: 500px;
  padding: var(--fm-space-3);
  border: 1px solid var(--fm-border);
  border-radius: var(--fm-radius-md);
  background: #f1f5f9;
}

.plan-canvas {
  width: 100%;
}

.plan-inner {
  position: relative;
  width: 100%;
  overflow: hidden;
  border-radius: var(--fm-radius-sm);
  background: #fff;
  box-shadow: inset 0 0 0 1px var(--fm-border);
}

.plan-image {
  display: block;
  width: 100%;
  height: 100%;
  user-select: none;
}

.plan-point {
  position: absolute;
  z-index: 2;
  display: grid;
  place-items: center;
  width: 26px;
  height: 26px;
  border: 2px solid #fff;
  border-radius: 50%;
  background: var(--fm-success);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  box-shadow: 0 2px 6px rgb(16 24 40 / 24%);
  transform: translate(-50%, -50%);
  cursor: grab;
  touch-action: none;
}

.plan-point.is-offline {
  background: var(--fm-offline);
}

.plan-point.is-selected {
  box-shadow: 0 0 0 4px rgb(242 100 48 / 45%);
}

.plan-point.is-readonly {
  cursor: default;
}

.plan-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--fm-space-3);
  min-height: 28px;
  margin-top: var(--fm-space-2);
  font-size: 12px;
}

.selected-actions {
  display: flex;
  align-items: center;
  gap: var(--fm-space-2);
  color: var(--fm-text-secondary);
}

.unplaced-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--fm-space-2);
  font-size: 13px;
}

.unplaced-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--fm-space-2);
  padding: 8px 10px;
  margin-bottom: var(--fm-space-2);
  border: 1px solid var(--fm-border);
  border-radius: var(--fm-radius-sm);
  background: #fff;
  cursor: grab;
}

.unplaced-item__main {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.unplaced-item__name {
  overflow: hidden;
  color: var(--fm-text);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
