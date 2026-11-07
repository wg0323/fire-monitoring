/**
 * 认证接口：手机号白名单登录、当前用户信息、退出。
 * @date 2026-11-06
 */
import { get, post } from './request';
import type { LoginResult, UserInfo } from '../store/user';

export interface PhoneLoginPayload {
  code?: string;
  phone?: string;
}

export const phoneLogin = (payload: PhoneLoginPayload): Promise<LoginResult> =>
  post<LoginResult>('/auth/phone-login', payload, true);

export const fetchInfo = (): Promise<UserInfo> => get<UserInfo>('/auth/info');

export const switchMerchant = (merchantId: string): Promise<void> =>
  post<void>('/auth/switch-merchant', { merchantId }, true);

export const logout = (): Promise<void> => post<void>('/auth/logout', {}, true);
