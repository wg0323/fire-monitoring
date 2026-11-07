<template>
  <el-dialog
    :model-value="modelValue"
    title="派单"
    width="520px"
    destroy-on-close
    @update:model-value="close"
  >
    <el-alert type="info" :closable="false" show-icon title="先选工作区，再从工作区成员中选择外勤；商户必须在所选工作区内。" />
    <el-form ref="formRef" :model="form" :rules="rules" label-width="88px" class="dispatch-form">
      <el-form-item label="工作区" prop="workAreaId">
        <el-select v-model="form.workAreaId" placeholder="请选择工作区" style="width: 100%">
          <el-option
            v-for="option in options"
            :key="option.workAreaId"
            :label="`${option.workAreaName}（${option.handlers.length} 人）`"
            :value="option.workAreaId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="外勤" prop="handlerId">
        <el-select
          v-model="form.handlerId"
          placeholder="请选择外勤"
          style="width: 100%"
          :disabled="!form.workAreaId"
        >
          <el-option
            v-for="handler in handlers"
            :key="handler.id"
            :label="handler.phone ? `${handler.name}（${handler.phone}）` : handler.name"
            :value="handler.id"
          />
        </el-select>
        <p v-if="form.workAreaId && handlers.length === 0" class="form-tip">该工作区还没有绑定外勤，请先在工作区管理中绑定用户。</p>
      </el-form-item>
      <el-form-item label="派单说明">
        <el-input v-model="form.dispatchRemark" type="textarea" :rows="3" maxlength="255" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="close(false)">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">确认派单</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { dispatchAlarm, listAlarmDispatchOptions } from '@/api/alarm';
import type { AlarmDispatchOption } from '@/types/api';

const props = withDefaults(
  defineProps<{ modelValue: boolean; alarmId: string; defaultRemark?: string }>(),
  { defaultRemark: '' },
);
const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void;
  (event: 'success'): void;
}>();

interface DispatchForm {
  workAreaId: string;
  handlerId: string;
  dispatchRemark: string;
}

const formRef = ref<FormInstance>();
const submitting = ref(false);
const options = ref<AlarmDispatchOption[]>([]);
const form = reactive<DispatchForm>({ workAreaId: '', handlerId: '', dispatchRemark: '' });
const rules: FormRules<DispatchForm> = {
  workAreaId: [{ required: true, message: '请选择工作区', trigger: 'change' }],
  handlerId: [{ required: true, message: '请选择外勤', trigger: 'change' }],
};

const handlers = computed(() => {
  const option = options.value.find((item) => item.workAreaId === form.workAreaId);
  return option?.handlers ?? [];
});

watch(() => form.workAreaId, () => {
  form.handlerId = '';
});

async function loadOptions(): Promise<void> {
  form.workAreaId = '';
  form.handlerId = '';
  form.dispatchRemark = props.defaultRemark;
  options.value = await listAlarmDispatchOptions();
}

watch(() => props.modelValue, (visible) => {
  if (visible) {
    void loadOptions();
  }
}, { immediate: true });

function close(value: boolean): void {
  emit('update:modelValue', value);
}

async function submit(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }
  submitting.value = true;
  try {
    await dispatchAlarm({
      id: props.alarmId,
      workAreaId: form.workAreaId,
      handlerId: form.handlerId,
      dispatchRemark: form.dispatchRemark || undefined,
    });
    ElMessage.success('已派单并生成工单');
    emit('success');
    close(false);
  } finally {
    submitting.value = false;
  }
}
</script>

<style scoped>
.dispatch-form {
  margin-top: var(--fm-space-4);
}
</style>
