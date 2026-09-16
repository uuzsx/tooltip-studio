# Tooltip Studio — Fabric 1.20.4

![Tooltip Studio 图标](src/main/resources/assets/tooltipstudio/icon.png)

通过 JSON 或资源包自定义物品提示框，支持名称居中、分割线、条件匹配及图片和文字装饰。

作者：**幼幼紫、千村**。版本 **1.6**，纯客户端，仅支持 **Minecraft 1.20.4**。
项目采用 [MIT 协议](LICENSE)，仓库：[uuzsx/tooltip-studio](https://github.com/uuzsx/tooltip-studio)。

## 安装与默认外观

使用 Minecraft 1.20.4、Fabric Loader 0.15.11 或更新版，以及适用于 1.20.4 的 Fabric API。移除旧版 Tooltip Studio JAR，放入 `tooltip-studio-1.6+1.20.4.jar`；服务端无需安装。

**本版只内置 default 一套基础样式。** 原样使用用户朋友提供的 Default.png 和 defalut.json，JSON 仅将 texture 改为 `tooltipstudio:textures/styles/default.png`。原极光的 9 套样式、贴图及图集生成脚本已移除，示例包也不包含旧素材。

首次运行生成：

```text
config/tooltipstudio/
├─ config.json
├─ styles/default.json
├─ decorations/
└─ textures/
```

新配置的 defaultStyle 为 default，rules 与 decorationRules 为空；所有普通物品默认使用基础款。仍可自行添加样式、物品匹配、NBT 匹配与资源包，不限制扩展能力。

## 从旧版升级

- 不改写、不删除游戏实例中的用户配置。
- 对于未修改的旧内置样式 JSON，如果它引用的原内置贴图已不存在，加载时跳过该旧预设；只保留识别指纹，不打包旧定义或图片。
- 已不存在的旧预设名称（例如 rare、legendary）在 defaultStyle、规则 style 和物品 TooltipStyle 中自动回退到 default。
- 同名自定义样式或资源包样式若仍有效，继续优先使用；不按名称删除用户文件。
- 修改过的旧样式不会被静默替换。如果它仍引用已删除的旧 PNG，需要改为自己的 PNG 与相应切片参数。错误重载仍保留上一份成功状态。

可以将 config.json 的 defaultStyle 明确改成 `default`，并手动清理不再需要的旧规则。即使本地没有 styles/default.json，内置基础定义也始终可用。
同名覆盖顺序：**资源包 JSON → 本地 JSON → 内置 default**。删除本地 default.json 会恢复内置基础款，不会自动重新生成该文件。

## 功能

- 物品名称居中，名称换行后每行仍居中，正文左对齐；保留原有文字颜色、附魔与 lore。
- 标题与正文间显示分割线，只有标题时不显示。分割线左右两端固定，仅中段拉伸。
- 基础样式的背景、九宫格边框、分割线和自带装饰来自同一张 PNG；装饰支持面板九个锚点、分割线三个锚点及绘制前后层。
- 独立装饰可用自己的 PNG 或自定义文字，支持 x_scale / y_scale；通过匹配规则，叠加到任意已选样式上；支持多项同时命中、四角定位，不替换基础款外观。
- 支持物品 ID/通配符、物品 tag、稀有度、NBT 路径和值，以及单件物品 TooltipStyle 覆盖。
- 本地与资源包均可提供样式 JSON，支持多层分类目录、优先级覆盖及热重载。
- offsetX / offsetY 控制整框偏移。长文字自动换行，超大提示框整体缩小；屏幕边缘限制包含外部装饰。
- 保留原版收纳袋图形组件，兼容 Shulker Box Tooltip 4.1.0 的框内/框外预览与锁定物品。

## 全局配置与匹配规则

`config/tooltipstudio/config.json` 初始内容：

```json
{
  "schemaVersion": 1,
  "enabled": true,
  "defaultStyle": "default",
  "nbtStyleKey": "TooltipStyle",
  "rules": [],
  "decorationRules": []
}
```

enabled=false 恢复原版 tooltip；nbtStyleKey="" 禁用单件物品覆盖。
选择顺序：**有效 TooltipStyle → priority 从大到小的首个匹配规则 → defaultStyle**。同 priority 时本地规则先于资源包规则；本地按数组顺序，资源包再按文件路径和数组顺序。

先建立自己的样式，例如 monumenta/forest，然后把下面的对象加入 rules 数组：

```json
{
  "style": "monumenta/forest",
  "priority": 300,
  "nbt": {"Monumenta.Location": "forest"}
}
```

不写 items 即可匹配任何原版或模组物品基底。客户端必须实际收到该 NBT；匹配不修改物品名字、lore、模型或服务器数据。

| 条件 | 示例与含义 |
| --- | --- |
| items | `["minecraft:diamond_sword", "minecraft:netherite_*"]`；仅支持 `*` 通配符 |
| tags | `["minecraft:planks"]`；物品分类标签，不带 `#` |
| rarities | `["common", "uncommon", "rare", "epic"]` 中的值 |
| nbt | `{"Monumenta.Location":"forest"}`；物品数据中的路径和值 |

各数组内部为“或”，不同条件与不同 NBT 路径之间为“且”；每条规则至少有一个非空条件。
NBT 路径从物品 tag 内部开始，不加 SelectedItem.tag.、tag.、nbt. 或 #。支持原版路径语法及列表下标，如 `Monumenta.MMLore[0]`；选出多个值时任一个匹配即可。
字符串精确、区分大小写，`*` 在 NBT 值中没有通配作用；可用候选数组，例如 `{"Monumenta.Location":["forest","valley"]}`。
数值按数值比较，数值 123 不等于字符串 "123"；true/false 匹配 NBT byte 1b/0b。缺失字段或类型不符时继续后续规则。
`plain.display.Name` 可匹配服务器存储的纯文本名字；原版 display.Name 是 JSON 文本字符串，需写完整值，不自动转成可见名字。
每规则最多 64 条 NBT 路径，每路径最多 64 个候选值；路径最多 512 字符，值最多 4096 字符。本地与资源包规则合计最多 4096 条。

## 通过资源包提供样式

```text
资源包 ZIP 根目录/
├─ pack.mcmeta
└─ assets/tooltipstudio/
   ├─ styles/monumenta/forest.json
   ├─ rules/example.json
   └─ textures/styles/default.png
```

Minecraft 1.20.4 的 pack.mcmeta 使用 pack_format=22。样式 JSON 与本地格式相同，PNG 资源 ID 如 `tooltipstudio:textures/styles/default.png`；也可用其他合法命名空间。资源包样式不能用 local:。
规则文件格式为 `{"schemaVersion":1,"rules":[...]}`，也可包含 decorationRules，或只提供 decorationRules。全局开关、默认样式和覆盖键仍由本地 config.json 控制。
多个资源包同路径 JSON 由高优先级包整份覆盖；不同路径的规则文件合并。用空 rules 数组可屏蔽低优先级包同路径的规则文件。
停用资源包会移除其独有定义与自带规则；若本地手工引用了随包移除的样式，需要同步改掉引用。

附带 [示例资源包](examples/resource-pack/README.md) 只使用同一张默认 PNG。monumenta/forest 将基础款向上偏移 12 个 GUI 像素，用来演示木棍 ID、木板 tag 与 forest NBT 匹配。

## 分类路径

style、defaultStyle 和 TooltipStyle 填相对于 styles/ 的路径，去掉 .json。例如：

| 文件 | 样式 ID |
| --- | --- |
| styles/default.json | default |
| styles/monumenta/forest.json | monumenta/forest |
| styles/monumenta/ring3/forest.json | monumenta/ring3/forest |

每层名字用小写英文字母、数字、下划线或连字符。引用使用正斜杠，不加 styles/、.json、命名空间、磁盘路径、`.` 或 `..`。路径不相对于规则文件所在目录。同名文件放在不同目录时互不覆盖。

## 按条件叠加独立装饰

新增的 decorationRules 与原有 rules 平级。rules 选择基础样式，所有命中的 decorationRules 额外叠加装饰，背景、边框、分割线和文字布局仍由原样式决定。
支持 ID、tag、稀有度、NBT 条件；有效 TooltipStyle 只决定基础样式，不会屏蔽独立装饰。默认款、本地自定义和资源包样式全部支持。

```json
"decorationRules": [
  {"decorations":["sword/top_left"],"priority":200,"nbt":{"Monumenta.Location":"forest"}}
]
```

这是已有 config.json 的字段片段。对应装饰放在 `config/tooltipstudio/decorations/sword/top_left.json`，或资源包的 `assets/tooltipstudio/decorations/sword/top_left.json`。
图片装饰 JSON 指定自己的 texture、textureWidth、textureHeight、region、anchor、x、y、foreground；文字装饰使用 type=text 与 text，无需 PNG，不需要复制基础样式。装饰 ID 使用相对于 decorations/ 的分类路径，与 styles/ 中的同名 ID 独立。
本地 PNG 放入 config/tooltipstudio/textures/ 并使用 local: 引用；资源包使用资源 ID。原样式内的 decorations 继续使用原图集并正常绘制。

多条命中可叠加，同一个装饰 ID 只画一次；同一绘制层内，高 priority 在上方，同 priority 本地规则优先。最多同时叠加 64 个不同装饰，超过时按优先级取前 64 个。
TOP_LEFT、TOP_RIGHT、BOTTOM_LEFT、BOTTOM_RIGHT 支持四角，也支持边中点、中心和分割线左/中/右；x/y 调整装饰相对位置。整框移动、缩放和屏幕边缘限制均包含独立装饰。

可直接启用 `tooltip-studio-decoration-pack-1.6+1.20.4.zip`：云杉木门左上角出现小剑，NBT `Monumenta.Location=forest` 同样匹配，`TooltipDecorations=all_corners` 演示四角。该包不包含基础样式 JSON，也不会修改本地设置。
完整参数、四角表格、本地安装与测试命令见 [独立装饰说明](examples/independent-decorations/README.md)。剑贴图原样来自用户提供的 test.png，仅在可选示例中分发，默认 JAR 不会自动添加装饰。

1.6 新增的 [锚点、文字与缩放示例](examples/advanced-decoration-pack/README.md) 提供完整 JSON 和可直接启用的资源包。图片和文字都可用 `x_scale` / `y_scale`（默认 1，范围 0.0625..16）。分割线锚点为 `SEPARATOR_LEFT`、`SEPARATOR_CENTER`、`SEPARATOR_RIGHT`；名称换行时自动跟随，没有分割线时隐藏对应装饰。

## 自定义一套样式

复制 [default.json](src/main/resources/assets/tooltipstudio/defaults/styles/default.json) 到 `config/tooltipstudio/styles/my_style.json`，然后将规则或 defaultStyle 指向 my_style。
可沿用内置 texture，也可把自己的 PNG 放到 config/tooltipstudio/textures/my_style.png，设置 `"texture":"local:my_style.png"`。
[examples/custom-style](examples/custom-style/使用方法.txt) 提供可直接复制的文件，示例 PNG 与默认基础款完全相同。

| 样式字段 | 说明 |
| --- | --- |
| texture / textureWidth / textureHeight | 单张 PNG 引用与实际宽高；最大 4096×4096 |
| background | 用 u、v、width、height 选取背景区域并拉伸填充 |
| frame.region | 九宫格边框源区域；left/top/right/bottom 固定四边宽度 |
| separator.region | 完整分割线源区域，包括两端和中段 |
| separator.leftCap / rightCap | 不拉伸的左右端点宽度 |
| separator.inset / marginTop / marginBottom | 左右缩进与上下间距；enabled=false 可隐藏 |
| padding | 正文距面板边缘的距离，不小于对应边框宽度 |
| minWidth / maxWidth | 正文最小宽度与换行宽度，不含 padding；最大 2048 |
| decorations | 可为空数组，最多 64 项；图片使用 region，文字使用 type=text 与 text；均支持 anchor、x/y、foreground、x_scale/y_scale |
| offsetX / offsetY | 可省略，各自默认 0；整数 -4096..4096，单位 GUI 像素 |
| offsetXMode | 默认 cursor：X 正值远离鼠标、负值靠近；screen：固定正右负左 |

装饰 anchor 支持 TOP_LEFT、TOP、TOP_RIGHT、LEFT、CENTER、RIGHT、BOTTOM_LEFT、BOTTOM、BOTTOM_RIGHT，以及 SEPARATOR_LEFT、SEPARATOR_CENTER、SEPARATOR_RIGHT；x/y 调整装饰位置，foreground=true 在文字后绘制。
分割线高度采用源区域高度，左右两端保留原宽，中段横向拉伸；超大 tooltip 整体缩小时所有内容一起缩小。

## 整框偏移与重载

在样式 JSON 最外层，与 texture 同级添加 `"offsetX":0,"offsetY":-12`，整框向上移动 12 个 GUI 像素。Y 正下负上；X 默认正值远离鼠标、负值靠近鼠标，tooltip 在鼠标左侧时自动反向，左右间距一致。未填写偏移保持原位。
若需要 1.5 的固定屏幕方向，添加 `"offsetXMode":"screen"`，此时 X 正右负左；默认 `"cursor"`。升级后旧样式若有非零 offsetX，会使用新的鼠标相对方向。
偏移在正常定位、自动换行和缩放之后应用，背景、边框、名称、正文、分割线、装饰与框内预览一起移动。tooltip 整体缩小时偏移值本身不再缩小。
最终仍保留屏幕边缘 4 GUI 像素，包含外部装饰；已贴边或已占满屏幕时会限制实际移动距离。

```text
/tooltipstudio reload
/tooltipstudio list
/tooltipstudio decorations
```

本地 JSON/PNG 改动后使用 reload；资源包改动后按 F3+T。启用、停用、排序资源包也会触发重载。list 显示完整样式 ID，decorations 显示独立装饰 ID。
非法 JSON、无效区域、未知样式或缺失贴图会拒绝本次重载并保留上次成功状态，首次加载失败使用原版 tooltip。

## 构建与验证

使用 JDK 17 与 Gradle Wrapper：`./gradlew build`（Windows 使用 gradlew.bat）。产物为 build/libs/tooltip-studio-1.6+1.20.4.jar 和 build/resourcepacks/tooltip-studio-example-pack-1.6+1.20.4.zip。
独立四角装饰示例为 build/resourcepacks/tooltip-studio-decoration-pack-1.6+1.20.4.zip；分割线、文字和缩放示例为 build/resourcepacks/tooltip-studio-advanced-decoration-pack-1.6+1.20.4.zip。
`runSmoke` 启动开发测试客户端；`-PsmokeRunDir=run-smoke-base-only` 可使用独立测试目录，`-PcompatShulker` 启用潜影盒兼容测试。
GitHub Actions 自动运行构建与测试，构建产物保存 14 天。详情见 [验证记录](VERIFICATION.md) 与 [素材说明](THIRD_PARTY_ASSETS.md)。

当前未适配其他 Minecraft 版本。直接自行绘制 tooltip 的第三方界面仍可能需要额外适配；未验证朋友服务器完整模组组合。
