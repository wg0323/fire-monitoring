/**
 * 小程序业务数据类型：与后端 VO 字段保持一致。
 * @date 2026-11-06
 */
export interface PageResult<T> {
  records: T[];
  total: number;
  current: number;
  size: number;
}

export interface AlarmItem {
  id: string;
  sn: string;
  deviceType?: string | null;
  deviceTypeName?: string | null;
  merchantId?: string | null;
  merchantName?: string | null;
  position?: string | null;
  alarmTime: string;
  alarmType?: string | null;
  alarmLevel: number;
  status: number;
  result?: number | null;
  verifyStatus: number;
  snapshotUrl?: string | null;
  handleSource?: string | null;
  handlerName?: string | null;
  dangerId?: string | null;
  finalizeTime?: string | null;
  dangerConvertDeadline?: string | null;
  dangerRemainSeconds?: number | null;
  recoverTime?: string | null;
  selfRecovered?: boolean | null;
  addRemark?: string | null;
  addPic1?: string | null;
  addPic2?: string | null;
  addPic3?: string | null;
  workOrderId?: string | null;
}

export interface AlarmTraceItem {
  id: string;
  traceType: number;
  content?: string | null;
  createTime?: string | null;
}

export interface WorkOrderItem {
  id: string;
  merchantName?: string | null;
  alarmSn?: string | null;
  alarmTime?: string | null;
  alarmResult?: number | null;
  workAreaName?: string | null;
  title: string;
  content?: string | null;
  handlerName?: string | null;
  status: number;
  acceptTime?: string | null;
  finishTime?: string | null;
  finishRemark?: string | null;
  finishPic1?: string | null;
  finishPic2?: string | null;
  finishPic3?: string | null;
  dangerStatus?: string | null;
  dangerRemainSeconds?: number | null;
  createTime?: string | null;
}

export interface HomeSummary {
  alarmCount: number;
  dangerCount: number;
  deviceCount: number;
  offlineCount: number;
  workOrderCount: number;
  inspectionCount: number;
  latestAlarms: AlarmItem[];
  latestDangers: Array<{
    id: string;
    remark?: string | null;
    merchantName?: string | null;
    status: number;
    sourceType?: number | null;
    createTime?: string | null;
  }>;
}

export interface DangerItem {
  id: string;
  merchantId?: string | null;
  merchantName?: string | null;
  sourceType?: number | null;
  dangerType?: string | null;
  remark?: string | null;
  position?: string | null;
  positionDetail?: string | null;
  pic1?: string | null;
  pic2?: string | null;
  pic3?: string | null;
  dangerLevel?: number | null;
  dangerLevelFinal?: number | null;
  status?: number | null;
  createBy?: string | null;
  createTime?: string | null;
  assigneeType?: string | null;
  assigneeId?: string | null;
  assigneeName?: string | null;
  workAreaId?: string | null;
  workAreaName?: string | null;
  alarmTime?: string | null;
  convertTime?: string | null;
  assignTime?: string | null;
  dealLimit?: string | null;
  dealTime?: string | null;
  auditTime?: string | null;
  auditResult?: number | null;
  auditRemark?: string | null;
  lastDealUserName?: string | null;
  lastDealTime?: string | null;
  lastDealRemark?: string | null;
  lastDealPic1?: string | null;
  lastDealPic2?: string | null;
  lastDealPic3?: string | null;
}

export interface DangerTimelineNode {
  nodeKey: string;
  title: string;
  result?: number | null;
  occurred: boolean;
  time?: string | null;
  operator?: string | null;
  content?: string | null;
  pics: string[];
  extra?: string | null;
}

export interface DangerAssignee {
  id: string;
  name?: string | null;
  phone?: string | null;
}

export interface InspectionPlanItem {
  id: string;
  workAreaId?: string | null;
  workAreaName?: string | null;
  name: string;
  startDate: string;
  endDate: string;
  status: number;
  remark?: string | null;
  merchantTotal: number;
  finishCount: number;
  dangerCount: number;
  createBy?: string | null;
  createTime?: string | null;
}

export interface InspectionMerchantItem {
  id: string;
  name: string;
  addr?: string | null;
  finished: boolean;
  lastResult?: number | null;
  lastPatrolBy?: string | null;
  lastPatrolTime?: string | null;
  dangerId?: string | null;
}

export interface MerchantOption {
  id: string;
  name?: string | null;
  addr?: string | null;
}

export interface LoginResult {
  tokenName: string;
  tokenValue: string;
  userId: string;
  userName: string;
  realName: string;
  userType: string;
  dataScope: string;
  currentMerchantId?: string | null;
  merchants?: MerchantOption[];
  roles: string[];
  permissions: string[];
}

export interface DictItem {
  id?: string | null;
  code: string;
  name: string;
}

export interface DeviceItem {
  id: string;
  name?: string | null;
  sn: string;
  type?: string | null;
  typeName?: string | null;
  merchantName?: string | null;
  position?: string | null;
  locationText?: string | null;
  buildingName?: string | null;
  floorName?: string | null;
  locationType?: number | null;
  status?: number | null;
  onlineStatus?: number | null;
  lastTime?: string | null;
  activeDate?: string | null;
  installDate?: string | null;
}

export interface NotificationItem {
  id: string;
  type: number;
  content: string;
  relatedId?: string | null;
  status: number;
  messageTime?: string | null;
}

export interface SubscribeConfig {
  alarmTemplateId?: string | null;
  dangerTemplateId?: string | null;
  enabled: boolean;
}
