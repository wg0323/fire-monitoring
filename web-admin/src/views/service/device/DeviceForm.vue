<template>
  <el-drawer v-model="drawerVisible" :title="device ? '编辑设备' : '新增设备'" size="640px" destroy-on-close>
    <el-form label-width="112px" class="device-form">
      <div class="form-section">基本信息</div>
      <el-form-item label="设备编号" required>
        <el-input
          v-model="form.sn"
          maxlength="32"
          placeholder="全局唯一，保存后不可修改"
          :disabled="Boolean(device)"
        />
      </el-form-item>
      <el-form-item label="设备名称">
        <el-input v-model="form.name" maxlength="64" placeholder="请输入设备名称" />
      </el-form-item>
      <el-form-item label="设备类型" required>
        <el-select
          v-model="form.type"
          placeholder="请选择设备类型"
          style="width: 100%"
          @change="handleTypeChange"
        >
          <el-option
            v-for="item in activeTypes"
            :key="item.code"
            :label="item.name"
            :value="item.code"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="所属商户">
        <el-select
          v-model="form.merchantId"
          clearable
          filterable
          placeholder="请选择商户"
          style="width: 100%"
          @change="handleMerchantChange"
        >
          <el-option
            v-for="item in merchants"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="制造商">
        <el-select v-model="form.maker" clearable placeholder="请选择制造商" style="width: 100%">
          <el-option
            v-for="item in activeMakers"
            :key="item.code"
            :label="item.name"
            :value="item.code"
          />
        </el-select>
      </el-form-item>

      <template v-if="fieldGroup === 'aep'">
        <div class="form-section">接入参数</div>
        <el-form-item label="AEP 设备 ID" required>
          <el-input v-model="form.aepId" maxlength="64" placeholder="天翼物联网平台设备 ID" />
        </el-form-item>
        <el-form-item label="AEP 密钥" required>
          <el-input
            v-model="form.aepKey"
            maxlength="128"
            show-password
            placeholder="天翼物联网平台设备密钥"
          />
        </el-form-item>
        <div class="form-tip field-tip">该类型设备经天翼物联网平台接入，凭证缺失将无法解析上报数据。</div>
      </template>
      <template v-else-if="fieldGroup === 'video'">
        <div class="form-section">接入参数</div>
        <el-form-item label="设备用户名" required>
          <el-input v-model="form.username" maxlength="64" placeholder="采集程序登录设备时使用" />
        </el-form-item>
        <el-form-item label="设备密码" required>
          <el-input
            v-model="form.password"
            maxlength="64"
            show-password
            placeholder="采集程序登录设备时使用"
          />
        </el-form-item>
        <div class="form-tip field-tip">
          通道数、登录句柄、IP 端口、抓图地址、在线状态等由采集程序回写，无需手工填写。
        </div>
      </template>

      <div class="form-section">定位</div>
      <el-form-item label="定位方式" required>
        <el-radio-group v-model="form.locationType" @change="handleLocationTypeChange">
          <el-radio :value="0">未设置</el-radio>
          <el-radio :value="1">室外 GPS</el-radio>
          <el-radio :value="2">室内楼层图</el-radio>
        </el-radio-group>
      </el-form-item>
      <template v-if="form.locationType === 1">
        <el-form-item label="经纬度" required>
          <div class="coord-row">
            <el-input-number
              v-model="form.lng"
              :precision="6"
              :step="0.000001"
              :min="-180"
              :max="180"
              controls-position="right"
              placeholder="经度"
            />
            <el-input-number
              v-model="form.lat"
              :precision="6"
              :step="0.000001"
              :min="-90"
              :max="90"
              controls-position="right"
              placeholder="纬度"
            />
            <el-button plain @click="mapVisible = true">地图选点</el-button>
          </div>
        </el-form-item>
      </template>
      <template v-else-if="form.locationType === 2">
        <el-form-item label="楼宇" required>
          <el-select
            v-model="form.buildingId"
            clearable
            placeholder="请选择楼宇"
            style="width: 100%"
            :disabled="!form.merchantId"
            @change="handleBuildingChange"
          >
            <el-option
              v-for="item in buildings"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="楼层" required>
          <el-select
            v-model="form.floorId"
            clearable
            placeholder="请选择楼层"
            style="width: 100%"
            :disabled="!form.buildingId"
            @change="handleFloorChange"
          >
            <el-option
              v-for="item in floors"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.floorX !== null && form.floorY !== null" label="平面坐标">
          <span class="text-secondary">X {{ form.floorX }} / Y {{ form.floorY }}</span>
        </el-form-item>
        <div class="form-tip floor-tip">楼层坐标在商户详情的平面图布点中维护，此处只绑定楼宇与楼层。</div>
      </template>

      <div class="form-section">安装与通讯</div>
      <el-form-item label="安装位置">
        <el-input v-model="form.position" maxlength="255" placeholder="展示在报警详情" />
      </el-form-item>
      <el-form-item label="SIM 卡">
        <el-input v-model="form.sim" maxlength="32" />
      </el-form-item>
      <el-form-item label="4G 卡号">
        <el-input v-model="form.cardNo" maxlength="24" />
      </el-form-item>
      <div v-if="fieldGroup === 'electric'" class="form-tip field-tip">
        电压、电流、温度、剩余电流、报警浓度由设备上报，新增时无需填写。
      </div>
      <el-form-item label="激活日期">
        <el-date-picker
          v-model="form.activeDate"
          type="date"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DDTHH:mm:ss"
          :disabled-date="futureDisabled"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="安装日期">
        <el-date-picker
          v-model="form.installDate"
          type="date"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DDTHH:mm:ss"
          :disabled-date="futureDisabled"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="状态">
        <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        <span class="form-tip">禁用后设备不再参与报警与展示</span>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="drawerVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
    </template>
  </el-drawer>

  <el-dialog v-model="mapVisible" title="地图选点" width="880px" append-to-body>
    <FmMap pick-mode :points="[]" :pick-value="pickValue" height="460px" @pick="handlePick" />
    <div class="form-tip">点击地图选择设备位置。</div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { createDevice, listBuildings, listFloors, updateDevice, type DevicePayload } from '@/api/device';
