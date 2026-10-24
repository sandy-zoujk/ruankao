<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { dashboardApi } from '../api'

const loading = ref(true)
const data = ref({ totalPlans: 0, completedPlans: 0, completionRate: 0, wrongQuestions: 0, unmasteredQuestions: 0, weekPlans: [], mastery: [] })
const chartRef = ref()
let chart

async function load() {
  loading.value = true
  try {
    data.value = (await dashboardApi.get()).data
    await nextTick()
    renderChart()
  } finally { loading.value = false }
}

function renderChart() {
  if (!chartRef.value || !data.value.mastery.length) return
  chart?.dispose()
  chart = echarts.init(chartRef.value)
  const items = data.value.mastery.slice(0, 10).reverse()
  chart.setOption({
    grid: { top: 10, right: 28, bottom: 25, left: 95 },
    xAxis: { type: 'value', max: 100, splitLine: { lineStyle: { color: '#edf1ef' } } },
    yAxis: { type: 'category', data: items.map(item => item.name), axisLabel: { width: 82, overflow: 'truncate' } },
    tooltip: { trigger: 'axis', formatter: params => `${params[0].name}：${params[0].value}%` },
    series: [{ type: 'bar', data: items.map(item => item.mastery), barWidth: 12, itemStyle: { color: '#2f887c', borderRadius: 8 } }]
  })
}

function resize() { chart?.resize() }
onMounted(() => { load(); window.addEventListener('resize', resize) })
onBeforeUnmount(() => { window.removeEventListener('resize', resize); chart?.dispose() })
</script>

<template>
  <div v-loading="loading">
    <div class="page-actions"><div><strong>今天也向证书迈进一步</strong><p>根据完成情况及时调整计划，重点突破高频章节。</p></div><el-button type="primary" @click="$router.push('/plans')">安排学习</el-button></div>
    <section class="stat-grid">
      <article class="panel stat-card"><span>计划完成率</span><strong>{{ data.completionRate }}%</strong><small>{{ data.completedPlans }} / {{ data.totalPlans }} 项</small></article>
      <article class="panel stat-card"><span>累计学习计划</span><strong>{{ data.totalPlans }}</strong><small>拆成小目标更容易坚持</small></article>
      <article class="panel stat-card"><span>错题总数</span><strong>{{ data.wrongQuestions }}</strong><small>记录原因比记录答案更重要</small></article>
      <article class="panel stat-card"><span>待掌握错题</span><strong>{{ data.unmasteredQuestions }}</strong><small>建议本周完成复盘</small></article>
    </section>
    <section class="dashboard-grid">
      <article class="panel chart-panel">
        <div class="panel-title"><h3>重点章节掌握度</h3><el-tag effect="plain">Top 10</el-tag></div>
        <div v-if="data.mastery.length" ref="chartRef" class="chart"></div><div v-else class="empty">运行数据库初始化脚本后显示章节数据</div>
      </article>
      <article class="panel list-panel">
        <div class="panel-title"><h3>未来 7 天计划</h3><el-link type="primary" @click="$router.push('/plans')">查看全部</el-link></div>
        <el-timeline v-if="data.weekPlans.length">
          <el-timeline-item v-for="plan in data.weekPlans" :key="plan.id" :timestamp="plan.planDate" :type="plan.completed ? 'success' : 'primary'">
            <strong>{{ plan.title }}</strong><div class="muted">{{ plan.category }} · {{ plan.durationMinutes }} 分钟</div>
          </el-timeline-item>
        </el-timeline><div v-else class="empty">未来一周还没有计划</div>
      </article>
    </section>
  </div>
</template>

