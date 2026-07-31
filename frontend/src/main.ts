import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import App from './App.vue'
import { configureStaticFeedback } from './config/antd'

import './styles/tailwind.css'
import './style.css'
import './styles/theme.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)
configureStaticFeedback()

app.mount('#app')
