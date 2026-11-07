/**
 * 工单接口：我的工单、详情、接单与完成。
 * @date 2026-11-06
 */
import { get, post } from './request';
import type { PageResult, WorkOrderItem } from './types';

export interface WorkOrderQuery {
  pageNum?: number;
  pageSize?: number;
  status?: number;
}

export interface WorkOrderFinishPayload {
  result: number;
  remark?: string;
  pic1?: string;
  pic2?: string;
  pic3?: string;
}

export const pageWorkOrders = (data: WorkOrderQuery): Promise<PageResult<WorkOrderItem>> =>
  get<PageResult<WorkOrderItem>>('/workorder/page', data);

export const getWorkOrder = (id: string): Promise<WorkOrderItem> =>
  get<WorkOrderItem>(`/workorder/${id}`);

export const acceptWorkOrder = (id: string): Promise<void> =>
  post<void>(`/workorder/${id}/accept`);

export const finishWorkOrder = (id: string, data: WorkOrderFinishPayload): Promise<void> =>
  post<void>(`/workorder/${id}/finish`, data);
