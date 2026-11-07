/**
 * 请求封装：token、Result 解包、401 重登、错误提示。
 * @date 2026-11-06
 */
import { API_BASE } from '../config/index';
import { clearSession, getToken } from '../store/user';
import { logError } from '../utils/log';

let redirecting = false;

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

export const request = async <T>(options: RequestOptions): Promise<T> => {
  let response: WechatMiniprogram.RequestSuccessCallbackResult<Result<T>>;
  try {
    // 基础库 3.4.10 不传 success/fail 返回 Promise，类型定义仍声明为 RequestTask，故显式断言
    response = await (wx.request<Result<T>>({
      url: `${API_BASE}${options.url}`,
      method: options.method ?? 'GET',
      data: compactData(options.data),
      header: {
        'content-type': 'application/json',
        'fm-token': getToken(),
      },
    }) as unknown as Promise<WechatMiniprogram.RequestSuccessCallbackResult<Result<T>>>);
  } catch (error) {
    const message = (error as { errMsg?: string }).errMsg || '网络异常';
    logError('request', error);
    if (!options.silent) {
      wx.showToast({ title: message, icon: 'none' });
    }
    throw new Error(message);
  }
  const result = response.data;
  if (response.statusCode === 401 || result?.code === 401) {
    void handleUnauthorized();
    throw new Error(result?.message ?? '登录已过期');
  }
  if (!result || result.code !== 0) {
    const message = result?.message ?? '请求失败';
    logError('request', new Error(message));
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
