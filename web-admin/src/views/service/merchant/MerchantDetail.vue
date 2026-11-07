<template>
  <section class="page-card merchant-detail" v-loading="loading">
    <div class="page-header detail-header">
      <div>
        <el-button class="back-link" link type="primary" @click="emit('back')">
          <svg
            width="14"
            height="14"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            aria-hidden="true"
          >
            <path d="M15 18l-6-6 6-6" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
          返回列表
        </el-button>
        <div class="detail-title">
          <h1 class="page-title">{{ detail?.name || '商户详情' }}</h1>
          <p class="page-subtitle">
            {{ detail?.typeName || '-' }} · {{ detail?.addr || '-' }}
          </p>
        </div>
      </div>
      <div class="detail-actions">
        <el-tag size="large" :type="detail?.status === 1 ? 'success' : 'info'" effect="plain">
          {{ detail?.status === 1 ? '启用' : '禁用' }}
        </el-tag>
        <el-button v-if="canEdit" type="primary" plain @click="emit('edit')">编辑</el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="基本信息" name="basic">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="商户类型">{{ detail?.typeName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="场所性质">
            {{ detail?.merchantKind === 2 ? '社会单位' : '九小场所' }}
          </el-descriptions-item>
          <el-descriptions-item label="登录账号">{{ detail?.loginAccount || '未创建' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ detail?.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="业主名称">{{ detail?.ownerName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="建筑面积">{{ detail?.floorage || '-' }}</el-descriptions-item>
          <el-descriptions-item label="员工人数">{{ detail?.employeeNum ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="绑定工作区">
            {{ detail?.workAreaNames?.join('、') || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="地址" :span="2">{{ detail?.addr || '-' }}</el-descriptions-item>
          <el-descriptions-item label="经纬度" :span="2">
            {{ coordinatesText }}
          </el-descriptions-item>
          <el-descriptions-item label="消防设施" :span="2">{{ detail?.fireRemark || '-' }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detail?.remark || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间" :span="2">
            {{ formatDateTime(detail?.createTime) }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="detail-block">
          <h3>商户位置</h3>
          <FmMap
            v-if="merchantPoints.length > 0"
            :points="merchantPoints"
            height="260px"
          />
          <div v-else class="empty-block">尚未设置商户经纬度</div>
        </div>

        <div v-if="photos.length > 0" class="detail-block">
          <h3>证照与照片</h3>
          <div class="photo-list">
            <el-image
              v-for="photo in photos"
              :key="photo"
              :src="photo"
              :preview-src-list="photos"
              fit="cover"
              class="detail-pic"
              alt="商户照片"
            />
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="楼宇楼层" name="building">
        <div class="table-toolbar">
          <span class="muted">平面图布点按楼层维护，设备点位坐标为百分比。</span>
          <el-button v-if="canEdit" type="primary" plain @click="openBuildingDialog()">新增楼宇</el-button>
        </div>

        <el-empty v-if="buildings.length === 0" description="尚未维护楼宇" />
        <div v-for="building in buildings" :key="building.id" class="building-block">
          <div class="building-head">
            <div>
              <strong>{{ building.name }}</strong>
              <span class="muted">共 {{ building.floors.length }} 层</span>
            </div>
            <div class="building-actions">
              <el-button v-if="canEdit" link type="primary" @click="openFloorDialog(building)">
                新增楼层
              </el-button>
              <el-button v-if="canEdit" link type="primary" @click="openBuildingDialog(building)">
                编辑
              </el-button>
              <el-button v-if="canEdit" link type="danger" @click="removeBuilding(building)">
                删除
              </el-button>
            </div>
          </div>
          <el-table :data="building.floors" border size="small">
            <el-table-column prop="name" label="楼层" width="100" />
            <el-table-column prop="floorNo" label="楼层编号" width="100" />
            <el-table-column label="平面图" width="110">
              <template #default="{ row }">
                <el-tag :type="row.floorPlanUrl ? 'success' : 'info'" effect="plain">
                  {{ row.floorPlanUrl ? '已上传' : '未上传' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="设备数" width="90">
              <template #default="{ row }">{{ row.deviceCount }}</template>
            </el-table-column>
            <el-table-column label="操作" min-width="160">
              <template #default="{ row }">
                <el-button link type="primary" @click="emit('open-plan', row.id)">
                  {{ canEdit ? '平面图布点' : '查看平面图' }}
                </el-button>
                <el-button v-if="canEdit" link type="primary" @click="openFloorDialog(building, row)">
                  编辑
                </el-button>
                <el-button v-if="canEdit" link type="danger" @click="removeFloor(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="设备" name="device">
        <div class="table-toolbar">
          <span class="muted">解绑后设备回到未绑定池，不删除设备档案。</span>
          <el-button v-if="canBind" type="primary" plain @click="openBindDevice">绑定设备</el-button>
        </div>
        <el-table :data="detail?.devices ?? []" border>
          <el-table-column prop="name" label="设备名称" min-width="140" />
          <el-table-column prop="sn" label="设备编号" min-width="150" />
          <el-table-column prop="typeName" label="类型" min-width="120" />
          <el-table-column prop="locationText" label="位置" min-width="180" />
          <el-table-column label="在线" width="90">
            <template #default="{ row }">
              <el-tag :type="row.onlineStatus === 1 ? 'success' : 'info'" effect="plain">
                {{ row.onlineStatus === 1 ? '在线' : '离线' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="最后通讯" width="165">
            <template #default="{ row }">{{ formatDateTime(row.lastTime) }}</template>
          </el-table-column>
          <el-table-column v-if="canBind" label="操作" width="90" fixed="right">
            <template #default="{ row }">
              <el-button link type="danger" @click="unbindDevice(row)">解绑</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="工作区" name="workArea">
        <div class="table-toolbar">
          <span class="muted">工作区决定外勤可见范围。</span>
          <el-button v-if="canBind" type="primary" plain @click="openBindWorkArea">绑定工作区</el-button>
        </div>
        <el-table :data="detail?.workAreas ?? []" border>
          <el-table-column prop="name" label="工作区名称" min-width="160" />
          <el-table-column prop="remark" label="说明" min-width="220" />
          <el-table-column v-if="canBind" label="操作" width="90" fixed="right">
            <template #default="{ row }">
              <el-button link type="danger" @click="unbindWorkArea(row)">解绑</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog
      v-model="buildingVisible"
      :title="buildingForm.id ? '编辑楼宇' : '新增楼宇'"
      width="480px"
      append-to-body
    >
      <el-form label-width="88px">
        <el-form-item label="楼宇名称" required>
          <el-input v-model="buildingForm.name" maxlength="64" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="buildingForm.sort" :min="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="buildingForm.remark" type="textarea" :rows="3" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="buildingVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="saveBuilding">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="floorVisible"
      :title="floorForm.id ? '编辑楼层' : '新增楼层'"
      width="480px"
      append-to-body
    >
      <el-form label-width="88px">
        <el-form-item label="所属楼宇">
          <span>{{ floorForm.buildingName }}</span>
        </el-form-item>
        <el-form-item label="楼层名称" required>
          <el-input v-model="floorForm.name" maxlength="64" placeholder="如 1F、B1" />
        </el-form-item>
        <el-form-item label="楼层编号">
          <el-input-number v-model="floorForm.floorNo" controls-position="right" />
          <span class="form-tip">负数地下、正数地上</span>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="floorForm.sort" :min="0" controls-position="right" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="floorVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="saveFloor">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="workAreaVisible" title="绑定工作区" width="520px" append-to-body>
      <el-select v-model="selectedWorkAreaIds" multiple placeholder="请选择工作区" style="width: 100%">
        <el-option
          v-for="area in workAreaOptionsList"
          :key="area.id"
          :label="area.name"
          :value="area.id"
        />
      </el-select>
      <template #footer>
        <el-button @click="workAreaVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="saveWorkAreas">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="deviceVisible" title="绑定设备" width="720px" append-to-body>
      <div class="filter-bar">
        <el-input
          v-model="deviceKeyword"
          placeholder="设备编号 / 名称"
          clearable
          style="width: 220px"
          @keyup.enter="loadUnboundDevices"
        />
        <el-button type="primary" plain @click="loadUnboundDevices">查询</el-button>
      </div>
      <el-table
        :data="unboundDevices"
        border
        size="small"
        row-key="id"
        @selection-change="handleDeviceSelection"
      >
        <el-table-column type="selection" width="46" />
        <el-table-column prop="sn" label="设备编号" min-width="150" />
        <el-table-column prop="name" label="设备名称" min-width="130" />
        <el-table-column label="类型" width="120">
          <template #default="{ row }">{{ row.typeName || row.type || '-' }}</template>
        </el-table-column>
        <el-table-column prop="position" label="安装位置" min-width="150" />
      </el-table>
      <el-empty v-if="unboundDevices.length === 0" description="没有可绑定的设备" />
      <template #footer>
        <el-button @click="deviceVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="saveDevices">绑定</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  bindMerchantDevices,
  bindMerchantWorkAreas,
  unbindMerchantDevice,
} from '@/api/merchant';
import {
  createBuilding,
  createFloor,
  deleteBuilding,
  deleteFloor,
  pageDevices,
  updateBuilding,
  updateFloor,
} from '@/api/device';
import { workAreaOptions } from '@/api/system';
import type {
  BuildingNode,
  DeviceBriefRow,
  FloorNode,
  MerchantDeviceBrief,
  MerchantDetail,
  WorkAreaBrief,
  WorkAreaItem,
} from '@/types/api';
import type { MapPoint } from '@/components/map/types';
import FmMap from '@/components/map/FmMap.vue';
import { formatDateTime } from '@/utils/format';

const props = defineProps<{
  detail: MerchantDetail | null;
  loading: boolean;
  canEdit: boolean;
  canBind: boolean;
  initialTab?: string;
}>();

const emit = defineEmits<{
  (event: 'back'): void;
  (event: 'refresh'): void;
  (event: 'edit'): void;
  (event: 'open-plan', floorId: string): void;
}>();

const activeTab = ref(props.initialTab || 'basic');
const submitting = ref(false);
const buildingVisible = ref(false);
const floorVisible = ref(false);
const workAreaVisible = ref(false);
const deviceVisible = ref(false);

const buildingForm = ref<{
  id?: string;
  name: string;
  sort: number;
  remark: string;
}>({ name: '', sort: 0, remark: '' });

const floorForm = ref<{
  id?: string;
  buildingId: string;
  buildingName: string;
  name: string;
  floorNo: number | null;
  sort: number;
}>({ buildingId: '', buildingName: '', name: '', floorNo: null, sort: 0 });

const workAreaOptionsList = ref<WorkAreaItem[]>([]);
const selectedWorkAreaIds = ref<string[]>([]);
const unboundDevices = ref<DeviceBriefRow[]>([]);
const selectedDeviceIds = ref<string[]>([]);
const deviceKeyword = ref('');

const buildings = computed<BuildingNode[]>(() => props.detail?.buildings ?? []);

watch(
  () => props.initialTab,
  (tab) => {
    if (tab) {
      activeTab.value = tab;
    }
  },
);

const photos = computed(() => {
  const detail = props.detail;
  if (!detail) {
    return [];
  }
  return [detail.businessLicense, detail.placePic1, detail.placePic2, detail.placePic3]
    .filter((photo): photo is string => Boolean(photo));
});

const coordinatesText = computed(() => {
  const detail = props.detail;
  if (!detail || detail.lng === null || detail.lng === undefined
    || detail.lat === null || detail.lat === undefined) {
    return '-';
  }
  return `${detail.lng}, ${detail.lat}`;
});

const merchantPoints = computed<MapPoint[]>(() => {
  const detail = props.detail;
  if (!detail || detail.lng === null || detail.lng === undefined
    || detail.lat === null || detail.lat === undefined) {
    return [];
  }
  return [{
    id: detail.id,
    name: detail.name,
    lng: detail.lng,
    lat: detail.lat,
    status: detail.status === 1 ? 'normal' : 'offline',
    subtitle: detail.addr ?? '',
  }];
});

function openBuildingDialog(building?: BuildingNode): void {
  buildingForm.value = building
    ? { id: building.id, name: building.name, sort: building.sort ?? 0, remark: building.remark ?? '' }
    : { name: '', sort: 0, remark: '' };
  buildingVisible.value = true;
}

async function saveBuilding(): Promise<void> {
  const form = buildingForm.value;
  if (!form.name.trim()) {
    ElMessage.warning('请输入楼宇名称');
    return;
  }
  submitting.value = true;
  try {
    const payload = {
      merchantId: props.detail?.id ?? '',
      name: form.name.trim(),
      sort: form.sort,
      remark: form.remark,
      status: 1,
    };
    if (form.id) {
      await updateBuilding(form.id, payload);
    } else {
      await createBuilding(payload);
    }
    ElMessage.success('已保存');
    buildingVisible.value = false;
    emit('refresh');
  } finally {
    submitting.value = false;
  }
}

async function removeBuilding(building: BuildingNode): Promise<void> {
  const confirmed = await confirmAction(`确认删除楼宇「${building.name}」？`);
  if (!confirmed) {
    return;
  }
  await deleteBuilding(building.id);
  ElMessage.success('已删除');
  emit('refresh');
}

function openFloorDialog(building: BuildingNode, floor?: FloorNode): void {
  floorForm.value = floor
    ? {
      id: floor.id,
      buildingId: building.id,
      buildingName: building.name,
      name: floor.name,
      floorNo: floor.floorNo ?? null,
      sort: 0,
    }
    : {
      buildingId: building.id,
      buildingName: building.name,
      name: '',
      floorNo: null,
      sort: 0,
    };
  floorVisible.value = true;
}

async function saveFloor(): Promise<void> {
  const form = floorForm.value;
  if (!form.name.trim()) {
    ElMessage.warning('请输入楼层名称');
    return;
  }
  submitting.value = true;
  try {
    const payload = {
      buildingId: form.buildingId,
      merchantId: props.detail?.id ?? '',
      name: form.name.trim(),
      floorNo: form.floorNo ?? undefined,
      sort: form.sort,
      status: 1,
    };
    if (form.id) {
      await updateFloor(form.id, payload);
    } else {
      await createFloor(payload);
    }
    ElMessage.success('已保存');
    floorVisible.value = false;
    emit('refresh');
  } finally {
    submitting.value = false;
  }
}

async function removeFloor(floor: FloorNode): Promise<void> {
  const confirmed = await confirmAction(`确认删除楼层「${floor.name}」？`);
  if (!confirmed) {
    return;
  }
  await deleteFloor(floor.id);
  ElMessage.success('已删除');
  emit('refresh');
}

async function openBindWorkArea(): Promise<void> {
  workAreaOptionsList.value = await workAreaOptions();
  selectedWorkAreaIds.value = (props.detail?.workAreas ?? []).map((area) => area.id);
  workAreaVisible.value = true;
}

async function saveWorkAreas(): Promise<void> {
  if (!props.detail) {
    return;
  }
  submitting.value = true;
  try {
    await bindMerchantWorkAreas(props.detail.id, selectedWorkAreaIds.value);
    ElMessage.success('已保存');
    workAreaVisible.value = false;
    emit('refresh');
  } finally {
    submitting.value = false;
  }
}

async function unbindWorkArea(area: WorkAreaBrief): Promise<void> {
  if (!props.detail) {
    return;
  }
  const confirmed = await confirmAction(`确认解绑工作区「${area.name}」？`);
  if (!confirmed) {
    return;
  }
  const remain = (props.detail.workAreas ?? [])
    .filter((item) => item.id !== area.id)
    .map((item) => item.id);
  await bindMerchantWorkAreas(props.detail.id, remain);
  ElMessage.success('已解绑');
  emit('refresh');
}

async function openBindDevice(): Promise<void> {
  deviceKeyword.value = '';
  selectedDeviceIds.value = [];
  await loadUnboundDevices();
  deviceVisible.value = true;
}

async function loadUnboundDevices(): Promise<void> {
  const page = await pageDevices({
    pageNum: 1,
    pageSize: 100,
    keyword: deviceKeyword.value.trim() || undefined,
    unbound: true,
    status: 1,
  });
  unboundDevices.value = page.records;
}

function handleDeviceSelection(rows: DeviceBriefRow[]): void {
  selectedDeviceIds.value = rows.map((row) => row.id);
}

async function saveDevices(): Promise<void> {
  if (!props.detail) {
    return;
  }
  if (selectedDeviceIds.value.length === 0) {
    ElMessage.warning('请选择设备');
    return;
  }
  submitting.value = true;
  try {
    await bindMerchantDevices(props.detail.id, selectedDeviceIds.value);
    ElMessage.success('已绑定');
    deviceVisible.value = false;
    emit('refresh');
  } finally {
    submitting.value = false;
  }
}

async function unbindDevice(device: MerchantDeviceBrief): Promise<void> {
  if (!props.detail) {
    return;
  }
  const confirmed = await confirmAction(`确认解绑设备「${device.sn}」？`);
  if (!confirmed) {
    return;
  }
  await unbindMerchantDevice(props.detail.id, device.id);
  ElMessage.success('已解绑');
  emit('refresh');
}

async function confirmAction(message: string): Promise<boolean> {
  try {
    await ElMessageBox.confirm(message, '操作确认', { type: 'warning' });
    return true;
  } catch {
    return false;
  }
}
</script>

<style scoped>
.detail-header {
  align-items: flex-end;
}

.back-link {
  height: auto;
  padding: 0;
  margin-bottom: var(--fm-space-2);
  font-size: 13px;
}

.back-link svg {
  margin-right: var(--fm-space-1);
}

.detail-title {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: var(--fm-space-2);
}

/* 类型与地址跟随标题同行，取消副标题的上外边距 */
.detail-title .page-subtitle {
  margin-top: 0;
}

.detail-actions {
  display: flex;
  align-items: center;
  gap: var(--fm-space-2);
}

/* 状态标签与同排的编辑按钮保持同高、同宽、同字号、同圆角 */
.detail-actions :deep(.el-tag) {
  padding: 0 15px;
  font-size: 14px;
  border-radius: var(--fm-radius-md);
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

.photo-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--fm-space-2);
}

.detail-pic {
  width: 120px;
  height: 90px;
  border-radius: var(--fm-radius-sm);
}

.building-block {
  margin-bottom: var(--fm-space-4);
  padding: var(--fm-space-4);
  border: 1px solid var(--fm-border);
  border-radius: var(--fm-radius-md);
}

.building-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--fm-space-3);
  margin-bottom: var(--fm-space-3);
}

.building-head strong {
  margin-right: var(--fm-space-2);
  font-size: 14px;
}

.building-actions {
  display: flex;
  gap: var(--fm-space-1);
}

.form-tip {
  margin-left: var(--fm-space-2);
  color: var(--fm-text-muted);
  font-size: 12px;
}
</style>
