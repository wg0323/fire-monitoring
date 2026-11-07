<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1 class="page-title">工单管理</h1>
        <p class="page-subtitle">工单只由派单生成；外勤在小程序接单并完成，后台只读。</p>
      </div>
      <el-tag effect="plain">{{ total }} 条</el-tag>
    </div>

    <div class="filter-bar">
      <el-input v-model="query.keyword" placeholder="工单号 / 工单标题" clearable style="width: 220px" @keyup.enter="search" />
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 140px">
        <el-option label="待接单" :value="0" />
        <el-option label="处理中" :value="1" />
        <el-option label="已完成" :value="2" />
      </el-select>
      <div class="filter-actions">
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="rows" border row-key="id">
      <el-table-column prop="id" label="工单号" min-width="160" />
      <el-table-column label="来源报警" min-width="130">
        <template #default="{ row }">{{ row.alarmSn || '-' }}</template>
      </el-table-column>
      <el-table-column label="商户" min-width="150">
        <template #default="{ row }">{{ row.merchantName || '-' }}</template>
      </el-table-column>
      <el-table-column label="工作区" width="120">
        <template #default="{ row }">{{ row.workAreaName || '-' }}</template>
      </el-table-column>
      <el-table-column label="处理人" width="110">
        <template #default="{ row }">{{ row.handlerName || '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusTone(row.status)" effect="plain">{{ workOrderStatusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="接单时间" width="165">
        <template #default="{ row }">{{ formatDateTime(row.acceptTime) }}</template>
      </el-table-column>
      <el-table-column label="完成时间" width="165">
        <template #default="{ row }">{{ formatDateTime(row.finishTime) }}</template>
      </el-table-column>
      <el-table-column label="故障结论" min-width="150">
        <template #default="{ row }">
          <span v-if="row.dangerStatus === 'PENDING'">
            待转隐患 {{ remainText(row.dangerRemainSeconds) }}
          </span>
          <span v-else>{{ dangerStatusLabel(row.dangerStatus) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
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

    <el-drawer v-model="drawerVisible" title="工单详情" size="560px" destroy-on-close>
      <div v-loading="detailLoading" class="workorder-detail">
        <template v-if="detail">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="工单号" :span="2">{{ detail.id }}</el-descriptions-item>
            <el-descriptions-item label="商户">{{ detail.merchantName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="工作区">{{ detail.workAreaName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="处理人">{{ detail.handlerName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ workOrderStatusLabel(detail.status) }}</el-descriptions-item>
            <el-descriptions-item label="接单时间">{{ formatDateTime(detail.acceptTime) }}</el-descriptions-item>
            <el-descriptions-item label="完成时间">{{ formatDateTime(detail.finishTime) }}</el-descriptions-item>
            <el-descriptions-item label="来源报警">{{ detail.alarmSn || '-' }}</el-descriptions-item>
            <el-descriptions-item label="报警时间">{{ formatDateTime(detail.alarmTime) }}</el-descriptions-item>
            <el-descriptions-item label="报警结论">{{ alarmResultLabel(detail.alarmResult) }}</el-descriptions-item>
            <el-descriptions-item label="转隐患状态">{{ dangerStatusLabel(detail.dangerStatus) }}</el-descriptions-item>
            <el-descriptions-item label="工单内容" :span="2">{{ detail.content || '-' }}</el-descriptions-item>
            <el-descriptions-item label="完成说明" :span="2">{{ detail.finishRemark || '-' }}</el-descriptions-item>
          </el-descriptions>

          <div v-if="finishPics.length > 0" class="finish-pics">
            <h3>完成照片</h3>
            <el-image
              v-for="pic in finishPics"
              :key="pic"
              :src="pic"
              :preview-src-list="finishPics"
              fit="cover"
              class="finish-pic"
              alt="工单完成照片"
            />
          </div>

          <div class="workorder-timeline">
            <h3>处理时间线</h3>
            <el-timeline>
              <el-timeline-item :timestamp="formatDateTime(detail.createTime)" placement="top">
                派单生成工单
              </el-timeline-item>
              <el-timeline-item :timestamp="formatDateTime(detail.acceptTime)" placement="top">
                外勤接单
              </el-timeline-item>
              <el-timeline-item :timestamp="formatDateTime(detail.finishTime)" placement="top">
                外勤提交结论
              </el-timeline-item>
            </el-timeline>
          </div>
        </template>
        <el-empty v-else-if="!detailLoading" description="工单不存在" />
      </div>
    </el-drawer>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { getWorkOrder, pageWorkOrders, type WorkOrderQuery } from '@/api/workorder';
import type { WorkOrderItem } from '@/types/api';
import {
  alarmResultLabel,
  dangerStatusLabel,
  formatDateTime,
  remainText,
  workOrderStatusLabel,
} from '@/utils/format';

const loading = ref(false);
const detailLoading = ref(false);
const rows = ref<WorkOrderItem[]>([]);
const total = ref(0);
const drawerVisible = ref(false);
const detail = ref<WorkOrderItem | null>(null);

const query = reactive<WorkOrderQuery>({
  pageNum: 1,
  pageSize: 20,
  status: undefined,
  keyword: '',
});

const finishPics = computed(() =>
  [detail.value?.finishPic1, detail.value?.finishPic2, detail.value?.finishPic3]
    .filter((pic): pic is string => Boolean(pic)));

async function load(): Promise<void> {
  loading.value = true;
  try {
    const page = await pageWorkOrders(query);
    rows.value = page.records;
    total.value = page.total;
  } catch (error) {
    // 失败已由请求拦截器提示，此处仅记录，避免 onMounted/事件处理器产生未处理的 rejection
    console.error('[workorder] 加载失败', error);
  } finally {
    loading.value = false;
  }
}

function search(): void {
  query.pageNum = 1;
  load();
}

function handleSizeChange(size: number): void {
  query.pageSize = size;
  query.pageNum = 1;
  load();
}

function resetQuery(): void {
  query.keyword = '';
  query.status = undefined;
  search();
}

function statusTone(status: number): 'warning' | 'primary' | 'success' {
  if (status === 0) {
    return 'warning';
  }
  if (status === 1) {
    return 'primary';
  }
  return 'success';
}

async function openDetail(row: WorkOrderItem): Promise<void> {
  detail.value = row;
  drawerVisible.value = true;
  detailLoading.value = true;
  try {
    detail.value = await getWorkOrder(row.id);
  } finally {
    detailLoading.value = false;
  }
}

onMounted(load);
</script>

<style scoped>
.workorder-detail {
  min-height: 200px;
}

.finish-pics,
.workorder-timeline {
  margin-top: var(--fm-space-5);
}

.finish-pics h3,
.workorder-timeline h3 {
  margin-bottom: var(--fm-space-3);
  color: var(--fm-text);
  font-size: 14px;
  font-weight: 600;
}

.finish-pic {
  width: 112px;
  height: 84px;
  margin-right: var(--fm-space-2);
  border-radius: var(--fm-radius-sm);
}
</style>
