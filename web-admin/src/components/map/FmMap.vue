<template>
  <div class="fm-map" :style="{ height }">
    <svg class="fm-map__sprite" aria-hidden="true" focusable="false">
      <symbol id="fm-type-food" viewBox="0 0 24 24">
        <path fill="currentColor" d="M11 9H9V2H7v7H5V2H3v7c0 2.12 1.66 3.84 3.75 3.97V22h2.5v-9.03C11.34 12.84 13 11.12 13 9V2h-2v7zm5-3v8h2.5v8H21V2c-2.76 0-5 2.24-5 4z" />
      </symbol>
      <symbol id="fm-type-shop" viewBox="0 0 24 24">
        <path fill="currentColor" d="M18 6h-2c0-2.21-1.79-4-4-4S8 3.79 8 6H6c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm-6-2c1.1 0 2 .9 2 2h-4c0-1.1.9-2 2-2zm6 16H6V8h2v2c0 .55.45 1 1 1s1-.45 1-1V8h4v2c0 .55.45 1 1 1s1-.45 1-1V8h2v12z" />
      </symbol>
      <symbol id="fm-type-hotel" viewBox="0 0 24 24">
        <path fill="currentColor" d="M7 13c1.66 0 3-1.34 3-3S8.66 7 7 7s-3 1.34-3 3 1.34 3 3 3zm12-6h-8v7H3V5H1v15h2v-3h18v3h2v-9c0-2.21-1.79-4-4-4z" />
      </symbol>
      <symbol id="fm-type-school" viewBox="0 0 24 24">
        <path fill="currentColor" d="M5 13.18v4L12 21l7-3.82v-4L12 17l-7-3.82zM12 3L1 9l11 6 9-4.91V17h2V9L12 3z" />
      </symbol>
      <symbol id="fm-type-hospital" viewBox="0 0 24 24">
        <path fill="currentColor" d="M10 3h4v7h7v4h-7v7h-4v-7H3v-4h7z" />
      </symbol>
      <symbol id="fm-type-elderly" viewBox="0 0 24 24">
        <path fill="currentColor" d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z" />
      </symbol>
      <symbol id="fm-type-other" viewBox="0 0 24 24">
        <path fill="currentColor" d="M6 10c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm12 0c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm-6 0c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z" />
      </symbol>
    </svg>

    <div v-show="!failed" ref="containerRef" class="fm-map__canvas" />

    <FmMapFallback
      v-if="failed"
      :points="points"
      height="100%"
      :note="errorText"
    />

    <div v-if="loading" class="fm-map__loading">地图加载中…</div>

    <div v-if="!failed && !pickMode && searchable" class="fm-map__search">
      <el-input
        v-model="keyword"
        size="small"
        placeholder="搜索商户"
        clearable
        aria-label="搜索商户"
      />
      <div v-if="keyword && filtered.length > 0" class="fm-map__results">
        <button
          v-for="point in filtered"
          :key="point.id"
          type="button"
          class="fm-map__result"
          @click="focusPoint(point)"
        >
          <span>{{ point.name }}</span>
          <span class="fm-map__result-sub">{{ point.subtitle }}</span>
        </button>
      </div>
      <div v-else-if="keyword" class="fm-map__results fm-map__results--empty">未找到匹配商户</div>
    </div>

    <div v-if="!failed && pickMode" class="fm-map__tip">点击地图选择商户位置</div>

    <div v-if="$slots.overlay" class="fm-map__overlay">
      <slot name="overlay" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { loadAmap } from '@/utils/amap';
import { formatDateTime } from '@/utils/format';
import { safeUrl } from '@/utils/url';
import type { AmapGeocoder, AmapLngLat, AmapMap, AmapMarker, AmapNamespace } from '@/types/amap';
import FmMapFallback from './FmMapFallback.vue';
import { merchantTypeIconHtml } from './merchantTypeIcon';
import type { MapPick, MapPoint, MapPointStatus } from './types';

const props = withDefaults(
  defineProps<{
    points: MapPoint[];
    height?: string;
    pickMode?: boolean;
    pickValue?: { lng: number; lat: number } | null;
    dark?: boolean;
    searchable?: boolean;
  }>(),
  {
    height: '420px',
    pickMode: false,
    pickValue: null,
    dark: false,
    searchable: true,
  },
);

const emit = defineEmits<{
  (event: 'select', point: MapPoint): void;
  (event: 'pick', value: MapPick): void;
}>();

const DEFAULT_CENTER: [number, number] = [97.1725, 31.1425];
const STATUS_COLORS: Record<MapPointStatus, string> = {
  fire: '#d92d20',
  fault: '#f79009',
  warning: '#f79009',
  offline: '#98a2b3',
  normal: '#12b76a',
};

const containerRef = ref<HTMLElement | null>(null);
const loading = ref(true);
const failed = ref(false);
const errorText = ref('高德地图不可用，当前为示意底图');
const keyword = ref('');