import type {
  BuildingItem,
  DeviceItem,
  DictItem,
  FloorItem,
  MerchantOption,
} from '@/types/api';
import type { MapPick } from '@/components/map/types';
import FmMap from '@/components/map/FmMap.vue';
import { resolveFieldGroup } from './deviceTypeFields';

interface DeviceFormState {
  sn: string;
  name: string;
  type: string;
  merchantId: string | null;
  maker: string | null;
  position: string;
  lng: number | null;
  lat: number | null;
  locationType: number;
  buildingId: string | null;
  floorId: string | null;
  floorX: number | null;
  floorY: number | null;
  sim: string;
  cardNo: string;
  activeDate: string | null;
  installDate: string | null;
  status: number;
  aepId: string;
  aepKey: string;
  username: string;
  password: string;
}

const props = defineProps<{
  visible: boolean;
  device: DeviceItem | null;
  deviceTypes: DictItem[];
  makers: DictItem[];
  merchants: MerchantOption[];
}>();

const emit = defineEmits<{
  (event: 'update:visible', value: boolean): void;
  (event: 'saved'): void;
}>();

const saving = ref(false);
const mapVisible = ref(false);
const buildings = ref<BuildingItem[]>([]);
const floors = ref<FloorItem[]>([]);
const form = reactive<DeviceFormState>(emptyForm());
let initialFloorId: string | null = null;
/** 编辑时保留采集程序回写的 f_ext 字段，提交时只覆盖本表单负责的键。 */
let extCache: Record<string, unknown> = {};

const drawerVisible = computed({
  get: () => props.visible,
  set: (value: boolean) => emit('update:visible', value),
});

const activeTypes = computed(() => props.deviceTypes.filter((item) => item.status === 1));
const activeMakers = computed(() => props.makers.filter((item) => item.status === 1));
const fieldGroup = computed(() => {
  const matched = props.deviceTypes.find((item) => item.code === form.type);
  return resolveFieldGroup(form.type, matched?.name);
});
const pickValue = computed(() => {
  if (form.lng === null || form.lat === null) {
    return null;
  }
  return { lng: form.lng, lat: form.lat };
});

watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      fillForm();
    }
  },
);

