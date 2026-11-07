/**
 * 定位：统一授权预检与 Promise 化调用，巡查/隐患/随手拍三处共用。
 * @date 2026-11-06
 */
import { logError } from './log';

export interface GeoPoint {
  lng: number;
  lat: number;
}

/** 授权被明确拒绝时给明确提示；未询问过则交给 getLocation 弹系统授权框 */
const ensureAuthorized = async (): Promise<boolean> => {
  try {
    const setting = await wx.getSetting();
    return setting.authSetting['scope.userLocation'] !== false;
  } catch (error) {
    logError('location.getSetting', error);
    return true;
  }
};

export const getLocation = async (): Promise<GeoPoint | null> => {
  if (!(await ensureAuthorized())) {
    // 用模态提示，避免被调用方后续的通用 toast 覆盖
    wx.showModal({
      title: '需要位置权限',
      content: '记录巡查与隐患现场位置需要位置信息，请在「设置」中开启后重试。',
      showCancel: false,
    });
    return null;
  }
  try {
    const result = await wx.getLocation({ type: 'gcj-02' });
    return { lng: result.longitude, lat: result.latitude };
  } catch (error) {
    logError('location.getLocation', error);
    return null;
  }
};
