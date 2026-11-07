<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1 class="page-title">角色管理</h1>
        <p class="page-subtitle">五类角色固定：平台管理员、租户管理员、租户值守、租户外勤、商户。</p>
      </div>
      <el-button v-auth="'role:edit'" type="primary" @click="openCreate">新增角色</el-button>
    </div>

    <div class="filter-bar">
      <el-input
        v-model="query.keyword"
        placeholder="角色名称"
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
      <el-table-column prop="roleName" label="角色名称" min-width="140" />
      <el-table-column label="角色标识" min-width="130">
        <template #default="{ row }">{{ roleLabel(row.roleKey) }}</template>
      </el-table-column>
      <el-table-column prop="tenantName" label="运营中心" min-width="120" />
      <el-table-column label="数据范围" min-width="120">
        <template #default="{ row }">{{ dataScopeLabel(row.dataScope) }}</template>
      </el-table-column>
      <el-table-column label="用户数" width="90">
        <template #default="{ row }">{{ row.userCount ?? 0 }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <FmStatusTag :status="row.status" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button v-auth="'role:edit'" link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button v-auth="'role:grant'" link type="primary" @click="openGrant(row)">分配权限</el-button>
          <el-button v-auth="'role:edit'" link type="danger" @click="remove(row)">删除</el-button>
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
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="请输入角色名称" maxlength="64" />
        </el-form-item>
        <el-form-item label="角色标识" prop="roleKey">
          <el-select v-model="form.roleKey" :disabled="Boolean(editingId)" style="width: 100%">
            <el-option v-for="item in roleKeyOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="数据范围">
          <el-input :model-value="dataScopeLabel(scopeOf(form.roleKey))" disabled />
          <p class="form-tip">数据范围由角色标识自动推导，不可手动修改。</p>
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

    <el-drawer v-model="grantVisible" :title="`分配权限 · ${grantRoleName}`" size="520px" destroy-on-close>
      <p class="grant-tip">勾选菜单与按钮；父级半选状态会自动保存。</p>
      <el-tree
        ref="treeRef"
        :data="menuTreeData"
        show-checkbox
        node-key="id"
        default-expand-all
        :props="{ label: 'name', children: 'children' }"
      >
        <template #default="{ data }">
          <span class="tree-node">
            <span>{{ data.name }}</span>
            <span class="tree-node__type">{{ menuTypeLabel(data.type) }}</span>
          </span>
        </template>
      </el-tree>
      <template #footer>
        <el-button @click="grantVisible = false">取消</el-button>
        <el-button type="primary" :loading="granting" @click="saveGrant">保存权限</el-button>
      </template>
    </el-drawer>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type TreeInstance } from 'element-plus';
import FmStatusTag from '@/components/FmStatusTag.vue';
import {
  assignRoleMenus,
  createRole,
  deleteRole,
  menuTree,
  pageRoles,
  roleMenuIds,
  updateRole,
  type RoleQuery,
  type RoleSavePayload,
} from '@/api/system';
import { useAuthStore } from '@/stores/auth';
import type { MenuNode, RoleItem } from '@/types/api';
import { dataScopeLabel, menuTypeLabel, roleLabel } from '@/utils/format';

interface RoleForm {
  roleName: string;
  roleKey: string;
  status: number;
}

const auth = useAuthStore();
const isPlatform = computed(() => auth.user?.userType === 'PLATFORM');

const loading = ref(false);
const submitting = ref(false);
const granting = ref(false);
const rows = ref<RoleItem[]>([]);
const total = ref(0);
const query = reactive<RoleQuery>({ pageNum: 1, pageSize: 20, keyword: '', status: undefined });

const drawerVisible = ref(false);
const drawerTitle = ref('新增角色');
const editingId = ref('');
const formRef = ref<FormInstance>();
const form = reactive<RoleForm>({ roleName: '', roleKey: '', status: 1 });

