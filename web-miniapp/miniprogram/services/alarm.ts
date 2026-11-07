/**
 * 报警接口：列表、详情、时间线与商户处置。
 * @date 2026-11-06
 */
import { get, post } from './request';
import type { AlarmItem, AlarmTraceItem, PageResult } from './types';

export interface AlarmQuery {
  pageNum?: number;
  pageSize?: number;
  level?: number;
  status?: number;
  result?: number;
  merchantId?: string;
  keyword?: string;
}

export interface MerchantHandlePayload {
  result: number;
  remark?: string;
  pic1?: string;
  pic2?: string;
  pic3?: string;
}

export const pageAlarms = (data: AlarmQuery): Promise<PageResult<AlarmItem>> =>
  get<PageResult<AlarmItem>>('/alarm/page', data);

export const getAlarm = (id: string): Promise<AlarmItem> =>
  get<AlarmItem>(`/alarm/${id}`);

export const getAlarmTimeline = (id: string): Promise<AlarmTraceItem[]> =>
  get<AlarmTraceItem[]>(`/alarm/${id}/timeline`);

export const merchantHandle = (id: string, data: MerchantHandlePayload): Promise<void> =>
  post<void>(`/alarm/${id}/merchant-handle`, data);
