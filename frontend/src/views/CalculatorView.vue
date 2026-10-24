<script setup>
import { reactive, ref } from 'vue'
import { calculatorApi } from '../api'

const form = reactive({ pv: 100, ev: 90, ac: 110, bac: 500 })
const result = ref(null), loading = ref(false)
async function calculate() { loading.value = true; try { result.value = (await calculatorApi.earnedValue(form)).data } finally { loading.value = false } }
</script>

<template>
  <div>
    <div class="page-actions"><div><strong>高频计算题：挣值管理</strong><p>输入计划价值、挣值、实际成本和完工预算，快速核对计算过程。</p></div></div>
    <section class="calculator-layout">
      <article class="panel calculator-card">
        <div class="panel-title"><h3>基础数据</h3></div>
        <el-form label-position="top" class="dialog-form">
          <el-form-item label="PV 计划价值"><el-input-number v-model="form.pv" :min="0" :precision="2" /></el-form-item>
          <el-form-item label="EV 挣值"><el-input-number v-model="form.ev" :min="0" :precision="2" /></el-form-item>
          <el-form-item label="AC 实际成本"><el-input-number v-model="form.ac" :min="0" :precision="2" /></el-form-item>
          <el-form-item label="BAC 完工预算"><el-input-number v-model="form.bac" :min="0" :precision="2" /></el-form-item>
          <el-button type="primary" size="large" :loading="loading" style="width:100%" @click="calculate">开始计算</el-button>
        </el-form>
        <div class="formula">CV = EV − AC　　SV = EV − PV<br>CPI = EV ÷ AC　　SPI = EV ÷ PV<br>EAC = BAC ÷ CPI　　VAC = BAC − EAC</div>
      </article>
      <article class="panel calculator-card">
        <div class="panel-title"><h3>计算结果</h3><el-tag v-if="result" :type="result.cpi >= 1 && result.spi >= 1 ? 'success' : 'warning'">项目状态</el-tag></div>
        <div v-if="result" class="result-grid">
          <div class="result-item"><span>成本偏差 CV</span><strong>{{ result.cv }}</strong><small>{{ result.costStatus }}</small></div>
          <div class="result-item"><span>进度偏差 SV</span><strong>{{ result.sv }}</strong><small>{{ result.scheduleStatus }}</small></div>
          <div class="result-item"><span>成本绩效指数 CPI</span><strong>{{ result.cpi }}</strong><small>{{ result.cpi >= 1 ? '成本效率良好' : '成本效率偏低' }}</small></div>
          <div class="result-item"><span>进度绩效指数 SPI</span><strong>{{ result.spi }}</strong><small>{{ result.spi >= 1 ? '进度符合预期' : '进度需要追赶' }}</small></div>
          <div class="result-item"><span>完工估算 EAC</span><strong>{{ result.eac }}</strong><small>按当前 CPI 预测</small></div>
          <div class="result-item"><span>完工偏差 VAC</span><strong>{{ result.vac }}</strong><small>{{ result.vac >= 0 ? '预计有预算结余' : '预计超过预算' }}</small></div>
        </div><div v-else class="empty">填写左侧数据并开始计算</div>
      </article>
    </section>
  </div>
</template>

