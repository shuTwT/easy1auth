import { computed, h, ref } from 'vue'
import { App, ConfigProvider, type ThemeConfig } from 'antdv-next'
import zhCN from 'antdv-next/locale/zh_CN'

const primaryColor = ref('#0369A1')
const headerColor = ref('#FFFFFF')
const sidebarColor = ref('linear-gradient(180deg, #0F172A 0%, #1E293B 100%)')

export const antdLocale = zhCN

export const antdTheme = computed<ThemeConfig>(() => ({
  token: {
    colorPrimary: primaryColor.value,
    colorInfo: '#0EA5E9',
    colorSuccess: '#10B981',
    colorWarning: '#F59E0B',
    colorError: '#EF4444',
    colorBgLayout: '#F1F5F9',
    colorBgContainer: '#FFFFFF',
    colorText: '#0F172A',
    colorTextSecondary: '#475569',
    colorBorder: '#E2E8F0',
    borderRadius: 8,
    fontSize: 14,
  },
  components: {
    Layout: {
      headerBg: headerColor.value,
      siderBg: sidebarColor.value,
    },
    Menu: {
      darkItemBg: 'transparent',
      darkSubMenuItemBg: 'transparent',
      darkItemSelectedBg: 'rgba(56, 189, 248, 0.14)',
      darkItemSelectedColor: '#38BDF8',
      darkItemHoverBg: 'rgba(255, 255, 255, 0.06)',
    },
  },
}))

export function setAdminTheme(panel?: {
  primaryColor?: string
  headerColor?: string
  sidebarColor?: string
}) {
  primaryColor.value = panel?.primaryColor || '#0369A1'
  headerColor.value = panel?.headerColor || '#FFFFFF'
  sidebarColor.value = panel?.sidebarColor || 'linear-gradient(180deg, #0F172A 0%, #1E293B 100%)'
}

/** Makes feedback called from Axios interceptors use the same locale and token context. */
export function configureStaticFeedback() {
  ConfigProvider.config({
    holderRender: children => h(
      ConfigProvider,
      { locale: antdLocale, theme: antdTheme.value, componentSize: 'middle' },
      { default: () => h(App, null, { default: () => children }) },
    ),
  })
}
