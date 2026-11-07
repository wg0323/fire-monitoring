import { http } from '@/utils/request';
import type { MerchantDetail, MerchantItem, MerchantPayload, PageResult } from '@/types/api';

export interface MerchantQuery {
  pageNum: number;
  pageSize: number;
  keyword?: string;
  type?: string;
  merchantKind?: number;
  status?: number;
  workAreaId?: string;
}

export function pageMerchants(params: MerchantQuery): Promise<PageResult<MerchantItem>> {
  return http.get<PageResult<MerchantItem>>(
    '/device/merchant/page',
    params,
  );
}

export function getMerchant(id: string): Promise<MerchantDetail> {
  return http.get<MerchantDetail>(`/device/merchant/${id}`);
}

export function createMerchant(payload: MerchantPayload): Promise<string> {
  return http.post<string>('/device/merchant', payload);
}

export function updateMerchant(id: string, payload: MerchantPayload): Promise<void> {
  return http.put<void>(`/device/merchant/${id}`, payload);
}

export function deleteMerchant(id: string): Promise<void> {
  return http.delete<void>(`/device/merchant/${id}`);
}

export function updateMerchantStatus(id: string, status: number): Promise<void> {
  return http.put<void>(`/device/merchant/${id}/status`, { status });
}

export function bindMerchantWorkAreas(id: string, workAreaIds: string[]): Promise<void> {
  return http.put<void>(`/device/merchant/${id}/work-areas`, { workAreaIds });
}

export function bindMerchantDevices(id: string, deviceIds: string[]): Promise<void> {
  return http.post<void>(`/device/merchant/${id}/devices`, { deviceIds });
}

export function unbindMerchantDevice(id: string, deviceId: string): Promise<void> {
  return http.delete<void>(`/device/merchant/${id}/devices/${deviceId}`);
}
