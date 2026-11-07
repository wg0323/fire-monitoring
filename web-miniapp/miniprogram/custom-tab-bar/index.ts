/**
 * 自定义 tabBar：按当前登录角色渲染可访问的 tab。
 * @date 2026-11-06
 */
import { getUser, roleKey } from '../store/user';

interface TabItem {
  pagePath: string;
  text: string;
  key: string;
}

const ALL_TABS: TabItem[] = [
  { pagePath: '/pages/home/index', text: '首页', key: 'home' },
  { pagePath: '/pages/alarm/list', text: '报警', key: 'alarm' },
  { pagePath: '/pages/danger/list', text: '隐患', key: 'danger' },
  { pagePath: '/pages/inspection/list', text: '巡查', key: 'inspection' },
  { pagePath: '/pages/mine/index', text: '我的', key: 'mine' },
];

const ROLE_TABS: Record<string, string[]> = {
  TENANT_ADMIN: ['home', 'alarm', 'danger', 'inspection', 'mine'],
  TENANT_FIELD: ['home', 'alarm', 'danger', 'inspection', 'mine'],
  MERCHANT: ['home', 'alarm', 'danger', 'mine'],
};

Component({
  data: {
    tabs: [] as TabItem[],
    selected: 0,
  },
  lifetimes: {
    attached() {
      this.refresh();
    },
  },
  methods: {
    refresh() {
      const role = roleKey(getUser());
      // 未知角色只给最小可用集，避免兜底成全量外勤权限
      const allowed = ROLE_TABS[role] ?? ['home', 'mine'];
      const tabs = ALL_TABS.filter((tab) => allowed.includes(tab.key));
      const pages = getCurrentPages();
      const current = pages[pages.length - 1]?.route ?? '';
      const selected = tabs.findIndex((tab) => tab.pagePath === `/${current}`);
      this.setData({ tabs, selected: selected < 0 ? 0 : selected });
    },
    onTap(event: WechatMiniprogram.TouchEvent) {
      const index = Number(event.currentTarget.dataset.index);
      const tab = this.data.tabs[index];
      if (!tab) {
        return;
      }
      this.setData({ selected: index });
      wx.switchTab({ url: tab.pagePath });
    },
  },
});
