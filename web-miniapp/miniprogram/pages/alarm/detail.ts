/**
 * 报警详情：设备信息、时间线，商户可提交火警/误报处置；故障由设备上报并走隐患流程。
 * @date 2026-11-06
 */
import { getAlarm, getAlarmTimeline, merchantHandle } from '../../services/alarm';
import type { AlarmItem, AlarmTraceItem } from '../../services/types';
import { getUser } from '../../store/user';
import { FILE_DATA, fileUrl } from '../../utils/file';
import {
  alarmLevelClass,
  alarmLevelLabel,
  alarmResultLabel,
  formatDate,
  remainText,
  traceTypeLabel,
} from '../../utils/format';
import { onShowRefresh, requireLogin } from '../../utils/guard';
import { logError } from '../../utils/log';

interface DetailView {
  id: string;
  sn: string;
  merchantName: string;
  deviceTypeName: string;
  position: string;
  alarmTimeText: string;
  levelText: string;
  levelClass: string;
  statusText: string;
  resultText: string;
  faultText: string;
  snapshotUrl: string;
  addRemark: string;
  photos: string[];
  handlerName: string;
  finalizeTimeText: string;
  isFault: boolean;
}

interface TraceView {
  text: string;
  desc: string;
}

let currentId = '';

const isFaultAlarm = (alarm: AlarmItem): boolean => alarm.result !== 0
  && (alarm.alarmLevel === 3 || alarm.alarmType === 'FAULT' || alarm.result === 2);

const toDetail = (alarm: AlarmItem): DetailView => {
  let statusText = alarm.status === 1 ? '已处理' : '未处理';
  if (alarm.status === 0 && alarm.verifyStatus === 1 && alarm.handleSource === 'MERCHANT') {
    statusText = '待值守复核';
  }
  let faultText = '';
  if (alarm.dangerId) {
    faultText = '已转隐患';
  } else if (alarm.selfRecovered) {
    faultText = '已自行恢复，不转隐患';
  } else if (alarm.dangerConvertDeadline) {
    faultText = `待转隐患 ${remainText(alarm.dangerRemainSeconds)}`;
  }
  return {
    id: alarm.id,
    sn: alarm.sn,
    merchantName: alarm.merchantName ?? '-',
    deviceTypeName: alarm.deviceTypeName ?? alarm.deviceType ?? '-',
    position: alarm.position ?? '-',
    alarmTimeText: formatDate(alarm.alarmTime),
    levelText: alarmLevelLabel(alarm.alarmLevel),
    levelClass: alarmLevelClass(alarm.alarmLevel),
    statusText,
    resultText: alarmResultLabel(alarm.result),
    faultText,
    snapshotUrl: alarm.snapshotUrl ?? '',
    addRemark: alarm.addRemark ?? '',
    photos: [alarm.addPic1, alarm.addPic2, alarm.addPic3]
      .filter((pic): pic is string => Boolean(pic)),
    handlerName: alarm.handlerName ?? '-',
    finalizeTimeText: formatDate(alarm.finalizeTime),
    isFault: isFaultAlarm(alarm),
  };
};

const toTrace = (trace: AlarmTraceItem): TraceView => ({
  text: `${traceTypeLabel(trace.traceType)} · ${formatDate(trace.createTime)}`,
  desc: trace.content ?? '-',
});

Page({
  data: {
    detail: null as DetailView | null,
    timeline: [] as TraceView[],
    ...FILE_DATA,
    isMerchant: false,
    canHandle: false,
    loading: false,
    panelVisible: false,
    handleResult: 1,
    handleTitle: '真实火警',
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
    const user = getUser();
    this.setData({ isMerchant: user?.roles?.includes('MERCHANT') ?? false });
    onShowRefresh('alarm/detail', () => this.load());
  },

  async load() {
    if (!currentId) {
      return;
    }
    this.setData({ loading: true });
    try {
      const [detail, timeline] = await Promise.all([
        getAlarm(currentId),
        getAlarmTimeline(currentId),
      ]);
      this.setData({
        detail: toDetail(detail),
        timeline: timeline.map(toTrace),
        canHandle: this.data.isMerchant && detail.status === 0 && !isFaultAlarm(detail),
        loading: false,
      });
    } catch (error) {
      logError('alarm/detail.load', error);
      this.setData({ loading: false });
    }
  },

  openPanel(event: WechatMiniprogram.TouchEvent) {
    const result = Number(event.currentTarget.dataset.result ?? 1);
    const titles: Record<number, string> = { 0: '误报', 1: '真实火警' };
    this.setData({
      panelVisible: true,
      handleResult: result,
      handleTitle: titles[result] ?? '处置',
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

  async submitHandle() {
    if (!this.data.detail || this.data.submitting) {
      return;
    }
    const [pic1, pic2, pic3] = this.data.pics;
    this.setData({ submitting: true });
    try {
      await merchantHandle(this.data.detail.id, {
        result: this.data.handleResult,
        remark: this.data.remark || undefined,
        pic1,
        pic2,
        pic3,
      });
      const message = this.data.handleResult === 1
        ? '已提交值守复核'
        : '已判定误报并收口';
      wx.showToast({ title: message, icon: 'none' });
      this.setData({ panelVisible: false });
      await this.load();
    } catch (error) {
      logError('alarm/detail.submitHandle', error);
    } finally {
      this.setData({ submitting: false });
    }
  },
});
