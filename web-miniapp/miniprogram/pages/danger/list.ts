/**
 * 隐患列表：按角色页签展示，管理员多「待指派」，外勤仅看指派给本人。
 * @date 2026-11-06
 */
import { pageDangers } from '../../services/danger';
import type { DangerItem } from '../../services/types';
import { getUser } from '../../store/user';
import {
  dangerRecordStatusLabel,
  dangerSourceLabel,
  formatDate,
  roleLabel,
} from '../../utils/format';
import { onShowRefresh, refreshTabBar, requireLogin } from '../../utils/guard';
import { logError } from '../../utils/log';

interface DangerCard {
  id: string;
  sourceText: string;
  typeText: string;
  merchantName: string;
  limitText: string;
  timeText: string;
  statusText: string;
  statusClass: string;
  picCount: number;
  overdue: boolean;
}

interface TabItem {
  value: number;
  label: string;
}

const ADMIN_TABS: TabItem[] = [
  { value: 0, label: '待指派' },
  { value: 1, label: '待整改' },
  { value: 2, label: '待审核' },
  { value: 3, label: '已完成' },
];

const OTHER_TABS: TabItem[] = [
  { value: 1, label: '待整改' },
  { value: 2, label: '待审核' },
  { value: 3, label: '已完成' },
];

const todayText = (): string => {
  const now = new Date();
  const pad = (input: number): string => `${input}`.padStart(2, '0');
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
};

const toCard = (danger: DangerItem): DangerCard => {
  const picCount = [danger.pic1, danger.pic2, danger.pic3].filter(Boolean).length;
  const overdue = Boolean(danger.dealLimit) && danger.status !== 3
    && String(danger.dealLimit) < todayText();
  return {
    id: danger.id,
    sourceText: dangerSourceLabel(danger.sourceType),
    typeText: danger.dangerType || '隐患',
    merchantName: danger.merchantName || '-',
    limitText: danger.dealLimit
      ? `整改期限 ${danger.dealLimit}${overdue ? '（逾期）' : ''}`
      : '未设置整改期限',
    timeText: formatDate(danger.createTime),
    statusText: dangerRecordStatusLabel(danger.status),
    statusClass: `status-${danger.status ?? 0}`,
    picCount,
    overdue,
  };
};

Page({
  data: {
    roleText: '',
    tabs: OTHER_TABS,
    activeTab: 1,
    cards: [] as DangerCard[],
    total: 0,
    pageNum: 1,
    hasMore: true,
    loading: false,
  },

  onShow() {
    if (!requireLogin()) {
      return;
    }
    refreshTabBar(this);
    const role = getUser()?.roles?.[0] ?? '';
    const isAdmin = role === 'TENANT_ADMIN';
    const tabs = isAdmin ? ADMIN_TABS : OTHER_TABS;
    const activeTab = tabs.some((tab) => tab.value === this.data.activeTab)
      ? this.data.activeTab
      : tabs[0].value;
    this.setData({
      roleText: roleLabel(role),
      tabs,
      activeTab,
    });
    onShowRefresh('danger/list', () => this.load(true));
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
    const value = Number(event.currentTarget.dataset.value);
    if (value === this.data.activeTab) {
      return;
    }
    this.setData({ activeTab: value });
    this.load(true);
  },

  async load(reset: boolean) {
    if (this.data.loading) {
      return;
    }
    const pageNum = reset ? 1 : this.data.pageNum + 1;
    this.setData({ loading: true });
    try {
      const page = await pageDangers({
        pageNum,
        pageSize: 20,
        status: this.data.activeTab,
      });
      const cards = page.records.map(toCard);
      this.setData({
        cards: reset ? cards : [...this.data.cards, ...cards],
        total: page.total,
        pageNum,
        hasMore: page.records.length >= 20,
        loading: false,
      });
    } catch (error) {
      logError('danger/list.load', error);
      this.setData({ loading: false });
    }
  },

  openDetail(event: WechatMiniprogram.TouchEvent) {
    const id = String(event.currentTarget.dataset.id ?? '');
    if (id) {
      wx.navigateTo({ url: `/pages/danger/detail?id=${id}` });
    }
  },

  goReport() {
    wx.navigateTo({ url: '/pages/danger/report' });
  },
});
