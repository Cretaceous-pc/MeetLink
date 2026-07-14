/**
 * Mock 拦截器 — 在 VITE_MOCK=true 时激活
 * 拦截所有后端 API 请求，返回假数据
 */
import axios from 'axios'
import {
  mockUsers,
  mockMessages,
  mockGroup,
  mockPrivateList,
  mockOnlineUsers,
  currentUser,
} from './data.js'

// 判断是否 mock 模式
const isMock = import.meta.env.VITE_MOCK === 'true'
if (!isMock) {
  console.log('[Mock] 未启用，使用真实后端')
} else {
  console.log('[Mock] 🎭 Mock 模式已激活，所有 API 返回假数据')
}

// 辅助：生成模拟 token 和公钥
const FAKE_TOKEN = 'mock-token-meetlink-debug-2024'
const FAKE_PUBLIC_KEY = `-----BEGIN PUBLIC KEY-----
MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC0mockKeyForDebug2024
-----END PUBLIC KEY-----`

// 辅助：从 data 中找到用户
function findUserById(id) {
  return mockUsers.find((u) => u.id === id) || null
}

// 辅助：构建用户列表 Map（key = id, value = user info）
function buildUserListMap() {
  const map = {}
  mockUsers.forEach((u) => {
    map[u.id] = {
      id: u.id,
      name: u.name,
      email: u.email,
      avatar: u.avatar,
      type: u.type,
      badge: u.badge || [],
    }
  })
  return map
}

// 辅助：生成成功响应
function ok(data) {
  return { code: 0, data, msg: 'success' }
}

// 辅助：延迟模拟网络
function delay(ms = 200) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

// ---------- 路由表 ----------
const routes = {
  // Login
  'GET /api/v1/login/public-key': async () => ok(FAKE_PUBLIC_KEY),
  'POST /api/v1/login/verify': async () => {
    return ok(FAKE_TOKEN)
  },
  'POST /api/v1/login': async () => {
    return ok({
      userId: currentUser.id,
      userName: currentUser.name,
      email: currentUser.email,
      avatar: currentUser.avatar,
      token: FAKE_TOKEN,
    })
  },

  // User
  'GET /api/v1/user/list': async () => ok(mockUsers.map((u) => ({ ...u }))),
  'GET /api/v1/user/list/map': async () => ok(buildUserListMap()),
  'GET /api/v1/user/online/web': async () => ok(mockOnlineUsers),
  'POST /api/v1/user/update': async (data) => {
    const user = findUserById(currentUser.id)
    if (user) {
      if (data.name) user.name = data.name
      if (data.avatar !== undefined) user.avatar = data.avatar
    }
    return ok(true)
  },

  // ChatList
  'GET /api/v1/chat-list/group': async () => ok(mockGroup),
  'GET /api/v1/chat-list/list/private': async () => ok(mockPrivateList),
  'POST /api/v1/chat-list/read': async () => ok(true),
  'POST /api/v1/chat-list/create': async () => ok(true),
  'POST /api/v1/chat-list/delete': async () => ok(true),

  // Message
  'POST /api/v1/message/record': async (data) => {
    // cursor 分页
    let list = [...mockMessages]
    if (data.cursorTime && data.cursorId) {
      const cursorDate = new Date(data.cursorTime).getTime()
      list = list.filter((m) => {
        const mt = new Date(m.createTime).getTime()
        return mt < cursorDate || (mt === cursorDate && m.id < data.cursorId)
      })
    }
    const result = list.slice(0, data.num || 20)
    return ok(result)
  },
  'POST /api/v1/message/recall': async () => ok(true),
}

// ---------- 安装 mock 拦截器 ----------
export function setupMock() {
  if (!isMock) return

  // 拦截 axios 请求
  axios.interceptors.request.use(async (config) => {
    const method = config.method.toUpperCase()
    const url = config.url || ''
    // 提取路径（去掉 baseURL）
    const path = url.replace(config.baseURL || '', '').replace(import.meta.env.VITE_HTTP_URL || '', '').split('?')[0]
    const key = `${method} ${path}`

    // 在 mock 模式下跳过 token 检查
    config.headers['x-token'] = FAKE_TOKEN

    const handler = routes[key]
    if (handler) {
      // 解析请求体
      let body = {}
      try {
        body = typeof config.data === 'string' ? JSON.parse(config.data) : config.data || {}
      } catch {
        // ignore
      }
      await delay(150 + Math.random() * 200)
      const result = await handler(body)
      // 构造 axios 响应，让 Http 类的 .then(res => res.data) 正常工作
      config.adapter = () =>
        Promise.resolve({
          data: result,
          status: 200,
          statusText: 'OK',
          headers: {},
          config,
        })
    } else {
      console.warn(`[Mock] 未匹配路由: ${key}`)
    }
    return config
  })
}
