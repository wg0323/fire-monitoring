import { http } from '@/utils/request';
import type { StatAlarm, StatCall, StatComprehensive, StatDanger, StatFault } from '@/types/api';

export interface StatQuery {
  startTime?: string;
  endTime?: string;
  pageNum?: number;
  pageSize?: number;
}

export function getStatComprehensive(query: StatQuery): Promise<StatComprehensive> {
  return http.get<StatComprehensive>('/stat/comprehensive', { ...query });
}

export function getStatAlarm(query: StatQuery): Promise<StatAlarm> {
  return http.get<StatAlarm>('/stat/alarm', { ...query });
}

export function getStatFault(query: StatQuery): Promise<StatFault> {
  return http.get<StatFault>('/stat/fault', { ...query });
}

export function getStatDanger(query: StatQuery): Promise<StatDanger> {
  return http.get<StatDanger>('/stat/danger', { ...query });
}

export function getStatFire(query: StatQuery): Promise<StatAlarm> {
  return http.get<StatAlarm>('/stat/fire', { ...query });
}

export function getStatCall(query: StatQuery): Promise<StatCall> {
  return http.get<StatCall>('/stat/call', { ...query });
}
