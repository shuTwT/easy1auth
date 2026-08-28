# Easy1Auth Admin · Design System

> 全局设计源。构建具体页面前先读取 `design-system/pages/[page-name].md`；页面规则优先于本文件。

**版本**：2.0 · 2026-08-08

**产品**：企业级多租户身份与访问管理控制台

**设计方向**：Enterprise Gateway / restrained glassmorphism

## 设计原则

1. **可信优先**：用深海蓝建立安全感，动作使用单一高辨识度的蓝色 CTA；绿色只表达成功或安全状态。
2. **信息分层**：页面背景、导航、内容面板、浮层四层分明，数据密集但保留呼吸感。
3. **渐进披露**：默认展示决策所需信息，复杂配置通过抽屉、对话框和详情页展开。
4. **可访问**：正文对比度至少 4.5:1；所有交互元素有可见 focus ring；支持键盘与 `prefers-reduced-motion`。

## 色彩令牌

| 语义 | CSS 变量 | 值 | 用途 |
|---|---|---|---|
| 品牌主色 | `--color-primary` | `#0369A1` | 主按钮、链接、选中态 |
| 主色悬停 | `--color-primary-hover` | `#075985` | hover/active |
| 品牌高亮 | `--color-primary-soft` | `#E0F2FE` | 选中背景、信息提示 |
| 安全成功 | `--color-success` | `#15803D` | 已启用、通过、健康 |
| 警告 | `--color-warning` | `#B45309` | 即将过期、风险提示 |
| 危险 | `--color-danger` | `#B91C1C` | 删除、失败、阻断 |
| 页面背景 | `--color-background` | `#F4F8FB` | 主内容区 |
| 面板 | `--color-surface` | `#FFFFFF` | 卡片、表格、表单 |
| 导航 | `--color-sidebar` | `#0F2742` | 侧边栏底色 |
| 主文字 | `--color-text` | `#102A43` | 标题、正文 |
| 次文字 | `--color-text-muted` | `#52677D` | 描述、辅助信息 |
| 边框 | `--color-border` | `#D7E2EC` | 分隔线、输入框 |

禁止用纯黑、荧光渐变或大面积橙/紫作为品牌色。透明层仅用于浮层：`rgba(255,255,255,.72)` + `backdrop-filter: blur(16px)`，并保留实色 fallback。

## 字体与排版

- **正文**：Fira Sans，fallback `ui-sans-serif, system-ui, sans-serif`。
- **数字/技术字段**：Fira Code，fallback `ui-monospace, monospace`。
- 标题使用 600–700；正文 400–500；中文行高 1.6，数字行高 1.2。
- `text-xs` 12px、`text-sm` 14px、正文 15px、`text-lg` 18px、页面标题 24px。

## 间距、圆角与层级

基础间距 4px：`xs 4`、`sm 8`、`md 16`、`lg 24`、`xl 32`、`2xl 48`（px）。

圆角：`sm 6px`（控件）、`md 10px`（卡片）、`lg 14px`（大面板）、`pill 999px`（状态）。

阴影采用低对比度：`sm 0 1px 2px rgba(16,42,67,.06)`、`md 0 8px 24px rgba(16,42,67,.08)`、`lg 0 20px 48px rgba(16,42,67,.14)`。

## 组件规范

### 导航与布局

- 左侧导航宽 248px，深色渐变 `#0F2742 → #123B5D`；活动项为 `#0EA5E9` 10% 背景 + 3px 左侧指示条。
- 顶栏高度 64px，内容区最大宽度 1440px，桌面内边距 24–32px。
- 移动端导航改为抽屉，禁止横向滚动；断点 768px/1024px/1440px。

### 按钮

- 高度 36px（紧凑）或 40px（标准），水平内边距 16px，圆角 8px。
- Primary 为 `--color-primary` 白字；Secondary 为白底 + `--color-border`；Danger 使用 `--color-danger`。
- hover 只改变颜色/阴影，不使用会造成布局位移的 scale；过渡 150–200ms。

### 卡片、表格与状态

- 卡片白底、1px 边框、10px 圆角；只有可点击卡片才显示 pointer 和 hover 阴影。
- 表格表头使用 `--color-background`，行 hover 使用 `--color-primary-soft` 的 55%；数字右对齐。
- 状态用文字 + 颜色 + 图标三重表达，不能只依赖颜色；图标统一 Lucide/Heroicons 24px viewBox，禁止 emoji。

### 表单、反馈与浮层

- 输入高度 40px，focus 为 2px `--color-primary` ring；错误文本使用 `role=alert` 或 `aria-live`。
- Toast 用于短反馈，表单错误靠近字段；空状态提供下一步操作。
- Modal/Drawer 使用半透明遮罩和 16px blur，z-index 分层：content 0、header 20、dropdown 40、modal 60、toast 80。

## 动效与无障碍

- 只为关键状态做动效，单页最多 1–2 个重点动效；进入使用 ease-out，退出使用 ease-in。
- 所有交互状态支持键盘；focus ring 不得被 `outline: none` 移除。
- `@media (prefers-reduced-motion: reduce)` 下将持续动画设为 none、过渡缩短为 0ms。
- 触控目标至少 44×44px；正文最小 14px；确保 375px、768px、1024px、1440px 下可用。

## 反模式

- 不使用 emoji 充当图标；不使用霓虹渐变、过度玻璃、深色模式默认值。
- 不隐藏 focus，不用纯颜色表达状态，不让 hover 改变布局，不在页面正文堆叠 error 文案。

## 交付检查

- [ ] 页面级覆盖文件已检查
- [ ] 颜色、字体、间距只引用语义令牌
- [ ] 键盘 focus、错误播报、减弱动效已验证
- [ ] 375/768/1024/1440px 无横向溢出
- [ ] 图标来自统一 SVG 图标集且有 aria-label/tooltip