const grantVisible = ref(false);
const grantRoleName = ref('');
const grantRoleId = ref('');
const treeRef = ref<TreeInstance>();
const menuTreeData = ref<MenuNode[]>([]);
const checkedMenuIds = ref<string[]>([]);

const roleKeyOptions = computed(() => {
  if (isPlatform.value) {
    return [{ value: 'PLATFORM_ADMIN', label: '平台管理员' }];
  }
  return [
    { value: 'TENANT_ADMIN', label: '租户管理员' },
    { value: 'TENANT_DUTY', label: '租户值守' },
    { value: 'TENANT_FIELD', label: '租户外勤' },
    { value: 'MERCHANT', label: '商户' },
  ];
});

const rules: FormRules<RoleForm> = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleKey: [{ required: true, message: '请选择角色标识', trigger: 'change' }],
};

function scopeOf(roleKey: string): string {
  if (roleKey === 'PLATFORM_ADMIN') {
    return 'ALL';
  }
  if (roleKey === 'TENANT_FIELD') {
    return 'WORK_AREA';
  }
  if (roleKey === 'MERCHANT') {
    return 'MERCHANT';
  }
  return 'TENANT';
}

async function load(): Promise<void> {
  loading.value = true;
  try {
    const page = await pageRoles({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      keyword: query.keyword || undefined,
      status: query.status,
    });
    rows.value = page.records;
    total.value = page.total;
  } catch (error) {
    // 失败已由请求拦截器提示，此处仅记录，避免 onMounted/事件处理器产生未处理的 rejection
    console.error('[system/role] 加载失败', error);
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
  Object.assign(form, {
    roleName: '',
    roleKey: isPlatform.value ? 'PLATFORM_ADMIN' : 'TENANT_ADMIN',
    status: 1,
  });
  editingId.value = '';
  drawerTitle.value = '新增角色';
  drawerVisible.value = true;
}

function openEdit(row: RoleItem): void {
  Object.assign(form, { roleName: row.roleName, roleKey: row.roleKey, status: row.status });
  editingId.value = row.id;
  drawerTitle.value = '编辑角色';
  drawerVisible.value = true;
}

async function submit(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }
  submitting.value = true;
  try {
    const payload: RoleSavePayload = {
      roleName: form.roleName.trim(),
      roleKey: form.roleKey,
      status: form.status,
    };
    if (editingId.value) {
      await updateRole(editingId.value, payload);
    } else {
      await createRole(payload);
    }
    ElMessage.success('保存成功');
    drawerVisible.value = false;
    await load();
  } finally {
    submitting.value = false;
  }
}

async function remove(row: RoleItem): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认删除角色「${row.roleName}」？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    });
  } catch {
    return;
  }
  await deleteRole(row.id);
  ElMessage.success('已删除');
  await load();
}

async function openGrant(row: RoleItem): Promise<void> {
  grantRoleName.value = row.roleName;
  grantRoleId.value = row.id;
  const [tree, menuIds] = await Promise.all([menuTree(), roleMenuIds(row.id)]);
  menuTreeData.value = tree;
  checkedMenuIds.value = menuIds;
  grantVisible.value = true;
  await nextTick();
  treeRef.value?.setCheckedKeys(menuIds, false);
}

async function saveGrant(): Promise<void> {
  if (!grantRoleId.value) {
    return;
  }
  granting.value = true;
  try {
    const checked = (treeRef.value?.getCheckedKeys() ?? []) as string[];
    const halfChecked = (treeRef.value?.getHalfCheckedKeys() ?? []) as string[];
    await assignRoleMenus(grantRoleId.value, Array.from(new Set([...halfChecked, ...checked])));
    ElMessage.success('权限已保存');
    grantVisible.value = false;
  } finally {
    granting.value = false;
  }
}

onMounted(load);
</script>

<style scoped>
.grant-tip {
  margin-bottom: 12px;
  color: var(--fm-text-secondary);
  font-size: 13px;
}

.tree-node {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.tree-node__type {
  color: var(--fm-text-muted);
  font-size: 12px;
}
</style>
