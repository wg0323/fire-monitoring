/**
 * 站内消息与订阅配置。
 * @date 2026-11-06
 */
import { get, post, request } from './request';
import type { NotificationItem, PageResult, SubscribeConfig } from './types';

export const pageNotifications = (
  pageNum: number,
  pageSize: number,
): Promise<PageResult<NotificationItem>> =>
  get<PageResult<NotificationItem>>('/notification/page', { pageNum, pageSize });

export const unreadNotificationCount = (): Promise<number> =>
  get<number>('/notification/unread-count');

export const markNotificationRead = (id: string): Promise<void> =>
  request<void>({ url: `/notification/${id}/read`, method: 'PUT' });

export const getSubscribeConfig = (): Promise<SubscribeConfig> =>
  get<SubscribeConfig>('/notification/subscribe-config');

export const recordSubscribe = (templateIds: string[]): Promise<void> =>
  post<void>('/notification/subscribe', { templateIds });
