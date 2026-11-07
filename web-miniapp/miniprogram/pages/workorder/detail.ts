/**
 * 工单详情：接单与完成，完成时提交结论并收口报警。
 * @date 2026-11-06
 */
import { acceptWorkOrder, finishWorkOrder, getWorkOrder } from '../../services/workorder';
import type { WorkOrderItem } from '../../services/types';
import { FILE_DATA, fileUrl } from '../../utils/file';
import {
  alarmResultLabel,
  dangerStatusLabel,
  formatDate,
  remainText,
  workOrderStatusLabel,
} from '../../utils/format';
import { markDirty, onShowRefresh, requireLogin } from '../../utils/guard';
import { logError } from '../../utils/log';

interface OrderDetail {
  id: string;
  merchantName: string;
  alarmSn: string;
  alarmTimeText: string;
  alarmResultText: string;
  workAreaName: string;
  handlerName: string;
  status: number;
  statusText: string;
  acceptTimeText: string;
  finishTimeText: string;
  content: string;
  finishRemark: string;
  photos: string[];
  dangerText: string;
  createTimeText: string;
}

let currentId = '';

const toDetail = (order: WorkOrderItem): OrderDetail => {
  let dangerText = dangerStatusLabel(order.dangerStatus);
  if (order.dangerStatus === 'PENDING') {
    dangerText = `待转隐患 ${remainText(order.dangerRemainSeconds)}`;
  }
  return {
    id: order.id,
    merchantName: order.merchantName ?? '-',
    alarmSn: order.alarmSn ?? '-',
    alarmTimeText: formatDate(order.alarmTime),
    alarmResultText: alarmResultLabel(order.alarmResult),
    workAreaName: order.workAreaName ?? '-',
    handlerName: order.handlerName ?? '-',
    status: order.status,
    statusText: workOrderStatusLabel(order.status),
    acceptTimeText: formatDate(order.acceptTime),
    finishTimeText: formatDate(order.finishTime),
    content: order.content ?? '-',
    finishRemark: order.finishRemark ?? '-',
    photos: [order.finishPic1, order.finishPic2, order.finishPic3]
      .filter((pic): pic is string => Boolean(pic)),
    dangerText,
    createTimeText: formatDate(order.createTime),
  };
};

Page({
  data: {
    detail: null as OrderDetail | null,
    ...FILE_DATA,
    loading: false,
    canAccept: false,
    canFinish: false,
    panelVisible: false,
    finishResult: 1,
    finishTitle: '真实火警',
    remark: '',
    pics: [] as string[],
    submitting: false,
  },

  onLoad(options: Record<string, string | undefined>) {
    currentId = options.id ?? '';
  },

  onShow() {
    if (!requireLogin()) {
      return;
    }
    onShowRefresh('workorder/detail', () => this.load());
  },

  async load() {
    if (!currentId) {
      return;
    }
    this.setData({ loading: true });
    try {
      const order = await getWorkOrder(currentId);
      this.setData({
        detail: toDetail(order),
        canAccept: order.status === 0,
        canFinish: order.status === 1,
        loading: false,
      });
    } catch (error) {
      logError('workorder/detail.load', error);
      this.setData({ loading: false });
    }
  },

  async accept() {
    if (!this.data.detail || this.data.submitting) {
      return;
    }
    this.setData({ submitting: true });
    try {
      await acceptWorkOrder(this.data.detail.id);
      wx.showToast({ title: '已接单', icon: 'success' });
      markDirty('workorder/list');
      await this.load();
    } catch (error) {
      logError('workorder/detail.submit', error);
    } finally {
      this.setData({ submitting: false });
    }
  },

  openFinish(event: WechatMiniprogram.TouchEvent) {
    const result = Number(event.currentTarget.dataset.result ?? 1);
    const titles: Record<number, string> = { 0: '误报', 1: '真实火警' };
    this.setData({
      panelVisible: true,
      finishResult: result,
      finishTitle: titles[result] ?? '结论',
      remark: '',
      pics: [],
    });
  },

  closePanel() {
    this.setData({ panelVisible: false });
  },

  noop() {
    // 阻止蒙层点击穿透
  },

  onRemarkInput(event: WechatMiniprogram.Input) {
    this.setData({ remark: event.detail.value });
  },

  onPicsChange(event: WechatMiniprogram.CustomEvent<{ value: string[] }>) {
    this.setData({ pics: event.detail.value ?? [] });
  },

  previewPhoto(event: WechatMiniprogram.TouchEvent) {
    const current = fileUrl(String(event.currentTarget.dataset.src ?? ''));
    wx.previewImage({ current, urls: (this.data.detail?.photos ?? []).map(fileUrl) });
  },

  async submitFinish() {
    if (!this.data.detail || this.data.submitting) {
      return;
    }
    const [pic1, pic2, pic3] = this.data.pics;
    this.setData({ submitting: true });
    try {
      await finishWorkOrder(this.data.detail.id, {
        result: this.data.finishResult,
        remark: this.data.remark || undefined,
        pic1,
        pic2,
        pic3,
      });
      wx.showToast({ title: '工单已完成', icon: 'none' });
      this.setData({ panelVisible: false });
      markDirty('workorder/list');
      await this.load();
    } catch (error) {
      logError('workorder/detail.submit', error);
    } finally {
      this.setData({ submitting: false });
    }
  },
});
