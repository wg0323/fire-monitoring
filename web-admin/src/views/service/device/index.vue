<template>
  <DeviceDetailView
    v-if="mode === 'detail'"
    :detail="detail"
    :loading="detailLoading"
    :can-edit="canEdit"
    @back="backToList"
    @edit="openEdit(detail ?? undefined)"
  />

  <section v-else class="page-card">
    <div class="page-header">
      <div>
        <h1 class="page-title">设备管理</h1>
        <p class="page-subtitle">设备档案、定位与在线状态；详情内查看实时快照、趋势与最近报警。</p>
      </div>
      <div class="header-actions">
        <el-button v-if="!auth.isPlatform" @click="handleExport">导出</el-button>
        <el-button v-if="canImport" @click="importVisible = true">批量导入</el-button>
        <el-button v-if="canEdit" type="primary" @click="openCreate">新增设备</el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-input
        v-model="query.keyword"
        placeholder="设备编号 / 名称 / 位置"
        clearable
        style="width: 220px"
        @keyup.enter="search"
      />
      <el-select v-model="query.type" placeholder="设备类型" clearable style="width: 150px">
        <el-option
          v-for="item in activeTypes"
          :key="item.code"
          :label="item.name"
          :value="item.code"
        />
      </el-select>
      <el-select
        v-model="query.merchantId"
        placeholder="所属商户"
        clearable
        filterable
        style="width: 180px"
      >
        <el-option
          v-for="item in merchants"
          :key="item.id"
          :label="item.name"
          :value="item.id"
        />
      </el-select>
      <el-select v-model="query.locationType" placeholder="定位方式" clearable style="width: 130px">
        <el-option label="未设置" :value="0" />
        <el-option label="室外 GPS" :value="1" />
        <el-option label="室内楼层图" :value="2" />
      </el-select>
      <el-select v-model="query.onlineStatus" placeholder="在线状态" clearable style="width: 120px">
        <el-option label="在线" :value="1" />
        <el-option label="离线" :value="0" />
      </el-select>
      <el-select v-model="query.status" placeholder="启用状态" clearable style="width: 120px">
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
      empty-text="暂无设备"
    >
      <el-table-column prop="sn" label="设备编号" width="190" show-overflow-tooltip />
      <el-table-column label="设备类型" width="120">
        <template #default="{ row }">{{ row.typeName || row.type || '-' }}</template>
      </el-table-column>
      <el-table-column label="所属商户" min-width="140" show-overflow-tooltip>
        <template #default="{ row }">{{ row.merchantName || '-' }}</template>
      </el-table-column>
      <el-table-column label="定位方式" width="96">
        <template #default="{ row }">{{ locationTypeText(row.locationType) }}</template>
      </el-table-column>
      <el-table-column label="位置" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">{{ row.locationText || '-' }}</template>
      </el-table-column>
      <el-table-column label="激活时间" width="156">
        <template #default="{ row }">{{ formatDate(row.activeDate) }}</template>
      </el-table-column>
      <el-table-column label="最后通讯" width="156">
        <template #default="{ row }">{{ formatTime(row.lastTime) }}</template>
      </el-table-column>
      <el-table-column label="在线状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.onlineStatus === 1 ? 'success' : 'info'" effect="plain">
            {{ row.onlineStatus === 1 ? '在线' : '离线' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">查看</el-button>
          <el-button v-if="canEdit" link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="canEdit" link type="danger" @click="removeDevice(row)">删除</el-button>
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

  <DeviceForm
    v-model:visible="formVisible"
    :device="editingDevice"
    :device-types="types"
    :makers="makers"
    :merchants="merchants"
    @saved="handleSaved"
  />

  <DeviceImportDialog v-model:visible="importVisible" @imported="load" />
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import dayjs from 'dayjs';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  deleteDevice,
  deviceMakers,
  deviceTypes,
  exportDevices,
  getDevice,
  merchantOptions,
  pageDevices,
  type DeviceQuery,
} from '@/api/device';
import type { DeviceDetail, DeviceItem, DictItem, MerchantOption } from '@/types/api';
import { useAuthStore } from '@/stores/auth';
import DeviceForm from './DeviceForm.vue';
import DeviceDetailView from './DeviceDetail.vue';
import DeviceImportDialog from './DeviceImportDialog.vue';

const auth = useAuthStore();
const mode = ref<'list' | 'detail'>('list');
const detailId = ref('');
const detail = ref<DeviceDetail | null>(null);
const detailLoading = ref(false);
const loading = ref(false);
const rows = ref<DeviceItem[]>([]);
const total = ref(0);
const types = ref<DictItem[]>([]);
const makers = ref<DictItem[]>([]);
const merchants = ref<MerchantOption[]>([]);
const formVisible = ref(false);
const importVisible = ref(false);
const editingDevice = ref<DeviceItem | null>(null);

const query = reactive<DeviceQuery>({
  pageNum: 1,
  pageSize: 20,
});

const canEdit = computed(() => !auth.isPlatform && auth.hasPerm('device:edit'));
const canImport = computed(() => !auth.isPlatform && auth.hasPerm('device:import'));
const activeTypes = computed(() => types.value.filter((item) => item.status === 1));

async function load(): Promise<void> {
  loading.value = true;
  try {
    const page = await pageDevices(query);
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
  query.merchantId = undefined;
  query.locationType = undefined;
  query.onlineStatus = undefined;
  query.status = undefined;
  search();
}

function openCreate(): void {
  editingDevice.value = null;
  formVisible.value = true;
}

function openEdit(device?: DeviceItem): void {
  if (!device) {
    return;
  }
  editingDevice.value = device;
  formVisible.value = true;
}

async function handleSaved(): Promise<void> {
  if (mode.value === 'detail' && detailId.value) {
    await loadDetail();
    return;
  }
  await load();
}

async function openDetail(row: DeviceItem): Promise<void> {
  mode.value = 'detail';
  detailId.value = row.id;
  await loadDetail();
}

async function loadDetail(): Promise<void> {
  if (!detailId.value) {
    return;
  }
  detailLoading.value = true;
  try {
    detail.value = await getDevice(detailId.value);
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

async function removeDevice(row: DeviceItem): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认删除设备「${row.sn}」？删除后不可恢复。`, '操作确认', {
      type: 'warning',
    });
  } catch {
    return;
  }
  await deleteDevice(row.id);
  ElMessage.success('已删除');
  if (mode.value === 'detail' && detailId.value === row.id) {
    backToList();
    return;
  }
  await load();
}

async function handleExport(): Promise<void> {
  try {
    await exportDevices(query);
  } catch (error) {
    // download 可能对 JSON 错误体 reject，此处兜底记录，避免事件处理器产生未处理的 rejection
    console.error('[device] 导出失败', error);
  }
}

function locationTypeText(type?: number | null): string {
  if (type === 1) {
    return '室外 GPS';
  }
  if (type === 2) {
    return '室内楼层图';
  }
  return '未设置';
}

function formatTime(value?: string | null): string {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-';
}

function formatDate(value?: string | null): string {
  return value ? dayjs(value).format('YYYY-MM-DD') : '-';
}

onMounted(async () => {
  const [typeList, makerList, merchantList] = await Promise.all([
    deviceTypes(),
    deviceMakers(),
    merchantOptions().catch(() => [] as MerchantOption[]),
  ]);
  types.value = typeList;
  makers.value = makerList;
  merchants.value = merchantList;
  await load();
});
</script>

<style scoped>
.header-actions {
  display: flex;
  gap: var(--fm-space-2);
}
</style>
