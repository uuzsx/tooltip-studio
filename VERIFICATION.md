# 1.2 样式分类路径验证

- 验证日期：2026-09-15；Minecraft 1.20.4、Windows、JDK 17、Gradle 8.6。
- 26 项 JUnit 测试通过：保留 20 项已有测试，新增本地多层扫描、同名文件隔离、完整路径覆盖、删除后移除、路径合法性及资源包分类目录加载检查。
- 实际客户端在 Loader 0.15.11 基础环境，以及 Loader 0.19.5 + Shulker Box Tooltip 4.1.0 + Cloth Config 13.0.121 环境分别完成 `build runSmoke`，均输出 `SMOKE COMPLETE`。
- 本地嵌套样式验证：不同文件夹中的 forest.json 独立加载，规则 style、defaultStyle 和 NBT TooltipStyle 正确选择完整路径；继续支持 local PNG。删除仍被规则引用的 JSON 后拒绝重载并保留上一份状态，修正引用后恢复。
- 示例 ZIP 同时加载平铺与 monumenta/forest 分类样式，NBT 条件正常匹配；高优先级资源包按完整路径覆盖分类样式，平铺样式保持独立。完整资源重载、错误来源提示、错误后恢复、停用移除与本地配置不被改写均通过。
- 已查看实际游戏截图，森林物品通过 monumenta/forest 规则使用图集样式；原有木棍与默认钻石样式正常。潜影盒框内/框外预览、锁定/解锁与上下文恢复回归通过。
- 默认 JSON、PNG、渲染器和兼容 Mixin 未修改；作者、图标、MIT 协议与 1.20.4 适配范围保持不变。

未验证：朋友服务器的完整模组与资源包组合、长期多人会话。使用旧版模组无法加载含分类样式的新示例包，需要升级到 1.2。

# 验证记录

验证日期：2026-09-14。

- 构建环境：Windows、JDK 17、Gradle 8.6、Fabric Loom 1.6.12。
- Minecraft 1.20.4、Yarn 1.20.4+build.3、Fabric Loader 0.15.11、Fabric API 0.97.2+1.20.4。
- `build runSmoke` 成功完成；6 项 JUnit 测试，0 失败、0 错误。
- 自动测试涵盖 9 套样式 JSON 校验、非法图集/分割线区域、规则和样式引用、ID 通配符以及 9..2048 像素宽度下不等长分割线端点的固定性。
- 实际启动 Minecraft 客户端并完成资源加载，Mixin 成功应用。
- 客户端检查：9 套默认样式、ID 规则、NBT 覆盖、非法 JSON 拒绝和旧配置保留、有效配置恢复、自定义样式与本地 PNG 加载、完整资源重载。
- 渲染截图同时覆盖 `DrawContext.drawItemTooltip` 与一个实际继承 `HandledScreen` 的测试界面的 `drawMouseoverTooltip` 调用。
- 已人工查看三张实际游戏截图：9 套外观、长标题换行、正文换行、只有标题时无分割线、中文、收纳袋图形内容、右下角位置限制，以及 60 行 lore 的完整显示。
- 故意输入错误 JSON 的测试会产生一次预期错误日志，随后恢复成功。
- 发布 JAR 不包含测试模组。原始极光项目只读，未修改。

初版尚未验证：第三方 tooltip/物品浏览器模组组合、真实多人服务器或长期游戏会话。未适配或宣称支持其他 Minecraft 版本。
测试界面无需创建世界，不生成或修改玩家存档。


# 0.1.1 兼容修复验证

- 根据用户提供的日志，确定 0.1.0 与 Shulker Box Tooltip 4.1.0 争用同一个 Redirect 目标。
- 在本地安装 Shulker Box Tooltip 4.1.0+1.20.4 与 Cloth Config 13.0.121，成功复现相同的 Redirect conflict / 0 of 1 injections 崩溃。
- 移除排他的 Redirect，使用 WrapMethod 保留原调用链；发布 JAR 内嵌 MixinExtras 0.4.1，包含其原始许可证。
- 修复后基础渲染检查在 Fabric Loader 0.15.11 + Shulker Box Tooltip 4.1.0 上成功完成。
- 完整兼容回归使用与用户日志一致的 Fabric Loader 0.19.5、MixinExtras 0.5.5、Fabric API 0.97.2+1.20.4、Shulker Box Tooltip 4.1.0、Cloth Config 13.0.121。
- 实际客户端截图确认：框内潜影盒内容预览、框外预览、锁定时保持被锁物品的样式、解锁后按当前物品恢复样式，以及普通非物品 tooltip 保持原版。
- 运行时断言确认：嵌套绘制上下文恢复、锁定/解锁物品选择、渲染异常后无遗留物品状态。
- 测试界面显式初始化 SBT 通常在进入世界后注册的预览 provider，使用真实 SBT 预览数据与组件；没有创建存档。
- 没有复现用户完整的 140 模组组合，也未完成多人服务器或长期会话验证。

- 最终 `build runSmoke` 在不安装 Shulker Box Tooltip 的 Loader 0.15.11 环境中通过，6 项 JUnit 测试及客户端渲染/异常恢复检查通过。

# 0.1.2 NBT 匹配验证

