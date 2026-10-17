<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  approveMerchant,
  listMerchants,
  listShopTypes,
  rejectMerchant
} from '../api/admin'
import type { MerchantItem, ShopType } from '../api/types'

const loading = ref(false)
const submitting = ref(false)
const rows = ref<MerchantItem[]>([])
const status = ref('')
const page = ref(1)
const size = ref(10)
const total = ref(0)

const shopTypes = ref<ShopType[]>([])
const approveVisible = ref(false)
const rejectVisible = ref(false)
const selectedTypeId = ref('')
const rejectReason = ref('')
const currentRow = ref<MerchantItem | null>(null)

async function load(pageNumber: number = page.value): Promise<void> {
  page.value = pageNumber
  loading.value = true
  try {
    const data = await listMerchants({
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

function statusText(value: string): string {
  if (value === 'PENDING') return '待审核'
  if (value === 'APPROVED') return '已通过'
  if (value === 'REJECTED') return '已驳回'
  return value
}

function tagType(value: string): 'success' | 'warning' | 'danger' | 'info' {
  if (value === 'APPROVED') return 'success'
  if (value === 'REJECTED') return 'danger'
  return 'warning'
}

function formatTime(value: string): string {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}

async function openApprove(row: MerchantItem): Promise<void> {
  currentRow.value = row
  selectedTypeId.value = ''
  approveVisible.value = true
  if (shopTypes.value.length === 0) {
    try {
      shopTypes.value = await listShopTypes()
    } catch {
      // 错误提示由 axios 拦截器统一处理
    }
  }
}

function openReject(row: MerchantItem): void {
  currentRow.value = row
  rejectReason.value = ''
  rejectVisible.value = true
}

async function submitApprove(): Promise<void> {
  if (!currentRow.value) return
  if (!selectedTypeId.value) {
    ElMessage.warning('请选择店铺分类')
    return
  }

  submitting.value = true
  try {
    await approveMerchant(currentRow.value.id, selectedTypeId.value)
    ElMessage.success('已通过入驻申请')
    approveVisible.value = false
    await load()
  } catch {
    // 错误提示由 axios 拦截器统一处理
  } finally {
    submitting.value = false
  }
}

async function submitReject(): Promise<void> {
  if (!currentRow.value) return
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请填写驳回原因')
    return
  }

  submitting.value = true
  try {
    await rejectMerchant(currentRow.value.id, rejectReason.value.trim())
    ElMessage.success('已驳回入驻申请')
    rejectVisible.value = false
    await load()
  } catch {
    // 错误提示由 axios 拦截器统一处理
  } finally {
    submitting.value = false
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
        placeholder="审核状态"
        clearable
        style="width: 160px"
        @change="load(1)"
      >
        <el-option label="待审核" value="PENDING" />
        <el-option label="已通过" value="APPROVED" />
        <el-option label="已驳回" value="REJECTED" />
      </el-select>
      <el-button type="primary" @click="load(1)">刷新</el-button>
    </div>

    <div class="card">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="shopName" label="店铺名称" min-width="150" />
        <el-table-column prop="contactName" label="联系人" width="100" />
        <el-table-column prop="contactPhone" label="联系电话" width="130" />
        <el-table-column prop="licenseNo" label="营业执照" min-width="160" />
        <el-table-column prop="address" label="地址" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="tagType(row.auditStatus)">{{ statusText(row.auditStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <template v-if="row.auditStatus === 'PENDING'">
              <el-button link type="primary" @click="openApprove(row)">通过</el-button>
              <el-button link type="danger" @click="openReject(row)">驳回</el-button>
            </template>
            <span v-else class="muted">已处理</span>
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

    <el-dialog v-model="approveVisible" title="通过入驻申请" width="420px">
      <el-form label-width="90px">
        <el-form-item label="店铺分类">
          <el-select v-model="selectedTypeId" placeholder="请选择分类" style="width: 100%">
            <el-option
              v-for="type in shopTypes"
              :key="type.id"
              :label="type.name"
              :value="type.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitApprove">
          确认通过
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rejectVisible" title="驳回入驻申请" width="460px">
      <el-input
        v-model="rejectReason"
        type="textarea"
        :rows="3"
        maxlength="500"
        show-word-limit
        placeholder="请填写驳回原因"
      />
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" :loading="submitting" @click="submitReject">
          确认驳回
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
