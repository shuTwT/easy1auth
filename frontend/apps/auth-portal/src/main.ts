import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
import './style.css'
import '@easy1auth/theme/styles.css'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/oauth-login' },
    { path: '/oauth-login', component: App },
    { path: '/oauth-login/social/callback', component: App },
    { path: '/oauth-consent', component: App },
    { path: '/:pathMatch(.*)*', redirect: '/oauth-login' }
  ]
})

createApp(App).use(router).mount('#app')
