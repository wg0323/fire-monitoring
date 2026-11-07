import axios, {
  type AxiosError,
  type AxiosRequestConfig,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios';
import { ElMessage } from 'element-plus';
import type { Result } from '@/types/api';

export const TOKEN_KEY = 'fm-admin-token';

interface RequestConfig extends InternalAxiosRequestConfig {
  requestKey?: string;
  requestId?: number;
}

interface PendingEntry {
  id: number;
  controller: AbortController;
}

// 仅写请求进入防重表；GET 由 http.get 复用在途 Promise 去重，避免轮询与手动查询互相取消
const pending = new Map<string, PendingEntry>();
const getInflight = new Map<string, Promise<unknown>>();
const WRITE_METHODS = new Set(['post', 'put', 'delete']);
let requestSeq = 0;

const service = axios.create({
  baseURL: '/api/admin',
  timeout: 15000,
});

function buildRequestKey(config: AxiosRequestConfig): string {
  return [
    config.method ?? 'get',
    config.url ?? '',
    JSON.stringify(config.params ?? {}),
    JSON.stringify(config.data ?? {}),
  ].join('|');
}

function isLoginRequest(url?: string): boolean {
  return Boolean(url && url.includes('/auth/login'));
}

// 401 并发保护：多个在途请求同时返回 401 时只清理并跳转一次
let unauthorizedRedirecting = false;

async function clearTokenAndRedirect(): Promise<void> {
  if (unauthorizedRedirecting) {
    return;
  }
  unauthorizedRedirecting = true;
  try {
    // 动态 import 打断 request -> store/router -> request 的循环依赖
    const [{ useAuthStore }, { default: router }] = await Promise.all([
      import('@/stores/auth'),
      import('@/router'),
    ]);
    useAuthStore().clear();
    if (router.currentRoute.value.path !== '/login') {
      await router.push('/login');
    }
  } catch (error) {
    // 动态加载或跳转失败时保留已清理状态，记录便于排查，不吞掉错误
    console.error('[auth] 401 跳转失败', error);
  } finally {
    unauthorizedRedirecting = false;
  }
}

// 403 统一提示并触发权限信息刷新；节流 + 单次刷新标志避免 403 风暴
let permissionRefreshing = false;
let lastForbiddenAt = 0;
const FORBIDDEN_COOLDOWN = 5000;

function refreshPermissions(): void {
  if (permissionRefreshing) {
    return;
  }
  permissionRefreshing = true;
  void import('@/stores/auth')
    .then(({ useAuthStore }) => useAuthStore().loadInfo())
    .catch(() => {
      // 刷新失败不阻塞当前操作，后续请求或重新登录会再次触发
    })
    .finally(() => {
      permissionRefreshing = false;
    });
}

function handleForbidden(url?: string): void {
  const now = Date.now();
  if (now - lastForbiddenAt < FORBIDDEN_COOLDOWN) {
    return;
  }
  lastForbiddenAt = now;
  ElMessage.error('无权限执行该操作，权限信息已刷新，请重试');
  // /auth/info 自身的 403 不再触发刷新，避免递归
  if (!url?.includes('/auth/info')) {
    refreshPermissions();
  }
}

service.interceptors.request.use((config) => {
  const requestConfig = config as RequestConfig;
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    requestConfig.headers.set('fm-token', token);
  }
  // GET 不参与 abort 防重：轮询与手动查询互不取消，改由 http.get 复用在途 Promise
  if (!WRITE_METHODS.has((requestConfig.method ?? 'get').toLowerCase())) {
    return requestConfig;
  }
  const key = buildRequestKey(requestConfig);
  const previous = pending.get(key);
  if (previous) {
    previous.controller.abort();
  }
  const controller = new AbortController();
  const id = ++requestSeq;
  requestConfig.requestKey = key;
  requestConfig.requestId = id;
  requestConfig.signal = controller.signal;
  pending.set(key, { id, controller });
  return requestConfig;
});

function removePending(config?: RequestConfig): void {
  if (!config?.requestKey) {
    return;
  }
  // 校验身份：被中止的旧请求不能删掉新请求登记的记录
  const entry = pending.get(config.requestKey);
  if (entry && entry.id === config.requestId) {
    pending.delete(config.requestKey);
  }
}

