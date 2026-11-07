import { http } from '@/utils/request';

export interface CallConfig {
  configured: boolean;
  accountId?: string | null;
  host?: string | null;
  fromExten?: string | null;
  status?: number | null;
  mockMode?: boolean;
}

export interface CallConfigPayload {
  accountId: string;
  apiSecret: string;
  host: string;
  fromExten?: string;
}

export function getCallConfig(): Promise<CallConfig> {
  return http.get<CallConfig>('/call-config');
}

export function saveCallConfig(payload: CallConfigPayload): Promise<void> {
  return http.put<void>('/call-config', payload);
}
