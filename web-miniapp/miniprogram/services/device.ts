/**
 * 商户选项与字典：随手拍选商户、隐患类型下拉。
 * @date 2026-11-06
 */
import { get } from './request';
import type { DeviceItem, DictItem, MerchantOption, PageResult } from './types';

export interface DeviceQuery {
  pageNum: number;
  pageSize: number;
  keyword?: string;
  onlineStatus?: number;
}

export const pageDevices = (query: DeviceQuery): Promise<PageResult<DeviceItem>> =>
  get<PageResult<DeviceItem>>('/device/page', query);

export const listMerchantOptions = (keyword?: string): Promise<MerchantOption[]> =>
  get<MerchantOption[]>('/device/merchant-options', { keyword });

export const listDangerTypes = (): Promise<DictItem[]> =>
  get<DictItem[]>('/device/dict/danger-type');
