/**
 * 隐患详情：时间线、整改/指派/审核操作与照片。
 * @date 2026-11-06
 */
import {
  assignDanger,
  auditDanger,
  dealDanger,
  getDanger,
  getDangerTimeline,
  listDangerAssignees,
} from '../../services/danger';
import type { DangerAssignee, DangerItem, DangerTimelineNode } from '../../services/types';
import { getUser } from '../../store/user';
import { FILE_DATA, fileUrl } from '../../utils/file';
import {
  dangerLevelText,
  dangerRecordStatusLabel,
  dangerSourceLabel,
  formatDate,
} from '../../utils/format';
import { markDirty, requireLogin } from '../../utils/guard';
import { getLocation } from '../../utils/location';
import { logError } from '../../utils/log';

interface TimelineCard {
  nodeKey: string;
  title: string;
  timeText: string;
  content: string;
  operator: string;
  extraText: string;
  occurred: boolean;
  failed: boolean;
  pics: string[];
}

const toTimeline = (node: DangerTimelineNode): TimelineCard => ({
  nodeKey: node.nodeKey,
  title: node.title,
  timeText: node.occurred ? formatDate(node.time) : '未发生',
  content: node.content || '',
  operator: node.operator || '',
  extraText: node.extra === 'AUTO' ? '自动转' : node.extra === 'MANUAL' ? '人工转' : '',
  occurred: node.occurred,
  failed: node.result === 2,
  pics: node.pics || [],
});

/** 详情头部与记录区的时间：有值统一格式化，无值保持空白 */
const timeText = (value?: string | null): string => (value ? formatDate(value) : '');

