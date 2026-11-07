import dayjs from 'dayjs';

const USER_TYPE_LABELS: Record<string, string> = {
  PLATFORM: '平台',
  TENANT: '租户',
  MERCHANT: '商户',
};

const ROLE_LABELS: Record<string, string> = {
  PLATFORM_ADMIN: '平台管理员',
  TENANT_ADMIN: '租户管理员',
  TENANT_DUTY: '租户值守',
  TENANT_FIELD: '租户外勤',
  MERCHANT: '商户',
};

const DATA_SCOPE_LABELS: Record<string, string> = {
  ALL: '全部租户',
  TENANT: '本租户',
  WORK_AREA: '所属工作区',
  MERCHANT: '本商户',
};

const MENU_TYPE_LABELS: Record<number, string> = {
  0: '目录',
  1: '菜单',
  2: '按钮',
};

export function formatDateTime(value?: string | null): string {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-';
}

export function userTypeLabel(value?: string | null): string {
  return value ? USER_TYPE_LABELS[value] ?? value : '-';
}

export function roleLabel(value?: string | null): string {
  return value ? ROLE_LABELS[value] ?? value : '-';
}

export function dataScopeLabel(value?: string | null): string {
  return value ? DATA_SCOPE_LABELS[value] ?? value : '-';
}

export function menuTypeLabel(value?: number | null): string {
  return value === null || value === undefined ? '-' : MENU_TYPE_LABELS[value] ?? String(value);
}

export function statusLabel(value?: number | null): string {
  if (value === 1) {
    return '启用';
  }
  if (value === 0) {
    return '禁用';
  }
  return '-';
}

export function roleOptionsForUserType(userType: string): string[] {
  if (userType === 'PLATFORM') {
    return ['PLATFORM_ADMIN'];
  }
  if (userType === 'MERCHANT') {
    return ['MERCHANT'];
  }
  return ['TENANT_ADMIN', 'TENANT_DUTY', 'TENANT_FIELD'];
}

const ALARM_LEVEL_LABELS: Record<number, string> = {
  1: '火警',
  3: '故障',
};

const ALARM_RESULT_LABELS: Record<number, string> = {
  0: '误报',
  1: '真实火警',
  2: '故障',
};

const ALARM_STATUS_LABELS: Record<number, string> = {
  0: '未处理',
  1: '已处理',
};

const VERIFY_STATUS_LABELS: Record<number, string> = {
  0: '待核警',
  1: '核警中',
  2: '已核警',
};

const HANDLE_SOURCE_LABELS: Record<string, string> = {
  DUTY: '值守',
  MERCHANT: '商户',
  FIELD: '外勤',
  ADMIN: '管理员',
  SYSTEM: '系统',
};

const TRACE_TYPE_LABELS: Record<number, string> = {
  0: '设备上报',
  1: '报警通知',
  2: '电话告警',
  3: '电话核警',
  4: '派单核警',
  5: '核警',
  6: '设备恢复',
  7: '商户处置',
  8: '转隐患',
};

const WORK_ORDER_STATUS_LABELS: Record<number, string> = {
  0: '待接单',
  1: '处理中',
  2: '已完成',
};

const DANGER_STATUS_LABELS: Record<string, string> = {
  PENDING: '待转隐患',
  CONVERTED: '已转隐患',
  RECOVERED: '已自行恢复',
  NONE: '不适用',
};

export function alarmLevelLabel(value?: number | null): string {
  return value === null || value === undefined ? '-' : ALARM_LEVEL_LABELS[value] ?? String(value);
}

// V4__remove_pre_alarm.sql 已删除 alarm_level=2（预警），标签表只剩 1/3，两端统一移除该分支。
export function alarmLevelTone(value?: number | null): string {
  if (value === 1) {
    return 'is-danger';
  }
  if (value === 3) {
    return 'is-fault';
  }
  return 'is-muted';
}

export function alarmResultLabel(value?: number | null): string {
  return value === null || value === undefined ? '-' : ALARM_RESULT_LABELS[value] ?? String(value);
}

export function alarmStatusLabel(value?: number | null): string {
  return value === null || value === undefined ? '-' : ALARM_STATUS_LABELS[value] ?? String(value);
}

export function verifyStatusLabel(value?: number | null): string {
  return value === null || value === undefined ? '-' : VERIFY_STATUS_LABELS[value] ?? String(value);
}

export function handleSourceLabel(value?: string | null): string {
  return value ? HANDLE_SOURCE_LABELS[value] ?? value : '-';
}

export function traceTypeLabel(value?: number | null): string {
  return value === null || value === undefined ? '-' : TRACE_TYPE_LABELS[value] ?? String(value);
}

export function traceResultLabel(value?: number | null): string {
  if (value === 1) {
    return '成功';
  }
  if (value === 2) {
    return '失败';
  }
  return '-';
}

export function workOrderStatusLabel(value?: number | null): string {
  return value === null || value === undefined ? '-' : WORK_ORDER_STATUS_LABELS[value] ?? String(value);
}

export function dangerStatusLabel(value?: string | null): string {
  return value ? DANGER_STATUS_LABELS[value] ?? value : '-';
}

/** 剩余时间文案：不足 1 小时显示分钟，避免出现负数 */
export function remainText(seconds?: number | null): string {
  if (seconds === null || seconds === undefined) {
    return '-';
  }
  const total = Math.max(0, Math.floor(seconds));
  if (total < 60) {
    return '不足 1 分钟';
  }
  return `剩 ${Math.floor(total / 60)} 分`;
}

const DANGER_RECORD_STATUS_LABELS: Record<number, string> = {
  0: '待指派',
  1: '待整改',
  2: '待审核',
  3: '已完成',
  4: '已驳回',
};

const DANGER_SOURCE_LABELS: Record<number, string> = {
  0: '随手拍',
  1: '巡查',
  2: '设备故障',
};

const PLAN_STATUS_LABELS: Record<number, string> = {
  0: '未开始',
  1: '进行中',
  2: '已结束',
};

export function dangerRecordStatusLabel(value?: number | null): string {
  return value === null || value === undefined
    ? '-'
    : DANGER_RECORD_STATUS_LABELS[value] ?? String(value);
}

export function dangerSourceLabel(value?: number | null): string {
  return value === null || value === undefined ? '-' : DANGER_SOURCE_LABELS[value] ?? String(value);
}

export function planStatusLabel(value?: number | null): string {
  return value === null || value === undefined ? '-' : PLAN_STATUS_LABELS[value] ?? String(value);
}
