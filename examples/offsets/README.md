# Tooltip Studio 1.4：整个 tooltip 的 XY 偏移

适用于 Fabric / Minecraft 1.20.4。先移除旧版 JAR，安装 `tooltip-studio-1.4+1.20.4.jar`，同一时间只保留一个版本。自定义样式、分类目录与 NBT 规则仍受支持；本版已移除旧极光预设和 PNG，升级处理见 [基础款说明](../base-style/README.md)。

## 往上移动一点

打开正在使用的样式 JSON，例如资源包里的 `assets/tooltipstudio/styles/monumenta/forest.json`，或者本地的 `config/tooltipstudio/styles/monumenta/forest.json`。

在最外层添加以下字段，注意与前后字段之间的逗号，其他内容保持原样：

```json
"offsetX": 0,
"offsetY": -12
```

这是添加到已有样式中的片段，不是完整样式文件。参数与 `texture`、`minWidth`、`decorations` 同级；不要放进 `rules`、`padding`、`separator` 或单个装饰对象中。

上述设置让整个 tooltip 向上移动 12 个 GUI 像素。觉得不够就把 `offsetY` 改为 `-20`，想回到原位则改成 `0` 或删除这个字段。

| 想要的方向 | 填写方式 |
| --- | --- |
| 向上 | `"offsetY": -12` |
| 向下 | `"offsetY": 12` |
| 向左 | `"offsetX": -16` |
| 向右 | `"offsetX": 16` |
| 向右并向上 | `"offsetX": 16, "offsetY": -12` |

两个参数都可省略，也可只填一个；默认均为 0。取值为 -4096 到 4096 的整数，每套样式分别设置。

## 重载与生效范围

- 本地配置修改后：在游戏里执行 `/tooltipstudio reload`。
- 资源包修改后：按 **F3+T**，或在资源包菜单中重新加载该包。
- 同名样式由资源包覆盖本地时，需要修改当前生效的资源包样式。
- 边框、背景、物品名称、分割线、lore、装饰和框内物品预览一起移动；只改变屏幕位置，不改变它们之间的布局。
- 原有 `style`、`defaultStyle`、物品 `TooltipStyle` 继续选择样式，无需为偏移修改匹配规则。所有选中这套样式的物品都会使用它的偏移值。

偏移是从原有正常定位结果开始移动，不会重新触发鼠标左右侧的自动选择。单位为 Minecraft GUI 像素，随游戏的界面缩放变化；例如 GUI 缩放为 2 时，12 个 GUI 像素通常对应 24 个屏幕像素。不是 PNG 图集中的坐标。

屏幕边缘仍留出 4 个 GUI 像素，计算时包含框外装饰。偏移过大时会停在边缘，不把提示框移出画面；已贴近顶部时，继续减小 offsetY 可能看不到变化。特别长的 tooltip 自动缩小时，偏移数值仍以屏幕 GUI 像素计算；若已占满屏幕高度，就没有继续上下移动的空间。

不填写新参数的旧样式位置保持不变。非法数值导致重载失败时会保留上次成功的样式，可查看命令错误消息或日志定位文件。

## 现成的资源包示例

本版 `tooltip-studio-example-pack-1.4+1.20.4.zip` 中的 `monumenta/forest` 已设置 `offsetX: 0`、`offsetY: -12`，匹配客户端物品 NBT 的 `Monumenta.Location = forest`。
内置 `default` 不设置偏移，使用同一张 PNG。可在有权限的 1.20.4 单人测试世界获取两个仅覆盖样式 ID 不同的物品进行比较：

```mcfunction
/give @s minecraft:diamond{TooltipStyle:"default",display:{Name:'{"text":"原来位置","italic":false}'}}
/give @s minecraft:diamond{TooltipStyle:"monumenta/forest",display:{Name:'{"text":"向上移动 12","italic":false}'}}
```

若只想调整已有样式，无需安装示例包，直接添加这两个参数即可。
