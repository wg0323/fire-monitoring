<template>
  <aside class="sidebar" :class="{ 'is-collapsed': app.collapsed }">
    <div class="brand">
      <span class="brand-logo">FM</span>
      <span v-if="!app.collapsed" class="brand-name">消防监控平台</span>
    </div>

    <el-scrollbar class="menu-scroll">
      <el-menu
        :default-active="route.path"
        :collapse="app.collapsed"
        :collapse-transition="false"
        background-color="transparent"
        text-color="#cbd5e1"
        active-text-color="#ffffff"
        router
      >
        <template v-for="menu in visibleMenus" :key="menu.id">
          <el-sub-menu v-if="isDirectory(menu)" :index="menu.menuKey">
            <template #title>
              <FmMenuIcon :name="menu.menuKey" />
              <span>{{ menu.name }}</span>
            </template>
            <el-menu-item
              v-for="child in pageChildren(menu)"
              :key="child.id"
              :index="child.path as string"
            >
              <FmMenuIcon :name="child.menuKey" />
              <span>{{ child.name }}</span>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="menu.path as string">
            <FmMenuIcon :name="menu.menuKey" />
            <span>{{ menu.name }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-scrollbar>

    <div v-if="!app.collapsed" class="sidebar-foot">值守 · 处置 · 巡查</div>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import FmMenuIcon from '@/components/FmMenuIcon.vue';
import { useAppStore } from '@/stores/app';
import { useAuthStore } from '@/stores/auth';
import type { MenuNode } from '@/types/api';

const route = useRoute();
const app = useAppStore();
const auth = useAuthStore();

const visibleMenus = computed(() => auth.menus.filter((menu) => {
  if (menu.type === 2 || menu.status !== 1) {
    return false;
  }
  if (menu.type === 1) {
    return Boolean(menu.path);
  }
  return pageChildren(menu).length > 0;
}));

function isDirectory(menu: MenuNode): boolean {
  return menu.type === 0 && pageChildren(menu).length > 0;
}

function pageChildren(menu: MenuNode): MenuNode[] {
  return (menu.children ?? []).filter((child) => child.type === 1 && child.path && child.status === 1);
}
</script>

<style scoped>
.sidebar {
  display: flex;
  flex: none;
  flex-direction: column;
  width: 232px;
  height: 100vh;
  overflow: hidden;
  background: var(--fm-navy-900);
  transition: width 0.2s ease;
}

.sidebar.is-collapsed {
  width: 64px;
}

.brand {
  display: flex;
  flex: none;
  align-items: center;
  gap: 10px;
  height: 56px;
  padding: 0 16px;
  border-bottom: 1px solid rgb(255 255 255 / 8%);
}

.brand-logo {
  display: grid;
  flex: none;
  place-items: center;
  width: 30px;
  height: 30px;
  border: 1px solid rgb(56 189 248 / 40%);
  border-radius: 8px;
  color: #22d3ee;
  font-family: var(--fm-font-number);
  font-size: 13px;
  font-weight: 700;
}

.brand-name {
  overflow: hidden;
  color: #e6f1ff;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.04em;
  white-space: nowrap;
}

.menu-scroll {
  flex: 1;
  padding: 8px 0;
}

.sidebar-foot {
  flex: none;
  padding: 12px 20px 18px;
  color: #59718a;
  font-size: 12px;
}

:deep(.el-menu) {
  border-right: none;
}

:deep(.el-menu-item),
:deep(.el-sub-menu__title) {
  height: 40px;
  margin: 2px 8px;
  border-radius: 8px;
  line-height: 40px;
  font-size: 14px;
}

:deep(.el-menu-item:hover),
:deep(.el-sub-menu__title:hover) {
  background: var(--fm-navy-800);
  color: #ffffff;
}

:deep(.el-menu-item.is-active) {
  position: relative;
  background: var(--fm-navy-800);
  color: #ffffff;
}

:deep(.el-menu-item.is-active)::before {
  position: absolute;
  top: 10px;
  bottom: 10px;
  left: 0;
  width: 3px;
  border-radius: 2px;
  background: var(--fm-accent);
  content: '';
}

:deep(.el-sub-menu .el-menu-item) {
  min-width: auto;
  padding-left: 44px !important;
}

.sidebar.is-collapsed :deep(.el-menu-item),
.sidebar.is-collapsed :deep(.el-sub-menu__title) {
  justify-content: center;
  margin: 2px 6px;
  padding: 0 !important;
}

.sidebar.is-collapsed :deep(.el-sub-menu .el-menu-item) {
  padding-left: 0 !important;
}
</style>
