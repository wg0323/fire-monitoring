/**
 * 首页接口。
 * @date 2026-11-06
 */
import { get } from './request';
import type { HomeSummary } from './types';

export const getHomeSummary = (): Promise<HomeSummary> =>
  get<HomeSummary>('/home/summary');
