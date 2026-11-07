/**
 * 环境配置：按小程序版本映射 dev/trial/release 三套 API 基址。
 * services 中的请求路径均相对 /api/mp，不要再拼 /mp 前缀。
 * @date 2026-11-06
 */

/** 发布前填写：正式/体验环境的 HTTPS 网关，必须以 /api/mp 结尾 */
export const RELEASE_API_BASE = 'https://RELEASE-API-BASE-NOT-SET.invalid/api/mp';

const DEV_API_BASE = 'http://127.0.0.1:8080/api/mp';
const PLACEHOLDER = 'RELEASE-API-BASE-NOT-SET';
const ENV_VERSION = wx.getAccountInfoSync().miniProgram.envVersion;

/** release/trial 未填写正式域名时给出明确错误，绝不回落到 127.0.0.1 */
export const RELEASE_CONFIG_ERROR: string | null = ENV_VERSION !== 'develop' && RELEASE_API_BASE.includes(PLACEHOLDER)
  ? 'RELEASE_API_BASE 未填写，请在 miniprogram/config/index.ts 配置正式 HTTPS 域名'
  : null;

const resolveApiBase = (): string => {
  if (ENV_VERSION === 'develop') {
    return DEV_API_BASE;
  }
  if (RELEASE_CONFIG_ERROR) {
    console.error(`[config] ${RELEASE_CONFIG_ERROR}`);
  }
  return RELEASE_API_BASE;
};

export const API_BASE = resolveApiBase();
/** 图片等静态资源基址：后端本地存储返回 /uploads/... 相对路径，需补上服务根地址 */
export const FILE_BASE = API_BASE.replace(/\/api(\/.*)?$/, '');
export const APP_VERSION = '1.0.0-P2';
