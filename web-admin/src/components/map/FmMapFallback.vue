<template>
  <div class="fm-map-fallback" :style="{ height }">
    <svg class="fm-map-fallback__svg" viewBox="0 0 100 100" preserveAspectRatio="none" aria-hidden="true">
      <defs>
        <pattern id="fm-map-grid" width="8" height="8" patternUnits="userSpaceOnUse">
          <path d="M 8 0 L 0 0 0 8" fill="none" stroke="currentColor" stroke-opacity="0.12" stroke-width="0.2" />
        </pattern>
      </defs>
      <rect width="100" height="100" fill="url(#fm-map-grid)" />
    </svg>
    <div
      v-for="point in placed"
      :key="point.id"
      class="fm-map-fallback__point"
      :class="`is-${point.status ?? 'normal'}`"
      :style="{ left: `${point.left}%`, top: `${point.top}%` }"
      :title="point.name"
    >
      <svg class="fm-map-fallback__icon" viewBox="0 0 24 24" aria-hidden="true">
        <use :href="`#fm-type-${merchantTypeSymbol(point.typeCode)}`" />
      </svg>
      <span class="fm-map-fallback__label">{{ point.name }}</span>
    </div>
    <div v-if="placed.length === 0" class="fm-map-fallback__empty">暂无地图点位</div>
    <div class="fm-map-fallback__note">{{ note }}</div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { merchantTypeSymbol } from './merchantTypeIcon';
import type { MapPoint } from './types';

const props = withDefaults(
  defineProps<{
    points: MapPoint[];
    height?: string;
    note?: string;
  }>(),
  {
    height: '100%',
    note: '高德地图不可用，当前为示意底图',
  },
);

const placed = computed(() => {
  if (props.points.length === 0) {
    return [];
  }
  const lngs = props.points.map((point) => point.lng);
  const lats = props.points.map((point) => point.lat);
  const minLng = Math.min(...lngs);
  const maxLng = Math.max(...lngs);
  const minLat = Math.min(...lats);
  const maxLat = Math.max(...lats);
  const lngSpan = maxLng - minLng || 0.01;
  const latSpan = maxLat - minLat || 0.01;
  return props.points.map((point) => ({
    ...point,
    left: 8 + ((point.lng - minLng) / lngSpan) * 84,
    top: 8 + ((maxLat - point.lat) / latSpan) * 84,
  }));
});
</script>

<style scoped>
.fm-map-fallback {
  position: relative;
  overflow: hidden;
  border-radius: var(--fm-radius-md);
  background: linear-gradient(180deg, #f8fafc, #eef2f7);
  color: #17395a;
}

.fm-map-fallback__svg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.fm-map-fallback__point {
  position: absolute;
  display: grid;
  place-items: center;
  width: 20px;
  height: 20px;
  border: 2px solid #fff;
  border-radius: 50%;
  background: var(--fm-success);
  box-shadow: 0 2px 6px rgb(16 24 40 / 24%);
  color: #fff;
  transform: translate(-50%, -50%);
}

.fm-map-fallback__icon {
  width: 12px;
  height: 12px;
}

.fm-map-fallback__point.is-fire {
  background: var(--fm-danger);
}

.fm-map-fallback__point.is-fault,
.fm-map-fallback__point.is-warning {
  background: var(--fm-warning);
}

.fm-map-fallback__point.is-offline {
  background: var(--fm-offline);
}

.fm-map-fallback__label {
  position: absolute;
  top: 22px;
  left: 50%;
  padding: 1px 5px;
  border-radius: 4px;
  background: rgb(16 24 40 / 72%);
  color: #fff;
  font-size: 10px;
  white-space: nowrap;
  transform: translateX(-50%);
}

.fm-map-fallback__empty {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  color: var(--fm-text-muted);
  font-size: 13px;
}

.fm-map-fallback__note {
  position: absolute;
  right: 12px;
  bottom: 10px;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgb(255 255 255 / 78%);
  color: var(--fm-text-secondary);
  font-size: 11px;
}
</style>
