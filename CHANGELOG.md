# 1.4

- 默认基础款使用用户朋友提供的 Default.png 与 defalut.json，样式 ID 为 default；PNG 原样内置，JSON 仅调整贴图引用路径。
- 仅保留这一套默认贴图，删除原极光的 9 套预设 JSON、PNG 及图集生成脚本；自定义示例与资源包示例也改用同一张默认 PNG。
- 新安装的 defaultStyle 为 default，rules 为空；保留 ID、稀有度、tag、NBT 匹配、分类路径、资源包、整体 XY 偏移与潜影盒预览兼容。
- default 对旧实例同样内置可用，无需新增本地文件；本地同名 JSON 和资源包仍可覆盖它。
- 升级不改写或删除用户配置。对未修改且已缺少原贴图的旧预设，用指纹识别并跳过；缺失的旧预设名称在默认设置、匹配规则与 TooltipStyle 中自动回退到 default。用户自定义的有效同名样式继续优先使用。

# 1.3

- 样式 JSON 新增可选 `offsetX`、`offsetY`，分别控制整个 tooltip 的水平、垂直偏移；默认 0，旧样式无需修改。
- 正 X 向右、负 X 向左，正 Y 向下、负 Y 向上；整数范围 -4096..4096，单位为屏幕 GUI 像素。背景、边框、名称、分割线、正文、装饰及框内预览一同移动。
- 在原有正常定位和缩放后应用偏移，保留含外部装饰的屏幕边缘限制；维持潜影盒预览与锁定物品的坐标转换。
- 本地与资源包样式均可设置并热重载，示例 monumenta/forest 向上偏移 12 像素；新增参数说明与游戏内前后对照截图测试。

# 1.2

- 本地 `config/tooltipstudio/styles/` 与资源包 `assets/tooltipstudio/styles/` 支持多层目录，样式 ID 使用相对路径去掉 `.json`，例如 `monumenta/forest`。
- 匹配规则的 style、全局 defaultStyle 与单件物品 TooltipStyle 均支持分类路径，`/tooltipstudio list` 显示完整 ID。旧的平铺样式与引用保持有效。
- 资源包覆盖按完整样式路径处理，不同目录中的同名文件互不覆盖；非法路径或失效引用仍拒绝重载并保留上次成功配置。
- 更新完整示例资源包，用 `monumenta/forest` 演示 Monumenta.Location 匹配，保留旧的 pack_forest 示例 ID 和原有 PNG。

# 1.1

- 新增资源包样式 JSON 加载：`assets/tooltipstudio/styles/名字.json`，使用现有单图样式格式。
- 新增资源包规则加载：`assets/tooltipstudio/rules/*.json`，支持 ID、tag、稀有度与 NBT 条件；资源包可以独立携带样式、贴图和规则。
- 同名样式由资源包覆盖本地；多个资源包同路径文件按游戏排序覆盖，不同规则文件合并并按 priority 匹配，本地规则在相同 priority 下优先。
- 启用、停用、资源包排序和 F3+T 触发重载；停用包移除其样式及自带规则，不改写本地配置。错误资源仍保留上次成功状态。
- 提供木棍/木板与 Monumenta forest 的完整 ZIP 资源包示例，保留 1.0 的图标、作者与 MIT 协议。

# 1.0

- 添加用户提供的模组图标，并在项目 README 中展示。
- 项目协议改为 MIT，版权署名为幼幼紫、千村；JAR 同时携带协议与第三方素材说明。
- 作者设为幼幼紫、千村，补充中文功能简介。
- 模组显示版本设为 1.0；发布文件名保留 +1.20.4，标明适用的 Minecraft 版本。
- 保留 0.1.2 的全部功能与配置格式。

# 0.1.2+1.20.4

- 新增规则 `nbt` 对象：按物品 NBT 路径和值选择样式，例如 `Monumenta.Location = "forest"`、`plain.display.Name = "Double Down"`。
- 支持多个字段同时满足、一个字段多个候选值，以及 NBT 条件与 ID、物品 tag、稀有度组合。
- 支持精确字符串、数值和布尔值；路径使用原版解析器，仅在配置重载时编译，悬停匹配不修改物品 NBT。
- 保留原有规则优先级、有效 `TooltipStyle` 覆盖和默认样式；旧配置无需迁移，内置默认规则保持不变。
- 非法 NBT 条件拒绝重载并保留上次配置，缺失字段正常跳过；保留 0.1.1 的潜影盒预览兼容修复。
- 附带 Monumenta 来源规则、材质规则对应写法、测试命令与实际客户端截图。只适用于 Minecraft 1.20.4。

# 0.1.1+1.20.4

- 修复与 Shulker Box Tooltip 4.1.0 的 `HandledScreenMixin` Redirect 冲突造成的启动崩溃。
- 用可串联的 WrapMethod 保留背包原方法和其他模组的预览/锁定逻辑，在最终绘制入口应用样式。
- 正确绘制 Shulker Box Tooltip 的框内、框外预览，并在锁定时根据被锁定的物品选择样式。
- 使用 finally 恢复嵌套 tooltip 的物品上下文，渲染异常和提前取消也不会留下状态。
- 内嵌 MixinExtras 0.4.1；原有 JSON 与 PNG 配置无需修改。

实现参考：[MixinExtras WrapMethod](https://github.com/LlamaLad7/MixinExtras/wiki/WrapMethod)、[Shulker Box Tooltip 4.1.0 源码](https://github.com/MisterPeModder/ShulkerBoxTooltip/tree/v4.1.0%2B1.20.4)。
