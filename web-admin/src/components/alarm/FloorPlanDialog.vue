<template>
  <el-dialog
    :model-value="modelValue"
    :title="`楼层图 · ${alarm?.floorName || '未绑定楼层'}`"
    width="820px"
    destroy-on-close
    @update:model-value="close"
  >
    <div class="floor-plan">
      <div class="floor-plan__meta">
        <span>设备：{{ alarm?.sn || '-' }}</span>
        <span>位置：{{ alarm?.position || '-' }}</span>
        <span>楼层：{{ alarm?.floorName || '未绑定' }}</span>
      </div>
      <div v-if="planUrl && !imageFailed" class="floor-plan__canvas">
        <img :src="planUrl" alt="楼层平面图" @error="imageFailed = true" />
        <span v-if="markerStyle" class="floor-plan__marker" :style="markerStyle" title="报警设备点位" />
      </div>
      <el-empty
        v-else
        :description="imageFailed ? '楼层图加载失败' : '该设备尚未绑定楼层平面图'"
      />
      <p class="form-tip">红点为报警设备在楼层图上的绑定点位。</p>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch, type CSSProperties } from 'vue';
import type { AlarmItem } from '@/types/api';

const props = defineProps<{ modelValue: boolean; alarm: AlarmItem | null }>();
const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void;
}>();

const imageFailed = ref(false);
const planUrl = computed(() => props.alarm?.floorPlanUrl || '');
const markerStyle = computed<CSSProperties | null>(() => {
  const x = props.alarm?.floorX;
  const y = props.alarm?.floorY;
  if (x === null || x === undefined || y === null || y === undefined) {
    return null;
  }
  return { left: `${x}%`, top: `${y}%` };
});

watch(() => props.modelValue, (visible) => {
  if (visible) {
    imageFailed.value = false;
  }
}, { immediate: true });

function close(value: boolean): void {
  emit('update:modelValue', value);
}
</script>

<style scoped>
.floor-plan__meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--fm-space-4);
  margin-bottom: var(--fm-space-3);
  color: var(--fm-text-secondary);
  font-size: 13px;
}

.floor-plan__canvas {
  position: relative;
  overflow: hidden;
  border: 1px solid var(--fm-border);
  border-radius: var(--fm-radius-md);
  background: #f9fafb;
}

.floor-plan__canvas img {
  display: block;
  width: 100%;
  height: auto;
}

.floor-plan__marker {
  position: absolute;
  width: 16px;
  height: 16px;
  border: 2px solid #fff;
  border-radius: 50%;
  background: var(--fm-danger);
  box-shadow: 0 0 0 4px rgba(240, 68, 56, 0.25);
  transform: translate(-50%, -50%);
}
</style>
