<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1 class="page-title">{{ fireOnly ? '火灾跟踪' : '火警跟踪' }}</h1>
        <p class="page-subtitle">
          {{ fireOnly ? '只展示核警结果为真实火警的报警记录。' : '展示全部设备报警，含火警与故障。' }}
        </p>
      </div>
      <el-tag effect="plain">{{ total }} 条</el-tag>
    </div>

    <div class="filter-bar">
      <el-select v-model="query.merchantId" placeholder="商户" clearable filterable style="width: 200px">
        <el-option v-for="merchant in merchants" :key="merchant.id" :label="merchant.name" :value="merchant.id" />
      </el-select>
      <el-input v-model="query.keyword" placeholder="设备编号 / 位置" clearable style="width: 200px" @keyup.enter="search" />
      <el-select v-model="query.level" placeholder="级别" clearable style="width: 120px">
        <el-option label="火警" :value="1" />
        <el-option label="故障" :value="3" />
      </el-select>
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        value-format="YYYY-MM-DD"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        style="width: 240px"
      />
      <div class="filter-actions">
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="rows" border row-key="id">
      <el-table-column prop="sn" label="设备编号" min-width="140" />
      <el-table-column prop="deviceTypeName" label="设备类型" width="110">
        <template #default="{ row }">{{ row.deviceTypeName || row.deviceType || '-' }}</template>
      </el-table-column>
      <el-table-column label="报警类型" width="100">
        <template #default="{ row }">{{ alarmLevelLabel(row.alarmLevel) }}</template>
      </el-table-column>
      <el-table-column prop="merchantName" label="商户" min-width="150">
        <template #default="{ row }">{{ row.merchantName || '-' }}</template>
      </el-table-column>
      <el-table-column v-if="!fireOnly" label="派单来源" width="120">
        <template #default="{ row }">{{ row.workAreaName || '-' }}</template>
      </el-table-column>
      <el-table-column label="上报时间" width="165">
        <template #default="{ row }">{{ formatDateTime(row.alarmTime) }}</template>
      </el-table-column>
      <el-table-column label="抓拍图" width="90">
        <template #default="{ row }">
          <el-image
            v-if="row.snapshotUrl"
            :src="row.snapshotUrl"
            :preview-src-list="[row.snapshotUrl]"
            fit="cover"
            class="snapshot"
            alt="报警抓拍图"
          />
          <span v-else class="muted">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="position" label="位置" min-width="150">
        <template #default="{ row }">{{ row.position || '-' }}</template>
      </el-table-column>
      <el-table-column label="结果" width="110">
        <template #default="{ row }">{{ alarmResultLabel(row.result) }}</template>
      </el-table-column>
      <el-table-column label="处置状态" width="110">
        <template #default="{ row }">
          <el-tag v-if="row.status === 1" type="success" effect="plain">已处理</el-tag>
          <el-tag v-else type="warning" effect="plain">未处理</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">查看</el-button>
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

    <AlarmDetailDrawer v-model="drawerVisible" :alarm-id="currentId" readonly @refresh="load" />
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import AlarmDetailDrawer from '@/components/alarm/AlarmDetailDrawer.vue';
import { listAlarmMerchants, pageAlarms, type AlarmQuery } from '@/api/alarm';
import type { AlarmItem, AlarmMerchantItem } from '@/types/api';
import { alarmLevelLabel, alarmResultLabel, formatDateTime } from '@/utils/format';

const props = withDefaults(defineProps<{ fireOnly?: boolean }>(), { fireOnly: false });

const loading = ref(false);
const rows = ref<AlarmItem[]>([]);
const merchants = ref<AlarmMerchantItem[]>([]);
const total = ref(0);
const dateRange = ref<[string, string] | null>(null);
const drawerVisible = ref(false);
const currentId = ref('');

const query = reactive<AlarmQuery>({
  pageNum: 1,
  pageSize: 20,
  merchantId: undefined,
  keyword: '',
  level: undefined,
  result: props.fireOnly ? 1 : undefined,
});

async function load(): Promise<void> {
  loading.value = true;
  try {
    const page = await pageAlarms({
      ...query,
      startTime: dateRange.value ? `${dateRange.value[0]} 00:00:00` : undefined,
      endTime: dateRange.value ? `${dateRange.value[1]} 23:59:59` : undefined,
    });
    rows.value = page.records;
    total.value = page.total;
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
  query.merchantId = undefined;
  query.keyword = '';
  query.level = undefined;
  dateRange.value = null;
  search();
}

function openDetail(row: AlarmItem): void {
  currentId.value = row.id;
  drawerVisible.value = true;
}

onMounted(async () => {
  const merchantList = await listAlarmMerchants();
  merchants.value = merchantList;
  await load();
});
</script>

<style scoped>
.snapshot {
  width: 48px;
  height: 36px;
  border-radius: var(--fm-radius-sm);
}
</style>
