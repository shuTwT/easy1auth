import { h } from 'vue'
import { App, ConfigProvider, type ThemeConfig } from 'antdv-next'
import zhCN from 'antdv-next/locale/zh_CN'

export const antdLocale = zhCN

export const antdTheme: ThemeConfig = {
  token: {
    colorPrimary: '#0369A1',
    colorInfo: '#0369A1',
    colorSuccess: '#15803D',
    colorWarning: '#B45309',
    colorError: '#B91C1C',
    colorBgLayout: '#F4F8FB',
    colorBgContainer: '#FFFFFF',
    colorText: '#102A43',
    colorTextSecondary: '#52677D',
    colorBorder: '#D7E2EC',
    colorLink: '#0369A1',
    fontFamily: '"Fira Sans", ui-sans-serif, system-ui, sans-serif',
    fontFamilyCode: '"Fira Code", ui-monospace, monospace',
    borderRadius: 10,
    controlHeight: 40,
    motion: true,
    fontSize: 14,
  },
  components: {
    Layout: {
      headerBg: '#FFFFFF',
      siderBg: 'linear-gradient(180deg, #0F2742 0%, #123B5D 100%)',
    },
    Menu: {
      darkItemBg: 'transparent',
      darkSubMenuItemBg: 'transparent',
      darkItemSelectedBg: 'rgba(56, 189, 248, 0.10)',
      darkItemSelectedColor: '#38BDF8',
      darkItemHoverBg: 'rgba(255, 255, 255, 0.06)',
    },
  },
}

/** Makes feedback called from Axios interceptors use the same locale and token context. */
export function configureStaticFeedback() {
  ConfigProvider.config({
    holderRender: children => h(
      ConfigProvider,
      { locale: antdLocale, theme: antdTheme, componentSize: 'middle' },
      { default: () => h(App, null, { default: () => children }) },
    ),
  })
}
