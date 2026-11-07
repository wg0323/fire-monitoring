<template>
  <div class="screen-root">
    <div
      class="screen-canvas"
      :style="{
        width: `${canvasWidth}px`,
        height: `${canvasHeight}px`,
        transform: `translate(-50%, -50%) scale(${scale})`,
      }"
    >
      <header class="screen-header">
        <div class="screen-brand">
          <span class="screen-brand__mark"></span>
          <div>
            <div class="screen-brand__name">消防监控平台</div>
            <div class="screen-brand__page">{{ pageTitle }}</div>
          </div>
        </div>
        <div class="screen-header__right">
          <span class="screen-clock">{{ clockText }}</span>
          <span class="screen-header__divider"></span>
          <span>最近刷新 {{ refreshText }}</span>
          <button type="button" class="screen-back" @click="backToAdmin">返回后台</button>
        </div>
      </header>
      <main class="screen-body">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, provide, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import dayjs from 'dayjs';
import { useAuthStore } from '@/stores/auth';
import { SCREEN_TICK } from '@/views/screen/useScreenTick';
import '@/views/screen/screen.css';

const REFRESH_INTERVAL = 30000;
const DESIGN_WIDTH = 1920;
const DESIGN_HEIGHT = 1080;

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();

const scale = ref(1);
const canvasWidth = ref(DESIGN_WIDTH);
const canvasHeight = ref(DESIGN_HEIGHT);
const clock = ref(new Date());
const lastRefresh = ref(new Date());
const tick = ref(0);

provide(SCREEN_TICK, tick);

const pageTitle = computed(() => String(route.meta.title ?? '数据大屏'));
const clockText = computed(() => dayjs(clock.value).format('YYYY-MM-DD HH:mm:ss'));
const refreshText = computed(() => dayjs(lastRefresh.value).format('HH:mm:ss'));

let clockTimer: number | undefined;
let refreshTimer: number | undefined;

function resize(): void {
  scale.value = Math.min(window.innerWidth / DESIGN_WIDTH, window.innerHeight / DESIGN_HEIGHT);
  canvasWidth.value = window.innerWidth / scale.value;
  canvasHeight.value = window.innerHeight / scale.value;
}

function backToAdmin(): void {
  router.push(auth.landingPath());
}

onMounted(() => {
  resize();
  window.addEventListener('resize', resize);
  clockTimer = window.setInterval(() => {
    clock.value = new Date();
  }, 1000);
  refreshTimer = window.setInterval(() => {
    tick.value += 1;
    lastRefresh.value = new Date();
  }, REFRESH_INTERVAL);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize);
  if (clockTimer) {
    window.clearInterval(clockTimer);
  }
  if (refreshTimer) {
    window.clearInterval(refreshTimer);
  }
});
</script>
