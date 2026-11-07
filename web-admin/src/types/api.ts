export interface Result<T> {
  code: number;
  message: string;
  data: T;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  current: number;
  size: number;
}

export interface LoginResult {
  tokenName: string;
  tokenValue: string;
  userId: string;
  userName: string;
  realName: string;
  userType: string;
  dataScope: string;
  roles: string[];
  permissions: string[];
}

export interface MenuNode {
  id: string;
  tenantId: string;
  parentId: string;
  name: string;
  menuKey: string;
  type: number;
  path?: string | null;
  perms?: string | null;
  seq: number;
  status: number;
  platformOnly: number;
  children: MenuNode[];
}

export interface UserInfo {
  id: string;
  userName: string;
  realName: string;
  phone?: string | null;
  userType: string;
  tenantId?: string | null;
  deptId?: string | null;
  merchantId?: string | null;
  tenantName?: string | null;
  merchantName?: string | null;
  workAreaNames: string[];
  dataScope: string;
  wxBound?: boolean;
  roles: string[];
  permissions: string[];
  menus: MenuNode[];
}

export interface OrgItem {
  id: string;
  name: string;
  code?: string | null;
  adcode?: string | null;
  address?: string | null;
  status: number;
  createTime?: string | null;
  updateTime?: string | null;
}

export interface RoleItem {
  id: string;
  tenantId: string;
  tenantName?: string | null;
  roleName: string;
  roleKey: string;
  dataScope: string;
  status: number;
  userCount?: number | null;
}

export interface UserItem {
  id: string;
  tenantId: string;
  tenantName?: string | null;
  deptId?: string | null;
  deptName?: string | null;
  merchantId?: string | null;
  merchantName?: string | null;
  userType: string;
  userName: string;
  realName: string;
  phone: string;
  status: number;
  lastLogin?: string | null;
  createTime?: string | null;
  roleId?: string | null;
  roleKey?: string | null;
  roleName?: string | null;
  workAreaIds: string[];
  workAreaNames: string[];
}

export interface WorkAreaItem {
  id: string;
  tenantId: string;
  tenantName?: string | null;
  name: string;
  remark?: string | null;
  userCount?: number | null;
  merchantCount?: number | null;
  userIds?: string[];
  merchantIds?: string[];
  createTime?: string | null;
}

export interface DeptNode {
  id: string;
  tenantId: string;
  parentId: string;
  name: string;
  status: number;
  children: DeptNode[];
}

export interface DictItem {
  id: string;
  code: string;
  name: string;
  remark?: string | null;
  status: number;
  sort?: number | null;
  offlineThreshold?: number | null;
  offlineUnit?: string | null;
  monitorFlag?: number | null;
}

export interface MerchantOption {
  id: string;
  name: string;
  addr?: string | null;
}

export interface SelectOption {
  value: string;
  label: string;
}

export interface PageQuery {
  pageNum: number;
  pageSize: number;
}

export interface AlarmContact {
  relation: string;
  name: string;
  phone: string;
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
  merchantHandleResult?: number | null;
  merchantHandleTime?: string | null;
  handleSource?: string | null;
  handlerName?: string | null;
  workAreaId?: string | null;
  workAreaName?: string | null;
  dangerId?: string | null;
  finalizeTime?: string | null;
  dangerConvertDeadline?: string | null;
  dangerRemainSeconds?: number | null;
  recoverTime?: string | null;
  selfRecovered?: boolean | null;
  dispatchTime?: string | null;
  dispatchRemark?: string | null;
  opUserName?: string | null;
  addRemark?: string | null;
  addPic1?: string | null;
  addPic2?: string | null;
  addPic3?: string | null;
  workOrderId?: string | null;
  workOrderStatus?: number | null;
  floorPlanUrl?: string | null;
  floorName?: string | null;
  floorX?: number | null;
  floorY?: number | null;
  contacts?: AlarmContact[] | null;
  lastTraceType?: number | null;
  lastTraceResult?: number | null;
  lastTraceContent?: string | null;
  lastTraceTime?: string | null;
  phoneAlerted?: boolean | null;
  createTime?: string | null;
}

export interface AlarmKpi {
  unhandledFire: number;
  pendingReview: number;
  verifying: number;
  todayHandled: number;
  avgResponseMinutes: number;
}

export interface AlarmMerchantItem {
  id: string;
  name: string;
  addr?: string | null;
  status?: number | null;
  pendingCount: number;
  maxLevel?: number | null;
}

export interface AlarmHandlerItem {
  id: string;
  name: string;
  phone?: string | null;
}

