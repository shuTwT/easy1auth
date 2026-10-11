import { h } from 'vue'
import { App, ConfigProvider } from 'antdv-next'
import zhCN from 'antdv-next/locale/zh_CN'

export const antdLocale = zhCN

/** Makes feedback called from Axios interceptors use the same locale and token context. */
export function configureStaticFeedback() {
  ConfigProvider.config({
    holderRender: children => h(
      ConfigProvider,
      { locale: antdLocale, componentSize: 'middle' },
      { default: () => h(App, null, { default: () => children }) },
    ),
  })
}
