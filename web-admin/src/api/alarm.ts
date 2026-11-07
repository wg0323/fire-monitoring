import { http } from '@/utils/request';
import type {
  AlarmDispatchOption,
  AlarmItem,
  AlarmKpi,
  AlarmMerchantItem,
  AlarmTraceItem,
  PageResult,
} from '@/types/api';

export interface AlarmQuery {
  pageNum: number;
  pageSize: number;
  merchantId?: string;
  sn?: string;
  keyword?: string;
  level?: number;
  status?: number;
  result?: number;
  verifyStatus?: number;
  excludeVerified?: boolean;
  startTime?: string;
  endTime?: string;
}

export interface AlarmVerifyPayload {
  id: string;
  result: number;
  remark?: string;
  pic1?: string;
  pic2?: string;
  pic3?: string;
}

export interface AlarmDispatchPayload {
  id: string;
  workAreaId: string;
  handlerId: string;
  dispatchRemark?: string;
}

export interface AlarmCallPayload {
  calledNo: string;
  callState?: string;
  callType?: number;
  connected?: boolean;
  contactName?: string;
  actionId?: string;
  remark?: string;
}

export interface CallResult {
  succeed: boolean;
  message: string;
  actionId?: string;
}

export function pageAlarms(params: AlarmQuery): Promise<PageResult<AlarmItem>> {
  return http.get<PageResult<AlarmItem>>('/alarm/page', params);
}

export function getAlarm(id: string): Promise<AlarmItem> {
  return http.get<AlarmItem>(`/alarm/${id}`);
}

export function getAlarmTimeline(id: string): Promise<AlarmTraceItem[]> {
  return http.get<AlarmTraceItem[]>(`/alarm/${id}/timeline`);
}

export function getAlarmKpi(): Promise<AlarmKpi> {
  return http.get<AlarmKpi>('/alarm/kpi');
}

export function listAlarmMerchants(
  keyword?: string,
  unverifiedOnly?: boolean,
): Promise<AlarmMerchantItem[]> {
  return http.get<AlarmMerchantItem[]>('/alarm/merchant-list', { keyword, unverifiedOnly });
}

export function listAlarmDispatchOptions(): Promise<AlarmDispatchOption[]> {
  return http.get<AlarmDispatchOption[]>('/alarm/dispatch-options');
}

export function verifyAlarm(payload: AlarmVerifyPayload): Promise<void> {
  return http.post<void>('/alarm/verify', payload);
}

export function dispatchAlarm(payload: AlarmDispatchPayload): Promise<void> {
  return http.post<void>('/alarm/dispatch', payload);
}

export function dialOut(payload: { alarmId: string; phone: string; type: number }): Promise<CallResult> {
  return http.post<CallResult>('/call', payload);
}

export function callAlarm(id: string, payload: AlarmCallPayload): Promise<void> {
  return http.post<void>(`/alarm/${id}/call`, payload);
}

export function convertAlarmDanger(id: string): Promise<string> {
  return http.post<string>(`/alarm/${id}/convert-danger`);
}
