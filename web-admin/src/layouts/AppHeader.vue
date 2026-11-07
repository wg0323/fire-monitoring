<template>
  <header class="app-header">
    <div class="header-left">
      <button class="icon-button" type="button" aria-label="折叠菜单" @click="app.toggleSidebar()">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
          <path d="M4 6h16M4 12h16M4 18h16" stroke-linecap="round" />
        </svg>
      </button>
      <el-breadcrumb separator="/">
        <el-breadcrumb-item v-for="crumb in crumbs" :key="crumb.name">
          {{ crumb.name }}
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="header-right">
      <span class="tenant-name">{{ auth.tenantName }}</span>
      <button
        class="icon-button bell"
        type="button"
        aria-label="报警提醒（暂未开放）"
        title="报警提醒暂未开放"
        aria-disabled="true"
        disabled
      >
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" aria-hidden="true">
          <path d="M18 8a6 6 0 0 0-12 0c0 7-3 8-3 8h18s-3-1-3-8" stroke-linecap="round" stroke-linejoin="round" />
          <path d="M13.7 21a2 2 0 0 1-3.4 0" stroke-linecap="round" />
        </svg>
      </button>
      <el-dropdown trigger="click" @command="handleCommand">
        <button class="user-trigger" type="button">
          <span class="user-avatar">{{ avatarText }}</span>
          <span class="user-meta">
            <span class="user-name">{{ auth.displayName }}</span>
            <span class="user-role">{{ roleLabel(auth.roles[0]) }}</span>
          </span>
        </button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAppStore } from '@/stores/app';
import { useAuthStore } from '@/stores/auth';
import { roleLabel } from '@/utils/format';
import { findMenuTrail } from '@/utils/tree';
import type { MenuNode } from '@/types/api';

const route = useRoute();
const router = useRouter();
const app = useAppStore();
const auth = useAuthStore();

const crumbs = computed<MenuNode[]>(() => {
  const trail = findMenuTrail(auth.menus, route.path);
  if (trail.length) {
    return trail;
  }
  return [{
    id: 'current',
    tenantId: '',
    parentId: '',
    name: route.meta.title || '页面',
    menuKey: '',
    type: 1,
    path: route.path,
    perms: null,
    seq: 0,
    status: 1,
    platformOnly: 0,
    children: [],
  }];
});

const avatarText = computed(() => (auth.displayName || 'FM').slice(0, 1));

async function handleCommand(command: string): Promise<void> {
  if (command !== 'logout') {
    return;
  }
  await auth.logout();
  app.resetTabs();
  await router.replace('/login');
}
</script>

<style scoped>
.app-header {
  display: flex;
  flex: none;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 20px;
  background: #ffffff;
  border-bottom: 1px solid var(--fm-border);
}

.header-left,
.header-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.header-left {
  min-width: 0;
}

.header-right {
  flex: none;
}

.tenant-name {
  color: var(--fm-text-secondary);
  font-size: 13px;
}

.bell {
  position: relative;
}

.icon-button:disabled,
.icon-button:disabled:hover {
  color: var(--fm-text-muted);
  background: transparent;
  cursor: not-allowed;
  opacity: 0.5;
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 6px;
  background: transparent;
  border: none;
  border-radius: var(--fm-radius-md);
  cursor: pointer;
}

.user-trigger:hover {
  background: var(--fm-primary-soft);
}

.user-avatar {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: var(--fm-primary);
  color: #ffffff;
  font-size: 13px;
  font-weight: 600;
}

.user-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  line-height: 1.2;
}

.user-name {
  color: var(--fm-text);
  font-size: 13px;
  font-weight: 500;
}

.user-role {
  color: var(--fm-text-muted);
  font-size: 11px;
}
</style>
