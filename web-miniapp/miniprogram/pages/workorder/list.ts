/**
 * 我的工单：外勤查看指派给本人的工单并接单。
 * @date 2026-11-06
 */
import { acceptWorkOrder, pageWorkOrders } from '../../services/workorder';
import type { WorkOrderItem } from '../../services/types';
import { getUser } from '../../store/user';
import { formatDate, workOrderStatusLabel } from '../../utils/format';
import { onShowRefresh, requireLogin } from '../../utils/guard';
import { logError } from '../../utils/log';

interface OrderCard {
  id: string;
  merchantName: string;
  alarmSn: string;
  workAreaName: string;
  title: string;
  status: number;
  statusText: string;
  timeText: string;
}

interface TabItem {
  key: string;
  label: string;
}

const TABS: TabItem[] = [
  { key: 'all', label: '全部' },
  { key: '0', label: '待接单' },
  { key: '1', label: '处理中' },
  { key: '2', label: '已完成' },
];

const toCard = (order: WorkOrderItem): OrderCard => ({
  id: order.id,
  merchantName: order.merchantName ?? '-',
  alarmSn: order.alarmSn ?? '-',
  workAreaName: order.workAreaName ?? '-',
  title: order.title,
  status: order.status,
  statusText: workOrderStatusLabel(order.status),
  timeText: formatDate(order.createTime),
});

Page({
  data: {
    tabs: TABS,
    activeTab: 'all',
    orders: [] as OrderCard[],
    loading: false,
    isMerchant: false,
  },

  onShow() {
    if (!requireLogin()) {
      return;
    }
    const user = getUser();
    this.setData({ isMerchant: user?.roles?.includes('MERCHANT') ?? false });
    onShowRefresh('workorder/list', () => this.load());
  },

  async onPullDownRefresh() {
    await this.load();
    wx.stopPullDownRefresh();
  },

  switchTab(event: WechatMiniprogram.TouchEvent) {
    const key = String(event.currentTarget.dataset.key ?? 'all');
    if (key === this.data.activeTab) {
      return;
    }
    this.setData({ activeTab: key });
    this.load();
  },

  async load() {
    this.setData({ loading: true });
    try {
      const page = await pageWorkOrders({
        pageNum: 1,
        pageSize: 50,
        status: this.data.activeTab === 'all' ? undefined : Number(this.data.activeTab),
      });
      this.setData({ orders: page.records.map(toCard), loading: false });
    } catch (error) {
      logError('workorder/list.load', error);
      this.setData({ loading: false });
    }
  },

  openDetail(event: WechatMiniprogram.TouchEvent) {
    const id = String(event.currentTarget.dataset.id ?? '');
    if (id) {
      wx.navigateTo({ url: `/pages/workorder/detail?id=${id}` });
    }
  },

  async accept(event: WechatMiniprogram.TouchEvent) {
    const id = String(event.currentTarget.dataset.id ?? '');
    if (!id) {
      return;
    }
    try {
      await acceptWorkOrder(id);
      wx.showToast({ title: '已接单', icon: 'success' });
      wx.navigateTo({ url: `/pages/workorder/detail?id=${id}` });
    } catch (error) {
      logError('workorder/list.accept', error);
    }
  },
});
