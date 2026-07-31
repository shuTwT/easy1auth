import {
  Alert as AntAlert,
  Avatar as AntAvatar,
  Button as AntButton,
  Card as AntCard,
  Checkbox as AntCheckbox,
  ColorPicker as AntColorPicker,
  Divider as AntDivider,
  Input as AntInput,
  InputNumber as AntInputNumber,
  Modal as AntModal,
  Pagination as AntPagination,
  Popover as AntPopover,
  Progress as AntProgress,
  Radio as AntRadio,
  RadioGroup as AntRadioGroup,
  Select as AntSelect,
  SelectOptGroup,
  SelectOption,
  Skeleton as AntSkeleton,
  Slider as AntSlider,
  Switch as AntSwitch,
  Tag as AntTag,
  Tree as AntTree,
  TreeSelect as AntTreeSelect,
  Upload as AntUpload,
} from 'antdv-next'
import { computed, defineComponent, h, inject, provide, type PropType, type Ref } from 'vue'

export type UploadFile = Record<string, any>
export type TreeNodeData = Record<string, any>

type ModelContext = { open: Ref<boolean>; setOpen: (value: boolean) => void }
const dialogKey = Symbol('antdv-dialog')
const selectKey = Symbol('antdv-select')
const numberKey = Symbol('antdv-number')
const tabsKey = Symbol('antdv-tabs')

const passthrough = (name: string, tag = 'div') => defineComponent({
  name,
  inheritAttrs: false,
  setup(_, { attrs, slots }) {
    return () => h(tag, attrs, slots.default?.())
  },
})

export const Button = defineComponent({
  name: 'EasyButton',
  inheritAttrs: false,
  props: { variant: String, size: String, type: String },
  setup(props, { attrs, slots }) {
    return () => h(AntButton, {
      ...attrs,
      type: props.variant === 'link' ? 'link' : props.variant === 'ghost' ? 'text' : props.variant === 'outline' ? 'default' : 'primary',
      danger: props.variant === 'destructive',
      htmlType: props.type === 'submit' || props.type === 'reset' ? props.type : undefined,
      size: props.size === 'lg' ? 'large' : props.size === 'sm' || props.size?.startsWith('icon') ? 'small' : 'middle',
      shape: props.size?.startsWith('icon') ? 'circle' : undefined,
    }, slots)
  },
})

const valueComponent = (name: string, component: any, valueProp = 'value') => defineComponent({
  name,
  inheritAttrs: false,
  props: { modelValue: { type: [String, Number, Boolean, Array, Object] as PropType<any>, default: undefined } },
  emits: ['update:modelValue'],
  setup(props, { attrs, slots, emit }) {
    return () => h(component, { ...attrs, [valueProp]: props.modelValue, [`onUpdate:${valueProp}`]: (value: any) => emit('update:modelValue', value) }, slots)
  },
})

export const Input = valueComponent('EasyInput', AntInput)
export const Textarea = valueComponent('EasyTextarea', AntInput.TextArea)
export const Checkbox = valueComponent('EasyCheckbox', AntCheckbox, 'checked')
export const Switch = valueComponent('EasySwitch', AntSwitch, 'checked')
export const Slider = valueComponent('EasySlider', AntSlider)
export const ColorPicker = valueComponent('EasyColorPicker', AntColorPicker)

export const Card = defineComponent({ name: 'EasyCard', inheritAttrs: false, setup(_, { attrs, slots }) { return () => h(AntCard, attrs, slots) } })
export const CardContent = passthrough('EasyCardContent')
export const CardHeader = passthrough('EasyCardHeader')
export const CardFooter = passthrough('EasyCardFooter')
export const CardTitle = passthrough('EasyCardTitle', 'h3')
export const CardDescription = passthrough('EasyCardDescription', 'p')
export const CardAction = passthrough('EasyCardAction')

