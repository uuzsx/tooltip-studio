# Tooltip Studio — Fabric 1.20.4

![Tooltip Studio 图标](src/main/resources/assets/tooltipstudio/icon.png)

通过配置或资源包中的 JSON 和单张贴图自定义物品提示框，支持名称居中、装饰分割线及物品与 NBT 匹配。

作者：**幼幼紫、千村**。当前版本 **1.2，只支持 Minecraft 1.20.4**，为纯客户端模组。

项目仓库：[uuzsx/tooltip-studio](https://github.com/uuzsx/tooltip-studio)。

项目采用 [MIT 协议](LICENSE)。借用的极光示例贴图保留原有权属，详见 [素材与第三方依赖说明](THIRD_PARTY_ASSETS.md)。

## 安装

1. 使用 Minecraft **1.20.4 + Fabric Loader 0.15.11 或更新版**。
2. 在游戏实例的 `mods/` 中放入 `tooltip-studio-1.2+1.20.4.jar` 和 **适用于 1.20.4 的 Fabric API**。
3. 启动游戏。第一次运行会自动生成下面的配置目录。服务端无需安装。

Fabric API 下载：<https://modrinth.com/mod/fabric-api/versions?g=1.20.4>

```text
游戏实例/
├─ mods/
│  └─ tooltip-studio-1.2+1.20.4.jar
└─ config/tooltipstudio/
   ├─ config.json            全局开关、默认样式、物品匹配规则
   ├─ styles/
   │  ├─ rare.json           一套样式一个 JSON，相对路径去掉 .json 就是样式 ID
   │  ├─ legendary.json
   │  └─ ...
   └─ textures/             放你自己的单张 PNG 图集
```

## 已实现

- 物品名称居中；名称换行后每一行仍居中，正文保持左对齐。
- 名称与后续说明/lore 之间插入分割线；只有名称时不显示分割线。
- 分割线左右两端固定，只拉伸中间。两端宽度、高度、纹理区域、上下间距和左右缩进均可配置。
- 边框使用九宫格布局：四角保持尺寸，四条边沿各自方向拉伸，中心由独立背景区域填充。
- **每套样式只引用一张 PNG**，背景、边框、分割线和所有装饰都从这张图中取区域。
- 装饰支持九个锚点、偏移和前后绘制层。
- 物品 ID/通配符、物品标签、稀有度、**NBT 路径和值**匹配，以及单件物品 NBT 覆盖。
- 本地 PNG 和资源包贴图均可使用。JSON 和本地 PNG 修改后可直接重载。
- 资源包可提供完整的样式 JSON、PNG 和匹配规则，启用后自动加载，停用后移除，支持 F3+T 重载。
- 本地和资源包样式均支持多层文件夹分类，例如 `monumenta/forest`；原有单层样式 ID 保持有效。
- 保留物品文字颜色与格式、附魔说明、原版和 Fabric 添加的 lore，以及原版收纳袋的图形 tooltip 组件。
- 长行自动换行；位置计算包含框外装饰。极长、极高的 tooltip 超出屏幕时整体缩小以完整显示。
- 错误 JSON、无效坐标、缺失贴图或图片尺寸不符会拒绝重载，并保留上次成功配置。首次加载失败则使用原版 tooltip。

## 重载

在进入世界后执行：

```text
/tooltipstudio reload
/tooltipstudio list
```

`reload` 重读配置与本地 PNG。修改资源包内容后使用 **F3+T**，它也会触发样式重载。
首次安装会写入示例；之后不覆盖你已有的文件，也不会自动恢复你主动删除的样式。
恢复全部默认配置可先备份并移走 `config/tooltipstudio/`，然后重启游戏。

## 按物品指定样式

`config/tooltipstudio/config.json` 示例：

```json
{
  "schemaVersion": 1,
  "enabled": true,
  "defaultStyle": "rare",
  "nbtStyleKey": "TooltipStyle",
  "rules": [
    {"style": "legendary", "priority": 100, "items": ["minecraft:netherite_*"]},
    {"style": "red_book", "priority": 90, "items": ["minecraft:enchanted_book"]},
    {"style": "mythical", "priority": 80, "tags": ["minecraft:swords"]},
    {"style": "epic", "priority": 50, "rarities": ["epic"]},
    {"style": "uncommon", "priority": 30, "rarities": ["uncommon"]}
  ]
}
```

选择顺序：**有效的 NBT 覆盖 → priority 从大到小的首个匹配规则 → defaultStyle**。
同优先级按 JSON 中的先后顺序。`items` / `tags` / `rarities` 各数组内部为“或”，不同条件之间为“且”。
例如同一条规则同时写 `items` 和 `rarities`，需要同时满足物品与稀有度。
省略或空数组表示不限制该项，但一条规则必须至少有一个非空条件数组或非空 `nbt` 对象。
`items` 只支持 `*` 通配符；`tags` 写完整标签 ID，不带 `#`。
稀有度只能使用 `common`、`uncommon`、`rare`、`epic`；`legendary` 等是样式名称，不是原版稀有度。
`enabled: false` 恢复原版 tooltip。`nbtStyleKey: ""` 禁用 NBT 覆盖。

单件物品指定样式（**以下是 1.20.4 的 NBT 命令语法**）：

```mcfunction
/give @s minecraft:diamond_sword{TooltipStyle:"legendary",display:{Name:'{"text":"星辉之刃","italic":false}',Lore:['{"text":"名称居中，下方为自定义分割线","color":"gray","italic":false}','{"text":"同一张图集提供背景、边框和装饰","color":"aqua","italic":false}']},HideFlags:127} 1
```

没有对应样式文件的 NBT 值会被忽略，然后继续匹配规则。

## 按服务器物品 NBT 匹配（0.1.2 新增）

`tags` 指的是 `minecraft:planks` 等物品分类标签；服务器存到物品数据里的 `Monumenta.Location` 等字段使用新的 `nbt` 条件。
在已有 `rules` 数组中添加以下规则，与相邻规则之间用逗号分隔：

```json
{
  "style": "mythical",
  "priority": 200,
  "nbt": {
    "Monumenta.Location": "forest"
  }
}
```

执行 `/tooltipstudio reload` 后，客户端物品 NBT 中 `Monumenta.Location` 精确等于 `forest` 的物品会使用 `mythical`。
不写 `items` 就可以匹配任意原版或模组物品基底，同一种木棍也可根据不同 NBT 选择不同样式。
这是新增的配置选项，**0.1.0 / 0.1.1 不支持，先更新 JAR**。

路径从物品的 `tag` 内部开始：截图命令中的 `SelectedItem.tag.Monumenta.Location` 在这里写成 `Monumenta.Location`。
不要加 `SelectedItem.tag.`、`tag.`、`nbt.` 或 `#` 前缀。点表示嵌套层级；字段名和值都区分大小写。

与你朋友的材质规则对应，限定锁链胸甲并匹配纯文本名字可以写成：

```json
{
  "style": "red_book",
  "priority": 210,
  "items": ["minecraft:chainmail_chestplate"],
  "nbt": {
    "plain.display.Name": "Double Down"
  }
}
```

这里的 `plain.display.Name` 是服务器提供的纯文本字段。原版 `display.Name` 保存的是 JSON 文本字符串，直接匹配它需要填写完整、精确的字符串；本模组不自动把文本 JSON 转成可见名字。
材质包 `.properties` 中的 `nbt.plain.display.Name=Double Down` 不直接粘贴进本模组配置，使用上面的 JSON 写法。

同一规则可以同时限制来源与等级：

```json
{
  "style": "epic",
  "priority": 300,
  "nbt": {
    "Monumenta.Location": ["forest", "valley"],
    "Monumenta.Tier": "artifact"
  }
}
```

- `nbt` 中的不同路径全部满足才匹配；某一路径的候选数组内部满足任意一个即可。
- `nbt` 与 `items` / `tags` / `rarities` 同时填写时，所有条件都要满足。
- 字符串完整、区分大小写地比较，`*` 和正则语法在 NBT 值里没有特殊含义。
- 路径支持原版 NBT 路径语法，例如 `Monumenta.MMLore[0]` 或 `Monumenta.MMLore[]`；路径选出多个值时任一值满足即可。含点的单个字段名应加引号，例如 JSON 键 `"\"key.with.dots\".Location"`。
- 可写数值，例如 `"CustomModelData": 123`。数字按数值比较，不要求 byte/int/long 类型相同；`123` 不等于字符串 `"123"`。布尔值 `true` / `false` 匹配 NBT byte `1b` / `0b`。
- 字段不存在、类型不符合、值不相同，均视为不匹配，继续后续规则。无匹配项时使用 `defaultStyle`。
- 有效的 `TooltipStyle` 仍具有最高优先级。需要纯规则选择时，可设 `nbtStyleKey` 为 `""`。
- 最多每规则 64 条 NBT 路径、每路径 64 个候选值；路径最多 512 字符，单个值最多 4096 字符。空候选数组、null、对象、非法路径会拒绝重载并保留上次配置。

在有命令权限的 **1.20.4 单人测试世界**中，用下面的命令获得测试木棍，不需要设置 `TooltipStyle`：

```mcfunction
/give @s minecraft:stick{Monumenta:{Location:"forest"},display:{Lore:['{"text":"NBT matching test","italic":false}']}}
```

再将 `forest` 改成 `desert` 获取另一根，两者会分别使用 `mythical` 与默认 `rare`（以附带示例配置为准）。
在服务器上直接悬停已有物品即可，客户端重载无需 OP。客户端必须实际收到对应 NBT；截图中的服务端 `/data get` 输出本身不能证明某个字段已同步给客户端。
匹配只读取数据，不改变物品名字、lore、模型、材质或服务器数据。物品模型由原有材质模组/资源包负责。

`examples/nbt-matching/` 提供完整示例配置、单条规则和测试命令。升级不会自动改写已有配置，也不会默认套用服务器专用规则。

## 通过资源包添加样式（1.1 新增）

将样式 JSON 放到 `assets/tooltipstudio/styles/名字.json`，并在 `texture` 中指定资源包 PNG 的资源 ID；样式字段与本地 JSON 完全相同。
如果希望启用包就自动套用，再将匹配规则放到 `assets/tooltipstudio/rules/任意名字.json`，格式为 `{"schemaVersion":1,"rules":[...]}`。
资源包启用后自动重载，修改内容后按 **F3+T**。本地配置不用搬走，资源包不会向 config 目录复制文件。

```text
资源包/
├─ pack.mcmeta
└─ assets/tooltipstudio/
   ├─ styles/monumenta/forest.json
   ├─ rules/my_server.json
   └─ textures/styles/my_forest.png
```

`monumenta/forest.json` 中的贴图路径示例：`"texture": "tooltipstudio:textures/styles/my_forest.png"`。样式路径与贴图路径分别填写，不必同名或同目录。
资源包规则 `my_server.json` 示例：

```json
{
  "schemaVersion": 1,
  "rules": [
    {"style": "monumenta/forest", "priority": 300, "nbt": {"Monumenta.Location": "forest"}}
  ]
}
```

同名样式由资源包覆盖本地定义；多个包的同路径 JSON 按游戏中的资源包顺序选择。不同规则文件合并后先比较 priority，相同 priority 时本地规则优先，资源包规则再按文件路径及数组顺序匹配。
同路径规则文件由高优先级包整份替换，空 rules 数组可屏蔽低优先级包的该文件。全局 enabled、defaultStyle 和 nbtStyleKey 仍由本地配置控制。
无效资源包 JSON 或贴图会保留上次成功配置。停用包时，它自带的规则和独有样式会移除；若本地规则手动引用了被移除的样式，也需要同步调整本地引用。

可直接安装的完整示例和详细用法位于 [examples/resource-pack](examples/resource-pack/README.md)：普通木棍/木板使用 pack_wood，Monumenta.Location 为 forest 的物品使用 monumenta/forest。新版示例包需要 1.2；1.1 的旧示例包仍可在 1.2 中使用。
本功能参考了 [Legendary Tooltips 的资源定义加载方式](https://github.com/AHilyard/LegendaryTooltips/blob/3d3dbacb1fb90dd8480e9323a74a3c0ab6bfa4fc/src/main/java/com/anthonyhilyard/legendarytooltips/config/FrameResourceParser.java)，使用本项目自己的 JSON 格式与实现，不直接读取其 frame_definitions.json。

## 按文件夹分类样式（1.2 新增）

样式 ID 是相对于 `styles/` 的完整路径，去掉末尾 `.json`，使用正斜杠 `/`。例如：

| 样式 JSON 位置 | `style` / `defaultStyle` / `TooltipStyle` 的值 |
| --- | --- |
| `assets/tooltipstudio/styles/forest.json` | `forest` |
| `assets/tooltipstudio/styles/monumenta/forest.json` | `monumenta/forest` |
| `assets/tooltipstudio/styles/monumenta/ring3/forest.json` | `monumenta/ring3/forest` |
| `config/tooltipstudio/styles/monumenta/forest.json` | `monumenta/forest` |

不同目录中的同名文件是不同样式；资源包只覆盖完整样式 ID 相同的本地定义。路径不相对于规则 JSON 所在目录，即使规则文件也分类存放，仍填写从 `styles/` 开始的路径。
每层目录和文件名只用小写英文字母、数字、`_`、`-`。引用时不加 `styles/`、`.json`、命名空间或磁盘路径，不使用反斜杠、空路径段、`.` 或 `..`。本地递归扫描不跟随符号链接。
原有 `forest`、`rare` 等写法无需修改；手工把文件搬到子目录时，需要同步修改规则、defaultStyle 和物品 TooltipStyle 中的引用。仅移动 JSON 不会改变 `texture` 的查找位置。
本地文件改动后运行 `/tooltipstudio reload`；资源包改动后按 **F3+T**。`/tooltipstudio list` 会显示完整分类路径。

## 新建一套自定义样式

最省事的办法：复制 `styles/rare.json` 为 `styles/my_style.json`，从源码或示例包取出 `rare.png`，放到
`config/tooltipstudio/textures/my_style.png`，修改该 JSON 中的 `texture` 为 `local:my_style.png`，然后修改图片并重载。
可以直接把 `defaultStyle` 改为 `my_style`，或者通过规则/NBT 选用它。`examples/custom-style/` 提供了可直接复制的文件。

下面是一份完整的最小样式。坐标与附带 rare 图集一致：

```json
{
  "texture": "local:my_style.png",
  "textureWidth": 128,
  "textureHeight": 128,
  "background": {"u": 24, "v": 0, "width": 1, "height": 1},
  "frame": {
    "region": {"u": 0, "v": 0, "width": 18, "height": 18},
    "left": 4, "top": 4, "right": 4, "bottom": 4
  },
  "separator": {
    "enabled": true,
    "region": {"u": 0, "v": 0, "width": 18, "height": 4},
    "leftCap": 4, "rightCap": 4,
    "inset": 0, "marginTop": 3, "marginBottom": 4
  },
  "padding": {"left": 9, "top": 9, "right": 9, "bottom": 9},
  "minWidth": 100,
  "maxWidth": 320,
  "decorations": [
    {
      "region": {"u": 0, "v": 68, "width": 12, "height": 10},
      "anchor": "TOP", "x": 0, "y": -8,
      "foreground": true
    }
  ]
}
```

所有 `u/v/width/height` 都是 **PNG 原始像素坐标**；`padding`、偏移和宽度是 **Minecraft GUI 像素**，会随 GUI 缩放整体放大。
图集尺寸不限于 128×128，但必须与 `textureWidth/textureHeight` 完全一致，且两边均不超过 4096。
贴图采用最近邻采样，适合像素画；PNG 透明通道保留。

| 字段 | 含义 |
|---|---|
| `texture` | `local:文件名.png` 读取 `config/tooltipstudio/textures/`；或使用 `命名空间:textures/路径.png` 读取资源包 |
| `background` | 一个区域拉伸填满整个面板；想要透明背景可指向全透明像素 |
| `frame.region` | 完整九宫格边框区域；中心不绘制，由 `background` 负责填充 |
| `frame.left/top/right/bottom` | 源区域四边固定宽度；中间必须至少有一个源像素 |
| `separator.region` | 分割线完整源区域，包括左端、中段、右端 |
| `separator.leftCap/rightCap` | 固定两端的源宽度与显示宽度；两者可不同，也可为 0 |
| `separator.inset` | 分割线相对正文区域左右各缩进多少像素 |
| `separator.marginTop/marginBottom` | 分割线与文字行区域之间的间距 |
| `padding` | 正文与整个面板外缘的距离；应至少等于边框宽度 |
| `minWidth/maxWidth` | 正文区域的最小宽度与换行宽度，不包含 padding；过窄的屏幕会降低正常换行宽度 |
| `decorations` | 装饰数组，可为空 `[]`，最多 64 个区域 |

分割线的显示高度就是 `region.height`，不会因面板高度变化而拉伸。它的三段关系如下：

```text
源区域:   [ leftCap ][ region.width - leftCap - rightCap ][ rightCap ]
显示区域: [ 保持宽度 ][          横向拉伸中段             ][ 保持宽度 ]
```

示例分割线暂时借用边框的顶部纹理条；你可以在同一张图的空白区域绘制独立分割线，然后只修改 `separator.region` 和两端宽度。
两端的像素尺寸在常规 tooltip 布局中固定。极端情况下的整体缩小会同时缩小文字、分割线和所有装饰，保持整体比例。

装饰 `anchor` 可为 `TOP_LEFT`、`TOP`、`TOP_RIGHT`、`LEFT`、`CENTER`、`RIGHT`、`BOTTOM_LEFT`、`BOTTOM`、`BOTTOM_RIGHT`。
锚点按**装饰与面板的对应边或中心**对齐，再加 `x/y` 偏移。例如 `TOP` 将装饰水平居中、顶部对齐；负 `y` 向框外上方移动。
`foreground: false` 在文字前绘制；`true` 在文字后绘制。装饰按数组顺序叠加。

## 替换内置贴图：资源包方式

内置样式引用 `tooltipstudio:textures/styles/rare.png` 等资源。
建立资源包并使用相同路径覆盖即可；1.20.4 的 `pack.mcmeta` 示例：

```json
{"pack":{"pack_format":22,"description":"Tooltip Studio custom artwork"}}
```

```text
我的资源包/
├─ pack.mcmeta
└─ assets/tooltipstudio/textures/styles/
   └─ rare.png
```

修改图片尺寸或切片布局后，记得同步调整配置 JSON。默认每种样式都只读取对应的一张图片，不依赖极光模组本体。

## 示例样式与素材

附带 `uncommon`、`rare`、`epic`、`legendary`、`mythical`、`cat_bell`、`red_book`、`tslat_sword`、`dream_dyeing_crystal_fragment` 共 9 套。
默认普通物品使用 `rare`，下界合金系列使用 `legendary`；其余按配置中的稀有度规则选择。
素材来自你指定的极光模组目录，作为临时示例使用，见 `THIRD_PARTY_ASSETS.md`。

## 构建和验证

使用 **JDK 17**（或 JDK 21）与附带 Gradle Wrapper。不要用系统默认的 JDK 25 运行 Gradle 8.6。
Windows PowerShell 示例：

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
.\gradlew.bat build
```

产物在 `build/libs/tooltip-studio-1.2+1.20.4.jar`。模组元数据中的版本为 `1.2`，文件名中的 `+1.20.4` 表示适用的 Minecraft 版本。`-sources.jar` 是源码，不是游戏加载的模组文件。
构建也会生成 `build/resourcepacks/tooltip-studio-example-pack-1.2+1.20.4.zip`，可直接放进游戏的 resourcepacks 文件夹启用。
第一次构建需要联网下载 Gradle、Minecraft 开发依赖和 Fabric。

- `gradlew.bat test`：验证示例 JSON、纹理区域边界、通配符、错误配置、分割线端点保持，以及 NBT 路径、值类型、只读匹配与缺失字段处理。
- `gradlew.bat runClient`：启动正常开发客户端。
- `gradlew.bat runSmoke`：启动专用渲染测试客户端，验证样式选择和热重载，并自动截图后退出。测试模组不进入发布 JAR。
- `python tools/pack_aurorian.py "原始贴图目录"`：重新打包用户提供的像素素材，需 Pillow；只读取原文件。

渲染测试源码保存在 `src/smoke/`；发行版只打包 `src/main/`。

## 兼容范围与后续版本

本版只适配 **1.20.4**，并在元数据中严格限制此版本。1.20.5 之后的物品数据组件与后续渲染 API 需要分别适配，不能把版本范围改宽就当作支持 1.21.4。
配置格式与切片运算可在后续移植中保留；NBT 路径匹配和物品数据读取需随 Minecraft 版本适配。

此模组使用可串联的 WrapMethod 传递物品上下文，在最终绘制入口应用样式。背包原方法及其他模组对内部调用的 Redirect 保持执行，覆盖普通背包、创造背包、容器中的物品提示。
Fabric 的物品文字 tooltip 回调继续生效。0.1.1 修复了与 Shulker Box Tooltip 4.1.0+1.20.4 的启动冲突，并为其框内/框外预览和锁定物品样式提供兼容处理。直接自行绘制 tooltip 的第三方界面或其他样式模组仍可能需要额外适配。

开发接口参考：[Fabric Yarn 1.20.4 DrawContext](https://maven.fabricmc.net/docs/yarn-1.20.4+build.3/net/minecraft/client/gui/DrawContext.html)、[TooltipComponent](https://maven.fabricmc.net/docs/yarn-1.20.4+build.3/net/minecraft/client/gui/tooltip/TooltipComponent.html)。具体构建和客户端验证见 `VERIFICATION.md`。

## 从 1.0 或 0.1.x 更新

从 mods 文件夹移除旧版 Tooltip Studio JAR，放入 `tooltip-studio-1.2+1.20.4.jar`，同一时间只保留一个版本。已有 JSON 与 PNG 配置无需迁移。需要 NBT 匹配功能时在规则中添加 `nbt`。MixinExtras 0.4.1 已内嵌，无需另外安装；Shulker Box Tooltip 仍是可选模组。

开发者兼容回归：`gradlew.bat runSmoke -PcompatShulker`。此开关仅向测试运行加入 Shulker Box Tooltip 4.1.0 与 Cloth Config 13.0.121，不会打包它们。

## 日常维护

主分支为 `main`，`v1.0` 标签标记首个正式版本。建议每个功能使用独立分支，修改后运行 `gradlew.bat build`，提交并推送到 GitHub。
GitHub Actions 会在推送到 `main` 或发起 Pull Request 时使用 Java 17 自动构建、执行测试，并保存 JAR 和示例资源包 ZIP 产物 14 天；也可在 Actions 页面手动运行。
图形客户端测试 `runSmoke` 需要本地图形环境，不在自动构建中运行。

仓库包含源码、示例配置、贴图、Gradle Wrapper、测试和说明文档。构建缓存、运行目录、游戏日志与存档由 `.gitignore` 排除。
更新版本时修改 `gradle.properties` 中的 `mod_version`，同步更新 README 和 CHANGELOG；Minecraft 适配版本仍单独维护。
