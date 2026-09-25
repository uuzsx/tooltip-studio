# NeoForge 版：安装、规则与版本差异

Tooltip Studio 1.10.0 新增六个独立的 NeoForge 构建。功能与 Fabric 1.9.0 一致；本次不改动已发布的 Fabric 版。

| Minecraft | 构建使用的 NeoForge | 游戏 Java | 资源包格式 |
|---|---|---|---|
| 1.21.1 | 21.1.251 | 21 | 34 |
| 1.21.4 | 21.4.157 | 21 | 46 |
| 26.1.1 | 26.1.1.15-beta | 25 | 84.0 |
| 26.1.2 | 26.1.2.109 | 25 | 84.0 |
| 26.2 | 26.2.0.88 | 25 | 88.0 |
| 26.3 | 26.3.0.16-beta | 25 | 97.1 |

26.1.1、26.3 对应的 NeoForge 构建为 beta。每个 JAR 仅声明支持文件名中的 Minecraft 版本，请勿跨版本混用。

## 安装与已有配置

1. 安装对应版本的 NeoForge，把对应的 `tooltip-studio-neoforge-1.10.0+版本.jar` 放进客户端 `mods`。
2. 不需要 Fabric API，不要同时放入 Fabric 版 Tooltip Studio。
3. 启动后配置仍在 `config/tooltipstudio/`；样式、装饰、规则的 JSON 格式和路径保持一致。
4. 示例 ZIP 放进 `resourcepacks` 并在游戏中启用。每个版本提供七种示例，包括组件匹配、分段文本及动画装饰。

这是客户端模组，服务器无需安装。其他模组的物品可以按其 `命名空间:物品ID`、物品 tag、自定义数据或持久化组件匹配；只能读取服务器实际发给客户端的数据。不同模组的同类属性未必使用同一个组件 ID 或数据结构。

## 旧的 Monumenta 匹配继续用

六个版本都使用物品组件。旧 `nbt` 字段会读取 `minecraft:custom_data` 中的内容，不需要在路径前添加 `components` 或 `minecraft:custom_data`：

```json
{
  "schemaVersion": 1,
  "rules": [
    {"style": "valley/forest", "priority": 300, "nbt": {"Monumenta.Location": "forest"}}
  ]
}
```

对应 `assets/tooltipstudio/styles/valley/forest.json`，且物品必须有 `minecraft:custom_data={Monumenta:{Location:"forest"}}`。
`TooltipStyle` 直接指定样式的方式仍从 `custom_data` 中读取。
这只是保留自定义数据规则；旧版 `display.Name`、`Damage` 等原版 NBT 字段不会自动转换成组件条件。

## 直接匹配组件

```json
{
  "schemaVersion": 1,
  "rules": [{
    "style": "default",
    "priority": 300,
    "items": ["minecraft:diamond_sword"],
    "components": {
      "minecraft:rarity": "epic",
      "minecraft:damage": 7,
      "minecraft:custom_data": {"Monumenta.Location": "forest"}
    }
  }]
}
```

同一规则中的物品、tag、`nbt`、`components` 条件需要同时满足。组件条件内不同键也需要同时满足；`"minecraft:rarity": ["rare", "epic"]` 表示任意一个值即可。
数值 `7` 和字符串 `"7"` 区分类型；组件不存在时不会按零值匹配。物品自带的默认组件也会参与匹配。

其他模组通过持久化 codec 注册的组件也可使用，例如 `"othermod:quality": "legendary"`。复杂对象可写内部路径；以该 Minecraft/模组版本实际编码的数据结构为准。未知组件 ID 或没有持久化 codec 的组件会导致配置校验失败，并保留上一份有效配置。
需要世界注册表的组件在进入世界后用该世界的注册表编码；无法编码时按不匹配处理。

装饰规则同样支持这些条件，匹配后只附加装饰，不更换背景、边框或分割线。

## 测试命令：注意文本组件语法

以下命令都需要允许使用命令。纯自定义数据、稀有度和损耗的写法在六个目标版本一致：

```mcfunction
/give @s minecraft:diamond_sword[minecraft:custom_data={Monumenta:{Location:"forest",Tier:"artifact"}},minecraft:rarity="epic",minecraft:damage=7]
```

