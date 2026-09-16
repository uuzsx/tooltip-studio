# Monumenta NBT 匹配示例 — Tooltip Studio 1.6 / Fabric 1.20.4

先启用随本版附带的示例资源包，或自行创建 monumenta/forest 样式。示例包只使用默认 PNG，匹配时将基础款向上偏移 12 个 GUI 像素。
forest-rule.json 是可添加到现有 config.json 的 rules 数组中的单条规则；示例资源包已经携带这条规则，无需重复添加。完整 config.json 用于独立演示，替换前备份自己的配置。

```json
{"style":"monumenta/forest","priority":300,"nbt":{"Monumenta.Location":"forest"}}
```

路径从物品 tag 内部开始，SelectedItem.tag.Monumenta.Location 在这里写 Monumenta.Location，不加 tag.、nbt. 或 #。
不写 items 时可匹配任意物品基底。同一路径候选数组为“或”，不同路径和其他条件之间为“且”。字符串精确区分大小写，Forest 不等于 forest。
tags 用于 minecraft:planks 这种物品分类标签，物品自身 NBT 使用 nbt 条件。

例如限定锁链胸甲并匹配服务器提供的纯文本名称：

```json
{"style":"monumenta/forest","priority":310,"items":["minecraft:chainmail_chestplate"],"nbt":{"plain.display.Name":"Double Down"}}
```

原版 display.Name 保存 JSON 文本字符串，不能当成纯文本名字直接匹配。材质包的 .properties 写法也不能直接粘贴为 JSON。

在有命令权限的 1.20.4 单人世界测试：

```mcfunction
/give @s minecraft:diamond{Monumenta:{Location:"forest"},display:{Lore:['{"text":"NBT matching test","italic":false}']}}
/give @s minecraft:diamond{Monumenta:{Location:"desert"},display:{Lore:['{"text":"NBT matching test","italic":false}']}}
```

第一件选择向上偏移的 monumenta/forest；第二件未命中该 NBT 规则，使用 default。有效的 TooltipStyle 或其他更高优先级规则仍可能覆盖。
服务器上直接悬停已有物品即可，无需 OP；客户端必须实际收到该字段。匹配只读取数据，不修改名字、lore、模型或服务器物品。
