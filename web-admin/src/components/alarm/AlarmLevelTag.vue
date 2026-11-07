<template>
  <span class="alarm-tag" :class="tone">{{ label }}</span>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { alarmLevelLabel, alarmLevelTone } from '@/utils/format';

const props = defineProps<{ level?: number | null }>();

const label = computed(() => alarmLevelLabel(props.level));
const tone = computed(() => alarmLevelTone(props.level));
</script>

<style scoped>
.alarm-tag {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  line-height: 18px;
  white-space: nowrap;
}

.is-danger {
  color: #b42318;
  background: var(--fm-danger-soft);
}

.is-warning {
  color: #b54708;
  background: var(--fm-warning-soft);
}

.is-fault {
  color: #9a3412;
  background: var(--fm-accent-soft);
}

.is-muted {
  color: #667085;
  background: #f2f4f7;
}
</style>
