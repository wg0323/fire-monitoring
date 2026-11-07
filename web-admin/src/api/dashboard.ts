import { http } from '@/utils/request';
import type { ScreenAlarm, ScreenComprehensive, ScreenDanger, ScreenDevice } from '@/types/api';

export function getComprehensiveScreen(): Promise<ScreenComprehensive> {
  return http.get<ScreenComprehensive>('/dashboard/comprehensive');
}

export function getAlarmScreen(range: string): Promise<ScreenAlarm> {
  return http.get<ScreenAlarm>('/dashboard/alarm', { range });
}

export function getDangerScreen(range: string): Promise<ScreenDanger> {
  return http.get<ScreenDanger>('/dashboard/danger', { range });
}

export function getDeviceScreen(range: string): Promise<ScreenDevice> {
  return http.get<ScreenDevice>('/dashboard/device', { range });
}
