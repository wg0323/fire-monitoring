import { download, http } from '@/utils/request';
import type {
  BuildingItem,
  DeviceBriefRow,
  DeviceDetail,
  DeviceImportResult,
  DeviceItem,
  DevicePointItem,
  DeviceTrendSeries,
  DictItem,
  FloorItem,
  MerchantOption,
  PageResult,
} from '@/types/api';

export interface DeviceQuery {
  pageNum: number;
  pageSize: number;
  keyword?: string;
  type?: string;
  merchantId?: string;
  status?: number;
  onlineStatus?: number;
  locationType?: number;
  unbound?: boolean;
}

export interface DevicePayload {
  sn: string;
  name?: string;
  type: string;
  merchantId?: string | null;
  maker?: string | null;
  position?: string;
  lng?: number | null;
  lat?: number | null;
  locationType: number;
  buildingId?: string | null;
  floorId?: string | null;
  floorX?: number | null;
  floorY?: number | null;
  sim?: string;
  cardNo?: string;
  channelNum?: number | null;
  activeDate?: string | null;
  installDate?: string | null;
  status: number;
  /** 类型独有字段（AEP 凭证、视频账号等），JSON 字符串原样落库 */
  fExt?: string;
}

export interface BuildingPayload {
  merchantId: string;
  name: string;
  sort?: number;
  remark?: string;
  status: number;
}

export interface FloorPayload {
  buildingId: string;
  merchantId: string;
  name: string;
  floorNo?: number;
  sort?: number;
  status: number;
}

export interface FloorPlanPayload {
  planUrl: string;
  planWidth: number;
  planHeight: number;
}

export interface FloorPoint {
  deviceId: string;
  x: number;
  y: number;
}

export interface FloorPointsPayload {
  points: FloorPoint[];
  removedDeviceIds: string[];
}

export function pageDevices(params: DeviceQuery): Promise<PageResult<DeviceBriefRow>> {
  return http.get<PageResult<DeviceBriefRow>>(
    '/device/page',
    params,
  );
}

export function listBuildings(merchantId: string): Promise<BuildingItem[]> {
  return http.get<BuildingItem[]>('/building/list', { merchantId });
}

export function createBuilding(payload: BuildingPayload): Promise<string> {
  return http.post<string>('/building', payload);
}

export function updateBuilding(id: string, payload: BuildingPayload): Promise<void> {
  return http.put<void>(`/building/${id}`, payload);
}

export function deleteBuilding(id: string): Promise<void> {
  return http.delete<void>(`/building/${id}`);
}

export function listFloors(params: { merchantId?: string; buildingId?: string }): Promise<FloorItem[]> {
  return http.get<FloorItem[]>('/floor/list', params);
}

export function createFloor(payload: FloorPayload): Promise<string> {
  return http.post<string>('/floor', payload);
}

export function updateFloor(id: string, payload: FloorPayload): Promise<void> {
  return http.put<void>(`/floor/${id}`, payload);
}

export function deleteFloor(id: string): Promise<void> {
  return http.delete<void>(`/floor/${id}`);
}

export function updateFloorPlan(id: string, payload: FloorPlanPayload): Promise<void> {
  return http.put<void>(`/floor/${id}/plan`, payload);
}

export function listFloorPoints(id: string): Promise<DevicePointItem[]> {
  return http.get<DevicePointItem[]>(`/floor/${id}/points`);
}

export function listFloorCandidates(id: string): Promise<DevicePointItem[]> {
  return http.get<DevicePointItem[]>(`/floor/${id}/candidates`);
}

export function saveFloorPoints(id: string, payload: FloorPointsPayload): Promise<void> {
  return http.put<void>(`/floor/${id}/points`, payload);
}

export function merchantTypes(): Promise<DictItem[]> {
  return http.get<DictItem[]>('/device/dict/merchant-type');
}

export function deviceTypes(): Promise<DictItem[]> {
  return http.get<DictItem[]>('/device/dict/device-type');
}

export function deviceMakers(): Promise<DictItem[]> {
  return http.get<DictItem[]>('/device/dict/device-maker');
}

export function merchantOptions(): Promise<MerchantOption[]> {
  return http.get<MerchantOption[]>('/device/merchant/options');
}

export function getDevice(id: string, range = 'week'): Promise<DeviceDetail> {
  return http.get<DeviceDetail>(`/device/${id}`, { range });
}

export function getDeviceTrend(id: string, range: string): Promise<DeviceTrendSeries[]> {
  return http.get<DeviceTrendSeries[]>(`/device/${id}/trend`, { range });
}

export function createDevice(payload: DevicePayload): Promise<string> {
  return http.post<string>('/device', payload);
}

export function updateDevice(id: string, payload: DevicePayload): Promise<void> {
  return http.put<void>(`/device/${id}`, payload);
}

export function deleteDevice(id: string): Promise<void> {
  return http.delete<void>(`/device/${id}`);
}

export function importDevices(file: File): Promise<DeviceImportResult> {
  const data = new FormData();
  data.append('file', file);
  return http.post<DeviceImportResult>('/device/import', data);
}

export function exportDevices(params: DeviceQuery): Promise<void> {
  return download('/device/export', params, '设备列表.xlsx');
}

export function downloadDeviceTemplate(): Promise<void> {
  return download('/device/template', undefined, '设备导入模板.xlsx');
}
