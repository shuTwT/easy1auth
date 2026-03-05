# Easy1Auth 登录页面设计规范

## 1. 设计理念

### 1.1 产品定位
Easy1Auth 是企业级统一身份管理平台（SaaS），需要传达：
- **专业性**：企业级安全认证
- **可信赖**：稳定可靠的身份管理
- **现代化**：前沿的技术架构
- **易用性**：简洁直观的用户体验

### 1.2 设计风格
**Glassmorphism（玻璃拟态）**
- 现代感强，符合 SaaS 产品调性
- 透明度和模糊效果营造层次感
- 适合展示安全性和专业性

## 2. 视觉设计系统

### 2.1 色彩方案

#### 主色调
- **Primary（主色）**: `#0369A1` - 企业蓝，传达专业和信任
- **Secondary（辅助色）**: `#0EA5E9` - 天空蓝，增加活力
- **CTA（行动号召）**: `#22C55E` - 安全绿，用于主要按钮

#### 功能色
- **Background（背景）**: `#F0F9FF` - 浅蓝背景
- **Text（文本）**: `#0C4A6E` - 深蓝文本
- **Error（错误）**: `#EF4444` - 警示红
- **Warning（警告）**: `#F59E0B` - 警告黄
- **Success（成功）**: `#10B981` - 成功绿

#### 玻璃效果
```css
/* Glass Card */
background: rgba(255, 255, 255, 0.8);
backdrop-filter: blur(20px);
border: 1px solid rgba(255, 255, 255, 0.3);
box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
```

### 2.2 字体系统

#### 字体家族
- **标题字体**: Poppins（现代、专业）
- **正文字体**: Open Sans（清晰、易读）

#### 字体导入
```css
@import url('https://fonts.googleapis.com/css2?family=Open+Sans:wght@300;400;500;600;700&family=Poppins:wght@400;500;600;700&display=swap');
```

#### 字体层级
| 元素 | 字体 | 字重 | 大小 | 行高 |
|------|------|------|------|------|
| H1（主标题） | Poppins | 700 | 48px | 1.2 |
| H2（副标题） | Poppins | 600 | 32px | 1.3 |
| H3（小标题） | Poppins | 600 | 24px | 1.4 |
| Body（正文） | Open Sans | 400 | 16px | 1.6 |
| Small（小字） | Open Sans | 400 | 14px | 1.5 |

### 2.3 间距系统
基于 8px 网格系统：
- **xs**: 4px
- **sm**: 8px
- **md**: 16px
- **lg**: 24px
- **xl**: 32px
- **2xl**: 48px

### 2.4 圆角
- **Small**: 4px（按钮、输入框）
- **Medium**: 8px（卡片）
- **Large**: 12px（大卡片）
- **Full**: 9999px（圆形元素）

### 2.5 阴影
```css
/* 轻微阴影 */
box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);

/* 中等阴影 */
box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);

/* 强烈阴影 */
box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
```

## 3. 组件设计规范

### 3.1 输入框

#### 状态
1. **默认状态**
   - 边框：`1px solid #E2E8F0`
   - 背景：`#FFFFFF`
   - 圆角：8px
   - 内边距：12px 16px

2. **聚焦状态**
   - 边框：`2px solid #0369A1`
   - 阴影：`0 0 0 3px rgba(3, 105, 161, 0.1)`
   - 过渡：`all 0.2s ease`

3. **错误状态**
   - 边框：`2px solid #EF4444`
   - 错误提示文字：`#EF4444`

4. **禁用状态**
   - 背景：`#F1F5F9`
   - 文字：`#94A3B8`
   - 光标：`not-allowed`

#### 尺寸
- **Large**: 高度 48px
- **Default**: 高度 40px
- **Small**: 高度 32px

### 3.2 按钮

#### 主要按钮
```css
background: linear-gradient(135deg, #0369A1 0%, #0EA5E9 100%);
color: #FFFFFF;
border-radius: 8px;
padding: 12px 24px;
font-weight: 600;
transition: all 0.2s ease;
```

**悬停状态**：
```css
transform: translateY(-2px);
box-shadow: 0 4px 16px rgba(3, 105, 161, 0.3);
```

**点击状态**：
```css
transform: translateY(0);
```

#### 次要按钮
```css
background: transparent;
color: #0369A1;
border: 2px solid #0369A1;
border-radius: 8px;
padding: 10px 22px;
font-weight: 600;
```

### 3.3 卡片

#### 玻璃卡片
```css
background: rgba(255, 255, 255, 0.8);
backdrop-filter: blur(20px);
border: 1px solid rgba(255, 255, 255, 0.3);
border-radius: 16px;
box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
padding: 40px;
```

## 4. 页面布局

### 4.1 整体布局
- **背景**: 渐变背景 + 动态几何图形
- **容器**: 居中的玻璃卡片
- **最大宽度**: 480px（移动端 100%）
- **内边距**: 40px（移动端 24px）

