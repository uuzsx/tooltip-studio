# Tooltip Studio 1.7 懒人教学配套包

适用：Fabric / Minecraft 1.20.4。作者：幼幼紫、千村。

## 第一次试

1. 将 tooltip-studio-1.7+1.20.4.jar 与适用于 1.20.4 的 Fabric API 放入游戏实例的 mods 文件夹，只保留一个版本的 Tooltip Studio。
2. 将 tooltip-studio-beginner-pack-1.7+1.20.4.zip 放入同一实例的 resourcepacks 文件夹，在“选项 → 资源包”中启用。初次跟教程时只启用这一个 Tooltip Studio 示例包。
3. 拿一根普通木棍，鼠标悬停。底部会出现“区域 : 传说”，两个部分颜色不同。没有命令权限也能用普通木棍测试。
4. 普通木棍通常只有名称，所以没有分割线。想同时看到分割线，可以在有命令权限的 1.20.4 测试世界执行文末 /give 指令。

本包只匹配 minecraft:stick。基础款沿用模组内置 default.png，不分发新贴图；将正文最小宽度设为 180，方便观察。不会修改物品原有名称、lore 或服务器数据。

## 开始修改

将 ZIP 解压到 resourcepacks/TooltipStudio-新手包/，确认该文件夹内直接有 pack.mcmeta 和 assets。然后在资源包菜单启用这个文件夹版本，停用原 ZIP，避免同时存在两份而改错。

在文件夹中用文本编辑器打开 JSON。保存为 UTF-8，注意后缀是 .json，不是 .json.txt；使用英文双引号，不写注释，最后一项后面不加逗号。每次保存后回游戏按 F3+T，再悬停看效果。

下列三个路径都从 assets/tooltipstudio/ 开始：

- decorations/demo/label.json：改字与颜色。改 text 的值就能换字，改 color 就能换颜色。多色用 segments，单色也可以删掉 segments 并使用外层 text；两种写法二选一。每段可加 bold:true、italic:true。
- rules/demo.json：改匹配。把两处 minecraft:stick 都换成 minecraft:diamond_sword，样式和文字装饰就会一起作用于钻石剑。rules 选基础外观，decorationRules 叠加装饰，互不替代。
- styles/demo.json：改整体外观与位置。offsetY 改为 -12 会把整框上移；offsetX 正值让两侧都远离鼠标，负值靠近。保持原 PNG 和切片时可先只调位置。

## 文件名就是引用名

styles/demo.json → style 填 demo；decorations/demo/label.json → decorations 填 demo/label。
引用不带 styles/ 或 decorations/，不带 .json。路径用 /，文件名用小写英文、数字、下划线或连字符。新增子文件夹后，在规则里同步写完整分类路径。

## 服务器按 NBT 匹配

若要所有基底、只按 Monumenta.Location=forest 匹配，把需要改变的那条规则里的 items 字段删除，改成下面这段字段：

```json
"nbt": {"Monumenta.Location": "forest"}
```

完整示例（可替换本包 rules/demo.json）：

```json
{
  "schemaVersion": 1,
  "rules": [
    {"style":"demo","nbt":{"Monumenta.Location":"forest"}}
  ],
  "decorationRules": [
    {"decorations":["demo/label"],"nbt":{"Monumenta.Location":"forest"}}
  ]
}
```

items 是物品 ID，tags 是物品分类标签，nbt 是物品携带的数据，三者不同。路径从物品 tag 内部开始，不要加 SelectedItem.tag.、tag. 或 nbt.。客户端必须收到这个值，字符串区分大小写。items 和 nbt 同时写就必须同时满足，不是二选一。
物品 tag 示例：用 `"tags":["minecraft:planks"]` 替换 items，匹配木板分类。至少保留一个匹配条件。

基础样式由 priority 数字较大的首个命中规则选择；物品自己的有效 TooltipStyle 优先于规则。独立装饰的所有命中规则会叠加，同 ID 只绘制一次。

## 装饰位置速查

anchor 是“贴在哪里”：TOP_LEFT 左上、TOP_RIGHT 右上、BOTTOM_LEFT 左下、BOTTOM_RIGHT 右下；TOP / BOTTOM 是上中 / 下中，LEFT / RIGHT 是左中 / 右中，CENTER 是正中。
SEPARATOR_LEFT / SEPARATOR_CENTER / SEPARATOR_RIGHT 对应分割线左端 / 中心 / 右端。没有分割线时这些装饰隐藏。
x 正右负左，y 正下负上，只移动这个装饰；不受整框 offsetXMode 的左右反转影响。x_scale / y_scale 单独缩放装饰的横向 / 纵向，默认 1。
foreground:true 将装饰画在正文之后，可遮住文字；装饰不会自动给正文让位置。

## 换自己的 PNG

背景、边框、分割线和样式自带的图片装饰放在同一张 PNG。将自己的图片放到资源包 assets/tooltipstudio/textures/styles/my.png，并把 styles/demo.json 的 texture 改成 tooltipstudio:textures/styles/my.png。
同步修改 textureWidth / textureHeight 为 PNG 实际尺寸。background、frame.region、separator.region 的 u/v 是取图起点，width/height 是区域大小；不可超出 PNG。frame 的四边不拉伸，separator 的 leftCap/rightCap 固定两端、中间拉伸。
独立图片装饰可以有自己的 PNG，不必与基础样式共用。新手先保持模板里的 PNG 和切片，等规则、文字调通再换图。

## 本地配置与资源包不是同一份

本地样式放 config/tooltipstudio/styles/，本地装饰放 config/tooltipstudio/decorations/；匹配规则放已有 config/tooltipstudio/config.json 的 rules 和 decorationRules 数组中。不要直接把资源包规则文件覆盖成全局 config.json。
本地 PNG 放 config/tooltipstudio/textures/，引用如 local:my.png；本地修改后运行 /tooltipstudio reload。资源包内不能写 local:，资源包改动用 F3+T。
同路径资源包 JSON 覆盖本地 JSON；多个包同路径按资源包优先级覆盖。优先改当前启用的那一份。

## 没变化先检查四件事

1. 包启用了吗？修改的是启用的文件夹版本吗？
2. 物品匹配了吗？样式规则和装饰规则里的条件都改了吗？
3. 引用名对吗？demo、demo/label 不带目录前缀和 .json。
4. JSON 保存后重载了吗？看聊天提示和 logs/latest.log；加载失败会保留上一次正确配置，所以画面可能仍是旧效果。

/tooltipstudio list 查看样式；/tooltipstudio decorations 查看独立装饰。

## 可复制指令（Minecraft 1.20.4，有命令权限时使用）

```mcfunction
/give @s minecraft:stick{display:{Name:'{"text":"教学木棍","italic":false}',Lore:['{"text":"物品自带的说明","italic":false}']}}
```

普通玩家只需安装模组并启用作者提供的资源包；服主/整合包作者完成配置后，把资源包分发给玩家即可，玩家客户端也必须安装 Tooltip Studio 与 Fabric API。服务端无需安装此客户端模组。
