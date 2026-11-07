<template>
  <MerchantDetailView
    v-if="mode === 'detail'"
    :detail="detail"
    :loading="detailLoading"
    :can-edit="canEdit"
    :can-bind="canBind"
    :initial-tab="detailTab"
    @back="backToList"
    @refresh="loadDetail"
    @edit="openEdit(detail ?? undefined)"
    @open-plan="openPlan"
  />

  <section v-else class="page-card">
    <div class="page-header">
      <div>
        <h1 class="page-title">商户管理</h1>
        <p class="page-subtitle">商户档案、楼宇楼层与平面图布点；设备与工作区绑定在详情内维护。</p>
      </div>
      <el-button v-if="canEdit" type="primary" @click="openCreate">新增商户</el-button>
    </div>

    <div class="filter-bar">
      <el-input
        v-model="query.keyword"
        placeholder="商户名称 / 业主 / 电话"
        clearable
        style="width: 220px"
        @keyup.enter="search"
      />
      <el-select v-model="query.type" placeholder="商户类型" clearable style="width: 150px">
        <el-option
          v-for="item in activeTypes"
          :key="item.code"
          :label="item.name"
          :value="item.code"
        />
      </el-select>
      <el-select v-model="query.merchantKind" placeholder="场所性质" clearable style="width: 130px">
        <el-option label="九小场所" :value="1" />
        <el-option label="社会单位" :value="2" />
      </el-select>
      <el-select
        v-if="showWorkAreaFilter"
        v-model="query.workAreaId"
        placeholder="工作区"
        clearable
        style="width: 150px"
      >
        <el-option
          v-for="area in workAreas"
          :key="area.id"
          :label="area.name"
          :value="area.id"
        />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 110px">
        <el-option label="启用" :value="1" />
        <el-option label="禁用" :value="0" />
      </el-select>
      <div class="filter-actions">
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </div>
    </div>

    <el-table
      v-loading="loading"
      :data="rows"
      border
      row-key="id"
      empty-text="暂无商户"
    >
      <el-table-column label="商户名称" min-width="190">
        <template #default="{ row }">
          <div class="merchant-name">
            <span>{{ row.name }}</span>
            <el-tag size="small" effect="plain">{{ row.typeName || '未分类' }}</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="登录账号" width="120">
        <template #default="{ row }">{{ row.loginAccount || '-' }}</template>
      </el-table-column>
      <el-table-column label="已绑定工作区" min-width="140">
        <template #default="{ row }">{{ row.workAreaNames?.join('、') || '-' }}</template>
      </el-table-column>
      <el-table-column prop="addr" label="地址" min-width="190" show-overflow-tooltip />
      <el-table-column label="设备数" width="90">
        <template #default="{ row }">{{ row.deviceCount ?? 0 }}</template>
      </el-table-column>
      <el-table-column label="面积 / 员工" width="130">
        <template #default="{ row }">
          {{ row.floorage || '-' }} / {{ row.employeeNum ?? '-' }}
        </template>
      </el-table-column>
      <el-table-column label="场所性质" width="110">
        <template #default="{ row }">
          {{ row.merchantKind === 2 ? '社会单位' : '九小场所' }}
        </template>
      </el-table-column>
      <el-table-column label="业主 / 电话" min-width="150">
        <template #default="{ row }">
          {{ row.ownerName || '-' }} / {{ row.phone || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="plain">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="190" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">查看</el-button>
          <el-button v-if="canEdit" link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-dropdown
            v-if="canEdit || canBind"
            class="more-actions"
            trigger="click"
            @command="handleCommand"
          >
            <el-button link type="primary">更多</el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item
                  :command="{ action: 'workArea', row }"
                  :disabled="!canBind"
                >
                  绑定工作区
                </el-dropdown-item>
                <el-dropdown-item
                  :command="{ action: 'device', row }"
                  :disabled="!canBind"
                >
                  绑定设备
                </el-dropdown-item>
                <el-dropdown-item
                  :command="{ action: 'toggle', row }"
                  :disabled="!canEdit"
                >
                  {{ row.status === 1 ? '禁用商户' : '启用商户' }}
                </el-dropdown-item>
                <el-dropdown-item
                  :command="{ action: 'delete', row }"
                  :disabled="!canEdit"
                  divided
                >
                  删除商户
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
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
  </section>

  <MerchantForm
    v-model:visible="formVisible"
    :merchant="editingMerchant"
    :merchant-types="types"
    @saved="handleSaved"
  />

  <FloorPlanEditor
    v-model:visible="planVisible"
    :buildings="detail?.buildings ?? []"
    :initial-floor-id="planFloorId"
    :readonly="!canEdit"
    @saved="loadDetail"
  />
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  deleteMerchant,
  getMerchant,
  pageMerchants,
  updateMerchantStatus,
  type MerchantQuery,
} from '@/api/merchant';
import { merchantTypes } from '@/api/device';
import { workAreaOptions } from '@/api/system';
import type { DictItem, MerchantDetail, MerchantItem, WorkAreaItem } from '@/types/api';
import { useAuthStore } from '@/stores/auth';
import MerchantForm from './MerchantForm.vue';
import MerchantDetailView from './MerchantDetail.vue';
import FloorPlanEditor from './FloorPlanEditor.vue';

const auth = useAuthStore();
const mode = ref<'list' | 'detail'>('list');
const detailTab = ref('basic');
const detailId = ref('');
const detail = ref<MerchantDetail | null>(null);
const detailLoading = ref(false);
const loading = ref(false);
const rows = ref<MerchantItem[]>([]);
const total = ref(0);
const types = ref<DictItem[]>([]);
const workAreas = ref<WorkAreaItem[]>([]);
const formVisible = ref(false);
const editingMerchant = ref<MerchantItem | null>(null);
const planVisible = ref(false);
const planFloorId = ref('');

const query = reactive<MerchantQuery>({
  pageNum: 1,
  pageSize: 20,
});

const canEdit = computed(() => !auth.isPlatform && auth.hasPerm('merchant:edit'));
const canBind = computed(() => !auth.isPlatform && auth.hasPerm('merchant:bind'));
const isTenantAdmin = computed(() => auth.isPlatform || auth.roles.includes('TENANT_ADMIN'));
const showWorkAreaFilter = computed(() => !auth.isPlatform && isTenantAdmin.value);
const activeTypes = computed(() => types.value.filter((item) => item.status === 1));

async function load(): Promise<void> {
  loading.value = true;
  try {
    const page = await pageMerchants(query);
    rows.value = page.records;
    total.value = page.total;
  } finally {
    loading.value = false;
  }
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
  query.keyword = '';
  query.type = undefined;
  query.merchantKind = undefined;
  query.workAreaId = undefined;
  query.status = undefined;
  search();
}

function openCreate(): void {
  editingMerchant.value = null;
  formVisible.value = true;
}

function openEdit(merchant?: MerchantItem): void {
  if (!merchant) {
    return;
  }
  editingMerchant.value = merchant;
  formVisible.value = true;
}

async function handleSaved(): Promise<void> {
  if (mode.value === 'detail' && detailId.value) {
    await loadDetail();
    return;
  }
  await load();
}

async function openDetail(row: MerchantItem, tab = 'basic'): Promise<void> {
  mode.value = 'detail';
  detailTab.value = tab;
  detailId.value = row.id;
  await loadDetail();
}

async function loadDetail(): Promise<void> {
  if (!detailId.value) {
    return;
  }
  detailLoading.value = true;
  try {
    detail.value = await getMerchant(detailId.value);
  } finally {
    detailLoading.value = false;
  }
}

function backToList(): void {
  mode.value = 'list';
  detail.value = null;
  detailId.value = '';
  load();
}

function openPlan(floorId: string): void {
  planFloorId.value = floorId;
  planVisible.value = true;
}

interface MerchantCommand {
  action: string;
  row: MerchantItem;
}

async function handleCommand(payload: MerchantCommand): Promise<void> {
  const { action, row } = payload;
  if (action === 'workArea') {
    await openDetail(row, 'workArea');
    return;
  }
  if (action === 'device') {
    await openDetail(row, 'device');
    return;
  }
  if (action === 'toggle') {
    await toggleStatus(row);
    return;
  }
  if (action === 'delete') {
    await removeMerchant(row);
  }
}

async function toggleStatus(row: MerchantItem): Promise<void> {
  const next = row.status === 1 ? 0 : 1;
  const confirmed = await confirmAction(`确认${next === 1 ? '启用' : '禁用'}商户「${row.name}」？`);
  if (!confirmed) {
    return;
  }
  await updateMerchantStatus(row.id, next);
  ElMessage.success(next === 1 ? '已启用' : '已禁用');
  if (mode.value === 'detail') {
    await loadDetail();
  } else {
    await load();
  }
}

async function removeMerchant(row: MerchantItem): Promise<void> {
  const confirmed = await confirmAction(`确认删除商户「${row.name}」？删除后不可恢复。`);
  if (!confirmed) {
    return;
  }
  await deleteMerchant(row.id);
  ElMessage.success('已删除');
  if (mode.value === 'detail' && detailId.value === row.id) {
    backToList();
    return;
  }
  await load();
}

async function confirmAction(message: string): Promise<boolean> {
  try {
    await ElMessageBox.confirm(message, '操作确认', { type: 'warning' });
    return true;
  } catch {
    return false;
  }
}

onMounted(async () => {
  types.value = await merchantTypes();
  if (showWorkAreaFilter.value) {
    try {
      workAreas.value = await workAreaOptions();
    } catch {
      workAreas.value = [];
    }
  }
  await load();
});
</script>

<style scoped>
.merchant-name {
  display: flex;
  align-items: center;
  gap: var(--fm-space-2);
}

/* el-dropdown 默认 vertical-align:top，且自身不是 .el-button，会打断相邻按钮的 12px 间距，
   这里补齐间距与垂直对齐，让“更多”与“查看/编辑”保持一致 */
.more-actions {
  margin-left: var(--fm-space-3);
  vertical-align: middle;
}
</style>
