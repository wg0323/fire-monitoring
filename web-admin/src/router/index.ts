import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';
import { useAppStore } from '@/stores/app';
import { useAuthStore } from '@/stores/auth';
import { menuTitleByPath } from '@/utils/tree';

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', public: true },
  },
  {
    path: '/screen',
    component: () => import('@/layouts/ScreenLayout.vue'),
    redirect: '/screen/comprehensive',
    children: [
      {
        path: 'comprehensive',
        name: 'screen-comprehensive',
        component: () => import('@/views/screen/comprehensive/index.vue'),
        meta: { title: '综合监控', screen: true },
      },
      {
        path: 'alarm',
        name: 'screen-alarm',
        component: () => import('@/views/screen/alarm/index.vue'),
        meta: { title: '报警监控', screen: true },
      },
      {
        path: 'danger',
        name: 'screen-danger',
        component: () => import('@/views/screen/danger/index.vue'),
        meta: { title: '巡查隐患', screen: true },
      },
      {
        path: 'device',
        name: 'screen-device',
        component: () => import('@/views/screen/device/index.vue'),
        meta: { title: '设备监控', screen: true },
      },
    ],
  },
  {
    path: '/',
    component: () => import('@/layouts/AppLayout.vue'),
    children: [
      {
        path: 'system/user',
        name: 'system-user',
        component: () => import('@/views/system/user/index.vue'),
        meta: { title: '用户管理' },
      },
      {
        path: 'system/work-area',
        name: 'system-work-area',
        component: () => import('@/views/system/work-area/index.vue'),
        meta: { title: '工作区管理' },
      },
      {
        path: 'system/tenant',
        name: 'system-tenant',
        component: () => import('@/views/system/tenant/index.vue'),
        meta: { title: '运营中心管理' },
      },
      {
        path: 'system/role',
        name: 'system-role',
        component: () => import('@/views/system/role/index.vue'),
        meta: { title: '角色管理' },
      },
      {
        path: 'system/menu',
        name: 'system-menu',
        component: () => import('@/views/system/menu/index.vue'),
        meta: { title: '菜单管理' },
      },
      {
        path: 'dict/alarm-type',
        name: 'dict-alarm-type',
        component: () => import('@/views/dict/index.vue'),
        meta: { title: '报警类型', dictType: 'alarm-type' },
      },
      {
        path: 'dict/device-type',
        name: 'dict-device-type',
        component: () => import('@/views/dict/index.vue'),
        meta: { title: '设备类型', dictType: 'device-type' },
      },
      {
        path: 'dict/merchant-type',
        name: 'dict-merchant-type',
        component: () => import('@/views/dict/index.vue'),
        meta: { title: '商户类型', dictType: 'merchant-type' },
      },
      {
        path: 'dict/danger-type',
        name: 'dict-danger-type',
        component: () => import('@/views/dict/index.vue'),
        meta: { title: '隐患类型', dictType: 'danger-type' },
      },
      {
        path: 'dict/device-maker',
        name: 'dict-device-maker',
        component: () => import('@/views/dict/index.vue'),
        meta: { title: '设备制造商', dictType: 'device-maker' },
      },
      {
        path: 'duty/fire',
        name: 'duty-fire',
        component: () => import('@/views/duty/fire/index.vue'),
        meta: { title: '火警处置' },
      },
      {
        path: 'duty/danger',
        name: 'duty-danger',
        component: () => import('@/views/duty/danger/index.vue'),
        meta: { title: '隐患处置' },
      },
      {
        path: 'trace/alarm',
        name: 'trace-alarm',
        component: () => import('@/views/trace/alarm/index.vue'),
        meta: { title: '火警跟踪' },
      },
      {
        path: 'trace/fire',
        name: 'trace-fire',
        component: () => import('@/views/trace/fire/index.vue'),
        meta: { title: '火灾跟踪' },
      },
      {
        path: 'trace/danger',
        name: 'trace-danger',
        component: () => import('@/views/trace/danger/index.vue'),
        meta: { title: '隐患跟踪' },
      },
      {
        path: 'service/merchant',
        name: 'service-merchant',
        component: () => import('@/views/service/merchant/index.vue'),
        meta: { title: '商户管理' },
      },
      {
        path: 'service/device',
        name: 'service-device',
        component: () => import('@/views/service/device/index.vue'),
        meta: { title: '设备管理' },
      },
      {
        path: 'service/inspection',
        name: 'service-inspection',
        component: () => import('@/views/service/inspection/index.vue'),
        meta: { title: '巡检管理' },
      },
      {
        path: 'service/call-config',
        name: 'service-call-config',
        component: () => import('@/views/service/call-config/index.vue'),
        meta: { title: '外呼配置' },
      },
      {
        path: 'archive/water',
        name: 'archive-water',
        component: () => import('@/views/archive/water/index.vue'),
        meta: { title: '水系统' },
      },
      {
        path: 'archive/electric',
        name: 'archive-electric',
        component: () => import('@/views/archive/electric/index.vue'),
        meta: { title: '电器火灾' },
      },
      {
        path: 'workorder/list',
        name: 'workorder-list',
        component: () => import('@/views/workorder/list/index.vue'),
        meta: { title: '工单列表' },
      },
      {
        path: 'stat/comprehensive',
        name: 'stat-comprehensive',
        component: () => import('@/views/stat/comprehensive/index.vue'),
        meta: { title: '综合分析' },
      },
      {
        path: 'stat/alarm',
        name: 'stat-alarm',
        component: () => import('@/views/stat/alarm/index.vue'),
        meta: { title: '报警分析' },
      },
      {
        path: 'stat/fault',
        name: 'stat-fault',
        component: () => import('@/views/stat/fault/index.vue'),
        meta: { title: '故障分析' },
      },
      {
        path: 'stat/danger',
        name: 'stat-danger',
        component: () => import('@/views/stat/danger/index.vue'),
        meta: { title: '隐患分析' },
      },
      {
        path: 'stat/fire',
        name: 'stat-fire',
        component: () => import('@/views/stat/fire/index.vue'),
        meta: { title: '真警分析' },
      },
      {
        path: 'stat/call',
        name: 'stat-call',
        component: () => import('@/views/stat/call/index.vue'),
        meta: { title: '通话分析' },
      },
      {
        path: 'report/month',
        name: 'report-month',
        component: () => import('@/views/common/Placeholder.vue'),
        meta: { title: '月度报告', underConstruction: true },
      },
      {
        path: 'report/year',
        name: 'report-year',
        component: () => import('@/views/common/Placeholder.vue'),
        meta: { title: '年度报告', underConstruction: true },
      },
      {
        path: ':pathMatch(.*)*',
        name: 'placeholder',
        component: () => import('@/views/common/Placeholder.vue'),
        meta: { title: '页面' },
      },
    ],
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
});

