<template>
  <el-dialog
    :model-value="modelValue"
    title="核警处置"
    width="520px"
    destroy-on-close
    @update:model-value="close"
  >
    <div v-if="alarm" class="verify-dialog">
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        title="按电话核实结果填写：确认火警或误报。提交后写入报警追踪表并收口该报警。"
      />
      <div class="verify-dialog__alarm">
        <span>{{ alarm.sn }}</span>
        <span class="muted">{{ alarm.merchantName || '-' }}</span>
      </div>
      <el-form label-width="88px">
        <el-form-item label="核警结论">
          <el-radio-group v-model="form.result">
            <el-radio :value="1">确认火警</el-radio>
            <el-radio :value="0">误报</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="核警说明">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="3"
            maxlength="255"
            show-word-limit
            placeholder="如：电话联系业主确认为烹饪油烟误报"
          />
        </el-form-item>
      </el-form>
      <p class="form-tip">结论将以「核警」类型记录到报警追踪表。</p>
    </div>
    <template #footer>
      <el-button @click="close(false)">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">提交核警处置</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { verifyAlarm } from '@/api/alarm';
import type { AlarmItem } from '@/types/api';

const props = defineProps<{ modelValue: boolean; alarm: AlarmItem | null }>();
const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void;
  (event: 'success'): void;
}>();

const submitting = ref(false);
const form = reactive({ result: 1, remark: '' });

watch(() => props.modelValue, (visible) => {
  if (visible) {
    form.result = 1;
    form.remark = '';
  }
}, { immediate: true });

function close(value: boolean): void {
  emit('update:modelValue', value);
}

async function submit(): Promise<void> {
  if (!props.alarm) {
    return;
  }
  submitting.value = true;
  try {
    await verifyAlarm({
      id: props.alarm.id,
      result: form.result,
      remark: form.remark || undefined,
    });
    ElMessage.success('核警处置已提交并写入报警追踪表');
    emit('success');
    close(false);
  } finally {
    submitting.value = false;
  }
}
</script>

<style scoped>
.verify-dialog__alarm {
  display: flex;
  align-items: center;
  gap: var(--fm-space-3);
  margin: var(--fm-space-4) 0;
  font-size: 13px;
}
</style>
