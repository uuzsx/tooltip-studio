# Tooltip Studio 资源包示例

需要 **Tooltip Studio 1.1 + Fabric / Minecraft 1.20.4**。这是资源包，放进游戏实例的 `resourcepacks` 文件夹，在游戏“选项 → 资源包”中启用；无需修改本地配置。

启用后：

- 普通木棍和属于 `minecraft:planks` 标签的木板使用 `pack_wood` 样式。
- 任意基底的物品，只要客户端 NBT 中 `Monumenta.Location` 精确为 `forest`，就使用 `pack_forest` 样式。
- 其他物品继续使用现有规则与默认样式。

如果物品有有效的 `TooltipStyle`，它仍具有最高优先级；自定义本地规则的 priority 更高时，也会优先于本示例规则。

## 文件结构

```text
资源包 ZIP 根目录/
├─ pack.mcmeta
├─ pack.png
└─ assets/tooltipstudio/
   ├─ styles/
   │  ├─ pack_wood.json
   │  └─ pack_forest.json
   ├─ rules/
   │  └─ example.json
   └─ textures/styles/
      ├─ pack_wood.png
      └─ pack_forest.png
```

打包时让 `pack.mcmeta` 直接位于 ZIP 根目录，不要再套一层文件夹。

`assets/tooltipstudio/styles/名字.json` 使用与本地 `config/tooltipstudio/styles/` 相同的样式 JSON 格式，文件名即样式 ID。
名字只用小写英文字母、数字、下划线和连字符；样式 JSON 不放子文件夹。推荐加上自己的前缀，例如 `myserver_forest`，避免与其他包同名。
PNG 可以放在资源包中任意合法命名空间路径，例如 `assets/myserver/textures/tooltips/forest.png`，对应 `texture` 为 `myserver:textures/tooltips/forest.png`。
资源包样式不能使用 `local:`；背景、边框、分割线与装饰仍从同一张 PNG 中取区域，所有切片参数保持原用法。

只有样式 JSON 时会注册新样式，还需要通过规则、defaultStyle 或物品 TooltipStyle 选用它。本例已经附带自动匹配规则。

## 匹配规则

在 `assets/tooltipstudio/rules/` 下新建 JSON，例如：

```json
{
  "schemaVersion": 1,
  "rules": [
    {
      "style": "pack_forest",
      "priority": 300,
      "nbt": { "Monumenta.Location": "forest" }
    }
  ]
}
```

规则字段与本地配置相同，支持 `items`、`tags`、`rarities`、`nbt`。资源包规则文件只包含 `schemaVersion` 和 `rules`；全局 enabled、defaultStyle、nbtStyleKey 仍在本地 config.json 控制。

## 覆盖与重载

1. 同名样式：启用的资源包覆盖本地 styles JSON；多个资源包中同路径文件按 Minecraft 的资源包优先级选择，上方优先。
2. 同路径规则 JSON：高优先级资源包整份替换低优先级文件，不拼接两份数组。用 `{"schemaVersion":1,"rules":[]}` 可以屏蔽低优先级包的同路径规则文件。
3. 不同路径规则 JSON：合并到本地规则中，按 priority 从高到低匹配。相同 priority 时本地规则在前，资源包规则按文件路径字典序排列，同文件按数组顺序。
4. 启用、停用或重新排序资源包时自动重载。修改资源包文件后按 **F3+T**；`/tooltipstudio reload` 会重读当前已加载的资源与本地配置，但不会扫描启用新资源包。
5. 停用资源包后，该包独有的样式和自带规则会移除；不写入、不覆盖本地配置文件。
6. 错误 JSON、未知样式引用、非法纹理区域或缺失 PNG 会拒绝本次样式重载，保留上次成功状态。日志与 `/tooltipstudio reload` 错误消息可查看出错资源路径。

如果你在本地 config.json 中手工引用了包里的样式，停用该包时应同步修改这些引用，否则会因未知样式拒绝重载。资源包自带规则会随包一起移除。
本地和资源包规则合计最多 4096 条。本地原有配置与默认样式无需迁移；不启用含 Tooltip Studio JSON 的资源包时行为与原版模组相同。

## 在单人世界测试 forest 匹配

使用有命令权限的 1.20.4 测试世界：

```mcfunction
/give @s minecraft:stick
/give @s minecraft:diamond{Monumenta:{Location:"forest"},display:{Lore:['{"text":"Resource pack tooltip","italic":false}']}}
```

第一件使用 pack_wood，第二件使用 pack_forest。服务器上直接悬停已有物品即可；客户端需要收到对应 NBT 字段。

## 格式与素材说明

本资源包使用 Tooltip Studio 的 JSON 格式，不读取 Legendary Tooltips 的 frame_definitions.json，也不依赖 Legendary Tooltips 本体。
示例 pack_wood / pack_forest PNG 分别来自 Tooltip Studio 已有的 uncommon / mythical 临时图集；原始素材来自用户提供的极光项目，保留原有权属。替换成自己的单图图集后，更新对应样式 JSON 即可。
