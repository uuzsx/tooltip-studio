# Tooltip Studio 1.6：分割线锚点、文字装饰、XY 缩放与偏移

适用于 Fabric / Minecraft 1.20.4。移除旧版 Tooltip Studio JAR，安装 `tooltip-studio-1.6+1.20.4.jar`，保留 Fabric API。

## 直接试用资源包

将 `tooltip-studio-advanced-decoration-pack-1.6+1.20.4.zip` 放入游戏的 resourcepacks 并启用。本包只提供装饰和规则，默认款与其他自定义款均可叠加，不替换背景、边框、分割线或正文。修改资源包后按 F3+T。

在有命令权限的 1.20.4 测试世界执行：

```mcfunction
/give @s minecraft:stick{TooltipDecorations:"advanced",display:{Name:'{"text":"装饰测试","italic":false}',Lore:['{"text":"分割线、文字和缩放测试","italic":false}']}}
```

鼠标悬停后，分割线两端各有一把横向 1.5 倍、纵向 0.75 倍的剑，中间显示金色“◆ 传说 ◆”，底部显示两行 0.8 倍文字。剑使用之前提供的 test.png，未改动图片内容。
将命令中的 advanced 改成 separator_only，可只看分割线装饰。测试命令带有 lore，确保出现分割线；只有标题且没有其他正文或物品预览时，分割线及其装饰都会隐藏，底部装饰仍显示。

## 新锚点的位置

| anchor | x=0、y=0 时的位置 |
| --- | --- |
| SEPARATOR_LEFT | 装饰左边缘对齐实际分割线左端，垂直居中于分割线 |
| SEPARATOR_CENTER | 装饰中心对齐实际分割线中心 |
| SEPARATOR_RIGHT | 装饰右边缘对齐实际分割线右端，垂直居中于分割线 |

位置会计入 separator.inset、分割线高度以及名称换行后的高度；不是整个面板的左边、右边或中心。
原有 TOP_LEFT、TOP、TOP_RIGHT、LEFT、CENTER、RIGHT、BOTTOM_LEFT、BOTTOM、BOTTOM_RIGHT 全部保留。
`x` 正右负左，`y` 正下负上，默认 0，整数范围 -256..256。装饰的 x/y 始终是这个方向，不跟随整框 offsetXMode 反转。

## 图片装饰与大小缩放

下面是完整的独立图片装饰定义，例如 `assets/tooltipstudio/decorations/advanced/left.json`：

```json
{
  "type": "texture",
  "texture": "tooltipstudio:textures/decorations/sword.png",
  "textureWidth": 128,
  "textureHeight": 128,
  "region": {"u": 0, "v": 0, "width": 16, "height": 16},
  "anchor": "SEPARATOR_LEFT",
  "x": -6,
  "y": 0,
  "foreground": true,
  "x_scale": 1.5,
  "y_scale": 0.75
}
```

type 可省略，默认 texture。源图上的 region 不变，上例最终显示为 24×12 GUI 像素。x_scale 与 y_scale 可各自省略，各自默认 1；支持小数，范围 0.0625 到 16，不能为 0 或负数。
缩放后仍按锚点对齐，例如 SEPARATOR_RIGHT 始终将缩放后图片的右边缘对齐分割线右端。x/y 在锚点对齐后应用，不随这两个缩放值放大。超大 tooltip 为适应屏幕而整体缩小时，图片、文字及相对位置会随整框一起缩小。

## 纯文字装饰

例如 `assets/tooltipstudio/decorations/advanced/title.json`：

```json
{
  "type": "text",
  "text": "◆ 传说 ◆",
  "color": "#FFD866",
  "bold": true,
  "italic": false,
  "shadow": true,
  "anchor": "SEPARATOR_CENTER",
  "x": 0,
  "y": 0,
  "foreground": true,
  "x_scale": 1,
  "y_scale": 1
}
```

文字装饰不需要 texture、textureWidth、textureHeight 或 region。text 是直接显示的文字，支持中文与 `\n` 换行；例如 `"text":"Monumenta\n自定义装饰文字"`。它是固定文字，不会自动读取物品 NBT 或替换变量，也不解析 JSON 文本组件。
使用游戏当前字体；color 可省略，默认白色，填写时用 #RRGGBB。bold / italic 默认 false，shadow 默认 true。最多 1024 个字符、16 行，不接受空白文字。多行文字按最长一行的整体矩形锚定，行内左对齐。
图片和文字的 foreground 默认 false，在面板之后、分割线和正文之前画；true 在正文之后叠加。装饰不占据正文排版空间，可能覆盖现有内容，可用 x/y 调整位置。

## 匹配条件、分类目录与本地配置

规则文件 `assets/tooltipstudio/rules/advanced_decorations.json` 可以写成：

```json
{
  "schemaVersion": 1,
  "decorationRules": [
    {
      "decorations": ["advanced/left", "advanced/title", "advanced/right"],
      "priority": 300,
      "nbt": {"Monumenta.Location": "forest"}
    }
  ]
}
```

这是 Monumenta 条件的完整替换示例；本包自带规则默认匹配上方测试命令的 TooltipDecorations。条件也可使用已有 items、tags、rarities；不写 items 就不限原版物品基底。原有 rules 继续选择基础样式，decorationRules 只叠加装饰。多个命中会叠加，相同装饰 ID 去重。

资源包目录如下，所有 JSON 均支持多层分类路径：

```text
pack.mcmeta
assets/tooltipstudio/
├─ decorations/advanced/left.json
├─ decorations/advanced/title.json
├─ decorations/advanced/right.json
├─ decorations/advanced/footer.json
├─ rules/advanced_decorations.json
└─ textures/decorations/sword.png
```

使用本地配置时，将装饰 JSON 放到 `config/tooltipstudio/decorations/advanced/`，图片放到 `config/tooltipstudio/textures/decorations/sword.png`，图片引用改为 `local:decorations/sword.png`。把 decorationRules 数组里的规则加入已有 config.json 的同名数组，不要覆盖整个配置。之后执行 `/tooltipstudio reload`；`/tooltipstudio decorations` 可查看完整装饰 ID。

这些新参数也适用于样式 JSON 自带的 decorations 数组。图片使用基础样式自身图集，只填写 region，不填写单独的 texture；文字与上述 text 示例完全相同。独立图片装饰则可以用自己的 PNG。

## 整个 tooltip 的左右间距

在正在使用的样式 JSON 最外层添加以下字段片段，与 texture 同级：

```json
"offsetX": 24,
"offsetY": -12,
"offsetXMode": "cursor"
```

正 offsetX 远离鼠标，负值靠近鼠标。tooltip 出现在鼠标右侧时正值向右，在左侧时正值向左；相同值会保持左右一致的鼠标间距。offsetY 仍是正值向下、负值向上。上例表示两侧都远离鼠标 24 GUI 像素，并向上移动 12 GUI 像素。
offsetXMode 可省略，默认 cursor。升级到 1.6 后，已有非零 offsetX 也采用新方向；如果要保留 1.5 的固定正右负左行为，设置 `"offsetXMode":"screen"`。
offsetX / offsetY 默认 0，范围 -4096..4096 的整数。它们移动整框及所有装饰，不改变正文布局。屏幕四边仍留出 4 GUI 像素，并计入框外装饰；贴边时实际位移和鼠标间距会受可用空间限制。

本地修改后运行 `/tooltipstudio reload`，资源包修改后按 F3+T。无效锚点、非法缩放、错误文字或缺失 PNG 会拒绝本次重载并保留上一份成功状态，具体文件见日志。
