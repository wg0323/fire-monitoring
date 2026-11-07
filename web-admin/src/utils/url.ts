/**
 * 外链安全校验：仅允许 http/https 绝对地址与站内相对路径，
 * 拒绝 javascript:/data:/vbscript: 等协议以及 //host 协议相对地址，避免 XSS。
 */

const HTTP_URL = /^https?:\/\//i;
// 形如 scheme: 的前缀（含 javascript:、data: 等）
const SCHEME = /^[a-z][a-z\d+.-]*:/i;

/**
 * 返回可安全用于 href/src 的地址；不合规时返回空字符串。
 */
export function safeUrl(value?: string | null): string | undefined {
  if (!value) {
    return undefined;
  }
  const url = value.trim();
  if (!url || url.startsWith('//')) {
    return undefined;
  }
  if (HTTP_URL.test(url)) {
    return url;
  }
  // 站内相对/绝对路径（以 /、./、?、# 或普通字符开头，且不含协议分隔符）
  if (!SCHEME.test(url)) {
    return url;
  }
  return undefined;
}
