# Tooltip Studio 1.7：一个装饰拼接多段彩色文字

适用于 Fabric / Minecraft 1.20.4。替换旧模组为 `tooltip-studio-1.7+1.20.4.jar`，同一实例只保留一个版本；保留 Fabric API。原有单段 text 写法、图片装饰、匹配规则和资源包均继续兼容。

## 完整装饰示例

下面是一个完整的装饰 JSON：前半段为灰色，后半段为红色。两段按数组顺序紧接着绘制，作为一个整体居中、移动、缩放。

```json
{
  "type": "text",
  "segments": [
    {"text": "Architect's Ring : ", "color": "#555555"},
    {"text": "Artifact", "color": "#FF5555"}
  ],
  "anchor": "BOTTOM",
  "x": 0,
  "y": 14,
  "shadow": true,
  "foreground": true,
  "x_scale": 1,
  "y_scale": 1
}
```

本例放在面板底部外侧，方便直接观察。需要其他位置时改 anchor、x、y 即可，例如 `"anchor":"SEPARATOR_CENTER","y":0` 放在分割线中央；没有分割线时会隐藏该锚点上的装饰。此功能叠加固定文字，不会替换或修改原物品 lore。

资源包内保存为 `assets/tooltipstudio/decorations/labels/artifact.json`；本地则保存为 `config/tooltipstudio/decorations/labels/artifact.json`。装饰 ID 都是 `labels/artifact`，不需要 PNG。

## 各段的格式

| 字段 | 含义 |
| --- | --- |
| segments | 1～64 段，按顺序拼接；与原来的 text 字符串二选一 |
| 每段的 text | 必填的文字；支持中文与 `\n` 换行，不自动插入空格 |
| 每段的 color | 可选，格式 #RRGGBB；省略时继承装饰外层 color |
| 每段的 bold / italic | 可选布尔值；省略时继承装饰外层设置，false 可取消继承的粗体或斜体 |

每一段都独立继承外层设置，不继承前一段的颜色或格式。外层默认白色、非粗体、非斜体；shadow 默认 true，统一控制整个装饰的阴影。
anchor、x/y、foreground、shadow、x_scale/y_scale 写在装饰外层，共同作用于所有段落。不要把它们写进单个 segment。x/y 以整体为单位移动；x_scale/y_scale 省略为 1。
全部段落拼接后最多 1024 个字符、16 行，不能全部为空白。换行后保留各段格式，各行左对齐，以最长一行的宽度计算整体锚点；不会在每段之间另加间距，也不会自动换行。文字是固定内容，不解析 NBT 占位符或 Minecraft JSON 文本组件。

以下例子演示继承、粗体和换行：

```json
{
  "type": "text",
  "color": "#AAAAAA",
  "segments": [
    {"text": "Masterwork : "},
    {"text": "★★★★", "color": "#FFD24A", "bold": true},
    {"text": "\nRekkengulch", "color": "#C06A38", "italic": true}
  ],
  "anchor": "BOTTOM",
  "y": 26,
  "foreground": true,
  "x_scale": 0.9,
  "y_scale": 0.9
}
```

旧的 `"text":"单段文字"` 原样保留。使用 segments 时删除外层 text，不要同时填写两者。

## 匹配规则

资源包规则文件示例 `assets/tooltipstudio/rules/segmented_text.json`：

```json
{
  "schemaVersion": 1,
  "decorationRules": [
    {
      "decorations": ["labels/artifact"],
      "priority": 300,
      "nbt": {"Monumenta.Location": "forest"}
    }
  ]
}
```

以上是按 Monumenta.Location 匹配的替换示例。本包实际自带规则使用下方测试命令中的 TooltipTextDemo，便于离线试用。
本地配置把该规则加入已有 config.json 的 decorationRules 数组即可，不要覆盖整份配置。ID、tag、稀有度、NBT 及多层分类路径沿用现有机制，所有基础样式均可叠加。

## 直接试用示例资源包

将 `tooltip-studio-segmented-text-pack-1.7+1.20.4.zip` 放入游戏的 resourcepacks 文件夹并启用。在有命令权限的 1.20.4 测试世界执行：

```mcfunction
/give @s minecraft:leather_boots{TooltipTextDemo:"artifact",HideFlags:127,display:{Name:'{"text":"Rooted Walkers","italic":false}',Lore:['{"text":"一个装饰：灰色前缀与红色 Artifact","italic":false}']}}
/give @s minecraft:leather_boots{TooltipTextDemo:"masterwork",HideFlags:127,display:{Name:'{"text":"多段文字测试","italic":false}',Lore:['{"text":"继承颜色、金色星星、换行与斜体","italic":false}']}}
```

本包只添加两份文字装饰与对应规则，沿用当前物品的基础样式，没有添加新背景或贴图。第一件展示灰色与红色拼接，第二件展示灰色前缀、金色粗体星星和棕色斜体第二行。

本地 JSON 修改后执行 `/tooltipstudio reload`，资源包修改后按 F3+T；`/tooltipstudio decorations` 可查看已加载的装饰 ID。无效段落、错误颜色、超限内容或同时填写 text 与 segments 会拒绝本次重载，保留上一份有效配置，具体文件与错误可在日志中查看。

也可把完整装饰对象加入样式 JSON 的 decorations 数组，此时随该样式显示，不需要额外的 decorationRules。独立装饰和样式内装饰的 segments 格式完全相同。
