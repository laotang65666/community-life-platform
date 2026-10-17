import { request } from './http'
import type {
  BlogItem,
  DashboardSummary,
  IngestKnowledgePayload,
  KnowledgeIngestResult,
  MerchantItem,
  PageResult,
  ShopType
} from './types'

export function getDashboard(): Promise<DashboardSummary> {
  return request<DashboardSummary>({
    method: 'GET',
    url: '/admin/dashboard/summary'
  })
}

export function listMerchants(params: {
  status?: string
  page: number
  size: number
}): Promise<PageResult<MerchantItem>> {
  return request<PageResult<MerchantItem>>({
    method: 'GET',
    url: '/admin/merchants',
    params
  })
}

export function approveMerchant(merchantId: string, typeId: string): Promise<null> {
  return request<null>({
    method: 'POST',
    url: `/admin/merchants/${merchantId}/approve`,
    data: { typeId }
  })
}

export function rejectMerchant(merchantId: string, reason: string): Promise<null> {
  return request<null>({
    method: 'POST',
    url: `/admin/merchants/${merchantId}/reject`,
    data: { reason }
  })
}

export function listBlogs(params: {
  status?: string
  page: number
  size: number
}): Promise<PageResult<BlogItem>> {
  return request<PageResult<BlogItem>>({
    method: 'GET',
    url: '/admin/blog-posts',
    params
  })
}

export function hideBlog(postId: string): Promise<null> {
  return request<null>({
    method: 'POST',
    url: `/admin/blog-posts/${postId}/hide`
  })
}

export function listShopTypes(): Promise<ShopType[]> {
  return request<ShopType[]>({
    method: 'GET',
    url: '/shop-types'
  })
}

export function ingestKnowledge(
  payload: IngestKnowledgePayload
): Promise<KnowledgeIngestResult> {
  return request<KnowledgeIngestResult>({
    method: 'POST',
    url: '/admin/ai/knowledge',
    data: payload
  })
}

export function deleteKnowledge(docId: string): Promise<number> {
  return request<number>({
    method: 'DELETE',
    url: `/admin/ai/knowledge/${docId}`
  })
}
