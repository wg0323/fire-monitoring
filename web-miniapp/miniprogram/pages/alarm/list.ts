/**
 * 报警列表：按角色收窄范围，支持页签筛选、下拉刷新与上拉加载。
 * @date 2026-11-06
 */
import { pageAlarms } from '../../services/alarm';
import type { AlarmItem } from '../../services/types';
import { getUser } from '../../store/user';
import {
  alarmLevelClass,
  alarmLevelLabel,
  alarmResultLabel,
  formatDate,
  roleLabel,
} from '../../utils/format';
import { onShowRefresh, refreshTabBar, requireLogin } from '../../utils/guard';
import { logError } from '../../utils/log';

interface AlarmCard {
  id: string;
  sn: string;
  merchantName: string;
  position: string;
  timeText: string;
  levelText: string;
  levelClass: string;
  statusText: string;
  pendingReview: boolean;
}

interface TabItem {
  key: string;
  label: string;
}

const TABS: TabItem[] = [
  { key: 'all', label: '全部' },
  { key: '1', label: '火警' },
  { key: '3', label: '故障' },
];

const toCard = (alarm: AlarmItem): AlarmCard => {
  const pendingReview = alarm.status === 0 && alarm.verifyStatus === 1 && alarm.handleSource === 'MERCHANT';
  let statusText = alarm.status === 1 ? alarmResultLabel(alarm.result) : '未处理';
  if (pendingReview) {
    statusText = '待复核';
  } else if (alarm.dangerId) {
    statusText = '已转隐患';
  } else if (alarm.selfRecovered) {
    statusText = '已自行恢复';
  } else if (alarm.dangerConvertDeadline) {
    statusText = '待转隐患';
  }
  return {
    id: alarm.id,
    sn: alarm.sn,
    merchantName: alarm.merchantName ?? '-',
    position: alarm.position ?? '-',
    timeText: formatDate(alarm.alarmTime),
    levelText: alarmLevelLabel(alarm.alarmLevel),
    levelClass: alarmLevelClass(alarm.alarmLevel),
    statusText,
    pendingReview,
  };
};

Page({
  data: {
    roleText: '',
    isMerchant: false,
    tabs: TABS,
    activeTab: 'all',
    alarms: [] as AlarmCard[],
    pageNum: 1,
    hasMore: true,
    loading: false,
  },

  onShow() {
    if (!requireLogin()) {
      return;
    }
    refreshTabBar(this);
    const user = getUser();
    this.setData({
      roleText: roleLabel(user?.roles?.[0]),
      isMerchant: user?.roles?.includes('MERCHANT') ?? false,
    });
    onShowRefresh('alarm/list', () => this.load(true));
  },

  async onPullDownRefresh() {
    await this.load(true);
    wx.stopPullDownRefresh();
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.load(false);
    }
  },

  switchTab(event: WechatMiniprogram.TouchEvent) {
    const key = String(event.currentTarget.dataset.key ?? 'all');
    if (key === this.data.activeTab) {
      return;
    }
    this.setData({ activeTab: key });
    this.load(true);
  },

  async load(reset: boolean) {
    if (this.data.loading) {
      return;
    }
    const pageNum = reset ? 1 : this.data.pageNum + 1;
    this.setData({ loading: true });
    try {
      const page = await pageAlarms({
        pageNum,
        pageSize: 20,
        level: this.data.activeTab === 'all' ? undefined : Number(this.data.activeTab),
      });
      const cards = page.records.map(toCard);
      this.setData({
        alarms: reset ? cards : [...this.data.alarms, ...cards],
        pageNum,
        hasMore: page.records.length >= 20,
        loading: false,
      });
    } catch (error) {
      logError('alarm/list.load', error);
      this.setData({ loading: false });
    }
  },

  openDetail(event: WechatMiniprogram.TouchEvent) {
    const id = String(event.currentTarget.dataset.id ?? '');
    if (id) {
      wx.navigateTo({ url: `/pages/alarm/detail?id=${id}` });
    }
  },
});
