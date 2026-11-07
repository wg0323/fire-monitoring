<template>
  <el-drawer
    v-model="drawerVisible"
    :title="merchant ? '编辑商户' : '新增商户'"
    size="640px"
    destroy-on-close
  >
    <el-form label-width="96px" class="merchant-form">
      <div class="form-section">基本信息</div>
      <el-form-item label="商户名称" required>
        <el-input v-model="form.name" maxlength="64" placeholder="请输入商户名称" />
      </el-form-item>
      <el-form-item label="商户类型" required>
        <el-select v-model="form.type" placeholder="请选择商户类型" style="width: 100%">
          <el-option
            v-for="item in activeTypes"
            :key="item.code"
            :label="item.name"
            :value="item.code"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="场所性质" required>
        <el-radio-group v-model="form.merchantKind">
          <el-radio :value="1">九小场所</el-radio>
          <el-radio :value="2">社会单位</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="状态">
        <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        <span class="form-tip">禁用后小程序不可见</span>
      </el-form-item>

      <div class="form-section">位置</div>
      <el-form-item label="地址" required>
        <el-input v-model="form.addr" maxlength="255" placeholder="请输入详细地址" />
      </el-form-item>
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

      <div class="form-section">联系与档案</div>
      <el-form-item label="联系电话" required>
        <el-input v-model="form.phone" maxlength="24" placeholder="用于电话核警" />
      </el-form-item>
      <el-form-item label="业主名称">
        <el-input v-model="form.ownerName" maxlength="64" />
      </el-form-item>
      <el-form-item label="建筑面积">
        <el-input v-model="form.floorage" maxlength="32" placeholder="单位 m²" />
      </el-form-item>
      <el-form-item label="员工人数">
        <el-input-number v-model="form.employeeNum" :min="0" :max="99999" controls-position="right" />
      </el-form-item>
      <el-form-item label="消防设施">
        <el-input v-model="form.fireRemark" type="textarea" :rows="2" maxlength="255" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="512" />
      </el-form-item>

      <div class="form-section">证照与照片</div>
      <el-form-item label="营业执照">
        <ImageUploadField v-model="form.businessLicense" />
      </el-form-item>
      <el-form-item label="场所照片">
        <div class="photo-row">
          <ImageUploadField v-model="form.placePic1" />
          <ImageUploadField v-model="form.placePic2" />
          <ImageUploadField v-model="form.placePic3" />
        </div>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="drawerVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
    </template>
  </el-drawer>

  <el-dialog v-model="mapVisible" title="地图选点" width="880px" append-to-body>
    <FmMap
      pick-mode
      :points="[]"
      :pick-value="pickValue"
      height="460px"
      @pick="handlePick"
    />
    <div class="form-tip">点击地图选择位置，选点后自动回填地址，可再手动修改。</div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { createMerchant, updateMerchant } from '@/api/merchant';
import type { DictItem, MerchantItem, MerchantPayload } from '@/types/api';
import type { MapPick } from '@/components/map/types';
import FmMap from '@/components/map/FmMap.vue';
import ImageUploadField from '@/components/merchant/ImageUploadField.vue';

interface MerchantFormState extends Omit<MerchantPayload, 'lng' | 'lat'> {
  lng: number | null;
  lat: number | null;
}

const props = defineProps<{
  visible: boolean;
  merchant: MerchantItem | null;
  merchantTypes: DictItem[];
}>();

const emit = defineEmits<{
  (event: 'update:visible', value: boolean): void;
  (event: 'saved'): void;
}>();

const PHONE_PATTERN = /^[0-9+\-\s]{6,24}$/;

const saving = ref(false);
const mapVisible = ref(false);
const form = reactive<MerchantFormState>(emptyForm());

const drawerVisible = computed({
  get: () => props.visible,
  set: (value: boolean) => emit('update:visible', value),
});

const activeTypes = computed(() => props.merchantTypes.filter((item) => item.status === 1));

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

