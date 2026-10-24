<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { practiceApi } from "../api";

const chapters = ref([]),
  history = ref([]),
  stats = reactive({
    attemptCount: 0,
    questionCount: 0,
    correctCount: 0,
    accuracy: 0,
  });
const setup = reactive({ chapter: "", questionCount: 10 }),
  attempt = ref(null),
  answers = reactive({}),
  result = ref(null),
  loading = ref(false),
  tab = ref("practice");
const available = computed(() =>
  setup.chapter
    ? chapters.value.find((x) => x.chapter === setup.chapter)?.questionCount ||
      0
    : chapters.value.reduce((n, x) => n + x.questionCount, 0),
);
async function loadSummary() {
  const [c, h, s] = await Promise.all([
    practiceApi.chapters(),
    practiceApi.history(),
    practiceApi.stats(),
  ]);
  chapters.value = c.data;
  history.value = h.data;
  Object.assign(stats, s.data);
}
async function start() {
  loading.value = true;
  try {
    const r = await practiceApi.create(setup);
    attempt.value = r.data;
    result.value = null;
    Object.keys(answers).forEach((k) => delete answers[k]);
  } finally {
    loading.value = false;
  }
}
async function submit() {
  await ElMessageBox.confirm(
    `已作答 ${Object.keys(answers).length}/${attempt.value.questionCount} 题，确定交卷吗？`,
    "确认交卷",
    { type: "warning" },
  );
  loading.value = true;
  try {
    const list = attempt.value.questions.map((q) => ({
      questionId: q.id,
      selectedOption: answers[q.id] || null,
    }));
    const r = await practiceApi.submit(attempt.value.id, list);
    result.value = r.data;
    attempt.value = null;
    ElMessage.success("交卷成功，错题已自动加入复盘");
    await loadSummary();
  } finally {
    loading.value = false;
  }
}
async function review(row) {
  const r = await practiceApi.detail(row.id);
  result.value = r.data;
  attempt.value = null;
  tab.value = "practice";
}
function reset() {
  attempt.value = null;
  result.value = null;
}
onMounted(loadSummary);
</script>

<template>
  <el-tabs v-model="tab">
    <el-tab-pane label="开始练习" name="practice">
      <section v-if="!attempt && !result" class="panel practice-setup">
        <h2>章节随机练习</h2>
        <p>选择章节与题量，交卷后统一查看得分和解析。</p>
        <el-form label-position="top" style="max-width: 520px">
          <el-form-item label="章节"
            ><el-select
              v-model="setup.chapter"
              clearable
              placeholder="全部章节"
              style="width: 100%"
              ><el-option
                v-for="c in chapters"
                :key="c.chapter"
                :label="`${c.chapter}（${c.questionCount}题）`"
                :value="c.chapter" /></el-select
          ></el-form-item>
          <el-form-item label="题量"
            ><el-input-number
              v-model="setup.questionCount"
              :min="1"
              :max="100"
            /><span class="form-tip"
              >当前可用 {{ available }} 题</span
            ></el-form-item
          >
          <el-button
            type="primary"
            :loading="loading"
            :disabled="available < setup.questionCount"
            @click="start"
            >开始练习</el-button
          >
        </el-form>
      </section>
      <div v-else-if="attempt" v-loading="loading">
        <div class="page-actions">
          <div>
            <strong>本次共 {{ attempt.questionCount }} 题</strong>
            <p>完成后统一交卷，未作答按错误处理。</p>
          </div>
          <el-button type="primary" @click="submit">交卷</el-button>
        </div>
        <article
          v-for="(q, i) in attempt.questions"
          :key="q.id"
          class="panel practice-question"
        >
          <h3>{{ i + 1 }}. {{ q.content }}</h3>
          <el-radio-group v-model="answers[q.id]" class="option-list"
            ><el-radio
              v-for="key in ['A', 'B', 'C', 'D']"
              :key="key"
              :value="key"
              ><b>{{ key }}.</b> {{ q["option" + key] }}</el-radio
            ></el-radio-group
          >
        </article>
      </div>
      <div v-else-if="result">
        <section class="panel result-summary">
          <strong>{{ result.score }} 分</strong
          ><span
            >答对 {{ result.correctCount }}/{{ result.questionCount }} 题</span
          ><el-button @click="reset">再练一次</el-button>
        </section>
        <article
          v-for="(q, i) in result.questions"
          :key="q.id"
          class="panel practice-question"
          :class="q.correct ? 'answer-correct' : 'answer-wrong'"
        >
          <h3>{{ i + 1 }}. {{ q.content }}</h3>
          <p>
            你的答案：{{ q.selectedOption || "未作答" }}　正确答案：{{
              q.correctOption
            }}
          </p>
          <p class="explanation">
            <strong>解析：</strong>{{ q.explanation || "暂无解析" }}
          </p>
        </article>
      </div>
    </el-tab-pane>
    <el-tab-pane label="成绩记录" name="history">
      <div class="stats-grid">
        <article class="panel stat-card">
          <span>练习次数</span><strong>{{ stats.attemptCount }}</strong>
        </article>
        <article class="panel stat-card">
          <span>累计答题</span><strong>{{ stats.questionCount }}</strong>
        </article>
        <article class="panel stat-card">
          <span>累计正确率</span><strong>{{ stats.accuracy }}%</strong>
        </article>
      </div>
      <section class="panel">
        <el-table :data="history"
          ><el-table-column
            prop="submittedAt"
            label="交卷时间"
            min-width="180"
          /><el-table-column prop="chapter" label="章节"
            ><template #default="{ row }">{{
              row.chapter || "全部章节"
            }}</template></el-table-column
          ><el-table-column label="成绩"
            ><template #default="{ row }"
              >{{ row.score }} 分（{{ row.correctCount }}/{{
                row.questionCount
              }}）</template
            ></el-table-column
          ><el-table-column width="100"
            ><template #default="{ row }"
              ><el-button link type="primary" @click="review(row)"
                >查看解析</el-button
              ></template
            ></el-table-column
          ></el-table
        >
      </section>
    </el-tab-pane>
  </el-tabs>
</template>
