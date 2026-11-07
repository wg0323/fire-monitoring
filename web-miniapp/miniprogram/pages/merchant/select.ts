/**
 * 选店页：多门店账号登录后选择进入哪家门店，也可从「我的」进入切换。
 * @date 2026-11-06
 */
import { fetchInfo, switchMerchant } from '../../services/auth';
import { getUser, setUser, type MerchantOption, type UserInfo } from '../../store/user';
import { requireLogin } from '../../utils/guard';
import { logError } from '../../utils/log';

Page({
  data: {
    merchants: [] as MerchantOption[],
    currentId: '',
    loading: false,
  },

  onLoad() {
    if (!requireLogin()) {
      return;
    }
    this.loadMerchants();
  },

  async loadMerchants() {
    const local = getUser();
    if (local) {
      this.applyUser(local);
    }
    try {
      const user = await fetchInfo();
      setUser(user);
      this.applyUser(user);
    } catch (error) {
      logError('merchant/select.loadMerchants', error);
      // 接口失败时沿用本地缓存的门店列表
    }
  },

  applyUser(user: UserInfo) {
    this.setData({
      merchants: user.merchants ?? [],
      currentId: user.merchantId ?? '',
    });
  },

  async onSelect(event: WechatMiniprogram.CustomEvent) {
    const merchantId = String(event.currentTarget.dataset.id ?? '');
    if (!merchantId || this.data.loading) {
      return;
    }
    if (merchantId === this.data.currentId) {
      wx.switchTab({ url: '/pages/home/index' });
      return;
    }
    this.setData({ loading: true });
    try {
      await switchMerchant(merchantId);
      const user = await fetchInfo();
      setUser(user);
      getApp<IAppOption>().globalData.user = user;
      wx.switchTab({ url: '/pages/home/index' });
    } catch (error) {
      logError('merchant/select.onSelect', error);
      wx.showToast({ title: error instanceof Error ? error.message : '切换失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },
});
