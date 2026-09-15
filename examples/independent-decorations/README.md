# Tooltip Studio 1.5：按条件叠加独立装饰

适用于 Fabric / Minecraft 1.20.4。独立装饰只叠加图片，不替换当前 tooltip 的背景、边框、分割线或文字布局。默认款、本地自定义样式、资源包样式和通过 TooltipStyle 选中的样式都支持。

## 最快测试：启用示例资源包

1. 移除旧版模组 JAR，安装 `tooltip-studio-1.5+1.20.4.jar`。
2. 将 `tooltip-studio-decoration-pack-1.5+1.20.4.zip` 放入游戏的 resourcepacks 文件夹，在游戏中启用。
3. 鼠标悬停云杉木门：左上角会出现小剑。普通木棍不会添加装饰。

该包不包含 styles JSON，也不修改本地 config.json，只提供独立装饰与匹配规则。
剑 PNG 原样取自用户提供的 test.png：整图 128×128，取左上角 16×16 的剑区域，不把整块透明画布当成装饰尺寸。
示例同时提供右上、左下、右下定义，都使用这张剑，不做旋转或镜像；以后可以为各角替换不同图片或区域。

有权限的 1.20.4 单人测试世界可使用：

```mcfunction
/give @s minecraft:spruce_door
/give @s minecraft:stick{Monumenta:{Location:"forest"}}
/give @s minecraft:stick{TooltipDecorations:"all_corners"}
```

第一、第二件显示左上角剑；第三件显示四角剑。这里的 TooltipDecorations 是示例规则使用的普通 NBT 字段，不是模组内置的强制覆盖键。
服务器物品需由客户端实际收到对应 NBT。原来选中的 tooltip 样式继续保留，启用示例包不会将所有物品换成默认款。

## 本地添加装饰

将本示例的 `decorations` 和 `textures` 文件夹合并到游戏实例 `config/tooltipstudio/` 下。
把 decoration-rules.json 中的 decorationRules 数组加入已有 config.json 的最外层，与 rules 平级。已有同名数组则合并所需规则；不要用该片段替换整个配置。

目录示例：

```text
config/tooltipstudio/
├─ config.json
├─ decorations/sword/top_left.json
└─ textures/decorations/sword.png
```

装饰 JSON：

```json
{
  "texture": "local:decorations/sword.png",
  "textureWidth": 128,
  "textureHeight": 128,
  "region": {"u": 0, "v": 0, "width": 16, "height": 16},
  "anchor": "TOP_LEFT",
  "x": -6,
  "y": -6,
  "foreground": true
}
```

装饰 ID 是相对于 decorations/ 的路径，去掉 .json，例如 sword/top_left。每层名字用小写字母、数字、下划线或连字符。它与同名的样式 ID 互不影响。

| 参数 | 含义 |
| --- | --- |
| texture | 本地 PNG 可用 local:，从 config/tooltipstudio/textures/ 开始；也可直接引用资源 ID |
| textureWidth / textureHeight | 整张 PNG 的实际宽高，1..4096；本例为 128，不是剑区域的 16 |
| region | 从 PNG 中取出的区域；宽高也是装饰的 GUI 尺寸，不拉伸整张透明画布 |
| anchor | 相对于 tooltip 面板的位置，随面板宽高自动移动 |
| x / y | 相对于锚点的偏移，默认 0，整数 -256..256；X 正右负左，Y 正下负上 |
| foreground | true 在文字和物品预览之后绘制；false（默认）在框体之后、分割线和文字之前绘制 |

## 四角位置

锚点以装饰矩形的对应边对齐面板边缘。例如 TOP_RIGHT 会将装饰的右边对齐面板右边，BOTTOM_RIGHT 同时将底边对齐面板底边。

| 位置 | anchor | 示例 x | 示例 y |
| --- | --- | --- | --- |
| 左上 | TOP_LEFT | -6 | -6 |
| 右上 | TOP_RIGHT | 6 | -6 |
| 左下 | BOTTOM_LEFT | -6 | 6 |
| 右下 | BOTTOM_RIGHT | 6 | 6 |

