<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1 class="page-title">巡检管理</h1>
        <p class="page-subtitle">计划按工作区创建，只覆盖该工作区商户；外勤仅能看到所属工作区的计划。</p>
      </div>
      <el-tag effect="plain">工作区覆盖 {{ coverageText }}</el-tag>
    </div>

    <div class="filter-bar">
      <el-input
        v-model="query.name"
        placeholder="计划名称"
        clearable
        style="width: 200px"
        @keyup.enter="search"
      />
      <el-select v-model="query.workAreaId" placeholder="工作区" clearable style="width: 180px">
        <el-option
          v-for="area in workAreas"
          :key="area.id"
          :label="area.name"
          :value="area.id"
        />
      </el-select>
      <div class="filter-actions">
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
        <el-button v-if="!auth.isPlatform" type="primary" plain @click="openCreate">新建计划</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="rows" border row-key="id">
      <el-table-column prop="name" label="计划名称" min-width="160" />
      <el-table-column label="工作区" min-width="130">
        <template #default="{ row }">{{ row.workAreaName || '-' }}</template>
      </el-table-column>
      <el-table-column label="起止日期" width="200">
        <template #default="{ row }">{{ row.startDate }} 至 {{ row.endDate }}</template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="planTone(row.status)" effect="plain">{{ planStatusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="应巡 / 已巡" width="120">
        <template #default="{ row }">{{ row.merchantTotal }} / {{ row.finishCount }}</template>
      </el-table-column>
      <el-table-column label="发现隐患" width="100">
        <template #default="{ row }">{{ row.dangerCount }}</template>
      </el-table-column>
      <el-table-column label="创建人" width="120">
        <template #default="{ row }">{{ row.createBy || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">查看</el-button>
          <el-button
            v-if="!auth.isPlatform"
            link
            type="primary"
            @click="openEdit(row)"
          >
            编辑
          </el-button>
          <el-button
            v-if="!auth.isPlatform && row.status === 0"
            link
            type="danger"
            @click="removePlan(row)"
          >
            删除
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
        @current-change="load"
        @size-change="handleSizeChange"
      />
    </div>

    <el-drawer
      v-model="formVisible"
      :title="form.id ? '编辑计划' : '新建计划'"
      size="480px"
      destroy-on-close
    >
      <el-form label-width="88px">
        <el-form-item label="计划名称" required>
          <el-input v-model="form.name" maxlength="64" placeholder="请输入计划名称" />
        </el-form-item>
        <el-form-item label="工作区" required>
          <el-select v-model="form.workAreaId" placeholder="请选择工作区" style="width: 100%">
            <el-option
              v-for="area in workAreas"
              :key="area.id"
              :label="area.name"
              :value="area.id"
            />
          </el-select>
          <div class="form-tip">计划只覆盖该工作区商户；全租户覆盖请按工作区分别建计划。</div>
        </el-form-item>
        <el-form-item label="开始日期" required>
          <el-date-picker
            v-model="form.startDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束日期" required>
          <el-date-picker
            v-model="form.endDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="formLoading" @click="submitForm">保存</el-button>
      </template>
    </el-drawer>

    <el-drawer v-model="detailVisible" title="计划详情" size="860px" destroy-on-close>
      <div v-loading="detailLoading" class="plan-detail">
        <template v-if="detail">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="计划名称" :span="2">{{ detail.name }}</el-descriptions-item>
            <el-descriptions-item label="工作区">{{ detail.workAreaName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ planStatusLabel(detail.status) }}</el-descriptions-item>
            <el-descriptions-item label="起止日期" :span="2">
              {{ detail.startDate }} 至 {{ detail.endDate }}
            </el-descriptions-item>
            <el-descriptions-item label="进度">
              {{ detail.finishCount }} / {{ detail.merchantTotal }}
            </el-descriptions-item>
            <el-descriptions-item label="发现隐患">{{ detail.dangerCount }}</el-descriptions-item>
            <el-descriptions-item label="说明" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
          </el-descriptions>

          <div class="detail-block">
            <h3>工作区商户</h3>
            <div class="filter-bar">
              <el-input
                v-model="merchantQuery.name"
                placeholder="商户名称"
                clearable
                style="width: 180px"
                @keyup.enter="searchMerchants"
              />
              <el-select
                v-model="merchantQuery.patrolStatus"
                placeholder="巡查状态"
                clearable
                style="width: 140px"
              >
                <el-option label="未巡查" :value="0" />
                <el-option label="正常" :value="1" />
                <el-option label="发现隐患" :value="2" />
              </el-select>
              <div class="filter-actions">
                <el-button type="primary" @click="searchMerchants">查询</el-button>
                <el-button @click="resetMerchantQuery">重置</el-button>
              </div>
            </div>
            <el-table v-loading="merchantLoading" :data="merchants" border size="small">
              <el-table-column prop="name" label="商户" min-width="150" />
              <el-table-column prop="addr" label="地址" min-width="160" />
              <el-table-column label="巡查状态" width="110">
                <template #default="{ row }">
                  <el-tag :type="merchantTone(row)" effect="plain">{{ merchantStatusText(row) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="巡查人" width="100">
                <template #default="{ row }">{{ row.lastPatrolBy || '-' }}</template>
              </el-table-column>
              <el-table-column label="巡查时间" width="165">
                <template #default="{ row }">{{ formatDateTime(row.lastPatrolTime) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="90" fixed="right">
                <template #default="{ row }">
                  <el-button v-if="row.finished" link type="primary" @click="openResult(row)">
                    结果
                  </el-button>
                  <span v-else class="muted">未巡查</span>
                </template>
              </el-table-column>
            </el-table>
            <div class="pager">
              <el-pagination
                v-model:current-page="merchantQuery.pageNum"
                v-model:page-size="merchantQuery.pageSize"
                small
                background
                layout="total, sizes, prev, pager, next"
                :total="merchantTotal"
                :page-sizes="[10, 20, 50, 100]"
                @current-change="loadMerchants"
                @size-change="handleMerchantSizeChange"
              />
            </div>
          </div>
        </template>
        <el-empty v-else-if="!detailLoading" description="计划不存在" />
      </div>
    </el-drawer>

    <el-drawer v-model="resultVisible" title="巡查结果" size="480px" destroy-on-close>
      <el-descriptions v-if="currentResult" :column="1" border>
        <el-descriptions-item label="商户">{{ currentResult.merchantName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="结果">
          {{ currentResult.result === 1 ? '发现隐患' : '正常' }}
        </el-descriptions-item>
        <el-descriptions-item label="说明">{{ currentResult.remark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="巡查人">{{ currentResult.patrolBy || '-' }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">
          {{ formatDateTime(currentResult.patrolTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="定位">
          {{ locationText(currentResult.lng, currentResult.lat) }}
        </el-descriptions-item>
        <el-descriptions-item label="关联隐患">
          {{ currentResult.dangerId || '-' }}
        </el-descriptions-item>
      </el-descriptions>
      <div v-if="resultPics.length > 0" class="detail-block">
        <h3>巡查照片</h3>
        <el-image
          v-for="pic in resultPics"
          :key="pic"
          :src="pic"
          :preview-src-list="resultPics"
          fit="cover"
          class="detail-pic"
          alt="巡查照片"
        />
      </div>
      <el-empty v-if="!currentResult" description="暂无巡查结果" />
    </el-drawer>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  createInspectionPlan,
  deleteInspectionPlan,
  getInspectionPlan,
  pageInspectionMerchants,
  pageInspectionPlans,
  pageInspectionResults,
  updateInspectionPlan,
  type InspectionMerchantQuery,
  type InspectionQuery,
  type InspectionSavePayload,
} from '@/api/inspection';
import { workAreaOptions } from '@/api/system';
import type {
  InspectionMerchantItem,
  InspectionPlanItem,
  InspectionResultItem,
  WorkAreaItem,
} from '@/types/api';
import { useAuthStore } from '@/stores/auth';
import { formatDateTime, planStatusLabel } from '@/utils/format';

const auth = useAuthStore();
const loading = ref(false);
const detailLoading = ref(false);
const formLoading = ref(false);
const rows = ref<InspectionPlanItem[]>([]);
const total = ref(0);
const workAreas = ref<WorkAreaItem[]>([]);
const coveragePlans = ref<InspectionPlanItem[]>([]);
const formVisible = ref(false);
const detailVisible = ref(false);
const resultVisible = ref(false);
const detail = ref<InspectionPlanItem | null>(null);
const merchants = ref<InspectionMerchantItem[]>([]);
const merchantTotal = ref(0);
const merchantLoading = ref(false);
const results = ref<InspectionResultItem[]>([]);
const currentResult = ref<InspectionResultItem | null>(null);

const query = reactive<InspectionQuery>({
  pageNum: 1,
  pageSize: 20,
});

const merchantQuery = reactive<InspectionMerchantQuery>({
  pageNum: 1,
  pageSize: 10,
});

const form = reactive<InspectionSavePayload>({
  workAreaId: '',
  name: '',
  startDate: '',
  endDate: '',
});

const resultPics = computed(() =>
  [currentResult.value?.pic1, currentResult.value?.pic2, currentResult.value?.pic3]
    .filter((pic): pic is string => Boolean(pic)));

const coverageText = computed(() => {
  const covered = new Set(
    coveragePlans.value
      .filter((plan) => plan.status === 1)
      .map((plan) => plan.workAreaId)
      .filter((id): id is string => Boolean(id)),
  );
  return `${covered.size} / ${workAreas.value.length}`;
});

async function load(): Promise<void> {
  loading.value = true;
  try {
    const page = await pageInspectionPlans(query);
    rows.value = page.records;
    total.value = page.total;
  } finally {
    loading.value = false;
  }
}

async function loadCoverage(): Promise<void> {
  const page = await pageInspectionPlans({ pageNum: 1, pageSize: 100 });
  coveragePlans.value = page.records;
}

function search(): void {
  query.pageNum = 1;
  load();
}

function handleSizeChange(size: number): void {
  query.pageSize = size;
  query.pageNum = 1;
  load();
}

function resetQuery(): void {
  query.name = '';
  query.workAreaId = undefined;
  search();
}

function planTone(status?: number | null): 'info' | 'primary' | 'success' {
  if (status === 0) {
    return 'info';
  }
  if (status === 1) {
    return 'primary';
  }
  return 'success';
}

function merchantStatusText(row: InspectionMerchantItem): string {
  if (!row.finished) {
    return '未巡查';
  }
  return row.lastResult === 1 ? '发现隐患' : '正常';
}

function merchantTone(row: InspectionMerchantItem): 'info' | 'danger' | 'success' {
  if (!row.finished) {
    return 'info';
  }
  return row.lastResult === 1 ? 'danger' : 'success';
}

function locationText(lng?: number | null, lat?: number | null): string {
  if (lng === null || lng === undefined || lat === null || lat === undefined) {
    return '-';
  }
  return `${lng}, ${lat}`;
}

function openCreate(): void {
  form.id = undefined;
  form.workAreaId = '';
  form.name = '';
  form.startDate = '';
  form.endDate = '';
  form.remark = '';
  formVisible.value = true;
}

function openEdit(row: InspectionPlanItem): void {
  form.id = row.id;
  form.workAreaId = row.workAreaId ?? '';
  form.name = row.name;
  form.startDate = row.startDate;
  form.endDate = row.endDate;
  form.remark = row.remark ?? '';
  formVisible.value = true;
}

async function submitForm(): Promise<void> {
  if (!form.name.trim()) {
    ElMessage.warning('请输入计划名称');
    return;
  }
  if (!form.workAreaId) {
    ElMessage.warning('请选择工作区');
    return;
  }
  if (!form.startDate || !form.endDate) {
    ElMessage.warning('请选择起止日期');
    return;
  }
  if (form.endDate < form.startDate) {
    ElMessage.warning('结束日期不能早于开始日期');
    return;
  }
  formLoading.value = true;
  try {
    if (form.id) {
      await updateInspectionPlan({ ...form });
      ElMessage.success('已保存');
    } else {
      await createInspectionPlan({ ...form });
      ElMessage.success('已创建');
    }
    formVisible.value = false;
    await load();
    await loadCoverage();
  } finally {
    formLoading.value = false;
  }
}

async function removePlan(row: InspectionPlanItem): Promise<void> {
  await ElMessageBox.confirm(`确认删除计划「${row.name}」？`, '删除确认', { type: 'warning' });
  await deleteInspectionPlan(row.id);
  ElMessage.success('已删除');
  await load();
  await loadCoverage();
}

async function openDetail(row: InspectionPlanItem): Promise<void> {
  detail.value = row;
  detailVisible.value = true;
  detailLoading.value = true;
  merchantQuery.name = '';
  merchantQuery.patrolStatus = undefined;
  merchantQuery.pageNum = 1;
  try {
    const [record, merchantPage, resultPage] = await Promise.all([
      getInspectionPlan(row.id),
      pageInspectionMerchants(row.id, merchantQuery),
      pageInspectionResults(row.id, 1, 100),
    ]);
    detail.value = record;
    merchants.value = merchantPage.records;
    merchantTotal.value = merchantPage.total;
    results.value = resultPage.records;
  } finally {
    detailLoading.value = false;
  }
}

async function loadMerchants(): Promise<void> {
  if (!detail.value) {
    return;
  }
  merchantLoading.value = true;
  try {
    const page = await pageInspectionMerchants(detail.value.id, merchantQuery);
    merchants.value = page.records;
    merchantTotal.value = page.total;
  } finally {
    merchantLoading.value = false;
  }
}

function searchMerchants(): void {
  merchantQuery.pageNum = 1;
  loadMerchants();
}

function handleMerchantSizeChange(size: number): void {
  merchantQuery.pageSize = size;
  merchantQuery.pageNum = 1;
  loadMerchants();
}

function resetMerchantQuery(): void {
  merchantQuery.name = '';
  merchantQuery.patrolStatus = undefined;
  searchMerchants();
}

function openResult(row: InspectionMerchantItem): void {
  currentResult.value = results.value.find((item) => item.merchantId === row.id)
    ?? {
      id: '',
      planId: detail.value?.id ?? '',
      merchantId: row.id,
      merchantName: row.name,
      patrolBy: row.lastPatrolBy,
      patrolTime: row.lastPatrolTime,
      result: row.lastResult ?? 0,
    };
  resultVisible.value = true;
}

onMounted(async () => {
  workAreas.value = await workAreaOptions();
  await Promise.all([load(), loadCoverage()]);
});
</script>

<style scoped>
.plan-detail {
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
</style>
