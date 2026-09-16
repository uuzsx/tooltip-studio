# Tooltip Studio 1.6 资源包示例

适用于 Fabric / Minecraft 1.20.4。把 ZIP 放入 resourcepacks 并在游戏中启用，ZIP 根目录直接包含 pack.mcmeta（pack_format=22）。
本包只使用与模组相同的默认 PNG，没有极光素材。示例样式 monumenta/forest 保留基础款外观，设置 offsetY=-12，用位置变化演示规则匹配。

```text
pack.mcmeta
pack.png
assets/tooltipstudio/styles/monumenta/forest.json
assets/tooltipstudio/rules/example.json
assets/tooltipstudio/textures/styles/default.png
```

三条规则分别匹配 Monumenta.Location=forest（priority 300）、minecraft:planks 标签（200）、普通木棍 ID（190），均选用 monumenta/forest。其他物品继续使用现有规则和默认样式。
若有有效 TooltipStyle 或更高优先级本地规则，仍优先按它们选择。

## 自己添加样式

样式 JSON 可以多层分类，相对于 styles/ 的路径去掉 .json 就是 ID：monumenta/forest.json 写成 `"style":"monumenta/forest"`。
名字用小写英文字母、数字、下划线、连字符，引用使用 `/`，不加 styles/、.json、命名空间或磁盘路径。不同目录中的同名文件是不同样式。
每套样式只引用一张 PNG，背景、边框、分割线和装饰从中取区域；texture 可以使用任意合法资源 ID，例如 `myserver:textures/tooltips/forest.png`，但资源包不能使用 local:。

在 rules/ 下添加 JSON：

```json
{
  "schemaVersion": 1,
  "rules": [
    {"style":"monumenta/forest","priority":300,"nbt":{"Monumenta.Location":"forest"}}
  ]
}
```

支持 items、tags、rarities、nbt；不同条件为“且”，同一数组为“或”。NBT 从物品 tag 内部开始，精确区分大小写。全局 enabled、defaultStyle、nbtStyleKey 仍在本地 config.json。

## 偏移、覆盖与重载

在样式最外层添加 offsetX / offsetY。X 默认正值远离鼠标、负值靠近，左右自动反向；offsetXMode=screen 时固定正右负左。Y 正下负上，各自默认 0，取值 -4096..4096，单位 GUI 像素。整框一起移动，屏幕四边保留 4 GUI 像素，到边缘时限制实际位移。
本例 offsetY=-12，普通物品匹配后向上移动 12，默认样式不偏移。

资源包同名样式覆盖本地；多个包同路径文件以游戏资源包优先级选择。相同路径的规则文件整份替换，不同文件合并后按 priority 匹配；同 priority 本地规则优先，再按资源文件路径和数组顺序。
修改包后按 F3+T。启用、停用或重排时自动重载，不复制或改写本地配置。停用后自带规则与独有样式移除；本地手工引用包内样式时，应同步调整引用。
无效 JSON、无效引用或缺失 PNG 会保留上次成功状态。规则合计最多 4096 条。

## 单人世界测试

```mcfunction
/give @s minecraft:stick
/give @s minecraft:diamond{Monumenta:{Location:"forest"},display:{Lore:['{"text":"Resource pack tooltip","italic":false}']}}
```

两者均使用向上偏移的 monumenta/forest；不带该 NBT 的普通钻石使用 default。服务器物品需要客户端实际收到对应 NBT。
使用 Tooltip Studio 自己的 JSON 格式，不读取 Legendary Tooltips 的 frame_definitions.json。
