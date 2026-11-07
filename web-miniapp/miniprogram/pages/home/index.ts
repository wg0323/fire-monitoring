/**
 * 首页：角色问候、2x2 KPI、快捷入口与最新报警。
 * @date 2026-11-06
 */
import { getHomeSummary } from '../../services/home';
import { unreadNotificationCount } from '../../services/notification';
import type { AlarmItem, HomeSummary } from '../../services/types';
import { getUser, type UserInfo } from '../../store/user';
import {
  alarmLevelClass,
  alarmLevelLabel,
  formatDate,
  greeting,
  roleLabel,
} from '../../utils/format';
import { onShowRefresh, refreshTabBar, requireLogin } from '../../utils/guard';
import { logError } from '../../utils/log';

interface AlarmBrief {
  id: string;
  sn: string;
  merchantName: string;
  position: string;
  timeText: string;
  levelText: string;
  levelClass: string;
  pendingReview: boolean;
}

const EMPTY_SUMMARY: HomeSummary = {
  alarmCount: 0,
  dangerCount: 0,
  deviceCount: 0,
  offlineCount: 0,
  workOrderCount: 0,
  inspectionCount: 0,
  latestAlarms: [],
  latestDangers: [],
};

const toBrief = (alarm: AlarmItem): AlarmBrief => ({
  id: alarm.id,
  sn: alarm.sn,
  merchantName: alarm.merchantName ?? '-',
  position: alarm.position ?? '-',
  timeText: formatDate(alarm.alarmTime),
  levelText: alarmLevelLabel(alarm.alarmLevel),
  levelClass: alarmLevelClass(alarm.alarmLevel),
  pendingReview: alarm.status === 0 && alarm.verifyStatus === 1 && alarm.handleSource === 'MERCHANT',
});

Page({
  data: {
    user: null as UserInfo | null,
    greeting: '',
    roleText: '',
    workAreaText: '',
    isMerchant: false,
    summary: EMPTY_SUMMARY,
    latestAlarms: [] as AlarmBrief[],
    unreadCount: 0,
    loading: false,
  },

  onShow() {
    if (!requireLogin()) {
      return;
    }
    refreshTabBar(this);
    this.loadUser();
    onShowRefresh('home', () => this.loadSummary());
  },

  async onPullDownRefresh() {
    await this.loadSummary();
    wx.stopPullDownRefresh();
  },

  loadUser() {
    const user = getUser();
    if (!user) {
      wx.reLaunch({ url: '/pages/login/index' });
      return;
    }
    this.setData({
      user,
      greeting: greeting(),
      roleText: roleLabel(user.roles?.[0]),
      workAreaText: user.workAreaNames?.join('、') ?? '',
      isMerchant: user.roles?.includes('MERCHANT') ?? false,
    });
  },

  async loadSummary() {
    this.setData({ loading: true });
    try {
      const [summary, unreadCount] = await Promise.all([
        getHomeSummary(),
        unreadNotificationCount(),
      ]);
      this.setData({
        summary,
        latestAlarms: (summary.latestAlarms ?? []).map(toBrief),
        unreadCount,
        loading: false,
      });
    } catch (error) {
      logError('home.loadSummary', error);
      this.setData({ loading: false });
    }
  },

  goAlarm() {
    wx.switchTab({ url: '/pages/alarm/list' });
  },

  goDangerList() {
    wx.switchTab({ url: '/pages/danger/list' });
  },

  goInspection() {
    if (this.data.isMerchant) {
      wx.showToast({ title: '商户账号不参与巡查', icon: 'none' });
      return;
    }
    wx.switchTab({ url: '/pages/inspection/list' });
  },

  goWorkOrder() {
    if (this.data.isMerchant) {
      wx.showToast({ title: '商户账号不处理工单', icon: 'none' });
      return;
    }
    wx.navigateTo({ url: '/pages/workorder/list' });
  },

  goMine() {
    wx.switchTab({ url: '/pages/mine/index' });
  },

  goReport() {
    wx.navigateTo({ url: '/pages/danger/report' });
  },

  goMessages() {
    wx.navigateTo({ url: '/pages/message/list' });
  },

  openDanger(event: WechatMiniprogram.TouchEvent) {
    const id = String(event.currentTarget.dataset.id ?? '');
    if (id) {
      wx.navigateTo({ url: `/pages/danger/detail?id=${id}` });
    }
  },

  openAlarm(event: WechatMiniprogram.TouchEvent) {
    const id = String(event.currentTarget.dataset.id ?? '');
    if (id) {
      wx.navigateTo({ url: `/pages/alarm/detail?id=${id}` });
    }
  },

  goDevices() {
    wx.navigateTo({ url: '/pages/device/list' });
  },
});
