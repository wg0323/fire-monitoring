<template>
  <el-dialog
    :model-value="modelValue"
    title="电话告警"
    width="560px"
    destroy-on-close
    @update:model-value="close"
  >
    <div v-if="alarm" class="phone-alert">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="联系业主或第一责任人核实火警。外呼暂不可用，可登记模拟通话结果完成流程测试。"
      />
      <div class="phone-alert__alarm">
        <span>{{ alarm.sn }}</span>
        <span class="muted">{{ alarm.position || alarm.floorName || alarm.merchantName || '-' }}</span>
      </div>

      <h3>选择联系人</h3>
      <el-radio-group v-model="selectedPhone" class="phone-alert__contacts">
        <el-radio v-for="contact in contacts" :key="contact.phone" :value="contact.phone">
          {{ contact.relation }} · {{ contact.name }} · {{ contact.phone }}
        </el-radio>
        <el-radio value="manual">手工输入号码</el-radio>
      </el-radio-group>
      <p v-if="contacts.length === 0" class="form-tip">商户档案未维护联系人电话，请手工输入；无号码可直接派单核警。</p>
      <el-input
        v-if="selectedPhone === 'manual'"
        v-model="manualPhone"
        placeholder="请输入业主或第一责任人电话"
        clearable
        class="phone-alert__manual"
      />

      <div class="phone-alert__dial">
        <el-button type="primary" :loading="dialing" @click="dial">发起外呼</el-button>
        <span v-if="dialResult" class="muted">{{ dialResult }}</span>
      </div>

      <h3>通话结果登记（Mock）</h3>
      <el-input v-model="remark" placeholder="通话备注（选填）" maxlength="255" show-word-limit />
      <p class="form-tip">登记结果会写入报警追踪表：已接通请填写核警处置，未接通将进入派单核警。</p>
      <div class="phone-alert__actions">
        <el-button :loading="recording" @click="record(false)">未接通，派单核警</el-button>
        <el-button type="primary" :loading="recording" @click="record(true)">已接通，填写核警处置</el-button>
      </div>
    </div>
    <template #footer>
      <el-button @click="close(false)">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { callAlarm, dialOut } from '@/api/alarm';
import type { AlarmContact, AlarmItem } from '@/types/api';

const props = defineProps<{ modelValue: boolean; alarm: AlarmItem | null }>();
const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void;
  (event: 'connected'): void;
  (event: 'disconnected'): void;
  (event: 'success'): void;
}>();

const contacts = computed<AlarmContact[]>(() => props.alarm?.contacts ?? []);
const selectedPhone = ref('');
const manualPhone = ref('');
const remark = ref('');
const actionId = ref('');
const dialing = ref(false);
const recording = ref(false);
const dialResult = ref('');

watch(() => props.modelValue, (visible) => {
  if (!visible) {
    return;
  }
  selectedPhone.value = contacts.value[0]?.phone ?? 'manual';
  manualPhone.value = '';
  remark.value = '';
  actionId.value = '';
  dialResult.value = '';
}, { immediate: true });

const currentPhone = computed(() => selectedPhone.value === 'manual'
  ? manualPhone.value.trim() : selectedPhone.value);
const currentContactLabel = computed(() => {
  if (selectedPhone.value === 'manual') {
    return '手工输入号码';
  }
  const contact = contacts.value.find((item) => item.phone === selectedPhone.value);
  return contact ? `${contact.relation} ${contact.name}` : '';
});

function close(value: boolean): void {
  emit('update:modelValue', value);
}

async function dial(): Promise<void> {
  if (!props.alarm || !currentPhone.value) {
    ElMessage.warning('请选择或输入被叫号码');
    return;
  }
  dialing.value = true;
  try {
    const result = await dialOut({ alarmId: props.alarm.id, phone: currentPhone.value, type: 1 });
    actionId.value = result.actionId || '';
    dialResult.value = result.succeed
      ? `外呼已发起：${result.message || currentPhone.value}`
      : `外呼未接通：${result.message || '未开通外呼'}，可登记模拟结果`;
  } catch {
    actionId.value = '';
    dialResult.value = '外呼暂不可用，可直接登记模拟通话结果';
  } finally {
    dialing.value = false;
  }
}

async function record(connected: boolean): Promise<void> {
  if (!props.alarm) {
    return;
  }
  if (!currentPhone.value) {
    if (connected) {
      ElMessage.warning('请选择或输入被叫号码');
      return;
    }
    ElMessage.info('未获取到联系电话，直接派单核警');
    emit('disconnected');
    close(false);
    return;
  }
  recording.value = true;
  try {
    await callAlarm(props.alarm.id, {
      calledNo: currentPhone.value,
      callType: 1,
      connected,
      callState: connected ? 'dealing' : 'notDeal',
      contactName: currentContactLabel.value,
      actionId: actionId.value || undefined,
      remark: remark.value || undefined,
    });
    ElMessage.success(connected ? '已接通，请填写核警处置' : '未接通，请派单核警');
    emit('success');
    if (connected) {
      emit('connected');
    } else {
      emit('disconnected');
    }
    close(false);
  } finally {
    recording.value = false;
  }
}
</script>

<style scoped>
.phone-alert__alarm {
  display: flex;
  align-items: center;
  gap: var(--fm-space-3);
  margin: var(--fm-space-4) 0;
  font-size: 13px;
}

.phone-alert h3 {
  margin: var(--fm-space-4) 0 var(--fm-space-2);
  font-size: 14px;
  font-weight: 600;
}

.phone-alert__contacts {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--fm-space-2);
}

.phone-alert__manual {
  margin-top: var(--fm-space-3);
}

.phone-alert__dial {
  display: flex;
  align-items: center;
  gap: var(--fm-space-3);
  margin-top: var(--fm-space-4);
  font-size: 12px;
}

.phone-alert__actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--fm-space-2);
  margin-top: var(--fm-space-4);
}
</style>