还支持 TOP、LEFT、CENTER、RIGHT、BOTTOM。设 x/y 为 0 时，装饰矩形位于面板对应角的内侧；示例的 6 像素偏移让它部分伸出边框。
独立装饰会与面板一起移动、缩放，并参与屏幕边缘限制。框外装饰会增加整框占用范围，贴边时面板可能整体调整位置，以避免装饰出屏。

## 按条件添加，不改变样式

在 config.json 最外层加入：

```json
"decorationRules": [
  {
    "decorations": ["sword/top_left"],
    "priority": 200,
    "nbt": {"Monumenta.Location": "forest"}
  }
]
```

这段是已有配置的字段片段，注意与前后字段之间的逗号。这里写 decorations，不写 style；原有 rules 数组仍单独负责选择基础样式。
支持与样式规则相同的 items（含 `*`）、tags、rarities、nbt。不同条件及不同 NBT 路径之间为“且”，数组内部为“或”；每条规则至少有一个条件。
例：`"items":["minecraft:spruce_door"]` 匹配云杉木门；`"tags":["minecraft:planks"]` 匹配木板分类。NBT 路径从物品 tag 内部开始，不加 SelectedItem.tag.；值精确且区分大小写。

**所有命中的装饰规则都会叠加。** 一条规则可在 decorations 数组里列多个 ID，多条规则也可分别添加四个角。同一个 ID 即使命中多次也只绘制一次。
同一绘制层内，高 priority 显示在上方；同 priority 本地规则优先，然后按资源文件路径与数组顺序，较前者在上方。单条规则数组中较前的 ID 在上方。foreground 的前后层划分优先于 priority。
每条规则最多 64 个 ID，所有规则合计最多 4096 条；单次 tooltip 最多叠加 64 个不同 ID，超过时按优先级保留前 64 个。

基础样式中的 decorations 仍使用基础图集、继续绘制；独立装饰使用自己的 PNG。在同一层中，独立装饰在基础样式自带装饰之后绘制。
有效 TooltipStyle 只决定基础样式，不会阻止独立装饰规则。enabled=false 则关闭整个模组效果，包括独立装饰。

## 放进资源包

```text
ZIP 根目录/
├─ pack.mcmeta
└─ assets/tooltipstudio/
   ├─ decorations/sword/top_left.json
   ├─ textures/decorations/sword.png
   └─ rules/independent_decorations.json
```

装饰 JSON 格式相同，将 texture 改成 `tooltipstudio:textures/decorations/sword.png`。资源包不允许 local:，可引用其他合法命名空间的 PNG。
规则文件可以只写装饰规则：

```json
{
  "schemaVersion": 1,
  "decorationRules": [
    {"decorations":["sword/top_left"],"priority":200,"nbt":{"Monumenta.Location":"forest"}}
  ]
}
```

同一个文件也可包含原有 rules。高优先级包会整份覆盖同路径规则文件，包括其中的两种规则；用空数组文件可以屏蔽低优先级包同路径规则。
同路径装饰定义由资源包覆盖本地，再按资源包优先级选择；不同分类目录里的同名文件独立存在。装饰和样式各有自己的定义目录。

## 重载与升级

本地 JSON/PNG 修改后执行 `/tooltipstudio reload`，用 `/tooltipstudio decorations` 查看已加载的装饰 ID；`/tooltipstudio list` 继续列出基础样式。
资源包修改后按 F3+T，启用、停用或排序也会重载。停用包后，其独有装饰与规则一起移除；如果本地规则引用该包的装饰，应同时移除或替换该引用。
错误 JSON、失效装饰引用、缺失 PNG 或越界区域会拒绝本次重载，保留上一份完整有效配置。
旧版配置不写 decorationRules 也照常工作，模组不会自动添加匹配规则或修改已有样式。默认 JAR 仍只内置默认款 tooltip，剑图片仅在示例文件和可选资源包内。