Page({
  data: {
    id: '',
    loading: false,
    ...FILE_DATA,
    detail: null as DangerItem | null,
    timeline: [] as TimelineCard[],
    statusText: '',
    statusClass: '',
    levelText: '',
    createTimeText: '',
    lastDealTimeText: '',
    auditTimeText: '',
    pics: [] as string[],
    dealPics: [] as string[],
    isAdmin: false,
    canDeal: false,
    canAssign: false,
    canAudit: false,
    panel: '',
    submitting: false,
    dealRemark: '',
    dealLocation: null as { lng: number; lat: number } | null,
    assignees: [] as DangerAssignee[],
    assigneeIndex: -1,
    assignDeadline: '',
    assignRemark: '',
    auditResult: 1,
    auditRemark: '',
  },

  onLoad(options: Record<string, string>) {
    if (!requireLogin()) {
      return;
    }
    this.setData({ id: options.id ?? '' });
    this.load();
  },

  async load() {
    if (!this.data.id) {
      return;
    }
    this.setData({ loading: true });
    try {
      const [detail, timeline] = await Promise.all([
        getDanger(this.data.id),
        getDangerTimeline(this.data.id),
      ]);
      this.applyDetail(detail, timeline.map(toTimeline));
    } catch (error) {
      logError('danger/detail.load', error);
      this.setData({ detail: null });
    } finally {
      this.setData({ loading: false });
    }
  },

  applyDetail(detail: DangerItem, timeline: TimelineCard[]) {
    const user = getUser();
    const role = user?.roles?.[0] ?? '';
    const isAdmin = role === 'TENANT_ADMIN';
    const isField = role === 'TENANT_FIELD';
    const isMerchant = role === 'MERCHANT';
    const canDeal = detail.status === 1
      && ((isField && detail.assigneeType === 'FIELD' && detail.assigneeId === user?.id)
        || (isMerchant && detail.assigneeType === 'MERCHANT'));
    this.setData({
      detail,
      timeline,
      statusText: dangerRecordStatusLabel(detail.status),
      statusClass: `status-${detail.status ?? 0}`,
      levelText: dangerLevelText(detail.dangerLevelFinal ?? detail.dangerLevel),
      createTimeText: timeText(detail.createTime),
      lastDealTimeText: timeText(detail.lastDealTime),
      auditTimeText: timeText(detail.auditTime),
      pics: [detail.pic1, detail.pic2, detail.pic3].filter((pic): pic is string => Boolean(pic)),
      isAdmin,
      canDeal,
      canAssign: isAdmin && detail.status === 0,
      canAudit: isAdmin && detail.status === 2,
    });
  },

  previewPic(event: WechatMiniprogram.TouchEvent) {
    const current = fileUrl(String(event.currentTarget.dataset.src ?? ''));
    wx.previewImage({ current, urls: this.data.pics.map(fileUrl) });
  },

  previewTimelinePic(event: WechatMiniprogram.TouchEvent) {
    const current = fileUrl(String(event.currentTarget.dataset.src ?? ''));
    const urls = this.data.timeline.reduce<string[]>((all, node) => [...all, ...node.pics], []).map(fileUrl);
    wx.previewImage({ current, urls });
  },

  openPanel(event: WechatMiniprogram.TouchEvent) {
    const panel = String(event.currentTarget.dataset.panel ?? '');
    this.setData({ panel });
    if (panel === 'deal') {
      this.setData({ dealRemark: '', dealPics: [], dealLocation: null });
    }
    if (panel === 'assign') {
      this.loadAssignees();
    }
    if (panel === 'audit') {
      this.setData({ auditResult: 1, auditRemark: '' });
    }
  },

  closePanel() {
    this.setData({ panel: '' });
  },

  noop() {
    return;
  },

  async loadAssignees() {
    try {
      const assignees = await listDangerAssignees(this.data.id);
      this.setData({ assignees, assigneeIndex: assignees.length > 0 ? 0 : -1 });
    } catch (error) {
      logError('danger/detail.loadAssignees', error);
      this.setData({ assignees: [], assigneeIndex: -1 });
    }
  },

  onDealPicsChange(event: WechatMiniprogram.CustomEvent<{ value: string[] }>) {
    this.setData({ dealPics: event.detail.value ?? [] });
  },

  async locate() {
    const location = await getLocation();
    if (!location) {
      wx.showToast({ title: '定位失败，可直接提交', icon: 'none' });
      return;
    }
    this.setData({ dealLocation: location });
    wx.showToast({ title: '已记录定位', icon: 'none' });
  },

  onAssigneeChange(event: WechatMiniprogram.PickerChange) {
    this.setData({ assigneeIndex: Number(event.detail.value) });
  },

  onDeadlineChange(event: WechatMiniprogram.PickerChange) {
    this.setData({ assignDeadline: String(event.detail.value) });
  },

  setAuditResult(event: WechatMiniprogram.TouchEvent) {
    this.setData({ auditResult: Number(event.currentTarget.dataset.result) });
  },

  onRemarkInput(event: WechatMiniprogram.Input) {
    const field = String(event.currentTarget.dataset.field ?? '');
    this.setData({ [field]: event.detail.value });
  },

  async submitDeal() {
    if (!this.data.dealRemark.trim()) {
      wx.showToast({ title: '请填写整改说明', icon: 'none' });
      return;
    }
    const [pic1, pic2, pic3] = this.data.dealPics;
    this.setData({ submitting: true });
    try {
      await dealDanger(this.data.id, {
        remark: this.data.dealRemark.trim(),
        pic1,
        pic2,
        pic3,
        lng: this.data.dealLocation?.lng,
        lat: this.data.dealLocation?.lat,
      });
      wx.showToast({ title: '已提交审核', icon: 'success' });
      this.setData({ panel: '' });
      markDirty('danger/list');
      await this.load();
    } catch (error) {
      logError('danger/detail.submit', error);
    } finally {
      this.setData({ submitting: false });
    }
  },

  async submitAssign() {
    const assignee = this.data.assignees[this.data.assigneeIndex];
    if (!assignee) {
      wx.showToast({ title: '请选择外勤', icon: 'none' });
      return;
    }
    this.setData({ submitting: true });
    try {
      await assignDanger(this.data.id, {
        assigneeId: assignee.id,
        dealLimit: this.data.assignDeadline || undefined,
        remark: this.data.assignRemark || undefined,
      });
      wx.showToast({ title: '已指派', icon: 'success' });
      this.setData({ panel: '' });
      markDirty('danger/list');
      await this.load();
    } catch (error) {
      logError('danger/detail.submit', error);
    } finally {
      this.setData({ submitting: false });
    }
  },

  async submitAudit() {
    if (this.data.auditResult === 4 && !this.data.auditRemark.trim()) {
      wx.showToast({ title: '驳回必须填写原因', icon: 'none' });
      return;
    }
    this.setData({ submitting: true });
    try {
      await auditDanger(this.data.id, {
        auditResult: this.data.auditResult,
        auditRemark: this.data.auditRemark.trim() || undefined,
      });
      wx.showToast({ title: this.data.auditResult === 4 ? '已驳回' : '审核通过', icon: 'success' });
      this.setData({ panel: '' });
      markDirty('danger/list');
      await this.load();
    } catch (error) {
      logError('danger/detail.submit', error);
    } finally {
      this.setData({ submitting: false });
    }
  },
});
