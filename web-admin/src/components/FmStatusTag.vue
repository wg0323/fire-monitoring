<template>
  <span class="fm-status" :class="tone">
    <i class="fm-status__dot" aria-hidden="true" />
    {{ text }}
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = withDefaults(defineProps<{ status?: number | null; onText?: string; offText?: string }>(), {
  status: 0,
  onText: '启用',
  offText: '禁用',
});

const tone = computed(() => (props.status === 1 ? 'is-success' : 'is-offline'));
const text = computed(() => (props.status === 1 ? props.onText : props.offText));
</script>

<style scoped>
.fm-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  line-height: 18px;
}

.fm-status__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.is-success {
  color: #027a48;
  background: var(--fm-success-soft);
}

.is-offline {
  color: #667085;
  background: #f2f4f7;
}
</style>
