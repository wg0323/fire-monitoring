<template>
  <section class="duty-page">
    <div class="kpi-grid">
      <div v-for="item in kpiCards" :key="item.label" class="kpi-card">
        <div class="kpi-card__label">{{ item.label }}</div>
        <div class="kpi-card__value number" :class="item.tone">{{ item.value }}</div>
      </div>
    </div>

    <div class="duty-body">
      <aside class="merchant-panel page-card">
        <div class="merchant-panel__head">
          <h2>商户</h2>
          <span class="muted">{{ merchants.length }}</span>
        </div>
        <el-input
          v-model="merchantKeyword"
          placeholder="搜索商户名称"
          clearable
          class="merchant-panel__search"
          @keyup.enter="loadMerchants"
        />
        <div class="merchant-list">
          <button
            type="button"
            class="merchant-item"
            :class="{ 'is-active': !selectedMerchantId }"
            @click="selectMerchant('')"
          >
            <span class="merchant-item__name">全部商户</span>
            <span class="merchant-item__count number">{{ totalPending }}</span>
          </button>
          <button
            v-for="merchant in merchants"
            :key="merchant.id"
            type="button"
            class="merchant-item"
            :class="{ 'is-active': selectedMerchantId === merchant.id }"
            @click="selectMerchant(merchant.id)"
          >
            <span class="merchant-item__main">
              <span class="merchant-item__name">{{ merchant.name }}</span>
              <span class="merchant-item__addr">{{ merchant.addr || '未填写地址' }}</span>
            </span>
            <span class="merchant-item__side">
              <span v-if="merchant.maxLevel" class="status-dot" :class="levelDotClass(merchant.maxLevel)" />
              <span v-if="merchant.pendingCount > 0" class="merchant-item__count is-danger number">
                {{ merchant.pendingCount }}
              </span>
            </span>
          </button>
        </div>
      </aside>

      <div class="alarm-panel page-card">
        <div class="alarm-panel__head">
          <div>
            <h1 class="page-title">火警处置</h1>
            <p class="page-subtitle">商户报火警需值守复核；故障报警从上报时间起满 1 小时自动转隐患。</p>
          </div>
          <div class="alarm-panel__tools">
            <el-switch v-model="muted" active-text="静音" inline-prompt @change="toggleMute" />
            <el-switch
              v-model="autoRefresh"
              active-text="自动刷新"
              inactive-text="已暂停"
              inline-prompt
              @change="toggleAutoRefresh"
            />
            <span class="muted">{{ lastRefreshText }}</span>
            <el-button :loading="loading" @click="safeRefresh">刷新</el-button>
          </div>
        </div>

        <el-tabs v-model="activeTab" @tab-change="search">
          <el-tab-pane label="全部" name="all" />
          <el-tab-pane label="火警" name="1" />
          <el-tab-pane label="故障" name="3" />
        </el-tabs>

        <el-table
          v-loading="loading"
          :data="alarms"
          border
          row-key="id"
          :row-class-name="rowClassName"
        >
          <el-table-column label="上报时间" width="165">
            <template #default="{ row }">{{ formatDateTime(row.alarmTime) }}</template>
          </el-table-column>
          <el-table-column prop="sn" label="设备编号" min-width="130" />
          <el-table-column prop="deviceTypeName" label="设备类型" width="105">
            <template #default="{ row }">{{ row.deviceTypeName || row.deviceType || '-' }}</template>
          </el-table-column>
          <el-table-column prop="position" label="位置" min-width="130">
            <template #default="{ row }">{{ row.position || '-' }}</template>
          </el-table-column>
          <el-table-column label="楼层图" width="110">
            <template #default="{ row }">
              <button type="button" class="floor-plan-cell" @click.stop="openFloorPlan(row)">
                <el-image
                  v-if="row.floorPlanUrl"
                  :src="row.floorPlanUrl"
                  fit="cover"
                  class="floor-plan-thumb"
                  alt="楼层平面图"
                />
                <span v-else class="floor-plan-empty">暂无图</span>
                <span class="floor-plan-name">{{ row.floorName || '未绑定楼层' }}</span>
              </button>
            </template>
          </el-table-column>
          <el-table-column label="报警类型" width="110">
            <template #default="{ row }">
              <AlarmLevelTag :level="row.alarmLevel" />
              <span class="status-sub">{{ alarmStatusLabel(row.status) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="电话告警" width="100">
            <template #default="{ row }">
              <el-tag
                v-if="!isDispatched(row)"
                :type="row.phoneAlerted ? 'success' : 'info'"
                effect="plain"
              >
                {{ row.phoneAlerted ? '已告警' : '未告警' }}
              </el-tag>
              <span v-else class="muted">-</span>
            </template>
          </el-table-column>
          <el-table-column label="核警状态" width="110">
            <template #default="{ row }">
              <el-tag :type="verifyTagType(row)" effect="light">{{ verifyStatusText(row) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="处置" min-width="190">
            <template #default="{ row }">
              <el-tag :type="progress(row).type" effect="plain">{{ progress(row).text }}</el-tag>
              <span class="status-sub">{{ row.lastTraceContent || '暂无处置动作' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="330" fixed="right">
            <template #default="{ row }">
              <el-button v-if="canPhoneAlert(row)" link type="primary" @click.stop="openPhoneAlert(row)">
                电话告警
              </el-button>
              <el-button v-if="canVerify(row)" link type="warning" @click.stop="openVerify(row)">
                核警处置
              </el-button>
              <el-button v-if="canDispatch(row)" link type="primary" @click.stop="openDispatch(row)">
                派单核警
              </el-button>
              <el-button link @click.stop="openDetail(row)">详情</el-button>
              <el-button
                v-if="canConvert(row)"
                link
                type="warning"
                @click.stop="convertDanger(row)"
              >
                转隐患
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pager">
          <el-pagination
            v-model:current-page="query.pageNum"
            v-model:page-size="query.pageSize"
            background
            layout="total, sizes, prev, pager, next"
            :total="total"
            :page-sizes="[10, 20, 50, 100]"
            @current-change="loadAlarms"
            @size-change="handleSizeChange"
          />
        </div>
      </div>
    </div>

    <AlarmDetailDrawer
      v-model="drawerVisible"
      :alarm-id="currentId"
      :readonly="isPlatform"
      @refresh="refreshAll"
    />
    <PhoneAlertDialog
      v-model="phoneVisible"
      :alarm="phoneAlarm"
      @connected="handlePhoneConnected"
      @disconnected="handlePhoneDisconnected"
      @success="refreshAll"
    />
    <VerifyDialog v-model="verifyVisible" :alarm="verifyAlarmItem" @success="refreshAll" />
    <FloorPlanDialog v-model="planVisible" :alarm="planAlarm" />
    <DispatchDialog
      v-if="dispatchAlarmId"
      v-model="dispatchVisible"
      :alarm-id="dispatchAlarmId"
      :default-remark="dispatchRemark"
      @success="refreshAll"
    />
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import AlarmDetailDrawer from '@/components/alarm/AlarmDetailDrawer.vue';
import AlarmLevelTag from '@/components/alarm/AlarmLevelTag.vue';
import DispatchDialog from '@/components/alarm/DispatchDialog.vue';
import FloorPlanDialog from '@/components/alarm/FloorPlanDialog.vue';
import PhoneAlertDialog from '@/components/alarm/PhoneAlertDialog.vue';
import VerifyDialog from '@/components/alarm/VerifyDialog.vue';
import {
  convertAlarmDanger,
  getAlarmKpi,
  listAlarmMerchants,
  pageAlarms,
  type AlarmQuery,
} from '@/api/alarm';
import { useAuthStore } from '@/stores/auth';
import type { AlarmItem, AlarmKpi, AlarmMerchantItem } from '@/types/api';
import { playAlarmBeep } from '@/utils/beep';
import {
  alarmResultLabel,
  alarmStatusLabel,
  formatDateTime,
  remainText,
  workOrderStatusLabel,
} from '@/utils/format';

const MUTE_KEY = 'fm-alarm-muted';
const AUTO_REFRESH_KEY = 'fm-alarm-auto-refresh';
const POLL_INTERVAL = 10_000;
const TRACE_CALL_ALARM = 2;
const TRACE_CALL_VERIFY = 3;
const TRACE_DISPATCH = 4;
const TRACE_VERIFY = 5;

type TagType = 'success' | 'info' | 'warning' | 'danger';
interface Progress {
  text: string;
  type: TagType;
}

const auth = useAuthStore();
const isPlatform = computed(() => auth.user?.userType === 'PLATFORM');

const loading = ref(false);
const alarms = ref<AlarmItem[]>([]);
const merchants = ref<AlarmMerchantItem[]>([]);
const total = ref(0);
const merchantKeyword = ref('');
const selectedMerchantId = ref('');
const activeTab = ref('all');
const muted = ref(localStorage.getItem(MUTE_KEY) === '1');
// 默认开启自动刷新，开发环境可直接观察数据变化；手动暂停后记住选择
const autoRefresh = ref(localStorage.getItem(AUTO_REFRESH_KEY) !== '0');
const lastRefreshText = ref('-');
const drawerVisible = ref(false);
const currentId = ref('');
const phoneVisible = ref(false);
const phoneAlarm = ref<AlarmItem | null>(null);
const verifyVisible = ref(false);
const verifyAlarmItem = ref<AlarmItem | null>(null);
const planVisible = ref(false);
const planAlarm = ref<AlarmItem | null>(null);
const dispatchVisible = ref(false);
const dispatchAlarmId = ref('');
const dispatchRemark = ref('');
const kpi = ref<AlarmKpi>({
  unhandledFire: 0,
  pendingReview: 0,
  verifying: 0,
  todayHandled: 0,
  avgResponseMinutes: 0,
});

const query = reactive<AlarmQuery>({ pageNum: 1, pageSize: 20 });
let timer: number | undefined;
let refreshing = false;
let lastPendingFire = -1;

const kpiCards = computed(() => [
  { label: '未处理火警', value: kpi.value.unhandledFire, tone: 'is-danger' },
  { label: '待复核', value: kpi.value.pendingReview, tone: 'is-warning' },
  { label: '核警中', value: kpi.value.verifying, tone: 'is-primary' },
  { label: '今日已处理', value: kpi.value.todayHandled, tone: 'is-success' },
  { label: '平均响应（分钟）', value: kpi.value.avgResponseMinutes, tone: '' },
]);

const totalPending = computed(() =>
  merchants.value.reduce((sum, item) => sum + (item.pendingCount ?? 0), 0));

async function loadKpi(): Promise<void> {
  kpi.value = await getAlarmKpi();
  if (lastPendingFire >= 0 && kpi.value.unhandledFire > lastPendingFire && !muted.value) {
    playAlarmBeep();
  }
  lastPendingFire = kpi.value.unhandledFire;
}

async function loadMerchants(): Promise<void> {
  merchants.value = await listAlarmMerchants(merchantKeyword.value || undefined, true);
}

async function loadAlarms(): Promise<void> {
  loading.value = true;
  try {
    const page = await pageAlarms({
      ...query,
      merchantId: selectedMerchantId.value || undefined,
      level: activeTab.value === 'all' ? undefined : Number(activeTab.value),
      excludeVerified: true,
    });
    alarms.value = page.records;
    total.value = page.total;
    lastRefreshText.value = `刷新于 ${formatDateTime(new Date().toISOString())}`;
  } catch (error) {
    // 失败已由请求拦截器提示，此处仅记录，避免查询/分页/事件处理器产生未处理的 rejection
    console.error('[duty/fire] 加载报警列表失败', error);
  } finally {
    loading.value = false;
  }
}

async function refreshAll(): Promise<void> {
  await Promise.all([loadKpi(), loadMerchants(), loadAlarms()]);
}

function search(): void {
  query.pageNum = 1;
  loadAlarms();
}

function handleSizeChange(size: number): void {
  query.pageSize = size;
  query.pageNum = 1;
  loadAlarms();
}

function selectMerchant(id: string): void {
  selectedMerchantId.value = id;
  query.pageNum = 1;
  loadAlarms();
}

function toggleMute(value: string | number | boolean): void {
  localStorage.setItem(MUTE_KEY, value ? '1' : '0');
}

function toggleAutoRefresh(value: string | number | boolean): void {
  localStorage.setItem(AUTO_REFRESH_KEY, value ? '1' : '0');
}

function isPendingReview(row: AlarmItem): boolean {
  return row.status === 0 && row.verifyStatus === 1 && row.handleSource === 'MERCHANT';
}

function verifyStatusText(row: AlarmItem): string {
  if (isPendingReview(row)) {
    return '待复核';
  }
  if (row.verifyStatus === 2) {
    return '已核警';
  }
  if (row.verifyStatus === 1) {
    return '核警中';
  }
  return '待核警';
}

function verifyTagType(row: AlarmItem): TagType {
  if (isPendingReview(row)) {
    return 'danger';
  }
  if (row.verifyStatus === 2) {
    return 'success';
  }
  if (row.verifyStatus === 1) {
    return 'warning';
  }
  return 'info';
}

function hasActiveWorkOrder(row: AlarmItem): boolean {
  return row.workOrderStatus === 0 || row.workOrderStatus === 1;
}

function isFaultRow(row: AlarmItem): boolean {
  return row.result !== 0 && (row.alarmLevel === 3 || row.result === 2);
}

function canHandle(row: AlarmItem): boolean {
  return !isPlatform.value && row.status === 0 && !isFaultRow(row);
}

function isDispatched(row: AlarmItem): boolean {
  return Boolean(row.workOrderId) || row.lastTraceType === TRACE_DISPATCH;
}

function canPhoneAlert(row: AlarmItem): boolean {
  return canHandle(row) && !isDispatched(row);
}

function canVerify(row: AlarmItem): boolean {
  return canHandle(row) && !hasActiveWorkOrder(row);
}

function canDispatch(row: AlarmItem): boolean {
  return canHandle(row) && !hasActiveWorkOrder(row) && !row.workOrderId && row.verifyStatus !== 2;
}

function progress(row: AlarmItem): Progress {
  if (row.dangerId) {
    return { text: '已转隐患', type: 'success' };
  }
  if (row.selfRecovered) {
    return { text: '已自行恢复', type: 'info' };
  }
  if (row.lastTraceType === TRACE_VERIFY) {
    return { text: `已核警 · ${alarmResultLabel(row.result)}`, type: 'success' };
  }
  if (row.workOrderId) {
    return { text: `已派单核警 · ${workOrderStatusLabel(row.workOrderStatus)}`, type: 'warning' };
  }
  if (row.lastTraceType === TRACE_DISPATCH) {
    return { text: '已派单核警', type: 'warning' };
  }
  if (row.lastTraceType === TRACE_CALL_ALARM || row.lastTraceType === TRACE_CALL_VERIFY) {
    return row.lastTraceResult === 2
      ? { text: '电话告警未接通 · 待派单核警', type: 'danger' }
      : { text: '电话告警已接通 · 待核警处置', type: 'warning' };
  }
  if (row.dangerConvertDeadline) {
    return { text: `待转隐患 ${remainText(row.dangerRemainSeconds)}`, type: 'warning' };
  }
  if (row.result !== null && row.result !== undefined) {
    return { text: alarmResultLabel(row.result), type: 'info' };
  }
  return { text: '待处置', type: 'info' };
}

function openPhoneAlert(row: AlarmItem): void {
  phoneAlarm.value = row;
  phoneVisible.value = true;
}

function openVerify(row: AlarmItem): void {
  verifyAlarmItem.value = row;
  verifyVisible.value = true;
}

function openFloorPlan(row: AlarmItem): void {
  planAlarm.value = row;
  planVisible.value = true;
}

function openDispatch(row: AlarmItem): void {
  dispatchAlarmId.value = row.id;
  dispatchRemark.value = '';
  dispatchVisible.value = true;
}

function handlePhoneConnected(): void {
  if (!phoneAlarm.value) {
    return;
  }
  if (isFaultRow(phoneAlarm.value)) {
    ElMessage.warning('故障报警请在隐患流程中处理');
    return;
  }
  if (!canVerify(phoneAlarm.value)) {
    ElMessage.warning('该报警已有在途工单或已处理，请由外勤完成工单后再核警');
    return;
  }
  verifyAlarmItem.value = phoneAlarm.value;
  verifyVisible.value = true;
}

function handlePhoneDisconnected(): void {
  if (!phoneAlarm.value) {
    return;
  }
  if (isFaultRow(phoneAlarm.value)) {
    ElMessage.warning('故障报警请在隐患流程中处理');
    return;
  }
  if (!canDispatch(phoneAlarm.value)) {
    ElMessage.warning('该报警已派单或已核警，无需重复派单');
    return;
  }
  dispatchAlarmId.value = phoneAlarm.value.id;
  dispatchRemark.value = '电话告警未接通，需派单核警';
  dispatchVisible.value = true;
}

function canConvert(row: AlarmItem): boolean {
  return isFaultRow(row) && !row.dangerId && !row.selfRecovered && !isPlatform.value;
}

function levelDotClass(level: number): string {
  if (level === 1) {
    return 'is-danger';
  }
  if (level === 2) {
    return 'is-warning';
  }
  return 'is-fault';
}

function rowClassName({ row }: { row: AlarmItem }): string {
  return isPendingReview(row) ? 'row-pending-review' : '';
}

function openDetail(row: AlarmItem): void {
  currentId.value = row.id;
  drawerVisible.value = true;
}

async function convertDanger(row: AlarmItem): Promise<void> {
  try {
    await ElMessageBox.confirm('将生成一条待指派隐患，确认转隐患？', '转隐患确认', {
      type: 'warning',
      confirmButtonText: '转隐患',
      cancelButtonText: '取消',
    });
  } catch {
    return;
  }
  await convertAlarmDanger(row.id);
  ElMessage.success('已生成待指派隐患');
  await refreshAll();
}

function safeRefresh(): void {
  if (refreshing) {
    return;
  }
  refreshing = true;
  refreshAll()
    .catch(() => {
      // 单次接口失败保留当前列表，等待下一轮自动刷新
    })
    .finally(() => {
      refreshing = false;
    });
}

function handleVisibility(): void {
  if (autoRefresh.value && !document.hidden) {
    safeRefresh();
  }
}

// 开发环境常驻 DevTools 调试，页面隐藏时也保持轮询；生产环境仅在前台刷新
onMounted(() => {
  safeRefresh();
  timer = window.setInterval(() => {
    if (autoRefresh.value && (import.meta.env.DEV || !document.hidden)) {
      safeRefresh();
    }
  }, POLL_INTERVAL);
  document.addEventListener('visibilitychange', handleVisibility);
});

onBeforeUnmount(() => {
  if (timer) {
    window.clearInterval(timer);
  }
  document.removeEventListener('visibilitychange', handleVisibility);
});
</script>

<style scoped>
.duty-page {
  display: flex;
  flex-direction: column;
  gap: var(--fm-space-4);
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: var(--fm-space-4);
}

.kpi-card {
  padding: var(--fm-space-4);
  background: var(--fm-card-bg);
  border: 1px solid var(--fm-border);
  border-radius: var(--fm-radius-lg);
  box-shadow: var(--fm-shadow-card);
}

.kpi-card__label {
  color: var(--fm-text-secondary);
  font-size: 13px;
}

.kpi-card__value {
  margin-top: var(--fm-space-2);
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
}

.is-danger {
  color: var(--fm-danger);
}

.is-warning {
  color: var(--fm-warning);
}

.is-primary {
  color: var(--fm-primary);
}

.is-success {
  color: var(--fm-success);
}

.duty-body {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: var(--fm-space-4);
  align-items: start;
}

.merchant-panel {
  padding: var(--fm-space-4);
}

.merchant-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--fm-space-3);
}

.merchant-panel__head h2 {
  font-size: 15px;
  font-weight: 600;
}

.merchant-panel__search {
  margin-bottom: var(--fm-space-3);
}

.merchant-list {
  display: flex;
  flex-direction: column;
  max-height: 620px;
  overflow-y: auto;
}

.merchant-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--fm-space-2);
  width: 100%;
  padding: 10px 8px;
  border: none;
  border-left: 3px solid transparent;
  background: transparent;
  color: var(--fm-text);
  text-align: left;
  cursor: pointer;
}

.merchant-item:hover {
  background: #f9fafb;
}

.merchant-item.is-active {
  border-left-color: var(--fm-primary);
  background: var(--fm-primary-soft);
}

.merchant-item__main {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.merchant-item__name {
  overflow: hidden;
  font-size: 13px;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.merchant-item__addr {
  overflow: hidden;
  color: var(--fm-text-muted);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.merchant-item__side {
  display: flex;
  align-items: center;
  gap: 6px;
}

.merchant-item__count {
  min-width: 20px;
  padding: 0 6px;
  border-radius: 999px;
  background: #f2f4f7;
  color: var(--fm-text-secondary);
  font-size: 12px;
  line-height: 18px;
  text-align: center;
}

.merchant-item__count.is-danger {
  background: var(--fm-danger-soft);
  color: var(--fm-danger);
}

.alarm-panel {
  min-width: 0;
}

.alarm-panel__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--fm-space-4);
}

.alarm-panel__tools {
  display: flex;
  align-items: center;
  gap: var(--fm-space-3);
}

:deep(.row-pending-review) {
  background: var(--fm-danger-soft);
}

:deep(.row-pending-review td:first-child) {
  border-left: 3px solid var(--fm-danger);
}

.status-dot.is-danger {
  background: var(--fm-danger);
}

.status-dot.is-warning {
  background: var(--fm-warning);
}

.status-dot.is-fault {
  background: var(--fm-accent);
}

.status-sub {
  display: block;
  max-width: 100%;
  margin-top: 2px;
  overflow: hidden;
  color: var(--fm-text-muted);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.floor-plan-cell {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  padding: 0;
  border: none;
  background: transparent;
  color: var(--fm-text);
  cursor: pointer;
}

.floor-plan-thumb {
  width: 64px;
  height: 44px;
  border: 1px solid var(--fm-border);
  border-radius: var(--fm-radius-sm);
}

.floor-plan-empty {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 44px;
  border: 1px dashed var(--fm-border);
  border-radius: var(--fm-radius-sm);
  color: var(--fm-text-muted);
  font-size: 12px;
}

.floor-plan-name {
  max-width: 96px;
  overflow: hidden;
  color: var(--fm-text-secondary);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 1439px) {
  .kpi-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
