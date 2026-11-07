<template>
  <div v-if="app.tabs.length" class="app-tabs">
    <button
      v-for="tab in app.tabs"
      :key="tab.path"
      class="app-tab"
      :class="{ 'is-active': tab.path === route.path }"
      type="button"
      @click="go(tab.path)"
    >
      <span class="app-tab__title">{{ tab.title }}</span>
      <span
        class="app-tab__close"
        role="button"
        tabindex="0"
        :aria-label="`关闭${tab.title}`"
        @click.stop="close(tab.path)"
        @keydown.enter.stop.prevent="close(tab.path)"
        @keydown.space.stop.prevent="close(tab.path)"
      >×</span>
    </button>
  </div>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router';
import { useAppStore } from '@/stores/app';
import { useAuthStore } from '@/stores/auth';

const route = useRoute();
const router = useRouter();
const app = useAppStore();
const auth = useAuthStore();

async function go(path: string): Promise<void> {
  if (path !== route.path) {
    await router.push(path);
  }
}

async function close(path: string): Promise<void> {
  const index = app.tabs.findIndex((tab) => tab.path === path);
  app.removeTab(path);
  if (path !== route.path) {
    return;
  }
  const next = app.tabs[index] ?? app.tabs[index - 1];
  await router.push(next ? next.path : auth.landingPath());
}
</script>

<style scoped>
.app-tabs {
  display: flex;
  flex: none;
  gap: 4px;
  height: 40px;
  padding: 0 16px;
  overflow-x: auto;
  background: #ffffff;
  border-bottom: 1px solid var(--fm-border);
}

.app-tab {
  position: relative;
  display: inline-flex;
  flex: none;
  align-items: center;
  gap: 6px;
  height: 40px;
  padding: 0 10px;
  color: var(--fm-text-secondary);
  background: transparent;
  border: none;
  font-size: 13px;
  cursor: pointer;
}

.app-tab:hover {
  color: var(--fm-primary);
}

.app-tab.is-active {
  color: var(--fm-primary);
  font-weight: 500;
}

.app-tab.is-active::after {
  position: absolute;
  right: 8px;
  bottom: 0;
  left: 8px;
  height: 2px;
  border-radius: 2px;
  background: var(--fm-accent);
  content: '';
}

.app-tab__close {
  display: grid;
  place-items: center;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  color: var(--fm-text-muted);
  font-size: 13px;
  line-height: 1;
}

.app-tab__close:hover {
  background: #eef2f6;
  color: var(--fm-text);
}
</style>
