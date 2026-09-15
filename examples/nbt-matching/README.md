# Monumenta NBT 匹配示例 — Tooltip Studio 1.3 / Fabric 1.20.4

1. 从游戏实例 mods 文件夹移除旧版 Tooltip Studio JAR，安装 1.3，仅保留一个版本。
2. 打开游戏实例 `config/tooltipstudio/config.json`。将 `forest-rule.json` 中的对象加进现有 `rules` 数组，与相邻对象用逗号分隔。
3. 进入世界，执行 `/tooltipstudio reload`，然后悬停 `Monumenta.Location` 值为 `forest` 的服务器物品。

这条规则会匹配任意基底物品，选用内置 `mythical` 样式；修改 `style` 可换成你自己的样式 ID，例如 `monumenta/forest` 对应 `styles/monumenta/forest.json`。
完整 `config.json` 是默认规则加一条 forest 规则。如选择整份替换，请先备份自己的原配置，避免覆盖已有规则。

```json
{
  "style": "mythical",
  "priority": 200,
  "nbt": {
    "Monumenta.Location": "forest"
  }
}
```

路径从物品 tag 内部开始。`SelectedItem.tag.Monumenta.Location` 对应本配置的 `Monumenta.Location`。
不加 `tag.`、`nbt.` 或 `#`。原有 `tags` 字段继续用于 `minecraft:planks` 这种物品分类标签。
字符串完整且区分大小写，`Forest` 或 `deep_forest` 不会匹配 `forest`。

好友截图中的 `nbt.plain.display.Name=Double Down` 可对应：

```json
{
  "style": "red_book",
  "priority": 210,
  "items": ["minecraft:chainmail_chestplate"],
  "nbt": { "plain.display.Name": "Double Down" }
}
```

同一规则的所有 NBT 字段都要满足；候选数组表示任意一个值即可；NBT 与 `items` / `tags` / `rarities` 同时填写时要同时满足：

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

在自己的 1.20.4 测试世界开启命令权限，逐行执行下面两条命令。使用附带完整示例配置时，第一根木棍显示 mythical，第二根显示默认 rare：

```mcfunction
/give @s minecraft:stick{Monumenta:{Location:"forest"},display:{Lore:['{"text":"NBT matching test","italic":false}']}}
/give @s minecraft:stick{Monumenta:{Location:"desert"},display:{Lore:['{"text":"Default style test","italic":false}']}}
```

规则优先级：有效 `TooltipStyle` → priority 从大到小首条命中（同值按排列顺序）→ defaultStyle。
测试物品不要另加 `TooltipStyle`；若希望完全靠规则，也可以将 `nbtStyleKey` 设为空字符串。

匹配不修改物品数据、名字、lore 或模型贴图。现有材质模组/资源包仍负责模型外观。
客户端必须能收到对应 NBT 字段；服务器 `/data get` 能看到字段，不一定意味着客户端也能看到。服务端无需安装本模组。

新版已经在本地 Minecraft 1.20.4 客户端测试；并未登录朋友的服务器或验证其完整材质模组组合。
