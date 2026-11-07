import { computed, ref } from 'vue';
import { defineStore } from 'pinia';

export interface AppTab {
  path: string;
  title: string;
}

const COLLAPSE_KEY = 'fm-admin-sidebar-collapsed';
const MAX_TABS = 8;

export const useAppStore = defineStore('app', () => {
  const collapsed = ref(localStorage.getItem(COLLAPSE_KEY) === '1');
  const tabs = ref<AppTab[]>([]);

  const tabPaths = computed(() => tabs.value.map((tab) => tab.path));

  function toggleSidebar(): void {
    collapsed.value = !collapsed.value;
    localStorage.setItem(COLLAPSE_KEY, collapsed.value ? '1' : '0');
  }

  function addTab(path: string, title: string): void {
    if (!path || path === '/login') {
      return;
    }
    const existing = tabs.value.find((tab) => tab.path === path);
    if (existing) {
      existing.title = title || existing.title;
      return;
    }
    tabs.value.push({ path, title: title || '页面' });
    if (tabs.value.length > MAX_TABS) {
      tabs.value.shift();
    }
  }

  function removeTab(path: string): void {
    tabs.value = tabs.value.filter((tab) => tab.path !== path);
  }

  function resetTabs(): void {
    tabs.value = [];
  }

  return {
    collapsed,
    tabs,
    tabPaths,
    toggleSidebar,
    addTab,
    removeTab,
    resetTabs,
  };
});
