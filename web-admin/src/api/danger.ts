import { http } from '@/utils/request';
import type { DangerItem, DangerTimelineNode, DictItem, PageResult } from '@/types/api';

export interface DangerQuery {
  pageNum: number;
  pageSize: number;
  status?: number;
  unfinished?: boolean;
  sourceType?: number;
  dangerType?: string;
  keyword?: string;
  dealLimitFrom?: string;
  dealLimitTo?: string;
}

export interface DangerAssignPayload {
  assigneeId: string;
  dealLimit?: string;
  dangerLevelFinal?: number;
  remark?: string;
}

export interface DangerBatchAssignPayload {
  ids: string[];
  assigneeId: string;
  dealLimit?: string;
  dangerLevelFinal?: number;
  remark?: string;
}

export interface DangerBatchAssignFailure {
  id: string;
  reason: string;
}

export interface DangerBatchAssignResult {
  total: number;
  successCount: number;
  failedCount: number;
  failures: DangerBatchAssignFailure[];
}

export interface DangerAuditPayload {
  auditResult: number;
  auditRemark?: string;
}

export function pageDangers(params: DangerQuery): Promise<PageResult<DangerItem>> {
  return http.get<PageResult<DangerItem>>('/danger/page', params);
}

export function getDanger(id: string): Promise<DangerItem> {
  return http.get<DangerItem>(`/danger/${id}`);
}

export function getDangerTimeline(id: string): Promise<DangerTimelineNode[]> {
  return http.get<DangerTimelineNode[]>(`/danger/${id}/timeline`);
}

export function assignDanger(id: string, payload: DangerAssignPayload): Promise<void> {
  return http.put<void>(`/danger/${id}/assign`, payload);
}

export function batchAssignDanger(payload: DangerBatchAssignPayload): Promise<DangerBatchAssignResult> {
  // 逐条独立事务，100 条可能超过全局 15s 超时
  return http.put<DangerBatchAssignResult>('/danger/batch-assign', payload, { timeout: 60000 });
}

export function auditDanger(id: string, payload: DangerAuditPayload): Promise<void> {
  return http.post<void>(`/danger/${id}/audit`, payload);
}

export function listDangerTypes(): Promise<DictItem[]> {
  return http.get<DictItem[]>('/device/dict/danger-type');
}
