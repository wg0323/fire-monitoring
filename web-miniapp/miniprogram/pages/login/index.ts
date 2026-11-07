/**
 * 登录页：微信手机号授权 + 开发环境手机号白名单登录。
 * @date 2026-11-06
 */
import { fetchInfo, phoneLogin } from '../../services/auth';
import { getToken, getUser, setToken, setUser } from '../../store/user';
import { logError } from '../../utils/log';

Page({
  data: {
    phone: '',
    loading: false,
    error: '',
    isDev: false,
  },

  onLoad() {
    if (getUser() && getToken()) {
      wx.switchTab({ url: '/pages/home/index' });
      return;
    }
    const account = wx.getAccountInfoSync();
    this.setData({ isDev: account.miniProgram.envVersion !== 'release' });
  },

  onPhoneChange(event: WechatMiniprogram.CustomEvent) {
    this.setData({ phone: String(event.detail ?? '') });
  },

  onGetPhoneNumber(event: WechatMiniprogram.CustomEvent) {
    const detail = event.detail as { code?: string; errMsg?: string };
    if (!detail.code) {
      this.setData({ error: '微信手机号授权失败，请重试' });
      return;
    }
    this.login({ code: detail.code });
  },

  onDevLogin() {
    const phone = this.data.phone.trim();
    if (!/^1\d{10}$/.test(phone)) {
      this.setData({ error: '请输入正确的 11 位手机号' });
      return;
    }
    this.login({ phone });
  },

  async login(payload: { code?: string; phone?: string }) {
    if (this.data.loading) {
      return;
    }
    this.setData({ loading: true, error: '' });
    try {
      const result = await phoneLogin(payload);
      setToken(result.tokenValue);
      const user = await fetchInfo();
      setUser(user);
      const app = getApp<IAppOption>();
      app.globalData.user = user;
      if ((user.merchants?.length ?? 0) > 1) {
        wx.redirectTo({ url: '/pages/merchant/select' });
        return;
      }
      wx.switchTab({ url: '/pages/home/index' });
    } catch (error) {
      logError('login.login', error);
      this.setData({ error: error instanceof Error ? error.message : '登录失败，请稍后重试' });
    } finally {
      this.setData({ loading: false });
    }
  },
});
