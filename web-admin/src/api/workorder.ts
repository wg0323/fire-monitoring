import { http } from '@/utils/request';
import type { PageResult, WorkOrderItem } from '@/types/api';

export interface WorkOrderQuery {
  pageNum: number;
  pageSize: number;
  status?: number;
  merchantId?: string;
  keyword?: string;
}

export function pageWorkOrders(params: WorkOrderQuery): Promise<PageResult<WorkOrderItem>> {
  return http.get<PageResult<WorkOrderItem>>('/workorder/page', params);
}

export function getWorkOrder(id: string): Promise<WorkOrderItem> {
  return http.get<WorkOrderItem>(`/workorder/${id}`);
}
