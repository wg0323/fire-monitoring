<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1 class="page-title">菜单管理</h1>
        <p class="page-subtitle">
          平台管理员维护 tenant 0 基线；租户管理员维护本租户菜单，新增只作用于当前租户。
        </p>
      </div>
      <el-button v-auth="'menu:edit'" type="primary" @click="openCreate(null)">新增一级菜单</el-button>
    </div>

    <el-table
      v-loading="loading"
      :data="menuTreeData"
      row-key="id"
      border
      default-expand-all
      :tree-props="{ children: 'children' }"
    >
      <el-table-column prop="name" label="名称" min-width="180" />
      <el-table-column prop="menuKey" label="菜单标识" min-width="150" />
      <el-table-column label="类型" width="80">
        <template #default="{ row }">{{ menuTypeLabel(row.type) }}</template>
      </el-table-column>
      <el-table-column prop="path" label="路由" min-width="170">
        <template #default="{ row }">{{ row.path || '-' }}</template>
      </el-table-column>
      <el-table-column prop="perms" label="权限标识" min-width="150">
        <template #default="{ row }">{{ row.perms || '-' }}</template>
      </el-table-column>
      <el-table-column prop="seq" label="排序" width="70" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <FmStatusTag :status="row.status" />
        </template>
      </el-table-column>
      <el-table-column v-if="isPlatform" label="平台专属" width="90">
        <template #default="{ row }">{{ row.platformOnly === 1 ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.type !== 2"
            v-auth="'menu:edit'"
            link
            type="primary"
            @click="openCreate(row)"
          >
            新增子项
          </el-button>
          <el-button v-auth="'menu:edit'" link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button v-auth="'menu:edit'" link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-drawer v-model="drawerVisible" :title="drawerTitle" size="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="上级菜单">
          <el-tree-select
            v-model="form.parentId"
            :data="parentOptions"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            check-strictly
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" maxlength="32" placeholder="菜单名称" />
        </el-form-item>
        <el-form-item label="菜单标识" prop="menuKey">
          <el-input v-model="form.menuKey" maxlength="64" placeholder="如 system:user" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio :value="0">目录</el-radio>
            <el-radio :value="1">菜单</el-radio>
            <el-radio :value="2">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.type !== 2" label="路由">
          <el-input v-model="form.path" maxlength="255" placeholder="如 /system/user" />
        </el-form-item>
        <el-form-item v-if="form.type === 2" label="权限标识">
          <el-input v-model="form.perms" maxlength="64" placeholder="如 user:edit" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.seq" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item v-if="isPlatform" label="平台专属">
          <el-switch v-model="form.platformOnly" :active-value="1" :inactive-value="0" />
          <p class="form-tip">平台专属菜单在创建租户时不会复制。</p>
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
import { createMenu, deleteMenu, menuTree, updateMenu, type MenuSavePayload } from '@/api/system';
import { useAuthStore } from '@/stores/auth';
import type { MenuNode } from '@/types/api';
import { menuTypeLabel } from '@/utils/format';

interface MenuForm {
  parentId: string;
  name: string;
  menuKey: string;
  type: number;
  path: string;
  perms: string;
  seq: number;
  status: number;
  platformOnly: number;
}

const auth = useAuthStore();
const isPlatform = computed(() => auth.user?.userType === 'PLATFORM');

const loading = ref(false);
const submitting = ref(false);
const menuTreeData = ref<MenuNode[]>([]);
const drawerVisible = ref(false);
const drawerTitle = ref('新增菜单');
const editingId = ref('');
const formRef = ref<FormInstance>();
const form = reactive<MenuForm>(emptyForm());

const parentOptions = computed(() => [{
  id: '0',
  name: '根目录',
  children: menuTreeData.value,
}]);

const rules: FormRules<MenuForm> = {
  name: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  menuKey: [{ required: true, message: '请输入菜单标识', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
};

function emptyForm(): MenuForm {
  return {
    parentId: '0',
    name: '',
    menuKey: '',
    type: 1,
    path: '',
    perms: '',
    seq: 0,
    status: 1,
    platformOnly: 0,
  };
}

async function load(): Promise<void> {
  loading.value = true;
  try {
    menuTreeData.value = await menuTree();
  } catch (error) {
    // 失败已由请求拦截器提示，此处仅记录，避免 onMounted/事件处理器产生未处理的 rejection
    console.error('[system/menu] 加载失败', error);
  } finally {
    loading.value = false;
  }
}

function openCreate(parent: MenuNode | null): void {
  Object.assign(form, emptyForm());
  form.parentId = parent?.id ?? '0';
  form.type = parent ? 1 : 0;
  editingId.value = '';
  drawerTitle.value = '新增菜单';
  drawerVisible.value = true;
}

function openEdit(row: MenuNode): void {
  Object.assign(form, {
    parentId: row.parentId || '0',
    name: row.name,
    menuKey: row.menuKey,
    type: row.type,
    path: row.path ?? '',
    perms: row.perms ?? '',
    seq: row.seq ?? 0,
    status: row.status,
    platformOnly: row.platformOnly ?? 0,
  });
  editingId.value = row.id;
  drawerTitle.value = '编辑菜单';
  drawerVisible.value = true;
}

async function submit(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }
  if (form.type === 1 && !form.path) {
    ElMessage.warning('菜单类型必须填写路由');
    return;
  }
  if (form.type === 2 && !form.perms) {
    ElMessage.warning('按钮类型必须填写权限标识');
    return;
  }
  submitting.value = true;
  try {
    const payload: MenuSavePayload = {
      parentId: form.parentId || '0',
      name: form.name.trim(),
      menuKey: form.menuKey.trim(),
      type: form.type,
      path: form.type === 2 ? null : form.path || null,
      perms: form.type === 2 ? form.perms : null,
      seq: form.seq,
      status: form.status,
      platformOnly: isPlatform.value ? form.platformOnly : 0,
    };
    if (editingId.value) {
      await updateMenu(editingId.value, payload);
    } else {
      await createMenu(payload);
    }
    ElMessage.success('保存成功');
    drawerVisible.value = false;
    await load();
  } finally {
    submitting.value = false;
  }
}

async function remove(row: MenuNode): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认删除菜单「${row.name}」？有子菜单时会被拒绝。`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    });
  } catch {
    return;
  }
  await deleteMenu(row.id);
  ElMessage.success('已删除');
  await load();
}

onMounted(load);
</script>
