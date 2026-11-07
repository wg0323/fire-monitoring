/**
 * 小程序入口：恢复本地用户信息，并检查发布环境配置。
 * @date 2026-11-06
 */
import { RELEASE_CONFIG_ERROR } from './config/index';
import { getUser } from './store/user';
import { logError } from './utils/log';

App<IAppOption>({
  globalData: {
    user: getUser(),
  },
  onLaunch() {
    if (RELEASE_CONFIG_ERROR) {
      logError('config', new Error(RELEASE_CONFIG_ERROR));
      wx.showToast({ title: '正式 API 域名未配置', icon: 'none', duration: 5000 });
    }
    // 登录页面会校验 token；这里只恢复本地用户信息，避免首屏闪烁。
  },
});
