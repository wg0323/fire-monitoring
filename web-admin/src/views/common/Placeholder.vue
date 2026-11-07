<template>
  <section class="page-card placeholder">
    <div class="placeholder__badge">{{ badge }}</div>
    <h1 class="placeholder__title">{{ title }}</h1>
    <p class="placeholder__desc">{{ description }}</p>
    <p class="placeholder__path">{{ route.path }}</p>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { menuTitleByPath } from '@/utils/tree';

const route = useRoute();
const auth = useAuthStore();

const underConstruction = computed(() => Boolean(route.meta.underConstruction));
const title = computed(() => menuTitleByPath(auth.menus, route.path) || route.meta.title || '页面');
const badge = computed(() => (underConstruction.value ? '在建' : 'P2+'));
const description = computed(() => (underConstruction.value
  ? '功能建设中，本期暂不提供该功能。'
  : '该页面将在 P2-P4 阶段交付，当前仅保留菜单、路由与权限入口。'));
</script>

<style scoped>
.placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 420px;
  text-align: center;
}

.placeholder__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  margin-bottom: 16px;
  border-radius: 14px;
  background: var(--fm-primary-soft);
  color: var(--fm-primary);
  font-family: var(--fm-font-number);
  font-size: 14px;
  font-weight: 700;
}

.placeholder__title {
  color: var(--fm-text);
  font-size: 20px;
  font-weight: 600;
}

.placeholder__desc {
  max-width: 420px;
  margin-top: 8px;
  color: var(--fm-text-secondary);
  font-size: 13px;
  line-height: 22px;
}

.placeholder__path {
  margin-top: 16px;
  padding: 4px 10px;
  border-radius: 999px;
  background: #f9fafb;
  color: var(--fm-text-muted);
  font-family: var(--fm-font-number);
  font-size: 12px;
}
</style>
