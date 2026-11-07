<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1 class="page-title">运营中心管理</h1>
        <p class="page-subtitle">仅平台管理员可维护；新建时自动复制一份本租户菜单基线。</p>
      </div>
      <el-button type="primary" @click="openCreate">新增运营中心</el-button>
    </div>

    <div class="filter-bar">
      <el-input
        v-model="query.keyword"
        placeholder="名称 / 编码"
        clearable
        style="width: 220px"
        @keyup.enter="search"
      />
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px">
        <el-option label="启用" :value="1" />
        <el-option label="禁用" :value="0" />
      </el-select>
      <div class="filter-actions">
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="rows" border>
      <el-table-column prop="name" label="运营中心名称" min-width="150" />
      <el-table-column prop="code" label="编码" min-width="100">
        <template #default="{ row }">{{ row.code || '-' }}</template>
      </el-table-column>
      <el-table-column prop="adcode" label="行政区划" min-width="110">
        <template #default="{ row }">{{ row.adcode || '-' }}</template>
      </el-table-column>
      <el-table-column prop="address" label="地址" min-width="200">
        <template #default="{ row }">{{ row.address || '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <FmStatusTag :status="row.status" />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="165">
        <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="更新时间" width="165">
        <template #default="{ row }">{{ formatDateTime(row.updateTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
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
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="如：昌都市" maxlength="64" />
        </el-form-item>
        <el-form-item label="编码">
          <el-input v-model="form.code" placeholder="可选，如 CD" maxlength="32" />
        </el-form-item>
        <el-form-item label="行政区划">
          <el-input v-model="form.adcode" placeholder="可选，如 540300" maxlength="16" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="form.address" type="textarea" :rows="2" maxlength="255" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="新建后会复制平台菜单基线；复制后与基线脱钩，后续基线变更不影响该租户。"
        />
      </el-form>
      <template #footer>
        <el-button @click="drawerVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </template>
    </el-drawer>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import FmStatusTag from '@/components/FmStatusTag.vue';
import {
  createTenant,
  deleteTenant,
  pageTenants,
  updateTenant,
  type OrgQuery,
  type OrgSavePayload,
} from '@/api/system';
import type { OrgItem } from '@/types/api';
import { formatDateTime } from '@/utils/format';

interface TenantForm {
  name: string;
  code: string;
  adcode: string;
  address: string;
  status: number;
}

const loading = ref(false);
const submitting = ref(false);
const rows = ref<OrgItem[]>([]);
const total = ref(0);
const drawerVisible = ref(false);
const drawerTitle = ref('新增运营中心');
const editingId = ref('');
const formRef = ref<FormInstance>();
const form = reactive<TenantForm>({ name: '', code: '', adcode: '', address: '', status: 1 });
const query = reactive<OrgQuery>({ pageNum: 1, pageSize: 20, keyword: '', status: undefined });

const rules: FormRules<TenantForm> = {
  name: [{ required: true, message: '请输入运营中心名称', trigger: 'blur' }],
};

async function load(): Promise<void> {
  loading.value = true;
  try {
    const page = await pageTenants({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      keyword: query.keyword || undefined,
      status: query.status,
    });
    rows.value = page.records;
    total.value = page.total;
  } catch (error) {
    // 失败已由请求拦截器提示，此处仅记录，避免 onMounted/事件处理器产生未处理的 rejection
    console.error('[system/tenant] 加载失败', error);
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
  query.status = undefined;
  search();
}

function openCreate(): void {
  Object.assign(form, { name: '', code: '', adcode: '', address: '', status: 1 });
  editingId.value = '';
  drawerTitle.value = '新增运营中心';
  drawerVisible.value = true;
}

function openEdit(row: OrgItem): void {
  Object.assign(form, {
    name: row.name,
    code: row.code ?? '',
    adcode: row.adcode ?? '',
    address: row.address ?? '',
    status: row.status,
  });
  editingId.value = row.id;
  drawerTitle.value = '编辑运营中心';
  drawerVisible.value = true;
}

async function submit(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }
  submitting.value = true;
  try {
    const payload: OrgSavePayload = {
      name: form.name.trim(),
      code: form.code || undefined,
      adcode: form.adcode || undefined,
      address: form.address || undefined,
      status: form.status,
    };
    if (editingId.value) {
      await updateTenant(editingId.value, payload);
      ElMessage.success('保存成功');
    } else {
      await createTenant(payload);
      ElMessage.success('运营中心已创建，菜单基线已复制');
    }
    drawerVisible.value = false;
    await load();
  } finally {
    submitting.value = false;
  }
}

async function remove(row: OrgItem): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认删除运营中心「${row.name}」？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    });
  } catch {
    return;
  }
  await deleteTenant(row.id);
  ElMessage.success('已删除');
  await load();
}

onMounted(load);
</script>

<style scoped>
:deep(.el-alert) {
  margin-top: 8px;
}
</style>
