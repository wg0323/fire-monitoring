import type { AmapNamespace } from '@/types/amap';

const SCRIPT_ID = 'fm-amap-script';

let loader: Promise<AmapNamespace> | null = null;

/**
 * 单例加载高德 JS API 2.0；未配置 Key 或加载失败时抛错，由地图组件降级。
 */
export function loadAmap(): Promise<AmapNamespace> {
  if (loader) {
    return loader;
  }
  loader = new Promise<AmapNamespace>((resolve, reject) => {
    const key = import.meta.env.VITE_AMAP_KEY;
    if (!key) {
      reject(new Error('AMAP_KEY_MISSING'));
      return;
    }
    if (window.AMap) {
      resolve(window.AMap);
      return;
    }
    const securityCode = import.meta.env.VITE_AMAP_SECURITY_CODE;
    if (securityCode) {
      window._AMapSecurityConfig = { securityJsCode: securityCode };
    }
    const script = document.createElement('script');
    script.id = SCRIPT_ID;
    script.async = true;
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${encodeURIComponent(key)}&plugin=AMap.Geocoder`;
    script.onload = () => {
      if (window.AMap) {
        resolve(window.AMap);
        return;
      }
      reject(new Error('AMAP_LOAD_FAILED'));
    };
    script.onerror = () => reject(new Error('AMAP_LOAD_FAILED'));
    document.head.appendChild(script);
  });
  return loader;
}
