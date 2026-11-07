import { http } from '@/utils/request';
import type {
  InspectionMerchantItem,
  InspectionPlanItem,
  InspectionResultItem,
  PageResult,
} from '@/types/api';

export interface InspectionQuery {
  pageNum: number;
  pageSize: number;
  name?: string;
  workAreaId?: string;
}

export interface InspectionSavePayload {
  id?: string;
  workAreaId: string;
  name: string;
  startDate: string;
  endDate: string;
  remark?: string;
}

export interface InspectionMerchantQuery {
  pageNum: number;
  pageSize: number;
  name?: string;
  patrolStatus?: number;
}

export function pageInspectionPlans(params: InspectionQuery): Promise<PageResult<InspectionPlanItem>> {
  return http.get<PageResult<InspectionPlanItem>>(
    '/inspection/plan/page',
    params,
  );
}

export function getInspectionPlan(id: string): Promise<InspectionPlanItem> {
  return http.get<InspectionPlanItem>(`/inspection/plan/${id}`);
}

export function createInspectionPlan(payload: InspectionSavePayload): Promise<string> {
  return http.post<string>('/inspection/plan', payload);
}

export function updateInspectionPlan(payload: InspectionSavePayload): Promise<void> {
  return http.put<void>('/inspection/plan', payload);
}

export function deleteInspectionPlan(id: string): Promise<void> {
  return http.delete<void>(`/inspection/plan/${id}`);
}

export function listInspectionMerchants(id: string): Promise<InspectionMerchantItem[]> {
  return http.get<InspectionMerchantItem[]>(`/inspection/plan/${id}/merchants`);
}

export function pageInspectionMerchants(
  id: string,
  params: InspectionMerchantQuery,
): Promise<PageResult<InspectionMerchantItem>> {
  return http.get<PageResult<InspectionMerchantItem>>(
    `/inspection/plan/${id}/merchant/page`,
    params,
  );
}

export function pageInspectionResults(
  id: string,
  pageNum: number,
  pageSize: number,
): Promise<PageResult<InspectionResultItem>> {
  return http.get<PageResult<InspectionResultItem>>(`/inspection/plan/${id}/results`, {
    pageNum,
    pageSize,
  });
}
