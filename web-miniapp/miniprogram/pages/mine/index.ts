/**
 * 个人中心：角色、租户/工作区/商户信息、清缓存与退出登录。
 * @date 2026-11-06
 */
import { fetchInfo, logout } from '../../services/auth';
import { getSubscribeConfig, recordSubscribe } from '../../services/notification';
import { clearSession, getUser, setUser, type UserInfo } from '../../store/user';
import { roleLabel } from '../../utils/format';
import { onShowRefresh, refreshTabBar, requireLogin } from '../../utils/guard';
import { logError } from '../../utils/log';

const confirm = async (title: string, content: string): Promise<boolean> => {
  try {
    const result = await wx.showModal({ title, content });
    return result.confirm;
  } catch (error) {
    logError('mine.confirm', error);
    return false;
  }
};

Page({
  data: {
    user: null as UserInfo | null,
    roleText: '',
    workAreaText: '',
    avatar: 'FM',
    showMerchantSwitch: false,
  },

  onShow() {
    if (!requireLogin()) {
      return;
    }
    refreshTabBar(this);
    onShowRefresh('mine', () => this.loadUser());
  },

  async loadUser() {
    const local = getUser();
    if (!local) {
      wx.reLaunch({ url: '/pages/login/index' });
      return;
    }
    try {
      const user = await fetchInfo();
      setUser(user);
      this.applyUser(user);
    } catch (error) {
      logError('mine.loadUser', error);
      this.applyUser(local);
    }
  },

  applyUser(user: UserInfo) {
    const app = getApp<IAppOption>();
    app.globalData.user = user;
    this.setData({
      user,
      roleText: roleLabel(user.roles?.[0]),
      workAreaText: user.workAreaNames?.join('、') ?? '',
      avatar: (user.realName || 'FM').slice(0, 1),
      showMerchantSwitch: (user.merchants?.length ?? 0) > 1,
    });
  },

  onSwitchMerchant() {
    wx.navigateTo({ url: '/pages/merchant/select' });
  },

  async onClearCache() {
    const confirmed = await confirm('清除缓存', '会清除本地登录态，需要重新登录。');
    if (!confirmed) {
      return;
    }
    clearSession();
    wx.reLaunch({ url: '/pages/login/index' });
  },

  onMessages() {
    wx.navigateTo({ url: '/pages/message/list' });
  },

  async onSubscribe() {
    try {
      const config = await getSubscribeConfig();
      const templateIds = [config.alarmTemplateId, config.dangerTemplateId]
        .filter((id): id is string => Boolean(id));
      if (!config.enabled || templateIds.length === 0) {
        wx.showToast({ title: '订阅模板未配置', icon: 'none' });
        return;
      }
      const result = await wx.requestSubscribeMessage({ tmplIds: templateIds });
      const accepted = templateIds.filter((id) => result[id] === 'accept');
      if (accepted.length === 0) {
        wx.showToast({ title: '未授权订阅', icon: 'none' });
        return;
      }
      await recordSubscribe(accepted);
      wx.showToast({ title: '订阅已开启', icon: 'success' });
    } catch (error) {
      logError('mine.onSubscribe', error);
      wx.showToast({ title: '订阅失败，请稍后重试', icon: 'none' });
    }
  },

  async onLogout() {
    const confirmed = await confirm('退出登录', '确认退出当前账号？');
    if (!confirmed) {
      return;
    }
    try {
      await logout();
    } catch (error) {
      logError('mine.onLogout', error);
    } finally {
      clearSession();
      wx.reLaunch({ url: '/pages/login/index' });
    }
  },
});