**1.21.1 / 1.21.4** 的名称和 lore 使用 JSON 字符串：

```mcfunction
/give @s minecraft:stick[minecraft:custom_name='{"text":"测试木棍","italic":false}',minecraft:lore=['{"text":"测试说明","italic":false}']]
```

**26.1.1 / 26.1.2 / 26.2 / 26.3** 使用直接嵌入的文本对象：

```mcfunction
/give @s minecraft:stick[minecraft:custom_name={text:"测试木棍",italic:false},minecraft:lore=[{text:"测试说明",italic:false}]]
```

这影响游戏 `/give` 和原版组件数据，不影响 Tooltip Studio 装饰中的 `text`、`segments`、`color` 等 JSON 字段。分段文本仍可这样写：

```json
{
  "schemaVersion": 1,
  "type": "text",
  "segments": [
    {"text": "Architect's Ring: ", "color": "#888888"},
    {"text": "Artifact", "color": "#FF5555"}
  ],
  "anchor": "SEPARATOR_CENTER",
  "x": 0,
  "y": -8,
  "foreground": true
}
```

## 资源包格式

样式与装饰 JSON、PNG 不需要为加载器改写；同 Minecraft 版本的 Fabric/NeoForge 可复用这些文件。
跨游戏版本请修改 `pack.mcmeta`，以及资源包内可能包含的原版物品模型等其他文件。直接使用对应版本的示例 ZIP 最方便。

1.21.1 示例：

```json
{"pack":{"pack_format":34,"description":"我的 Tooltip 样式"}}
```

1.21.4 将 `34` 改为 `46`。26.x 使用新的范围格式；26.3 示例：

```json
{"pack":{"min_format":[97,1],"max_format":[97,1],"description":"我的 Tooltip 样式"}}
```

26.1.1 / 26.1.2 两处改为 `[84,0]`，26.2 改为 `[88,0]`。
贴图仍支持原样大小写文件名及独立动画 PNG；样式、装饰 JSON 的文件名和 ID 继续使用小写。

## 兼容边界与维护

使用 NeoForge 原生 tooltip 事件，保留其它 tooltip 组件的文字和图片绘制入口。居中标题、分割线、三段拉伸、动画、独立贴图、多段文字、缩放与相对鼠标偏移继续适用。
同时安装另一个重绘整个 tooltip 的模组时，应关闭其中一方的外观重绘，或逐个验证组合；不能据此保证所有第三方 tooltip 模组兼容。

资源包改动按 F3+T；本地配置改动使用 `/tooltipstudio reload`。`/tooltipstudio list` 和 `/tooltipstudio decorations` 查看已加载的定义。错误重载保留上次有效配置，详细原因见游戏日志。

开发构建从仓库 `neoforge` 目录执行：

```powershell
.\gradlew.bat -Pmc=1.21.1 build
.\gradlew.bat -Pmc=26.3 build
.\gradlew.bat -Pmc=26.3 runSmoke
.\gradlew.bat -Pmc=26.3 -PpackagedSmoke runSmoke
```

构建需要 JDK 25；1.21.x 目标另需安装 JDK 21，Gradle toolchain 自动选择。产物在 `neoforge/build/版本/libs/`，示例包在同级 `resourcepacks/`。`runSmoke` 是隔离的开发测试模组，不会进入发布 JAR。
`packagedSmoke` 会让测试客户端加载打包后的 JAR。26.x 测试会在隔离的 `run-smoke/版本/` 中创建临时单人世界，以取得真实的物品默认组件和世界注册表。同一份 checkout 的不同目标请依次构建；并行验证使用独立 checkout。

底层适配分别处理旧版即时渲染、26.x 延后绘制、NBT 数值/字符串 API，以及 26.3 的覆盖层资源包接口。构建脚本逐版本固定依赖，避免用一个宽泛的版本范围掩盖 API 差异。

官方参考：[NeoForge 文档](https://docs.neoforged.net/)、[ModDevGradle](https://github.com/neoforged/ModDevGradle)、[26.1 发布说明](https://neoforged.net/news/26.1release/)。
