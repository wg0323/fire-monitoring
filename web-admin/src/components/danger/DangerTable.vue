<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1 class="page-title">{{ isDuty ? '隐患处置' : '隐患跟踪' }}</h1>
        <p class="page-subtitle">
          {{
            isDuty
              ? '只展示未完结的隐患：待指派可指派外勤，待审核可审批；审核通过后转入隐患跟踪。'
              : '全部隐患的处置记录，仅查看；指派与审核请到「安全值守 / 隐患处置」。'
          }}
        </p>
      </div>
      <el-tag effect="plain">{{ total }} 条</el-tag>
    </div>

    <el-tabs v-if="isDuty" v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="全部未完结" name="unfinished" />
      <el-tab-pane label="待指派" name="0" />
      <el-tab-pane label="待整改" name="1" />
      <el-tab-pane label="待审核" name="2" />
    </el-tabs>

    <div class="filter-bar">
      <el-select v-if="!isDuty" v-model="query.status" placeholder="状态" clearable style="width: 130px">
        <el-option label="待指派" :value="0" />
        <el-option label="待整改" :value="1" />
        <el-option label="待审核" :value="2" />
        <el-option label="已完成" :value="3" />
      </el-select>
      <el-select v-model="query.sourceType" placeholder="来源" clearable style="width: 130px">
        <el-option label="随手拍" :value="0" />
        <el-option label="巡查" :value="1" />
        <el-option label="设备故障" :value="2" />
      </el-select>
      <el-select v-model="query.dangerType" placeholder="隐患类型" clearable style="width: 150px">
        <el-option v-for="item in dangerTypes" :key="item.code" :label="item.name" :value="item.code" />
      </el-select>
      <el-date-picker
        v-model="limitRange"
        type="daterange"
        value-format="YYYY-MM-DD"
        start-placeholder="整改期限起"
        end-placeholder="整改期限止"
        style="width: 260px"
        @change="handleLimitChange"
      />
      <el-input
        v-model="query.keyword"
        placeholder="位置 / 描述"
        clearable
        style="width: 200px"
        @keyup.enter="search"
      />
      <div class="filter-actions">
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </div>
    </div>

    <div v-if="isDuty" class="table-toolbar">
      <span class="muted">已选 {{ selectedRows.length }} / {{ MAX_BATCH_ASSIGN }} 条</span>
      <el-button
        v-auth="'danger:assign'"
        type="primary"
        :disabled="selectedRows.length === 0"
        @click="openBatchAssign"
      >
        批量指派
      </el-button>
    </div>

    <el-table
      ref="tableRef"
      v-loading="loading"
      :data="rows"
      border
      row-key="id"
      @selection-change="handleSelectionChange"
    >
      <el-table-column
        v-if="isDuty"
        type="selection"
        width="46"
        reserve-selection
        :selectable="selectableForAssign"
      />
      <el-table-column label="等级" width="90">
        <template #default="{ row }">
          <el-tag :type="dangerLevelTone(row)" effect="plain">{{ dangerLevelText(row) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="分类" min-width="110">
        <template #default="{ row }">{{ row.dangerType || '-' }}</template>
      </el-table-column>
      <el-table-column label="位置" min-width="160">
        <template #default="{ row }">{{ row.position || row.positionDetail || '-' }}</template>
      </el-table-column>
      <el-table-column label="整改期限" width="140">
        <template #default="{ row }">
          <span :class="{ 'is-overdue': isOverdue(row) }">{{ overdueText(row) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTone(row.status)" effect="plain">{{ dangerRecordStatusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="上报时间" width="165">
        <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="照片" width="70">
        <template #default="{ row }">{{ picCount(row) }}</template>
      </el-table-column>
      <el-table-column label="商户" min-width="150">
        <template #default="{ row }">{{ row.merchantName || '-' }}</template>
      </el-table-column>
      <el-table-column label="来源" width="100">
        <template #default="{ row }">{{ dangerSourceLabel(row.sourceType) }}</template>
      </el-table-column>
      <el-table-column label="操作" :width="isDuty ? 150 : 90" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">{{ isDuty ? '详情' : '查看' }}</el-button>
          <template v-if="isDuty">
            <el-button
              v-if="canAssign(row)"
              v-auth="'danger:assign'"
              link
              type="primary"
              @click="openAssign(row)"
            >
              指派
            </el-button>
            <el-button
              v-if="canAudit(row)"
              v-auth="'danger:audit'"
              link
              type="primary"
              @click="openAudit(row)"
            >
              审核
            </el-button>
          </template>
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
        @current-change="load"
        @size-change="handleSizeChange"
      />
    </div>

    <el-drawer v-model="drawerVisible" title="隐患详情" size="640px" destroy-on-close>
      <div v-loading="detailLoading" class="danger-detail">
        <template v-if="detail">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="隐患编号" :span="2">{{ detail.id }}</el-descriptions-item>
            <el-descriptions-item label="商户">{{ detail.merchantName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="工作区">{{ detail.workAreaName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="类型">{{ detail.dangerType || '-' }}</el-descriptions-item>
            <el-descriptions-item label="来源">{{ dangerSourceLabel(detail.sourceType) }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              {{ dangerRecordStatusLabel(detail.status) }}
            </el-descriptions-item>
            <el-descriptions-item label="等级">{{ dangerLevelText(detail) }}</el-descriptions-item>
            <el-descriptions-item label="位置" :span="2">
              {{ detail.position || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="描述" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
            <el-descriptions-item label="责任人">{{ detail.assigneeName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="整改期限">{{ detail.dealLimit || '-' }}</el-descriptions-item>
          </el-descriptions>

          <div v-if="detailPics.length > 0" class="detail-block">
            <h3>现场照片</h3>
            <el-image
              v-for="pic in detailPics"
              :key="pic"
              :src="pic"
              :preview-src-list="detailPics"
              fit="cover"
              class="detail-pic"
              alt="隐患现场照片"
            />
          </div>

          <div class="detail-block">
            <h3>处理时间线</h3>
            <el-timeline>
              <el-timeline-item
                v-for="node in timeline"
                :key="node.nodeKey"
                :timestamp="node.occurred ? formatDateTime(node.time) : '未发生'"
                :type="node.result === 2 ? 'danger' : node.occurred ? 'primary' : 'info'"
                placement="top"
              >
                <div class="timeline-title">
                  {{ node.title }}
                  <span v-if="node.extra" class="timeline-extra">{{ convertSourceText(node.extra) }}</span>
                </div>
                <div v-if="node.occurred" class="timeline-content">
                  <span v-if="node.operator">操作人：{{ node.operator }}</span>
                  <span v-if="node.content">{{ node.content }}</span>
                </div>
              </el-timeline-item>
            </el-timeline>
          </div>

          <div v-if="lastDealPics.length > 0 || detail.lastDealRemark" class="detail-block">
            <h3>整改记录</h3>
            <p class="timeline-content">
              {{ detail.lastDealUserName || '-' }} · {{ formatDateTime(detail.lastDealTime) }}
            </p>
            <p class="timeline-content">{{ detail.lastDealRemark || '-' }}</p>
            <el-image
              v-for="pic in lastDealPics"
              :key="pic"
              :src="pic"
              :preview-src-list="lastDealPics"
              fit="cover"
              class="detail-pic"
              alt="整改照片"
            />
          </div>

          <div v-if="detail.auditTime" class="detail-block">
            <h3>审核意见</h3>
            <p class="timeline-content">
              {{ detail.auditResult === 4 ? '驳回' : '通过' }} · {{ formatDateTime(detail.auditTime) }}
            </p>
            <p v-if="detail.auditByName" class="timeline-content">审核人：{{ detail.auditByName }}</p>
            <p class="timeline-content">{{ detail.auditRemark || '-' }}</p>
          </div>
        </template>
        <el-empty v-else-if="!detailLoading" description="隐患不存在" />
      </div>
      <template #footer>
        <div class="drawer-footer">
          <template v-if="isDuty">
            <el-button
              v-if="detail && canAssign(detail)"
              v-auth="'danger:assign'"
              type="primary"
              @click="openAssign(detail)"
            >
              指派外勤
            </el-button>
            <el-button
              v-if="detail && canAudit(detail)"
              v-auth="'danger:audit'"
              type="primary"
              @click="openAudit(detail)"
            >
              审核
            </el-button>
          </template>
          <el-button @click="drawerVisible = false">关闭</el-button>
        </div>
      </template>
    </el-drawer>

    <el-dialog v-model="assignVisible" title="指派外勤" width="480px" destroy-on-close>
      <el-form label-width="96px">
        <el-form-item label="工作区">
          <el-input :model-value="currentDanger?.workAreaName || '未绑定工作区'" disabled />
        </el-form-item>
        <el-form-item label="外勤">
          <el-select v-model="assignForm.assigneeId" placeholder="请选择外勤" style="width: 100%">
            <el-option
              v-for="handler in assignHandlers"
              :key="handler.id"
              :label="handler.name"
              :value="handler.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="整改期限">
          <el-date-picker
            v-model="assignForm.dealLimit"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="assignForm.remark" type="textarea" :rows="3" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="assignLoading" @click="submitAssign">确定指派</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="batchAssignVisible" title="批量指派" width="520px" destroy-on-close>
      <el-alert
        v-if="batchAssignWarning"
        class="batch-alert"
        :title="batchAssignWarning"
        type="warning"
        :closable="false"
        show-icon
      />
      <el-form label-width="96px">
        <el-form-item label="已选隐患">{{ selectedRows.length }} 条</el-form-item>
        <el-form-item label="工作区">
          <el-input :model-value="batchWorkAreaText" disabled />
        </el-form-item>
        <el-form-item label="外勤">
          <el-select
            v-model="batchAssignForm.assigneeId"
            placeholder="请选择外勤"
            style="width: 100%"
            :disabled="!batchWorkAreaId"
          >
            <el-option
              v-for="handler in batchAssignHandlers"
              :key="handler.id"
              :label="handler.name"
              :value="handler.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="整改期限">
          <el-date-picker
            v-model="batchAssignForm.dealLimit"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="batchAssignForm.remark" type="textarea" :rows="3" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchAssignVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="batchAssignLoading"
          :disabled="Boolean(batchAssignWarning)"
          @click="submitBatchAssign"
        >
          确定指派
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="auditVisible" title="隐患审核" width="480px" destroy-on-close>
      <el-form label-width="96px">
        <el-form-item label="审核结论">
          <el-radio-group v-model="auditForm.auditResult">
            <el-radio :value="1">通过</el-radio>
            <el-radio :value="4">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核意见" :required="auditForm.auditResult === 4">
          <el-input v-model="auditForm.auditRemark" type="textarea" :rows="3" maxlength="300" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditVisible = false">取消</el-button>
        <el-button type="primary" :loading="auditLoading" @click="submitAudit">提交审核</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox, type TableInstance } from 'element-plus';
import {
  assignDanger,
  auditDanger,
  batchAssignDanger,
  getDanger,
  getDangerTimeline,
  listDangerTypes,
  pageDangers,
  type DangerAssignPayload,
  type DangerAuditPayload,
  type DangerBatchAssignPayload,
  type DangerBatchAssignResult,
  type DangerQuery,
} from '@/api/danger';
import { listAlarmDispatchOptions } from '@/api/alarm';
import type {
  AlarmDispatchOption,
  DangerItem,
  DangerTimelineNode,
  DictItem,
} from '@/types/api';
import { useAuthStore } from '@/stores/auth';
import {
  dangerRecordStatusLabel,
  dangerSourceLabel,
  formatDateTime,
} from '@/utils/format';

const props = withDefaults(defineProps<{ mode: 'duty' | 'trace' }>(), { mode: 'trace' });

const MAX_BATCH_ASSIGN = 100;

const isDuty = computed(() => props.mode === 'duty');
const auth = useAuthStore();
const canOperate = computed(() =>
  auth.roles.includes('TENANT_ADMIN') || auth.roles.includes('TENANT_DUTY'));
const loading = ref(false);
const detailLoading = ref(false);
const assignLoading = ref(false);
const auditLoading = ref(false);
const rows = ref<DangerItem[]>([]);
const total = ref(0);
const dangerTypes = ref<DictItem[]>([]);
const limitRange = ref<[string, string] | null>(null);
const activeTab = ref('unfinished');
const drawerVisible = ref(false);
const assignVisible = ref(false);
const auditVisible = ref(false);
const detail = ref<DangerItem | null>(null);
const timeline = ref<DangerTimelineNode[]>([]);
const dispatchOptions = ref<AlarmDispatchOption[]>([]);
const currentDanger = ref<DangerItem | null>(null);
const tableRef = ref<TableInstance>();
const selectedRows = ref<DangerItem[]>([]);
const batchAssignVisible = ref(false);
const batchAssignLoading = ref(false);

const query = reactive<DangerQuery>({
  pageNum: 1,
  pageSize: 20,
  unfinished: isDuty.value ? true : undefined,
});

const assignForm = reactive<DangerAssignPayload>({
  assigneeId: '',
});

const batchAssignForm = reactive<Omit<DangerBatchAssignPayload, 'ids'>>({
  assigneeId: '',
  dealLimit: undefined,
  remark: '',
});

const auditForm = reactive<DangerAuditPayload>({
  auditResult: 1,
  auditRemark: '',
});

const detailPics = computed(() =>
  [detail.value?.pic1, detail.value?.pic2, detail.value?.pic3]
    .filter((pic): pic is string => Boolean(pic)));

const lastDealPics = computed(() =>
  [detail.value?.lastDealPic1, detail.value?.lastDealPic2, detail.value?.lastDealPic3]
    .filter((pic): pic is string => Boolean(pic)));

const assignHandlers = computed(() => {
  const workAreaId = currentDanger.value?.workAreaId;
  if (!workAreaId) {
    return [];
  }
  return dispatchOptions.value.find((option) => option.workAreaId === workAreaId)?.handlers ?? [];
});

const batchWorkAreaIds = computed(() => {
  const ids = new Set<string>();
  selectedRows.value.forEach((row) => {
    if (row.workAreaId) {
      ids.add(row.workAreaId);
    }
  });
  return ids;
});

const batchWorkAreaId = computed(() => {
  if (batchWorkAreaIds.value.size !== 1) {
    return '';
  }
  return [...batchWorkAreaIds.value][0];
});

const batchWorkAreaText = computed(() => {
  if (batchWorkAreaIds.value.size > 1) {
    return `${batchWorkAreaIds.value.size} 个工作区`;
  }
  if (!batchWorkAreaId.value) {
    return '未绑定工作区';
  }
  return selectedRows.value.find((row) => row.workAreaId === batchWorkAreaId.value)?.workAreaName
    || batchWorkAreaId.value;
});

const batchAssignHandlers = computed(() => {
  if (!batchWorkAreaId.value) {
    return [];
  }
  return dispatchOptions.value.find((option) => option.workAreaId === batchWorkAreaId.value)?.handlers ?? [];
});

const batchAssignWarning = computed(() => {
  if (selectedRows.value.length === 0) {
    return '请先勾选待指派的隐患';
  }
  if (selectedRows.value.length > MAX_BATCH_ASSIGN) {
    return `单次最多指派 ${MAX_BATCH_ASSIGN} 条，请分批提交`;
  }
  if (batchWorkAreaIds.value.size > 1) {
    return '所选隐患分属多个工作区，请只勾选同一工作区的隐患';
  }
  if (!batchWorkAreaId.value) {
    return '所选隐患未绑定工作区，请先在商户管理中绑定工作区';
  }
  if (batchAssignHandlers.value.length === 0) {
    return '该工作区暂无可指派的外勤';
  }
  return '';
});

async function load(): Promise<void> {
  loading.value = true;
  try {
    const page = await pageDangers(query);
    rows.value = page.records;
    total.value = page.total;
  } finally {
    loading.value = false;
  }
}

async function loadTypes(): Promise<void> {
  dangerTypes.value = await listDangerTypes();
}

function search(): void {
  query.pageNum = 1;
  tableRef.value?.clearSelection();
  load();
}

function handleTabChange(): void {
  tableRef.value?.clearSelection();
  if (activeTab.value === 'unfinished') {
    query.unfinished = true;
    query.status = undefined;
  } else {
    query.unfinished = undefined;
    query.status = Number(activeTab.value);
  }
  search();
}

function handleSizeChange(size: number): void {
  query.pageSize = size;
  query.pageNum = 1;
  load();
}

function handleLimitChange(value: [string, string] | null): void {
  query.dealLimitFrom = value?.[0];
  query.dealLimitTo = value?.[1];
}

function resetQuery(): void {
  query.status = undefined;
  query.unfinished = isDuty.value ? true : undefined;
  activeTab.value = 'unfinished';
  query.sourceType = undefined;
  query.dangerType = undefined;
  query.keyword = '';
  query.dealLimitFrom = undefined;
  query.dealLimitTo = undefined;
  limitRange.value = null;
  search();
}

function statusTone(status?: number | null): 'info' | 'warning' | 'primary' | 'success' | 'danger' {
  if (status === 0) {
    return 'info';
  }
  if (status === 1) {
    return 'warning';
  }
  if (status === 2) {
    return 'primary';
  }
  if (status === 3) {
    return 'success';
  }
  return 'danger';
}

function dangerLevelText(danger: DangerItem): string {
  const level = danger.dangerLevelFinal ?? danger.dangerLevel;
  return level === 1 ? '重大' : '普通';
}

function dangerLevelTone(danger: DangerItem): 'danger' | 'info' {
  const level = danger.dangerLevelFinal ?? danger.dangerLevel;
  return level === 1 ? 'danger' : 'info';
}

function picCount(danger: DangerItem): number {
  return [danger.pic1, danger.pic2, danger.pic3].filter(Boolean).length;
}

function isOverdue(danger: DangerItem): boolean {
  const limit = danger.dealLimit;
  if (!limit) {
    return false;
  }
  return danger.status !== 3 && limit < new Date().toISOString().slice(0, 10);
}

function overdueText(danger: DangerItem): string {
  if (!danger.dealLimit) {
    return '-';
  }
  return isOverdue(danger) ? `${danger.dealLimit}（逾期）` : danger.dealLimit;
}

function canAssign(danger: DangerItem): boolean {
  return canOperate.value && danger.status === 0;
}

function canAudit(danger: DangerItem): boolean {
  return canOperate.value && danger.status === 2;
}

function selectableForAssign(row: DangerItem): boolean {
  return row.status === 0;
}

function handleSelectionChange(rows: DangerItem[]): void {
  selectedRows.value = rows.filter((row) => row.status === 0);
}

function convertSourceText(extra?: string | null): string {
  if (extra === 'AUTO') {
    return '自动转';
  }
  if (extra === 'MANUAL') {
    return '人工转';
  }
  return '';
}

async function openDetail(row: DangerItem): Promise<void> {
  detail.value = row;
  drawerVisible.value = true;
  detailLoading.value = true;
  try {
    const [record, nodes] = await Promise.all([getDanger(row.id), getDangerTimeline(row.id)]);
    detail.value = record;
    timeline.value = nodes;
  } finally {
    detailLoading.value = false;
  }
}

async function openAssign(row: DangerItem): Promise<void> {
  currentDanger.value = row;
  assignForm.assigneeId = '';
  assignForm.dealLimit = undefined;
  assignForm.remark = '';
  if (dispatchOptions.value.length === 0) {
    dispatchOptions.value = await listAlarmDispatchOptions();
  }
  assignVisible.value = true;
}

async function submitAssign(): Promise<void> {
  if (!currentDanger.value || !assignForm.assigneeId) {
    ElMessage.warning('请选择外勤');
    return;
  }
  assignLoading.value = true;
  try {
    await assignDanger(currentDanger.value.id, { ...assignForm });
    ElMessage.success('已指派');
    assignVisible.value = false;
    await load();
    if (drawerVisible.value && detail.value) {
      await refreshDetail(detail.value.id);
    }
  } finally {
    assignLoading.value = false;
  }
}

async function openBatchAssign(): Promise<void> {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先勾选待指派的隐患');
    return;
  }
  batchAssignForm.assigneeId = '';
  batchAssignForm.dealLimit = undefined;
  batchAssignForm.remark = '';
  if (dispatchOptions.value.length === 0) {
    dispatchOptions.value = await listAlarmDispatchOptions();
  }
  batchAssignVisible.value = true;
}

async function submitBatchAssign(): Promise<void> {
  if (batchAssignWarning.value) {
    ElMessage.warning(batchAssignWarning.value);
    return;
  }
  if (!batchAssignForm.assigneeId) {
    ElMessage.warning('请选择外勤');
    return;
  }
  batchAssignLoading.value = true;
  const submitted = selectedRows.value.slice();
  try {
    const result = await batchAssignDanger({
      ids: submitted.map((row) => row.id),
      assigneeId: batchAssignForm.assigneeId,
      dealLimit: batchAssignForm.dealLimit,
      remark: batchAssignForm.remark,
    });
    batchAssignVisible.value = false;
    tableRef.value?.clearSelection();
    await load();
    if (result.failedCount === 0) {
      ElMessage.success(`已指派 ${result.successCount} 条`);
      return;
    }
    const rowMap = new Map(submitted.map((row) => [row.id, row]));
    const describe = (item: DangerBatchAssignResult['failures'][number]): string => {
      const row = rowMap.get(item.id);
      const label = row
        ? `${row.merchantName ?? '-'} ${row.position || row.positionDetail || ''}`.trim()
        : item.id;
      return `${label}：${item.reason}`;
    };
    const preview = result.failures.slice(0, 5).map(describe).join('；');
    const more = result.failures.length > 5 ? `；等共 ${result.failedCount} 条失败` : '';
    ElMessageBox.alert(
      `成功 ${result.successCount} 条，失败 ${result.failedCount} 条。${preview}${more}`,
      '批量指派结果',
      { type: 'warning' },
    );
  } finally {
    batchAssignLoading.value = false;
  }
}

async function openAudit(row: DangerItem): Promise<void> {
  currentDanger.value = row;
  auditForm.auditResult = 1;
  auditForm.auditRemark = '';
  auditVisible.value = true;
}

async function submitAudit(): Promise<void> {
  if (!currentDanger.value) {
    return;
  }
  if (auditForm.auditResult === 4 && !(auditForm.auditRemark ?? '').trim()) {
    ElMessage.warning('驳回必须填写原因');
    return;
  }
  await ElMessageBox.confirm(
    auditForm.auditResult === 4 ? '驳回后隐患退回待整改，确认提交？' : '审核通过后不可再修改，确认提交？',
    '审核确认',
    { type: 'warning' },
  );
  auditLoading.value = true;
  try {
    await auditDanger(currentDanger.value.id, { ...auditForm });
    ElMessage.success('审核已提交');
    auditVisible.value = false;
    await load();
    if (drawerVisible.value && detail.value) {
      await refreshDetail(detail.value.id);
    }
  } finally {
    auditLoading.value = false;
  }
}

async function refreshDetail(id: string): Promise<void> {
  const [record, nodes] = await Promise.all([getDanger(id), getDangerTimeline(id)]);
  detail.value = record;
  timeline.value = nodes;
}

onMounted(() => {
  loadTypes();
  load();
});
</script>

<style scoped>
.danger-detail {
  min-height: 200px;
}

.detail-block {
  margin-top: var(--fm-space-5);
}

.detail-block h3 {
  margin-bottom: var(--fm-space-3);
  color: var(--fm-text);
  font-size: 14px;
  font-weight: 600;
}

.detail-pic {
  width: 112px;
  height: 84px;
  margin-right: var(--fm-space-2);
  border-radius: var(--fm-radius-sm);
}

.timeline-title {
  color: var(--fm-text);
  font-weight: 600;
}

.timeline-extra {
  margin-left: var(--fm-space-2);
  padding: 0 6px;
  border-radius: 999px;
  background: var(--fm-primary-soft);
  color: var(--fm-primary);
  font-size: 12px;
  font-weight: 400;
}

.timeline-content {
  display: block;
  margin-top: var(--fm-space-1);
  color: var(--fm-text-secondary);
  font-size: 13px;
  line-height: 20px;
}

.is-overdue {
  color: var(--fm-danger);
}

.batch-alert {
  margin-bottom: var(--fm-space-3);
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--fm-space-2);
}
</style>
