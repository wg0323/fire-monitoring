<template>
  <div class="screen-kpi" :class="`screen-kpi--${tone}`">
    <div class="screen-kpi__label">{{ label }}</div>
    <div class="screen-kpi__value">
      {{ displayValue }}<span v-if="unit" class="screen-kpi__unit">{{ unit }}</span>
    </div>
    <div v-if="sub" class="screen-kpi__sub">{{ sub }}</div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = withDefaults(
  defineProps<{
    label: string;
    value: number | string;
    unit?: string;
    sub?: string;
    tone?: 'cyan' | 'orange' | 'danger' | 'success' | 'offline';
  }>(),
  {
    unit: '',
    sub: '',
    tone: 'cyan',
  },
);

const displayValue = computed(() => (typeof props.value === 'number' ? String(props.value) : props.value));
</script>

<style scoped>
.screen-kpi {
  padding: 12px 14px;
  background: rgb(8 22 38 / 72%);
  border: 1px solid rgb(56 189 248 / 12%);
  border-radius: 3px;
}

.screen-kpi__label {
  color: var(--screen-text-secondary);
  font-size: 12px;
}

.screen-kpi__value {
  margin-top: 4px;
  color: var(--kpi-color, var(--screen-cyan));
  font-family: var(--fm-font-number, 'DIN', sans-serif);
  font-size: 30px;
  font-weight: 700;
  line-height: 36px;
}

.screen-kpi__unit {
  margin-left: 4px;
  color: var(--screen-text-secondary);
  font-size: 12px;
  font-weight: 400;
}

.screen-kpi__sub {
  margin-top: 2px;
  color: var(--screen-text-secondary);
  font-size: 12px;
}

.screen-kpi--orange {
  --kpi-color: var(--screen-orange);
}

.screen-kpi--danger {
  --kpi-color: #ff6b5e;
}

.screen-kpi--success {
  --kpi-color: #34d399;
}

.screen-kpi--offline {
  --kpi-color: #cbd5e1;
}
</style>
