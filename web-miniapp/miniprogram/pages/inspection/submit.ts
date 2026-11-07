/**
 * 巡查结果提交：正常/隐患，发现隐患时同步生成商户整改隐患。
 * @date 2026-11-06
 */
import { listDangerTypes } from '../../services/device';
import { submitInspectionResult } from '../../services/inspection';
import type { DictItem } from '../../services/types';
import { markDirty, requireLogin } from '../../utils/guard';
import { getLocation } from '../../utils/location';
import { logError } from '../../utils/log';

Page({
  data: {
    planId: '',
    merchantId: '',
    merchantName: '',
    result: 0,
    types: [] as DictItem[],
    dangerType: '',
    remark: '',
    pics: [] as string[],
    location: null as { lng: number; lat: number } | null,
    submitting: false,
  },

  onLoad(options: Record<string, string>) {
    if (!requireLogin()) {
      return;
    }
    this.setData({
      planId: options.planId ?? '',
      merchantId: options.merchantId ?? '',
      merchantName: decodeURIComponent(options.merchantName ?? ''),
    });
    this.loadTypes();
  },

  async loadTypes() {
    try {
      const types = await listDangerTypes();
      this.setData({ types });
    } catch (error) {
      logError('inspection/submit.loadTypes', error);
      this.setData({ types: [] });
    }
  },

  selectResult(event: WechatMiniprogram.TouchEvent) {
    this.setData({ result: Number(event.currentTarget.dataset.result) });
  },

  selectType(event: WechatMiniprogram.TouchEvent) {
    const code = String(event.currentTarget.dataset.code ?? '');
    this.setData({ dangerType: this.data.dangerType === code ? '' : code });
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
    if (this.data.result === 1 && !this.data.dangerType) {
      wx.showToast({ title: '请选择隐患类型', icon: 'none' });
      return;
    }
    if (this.data.result === 1 && !this.data.remark.trim()) {
      wx.showToast({ title: '请描述发现的隐患', icon: 'none' });
      return;
    }
    const [pic1, pic2, pic3] = this.data.pics;
    this.setData({ submitting: true });
    try {
      await submitInspectionResult({
        planId: this.data.planId,
        merchantId: this.data.merchantId,
        result: this.data.result,
        dangerType: this.data.result === 1 ? this.data.dangerType : undefined,
        remark: this.data.remark.trim() || undefined,
        pic1,
        pic2,
        pic3,
        lng: this.data.location?.lng,
        lat: this.data.location?.lat,
      });
      wx.showToast({ title: '提交成功', icon: 'success' });
      markDirty('inspection/detail');
      setTimeout(() => wx.navigateBack(), 600);
    } catch (error) {
      logError('inspection/submit.submit', error);
    } finally {
      this.setData({ submitting: false });
    }
  },
});
