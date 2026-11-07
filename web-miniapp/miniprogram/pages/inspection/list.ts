/**
 * 巡查计划列表：外勤只看所属工作区，卡片展示进度。
 * @date 2026-11-06
 */
import { pageInspectionPlans } from '../../services/inspection';
import type { InspectionPlanItem } from '../../services/types';
import { planStatusLabel } from '../../utils/format';
import { onShowRefresh, refreshTabBar, requireLogin, requireRole } from '../../utils/guard';
import { logError } from '../../utils/log';

interface PlanCard {
  id: string;
  name: string;
  workAreaName: string;
  dateText: string;
  statusText: string;
  statusClass: string;
  progressText: string;
  percent: number;
  dangerCount: number;
}

const toCard = (plan: InspectionPlanItem): PlanCard => {
  const percent = plan.merchantTotal > 0
    ? Math.min(100, Math.round((plan.finishCount * 100) / plan.merchantTotal))
    : 0;
  return {
    id: plan.id,
    name: plan.name,
    workAreaName: plan.workAreaName || '-',
    dateText: `${plan.startDate} 至 ${plan.endDate}`,
    statusText: planStatusLabel(plan.status),
    statusClass: `plan-status-${plan.status}`,
    progressText: `${plan.finishCount} / ${plan.merchantTotal}`,
    percent,
    dangerCount: plan.dangerCount,
  };
};

Page({
  data: {
    cards: [] as PlanCard[],
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
    if (!requireRole(['TENANT_ADMIN', 'TENANT_FIELD'])) {
      return;
    }
    onShowRefresh('inspection/list', () => this.load(true));
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

  async load(reset: boolean) {
    if (this.data.loading) {
      return;
    }
    const pageNum = reset ? 1 : this.data.pageNum + 1;
    this.setData({ loading: true });
    try {
      const page = await pageInspectionPlans({ pageNum, pageSize: 20 });
      const cards = page.records.map(toCard);
      this.setData({
        cards: reset ? cards : [...this.data.cards, ...cards],
        total: page.total,
        pageNum,
        hasMore: page.records.length >= 20,
        loading: false,
      });
    } catch (error) {
      logError('inspection/list.load', error);
      this.setData({ loading: false });
    }
  },

  openDetail(event: WechatMiniprogram.TouchEvent) {
    const id = String(event.currentTarget.dataset.id ?? '');
    if (id) {
      wx.navigateTo({ url: `/pages/inspection/detail?id=${id}` });
    }
  },
});
