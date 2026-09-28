import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Antd from 'antdv-next'
import router from './router'
import App from './App.vue'
import { configureStaticFeedback } from './config/antd'

import './styles/tailwind.css'
import './style.css'
import './styles/theme.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(Antd)
configureStaticFeedback()

app.mount('#app')
