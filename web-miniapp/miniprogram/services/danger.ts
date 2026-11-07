/**
 * 隐患接口：列表、详情、时间线、随手拍、整改、指派与审核。
 * @date 2026-11-06
 */
import { get, post } from './request';
import type {
  DangerAssignee,
  DangerItem,
  DangerTimelineNode,
  PageResult,
} from './types';

export interface DangerQuery {
  pageNum: number;
  pageSize: number;
  status?: number;
  sourceType?: number;
  keyword?: string;
}

export interface DangerReportPayload {
  merchantId: string;
  dangerType?: string;
  remark: string;
  position?: string;
  pic1?: string;
  pic2?: string;
  pic3?: string;
  lng?: number;
  lat?: number;
}

export interface DangerDealPayload {
  remark: string;
  pic1?: string;
  pic2?: string;
  pic3?: string;
  lng?: number;
  lat?: number;
}

export interface DangerAssignPayload {
  assigneeId: string;
  dealLimit?: string;
  remark?: string;
}

export interface DangerAuditPayload {
  auditResult: number;
  auditRemark?: string;
}

export const pageDangers = (data: DangerQuery): Promise<PageResult<DangerItem>> =>
  get<PageResult<DangerItem>>('/danger/page', data);

export const getDanger = (id: string): Promise<DangerItem> =>
  get<DangerItem>(`/danger/${id}`);

export const getDangerTimeline = (id: string): Promise<DangerTimelineNode[]> =>
  get<DangerTimelineNode[]>(`/danger/${id}/timeline`);

export const reportDanger = (data: DangerReportPayload): Promise<string> =>
  post<string>('/danger', data);

export const dealDanger = (id: string, data: DangerDealPayload): Promise<void> =>
  post<void>(`/danger/${id}/deal`, data);

export const assignDanger = (id: string, data: DangerAssignPayload): Promise<void> =>
  post<void>(`/danger/${id}/assign`, data);

export const auditDanger = (id: string, data: DangerAuditPayload): Promise<void> =>
  post<void>(`/danger/${id}/audit`, data);

export const listDangerAssignees = (dangerId: string): Promise<DangerAssignee[]> =>
  get<DangerAssignee[]>('/danger/assignee-options', { dangerId });
