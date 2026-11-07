import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import type { MenuNode, UserInfo } from '@/types/api';
import { fetchInfo, login as loginApi, logout as logoutApi, type LoginPayload } from '@/api/auth';
import { TOKEN_KEY } from '@/utils/request';
import { collectPaths, firstMenuPath, hasMenuPath as hasMenu } from '@/utils/tree';

const ACCOUNT_KEY = 'fm-admin-account';

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) ?? '');
  const user = ref<UserInfo | null>(null);

  const roles = computed(() => user.value?.roles ?? []);
  const permissions = computed(() => user.value?.permissions ?? []);
  const menus = computed<MenuNode[]>(() => user.value?.menus ?? []);
  const isPlatform = computed(() => user.value?.userType === 'PLATFORM');
  const displayName = computed(() => user.value?.realName || user.value?.userName || '');
  const tenantName = computed(() => user.value?.tenantName || (isPlatform.value ? '平台' : '-'));

  function setToken(value: string): void {
    token.value = value;
    localStorage.setItem(TOKEN_KEY, value);
  }

  async function login(payload: LoginPayload): Promise<void> {
    const result = await loginApi(payload);
    setToken(result.tokenValue);
    localStorage.setItem(ACCOUNT_KEY, payload.userName);
    await loadInfo();
  }

  async function loadInfo(): Promise<void> {
    user.value = await fetchInfo();
  }

  function hasPerm(perm: string): boolean {
    return permissions.value.includes('*') || permissions.value.includes(perm);
  }

  function hasMenuPath(path: string): boolean {
    return hasMenu(menus.value, path);
  }

  function landingPath(): string {
    const paths = collectPaths(menus.value);
    if (isPlatform.value && paths.includes('/screen/comprehensive')) {
      return '/screen/comprehensive';
    }
    if (paths.includes('/duty/fire')) {
      return '/duty/fire';
    }
    return firstMenuPath(menus.value) || '/login';
  }

  function clear(): void {
    token.value = '';
    user.value = null;
    localStorage.removeItem(TOKEN_KEY);
  }

  async function logout(): Promise<void> {
    try {
      await logoutApi();
    } finally {
      clear();
    }
  }

  return {
    token,
    user,
    roles,
    permissions,
    menus,
    isPlatform,
    displayName,
    tenantName,
    login,
    loadInfo,
    hasPerm,
    hasMenuPath,
    landingPath,
    clear,
    logout,
    accountKey: ACCOUNT_KEY,
  };
});
