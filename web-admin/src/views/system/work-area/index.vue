<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1 class="page-title">工作区管理</h1>
        <p class="page-subtitle">工作区决定外勤可见的商户与派单选人范围。</p>
      </div>
      <el-button v-auth="'work-area:edit'" type="primary" @click="openCreate">新增工作区</el-button>
    </div>

    <div class="filter-bar">
      <el-input
        v-model="query.keyword"
        placeholder="工作区名称"
        clearable
        style="width: 220px"
        @keyup.enter="search"
      />
      <el-select v-if="isPlatform" v-model="query.tenantId" placeholder="运营中心" clearable style="width: 180px">
        <el-option v-for="tenant in tenants" :key="tenant.id" :label="tenant.name" :value="tenant.id" />
      </el-select>
      <div class="filter-actions">
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="rows" border>
      <el-table-column prop="name" label="工作区名称" width="200" show-overflow-tooltip />
      <el-table-column v-if="isPlatform" prop="tenantName" label="运营中心" min-width="120" />
      <el-table-column label="绑定用户" width="100">
        <template #default="{ row }">{{ row.userCount ?? 0 }}</template>
      </el-table-column>
      <el-table-column label="绑定商户" width="100">
        <template #default="{ row }">{{ row.merchantCount ?? 0 }}</template>
      </el-table-column>
      <el-table-column prop="remark" label="说明" min-width="240" show-overflow-tooltip>
        <template #default="{ row }">{{ row.remark || '-' }}</template>
      </el-table-column>
      <el-table-column label="创建时间" width="165">
        <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <el-button v-auth="'work-area:edit'" link type="primary" @click="openBind(row)">绑定商户/用户</el-button>
          <el-button v-auth="'work-area:edit'" link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button v-auth="'work-area:edit'" link type="danger" @click="remove(row)">删除</el-button>
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

    <el-drawer v-model="drawerVisible" :title="drawerTitle" size="480px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item v-if="isPlatform" label="运营中心" prop="tenantId">
          <el-select v-model="form.tenantId" placeholder="请选择" style="width: 100%">
            <el-option v-for="tenant in tenants" :key="tenant.id" :label="tenant.name" :value="tenant.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="如：昌都城区" maxlength="24" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="255" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="drawerVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </template>
    </el-drawer>

    <el-drawer v-model="bindVisible" title="绑定商户与用户" size="640px" destroy-on-close>
      <div class="bind-section">
        <h3>已绑定用户</h3>
        <el-transfer
          v-model="selectedUserIds"
          :data="userTransferData"
          filterable
          :titles="['可选用户', '已绑定']"
          :props="{ key: 'key', label: 'label' }"
        />
      </div>
      <div class="bind-section">
        <h3>已绑定商户</h3>
        <el-transfer
          v-model="selectedMerchantIds"
          :data="merchantTransferData"
          filterable
          :titles="['可选商户', '已绑定']"
          :props="{ key: 'key', label: 'label' }"
        />
      </div>
      <template #footer>
        <el-button @click="bindVisible = false">取消</el-button>
        <el-button type="primary" :loading="binding" @click="saveBind">保存绑定</el-button>
      </template>
    </el-drawer>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import {
  bindWorkAreaMerchants,
  bindWorkAreaUsers,
  createWorkArea,
  deleteWorkArea,
  getWorkArea,
  listTenants,
  merchantOptions,
  pageWorkAreas,
  updateWorkArea,
  userOptions,
  type WorkAreaQuery,
  type WorkAreaSavePayload,
} from '@/api/system';
import { useAuthStore } from '@/stores/auth';
import type { MerchantOption, OrgItem, UserItem, WorkAreaItem } from '@/types/api';
import { formatDateTime } from '@/utils/format';

interface WorkAreaForm {
  tenantId: string;
  name: string;
  remark: string;
}

interface TransferItem {
  key: string;
  label: string;
}

const auth = useAuthStore();
const isPlatform = computed(() => auth.user?.userType === 'PLATFORM');

const loading = ref(false);
const submitting = ref(false);
const binding = ref(false);
const rows = ref<WorkAreaItem[]>([]);
const total = ref(0);
const tenants = ref<OrgItem[]>([]);

const query = reactive<WorkAreaQuery>({
  pageNum: 1,
  pageSize: 20,
  keyword: '',
  tenantId: '',
});

