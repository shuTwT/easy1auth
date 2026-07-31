import { createApp } from 'vue'
import { createPinia } from 'pinia'
import {
  Alert, Avatar, Badge, Button, Card, Checkbox, ColorPicker, Divider, Input,
  InputNumber, Modal, Progress, Radio, RadioGroup, Select, SelectOptGroup,
  SelectOption, Skeleton, Slider, Switch, Tag, Tree, TreeSelect, Upload,
} from 'antdv-next'
import router from './router'
import App from './App.vue'
import { configureStaticFeedback } from './config/antd'

import './styles/tailwind.css'
import './style.css'
import './styles/theme.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)
for (const [name, component] of Object.entries({
  Alert, Avatar, Badge, Button, Card, Checkbox, ColorPicker, Divider, Input,
  InputNumber, Modal, Progress, Radio, RadioGroup, Select, SelectOptGroup,
  SelectOption, Skeleton, Slider, Switch, Tag, Tree, TreeSelect, Upload,
  InputTextArea: Input.TextArea,
})) {
  app.component(name, component)
}
configureStaticFeedback()

app.mount('#app')
