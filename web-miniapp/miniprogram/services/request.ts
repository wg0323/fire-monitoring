/**
 * 请求封装：token、Result 解包、401 重登、错误提示。
 * @date 2026-11-06
 */
import { API_BASE } from '../config/index';
import { clearSession, getToken } from '../store/user';
import { logError } from '../utils/log';

let redirecting = false;

/** 免登录接口：它们的 401 是业务失败（如手机号未开通），不是会话过期 */
const AUTH_FREE_URLS = ['/auth/phone-login', '/auth/wx-login'];

/** 非标准响应摘要：Spring 默认错误页是 JSON 但无 message，网关/代理可能返回 HTML 或空体 */
const describeResponse = (data: unknown): string => {
  if (data === null || data === undefined || data === '') {
    return '空响应体';
  }
  if (typeof data === 'string') {
    return data.slice(0, 120);
  }
  try {
    const text = JSON.stringify(data);
    return text ? text.slice(0, 200) : '无法序列化的响应体';
  } catch {
    return '无法序列化的响应体';
  }
};

/** 401 统一入口：清除会话，模块级标志防并发重复 reLaunch */
export const handleUnauthorized = async (): Promise<void> => {
  clearSession();
  if (redirecting) {
    return;
  }
  redirecting = true;
  try {
    await wx.reLaunch({ url: '/pages/login/index' });
  } catch (error) {
    logError('request.handleUnauthorized', error);
  } finally {
    redirecting = false;
  }
};

interface Result<T> {
  code: number;
  message: string;
  data: T;
}

interface RequestOptions {
  url: string;
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE';
  data?: object;
  silent?: boolean;
}

/**
 * 剔除值为 undefined 的参数：wx.request 会把它序列化成字符串 "undefined"。
 * 只重建普通对象，数组等原样透传，避免改变它们的序列化结果。
 */
const compactData = (data?: object): object | undefined => {
  if (!data || Array.isArray(data) || Object.getPrototypeOf(data) !== Object.prototype) {
    return data;
  }
  const source = data as Record<string, unknown>;
  const compacted: Record<string, unknown> = {};
  for (const key of Object.keys(source)) {
    if (source[key] !== undefined) {
      compacted[key] = source[key];
    }
  }
  return compacted;
};

interface RawResponse {
  statusCode: number;
  data: unknown;
}

/**
 * 用 success/fail 回调显式封装 wx.request，不依赖「不传回调即返回 Promise」。
 * 开发者工具热重载（compileHotReLoad + useApiHook）后该行为可能失效而返回 RequestTask，
 * await 会把 RequestTask 当普通对象直接 resolve，表现为 Network 里请求成功、前端却拿到空响应。
 */
const sendRequest = (options: RequestOptions): Promise<RawResponse> =>
  new Promise((resolve, reject) => {
    wx.request({
      url: `${API_BASE}${options.url}`,
      method: options.method ?? 'GET',
      data: compactData(options.data),
      dataType: 'json',
      header: {
        'content-type': 'application/json',
        'fm-token': getToken(),
      },
      success: (res) => resolve({ statusCode: res.statusCode, data: res.data as unknown }),
      fail: (error) => reject(error),
    });
  });

/** dataType 未自动解析时 data 可能是字符串，做一次安全的 JSON 解析兜底 */
const parseResultData = (data: unknown): unknown => {
  if (typeof data !== 'string' || data.length === 0) {
    return data;
  }
  try {
    return JSON.parse(data);
  } catch {
    return data;
  }
};

export const request = async <T>(options: RequestOptions): Promise<T> => {
  let response: RawResponse;
  try {
    response = await sendRequest(options);
  } catch (error) {
    const message = (error as { errMsg?: string }).errMsg || '网络异常';
    logError('request', new Error(`${options.url} ${message}`));
    if (!options.silent) {
      wx.showToast({ title: message, icon: 'none' });
    }
    throw new Error(message);
  }
  const raw = parseResultData(response.data);
  const result = typeof raw === 'object' && raw !== null ? raw as Result<T> : undefined;
  if (response.statusCode === 401 || result?.code === 401) {
    // 登录接口的 401 是业务失败，交给登录页展示原因；统一重登会重建页面并冲掉错误提示
    if (!AUTH_FREE_URLS.includes(options.url)) {
      void handleUnauthorized();
    }
    throw new Error(result?.message ?? '登录已过期');
  }
  if (!result || result.code !== 0) {
    const status = response.statusCode;
    const message = result?.message ?? (status >= 400
      ? `后端返回 HTTP ${status}，请确认 fm-api 已启动完成且接口地址正确`
      : '响应格式异常');
    logError('request', new Error(`${options.url} ${message} [HTTP ${status}] ${describeResponse(raw)}`));
    if (!options.silent) {
      wx.showToast({ title: message, icon: 'none' });
    }
    throw new Error(message);
  }
  return result.data;
};

export const get = <T>(url: string, data?: object): Promise<T> =>
  request<T>({ url, data });

export const post = <T>(url: string, data?: object, silent = false): Promise<T> =>
  request<T>({ url, method: 'POST', data, silent });
