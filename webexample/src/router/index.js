import { createRouter, createWebHistory } from 'vue-router'
import ServerView from "@/views/ServerView.vue";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: ServerView
    },
    {
      path: '/about',
      name: 'about',
      //懒加载
      component: () => import('../views/ServerView.vue')
    }
  ]
})

export default router