- 2026-09-14，Windows / JDK 17 / Gradle 8.6，Minecraft 1.20.4。
- 13 项 JUnit 测试全部通过（6 项原有测试 + 7 项 NBT 测试）。新增检查涵盖嵌套路径、精确和区分大小写的字符串、多字段 AND / 候选值 OR、plain.display.Name、带引号的键、列表路径、数值精度、字符串与数字不混淆、布尔类型、缺失字段、只读匹配、非法条件/路径拒绝，以及交付示例配置有效性。
- 不安装 Shulker Box Tooltip 时，Loader 0.15.11 的 `build runSmoke` 完成资源加载、NBT 热重载、全部断言和渲染截图，输出 `SMOKE COMPLETE`。
- 使用 Loader 0.19.5 + Fabric API 0.97.2+1.20.4 + Shulker Box Tooltip 4.1.0 + Cloth Config 13.0.121，再次执行 `build runSmoke -PcompatShulker -Ploader_version=0.19.5` 成功，输出 `SMOKE COMPLETE`。
- 实际客户端检查：同一原版物品基底根据不同 Monumenta.Location 选择不同样式；多个 NBT 条件和 ID/tag/稀有度组合；规则优先级及同优先级顺序；旧 ID/稀有度规则；有效 TooltipStyle 覆盖；截图中的 plain.display.Name 写法；NBT 序列化往返后仍可匹配且不修改数据。
- 无服务器标签同步的测试界面临时给木棍绑定测试物品 tag，检查 tag + NBT 的 AND 关系，然后恢复原标签。该逻辑仅存在于测试源码，发行 JAR 不包含测试模组。
- 故意写入非法 NBT 路径后，重载失败且旧样式选择仍正常；恢复配置后重新加载成功。测试结束恢复原配置。
- 使用真实 HandledScreen 物品提示入口拍摄 NBT 对照截图，六件物品均为木棍，未使用 TooltipStyle 强制覆盖；已查看截图。
- 潜影盒框内/框外预览、锁定/解锁、嵌套上下文和异常恢复回归通过；已查看兼容截图。
- 主模组的默认 JSON、PNG 资源和现有兼容 Mixin 保持不变。更新不会自动给用户已有配置添加 Monumenta 规则。

范围限制：本地客户端构造了与截图相同字段结构的数据，未连接朋友的服务器，也未验证朋友完整的材质模组/资源包组合。客户端必须实际收到需要匹配的 NBT。当前发布仍只支持 Minecraft 1.20.4。

# 1.0 项目信息更新验证

- `gradle build` 成功，13 项测试全部通过。
- 检查发布 JAR 的 fabric.mod.json：版本为 `1.0`，作者依次为 `幼幼紫`、`千村`，简介为中文单句；适用 Minecraft 版本仍为 `1.20.4`。
- 对比 0.1.2 发布 JAR，主模组 class、样式和纹理资源、Mixin 配置与 refmap 内容一致。本次仅更新项目信息和对应文档，游戏功能未变。
- 此次没有重复运行游戏客户端，渲染和潜影盒兼容依据上一版相同主模组实现的实机测试记录。

# 1.0 图标与 MIT 协议更新验证

- `gradle build` 成功，13 项测试全部通过。
- 发布 JAR 的图标路径指向 `assets/tooltipstudio/icon.png`，PNG 与用户提供的 logo.png 字节完全一致，尺寸为 32×32。
- 发布 JAR 的协议标识为 `MIT`，含有完整 LICENSE 和 THIRD_PARTY_ASSETS.md，作者与版本保持为幼幼紫、千村及 1.0。
- 主模组 class 与上次发布版本一致；本次更新涉及图标、协议、打包内容和文档，没有重复运行游戏客户端。

# 1.1 资源包样式与规则验证

- 验证日期：2026-09-14；Minecraft 1.20.4、Windows、JDK 17、Gradle 8.6。
- 20 项 JUnit 测试通过（13 项已有测试 + 7 项资源包测试）。使用真实 DirectoryResourcePack 与 LifecycledResourceManagerImpl 检查 JSON 加载、资源优先级、整份规则文件覆盖、多个规则文件合并、同 priority 的稳定顺序、停用移除、错误来源提示、命名空间隔离与示例包有效性。
- 实际客户端分别在基础 Loader 0.15.11 环境、Loader 0.19.5 + Shulker Box Tooltip 4.1.0 + Cloth Config 13.0.121 环境运行 `build runSmoke`，均输出 `SMOKE COMPLETE` 并构建成功。
- 客户端从 examples/resource-pack 创建真实 ZIP，经 Minecraft 资源包管理器启用，执行与 F3+T 相同的完整资源重载流程；无需把样式或规则 JSON 安装到 config 目录。
- 验证启用 ZIP 新增两套样式、木棍 ID 与 Monumenta.Location NBT 自动匹配、TooltipStyle 选择包内样式、本地与包内规则同 priority 时本地优先、未命中物品保留默认外观。
- 额外启用高优先级目录包，验证同名样式覆盖本地示例 JSON、同路径样式覆盖低优先级包、空规则文件屏蔽低优先级包的同路径规则。
- 完整资源重载时故意写入错误 JSON，确认错误消息含包与资源路径，且保留上一版样式及规则；缺失 PNG 同样保留旧状态，修正后恢复。
- 停用示例包后恢复 9 套本地样式，包内独有样式和规则消失，木棍恢复默认样式。本地 config.json 前后字节一致。
- 已查看实际游戏截图：资源包木棍样式、forest NBT 样式、原有默认样式；潜影盒框内/框外、锁定/解锁与嵌套上下文回归通过。
- 示例包 pack_format 为 22，样式、规则与 PNG 均在 ZIP 内；不依赖 Legendary Tooltips，本模组沿用自己的单图 JSON 格式。

未验证：朋友服务器完整模组组合、服务器下发资源包及长期多人会话。1.20.4 以外版本仍未适配。