router.beforeEach(async (to) => {
  const auth = useAuthStore();
  if (to.meta.public) {
    if (to.path === '/login' && auth.token) {
      try {
        if (!auth.user) {
          await auth.loadInfo();
        }
        const landing = auth.landingPath();
        if (landing !== '/login') {
          return landing;
        }
      } catch {
        auth.clear();
      }
    }
    return true;
  }

  if (!auth.token) {
    return { path: '/login', query: { redirect: to.fullPath } };
  }
  if (!auth.user) {
    try {
      await auth.loadInfo();
    } catch {
      auth.clear();
      return { path: '/login' };
    }
  }

  const landing = auth.landingPath();
  if (to.path === '/') {
    if (landing === '/login') {
      auth.clear();
      return { path: '/login' };
    }
    return landing;
  }
  if (!auth.hasMenuPath(to.path)) {
    if (landing === '/login' || landing === to.path) {
      auth.clear();
      return { path: '/login' };
    }
    return landing;
  }
  return true;
});

router.afterEach((to) => {
  if (to.path === '/login' || to.meta.screen) {
    return;
  }
  const auth = useAuthStore();
  const app = useAppStore();
  const title = menuTitleByPath(auth.menus, to.path) || to.meta.title || '页面';
  app.addTab(to.path, title);
});

// 权限信息只在缺少 user 时加载一次，后台调整角色/菜单后可能长期不生效。
// 这里复用已有的 /auth/info（loadInfo）做轻量刷新，不新增后端接口：
// 切回前台或定时触发，带节流与并发标志，避免频繁请求与并发刷新。
const PERM_REFRESH_INTERVAL = 5 * 60 * 1000;
let lastPermRefresh = 0;
let permRefreshing = false;

async function refreshPermissions(): Promise<void> {
  const auth = useAuthStore();
  if (!auth.token || window.location.pathname === '/login' || permRefreshing) {
    return;
  }
  const now = Date.now();
  if (now - lastPermRefresh < PERM_REFRESH_INTERVAL) {
    return;
  }
  lastPermRefresh = now;
  permRefreshing = true;
  try {
    await auth.loadInfo();
  } catch {
    // 刷新失败保留现有权限，交由后续 401/403 流程处理
  } finally {
    permRefreshing = false;
  }
}

if (typeof window !== 'undefined') {
  document.addEventListener('visibilitychange', () => {
    if (!document.hidden) {
      void refreshPermissions();
    }
  });
  window.setInterval(() => {
    if (!document.hidden) {
      void refreshPermissions();
    }
  }, PERM_REFRESH_INTERVAL);
}

export default router;
