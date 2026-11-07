/**
 * 文案与时间格式化。
 * @date 2026-11-06
 */
const ROLE_LABELS: Record<string, string> = {
  PLATFORM_ADMIN: '平台管理员',
  TENANT_ADMIN: '租户管理员',
  TENANT_DUTY: '租户值守',
  TENANT_FIELD: '租户外勤',
  MERCHANT: '商户',
};

export const roleLabel = (role?: string | null): string => (role ? ROLE_LABELS[role] ?? role : '-');

export const greeting = (): string => {
  const hour = new Date().getHours();
  if (hour < 6) {
    return '夜深了';
  }
  if (hour < 12) {
    return '早上好';
  }
  if (hour < 14) {
    return '中午好';
  }
  if (hour < 18) {
    return '下午好';
  }
  return '晚上好';
};

/**
 * 后端 LocalDateTime 出参为无时区的 ISO-8601（2026-11-05T20:46:00，可能带小数秒），
 * 按字面本地时间展示；兼容空格/斜杠分隔、无秒与纯日期等历史格式。
 * 按字段构造 Date，不依赖 new Date(字符串)，避免小程序 JSCore / iOS 的解析差异。
 * 带 Z 或偏移量的串不在支持范围，返回 '-'，避免静默显示错误时区的时间。
 */
const DATE_TIME_PATTERN = /^(\d{4})-(\d{2})-(\d{2})(?:[T\s]+(\d{2}):(\d{2})(?::(\d{2}))?(?:\.\d+)?)?$/;

const parseDate = (value: string): Date | null => {
  const text = value.trim().replace(/\//g, '-');
  const matched = DATE_TIME_PATTERN.exec(text);
  if (!matched) {
    return null;
  }
  const [, year, month, day, hour, minute, second] = matched;
  const [y, m, d, h, mi, s] = [
    Number(year),
    Number(month),
    Number(day),
    Number(hour ?? 0),
    Number(minute ?? 0),
    Number(second ?? 0),
  ];
  const date = new Date(y, m - 1, d, h, mi, s);
  // Date 会把 2026-13-45、25:70 这类越界值自动进位，回读校验后降级为无效值
  const valid = date.getFullYear() === y
    && date.getMonth() + 1 === m
    && date.getDate() === d
    && date.getHours() === h
    && date.getMinutes() === mi
    && date.getSeconds() === s;
  return valid ? date : null;
};

export const formatDate = (value?: string | null): string => {
  if (typeof value !== 'string' || !value.trim()) {
    return '-';
  }
  const date = parseDate(value);
  if (!date) {
    return '-';
  }
  const pad = (input: number): string => `${input}`.padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
};

const ALARM_LEVEL_LABELS: Record<number, string> = {
  1: '火警',
  3: '故障',
};

const ALARM_RESULT_LABELS: Record<number, string> = {
  0: '误报',
  1: '真实火警',
  2: '故障',
};

const WORK_ORDER_STATUS_LABELS: Record<number, string> = {
  0: '待接单',
  1: '处理中',
  2: '已完成',
};

export const alarmLevelLabel = (level?: number | null): string =>
  (level === null || level === undefined ? '-' : ALARM_LEVEL_LABELS[level] ?? `${level}`);

// V4__remove_pre_alarm.sql 已删除 alarm_level=2（预警），与管理端保持一致，不再保留该分支。
export const alarmLevelClass = (level?: number | null): string => {
  if (level === 1) {
    return 'level-danger';
  }
  return 'level-fault';
};

export const alarmResultLabel = (result?: number | null): string =>
  (result === null || result === undefined ? '-' : ALARM_RESULT_LABELS[result] ?? `${result}`);

export const workOrderStatusLabel = (status?: number | null): string =>
  (status === null || status === undefined ? '-' : WORK_ORDER_STATUS_LABELS[status] ?? `${status}`);

export const dangerStatusLabel = (status?: string | null): string => {
  if (status === 'PENDING') {
    return '待转隐患';
  }
  if (status === 'CONVERTED') {
    return '已转隐患';
  }
  if (status === 'RECOVERED') {
    return '已自行恢复';
  }
  if (status === 'NONE') {
    return '不适用';
  }
  return '-';
};

export const remainText = (seconds?: number | null): string => {
  if (seconds === null || seconds === undefined) {
    return '-';
  }
  const total = Math.max(0, Math.floor(seconds));
  return total < 60 ? '不足 1 分钟' : `剩 ${Math.floor(total / 60)} 分`;
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

export const traceTypeLabel = (traceType?: number | null): string =>
  (traceType === null || traceType === undefined ? '-' : TRACE_TYPE_LABELS[traceType] ?? `${traceType}`);

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

export const dangerRecordStatusLabel = (status?: number | null): string =>
  (status === null || status === undefined ? '-' : DANGER_RECORD_STATUS_LABELS[status] ?? `${status}`);

export const dangerSourceLabel = (sourceType?: number | null): string =>
  (sourceType === null || sourceType === undefined ? '-' : DANGER_SOURCE_LABELS[sourceType] ?? `${sourceType}`);

export const planStatusLabel = (status?: number | null): string =>
  (status === null || status === undefined ? '-' : PLAN_STATUS_LABELS[status] ?? `${status}`);

export const dangerLevelText = (level?: number | null): string => (level === 1 ? '重大' : '普通');

export const patrolStatusText = (finished: boolean, lastResult?: number | null): string => {
  if (!finished) {
    return '未巡查';
  }
  return lastResult === 1 ? '发现隐患' : '正常';
};