export const Badge = defineComponent({
  name: 'EasyBadge',
  inheritAttrs: false,
  props: { variant: String },
  setup(props, { attrs, slots }) {
    const color = props.variant === 'destructive' ? 'error' : props.variant === 'secondary' ? 'blue' : props.variant === 'outline' ? undefined : 'processing'
    return () => h(AntTag, { ...attrs, color }, slots.default?.())
  },
})
export const Avatar = defineComponent({ name: 'EasyAvatar', inheritAttrs: false, setup(_, { attrs, slots }) { return () => h(AntAvatar, attrs, slots) } })
export const AvatarFallback = passthrough('EasyAvatarFallback', 'span')
export const AvatarImage = passthrough('EasyAvatarImage', 'img')
export const Separator = defineComponent({ name: 'EasyDivider', inheritAttrs: false, setup(_, { attrs }) { return () => h(AntDivider, attrs) } })
export const Progress = defineComponent({ name: 'EasyProgress', inheritAttrs: false, props: { modelValue: Number }, setup(props, { attrs }) { return () => h(AntProgress, { ...attrs, percent: props.modelValue ?? Number(attrs.value) }) } })
export const Skeleton = defineComponent({ name: 'EasySkeleton', inheritAttrs: false, setup(_, { attrs }) { return () => h(AntSkeleton, attrs) } })

export const Select = defineComponent({
  name: 'EasySelect',
  inheritAttrs: false,
  props: { modelValue: { type: [String, Number, Array, Object] as PropType<any>, default: undefined } },
  emits: ['update:modelValue'],
  setup(props, { attrs, slots, emit }) {
    provide(selectKey, true)
    return () => h(AntSelect, { ...attrs, value: props.modelValue, 'onUpdate:value': (value: any) => emit('update:modelValue', value) }, slots)
  },
})
export const SelectItem = defineComponent({ name: 'EasySelectItem', inheritAttrs: false, props: { value: { type: [String, Number, Object] as PropType<any>, required: true } }, setup(props, { attrs, slots }) { return () => h(SelectOption, { ...attrs, value: props.value }, slots) } })
export const SelectGroup = defineComponent({ name: 'EasySelectGroup', inheritAttrs: false, setup(_, { attrs, slots }) { return () => h(SelectOptGroup, attrs, slots) } })
export const SelectContent = defineComponent({ name: 'EasySelectContent', setup(_, { slots }) { return () => slots.default?.() } })
export const SelectTrigger = defineComponent({ name: 'EasySelectTrigger', setup(_, { slots }) { return () => slots.default?.() } })
export const SelectValue = defineComponent({ name: 'EasySelectValue', setup() { return () => null } })
export const SelectItemText = defineComponent({ name: 'EasySelectItemText', setup(_, { slots }) { return () => slots.default?.() } })
export const SelectLabel = passthrough('EasySelectLabel', 'span')
export const SelectSeparator = defineComponent({ name: 'EasySelectSeparator', setup() { return () => null } })
export const SelectScrollDownButton = SelectSeparator
export const SelectScrollUpButton = SelectSeparator

export const RadioGroup = valueComponent('EasyRadioGroup', AntRadioGroup)
export const RadioGroupItem = defineComponent({ name: 'EasyRadioGroupItem', inheritAttrs: false, props: { value: { type: [String, Number, Boolean] as PropType<any>, required: true } }, setup(props, { attrs, slots }) { return () => h(AntRadio, { ...attrs, value: props.value }, slots) } })

export const NumberField = defineComponent({
  name: 'EasyNumberField',
  props: { modelValue: { type: Number, default: undefined }, min: Number, max: Number, step: Number },
  emits: ['update:modelValue'],
  setup(props, { slots, emit }) {
    provide(numberKey, { value: computed(() => props.modelValue), set: (value: number) => emit('update:modelValue', value), min: props.min, max: props.max, step: props.step || 1 })
    return () => h('div', { class: 'inline-flex items-center gap-1' }, slots.default?.())
  },
})
export const NumberFieldContent = passthrough('EasyNumberFieldContent')
export const NumberFieldInput = defineComponent({ name: 'EasyNumberFieldInput', setup() { const context = inject<any>(numberKey)!; return () => h(AntInputNumber, { value: context.value.value, min: context.min, max: context.max, step: context.step, 'onUpdate:value': context.set }) } })
export const NumberFieldIncrement = defineComponent({ name: 'EasyNumberFieldIncrement', setup(_, { attrs, slots }) { const context = inject<any>(numberKey)!; return () => h(AntButton, { ...attrs, size: 'small', onClick: () => context.set((context.value.value || 0) + context.step) }, slots.default?.() || '+') } })
export const NumberFieldDecrement = defineComponent({ name: 'EasyNumberFieldDecrement', setup(_, { attrs, slots }) { const context = inject<any>(numberKey)!; return () => h(AntButton, { ...attrs, size: 'small', onClick: () => context.set((context.value.value || 0) - context.step) }, slots.default?.() || '−') } })