let amap: AmapNamespace | null = null;
let map: AmapMap | null = null;
let markers: AmapMarker[] = [];
let pickMarker: AmapMarker | null = null;
let geocoder: AmapGeocoder | null = null;

const filtered = computed(() => {
  const text = keyword.value.trim();
  if (!text) {
    return [];
  }
  return props.points.filter((point) => point.name.includes(text)).slice(0, 8);
});

async function initMap(): Promise<void> {
  if (!containerRef.value) {
    return;
  }
  try {
    amap = await loadAmap();
    map = new amap.Map(containerRef.value, {
      zoom: 12,
      center: initialCenter(),
      mapStyle: props.dark ? 'amap://styles/dark' : 'amap://styles/normal',
      viewMode: '2D',
      resizeEnable: true,
    });
    if (props.pickMode) {
      map.on('click', handleMapClick);
      renderPickMarker();
    } else {
      renderMarkers();
    }
    loading.value = false;
  } catch (error) {
    loading.value = false;
    failed.value = true;
    errorText.value = error instanceof Error && error.message === 'AMAP_KEY_MISSING'
      ? '未配置高德地图 Key，当前为示意底图'
      : '高德地图加载失败，当前为示意底图';
  }
}

function initialCenter(): [number, number] {
  if (props.pickValue) {
    return [props.pickValue.lng, props.pickValue.lat];
  }
  if (props.points.length === 0) {
    return DEFAULT_CENTER;
  }
  const lng = props.points.reduce((sum, point) => sum + point.lng, 0) / props.points.length;
  const lat = props.points.reduce((sum, point) => sum + point.lat, 0) / props.points.length;
  return [lng, lat];
}

function renderMarkers(): void {
  const namespace = amap;
  const currentMap = map;
  if (!namespace || !currentMap) {
    return;
  }
  if (markers.length > 0) {
    currentMap.remove(markers);
    markers = [];
  }
  if (props.points.length === 0) {
    return;
  }
  const infoWindow = new namespace.InfoWindow({ offset: new namespace.Pixel(0, -14) });
  markers = props.points.map((point) => {
    const marker = new namespace.Marker({
      position: [point.lng, point.lat],
      content: markerContent(point),
      offset: new namespace.Pixel(-11, -11),
      title: point.name,
    });
    marker.on('click', () => {
      infoWindow.setContent(infoContent(point));
      infoWindow.open(currentMap, marker.getPosition());
      emit('select', point);
    });
    return marker;
  });
  currentMap.add(markers);
  currentMap.setFitView(markers, false, [48, 48, 48, 48], 15);
}

function renderPickMarker(): void {
  const namespace = amap;
  const currentMap = map;
  if (!namespace || !currentMap || !props.pickValue) {
    return;
  }
  const position: [number, number] = [props.pickValue.lng, props.pickValue.lat];
  if (pickMarker) {
    currentMap.remove([pickMarker]);
    pickMarker = null;
  }
  pickMarker = new namespace.Marker({
    position,
    content: '<div class="fm-map-pin"></div>',
    offset: new namespace.Pixel(-10, -20),
  });
  currentMap.add([pickMarker]);
  currentMap.setZoomAndCenter(15, position);
}

function handleMapClick(event: { lnglat: AmapLngLat }): void {
  const { lng, lat } = event.lnglat;
  const namespace = amap;
  if (!namespace) {
    emit('pick', { lng, lat });
    return;
  }
  if (!geocoder) {
    geocoder = new namespace.Geocoder();
  }
  geocoder.getAddress([lng, lat], (status, result) => {
    const address = status === 'complete' ? result.regeocode?.formattedAddress : undefined;
    emit('pick', { lng, lat, address });
  });
}

function focusPoint(point: MapPoint): void {
  if (!map) {
    return;
  }
  map.setZoomAndCenter(15, [point.lng, point.lat]);
  emit('select', point);
  keyword.value = '';
}

function markerContent(point: MapPoint): string {
  const color = STATUS_COLORS[point.status ?? 'normal'];
  return `<div class="fm-map-marker" style="--fm-marker-color:${color}">`
    + `${merchantTypeIconHtml(point.typeCode)}</div>`;
}

function infoContent(point: MapPoint): string {
  const title = `<div class="fm-map-info__title">${escapeHtml(point.name)}</div>`;
  const subtitle = point.subtitle
    ? `<div class="fm-map-info__sub">${escapeHtml(point.subtitle)}</div>`
    : '';
  const rows = infoRows(point);
  if (rows.length === 0) {
    return `<div class="fm-map-info">${title}${subtitle}</div>`;
  }
  const picUrl = safeImageUrl(point.picUrl);
  const picture = picUrl
    ? `<img class="fm-map-info__pic" src="${escapeHtml(picUrl)}" alt="${escapeHtml(point.name)}" />`
    : '';
  return `<div class="fm-map-info">
    <div class="fm-map-info__head">${picture}<div class="fm-map-info__head-text">${title}${subtitle}</div></div>
    <div class="fm-map-info__rows">${rows.join('')}</div>
  </div>`;
}

