/**
 * 站内消息：分页展示、标记已读并按类型跳转报警或隐患详情。
 * @date 2026-11-06
 */
import { markNotificationRead, pageNotifications } from '../../services/notification';
import type { NotificationItem } from '../../services/types';
import { formatDate } from '../../utils/format';
import { onShowRefresh, requireLogin } from '../../utils/guard';
import { logError } from '../../utils/log';

interface MessageCard {
  id: string;
  type: number;
  typeText: string;
  content: string;
  timeText: string;
  unread: boolean;
  relatedId: string;
}

const TYPE_LABELS: Record<number, string> = {
  1: '报警',
  2: '故障',
  3: '隐患',
  4: '系统',
};

const toCard = (item: NotificationItem): MessageCard => ({
  id: item.id,
  type: item.type,
  typeText: TYPE_LABELS[item.type] ?? '消息',
  content: item.content,
  timeText: formatDate(item.messageTime),
  unread: item.status === 0,
  relatedId: item.relatedId ? String(item.relatedId) : '',
});

Page({
  data: {
    items: [] as MessageCard[],
    pageNum: 1,
    hasMore: true,
    loading: false,
  },

  onShow() {
    if (!requireLogin()) {
      return;
    }
    onShowRefresh('message/list', () => this.load(true));
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
      const page = await pageNotifications(pageNum, 20);
      const cards = page.records.map(toCard);
      this.setData({
        items: reset ? cards : [...this.data.items, ...cards],
        pageNum,
        hasMore: page.records.length >= 20,
        loading: false,
      });
    } catch (error) {
      logError('message/list.load', error);
      this.setData({ loading: false });
    }
  },

  async openItem(event: WechatMiniprogram.TouchEvent) {
    const id = String(event.currentTarget.dataset.id ?? '');
    const type = Number(event.currentTarget.dataset.type);
    const relatedId = String(event.currentTarget.dataset.related ?? '');
    const unread = Boolean(event.currentTarget.dataset.unread);
    if (id && unread) {
      try {
        await markNotificationRead(id);
        const items = this.data.items.map((item) => (
          item.id === id ? { ...item, unread: false } : item
        ));
        this.setData({ items });
      } catch (error) {
        logError('message/list.markRead', error);
        return;
      }
    }
    if (type === 3 && relatedId) {
      wx.navigateTo({ url: `/pages/danger/detail?id=${relatedId}` });
      return;
    }
    if ((type === 1 || type === 2) && relatedId) {
      wx.navigateTo({ url: `/pages/alarm/detail?id=${relatedId}` });
    }
  },
});
