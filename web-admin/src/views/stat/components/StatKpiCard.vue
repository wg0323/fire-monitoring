<template>
  <div class="stat-kpi" :class="`is-${tone}`">
    <span class="stat-kpi-label">{{ label }}</span>
    <strong class="stat-kpi-value">{{ display }}</strong>
    <span v-if="hint" class="stat-kpi-hint">{{ hint }}</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = withDefaults(
  defineProps<{
    label: string;
    value: number | string;
    suffix?: string;
    hint?: string;
    tone?: 'primary' | 'danger' | 'warning' | 'success' | 'offline';
  }>(),
  {
    suffix: '',
    hint: '',
    tone: 'primary',
  },
);

const display = computed(() => `${props.value}${props.suffix}`);
</script>

<style scoped>
.stat-kpi {
  display: flex;
  flex-direction: column;
  gap: var(--fm-space-1);
  padding: var(--fm-space-4) var(--fm-space-5);
  background: var(--fm-card-bg);
  border: 1px solid var(--fm-border);
  border-radius: var(--fm-radius-md);
  box-shadow: var(--fm-shadow-card);
}

.stat-kpi-label {
  color: var(--fm-text-secondary);
  font-size: 13px;
}

.stat-kpi-value {
  color: var(--fm-primary);
  font-family: var(--fm-font-number);
  font-size: 26px;
  font-weight: 600;
  line-height: 34px;
}

.stat-kpi-hint {
  color: var(--fm-text-muted);
  font-size: 12px;
}

.stat-kpi.is-danger .stat-kpi-value {
  color: var(--fm-danger);
}

.stat-kpi.is-warning .stat-kpi-value {
  color: var(--fm-warning);
}

.stat-kpi.is-success .stat-kpi-value {
  color: var(--fm-success);
}

.stat-kpi.is-offline .stat-kpi-value {
  color: var(--fm-offline);
}
</style>
