export interface LoginData {
  accessToken: string
  refreshToken: string
  expiresIn: number
  role: string
}

export interface PageResult<T> {
  list: T[]
  page: number
  size: number
  total: number
  pages: number
}

export interface DailyTrend {
  date: string
  orderCount: number
  paidCount: number
  completedCount: number
  cancelCount: number
  closeCount: number
  gmvYuan: string
}

export interface DashboardSummary {
  userCount: number
  merchantCount: number
  shopCount: number
  todayOrders: number
  todayGmv: number
  pendingMerchantAudits: number
  last7Days: DailyTrend[]
}

export interface MerchantItem {
  id: string
  userId: string
  shopName: string
  licenseNo: string
  address: string
  contactName: string
  contactPhone: string
  auditStatus: 'PENDING' | 'APPROVED' | 'REJECTED' | string
  auditRemark: string
  createdAt: string
}

export interface BlogItem {
  id: string
  userId: string
  authorNickname: string
  authorAvatar: string
  title: string
  images: string[]
  likedCount: number
  commentCount: number
  createdAt: string
}

export interface ShopType {
  id: string
  name: string
  icon: string
  sort: number
}

export interface IngestKnowledgePayload {
  docId: string
  scope: 'USER' | 'MERCHANT' | 'ALL'
  title: string
  content: string
}

export interface KnowledgeIngestResult {
  docId: string
  totalChunks: number
  insertedChunks: number
  deletedChunks: number
}
