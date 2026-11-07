<template>
  <section v-loading="loading" class="page-card">
    <div class="page-header">
      <div>
        <h1 class="page-title">外呼配置</h1>
        <p class="page-subtitle">配置当前运营中心的七陌外呼账号；密钥不回显，保存时需重新填写。</p>
      </div>
      <el-tag :type="config.configured ? 'success' : 'info'" effect="plain">
        {{ config.configured ? '已配置' : '未配置' }}
      </el-tag>
    </div>

    <el-alert
      v-if="config.mockMode"
      type="warning"
      :closable="false"
      show-icon
      title="当前为模拟外呼模式（FM_CALL_MOCK=true），不会真正拨打电话。"
      description="外呼接口直接返回成功并写入通话记录，便于无话机联调；关闭该环境变量并重启后恢复真实外呼。"
    />

    <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" class="call-form">
      <el-form-item label="账号 ID" prop="accountId">
        <el-input v-model="form.accountId" maxlength="64" placeholder="七陌账号 ID" />
      </el-form-item>
      <el-form-item label="接口密钥" prop="apiSecret">
        <el-input
          v-model="form.apiSecret"
          type="password"
          show-password
          maxlength="128"
          placeholder="密钥不回显，保存时请重新填写"
        />
      </el-form-item>
      <el-form-item label="接口地址" prop="host">
        <el-input v-model="form.host" maxlength="255" placeholder="如 https://api.7moor.com" />
      </el-form-item>
      <el-form-item label="外呼分机号" prop="fromExten">
        <el-input v-model="form.fromExten" maxlength="32" placeholder="选填，默认 8000" />
      </el-form-item>
    </el-form>

    <div class="form-actions">
      <el-button type="primary" :loading="saving" @click="submit">保存配置</el-button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import {
  getCallConfig,
  saveCallConfig,
  type CallConfig,
} from '@/api/callConfig';

interface CallConfigForm {
  accountId: string;
  apiSecret: string;
  host: string;
  fromExten: string;
}

const formRef = ref<FormInstance | null>(null);
const loading = ref(false);
const saving = ref(false);
const config = ref<CallConfig>({ configured: false });
const form = reactive<CallConfigForm>({
  accountId: '',
  apiSecret: '',
  host: '',
  fromExten: '',
});

const rules: FormRules<CallConfigForm> = {
  accountId: [{ required: true, message: '请输入账号 ID', trigger: 'blur' }],
  apiSecret: [{ required: true, message: '请输入接口密钥', trigger: 'blur' }],
  host: [{ required: true, message: '请输入接口地址', trigger: 'blur' }],
};

onMounted(load);

async function load(): Promise<void> {
  loading.value = true;
  try {
    config.value = await getCallConfig();
    form.accountId = config.value.accountId ?? '';
    form.host = config.value.host ?? '';
    form.fromExten = config.value.fromExten ?? '';
    form.apiSecret = '';
  } catch (error) {
    // 失败已由请求拦截器提示，此处仅记录，避免 onMounted/事件处理器产生未处理的 rejection
    console.error('[service/call-config] 加载失败', error);
  } finally {
    loading.value = false;
  }
}

async function submit(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }
  saving.value = true;
  try {
    await saveCallConfig({
      accountId: form.accountId.trim(),
      apiSecret: form.apiSecret.trim(),
      host: form.host.trim(),
      fromExten: form.fromExten.trim() || undefined,
    });
    ElMessage.success('外呼配置已保存');
    await load();
  } finally {
    saving.value = false;
  }
}
</script>

<style scoped>
.call-form {
  max-width: 560px;
  margin-top: var(--fm-space-5);
}

.form-actions {
  margin-top: var(--fm-space-5);
}
</style>