### 4.2 响应式断点
- **Mobile**: < 768px
- **Tablet**: 768px - 1024px
- **Desktop**: > 1024px

### 4.3 网格系统
- 12 列网格
- 列间距：24px
- 容器最大宽度：1440px

## 5. 交互设计

### 5.1 动画原则
- **时长**: 150-300ms
- **缓动函数**: `ease-in-out`
- **避免**: 过度动画和弹跳效果

### 5.2 过渡效果
```css
/* 颜色过渡 */
transition: color 0.2s ease;

/* 背景过渡 */
transition: background-color 0.2s ease;

/* 变换过渡 */
transition: transform 0.2s ease;
```

### 5.3 加载状态
- 使用旋转动画
- 颜色：主色调
- 大小：20px（小）、24px（中）、32px（大）

## 6. 可访问性

### 6.1 颜色对比度
- 正文文本：至少 4.5:1
- 大标题：至少 3:1
- 使用对比度检查工具验证

### 6.2 键盘导航
- 所有交互元素可通过 Tab 键访问
- 焦点状态清晰可见
- 逻辑顺序

### 6.3 屏幕阅读器
- 所有图片添加 alt 属性
- 表单元素关联 label
- 使用语义化 HTML

### 6.4 减少动画
```css
@media (prefers-reduced-motion: reduce) {
  * {
    animation-duration: 0.01ms !important;
    transition-duration: 0.01ms !important;
  }
}
```

## 7. 图标规范

### 7.1 图标库
- **推荐**: Heroicons、Lucide Icons
- **禁止**: 使用 emoji 作为图标

### 7.2 图标尺寸
- **Small**: 16px × 16px
- **Default**: 20px × 20px
- **Large**: 24px × 24px

### 7.3 图标颜色
- 默认：`#64748B`
- 激活：`#0369A1`
- 悬停：`#0EA5E9`

## 8. 表单设计规范

### 8.1 标签位置
- 标签位于输入框上方
- 标签与输入框间距：8px
- 必填项标记：红色星号

### 8.2 验证反馈
- 实时验证（失焦后）
- 错误提示位于输入框下方
- 成功状态：绿色勾选图标

### 8.3 密码显示/隐藏
- 使用眼睛图标
- 图标位于输入框右侧
- 点击切换密码可见性

## 9. 移动端适配

### 9.1 触摸目标
- 最小触摸区域：44px × 44px
- 按钮间距：至少 8px

### 9.2 输入优化
- 使用合适的 input type
- 自动完成建议
- 自动聚焦首个输入框

### 9.3 布局调整
- 单列布局
- 全宽按钮
- 底部固定操作栏

## 10. 性能优化

### 10.1 图片优化
- 使用 WebP 格式
- 懒加载非关键图片
- 响应式图片

### 10.2 CSS 优化
- 使用 CSS 变量
- 避免过度使用阴影和模糊
- 压缩 CSS 文件

### 10.3 字体优化
- 使用 `font-display: swap`
- 预加载关键字体
- 限制字体变体数量

## 11. 设计交付清单

### 11.1 视觉质量
- [ ] 无 emoji 图标（使用 SVG）
- [ ] 图标来源一致（Heroicons/Lucide）
- [ ] 品牌标识正确
- [ ] 悬停状态无布局偏移
- [ ] 使用主题色而非 var() 包装

### 11.2 交互
- [ ] 所有可点击元素有 `cursor: pointer`
- [ ] 悬停状态提供清晰视觉反馈
- [ ] 过渡流畅（150-300ms）
- [ ] 键盘导航焦点状态可见

### 11.3 亮色/暗色模式
- [ ] 亮色模式文本对比度充足（4.5:1 最小）
- [ ] 玻璃/透明元素在亮色模式可见
- [ ] 边框在两种模式下可见
- [ ] 交付前测试两种模式

### 11.4 布局
- [ ] 浮动元素距边缘有适当间距
- [ ] 无内容隐藏在固定导航栏后
- [ ] 在 375px、768px、1024px、1440px 响应式
- [ ] 移动端无横向滚动

### 11.5 可访问性
- [ ] 所有图片有 alt 文本
- [ ] 表单输入有标签
- [ ] 颜色不是唯一指示器
- [ ] 遵守 `prefers-reduced-motion`

## 12. 设计文件

### 12.1 高保真设计稿
- 桌面端设计稿（1440px）
- 移动端设计稿（375px）
- 平板端设计稿（768px）

### 12.2 交互原型
- 登录流程演示
- 错误状态演示
- 加载状态演示

### 12.3 设计资源
- 色彩调色板
- 字体文件
- 图标库
- 组件库

---

**版本**: v1.0  
**创建日期**: 2026-03-05  
**最后更新**: 2026-03-05
