/**
 * 设备列表：按角色数据范围展示本租户、本商户或所属工作区设备。
 * @date 2026-11-06
 */
import { pageDevices } from '../../services/device';
import type { DeviceItem } from '../../services/types';
import { formatDate } from '../../utils/format';
import { onShowRefresh, requireLogin } from '../../utils/guard';
import { logError } from '../../utils/log';

type OnlineFilter = 'all' | 'online' | 'offline';

interface TabItem {
  key: OnlineFilter;
  label: string;
}

interface DeviceCard {
  id: string;
  name: string;
  sn: string;
  typeText: string;
  merchantName: string;
  position: string;
  online: boolean;
  onlineText: string;
  lastTimeText: string;
}

const TABS: TabItem[] = [
  { key: 'all', label: '全部' },
  { key: 'online', label: '在线' },
  { key: 'offline', label: '离线' },
];

const toCard = (device: DeviceItem): DeviceCard => ({
  id: device.id,
  name: device.name ?? device.sn,
  sn: device.sn,
  typeText: device.typeName ?? device.type ?? '-',
  merchantName: device.merchantName ?? '-',
  position: device.locationText || device.position || '未设置位置',
  online: device.onlineStatus === 1,
  onlineText: device.onlineStatus === 1 ? '在线' : '离线',
  lastTimeText: formatDate(device.lastTime),
});

const onlineStatusOf = (filter: OnlineFilter): number | undefined => {
  if (filter === 'online') {
    return 1;
  }
  if (filter === 'offline') {
    return 0;
  }
  return undefined;
};

Page({
  data: {
    tabs: TABS,
    activeTab: 'all' as OnlineFilter,
    keyword: '',
    devices: [] as DeviceCard[],
    total: 0,
    pageNum: 1,
    pageSize: 20,
    hasMore: true,
    loading: false,
  },

  onShow() {
    if (!requireLogin()) {
      return;
    }
    onShowRefresh('device/list', () => this.load(true));
  },

  async onPullDownRefresh() {
    await this.load(true);
    wx.stopPullDownRefresh();
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

  switchTab(event: WechatMiniprogram.TouchEvent) {
    const key = String(event.currentTarget.dataset.key ?? 'all') as OnlineFilter;
    if (key === this.data.activeTab) {
      return;
    }
    this.setData({ activeTab: key }, () => this.load(true));
  },

  async load(reset = false) {
    if (this.data.loading) {
      return;
    }
    const pageNum = reset ? 1 : this.data.pageNum;
    this.setData({ loading: true });
    try {
      const page = await pageDevices({
        pageNum,
        pageSize: this.data.pageSize,
        keyword: this.data.keyword.trim() || undefined,
        onlineStatus: onlineStatusOf(this.data.activeTab),
      });
      const rows = page.records.map(toCard);
      const devices = reset ? rows : [...this.data.devices, ...rows];
      this.setData({
        devices,
        total: page.total,
        pageNum: pageNum + 1,
        hasMore: devices.length < page.total,
        loading: false,
      });
    } catch (error) {
      logError('device/list.load', error);
      this.setData({ loading: false });
    }
  },
});
