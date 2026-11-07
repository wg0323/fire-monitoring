<template>
  <el-dialog v-model="dialogVisible" title="批量导入设备" width="720px" destroy-on-close>
    <div class="import-steps">
      <div class="step">
        <span class="step-index">1</span>
        <span>下载模板并填写设备信息</span>
        <el-button link type="primary" @click="downloadTemplate">下载设备导入模板</el-button>
      </div>
      <div class="step">
        <span class="step-index">2</span>
        <span>选择填写好的 Excel 文件并导入，成功行直接入库，失败行会在下方列出原因</span>
      </div>
    </div>

    <el-upload
      ref="uploadRef"
      drag
      :auto-upload="false"
      :limit="1"
      accept=".xlsx,.xls"
      :file-list="fileList"
      :on-change="handleFileChange"
      :on-remove="handleRemove"
      :on-exceed="handleExceed"
    >
      <div class="upload-text">将 Excel 文件拖到此处，或点击选择</div>
      <div class="form-tip">仅支持 xlsx / xls，单次最多 2000 行，文件不超过 5MB</div>
    </el-upload>

    <div class="form-tip import-hint">
      设备类型、制造商、商户、楼宇、楼层按名称匹配；定位方式填“未设置 / 室外GPS / 室内楼层图”。
    </div>

    <div class="import-actions">
      <el-button type="primary" :loading="importing" :disabled="!file" @click="submit">
        开始导入
      </el-button>
    </div>

    <div v-if="result" class="import-result">
      <div class="result-summary">
        共 {{ result.total }} 条，成功
        <span class="success-count">{{ result.success }}</span> 条，失败
        <span class="failed-count">{{ result.failed }}</span> 条
      </div>
      <el-table
        v-if="result.errors.length"
        :data="result.errors"
        size="small"
        max-height="240"
        empty-text="无失败行"
      >
        <el-table-column prop="rowNum" label="行号" width="70" />
        <el-table-column prop="sn" label="设备编号" width="160" />
        <el-table-column prop="message" label="失败原因" min-width="240" />
      </el-table>
      <div v-if="result.errors.length >= 100" class="form-tip">失败明细仅展示前 100 条。</div>
    </div>

    <template #footer>
      <el-button @click="dialogVisible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import type { UploadFile, UploadInstance, UploadUserFile } from 'element-plus';
import { downloadDeviceTemplate, importDevices } from '@/api/device';
import type { DeviceImportResult } from '@/types/api';

const MAX_FILE_SIZE = 5 * 1024 * 1024;

const props = defineProps<{ visible: boolean }>();

const emit = defineEmits<{
  (event: 'update:visible', value: boolean): void;
  (event: 'imported'): void;
}>();

const uploadRef = ref<UploadInstance>();
const fileList = ref<UploadUserFile[]>([]);
const file = ref<File | null>(null);
const importing = ref(false);
const result = ref<DeviceImportResult | null>(null);

const dialogVisible = computed({
  get: () => props.visible,
  set: (value: boolean) => emit('update:visible', value),
});

watch(
  () => props.visible,
  (visible) => {
    if (!visible) {
      reset();
    }
  },
);

function reset(): void {
  file.value = null;
  fileList.value = [];
  result.value = null;
  importing.value = false;
  uploadRef.value?.clearFiles();
}

async function downloadTemplate(): Promise<void> {
  try {
    await downloadDeviceTemplate();
  } catch (error) {
    // download 可能对 JSON 错误体 reject，此处兜底记录，避免事件处理器产生未处理的 rejection
    console.error('[device-import] 下载模板失败', error);
  }
}

function handleFileChange(uploadFile: UploadFile): void {
  result.value = null;
  if (!uploadFile.raw) {
    return;
  }
  if (uploadFile.raw.size > MAX_FILE_SIZE) {
    ElMessage.warning('文件不能超过 5MB');
    uploadRef.value?.clearFiles();
    file.value = null;
    fileList.value = [];
    return;
  }
  file.value = uploadFile.raw;
  fileList.value = [{ name: uploadFile.name, uid: uploadFile.uid }];
}

function handleRemove(): void {
  file.value = null;
  fileList.value = [];
  result.value = null;
}

function handleExceed(): void {
  ElMessage.warning('一次只能选择一个文件，请先移除已选文件');
}

async function submit(): Promise<void> {
  if (!file.value) {
    ElMessage.warning('请选择导入文件');
    return;
  }
  importing.value = true;
  try {
    result.value = await importDevices(file.value);
    if (result.value.success > 0) {
      emit('imported');
    }
    if (result.value.failed === 0) {
      ElMessage.success(`导入成功 ${result.value.success} 条`);
    } else {
      ElMessage.warning(`导入完成：成功 ${result.value.success} 条，失败 ${result.value.failed} 条`);
    }
  } finally {
    importing.value = false;
  }
}
</script>

<style scoped>
.import-steps {
  display: flex;
  flex-direction: column;
  gap: var(--fm-space-2);
  margin-bottom: var(--fm-space-4);
}

.step {
  display: flex;
  align-items: center;
  gap: var(--fm-space-2);
  color: var(--fm-text-secondary);
  font-size: 13px;
}

.step-index {
  display: inline-grid;
  place-items: center;
  width: 20px;
  height: 20px;
  color: #fff;
  background: var(--fm-primary);
  border-radius: 50%;
  font-size: 12px;
}

.upload-text {
  color: var(--fm-text-secondary);
  font-size: 14px;
}

.import-hint {
  margin-top: var(--fm-space-2);
}

.import-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--fm-space-3);
}

.import-result {
  margin-top: var(--fm-space-4);
  padding-top: var(--fm-space-3);
  border-top: 1px solid var(--fm-border);
}

.result-summary {
  margin-bottom: var(--fm-space-2);
  font-size: 14px;
}

.success-count {
  color: var(--fm-success);
  font-weight: 600;
}

.failed-count {
  color: var(--fm-danger);
  font-weight: 600;
}
</style>