export interface AlarmDispatchOption {
  workAreaId: string;
  workAreaName: string;
  handlers: AlarmHandlerItem[];
}

export interface AlarmTraceItem {
  id: string;
  traceType: number;
  result?: number | null;
  content?: string | null;
  detail?: string | null;
  createTime?: string | null;
}

export interface WorkOrderItem {
  id: string;
  tenantId: string;
  merchantId?: string | null;
  merchantName?: string | null;
  alarmId?: string | null;
  alarmSn?: string | null;
  alarmTime?: string | null;
  alarmLevel?: number | null;
  alarmResult?: number | null;
  workAreaId?: string | null;
  workAreaName?: string | null;
  title: string;
  content?: string | null;
  handlerId?: string | null;
  handlerName?: string | null;
  status: number;
  priority: number;
  source?: string | null;
  acceptTime?: string | null;
  finishTime?: string | null;
  finishRemark?: string | null;
  finishPic1?: string | null;
  finishPic2?: string | null;
  finishPic3?: string | null;
  dangerStatus?: string | null;
  dangerRemainSeconds?: number | null;
  dangerId?: string | null;
  createTime?: string | null;
}

export interface DangerItem {
  id: string;
  merchantId?: string | null;
  merchantName?: string | null;
  alarmId?: string | null;
  sourceAlarmId?: string | null;
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
  assignBy?: string | null;
  assignTime?: string | null;
  dealLimit?: string | null;
  dealBy?: string | null;
  dealTime?: string | null;
  auditTime?: string | null;
  auditByName?: string | null;
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

export interface InspectionPlanItem {
  id: string;
  tenantId: string;
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

export interface InspectionResultItem {
  id: string;
  planId: string;
  merchantId: string;
  merchantName?: string | null;
  userId?: string | null;
  workAreaId?: string | null;
  dangerId?: string | null;
  patrolBy?: string | null;
  patrolTime?: string | null;
  lng?: number | null;
  lat?: number | null;
  result: number;
  remark?: string | null;
  pic1?: string | null;
  pic2?: string | null;
  pic3?: string | null;
}

export interface WorkAreaBrief {
  id: string;
  name: string;
  remark?: string | null;
}

export interface FloorNode {
  id: string;
  name: string;
  floorNo?: number | null;
  floorPlanUrl?: string | null;
  planWidth?: number | null;
  planHeight?: number | null;
  deviceCount: number;
}

export interface BuildingNode {
  id: string;
  name: string;
  sort?: number | null;
  remark?: string | null;
  floors: FloorNode[];
}

export interface MerchantDeviceBrief {
  id: string;
  name?: string | null;
  sn: string;
  type?: string | null;
  typeName?: string | null;
  position?: string | null;
  onlineStatus?: number | null;
  lastTime?: string | null;
  locationText?: string | null;
}

export interface MerchantItem {
  id: string;
  tenantId: string;
  name: string;
  type?: string | null;
  typeName?: string | null;
  merchantKind?: number | null;
  lng?: number | null;
  lat?: number | null;
  addr?: string | null;
  area?: string | null;
  ownerName?: string | null;
  phone?: string | null;
  businessScope?: string | null;
  businessContent?: string | null;
  floorage?: string | null;
  employeeNum?: number | null;
  allowNum?: number | null;
  businessHours?: string | null;
  roundEnv?: string | null;
  placeFlag?: number | null;
  contact1?: string | null;
  phone1?: string | null;
  contact2?: string | null;
  phone2?: string | null;
  businessLicense?: string | null;
  fireRemark?: string | null;
  placePic1?: string | null;
  placePic2?: string | null;
  placePic3?: string | null;
  remark?: string | null;
  insureFlag?: number | null;
  status: number;
  workAreaNames: string[];
  deviceCount: number;
  loginAccount?: string | null;
  createTime?: string | null;
}

export interface MerchantDetail extends MerchantItem {
  workAreas: WorkAreaBrief[];
  buildings: BuildingNode[];
  devices: MerchantDeviceBrief[];
}

export interface BuildingItem {
  id: string;
  tenantId?: string;
  merchantId: string;
  name: string;
  sort?: number | null;
  remark?: string | null;
  status: number;
  deviceCount: number;
}

export interface FloorItem {
  id: string;
  tenantId?: string;
  buildingId: string;
  merchantId: string;
  name: string;
  floorNo?: number | null;
  floorPlanUrl?: string | null;
  planWidth?: number | null;
  planHeight?: number | null;
  sort?: number | null;
  status: number;
  buildingName?: string | null;
  hasPlan: boolean;
  deviceCount?: number | null;
}

export interface MerchantPayload {
  name: string;
  type: string;
  merchantKind: number;
  lng: number;
  lat: number;
  addr: string;
  area?: string;
  ownerName?: string;
  phone: string;
  floorage?: string;
  employeeNum?: number | null;
  fireRemark?: string;
  remark?: string;
  businessLicense?: string;
  placePic1?: string;
  placePic2?: string;
  placePic3?: string;
  status: number;
}

export interface DeviceBriefRow {
  id: string;
  name?: string | null;
  sn: string;
  type?: string | null;
  typeName?: string | null;
  merchantId?: string | null;
  position?: string | null;
  status?: number | null;
  onlineStatus?: number | null;
  lastTime?: string | null;
  locationType?: number | null;
}

export interface DevicePointItem {
  id: string;
  name?: string | null;
  sn: string;
  type?: string | null;
  locationType?: number | null;
  floorX?: number | null;
  floorY?: number | null;
  status?: number | null;
  onlineStatus?: number | null;
}

export interface UploadResult {
  url: string;
}

export interface DeviceItem {
  id: string;
  tenantId?: string;
  name?: string | null;
  sn: string;
  type?: string | null;
  typeName?: string | null;
  merchantId?: string | null;
  merchantName?: string | null;
  maker?: string | null;
  makerName?: string | null;
  position?: string | null;
  lng?: number | null;
  lat?: number | null;
  locationType?: number | null;
  buildingId?: string | null;
  floorId?: string | null;
  buildingName?: string | null;
  floorName?: string | null;
  floorX?: number | null;
  floorY?: number | null;
  locationText?: string | null;
  sim?: string | null;
  cardNo?: string | null;
  channelNum?: number | null;
  status?: number | null;
  onlineStatus?: number | null;
  lastTime?: string | null;
  activeDate?: string | null;
  expireDate?: string | null;
  installDate?: string | null;
  fExt?: string | null;
  createTime?: string | null;
}

export interface DeviceSnapshotItem {
  label: string;
  value: string;
  unit?: string | null;
  time?: string | null;
}

export interface DeviceAlarmBrief {
  id: string;
  alarmTime?: string | null;
  alarmType?: string | null;
  alarmTypeName?: string | null;
  alarmLevel?: number | null;
  status?: number | null;
  result?: number | null;
  position?: string | null;
}

export interface DeviceTrendPoint {
  time: string;
  value?: number | null;
}

export interface DeviceTrendSeries {
  metric: string;
  label: string;
  unit?: string | null;
  max?: number | null;
  min?: number | null;
  avg?: number | null;
  hasData: boolean;
  points: DeviceTrendPoint[];
}

export interface DeviceDetail extends DeviceItem {
  snapshot: DeviceSnapshotItem[];
  trends: DeviceTrendSeries[];
  recentAlarms: DeviceAlarmBrief[];
}

export interface DeviceImportError {
  rowNum: number;
  sn?: string | null;
  message: string;
}

export interface DeviceImportResult {
  total: number;
  success: number;
  failed: number;
  errors: DeviceImportError[];
}

export interface ArchiveDeviceItem {
  deviceId: string;
  name?: string | null;
  sn: string;
  type?: string | null;
  position?: string | null;
  onlineStatus?: number | null;
  lastTime?: string | null;
  measureTime?: string | null;
  measureValue?: number | null;
  measureType?: number | null;
  phaseType?: number | null;
  liveVoltage?: number | null;
  liveCurrent?: number | null;
  residualCurrent?: number | null;
  temperature?: number | null;
  voltageA?: number | null;
  voltageB?: number | null;
  voltageC?: number | null;
  currentA?: number | null;
  currentB?: number | null;
  currentC?: number | null;
  powerA?: number | null;
  powerB?: number | null;
  powerC?: number | null;
}

export interface ArchiveFloorNode {
  floorId: string;
  floorName: string;
  devices: ArchiveDeviceItem[];
}

export interface ArchiveBuildingNode {
  buildingId: string;
  buildingName: string;
  floors: ArchiveFloorNode[];
  devices: ArchiveDeviceItem[];
}

export interface ArchiveMerchantNode {
  merchantId: string;
  merchantName: string;
  buildings: ArchiveBuildingNode[];
  devices: ArchiveDeviceItem[];
}

export interface ScreenTrendPoint {
  time: string;
  value: number;
}

export interface ScreenTypeCount {
  name: string;
  value: number;
}

export interface MerchantStatusPoint {
  merchantId: string;
  name: string;
  lng?: number | null;
  lat?: number | null;
  status: number;
  statusText: string;
  picUrl?: string | null;
  type?: string | null;
  typeName?: string | null;
  alarmTime?: string | null;
  contact?: string | null;
  phone?: string | null;
}

export interface ScreenComprehensive {
  fireTotal: number;
  fireHandled: number;
  fireTrue: number;
  dangerUnhandled: number;
  dangerToday: number;
  dangerOverdue: number;
  patrolMerchants: number;
  patrolPercent: number;
  accessMerchants: number;
  accessDevices: number;
  deviceTotal: number;
  deviceNormal: number;
  deviceAlarm: number;
  deviceFault: number;
  deviceOffline: number;
  normalPercent: number;
  alarmPercent: number;
  faultPercent: number;
  offlinePercent: number;
  merchantPoints: MerchantStatusPoint[];
}

export interface ScreenAlarmItem {
  id: string;
  alarmTime?: string | null;
  merchantName?: string | null;
  position?: string | null;
  alarmLevel?: number | null;
  status?: number | null;
  result?: number | null;
  lng?: number | null;
  lat?: number | null;
}

export interface ScreenAlarm {
  trend: ScreenTrendPoint[];
  alarms: ScreenAlarmItem[];
  merchantPoints: MerchantStatusPoint[];
}

export interface ScreenDangerItem {
  id: string;
  merchantName?: string | null;
  dangerType?: string | null;
  dangerTypeName?: string | null;
  status?: number | null;
  sourceType?: number | null;
  createTime?: string | null;
  dealLimit?: string | null;
}

export interface ScreenDanger {
  monthTotal: number;
  monthDone: number;
  monthUnfinished: number;
  monthOverdue: number;
  trend: ScreenTrendPoint[];
  dangers: ScreenDangerItem[];
  planCount: number;
  planDone: number;
  planDanger: number;
  typeRanking: ScreenTypeCount[];
  merchantPoints: MerchantStatusPoint[];
}

export interface ScreenFaultItem {
  id: string;
  deviceSn?: string | null;
  deviceName?: string | null;
  merchantName?: string | null;
  position?: string | null;
  alarmTime?: string | null;
  status?: number | null;
  recoverTime?: string | null;
}

export interface ScreenDeviceTypeStat {
  type: string;
  typeName: string;
  total: number;
  normal: number;
  alarm: number;
  fault: number;
  offline: number;
  normalPercent: number;
  alarmPercent: number;
  faultPercent: number;
  offlinePercent: number;
}

export interface ScreenDevice {
  types: ScreenDeviceTypeStat[];
  faultTrend: ScreenTrendPoint[];
  faults: ScreenFaultItem[];
  merchantPoints: MerchantStatusPoint[];
}

export interface StatTrendPoint {
  time: string;
  value: number;
}

export interface StatRanking {
  name: string;
  value: number;
}

export interface StatComprehensive {
  alarmTotal: number;
  faultTotal: number;
  dangerTotal: number;
  fireTotal: number;
  callTotal: number;
  alarmTrend: StatTrendPoint[];
  dangerTypeRanking: StatRanking[];
  faultMerchantRanking: StatRanking[];
  callMerchantRanking: StatRanking[];
}

export interface StatAlarmItem {
  id: string;
  sn?: string | null;
  merchantName?: string | null;
  position?: string | null;
  alarmTime?: string | null;
  handleSource?: string | null;
  result?: number | null;
  status?: number | null;
}

export interface StatAlarm {
  trend: StatTrendPoint[];
  page: PageResult<StatAlarmItem>;
}

export interface StatFaultItem extends StatAlarmItem {
  alarmTypeName?: string | null;
}

export interface StatFault {
  trend: StatTrendPoint[];
  page: PageResult<StatFaultItem>;
}

export interface StatDangerType {
  dangerType?: string | null;
  dangerTypeName: string;
  // 后端 VO 对应 Long/Double，统计口径下可能为空
  total: number | null;
  done: number | null;
  undone: number | null;
  overdue: number | null;
  doneRate: number | null;
  overdueRate: number | null;
}

export interface StatDanger {
  total: number | null;
  done: number | null;
  undone: number | null;
  overdue: number | null;
  doneRate: number | null;
  overdueRate: number | null;
  rows: StatDangerType[];
}

export interface StatCallItem {
  id: string;
  merchantName?: string | null;
  alarmTypeName?: string | null;
  alarmTime?: string | null;
  verifyStatus?: number | null;
  callBegin?: string | null;
  calledNo?: string | null;
  fileUrl?: string | null;
}

export interface StatCall {
  page: PageResult<StatCallItem>;
}