async function resolveErrorMessage(error: AxiosError<Result<unknown>>): Promise<string> {
  const data = error.response?.data as unknown;
  if (data instanceof Blob) {
    try {
      const parsed = JSON.parse(await data.text()) as Result<unknown>;
      return parsed.message || '请求失败';
    } catch {
      return '请求失败';
    }
  }
  return (data as Result<unknown> | undefined)?.message || error.message || '网络异常';
}

service.interceptors.response.use(
  (response: AxiosResponse<Result<unknown>>) => {
    removePending(response.config as RequestConfig);
    const result = response.data;
    if (result && typeof result.code === 'number' && result.code !== 0) {
      if (result.code === 401 && !isLoginRequest(response.config.url)) {
        void clearTokenAndRedirect();
      } else if (result.code === 403) {
        handleForbidden(response.config.url);
      } else if (!isLoginRequest(response.config.url)) {
        ElMessage.error(result.message || '请求失败');
      }
      return Promise.reject(new Error(result.message || '请求失败'));
    }
    return response;
  },
  async (error: AxiosError<Result<unknown>>) => {
    removePending(error.config as RequestConfig | undefined);
    if (axios.isCancel(error)) {
      return Promise.reject(error);
    }
    const status = error.response?.status;
    const message = await resolveErrorMessage(error);
    if (status === 401 && !isLoginRequest(error.config?.url)) {
      void clearTokenAndRedirect();
    } else if (status === 403) {
      handleForbidden(error.config?.url);
    } else if (!isLoginRequest(error.config?.url)) {
      ElMessage.error(message);
    }
    return Promise.reject(error);
  },
);

export const http = {
  get<T, P = object>(url: string, params?: P): Promise<T> {
    const key = buildRequestKey({ method: 'get', url, params });
    const inflight = getInflight.get(key);
    if (inflight) {
      // 同 key 的 GET 复用同一个 Promise，轮询与手动查询不再互相取消
      return inflight as Promise<T>;
    }
    const request = service
      .get<Result<T>>(url, { params })
      .then((response) => response.data.data)
      .finally(() => {
        getInflight.delete(key);
      });
    getInflight.set(key, request);
    return request;
  },
  post<T>(url: string, data?: unknown): Promise<T> {
    return service.post<Result<T>>(url, data).then((response) => response.data.data);
  },
  put<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return service.put<Result<T>>(url, data, config).then((response) => response.data.data);
  },
  delete<T, P = object>(url: string, params?: P): Promise<T> {
    return service.delete<Result<T>>(url, { params }).then((response) => response.data.data);
  },
};

function parseFileName(disposition?: string): string | undefined {
  if (!disposition) {
    return undefined;
  }
  const decode = (value: string): string => {
    try {
      return decodeURIComponent(value);
    } catch {
      // 非法转义序列时返回原文，避免抛错中断下载
      return value;
    }
  };
  const utf8 = /filename\*=UTF-8''([^;]+)/i.exec(disposition);
  if (utf8) {
    return decode(utf8[1]);
  }
  const plain = /filename="?([^";]+)"?/i.exec(disposition);
  return plain ? decode(plain[1]) : undefined;
}

export async function download<P = object>(
  url: string,
  params?: P,
  fallbackName = 'download.xlsx',
): Promise<void> {
  const response = await service.get<Blob>(url, { params, responseType: 'blob' });
  const blob = response.data;
  // 后端出错时常以 200 + JSON 错误体返回，此时 blob.type 为 application/json
  if (blob.type.includes('json')) {
    let message = '下载失败';
    try {
      const parsed = JSON.parse(await blob.text()) as Result<unknown>;
      message = parsed.message || message;
    } catch {
      // 解析失败时沿用默认提示
    }
    return Promise.reject(new Error(message));
  }
  const objectUrl = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = objectUrl;
  link.download = parseFileName(response.headers['content-disposition']) ?? fallbackName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  // 延后释放，避免部分浏览器在触发点击后立即回收导致下载失败
  window.setTimeout(() => {
    URL.revokeObjectURL(objectUrl);
  }, 0);
}
