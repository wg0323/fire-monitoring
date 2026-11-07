<template>
  <svg
    class="fm-menu-icon"
    width="18"
    height="18"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    stroke-width="1.6"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
  >
    <template v-if="group === 'screen'">
      <rect x="3" y="4" width="7" height="7" rx="1" />
      <rect x="14" y="4" width="7" height="7" rx="1" />
      <rect x="3" y="14" width="7" height="7" rx="1" />
      <rect x="14" y="14" width="7" height="7" rx="1" />
    </template>
    <template v-else-if="group === 'alert'">
      <path d="M18 8a6 6 0 0 0-12 0c0 7-3 8-3 8h18s-3-1-3-8" />
      <path d="M13.7 21a2 2 0 0 1-3.4 0" />
    </template>
    <template v-else-if="group === 'trace'">
      <path d="M4 5h16M4 12h16M4 19h10" />
      <circle cx="19" cy="19" r="2" />
    </template>
    <template v-else-if="group === 'chart'">
      <path d="M4 19V5M4 19h16" />
      <path d="M8 15l3-4 3 2 4-6" />
    </template>
    <template v-else-if="group === 'system'">
      <path d="M12 3l7 3v5c0 4-3 7-7 9-4-2-7-5-7-9V6l7-3z" />
      <path d="M9 12l2 2 4-4" />
    </template>
    <template v-else>
      <circle cx="12" cy="12" r="8" />
      <path d="M12 8v4l3 2" />
    </template>
  </svg>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{ name: string }>();

const group = computed(() => {
  const key = props.name || '';
  if (key.startsWith('screen')) {
    return 'screen';
  }
  if (key.startsWith('duty')) {
    return 'alert';
  }
  if (key.startsWith('trace') || key.startsWith('workorder') || key.startsWith('service')
    || key.startsWith('archive')) {
    return 'trace';
  }
  if (key.startsWith('stat') || key.startsWith('report')) {
    return 'chart';
  }
  if (key.startsWith('system') || key.startsWith('dict')) {
    return 'system';
  }
  return 'default';
});
</script>

<style scoped>
.fm-menu-icon {
  flex: none;
  width: 18px;
  height: 18px;
  margin-right: 10px;
}

.el-menu--collapse .fm-menu-icon {
  margin-right: 0;
}
</style>
