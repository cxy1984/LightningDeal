import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    redirect: '/activity',
    children: [
      {
        path: 'activity',
        name: 'ActivityList',
        component: () => import('@/views/ActivityList.vue'),
        meta: { title: '秒杀活动' }
      },
      {
        path: 'activity/detail/:id',
        name: 'ActivityDetail',
        component: () => import('@/views/ActivityDetail.vue'),
        meta: { title: '活动详情' }
      },
      {
        path: 'orders',
        name: 'MyOrders',
        component: () => import('@/views/MyOrders.vue'),
        meta: { title: '我的订单', requireAuth: true }
      },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '实时大屏' }
      },
      {
        path: 'admin/activity',
        name: 'ActivityManage',
        component: () => import('@/views/admin/ActivityManage.vue'),
        meta: { title: '活动管理', requireAuth: true }
      },
      {
        path: 'admin/activity/create',
        name: 'ActivityCreate',
        component: () => import('@/views/admin/ActivityForm.vue'),
        meta: { title: '创建活动', requireAuth: true }
      },
      {
        path: 'admin/activity/edit/:id',
        name: 'ActivityEdit',
        component: () => import('@/views/admin/ActivityForm.vue'),
        meta: { title: '编辑活动', requireAuth: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  document.title = `${to.meta.title} | LightningDeal 秒杀系统`
  const token = localStorage.getItem('token')
  if (to.meta.requireAuth && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
