/**
 * 巡查计划详情：工作区商户与巡查状态，支持名称检索与滚动加载。
 * @date 2026-11-06
 */
import { getInspectionPlan, pageInspectionMerchants } from '../../services/inspection';
import type { InspectionMerchantItem, InspectionPlanItem } from '../../services/types';
import { formatDate, planStatusLabel, patrolStatusText } from '../../utils/format';
import { onShowRefresh, requireLogin } from '../../utils/guard';
import { logError } from '../../utils/log';

interface MerchantCard {
  id: string;
  name: string;
  addr: string;
  statusText: string;
  statusClass: string;
  patrolText: string;
}

const toCard = (merchant: InspectionMerchantItem): MerchantCard => ({
  id: merchant.id,
  name: merchant.name,
  addr: merchant.addr || '-',
  statusText: patrolStatusText(merchant.finished, merchant.lastResult),
  statusClass: !merchant.finished ? 'patrol-pending' : merchant.lastResult === 1 ? 'patrol-danger' : 'patrol-done',
  patrolText: merchant.finished
    ? `${merchant.lastPatrolBy || '-'} · ${formatDate(merchant.lastPatrolTime)}`
    : '未巡查',
});

Page({
  data: {
    id: '',
    loading: false,
    plan: null as InspectionPlanItem | null,
    statusText: '',
    progressText: '',
    keyword: '',
    merchants: [] as MerchantCard[],
    total: 0,
    pageNum: 1,
    pageSize: 20,
    hasMore: true,
  },

  onLoad(options: Record<string, string>) {
    if (!requireLogin()) {
      return;
    }
    this.setData({ id: options.id ?? '' });
    this.load(true);
  },

  onShow() {
    if (this.data.plan) {
      onShowRefresh('inspection/detail', () => this.load(true));
    }
  },

  onReachBottom() {
    if (this.data.loading || !this.data.hasMore) {
      return;
    }
    this.load(false);
  },

  onKeywordInput(event: WechatMiniprogram.Input) {
    this.setData({ keyword: event.detail.value });
  },

  search() {
    this.load(true);
  },

  async load(reset: boolean) {
    if (!this.data.id || this.data.loading) {
      return;
    }
    const pageNum = reset ? 1 : this.data.pageNum;
    this.setData({ loading: true });
    try {
      if (reset) {
        const plan = await getInspectionPlan(this.data.id);
        this.setData({
          plan,
          statusText: planStatusLabel(plan.status),
          progressText: `${plan.finishCount} / ${plan.merchantTotal}`,
        });
      }
      const page = await pageInspectionMerchants(this.data.id, {
        pageNum,
        pageSize: this.data.pageSize,
        name: this.data.keyword.trim() || undefined,
      });
      const cards = page.records.map(toCard);
      const merchants = reset ? cards : [...this.data.merchants, ...cards];
      this.setData({
        merchants,
        total: page.total,
        pageNum: pageNum + 1,
        hasMore: merchants.length < page.total,
      });
    } catch (error) {
      logError('inspection/detail.load', error);
      if (reset) {
        this.setData({ plan: null });
      }
    } finally {
      this.setData({ loading: false });
    }
  },

  openSubmit(event: WechatMiniprogram.TouchEvent) {
    const merchantId = String(event.currentTarget.dataset.id ?? '');
    const merchantName = String(event.currentTarget.dataset.name ?? '');
    if (!merchantId || !this.data.id) {
      return;
    }
    wx.navigateTo({
      url: `/pages/inspection/submit?planId=${this.data.id}&merchantId=${merchantId}&merchantName=${encodeURIComponent(merchantName)}`,
    });
  },
});
