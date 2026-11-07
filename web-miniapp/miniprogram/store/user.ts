/**
 * 用户会话存储：token 与用户信息统一读写，页面与请求层共用。
 * @date 2026-11-06
 */
import type { MerchantOption } from '../services/types';

export type { LoginResult, MerchantOption } from '../services/types';

export interface UserInfo {
  id: string;
  userName: string;
  realName: string;
  phone?: string | null;
  userType: string;
  tenantId?: string | null;
  tenantName?: string | null;
  merchantId?: string | null;
  merchantName?: string | null;
  merchants?: MerchantOption[];
  workAreaNames: string[];
  roles: string[];
}

const TOKEN_KEY = 'fm-mp-token';
const USER_KEY = 'fm-mp-user';
const META_KEY = 'fm-mp-session-meta';
const SESSION_VERSION = 1;
const SESSION_TTL = 7 * 24 * 60 * 60 * 1000;

interface SessionMeta {
  version: number;
  expiresAt: number;
}

const clearStorage = (): void => {
  wx.removeStorageSync(TOKEN_KEY);
  wx.removeStorageSync(USER_KEY);
  wx.removeStorageSync(META_KEY);
};

/** 校验版本与过期时间，失效时自动清理，避免旧明文会话被长期复用 */
const validSession = (): boolean => {
  const meta = wx.getStorageSync<SessionMeta | ''>(META_KEY);
  if (!meta || meta.version !== SESSION_VERSION || meta.expiresAt <= Date.now()) {
    clearStorage();
    return false;
  }
  return true;
};

const writeMeta = (): void => {
  const meta: SessionMeta = { version: SESSION_VERSION, expiresAt: Date.now() + SESSION_TTL };
  wx.setStorageSync(META_KEY, meta);
};

export const getToken = (): string => validSession() ? wx.getStorageSync<string>(TOKEN_KEY) || '' : '';

export const setToken = (token: string): void => {
  wx.setStorageSync(TOKEN_KEY, token);
  writeMeta();
};

export const getUser = (): UserInfo | null => {
  if (!validSession()) {
    return null;
  }
  return wx.getStorageSync<UserInfo>(USER_KEY) || null;
};

export const setUser = (user: UserInfo): void => {
  wx.setStorageSync(USER_KEY, user);
  writeMeta();
};

export const clearSession = (): void => {
  clearStorage();
};

export const roleKey = (user: UserInfo | null): string => user?.roles?.[0] ?? '';
