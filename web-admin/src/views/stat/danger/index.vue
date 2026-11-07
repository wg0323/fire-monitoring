<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1 class="page-title">隐患分析</h1>
        <p class="page-subtitle">按日期段统计隐患完成率与逾期率，并按隐患类型拆解。</p>
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
        <el-button type="primary" :loading="loading" @click="load">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
    </div>

    <div class="kpi-grid">
      <StatKpiCard label="隐患总数" :value="stat.total ?? 0" />
      <StatKpiCard label="已整改" :value="stat.done ?? 0" tone="success" />
      <StatKpiCard label="未整改" :value="stat.undone ?? 0" tone="warning" />
      <StatKpiCard label="逾期隐患" :value="stat.overdue ?? 0" tone="danger" />
      <StatKpiCard label="完成率" :value="stat.doneRate ?? 0" suffix="%" tone="success" />
      <StatKpiCard label="逾期率" :value="stat.overdueRate ?? 0" suffix="%" tone="danger" />
    </div>

    <el-table v-loading="loading" :data="stat.rows" border :row-key="dangerRowKey">
      <el-table-column label="隐患类型" min-width="160">
        <template #default="{ row }">{{ row.dangerTypeName || '-' }}</template>
      </el-table-column>
      <el-table-column prop="total" label="隐患总数" width="110" />
      <el-table-column prop="done" label="已整改" width="100" />
      <el-table-column prop="undone" label="未整改" width="100" />
      <el-table-column prop="overdue" label="逾期" width="90" />
      <el-table-column label="完成率" width="110">
        <template #default="{ row }">{{ row.doneRate ?? 0 }}%</template>
      </el-table-column>
      <el-table-column label="逾期率" width="110">
        <template #default="{ row }">{{ row.overdueRate ?? 0 }}%</template>
      </el-table-column>
    </el-table>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import dayjs from 'dayjs';
import { getStatDanger } from '@/api/stat';
import type { StatDanger, StatDangerType } from '@/types/api';
import StatKpiCard from '../components/StatKpiCard.vue';

// dangerType 可能为空，直接作 row-key 会产生重复 key；为空时按行对象分配稳定兜底键
const dangerKeys = new WeakMap<StatDangerType, string>();
let dangerKeySeq = 0;
function dangerRowKey(row: StatDangerType): string {
  if (row.dangerType) {
    return row.dangerType;
  }
  let key = dangerKeys.get(row);
  if (!key) {
    dangerKeySeq += 1;
    key = `danger-${dangerKeySeq}`;
    dangerKeys.set(row, key);
  }
  return key;
}

const loading = ref(false);
const range = ref<[string, string] | null>(defaultRange());
const stat = ref<StatDanger>({
  total: 0,
  done: 0,
  undone: 0,
  overdue: 0,
  doneRate: 0,
  overdueRate: 0,
  rows: [],
});

function defaultRange(): [string, string] {
  return [dayjs().subtract(6, 'day').format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')];
}

async function load(): Promise<void> {
  const [start, end] = range.value ?? defaultRange();
  loading.value = true;
  try {
    stat.value = await getStatDanger({
      startTime: `${start} 00:00:00`,
      endTime: `${end} 23:59:59`,
    });
  } catch (error) {
    // 失败已由请求拦截器提示，此处仅记录，避免 onMounted/事件处理器产生未处理的 rejection
    console.error('[stat/danger] 加载失败', error);
  } finally {
    loading.value = false;
  }
}

function reset(): void {
  range.value = defaultRange();
  load();
}

onMounted(load);
</script>

<style scoped>
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: var(--fm-space-4);
  margin-bottom: var(--fm-space-4);
}
</style>