function infoRows(point: MapPoint): string[] {
  const rows: string[] = [];
  appendInfoRow(rows, '商户类型', point.typeName);
  appendInfoRow(rows, '报警时间', formatDateTime(point.alarmTime));
  appendInfoRow(rows, '联系人', point.contact);
  appendInfoRow(rows, '电话', point.phone);
  return rows;
}

function appendInfoRow(rows: string[], label: string, value?: string | null): void {
  if (!value || value === '-') {
    return;
  }
  rows.push(`<div class="fm-map-info__row"><span class="fm-map-info__label">${label}</span>`
    + `<span class="fm-map-info__value">${escapeHtml(value)}</span></div>`);
}

function safeImageUrl(value?: string | null): string {
  // 复用统一白名单：http/https 与站内相对路径，拒绝 //host 协议相对及危险协议
  return safeUrl(value) ?? '';
}

function escapeHtml(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

watch(
  () => props.points,
  async () => {
    if (!map) {
      return;
    }
    await nextTick();
    renderMarkers();
  },
  { deep: true },
);

watch(
  () => props.pickValue,
  () => {
    renderPickMarker();
  },
  { deep: true },
);

onMounted(() => {
  initMap();
});

onBeforeUnmount(() => {
  if (map) {
    map.destroy();
    map = null;
  }
});
</script>

<style>
.fm-map-marker {
  display: grid;
  place-items: center;
  width: 22px;
  height: 22px;
  border: 2px solid rgb(255 255 255 / 85%);
  border-radius: 50%;
  background: var(--fm-marker-color, #12b76a);
  box-shadow: 0 2px 6px rgb(16 24 40 / 28%);
  color: #fff;
  cursor: pointer;
}

.fm-map-marker__icon {
  width: 13px;
  height: 13px;
}

.fm-map-marker:hover {
  transform: scale(1.2);
}

.fm-map-pin {
  width: 20px;
  height: 20px;
  border: 2px solid #fff;
  border-radius: 50% 50% 50% 0;
  background: var(--fm-accent, #f26430);
  box-shadow: 0 2px 8px rgb(16 24 40 / 30%);
  transform: rotate(-45deg);
}

.fm-map-info {
  min-width: 220px;
  max-width: 300px;
  padding: 2px 4px;
}

.fm-map-info__head {
  display: flex;
  gap: 10px;
  align-items: center;
}

.fm-map-info__head-text {
  min-width: 0;
}

.fm-map-info__pic {
  flex: none;
  width: 76px;
  height: 56px;
  border-radius: var(--fm-radius-sm);
  background: var(--fm-page-bg);
  object-fit: cover;
}

.fm-map-info__title {
  font-size: 13px;
  font-weight: 600;
  color: var(--fm-text);
}

.fm-map-info__sub {
  margin-top: 2px;
  font-size: 12px;
  color: var(--fm-text-secondary);
}

.fm-map-info__rows {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid var(--fm-border);
}

.fm-map-info__row {
  display: flex;
  gap: 8px;
  font-size: 12px;
  line-height: 20px;
}

.fm-map-info__label {
  flex: none;
  width: 56px;
  color: var(--fm-text-muted);
}

.fm-map-info__value {
  min-width: 0;
  color: var(--fm-text-secondary);
  word-break: break-all;
}
</style>

<style scoped>
.fm-map {
  position: relative;
  overflow: hidden;
  border-radius: var(--fm-radius-md);
  background: #eef2f7;
}

.fm-map__canvas {
  width: 100%;
  height: 100%;
}

.fm-map__sprite {
  position: absolute;
  width: 0;
  height: 0;
  overflow: hidden;
}

.fm-map__loading {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  color: var(--fm-text-muted);
  font-size: 13px;
  background: rgb(248 250 252 / 72%);
}

.fm-map__search {
  position: absolute;
  top: 12px;
  left: 12px;
  width: 260px;
}

.fm-map__results {
  margin-top: 6px;
  overflow: hidden;
  max-height: 220px;
  overflow-y: auto;
  background: #fff;
  border: 1px solid var(--fm-border);
  border-radius: var(--fm-radius-sm);
  box-shadow: var(--fm-shadow-pop);
}

.fm-map__results--empty {
  padding: 10px 12px;
  color: var(--fm-text-muted);
  font-size: 12px;
}

.fm-map__result {
  display: flex;
  flex-direction: column;
  gap: 2px;
  width: 100%;
  padding: 8px 12px;
  border: none;
  background: transparent;
  color: var(--fm-text);
  font-size: 13px;
  text-align: left;
  cursor: pointer;
}

.fm-map__result:hover {
  background: var(--fm-primary-soft);
}

.fm-map__result-sub {
  color: var(--fm-text-muted);
  font-size: 11px;
}

.fm-map__tip {
  position: absolute;
  right: 12px;
  bottom: 12px;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgb(16 24 40 / 72%);
  color: #fff;
  font-size: 12px;
}

.fm-map__overlay {
  position: absolute;
  top: 12px;
  left: 12px;
  z-index: 2;
}
</style>
