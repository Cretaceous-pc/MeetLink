/**
 * 站点配置
 */

export default {
  // 品牌文案
  brand: {
    name: 'MeetLink',
    fullName: 'MeetLink',
    slogan: '在线聊天室',
    login: {
      title: 'MeetLink在线聊天',
      welcome: '欢迎使用MeetLink',
      personalInfoTitle: '填写个人信息',
    },
  },

  // API配置 (从环境变量读取)
  api: {
    httpUrl: import.meta.env.VITE_HTTP_URL || '',
    wsUrl: import.meta.env.VITE_WS_URL || '',
  },
}