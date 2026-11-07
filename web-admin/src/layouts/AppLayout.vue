<template>
  <div class="app-layout">
    <AppSidebar />
    <div class="app-body">
      <AppHeader />
      <AppTabs />
      <main class="app-content">
        <router-view v-slot="{ Component }">
          <!-- 仅在打开中的页签间缓存；关闭页签时重建 keep-alive，避免已关闭页签仍被缓存 -->
          <keep-alive :key="cacheKey" :max="8">
            <component :is="Component" :key="route.path" />
          </keep-alive>
        </router-view>
      </main>
    </div>
    <div class="narrow-tip">请使用 1280 以上分辨率</div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { useAppStore } from '@/stores/app';
import AppHeader from './AppHeader.vue';
import AppSidebar from './AppSidebar.vue';
import AppTabs from './AppTabs.vue';

const route = useRoute();
const app = useAppStore();

// 关闭页签（路径集合变小）时自增 cacheKey，触发 keep-alive 重建以丢弃旧缓存
const cacheKey = ref(0);
watch(
  () => app.tabPaths,
  (next, prev) => {
    if (prev.some((path) => !next.includes(path))) {
      cacheKey.value += 1;
    }
  },
);
</script>

<style scoped>
.app-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
  background: var(--fm-page-bg);
}

.app-body {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  height: 100vh;
}

.app-content {
  flex: 1;
  min-width: 960px;
  overflow: auto;
  padding: 20px;
}
</style>
