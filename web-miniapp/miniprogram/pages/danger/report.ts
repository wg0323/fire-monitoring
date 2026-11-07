/**
 * 随手拍：选择商户、隐患类型、描述、照片与定位，提交后进入商户整改。
 * @date 2026-11-06
 */
import { listDangerTypes, listMerchantOptions } from '../../services/device';
import { reportDanger } from '../../services/danger';
import type { DictItem, MerchantOption } from '../../services/types';
import { getUser } from '../../store/user';
import { markDirty, requireLogin } from '../../utils/guard';
import { getLocation } from '../../utils/location';
import { logError } from '../../utils/log';

const SEARCH_DEBOUNCE = 300;
let merchantSearchTimer: number | undefined;

Page({
  data: {
    isMerchant: false,
    merchants: [] as MerchantOption[],
    selectedMerchant: null as MerchantOption | null,
    merchantKeyword: '',
    merchantPanel: false,
    types: [] as DictItem[],
    selectedType: '',
    remark: '',
    pics: [] as string[],
    location: null as { lng: number; lat: number } | null,
    submitting: false,
  },

  onLoad() {
    if (!requireLogin()) {
      return;
    }
    const isMerchant = getUser()?.roles?.includes('MERCHANT') ?? false;
    this.setData({ isMerchant });
    this.loadMerchants();
    this.loadTypes();
  },

  onUnload() {
    if (merchantSearchTimer !== undefined) {
      clearTimeout(merchantSearchTimer);
      merchantSearchTimer = undefined;
    }
  },

  async loadMerchants(keyword?: string) {
    try {
      const merchants = await listMerchantOptions(keyword);
      const selected = this.data.selectedMerchant
        ?? (this.data.isMerchant ? merchants[0] ?? null : null);
      this.setData({ merchants, selectedMerchant: selected });
    } catch (error) {
      logError('danger/report.loadMerchants', error);
      this.setData({ merchants: [] });
    }
  },

  async loadTypes() {
    try {
      const types = await listDangerTypes();
      this.setData({ types });
    } catch (error) {
      logError('danger/report.loadTypes', error);
      this.setData({ types: [] });
    }
  },

  openMerchantPanel() {
    if (this.data.isMerchant) {
      return;
    }
    this.setData({ merchantPanel: true });
  },

  closeMerchantPanel() {
    this.setData({ merchantPanel: false });
  },

  noop() {
    return;
  },

  onMerchantSearch(event: WechatMiniprogram.Input) {
    const merchantKeyword = event.detail.value;
    this.setData({ merchantKeyword });
    if (merchantSearchTimer !== undefined) {
      clearTimeout(merchantSearchTimer);
    }
    merchantSearchTimer = setTimeout(() => {
      this.loadMerchants(merchantKeyword);
    }, SEARCH_DEBOUNCE);
  },

  selectMerchant(event: WechatMiniprogram.TouchEvent) {
    const id = String(event.currentTarget.dataset.id ?? '');
    const selectedMerchant = this.data.merchants.find((item) => item.id === id) ?? null;
    this.setData({ selectedMerchant, merchantPanel: false });
  },

  selectType(event: WechatMiniprogram.TouchEvent) {
    const code = String(event.currentTarget.dataset.code ?? '');
    this.setData({ selectedType: this.data.selectedType === code ? '' : code });
  },

  onRemarkInput(event: WechatMiniprogram.Input) {
    this.setData({ remark: event.detail.value });
  },

  onPicsChange(event: WechatMiniprogram.CustomEvent<{ value: string[] }>) {
    this.setData({ pics: event.detail.value ?? [] });
  },

  async locate() {
    const location = await getLocation();
    if (!location) {
      wx.showToast({ title: '定位失败，可直接提交', icon: 'none' });
      return;
    }
    this.setData({ location });
    wx.showToast({ title: '已记录定位', icon: 'none' });
  },

  async submit() {
    if (!this.data.selectedMerchant) {
      wx.showToast({ title: '请选择商户', icon: 'none' });
      return;
    }
    if (!this.data.selectedType) {
      wx.showToast({ title: '请选择隐患类型', icon: 'none' });
      return;
    }
    if (!this.data.remark.trim()) {
      wx.showToast({ title: '请填写隐患描述', icon: 'none' });
      return;
    }
    const [pic1, pic2, pic3] = this.data.pics;
    this.setData({ submitting: true });
    try {
      await reportDanger({
        merchantId: this.data.selectedMerchant.id,
        dangerType: this.data.selectedType,
        remark: this.data.remark.trim(),
        pic1,
        pic2,
        pic3,
        lng: this.data.location?.lng,
        lat: this.data.location?.lat,
      });
      wx.showToast({ title: '已提交给商户', icon: 'success' });
      markDirty('danger/list');
      wx.navigateBack();
    } catch (error) {
      logError('danger/report.submit', error);
    } finally {
      this.setData({ submitting: false });
    }
  },
});
