import { onBeforeUnmount, ref } from 'vue'

const dateFormatter = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  hour12: false
})

/**
 * 个人中心时间格式化，空值显示“暂无记录”。
 */
export function useProfileFormat() {
  const formatDate = (value: string | null | undefined) => {
    if (!value) {
      return '暂无记录'
    }
    return dateFormatter.format(new Date(value))
  }

  return { formatDate }
}

/**
 * 倒计时：用于验证码重发间隔，组件卸载时自动清理定时器。
 */
export function useCountdown(seconds = 60) {
  const countdown = ref(0)
  let timer: ReturnType<typeof setInterval> | null = null

  const stop = () => {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }

  const start = () => {
    stop()
    countdown.value = seconds
    timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        stop()
      }
    }, 1000)
  }

  onBeforeUnmount(stop)

  return {
    countdown,
    start,
    stop
  }
}
