/**
 * 页面角色守卫、自定义 tabBar 刷新与 onShow 节流刷新。
 * @date 2026-11-06
 */
import { getToken, getUser } from '../store/user';

export const requireLogin = (): boolean => {
  if (!getToken()) {
    wx.reLaunch({ url: '/pages/login/index' });
    return false;
  }
  return true;
};

export const requireRole = (roles: string[]): boolean => {
  const user = getUser();
  if (!user || !user.roles.some((role) => roles.includes(role))) {
    wx.showToast({ title: '当前账号无权访问', icon: 'none' });
    wx.switchTab({ url: '/pages/home/index' });
    return false;
  }
  return true;
};

interface TabBarLike {
  refresh?: () => void;
}

interface PageWithTabBar {
  getTabBar?: () => TabBarLike | undefined;
}

export const refreshTabBar = (page: unknown): void => {
  const tabBar = (page as PageWithTabBar).getTabBar?.();
  tabBar?.refresh?.();
};

/** 首次必加载，之后按时间窗口节流；写操作可用 markDirty 强制下次 onShow 刷新 */
const REFRESH_INTERVAL = 60 * 1000;
const refreshState = new Map<string, { lastAt: number; dirty: boolean }>();

export const markDirty = (key: string): void => {
  const record = refreshState.get(key);
  if (record) {
    record.dirty = true;
  } else {
    refreshState.set(key, { lastAt: 0, dirty: true });
  }
};

export const onShowRefresh = (key: string, fn: () => void): void => {
  const now = Date.now();
  const record = refreshState.get(key);
  if (!record || record.dirty || now - record.lastAt >= REFRESH_INTERVAL) {
    refreshState.set(key, { lastAt: now, dirty: false });
    fn();
  }
};
