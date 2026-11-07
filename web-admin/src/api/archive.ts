import { http } from '@/utils/request';
import type { ArchiveMerchantNode, DeviceTrendSeries } from '@/types/api';

export type ArchiveKind = 'water' | 'electric';

export function archiveTree(kind: ArchiveKind): Promise<ArchiveMerchantNode[]> {
  return http.get<ArchiveMerchantNode[]>(`/archive/${kind}/tree`);
}

export function archiveTrend(
  kind: ArchiveKind,
  deviceId: string,
  range: string,
): Promise<DeviceTrendSeries[]> {
  return http.get<DeviceTrendSeries[]>(`/archive/${kind}/${deviceId}/trend`, { range });
}
