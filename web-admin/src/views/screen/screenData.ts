import type { MapPoint, MapPointStatus } from '@/components/map/types';
import type { DeviceTrendSeries, MerchantStatusPoint, ScreenTrendPoint } from '@/types/api';

export function toMapPoints(points: MerchantStatusPoint[]): MapPoint[] {
  return points
    .filter((point) =>
      point.lng !== null && point.lng !== undefined
      && point.lat !== null && point.lat !== undefined)
    .map((point) => ({
      id: point.merchantId,
      name: point.name,
      lng: point.lng as number,
      lat: point.lat as number,
      status: mapStatus(point.status),
      subtitle: point.statusText,
      picUrl: point.picUrl,
      typeCode: point.type,
      typeName: point.typeName,
      alarmTime: point.alarmTime,
      contact: point.contact,
      phone: point.phone,
    }));
}

export function toTrendSeries(
  points: ScreenTrendPoint[],
  label: string,
  unit: string,
): DeviceTrendSeries[] {
  return [
    {
      metric: 'count',
      label,
      unit,
      hasData: points.length > 0,
      points: points.map((point) => ({ time: point.time, value: point.value })),
    },
  ];
}

function mapStatus(status: number): MapPointStatus {
  if (status === 1) {
    return 'fire';
  }
  if (status === 2) {
    return 'fault';
  }
  if (status === 3 || status === 4) {
    return 'offline';
  }
  return 'normal';
}