function emptyForm(): MerchantFormState {
  return {
    name: '',
    type: '',
    merchantKind: 1,
    lng: null,
    lat: null,
    addr: '',
    ownerName: '',
    phone: '',
    floorage: '',
    employeeNum: null,
    fireRemark: '',
    remark: '',
    businessLicense: '',
    placePic1: '',
    placePic2: '',
    placePic3: '',
    status: 1,
  };
}

function fillForm(): void {
  const merchant = props.merchant;
  if (!merchant) {
    Object.assign(form, emptyForm());
    return;
  }
  Object.assign(form, {
    name: merchant.name,
    type: merchant.type ?? '',
    merchantKind: merchant.merchantKind ?? 1,
    lng: merchant.lng ?? null,
    lat: merchant.lat ?? null,
    addr: merchant.addr ?? '',
    ownerName: merchant.ownerName ?? '',
    phone: merchant.phone ?? '',
    floorage: merchant.floorage ?? '',
    employeeNum: merchant.employeeNum ?? null,
    fireRemark: merchant.fireRemark ?? '',
    remark: merchant.remark ?? '',
    businessLicense: merchant.businessLicense ?? '',
    placePic1: merchant.placePic1 ?? '',
    placePic2: merchant.placePic2 ?? '',
    placePic3: merchant.placePic3 ?? '',
    status: merchant.status,
  });
}

function handlePick(value: MapPick): void {
  form.lng = Number(value.lng.toFixed(6));
  form.lat = Number(value.lat.toFixed(6));
  if (value.address) {
    form.addr = value.address;
  }
}

function validate(): boolean {
  if (!form.name.trim()) {
    ElMessage.warning('请输入商户名称');
    return false;
  }
  if (form.name.trim().length > 64) {
    ElMessage.warning('商户名称不能超过 64 字');
    return false;
  }
  if (!form.type) {
    ElMessage.warning('请选择商户类型');
    return false;
  }
  if (form.merchantKind !== 1 && form.merchantKind !== 2) {
    ElMessage.warning('请选择场所性质');
    return false;
  }
  if (!form.phone.trim() || !PHONE_PATTERN.test(form.phone.trim())) {
    ElMessage.warning('联系电话格式不正确');
    return false;
  }
  if (!form.addr.trim()) {
    ElMessage.warning('请输入地址');
    return false;
  }
  if (form.lng === null || form.lat === null) {
    ElMessage.warning('请选择商户位置');
    return false;
  }
  if (form.lng < -180 || form.lng > 180 || form.lat < -90 || form.lat > 90) {
    ElMessage.warning('经纬度超出合法范围');
    return false;
  }
  return true;
}

async function submit(): Promise<void> {
  if (!validate()) {
    return;
  }
  const payload = buildPayload();
  saving.value = true;
  try {
    if (props.merchant) {
      await updateMerchant(props.merchant.id, payload);
      ElMessage.success('已保存');
    } else {
      await createMerchant(payload);
      ElMessage.success('已创建');
    }
    drawerVisible.value = false;
    emit('saved');
  } finally {
    saving.value = false;
  }
}

function buildPayload(): MerchantPayload {
  return {
    name: form.name.trim(),
    type: form.type,
    merchantKind: form.merchantKind,
    lng: Number((form.lng as number).toFixed(6)),
    lat: Number((form.lat as number).toFixed(6)),
    addr: form.addr.trim(),
    ownerName: form.ownerName?.trim(),
    phone: form.phone.trim(),
    floorage: form.floorage?.trim(),
    employeeNum: form.employeeNum ?? null,
    fireRemark: form.fireRemark?.trim(),
    remark: form.remark?.trim(),
    businessLicense: form.businessLicense,
    placePic1: form.placePic1,
    placePic2: form.placePic2,
    placePic3: form.placePic3,
    status: form.status,
  };
}
</script>

<style scoped>
.merchant-form {
  padding-bottom: 24px;
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
  width: 150px;
}

.photo-row {
  display: flex;
  flex-wrap: wrap;
  gap: var(--fm-space-3);
}

.form-tip {
  margin-top: var(--fm-space-1);
  color: var(--fm-text-muted);
  font-size: 12px;
}
</style>
