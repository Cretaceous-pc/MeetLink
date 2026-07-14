/**
 * Mock 数据 — 前端独立调试用
 * 包含假用户、假消息、假聊天列表和群聊信息
 */

const now = Date.now()
const MIN = 60 * 1000
const HOUR = 60 * MIN

// ---------- 假用户 ----------
const mockUsers = [
  { id: 'u1', name: 'Alice',   email: 'alice@meetlink.dev',   avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Alice',   type: 'user' },
  { id: 'u2', name: 'Bob',     email: 'bob@meetlink.dev',     avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Bob',     type: 'user' },
  { id: 'u3', name: 'Charlie', email: 'charlie@meetlink.dev', avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Charlie', type: 'user' },
  { id: 'u4', name: 'Diana',   email: 'diana@meetlink.dev',   avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Diana',   type: 'user' },
  { id: 'u5', name: 'Eve',     email: 'eve@meetlink.dev',     avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Eve',     type: 'user' },
  { id: 'u6', name: 'Frank',   email: 'frank@meetlink.dev',   avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Frank',   type: 'user' },
  { id: 'u7', name: 'Grace',   email: 'grace@meetlink.dev',   avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Grace',   type: 'user' },
  { id: 'u8', name: 'Henry',   email: 'henry@meetlink.dev',   avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Henry',   type: 'user' },
  // 机器人
  { id: 'doubao',   name: '豆包',       email: 'doubao@robot.dev',   avatar: '', type: 'bot', badge: [] },
  { id: 'deepseek', name: 'DeepSeek',   email: 'deepseek@robot.dev', avatar: '', type: 'bot', badge: [] },
]

// ---------- 当前登录用户 ----------
const currentUser = mockUsers[0] // Alice

// ---------- 假消息 ----------
const mockMessages = []
const senderIds = ['u1', 'u2', 'u3', 'u4', 'u5', 'u6', 'u7', 'u8', 'doubao', 'deepseek']
const textPool = [
  '大家好啊！👋',
  '今天天气真不错',
  '有人在吗？',
  '我刚写完一个新功能，好有成就感 😄',
  '推荐大家看一部电影《星际穿越》，超好看！',
  '有没有人一起联机打游戏？',
  '周末有什么安排吗？',
  '最近在学 Vue 3，感觉比 Vue 2 强太多了',
  '分享一个实用工具：https://caniuse.com',
  '哈哈哈太好笑了 😂',
  '今天加班到很晚...',
  '早上好！新的一天开始了 ☀️',
  '晚安各位 🌙',
  '明天开会记得带电脑',
  '这个项目的架构设计得很棒 👍',
  '有没有人用 Tailwind CSS？交流一下',
  '下午茶时间到 ☕',
  '刚跑完 5 公里，爽！',
  '谁有好的 VS Code 插件推荐？',
  '这个 bug 终于修好了，花了我两个小时...',
  '周五了！🎉',
  '分享一首好听的歌',
  '新来的同事技术很强啊',
  '这个需求不太合理吧？',
  '好的，我来处理',
  '已部署到测试环境',
  '代码 review 通过了',
  '准备发版了',
  '收到！',
  '没问题 👍',
]

// 生成 30 条时间倒序的假消息（最新 = 1 分钟前，最早 = 3 小时前）
for (let i = 0; i < 30; i++) {
  const fromId = senderIds[i % senderIds.length]
  const user = mockUsers.find((u) => u.id === fromId)
  const createTime = new Date(now - (i + 1) * 6 * MIN) // 每条间隔约 6 分钟
  mockMessages.push({
    id: `msg_${String(i).padStart(4, '0')}`,
    fromId,
    toId: '1',
    source: 'group',
    type: 'text',
    message: JSON.stringify([{ type: 'text', content: textPool[i % textPool.length] }]),
    fromInfo: {
      id: user.id,
      name: user.name,
      email: user.email,
      avatar: user.avatar,
      type: user.type,
      badge: user.badge || [],
    },
    isShowTime: i === 0 || i === 29 || i % 5 === 0,
    createTime: createTime.toISOString(),
    updateTime: createTime.toISOString(),
  })
}

// ---------- 假群聊 ----------
const mockGroup = {
  id: '1',
  name: 'MeetLink在线聊天群',
  avatar: '',
  lastMessage: mockMessages[0],
  unreadCount: 0,
}

// ---------- 假私聊列表 ----------
const mockPrivateList = [
  {
    id: 'cl1',
    userId: 'u1',
    targetId: 'u2',
    targetInfo: mockUsers[1],
    lastMessage: {
      id: 'pmsg_01',
      fromId: 'u2',
      toId: 'u1',
      source: 'user',
      type: 'text',
      message: JSON.stringify([{ type: 'text', content: '在吗？有事找你' }]),
      createTime: new Date(now - 10 * MIN).toISOString(),
    },
    unreadCount: 2,
  },
  {
    id: 'cl2',
    userId: 'u1',
    targetId: 'u3',
    targetInfo: mockUsers[2],
    lastMessage: {
      id: 'pmsg_02',
      fromId: 'u1',
      toId: 'u3',
      source: 'user',
      type: 'text',
      message: JSON.stringify([{ type: 'text', content: '好的，没问题' }]),
      createTime: new Date(now - 2 * HOUR).toISOString(),
    },
    unreadCount: 0,
  },
]

// ---------- 假在线用户 ----------
const mockOnlineUsers = ['Alice', 'Bob', 'Charlie', 'Diana', 'Eve', '豆包', 'DeepSeek']

// ---------- 导出 ----------
export {
  mockUsers,
  mockMessages,
  mockGroup,
  mockPrivateList,
  mockOnlineUsers,
  currentUser,
}