function emptyForm(): DeviceFormState {
  return {
    sn: '',
    name: '',
    type: '',
    merchantId: null,
    maker: null,
    position: '',
    lng: null,
    lat: null,
    locationType: 0,
    buildingId: null,
    floorId: null,
    floorX: null,
    floorY: null,
    sim: '',
    cardNo: '',
    activeDate: null,
    installDate: null,
    status: 1,
    aepId: '',
    aepKey: '',
    username: '',
    password: '',
  };
}

async function fillForm(): Promise<void> {
  Object.assign(form, emptyForm());
  buildings.value = [];
  floors.value = [];
  initialFloorId = null;
  extCache = {};
  if (!props.device) {
    return;
  }
  Object.assign(form, {
    sn: props.device.sn,
    name: props.device.name ?? '',
    type: props.device.type ?? '',
    merchantId: props.device.merchantId ?? null,
    maker: props.device.maker ?? null,
    position: props.device.position ?? '',
    lng: props.device.lng ?? null,
    lat: props.device.lat ?? null,
    locationType: props.device.locationType ?? 0,
    buildingId: props.device.buildingId ?? null,
    floorId: props.device.floorId ?? null,
    floorX: props.device.floorX ?? null,
    floorY: props.device.floorY ?? null,
    sim: props.device.sim ?? '',
    cardNo: props.device.cardNo ?? '',
    activeDate: props.device.activeDate ?? null,
    installDate: props.device.installDate ?? null,
    status: props.device.status ?? 1,
  });
  extCache = parseFExt(props.device.fExt);
  form.aepId = extText(extCache.aepId);
  form.aepKey = extText(extCache.aepKey);
  form.username = extText(extCache.username);
  form.password = extText(extCache.password);
  initialFloorId = form.floorId;
  if (form.merchantId) {
    await loadBuildings(form.merchantId);
  }
  if (form.buildingId && form.merchantId) {
    await loadFloors(form.merchantId, form.buildingId);
  }
}

async function loadBuildings(merchantId: string): Promise<void> {
  try {
    buildings.value = await listBuildings(merchantId);
  } catch {
    buildings.value = [];
  }
}

async function loadFloors(merchantId: string, buildingId: string): Promise<void> {
  try {
    floors.value = await listFloors({ merchantId, buildingId });
  } catch {
    floors.value = [];
  }
}

async function handleMerchantChange(): Promise<void> {
  const merchantId = form.merchantId;
  form.buildingId = null;
  form.floorId = null;
  form.floorX = null;
  form.floorY = null;
  floors.value = [];
  buildings.value = [];
  if (merchantId) {
    await loadBuildings(merchantId);
  }
}

async function handleBuildingChange(): Promise<void> {
  const buildingId = form.buildingId;
  form.floorId = null;
  form.floorX = null;
  form.floorY = null;
  floors.value = [];
  if (buildingId && form.merchantId) {
    await loadFloors(form.merchantId, buildingId);
  }
}

function handleFloorChange(): void {
  const floorId = form.floorId;
  if (floorId !== initialFloorId) {
    form.floorX = null;
    form.floorY = null;
  }
}

function handleLocationTypeChange(): void {
  const value = form.locationType;
  if (value !== 1) {
    form.lng = null;
    form.lat = null;
  }
  if (value !== 2) {
    form.buildingId = null;
    form.floorId = null;
    form.floorX = null;
    form.floorY = null;
    floors.value = [];
  }
}

/** 切换设备类型时清掉不适用的接入凭证，避免把 AEP / 视频账号写进别的类型。 */
function handleTypeChange(): void {
  if (fieldGroup.value !== 'aep') {
    form.aepId = '';
    form.aepKey = '';
  }
  if (fieldGroup.value !== 'video') {
    form.username = '';
    form.password = '';
  }
}

function parseFExt(raw?: string | null): Record<string, unknown> {
  if (!raw) {
    return {};
  }
  try {
    const parsed = JSON.parse(raw);
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed)
      ? (parsed as Record<string, unknown>)
      : {};
  } catch {
    return {};
  }
}

function extText(value: unknown): string {
  if (value === null || value === undefined) {
    return '';
  }
  return typeof value === 'string' ? value : String(value);
}

