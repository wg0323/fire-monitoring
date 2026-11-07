<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1 class="page-title">通话分析</h1>
        <p class="page-subtitle">按日期段查询外呼录音记录，录音地址可直接下载。</p>
      </div>
    </div>

    <div class="filter-bar">
      <el-date-picker
        v-model="range"
        type="daterange"
        value-format="YYYY-MM-DD"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        style="width: 260px"
      />
      <div class="filter-actions">
        <el-button type="primary" :loading="loading" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="rows" border row-key="id">
      <el-table-column label="商户名称" min-width="150">
        <template #default="{ row }">{{ row.merchantName || '-' }}</template>
      </el-table-column>
      <el-table-column label="报警类型" width="110">
        <template #default="{ row }">{{ row.alarmTypeName || '-' }}</template>
      </el-table-column>
      <el-table-column label="报警时间" width="165">
        <template #default="{ row }">{{ formatDateTime(row.alarmTime) }}</template>
      </el-table-column>
      <el-table-column label="核警状态" width="110">
        <template #default="{ row }">{{ verifyStatusLabel(row.verifyStatus) }}</template>
      </el-table-column>
      <el-table-column label="拨号时间" width="165">
        <template #default="{ row }">{{ formatDateTime(row.callBegin) }}</template>
      </el-table-column>
      <el-table-column label="拨号电话" width="140">
        <template #default="{ row }">{{ row.calledNo || '-' }}</template>
      </el-table-column>
      <el-table-column label="录音" width="90">
        <template #default="{ row }">
          <a v-if="safeUrl(row.fileUrl)" :href="safeUrl(row.fileUrl)" target="_blank" rel="noopener noreferrer">下载</a>
          <span v-else class="muted">-</span>
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
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import dayjs from 'dayjs';
import { getStatCall } from '@/api/stat';
import type { StatCallItem } from '@/types/api';
import { formatDateTime, verifyStatusLabel } from '@/utils/format';
import { safeUrl } from '@/utils/url';

const loading = ref(false);
const rows = ref<StatCallItem[]>([]);
const total = ref(0);
const range = ref<[string, string] | null>(defaultRange());
const query = reactive({ pageNum: 1, pageSize: 20 });

function defaultRange(): [string, string] {
  return [dayjs().subtract(6, 'day').format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')];
}

async function load(): Promise<void> {
  const [start, end] = range.value ?? defaultRange();
  loading.value = true;
  try {
    const result = await getStatCall({
      startTime: `${start} 00:00:00`,
      endTime: `${end} 23:59:59`,
      pageNum: query.pageNum,
      pageSize: query.pageSize,
    });
    rows.value = result.page.records;
    total.value = result.page.total;
  } catch (error) {
    // 失败已由请求拦截器提示，此处仅记录，避免 onMounted/事件处理器产生未处理的 rejection
    console.error('[stat/call] 加载失败', error);
  } finally {
    loading.value = false;
  }
}

function search(): void {
  query.pageNum = 1;
  load();
}

function reset(): void {
  range.value = defaultRange();
  search();
}

function handleSizeChange(): void {
  query.pageNum = 1;
  load();
}

onMounted(load);
</script>