export const Dialog = defineComponent({
  name: 'EasyDialog',
  props: { open: Boolean, modelValue: Boolean },
  emits: ['update:open', 'update:modelValue'],
  setup(props, { slots, emit }) {
    const open = computed(() => props.open ?? props.modelValue ?? false)
    provide(dialogKey, { open, setOpen: (value: boolean) => { emit('update:open', value); emit('update:modelValue', value) } } satisfies ModelContext)
    return () => slots.default?.()
  },
})
export const DialogContent = defineComponent({ name: 'EasyDialogContent', inheritAttrs: false, setup(_, { attrs, slots }) { const context = inject<ModelContext>(dialogKey)!; return () => h(AntModal, { ...attrs, open: context.open.value, footer: null, 'onUpdate:open': context.setOpen }, slots) } })
export const DialogHeader = passthrough('EasyDialogHeader')
export const DialogFooter = passthrough('EasyDialogFooter')
export const DialogTitle = passthrough('EasyDialogTitle', 'h3')
export const DialogDescription = passthrough('EasyDialogDescription', 'p')
export const DialogClose = defineComponent({ name: 'EasyDialogClose', setup(_, { slots }) { const context = inject<ModelContext>(dialogKey)!; return () => h('span', { onClick: () => context.setOpen(false) }, slots.default?.()) } })
export const DialogTrigger = passthrough('EasyDialogTrigger', 'span')
export const DialogOverlay = defineComponent({ name: 'EasyDialogOverlay', setup() { return () => null } })
export const DialogScrollContent = DialogContent

export const AlertDialog = Dialog
export const AlertDialogContent = DialogContent
export const AlertDialogHeader = DialogHeader
export const AlertDialogFooter = DialogFooter
export const AlertDialogTitle = DialogTitle
export const AlertDialogDescription = DialogDescription
export const AlertDialogAction = Button
export const AlertDialogCancel = Button
export const AlertDialogMedia = passthrough('EasyAlertDialogMedia')
export const AlertDialogTrigger = DialogTrigger

export const Alert = defineComponent({ name: 'EasyAlert', inheritAttrs: false, setup(_, { attrs, slots }) { return () => h(AntAlert, { ...attrs, type: attrs.variant === 'destructive' ? 'error' : 'info' }, { message: () => slots.default?.() }) } })
export const AlertTitle = passthrough('EasyAlertTitle', 'strong')
export const AlertDescription = passthrough('EasyAlertDescription', 'span')
export const AlertAction = passthrough('EasyAlertAction')

export const Tree = valueComponent('EasyTree', AntTree, 'checkedKeys')
export const TreeSelect = valueComponent('EasyTreeSelect', AntTreeSelect)
export const Upload = valueComponent('EasyUpload', AntUpload, 'fileList')
export const ScrollArea = passthrough('EasyScrollArea')
export const ScrollBar = passthrough('EasyScrollBar')
export const Popover = defineComponent({ name: 'EasyPopover', inheritAttrs: false, setup(_, { attrs, slots }) { return () => h(AntPopover, attrs, slots) } })
export const PopoverTrigger = defineComponent({ name: 'EasyPopoverTrigger', setup(_, { slots }) { return () => slots.default?.() } })
export const PopoverContent = passthrough('EasyPopoverContent')
export const PopoverAnchor = passthrough('EasyPopoverAnchor')
export const PopoverHeader = passthrough('EasyPopoverHeader')
export const PopoverTitle = passthrough('EasyPopoverTitle', 'h3')
export const PopoverDescription = passthrough('EasyPopoverDescription', 'p')

