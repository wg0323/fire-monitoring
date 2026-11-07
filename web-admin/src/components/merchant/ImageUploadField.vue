<template>
  <div class="image-field">
    <el-image
      v-if="modelValue"
      :src="modelValue"
      fit="cover"
      class="image-field__preview"
      :preview-src-list="[modelValue]"
      alt="已上传图片"
    />
    <el-upload
      v-else
      class="image-field__upload"
      :show-file-list="false"
      accept="image/jpeg,image/png"
      :http-request="handleUpload"
    >
      <div class="image-field__placeholder">
        <span>{{ uploading ? '上传中…' : '上传图片' }}</span>
      </div>
    </el-upload>
    <button
      v-if="modelValue"
      type="button"
      class="image-field__remove"
      @click="clear"
    >
      移除
    </button>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import type { UploadRequestOptions } from 'element-plus';
import { uploadImage } from '@/api/upload';

defineProps<{
  modelValue?: string;
}>();

const emit = defineEmits<{
  (event: 'update:modelValue', value: string): void;
}>();

const uploading = ref(false);

async function handleUpload(options: UploadRequestOptions): Promise<void> {
  const file = options.file;
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件');
    return;
  }
  uploading.value = true;
  try {
    const result = await uploadImage(file);
    emit('update:modelValue', result.url);
  } finally {
    uploading.value = false;
  }
}

function clear(): void {
  emit('update:modelValue', '');
}
</script>

<style scoped>
.image-field {
  display: flex;
  align-items: center;
  gap: var(--fm-space-2);
}

.image-field__preview {
  width: 96px;
  height: 72px;
  border: 1px solid var(--fm-border);
  border-radius: var(--fm-radius-sm);
}

.image-field__upload {
  width: 96px;
  height: 72px;
}

.image-field__placeholder {
  display: grid;
  place-items: center;
  width: 96px;
  height: 72px;
  border: 1px dashed var(--fm-border);
  border-radius: var(--fm-radius-sm);
  background: #f9fafb;
  color: var(--fm-text-muted);
  font-size: 12px;
}

.image-field__remove {
  padding: 0;
  border: none;
  background: transparent;
  color: var(--fm-danger);
  font-size: 12px;
  cursor: pointer;
}
</style>
