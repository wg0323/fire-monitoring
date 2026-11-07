<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1 class="page-title">用户管理</h1>
        <p class="page-subtitle">平台可管理全部租户账号；租户管理员只维护本租户账号。</p>
      </div>
      <el-button v-auth="'user:edit'" type="primary" @click="openCreate">新增用户</el-button>
    </div>

    <div class="filter-bar">
      <el-input
        v-model="query.keyword"
        placeholder="姓名 / 登录名 / 手机号"
        clearable
        style="width: 220px"
        @keyup.enter="search"
      />
      <el-select v-model="query.userType" placeholder="用户类型" clearable style="width: 130px">
        <el-option label="平台" value="PLATFORM" />
        <el-option label="租户" value="TENANT" />
        <el-option label="商户" value="MERCHANT" />
      </el-select>
      <el-select v-if="isPlatform" v-model="query.tenantId" placeholder="运营中心" clearable style="width: 170px">
        <el-option v-for="tenant in tenants" :key="tenant.id" :label="tenant.name" :value="tenant.id" />
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

    <el-table v-loading="loading" :data="rows" border>
      <el-table-column prop="realName" label="真实姓名" min-width="110" />
      <el-table-column prop="userName" label="登录名" min-width="110" />
      <el-table-column prop="phone" label="手机号" min-width="125" />
      <el-table-column label="类型" width="80">
        <template #default="{ row }">{{ userTypeLabel(row.userType) }}</template>
      </el-table-column>
      <el-table-column v-if="isPlatform" prop="tenantName" label="运营中心" min-width="105" />
      <el-table-column label="角色" min-width="110">
        <template #default="{ row }">{{ row.roleName || roleLabel(row.roleKey) }}</template>
      </el-table-column>
      <el-table-column prop="deptName" label="部门" min-width="100">
        <template #default="{ row }">{{ row.deptName || '-' }}</template>
      </el-table-column>
      <el-table-column label="工作区" min-width="150">
        <template #default="{ row }">
          {{ row.workAreaNames?.length ? row.workAreaNames.join('、') : '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="merchantName" label="商户" min-width="120">
        <template #default="{ row }">{{ row.merchantName || '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <FmStatusTag :status="row.status" />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="165">
        <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="190" fixed="right">
        <template #default="{ row }">
          <el-button v-auth="'user:edit'" link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button v-auth="'user:edit'" link type="primary" @click="resetPassword(row)">重置密码</el-button>
          <el-button v-auth="'user:edit'" link type="danger" @click="remove(row)">删除</el-button>
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

    <el-drawer v-model="drawerVisible" :title="drawerTitle" size="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item v-if="isPlatform && form.userType !== 'PLATFORM'" label="运营中心" prop="tenantId">
          <el-select v-model="form.tenantId" placeholder="请选择" :disabled="Boolean(editingId)" style="width: 100%" @change="handleTenantChange">
            <el-option v-for="tenant in tenants" :key="tenant.id" :label="tenant.name" :value="tenant.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="用户类型" prop="userType">
          <el-select v-model="form.userType" :disabled="Boolean(editingId)" style="width: 100%" @change="handleUserTypeChange">
            <el-option v-for="item in userTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色" prop="roleId">
          <el-select v-model="form.roleId" placeholder="请选择角色" style="width: 100%">
            <el-option v-for="role in roles" :key="role.id" :label="role.roleName" :value="role.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.userType === 'TENANT'" label="部门">
          <el-tree-select
            v-model="form.deptId"
            :data="depts"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            check-strictly
            clearable
            placeholder="可选"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="登录名" prop="userName">
          <el-input v-model="form.userName" :disabled="Boolean(editingId)" placeholder="请输入登录名" />
        </el-form-item>
        <el-form-item v-if="!editingId" label="初始密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="留空默认 123456" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" maxlength="11" placeholder="小程序白名单键，全局唯一" />
        </el-form-item>
        <el-form-item v-if="form.userType === 'MERCHANT'" label="所属商户" prop="merchantId">
          <el-select v-model="form.merchantId" filterable placeholder="请选择商户" style="width: 100%">
            <el-option v-for="merchant in merchants" :key="merchant.id" :label="merchant.name" :value="merchant.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.userType === 'TENANT'" label="工作区">
          <el-select v-model="form.workAreaIds" multiple placeholder="外勤至少绑定一个" style="width: 100%">
            <el-option v-for="area in workAreas" :key="area.id" :label="area.name" :value="area.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="drawerVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </template>
    </el-drawer>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import FmStatusTag from '@/components/FmStatusTag.vue';
import {
  createUser,
  deleteUser,
  deptTree,
  listRoles,
  listTenants,
  merchantOptions,
  pageUsers,
  resetUserPassword,
  updateUser,
  workAreaOptions,
  type UserQuery,
  type UserSavePayload,
} from '@/api/system';
import { useAuthStore } from '@/stores/auth';
import type { DeptNode, MerchantOption, OrgItem, RoleItem, UserItem, WorkAreaItem } from '@/types/api';
import { formatDateTime, roleLabel, userTypeLabel } from '@/utils/format';

interface UserForm {
  tenantId: string;
  deptId: string | null;
  userType: string;
  userName: string;
  password: string;
  realName: string;
  phone: string;
  merchantId: string | null;
  roleId: string;
  status: number;
  workAreaIds: string[];
}

const auth = useAuthStore();
const isPlatform = computed(() => auth.user?.userType === 'PLATFORM');

const loading = ref(false);
const submitting = ref(false);
const rows = ref<UserItem[]>([]);
const total = ref(0);
const tenants = ref<OrgItem[]>([]);
const roles = ref<RoleItem[]>([]);
const depts = ref<DeptNode[]>([]);
const workAreas = ref<WorkAreaItem[]>([]);
const merchants = ref<MerchantOption[]>([]);

const query = reactive<UserQuery>({
  pageNum: 1,
  pageSize: 20,
  keyword: '',
  userType: '',
  tenantId: '',
  status: undefined,
});

const drawerVisible = ref(false);
const drawerTitle = ref('新增用户');
const editingId = ref('');
const formRef = ref<FormInstance>();
const form = reactive<UserForm>(emptyForm());

const rules: FormRules<UserForm> = {
  userType: [{ required: true, message: '请选择用户类型', trigger: 'change' }],
  roleId: [{ required: true, message: '请选择角色', trigger: 'change' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  userName: [{ required: true, message: '请输入登录名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
};

const userTypeOptions = computed(() => {
  if (isPlatform.value) {
    return [
      { value: 'PLATFORM', label: '平台' },
      { value: 'TENANT', label: '租户' },
      { value: 'MERCHANT', label: '商户' },
    ];
  }
  return [
    { value: 'TENANT', label: '租户' },
    { value: 'MERCHANT', label: '商户' },
  ];
});

function emptyForm(): UserForm {
  return {
    tenantId: '',
    deptId: null,
    userType: 'TENANT',
    userName: '',
    password: '',
    realName: '',
    phone: '',
    merchantId: null,
    roleId: '',
    status: 1,
    workAreaIds: [],
  };
}

async function load(): Promise<void> {
  loading.value = true;
  try {
    const page = await pageUsers({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      keyword: query.keyword || undefined,
      userType: query.userType || undefined,
      tenantId: query.tenantId || undefined,
      status: query.status,
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

async function loadFormOptions(): Promise<void> {
  if (form.userType === 'PLATFORM') {
    depts.value = [];
    workAreas.value = [];
    merchants.value = [];
    roles.value = await listRoles('0', 'PLATFORM');
    if (form.roleId && !roles.value.some((role) => role.id === form.roleId)) {
      form.roleId = '';
    }
    return;
  }
  const tenantId = form.tenantId && form.tenantId !== '0' ? form.tenantId : '';
  if (!tenantId) {
    depts.value = [];
    workAreas.value = [];
    merchants.value = [];
    roles.value = [];
    form.roleId = '';
    return;
  }
  const [deptData, areaData, merchantData, roleData] = await Promise.all([
    deptTree(tenantId),
    workAreaOptions(tenantId),
    merchantOptions(tenantId),
    listRoles(tenantId, form.userType),
  ]);
  depts.value = deptData;
  workAreas.value = areaData;
  merchants.value = merchantData;
  roles.value = roleData;
  if (form.roleId && !roles.value.some((role) => role.id === form.roleId)) {
    form.roleId = '';
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
  query.userType = '';
  query.tenantId = '';
  query.status = undefined;
  search();
}

async function openCreate(): Promise<void> {
  Object.assign(form, emptyForm());
  form.userType = 'TENANT';
  if (isPlatform.value) {
    form.tenantId = query.tenantId || tenants.value[0]?.id || '';
  } else {
    form.tenantId = auth.user?.tenantId || '';
  }
  editingId.value = '';
  drawerTitle.value = '新增用户';
  drawerVisible.value = true;
  await loadFormOptions();
}

async function openEdit(row: UserItem): Promise<void> {
  Object.assign(form, {
    tenantId: row.tenantId,
    deptId: row.deptId || null,
    userType: row.userType,
    userName: row.userName,
    password: '',
    realName: row.realName,
    phone: row.phone,
    merchantId: row.merchantId || null,
    roleId: row.roleId || '',
    status: row.status,
    workAreaIds: row.workAreaIds ?? [],
  });
  editingId.value = row.id;
  drawerTitle.value = '编辑用户';
  drawerVisible.value = true;
  await loadFormOptions();
}

async function handleTenantChange(): Promise<void> {
  form.roleId = '';
  form.deptId = null;
  form.merchantId = null;
  form.workAreaIds = [];
  await loadFormOptions();
}

async function handleUserTypeChange(): Promise<void> {
  form.roleId = '';
  form.merchantId = null;
  form.deptId = null;
  form.workAreaIds = [];
  if (form.userType === 'PLATFORM') {
    form.tenantId = '0';
  } else if (!form.tenantId || form.tenantId === '0') {
    form.tenantId = isPlatform.value
      ? (query.tenantId || tenants.value[0]?.id || '')
      : (auth.user?.tenantId || '');
  }
  await loadFormOptions();
}

async function submit(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }
  if (form.userType === 'MERCHANT' && !form.merchantId) {
    ElMessage.warning('请选择商户');
    return;
  }
  submitting.value = true;
  try {
    const payload: UserSavePayload = {
      tenantId: form.tenantId || undefined,
      deptId: form.deptId || null,
      userType: form.userType,
      userName: form.userName.trim(),
      password: form.password || undefined,
      realName: form.realName.trim(),
      phone: form.phone.trim(),
      merchantId: form.userType === 'MERCHANT' ? form.merchantId : null,
      roleId: form.roleId,
      status: form.status,
      workAreaIds: form.userType === 'TENANT' ? form.workAreaIds : [],
    };
    if (editingId.value) {
      await updateUser(editingId.value, payload);
    } else {
      await createUser(payload);
    }
    ElMessage.success('保存成功');
    drawerVisible.value = false;
    await load();
  } finally {
    submitting.value = false;
  }
}

async function remove(row: UserItem): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认删除用户「${row.realName}」？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    });
  } catch {
    return;
  }
  await deleteUser(row.id);
  ElMessage.success('已删除');
  await load();
}

async function resetPassword(row: UserItem): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认将「${row.realName}」的密码重置为 123456？`, '重置密码', {
      type: 'warning',
      confirmButtonText: '重置',
      cancelButtonText: '取消',
    });
  } catch {
    return;
  }
  await resetUserPassword(row.id);
  ElMessage.success('密码已重置');
}

onMounted(async () => {
  await loadBaseOptions();
  await load();
});
</script>

<style scoped>
.filter-bar :deep(.el-input__wrapper),
.filter-bar :deep(.el-select__wrapper) {
  min-height: 32px;
}

:deep(.el-table .el-button + .el-button) {
  margin-left: 8px;
}
</style>
