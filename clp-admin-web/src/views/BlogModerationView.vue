<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { hideBlog, listBlogs } from '../api/admin'
import type { BlogItem } from '../api/types'

const loading = ref(false)
const rows = ref<BlogItem[]>([])
const status = ref('')
const page = ref(1)
const size = ref(10)
const total = ref(0)

async function load(pageNumber: number = page.value): Promise<void> {
  page.value = pageNumber
  loading.value = true
  try {
    const data = await listBlogs({
      status: status.value || undefined,
      page: page.value,
      size: size.value
    })
    rows.value = data.list
    total.value = data.total
  } catch {
    // 错误提示由 axios 拦截器统一处理
  } finally {
    loading.value = false
  }
}

function formatTime(value: string): string {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}

async function hide(row: BlogItem): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定隐藏「${row.title}」吗?`, '提示', {
      type: 'warning',
      confirmButtonText: '隐藏',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  try {
    await hideBlog(row.id)
    ElMessage.success('已隐藏')
    await load()
  } catch {
    // 错误提示由 axios 拦截器统一处理
  }
}

onMounted(() => {
  void load(1)
})
</script>

<template>
  <div class="page">
    <div class="card toolbar">
      <el-select
        v-model="status"
        placeholder="内容状态"
        clearable
        style="width: 160px"
        @change="load(1)"
      >
        <el-option label="已发布" value="PUBLISHED" />
        <el-option label="已隐藏" value="HIDDEN" />
      </el-select>
      <el-button type="primary" @click="load(1)">刷新</el-button>
      <span class="muted">隐藏后用户端不再可见,管理员仍可在这里查到。</span>
    </div>

    <div class="card">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column label="封面" width="88">
          <template #default="{ row }">
            <el-image
              v-if="row.images && row.images.length"
              :src="row.images[0]"
              fit="cover"
              class="thumb"
            />
            <span v-else class="muted">无图</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="authorNickname" label="作者" width="130" />
        <el-table-column prop="likedCount" label="点赞" width="80" align="center" />
        <el-table-column prop="commentCount" label="评论" width="80" align="center" />
        <el-table-column label="发布时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" @click="hide(row)">隐藏</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="load"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.thumb {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  display: block;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
