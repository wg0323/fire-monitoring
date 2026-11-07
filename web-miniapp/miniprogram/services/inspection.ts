/**
 * 巡查接口：计划列表、详情、工作区商户与结果提交。
 * @date 2026-11-06
 */
import { get, post } from './request';
import type {
  InspectionMerchantItem,
  InspectionPlanItem,
  PageResult,
} from './types';

export interface InspectionQuery {
  pageNum: number;
  pageSize: number;
  name?: string;
}

export interface InspectionMerchantQuery {
  pageNum: number;
  pageSize: number;
  name?: string;
}

export interface InspectionResultPayload {
  planId: string;
  merchantId: string;
  result: number;
  dangerType?: string;
  remark?: string;
  pic1?: string;
  pic2?: string;
  pic3?: string;
  lng?: number;
  lat?: number;
}

export const pageInspectionPlans = (data: InspectionQuery): Promise<PageResult<InspectionPlanItem>> =>
  get<PageResult<InspectionPlanItem>>('/inspection/plan/page', data);

export const getInspectionPlan = (id: string): Promise<InspectionPlanItem> =>
  get<InspectionPlanItem>(`/inspection/plan/${id}`);

export const pageInspectionMerchants = (
  id: string,
  data: InspectionMerchantQuery,
): Promise<PageResult<InspectionMerchantItem>> =>
  get<PageResult<InspectionMerchantItem>>(`/inspection/plan/${id}/merchant/page`, data);

export const submitInspectionResult = (data: InspectionResultPayload): Promise<void> =>
  post<void>('/inspection/result', data);
