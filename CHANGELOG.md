# 1.8.1

- 修复 texture 引用包含大写字母时被 Minecraft Identifier 校验提前拒绝的问题；例如资源包中的 reverie_fireR.png 可按实际大小写直接引用，无需改名。
- 样式主图、内嵌装饰、独立装饰及动画帧条共用修复；兼容文件夹/ZIP 资源包和版本覆盖层，保留资源包优先级与过滤规则。同一包内先查原样路径，再查全小写路径。
- 本地 local: 前缀不区分大小写，实际本地文件路径保留原样。缺失文件仍拒绝重载并保留旧状态；未改变样式/装饰 ID、物品 ID、NBT 值的匹配规则。

# 1.8

- 图片装饰新增 animation，支持横向或纵向 PNG 序列帧，配置帧数与每帧时长后按真实时间循环；适用于样式内装饰和独立匹配装饰，支持原有锚点、XY 偏移、缩放及绘制层。
- 样式内 decorations 可选 texture、textureWidth、textureHeight，允许每个装饰引用独立 PNG；省略时继续使用样式图集。静态、动画、本地 local: 与资源包资源 ID 均支持。
- 动画仅改变单帧取图区域，不改变面板布局；加载时校验所有帧及独立贴图，错误保留上一份完整有效配置。同一贴图与尺寸只上传一次，不逐帧读取文件。
- 新增可选星光动画资源包：木棍左上角由样式内装饰绘制，右上角通过独立匹配规则叠加。模组仍只内置原默认基础款，不包含参考 GIF 素材。

# 1.7

- 文本装饰新增 segments 数组，一个装饰可以连续拼接多段不同颜色、粗体、斜体的文字；各段省略的格式继承装饰外层设置，显式 false 可取消继承。
- 拼接后统一计算宽高、锚点、偏移及 XY 缩放；支持换行与多行文本，各段之间不自动添加空格。阴影与绘制层作用于整个装饰。
- 同时支持独立装饰、样式自带 decorations、本地 JSON 和资源包。旧 text 字符串保持兼容，text 与 segments 二选一；最多 64 段，合计 1024 字符、16 行。非法配置保留上一份有效状态。
- 提供灰色 Architect's Ring 前缀加红色 Artifact、金色粗体星星和换行斜体的可选示例资源包，无需新贴图；默认基础款和既有匹配机制不变。

# 1.6

- 修复整框横向偏移：offsetX 默认以鼠标为参照，正值远离、负值靠近，tooltip 切换左右时自动反向，使两侧间距一致；可用 offsetXMode=screen 保留旧的固定屏幕方向。offsetY 行为不变。
- 装饰新增 SEPARATOR_LEFT、SEPARATOR_CENTER、SEPARATOR_RIGHT 锚点，按实际分割线端点和中线定位，随名称换行移动；没有显示分割线时隐藏对应装饰。
- 新增 type=text 的纯文字装饰，支持自定义文字、#RRGGBB 颜色、粗体、斜体、阴影与换行；无需 PNG。适用于独立装饰和样式内 decorations。
- 图片与文字装饰均支持 x_scale / y_scale 独立缩放，默认 1，范围 0.0625..16。锚点按缩放后的尺寸计算，x/y 偏移不被装饰自身缩放；屏幕边缘限制包含文字和缩放后的图片。
- 新增可选资源包与游戏内演示，保留默认基础款、旧匹配机制、资源包分类路径与潜影盒兼容。

# 1.5

- 新增独立 decorations 定义与 decorationRules，匹配后在已选 tooltip 上额外绘制图片，不替换背景、边框、分割线或文字布局；适用于默认款、本地样式、资源包样式及 TooltipStyle 覆盖。
- 装饰可引用单独的 PNG，并使用 region、九个 anchor、x/y 与 foreground 指定取图区域、位置与绘制层。本地和资源包均支持多层分类路径、覆盖和热重载。
- 沿用 ID、tag、稀有度和 NBT 条件，多条命中同时叠加，相同 ID 去重；同层按 priority 与稳定顺序绘制，单次最多 64 项。完整 tooltip 的偏移、缩放和屏幕边缘限制包含独立装饰。
- 装饰与基础样式共同校验并一次性更新；无效 JSON、引用或贴图保留之前的完整状态。旧配置可省略 decorationRules，无需迁移；原有样式内装饰继续使用。
- 增加 /tooltipstudio decorations 命令，重载提示同时显示样式与独立装饰数量。
- 提供用户 test.png 的四角剑示例、本地文件与独立资源包；示例包匹配云杉木门和 Monumenta.Location，不包含基础样式定义。默认 JAR 仍只内置 default tooltip。

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