/**
 * f_ext 交给后端原样落库：只覆盖表单负责的键，采集程序回写的字段（登录句柄、
 * 通道数、IP 端口、抓图地址等）在编辑时必须保留。
 */
function buildFExt(): string | undefined {
  const ext: Record<string, unknown> = { ...extCache };
  const group = fieldGroup.value;
  if (group !== 'unknown') {
    delete ext.aepId;
    delete ext.aepKey;
    delete ext.username;
    delete ext.password;
    if (group === 'aep') {
      ext.aepId = form.aepId.trim();
      ext.aepKey = form.aepKey.trim();
    } else if (group === 'video') {
      ext.username = form.username.trim();
      ext.password = form.password;
    }
  }
  if (!Object.keys(ext).length) {
    return props.device ? '{}' : undefined;
  }
  return JSON.stringify(ext);
}

function handlePick(pick: MapPick): void {
  form.lng = Number(pick.lng.toFixed(6));
  form.lat = Number(pick.lat.toFixed(6));
  mapVisible.value = false;
}

function futureDisabled(date: Date): boolean {
  return date.getTime() > Date.now();
}

function validate(): boolean {
  if (!form.sn.trim()) {
    ElMessage.warning('请输入设备编号');
    return false;
  }
  if (!form.type) {
    ElMessage.warning('请选择设备类型');
    return false;
  }
  if (fieldGroup.value === 'aep') {
    if (!form.aepId.trim()) {
      ElMessage.warning('请填写 AEP 设备 ID');
      return false;
    }
    if (!form.aepKey.trim()) {
      ElMessage.warning('请填写 AEP 密钥');
      return false;
    }
  }
  if (fieldGroup.value === 'video') {
    if (!form.username.trim()) {
      ElMessage.warning('请填写设备用户名');
      return false;
    }
    if (!form.password) {
      ElMessage.warning('请填写设备密码');
      return false;
    }
  }
  if (form.locationType === 1 && (form.lng === null || form.lat === null)) {
    ElMessage.warning('GPS 定位必须填写经纬度');
    return false;
  }
  if (form.locationType === 2 && (!form.buildingId || !form.floorId)) {
    ElMessage.warning('楼层定位必须选择楼宇与楼层');
    return false;
  }
  return true;
}

async function submit(): Promise<void> {
  if (!validate()) {
    return;
  }
  saving.value = true;
  try {
    const payload = buildPayload();
    if (props.device) {
      await updateDevice(props.device.id, payload);
      ElMessage.success('已保存');
    } else {
      await createDevice(payload);
      ElMessage.success('已创建');
    }
    drawerVisible.value = false;
    emit('saved');
  } finally {
    saving.value = false;
  }
}

function buildPayload(): DevicePayload {
  return {
    sn: form.sn.trim(),
    name: form.name.trim() || undefined,
    type: form.type,
    merchantId: form.merchantId || null,
    maker: form.maker || null,
    position: form.position.trim() || undefined,
    lng: form.locationType === 1 ? form.lng : null,
    lat: form.locationType === 1 ? form.lat : null,
    locationType: form.locationType,
    buildingId: form.locationType === 2 ? form.buildingId : null,
    floorId: form.locationType === 2 ? form.floorId : null,
    floorX: form.locationType === 2 ? form.floorX : null,
    floorY: form.locationType === 2 ? form.floorY : null,
    sim: form.sim.trim() || undefined,
    cardNo: form.cardNo.trim() || undefined,
    activeDate: form.activeDate,
    installDate: form.installDate,
    status: form.status,
    fExt: buildFExt(),
  };
}
</script>

<style scoped>
.device-form {
  padding-bottom: 24px;
}

.device-form :deep(.el-form-item__label) {
  white-space: nowrap;
}

.form-section {
  margin: 8px 0 16px;
  padding-left: 8px;
  border-left: 3px solid var(--fm-primary);
  color: var(--fm-text);
  font-size: 14px;
  font-weight: 600;
}

.coord-row {
  display: flex;
  align-items: center;
  gap: var(--fm-space-2);
  width: 100%;
}

.coord-row .el-input-number {
  width: 138px;
}

.floor-tip {
  padding-left: 112px;
}

.field-tip {
  padding-left: 112px;
}
</style>