export const Pagination = defineComponent({ name: 'EasyPagination', inheritAttrs: false, props: { page: Number, pageSize: Number }, emits: ['update:page', 'update:pageSize'], setup(props, { attrs, emit }) { return () => h(AntPagination, { ...attrs, current: props.page, pageSize: props.pageSize, 'onUpdate:current': (value: number) => emit('update:page', value), 'onUpdate:pageSize': (value: number) => emit('update:pageSize', value) }) } })
export const PaginationContent = passthrough('EasyPaginationContent')
export const PaginationItem = passthrough('EasyPaginationItem')
export const PaginationNext = passthrough('EasyPaginationNext', 'button')
export const PaginationPrevious = passthrough('EasyPaginationPrevious', 'button')
export const PaginationEllipsis = passthrough('EasyPaginationEllipsis', 'span')
export const PaginationFirst = PaginationPrevious
export const PaginationLast = PaginationNext
export const PaginationLink = passthrough('EasyPaginationLink', 'button')

export const Table = passthrough('EasyTable', 'table')
export const TableHeader = passthrough('EasyTableHeader', 'thead')
export const TableBody = passthrough('EasyTableBody', 'tbody')
export const TableFooter = passthrough('EasyTableFooter', 'tfoot')
export const TableRow = passthrough('EasyTableRow', 'tr')
export const TableHead = passthrough('EasyTableHead', 'th')
export const TableCell = passthrough('EasyTableCell', 'td')
export const TableCaption = passthrough('EasyTableCaption', 'caption')
export const TableEmpty = passthrough('EasyTableEmpty', 'td')

export const Tabs = defineComponent({ name: 'EasyTabs', props: { modelValue: String, defaultValue: String }, emits: ['update:modelValue'], setup(props, { slots, emit }) { const current = computed(() => props.modelValue || props.defaultValue || ''); provide(tabsKey, { current, set: (value: string) => emit('update:modelValue', value) }); return () => h('div', slots.default?.()) } })
export const TabsList = passthrough('EasyTabsList')
export const TabsTrigger = defineComponent({ name: 'EasyTabsTrigger', props: { value: { type: String, required: true } }, setup(props, { slots }) { const context = inject<any>(tabsKey)!; return () => h(AntButton, { type: context.current.value === props.value ? 'primary' : 'default', size: 'small', onClick: () => context.set(props.value) }, slots.default?.()) } })
export const TabsContent = defineComponent({ name: 'EasyTabsContent', props: { value: { type: String, required: true } }, setup(props, { slots }) { const context = inject<any>(tabsKey)!; return () => context.current.value === props.value ? h('div', slots.default?.()) : null } })

export const DropdownMenu = passthrough('EasyDropdownMenu')
export const DropdownMenuContent = passthrough('EasyDropdownMenuContent')
export const DropdownMenuItem = passthrough('EasyDropdownMenuItem', 'button')
export const DropdownMenuTrigger = passthrough('EasyDropdownMenuTrigger', 'span')
export const DropdownMenuSeparator = Separator
export const DropdownMenuGroup = passthrough('EasyDropdownMenuGroup')
export const DropdownMenuLabel = passthrough('EasyDropdownMenuLabel')
export const DropdownMenuCheckboxItem = DropdownMenuItem
export const DropdownMenuRadioGroup = passthrough('EasyDropdownMenuRadioGroup')
export const DropdownMenuRadioItem = DropdownMenuItem
export const DropdownMenuShortcut = passthrough('EasyDropdownMenuShortcut', 'span')
export const DropdownMenuSub = passthrough('EasyDropdownMenuSub')
export const DropdownMenuSubContent = passthrough('EasyDropdownMenuSubContent')
export const DropdownMenuSubTrigger = DropdownMenuItem

export const Tooltip = passthrough('EasyTooltip')
export const TooltipProvider = passthrough('EasyTooltipProvider')
export const TooltipTrigger = passthrough('EasyTooltipTrigger', 'span')
export const TooltipContent = passthrough('EasyTooltipContent')
export const Collapsible = passthrough('EasyCollapsible')
export const CollapsibleContent = passthrough('EasyCollapsibleContent')
export const CollapsibleTrigger = passthrough('EasyCollapsibleTrigger', 'span')
export const Label = passthrough('EasyLabel', 'label')
export const Sheet = Dialog
export const SheetContent = DialogContent
export const SheetHeader = DialogHeader
export const SheetFooter = DialogFooter
export const SheetTitle = DialogTitle
export const SheetDescription = DialogDescription
export const SheetClose = DialogClose
export const SheetTrigger = DialogTrigger
export const SheetOverlay = DialogOverlay
