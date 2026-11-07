import { http } from '@/utils/request';
import type { LoginResult, MenuNode, UserInfo } from '@/types/api';

export interface LoginPayload {
  userName: string;
  password: string;
}

export interface PasswordPayload {
  oldPassword: string;
  newPassword: string;
}

export function login(payload: LoginPayload): Promise<LoginResult> {
  return http.post<LoginResult>('/auth/login', payload);
}

export function logout(): Promise<void> {
  return http.post<void>('/auth/logout');
}

export function fetchInfo(): Promise<UserInfo> {
  return http.get<UserInfo>('/auth/info');
}

export function fetchMenus(): Promise<MenuNode[]> {
  return http.get<MenuNode[]>('/auth/menus');
}

export function changePassword(payload: PasswordPayload): Promise<void> {
  return http.put<void>('/auth/password', payload);
}