const drawerVisible = ref(false);
const drawerTitle = ref('新增工作区');
const editingId = ref('');
const formRef = ref<FormInstance>();
const form = reactive<WorkAreaForm>({ tenantId: '', name: '', remark: '' });
const rules: FormRules<WorkAreaForm> = {
  tenantId: [{ required: true, message: '请选择运营中心', trigger: 'change' }],
  name: [{ required: true, message: '请输入工作区名称', trigger: 'blur' }],
};

const bindVisible = ref(false);
const currentArea = ref<WorkAreaItem | null>(null);
const selectedUserIds = ref<string[]>([]);
const selectedMerchantIds = ref<string[]>([]);
const userTransferData = ref<TransferItem[]>([]);
const merchantTransferData = ref<TransferItem[]>([]);

async function load(): Promise<void> {
  loading.value = true;
  try {
    const page = await pageWorkAreas({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      keyword: query.keyword || undefined,
      tenantId: query.tenantId || undefined,
    });
    rows.value = page.records;
    total.value = page.total;
  } finally {
    loading.value = false;
  }
}

async function loadBaseOptions(): Promise<void> {
  if (isPlatform.value) {
    tenants.value = await listTenants();
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
  query.tenantId = '';
  search();
}

function openCreate(): void {
  Object.assign(form, {
    tenantId: isPlatform.value ? (query.tenantId || tenants.value[0]?.id || '') : (auth.user?.tenantId || ''),
    name: '',
    remark: '',
  });
  editingId.value = '';
  drawerTitle.value = '新增工作区';
  drawerVisible.value = true;
}

function openEdit(row: WorkAreaItem): void {
  Object.assign(form, {
    tenantId: row.tenantId,
    name: row.name,
    remark: row.remark ?? '',
  });
  editingId.value = row.id;
  drawerTitle.value = '编辑工作区';
  drawerVisible.value = true;
}

async function submit(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }
  submitting.value = true;
  try {
    const payload: WorkAreaSavePayload = {
      tenantId: isPlatform.value ? form.tenantId : undefined,
      name: form.name.trim(),
      remark: form.remark || undefined,
    };
    if (editingId.value) {
      await updateWorkArea(editingId.value, payload);
    } else {
      await createWorkArea(payload);
    }
    ElMessage.success('保存成功');
    drawerVisible.value = false;
    await load();
  } finally {
    submitting.value = false;
  }
}

async function remove(row: WorkAreaItem): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认删除工作区「${row.name}」？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    });
  } catch {
    return;
  }
  await deleteWorkArea(row.id);
  ElMessage.success('已删除');
  await load();
}

async function openBind(row: WorkAreaItem): Promise<void> {
  const [detail, users, merchants] = await Promise.all([
    getWorkArea(row.id),
    userOptions(row.tenantId),
    merchantOptions(row.tenantId),
  ]);
  currentArea.value = detail;
  selectedUserIds.value = detail.userIds ?? [];
  selectedMerchantIds.value = detail.merchantIds ?? [];
  userTransferData.value = users.map((item: UserItem) => ({
    key: item.id,
    label: `${item.realName}（${item.phone}）`,
  }));
  merchantTransferData.value = merchants.map((item: MerchantOption) => ({
    key: item.id,
    label: item.name,
  }));
  bindVisible.value = true;
}

async function saveBind(): Promise<void> {
  if (!currentArea.value) {
    return;
  }
  binding.value = true;
  try {
    await Promise.all([
      bindWorkAreaUsers(currentArea.value.id, selectedUserIds.value),
      bindWorkAreaMerchants(currentArea.value.id, selectedMerchantIds.value),
    ]);
    ElMessage.success('绑定已保存');
    bindVisible.value = false;
    await load();
  } finally {
    binding.value = false;
  }
}

onMounted(async () => {
  await loadBaseOptions();
  await load();
});
</script>

<style scoped>
.bind-section {
  margin-bottom: 24px;
}

.bind-section h3 {
  margin-bottom: 12px;
  color: var(--fm-text);
  font-size: 14px;
  font-weight: 600;
}

.bind-section :deep(.el-transfer) {
  display: flex;
  align-items: center;
  justify-content: center;
}

.bind-section :deep(.el-transfer-panel) {
  width: 240px;
}
</style>
