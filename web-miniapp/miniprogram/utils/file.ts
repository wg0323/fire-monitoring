/**
 * 图片地址归一：本地存储上传返回 /uploads/... 相对路径，小程序没有同源概念，
 * 直接用作 image 的 src 会被解析成开发者工具 pageframe 地址而 404。
 * @date 2026-11-06
 */
import { FILE_BASE } from '../config/index';

/** 渲染层 WXS 与页面共用的唯一 base 注入点：换域名只需改 config/index.ts */
export { FILE_BASE } from '../config/index';
export const FILE_DATA: { fileBase: string } = { fileBase: FILE_BASE };

export const fileUrl = (path?: string | null): string => {
  if (!path) {
    return '';
  }
  // MinIO 等已是绝对地址、以及本地临时文件，原样返回
  if (/^(https?:)?\/\//i.test(path) || path.startsWith('wxfile://')) {
    return path;
  }
  return path.startsWith('/') ? `${FILE_BASE}${path}` : `${FILE_BASE}/${path}`;
};
