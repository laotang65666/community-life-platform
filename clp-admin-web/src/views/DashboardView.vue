<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import type { ECharts, EChartsOption } from 'echarts'
import { getDashboard } from '../api/admin'
import type { DashboardSummary } from '../api/types'

const loading = ref(false)
const data = ref<DashboardSummary | null>(null)
const orderChartRef = ref<HTMLDivElement | null>(null)
const gmvChartRef = ref<HTMLDivElement | null>(null)

let orderChart: ECharts | null = null
let gmvChart: ECharts | null = null

const summary = computed<DashboardSummary>(() => data.value ?? {
  userCount: 0,
  merchantCount: 0,
  shopCount: 0,
  todayOrders: 0,
  todayGmv: 0,
  pendingMerchantAudits: 0,
  last7Days: []
})

function money(cents: number): string {
  return '¥ ' + (cents / 100).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}

function number(value: number): string {
  return value.toLocaleString('zh-CN')
}

function renderCharts(): void {
  const current = data.value
  if (!current) {
    return
  }

  const trend = current.last7Days ?? []
  const dates = trend.map((item) => item.date?.slice(5) ?? '')
  const orderCounts = trend.map((item) => item.orderCount)
  const paidCounts = trend.map((item) => item.paidCount)
  const gmvValues = trend.map((item) => Number(item.gmvYuan || 0))

  if (orderChartRef.value) {
    orderChart = orderChart ?? echarts.init(orderChartRef.value)
    const orderOption: EChartsOption = {
      tooltip: { trigger: 'axis' },
      legend: { data: ['下单', '支付'], top: 0 },
      grid: { left: 40, right: 16, top: 40, bottom: 28 },
      xAxis: { type: 'category', data: dates, axisLine: { lineStyle: { color: '#e5e7eb' } } },
      yAxis: { type: 'value', splitLine: { lineStyle: { color: '#f1f5f9' } } },
      series: [
        { name: '下单', type: 'bar', data: orderCounts, barMaxWidth: 18, itemStyle: { color: '#2563eb', borderRadius: [4, 4, 0, 0] } },
        { name: '支付', type: 'bar', data: paidCounts, barMaxWidth: 18, itemStyle: { color: '#10b981', borderRadius: [4, 4, 0, 0] } }
      ]
    }
    orderChart.setOption(orderOption)
  }

  if (gmvChartRef.value) {
    gmvChart = gmvChart ?? echarts.init(gmvChartRef.value)
    const gmvOption: EChartsOption = {
      tooltip: { trigger: 'axis', valueFormatter: (value) => `¥ ${Number(value).toFixed(2)}` },
      grid: { left: 52, right: 16, top: 30, bottom: 28 },
      xAxis: { type: 'category', data: dates, boundaryGap: false, axisLine: { lineStyle: { color: '#e5e7eb' } } },
      yAxis: { type: 'value', splitLine: { lineStyle: { color: '#f1f5f9' } } },
      series: [{
        name: '成交额',
        type: 'line',
        smooth: true,
        data: gmvValues,
        symbolSize: 7,
        lineStyle: { color: '#2563eb', width: 2.5 },
        itemStyle: { color: '#2563eb' },
        areaStyle: { color: 'rgba(37, 99, 235, 0.10)' }
      }]
    }
    gmvChart.setOption(gmvOption)
  }
}

function resize(): void {
  orderChart?.resize()
  gmvChart?.resize()
}

async function load(): Promise<void> {
  loading.value = true
  try {
    data.value = await getDashboard()
    renderCharts()
  } catch {
    // 错误提示由 axios 拦截器统一处理
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void load()
  window.addEventListener('resize', resize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  orderChart?.dispose()
  gmvChart?.dispose()
})
</script>

<template>
  <div v-loading="loading" class="page">
    <div class="toolbar">
      <span class="muted">数据来自订单事件与实时订单表,趋势最多延迟数秒。</span>
      <el-button size="small" @click="load">刷新</el-button>
    </div>

    <div class="kpi-grid">
      <div class="card kpi accent">
        <div class="label">今日订单</div>
        <div class="value">{{ number(summary.todayOrders) }}</div>
        <div class="hint">当天创建的全部订单</div>
      </div>
      <div class="card kpi accent">
        <div class="label">今日成交额</div>
        <div class="value">{{ money(summary.todayGmv) }}</div>
        <div class="hint">已支付及之后状态</div>
      </div>
      <div class="card kpi warn">
        <div class="label">待审核商户</div>
        <div class="value">{{ number(summary.pendingMerchantAudits) }}</div>
        <div class="hint">需要管理员处理</div>
      </div>
      <div class="card kpi">
        <div class="label">用户数</div>
        <div class="value">{{ number(summary.userCount) }}</div>
        <div class="hint">平台注册用户</div>
      </div>
      <div class="card kpi">
        <div class="label">商户数</div>
        <div class="value">{{ number(summary.merchantCount) }}</div>
        <div class="hint">审核通过</div>
      </div>
      <div class="card kpi">
        <div class="label">店铺数</div>
        <div class="value">{{ number(summary.shopCount) }}</div>
        <div class="hint">未删除店铺</div>
      </div>
    </div>

    <div class="chart-grid">
      <div class="card">
        <div class="card-title">近 7 天订单</div>
        <div class="card-sub">下单数 / 支付数</div>
        <div ref="orderChartRef" class="chart"></div>
      </div>
      <div class="card">
        <div class="card-title">近 7 天成交额</div>
        <div class="card-sub">单位:元</div>
        <div ref="gmvChartRef" class="chart"></div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: 16px;
  margin-bottom: 18px;
}

.kpi .label {
  font-size: 13px;
  color: var(--text-muted);
  margin-bottom: 8px;
}

.kpi .value {
  font-size: 30px;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.kpi .hint {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 6px;
}

.kpi.accent .value {
  color: var(--primary);
}

.kpi.warn .value {
  color: #f59e0b;
}

.chart-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.card-title {
  font-size: 15px;
  font-weight: 700;
}

.card-sub {
  font-size: 12px;
  color: var(--text-muted);
  margin: 4px 0 10px;
}

.chart {
  width: 100%;
  height: 260px;
}

@media (max-width: 900px) {
  .chart-grid {
    grid-template-columns: 1fr;
  }
}
</style>
