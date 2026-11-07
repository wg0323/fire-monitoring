<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1 class="page-title">{{ title }}</h1>
        <p class="page-subtitle">平台管理员维护平台基线字典，租户管理员维护本运营中心字典；已被业务数据引用的字典项不能删除。</p>
      </div>
      <el-button v-auth="'dict:edit'" type="primary" @click="openCreate">新增字典项</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" border>
      <el-table-column prop="code" label="代码" min-width="140" />
      <el-table-column prop="name" label="名称" min-width="160" />
      <el-table-column prop="remark" label="备注" min-width="220">
        <template #default="{ row }">{{ row.remark || '-' }}</template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="80">
        <template #default="{ row }">{{ row.sort ?? 0 }}</template>
      </el-table-column>
      <el-table-column v-if="isDeviceType" label="离线阈值" width="100">
        <template #default="{ row }">{{ row.offlineThreshold ?? '-' }}</template>
      </el-table-column>
      <el-table-column v-if="isDeviceType" label="离线单位" width="100">
        <template #default="{ row }">{{ unitLabel(row.offlineUnit) }}</template>
      </el-table-column>
      <el-table-column v-if="isDeviceType" label="大屏监测" width="90">
        <template #default="{ row }">{{ row.monitorFlag === 1 ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <FmStatusTag :status="row.status" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button v-auth="'dict:edit'" link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button v-auth="'dict:edit'" link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-drawer v-model="drawerVisible" :title="drawerTitle" size="480px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="代码" prop="code">
          <el-input v-model="form.code" maxlength="32" :disabled="!!editingId" placeholder="唯一代码，如 fire" />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" maxlength="32" placeholder="展示名称" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="512" show-word-limit />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="9999" />
        </el-form-item>
        <template v-if="isDeviceType">
          <el-form-item label="离线阈值">
            <el-input-number v-model="form.offlineThreshold" :min="0" :max="99999" placeholder="不填表示不判定" />
          </el-form-item>
          <el-form-item label="离线单位">
            <el-select v-model="form.offlineUnit" clearable style="width: 100%">
              <el-option label="小时" value="HOUR" />
              <el-option label="分钟" value="MINUTE" />
              <el-option label="天" value="DAY" />
            </el-select>
          </el-form-item>
          <el-form-item label="大屏监测">
            <el-switch v-model="form.monitorFlag" :active-value="1" :inactive-value="0" />
          </el-form-item>
        </template>
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
import { computed, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import FmStatusTag from '@/components/FmStatusTag.vue';
import { createDict, deleteDict, listDict, updateDict, type DictSavePayload } from '@/api/system';
import type { DictItem } from '@/types/api';

interface DictForm {
  code: string;
  name: string;
  remark: string;
  status: number;
  sort: number;
  offlineThreshold: number | null;
  offlineUnit: string;
  monitorFlag: number;
}

const offlineUnitLabels: Record<string, string> = { HOUR: '小时', MINUTE: '分钟', DAY: '天' };

const route = useRoute();
const dictType = computed(() => String(route.meta.dictType ?? ''));
const title = computed(() => String(route.meta.title ?? '字典管理'));
const isDeviceType = computed(() => dictType.value === 'device-type');

const loading = ref(false);
const submitting = ref(false);
const rows = ref<DictItem[]>([]);
const drawerVisible = ref(false);
const drawerTitle = ref('新增字典项');
const editingId = ref('');
const formRef = ref<FormInstance>();
const form = reactive<DictForm>({
  code: '',
  name: '',
  remark: '',
  status: 1,
  sort: 0,
  offlineThreshold: null,
  offlineUnit: 'HOUR',
  monitorFlag: 0,
});

const rules: FormRules<DictForm> = {
  code: [{ required: true, message: '请输入代码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
};

function unitLabel(unit?: string | null): string {
  return unit ? offlineUnitLabels[unit] ?? unit : '-';
}

async function load(): Promise<void> {
  if (!dictType.value) {
    return;
  }
  loading.value = true;
  try {
    rows.value = await listDict(dictType.value);
  } finally {
    loading.value = false;
  }
}

function openCreate(): void {
  Object.assign(form, {
    code: '',
    name: '',
    remark: '',
    status: 1,
    sort: 0,
    offlineThreshold: null,
    offlineUnit: 'HOUR',
    monitorFlag: 0,
  });
  editingId.value = '';
  drawerTitle.value = '新增字典项';
  drawerVisible.value = true;
}

function openEdit(row: DictItem): void {
  Object.assign(form, {
    code: row.code,
    name: row.name,
    remark: row.remark ?? '',
    status: row.status,
    sort: row.sort ?? 0,
    offlineThreshold: row.offlineThreshold ?? null,
    offlineUnit: row.offlineUnit ?? 'HOUR',
    monitorFlag: row.monitorFlag ?? 0,
  });
  editingId.value = row.id;
  drawerTitle.value = '编辑字典项';
  drawerVisible.value = true;
}

async function submit(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }
  submitting.value = true;
  try {
    const payload: DictSavePayload = {
      code: form.code.trim(),
      name: form.name.trim(),
      remark: form.remark || undefined,
      status: form.status,
      sort: form.sort,
    };
    if (isDeviceType.value) {
      payload.offlineThreshold = form.offlineThreshold;
      payload.offlineUnit = form.offlineUnit || undefined;
      payload.monitorFlag = form.monitorFlag;
    }
    if (editingId.value) {
      await updateDict(dictType.value, editingId.value, payload);
    } else {
      await createDict(dictType.value, payload);
    }
    ElMessage.success('保存成功');
    drawerVisible.value = false;
    await load();
  } finally {
    submitting.value = false;
  }
}

async function remove(row: DictItem): Promise<void> {
  const tip = `确认删除字典项「${row.name}」？已被设备、报警、隐患或商户引用的字典项不能删除。`;
  try {
    await ElMessageBox.confirm(tip, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    });
  } catch {
    return;
  }
  await deleteDict(dictType.value, row.id);
  ElMessage.success('已删除');
  await load();
}

watch(dictType, () => {
  load();
}, { immediate: true });
</script>
