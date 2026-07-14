import { createRouter, createWebHistory } from 'vue-router'
import Chat from '@/views/ChatPage.vue'
import Login from '@/views/LoginPage.vue'
import RoomSelect from '@/views/RoomSelectPage.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: Login,
    },
    {
      path: '/rooms',
      name: 'rooms',
      component: RoomSelect,
    },
    {
      path: '/chat/:roomId',
      name: 'chat',
      component: Chat,
    },
    {
      path: '/',
      redirect: '/rooms',
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/rooms',
    },
  ],
})

router.beforeEach((to, from, next) => {
  let token = window.localStorage.getItem('x-token')
  // 无 token 且不在登录页 → 去登录
  if (!token && to.path !== '/login') {
    next({ path: '/login' })
    return
  }
  // 有 token 但去登录页 → 去房间选择
  if (token && to.path === '/login') {
    next({ path: '/rooms' })
    return
  }
  next()
})

export default router
