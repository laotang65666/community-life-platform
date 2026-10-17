<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteKnowledge, ingestKnowledge } from '../api/admin'
import type { IngestKnowledgePayload, KnowledgeIngestResult } from '../api/types'

const submitting = ref(false)
const deleting = ref(false)
const result = ref<KnowledgeIngestResult | null>(null)
const deleteDocId = ref('')

const form = reactive<IngestKnowledgePayload>({
  docId: '',
  scope: 'USER',
  title: '',
  content: ''
})

async function submit(): Promise<void> {
  if (!form.docId.trim() || !form.title.trim() || !form.content.trim()) {
    ElMessage.warning('docId、标题和正文都不能为空')
    return
  }

  submitting.value = true
  try {
    result.value = await ingestKnowledge({
      docId: form.docId.trim(),
      scope: form.scope,
      title: form.title.trim(),
      content: form.content
    })
    ElMessage.success('知识已导入')
  } catch {
    // 错误提示由 axios 拦截器统一处理
  } finally {
    submitting.value = false
  }
}

async function remove(): Promise<void> {
  if (!deleteDocId.value.trim()) {
    ElMessage.warning('请输入要删除的 docId')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定删除知识文档「${deleteDocId.value.trim()}」吗?`,
      '提示',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }

  deleting.value = true
  try {
    const count = await deleteKnowledge(deleteDocId.value.trim())
    ElMessage.success(`已删除 ${count} 个分片`)
    deleteDocId.value = ''
  } catch {
    // 错误提示由 axios 拦截器统一处理
  } finally {
    deleting.value = false
  }
}
</script>

<template>
  <div class="page">
    <div class="card">
      <h3>导入 / 更新知识</h3>
      <p class="muted">
        正文支持 Markdown,后端会按标题和段落切分,并按内容哈希增量向量化。
        相同 docId 重复导入只会补齐变化的分片。
      </p>

      <el-form label-width="90px" class="knowledge-form">
        <el-form-item label="文档 ID">
          <el-input v-model="form.docId" placeholder="例如 user-order-v2" />
        </el-form-item>
        <el-form-item label="适用端">
          <el-select v-model="form.scope" style="width: 220px">
            <el-option label="用户端" value="USER" />
            <el-option label="商户端" value="MERCHANT" />
            <el-option label="两端通用" value="ALL" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="例如 下单与订单规则" />
        </el-form-item>
        <el-form-item label="正文">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="10"
            placeholder="## 章节标题&#10;正文内容……"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="submit">
            导入知识
          </el-button>
        </el-form-item>
      </el-form>

      <el-alert
        v-if="result"
        type="success"
        show-icon
        :closable="false"
        :title="`${result.docId}:共 ${result.totalChunks} 个分片,新增 ${result.insertedChunks} 个,删除 ${result.deletedChunks} 个`"
      />
    </div>

    <div class="card">
      <h3>删除知识文档</h3>
      <p class="muted">按 docId 删除该文档的全部知识分片。</p>
      <div class="delete-row">
        <el-input v-model="deleteDocId" placeholder="输入 docId" style="max-width: 320px" />
        <el-button type="danger" :loading="deleting" @click="remove">删除</el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
h3 {
  margin: 0 0 6px;
  font-size: 16px;
}

.knowledge-form {
  margin-top: 18px;
}

.delete-row {
  display: flex;
  gap: 12px;
  margin-top: 14px;
}
</style>
