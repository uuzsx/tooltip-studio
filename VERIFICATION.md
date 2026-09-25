# 1.8.1 大写贴图路径验证

- 验证日期：2026-09-25；Minecraft 1.20.4、Windows、JDK 17、Gradle 8.6。79 项 JUnit 测试通过，新增 8 项覆盖截图原样路径 reverie_fireR.png、目录/扩展名大写、命名空间归一化、全小写回退、同包精确路径优先、跨包优先级、资源过滤、本地路径及非法路径拒绝。
- Loader 0.15.11 的 `build runSmoke -PsmokeRunDir=run-smoke-case` 和 Loader 0.19.5 + Shulker Box Tooltip 4.1.0 + Cloth Config 13.0.121 的 `build runSmoke -PcompatShulker -Ploader_version=0.19.5` 均输出 `SMOKE COMPLETE`。
- 游戏中原样复现 16×64、4 帧、frameTime=4 的 reverie_fireR.png：文件夹资源包、ZIP 资源包与原版版本覆盖层均成功读取；主样式大写图集、内嵌动画和独立装饰一起加载。Framebuffer 像素验证两个装饰按预期尺寸绘制，覆盖层实际读取高层蓝色 PNG。
- 本地 LOCAL:CaseFixture/Default.PNG 无需改名即可加载。缺失的大写 PNG 拒绝重载且保留完整旧状态；修复后恢复，停用资源包移除相关定义，本地 config.json 保持原字节。
- 动画时序、多段文字、分割线锚点、屏幕偏移、NBT/tag/ID 匹配及潜影盒预览、锁定/解锁和嵌套上下文回归通过。发行 JAR 的两个新增 mixin 字段已映射至 intermediary 名称，refmap 正常打包。

范围：仅兼容 texture 引用中的大小写，不放宽样式/装饰 JSON 文件名或 ID 规则。PNG 路径应与实际文件一致；同一包内找不到原样路径时才尝试全小写路径，不猜测其他大小写组合。未验证朋友服务器的完整模组组合。

# 1.8 动态装饰与单独贴图验证

- 验证日期：2026-09-21；Minecraft 1.20.4、Windows、JDK 17、Gradle 8.6。71 项 JUnit 测试通过，新增 7 项覆盖帧时间边界与循环、横纵排列、非零首帧坐标、全部帧越界检查、无效动画、独立尺寸、继承图集、旧序列化及资源包路径限制。
- Loader 0.15.11 的 `build runSmoke -PsmokeRunDir=run-smoke-animation`，以及 Loader 0.19.5 + Shulker Box Tooltip 4.1.0 + Cloth Config 13.0.121 的 `build runSmoke -PcompatShulker -Ploader_version=0.19.5` 均输出 `SMOKE COMPLETE`。
- 实际 framebuffer 连续采样确认：本地样式内纵向动画与独立横向动画的三帧颜色均实际出现并循环；x_scale=1.5 / y_scale=0.75 后的单帧边界保持固定，单独静态 PNG 按自己的尺寸绘制。三个装饰区域之外的所有像素与无装饰的 tooltip 完全一致。
- 缺失样式内 PNG 与动画后续帧越界均拒绝重载，保留之前完整快照；修复后恢复。真实 ZIP 资源重载同时加载样式内与独立匹配动画，相同 PNG 只上传一次；停用包后清除其独有样式和装饰，不改写本地配置。
- 已查看实际木棍示例截图；八张连续游戏截图展示默认面板两角的星光大小变化。原有 NBT/tag/ID 匹配、偏移、分割线锚点、多段文字、缩放、错误恢复和潜影盒框内/框外预览、锁定/解锁、嵌套上下文回归通过。
- 仍只内置用户指定的 default 基础款。动画 PNG 仅放入可选示例包，未使用参考 GIF 的图片素材；README 保留用户精简版正文，追加本次新参数说明。

范围限制：动画采用连续横向或纵向 PNG 帧条，不直接播放 GIF/APNG，不读取 .png.mcmeta，不支持网格、逐帧时长或插值。未验证朋友服务器完整模组与资源包组合，未适配其他 Minecraft 版本。

# 1.7 多段彩色文字验证

- 验证日期：2026-09-18；Minecraft 1.20.4、Windows、JDK 17、Gradle 8.6。
- 64 项 JUnit 测试通过。新增 8 项覆盖连续多色拼接、格式继承及 false 覆盖、不向后续段落泄漏格式、跨段 CRLF 和换行、旧字符串与序列化兼容、局部斜体边界、非法段落与总量限制、本地样式和无 PNG 资源包加载。
- Loader 0.15.11 的 `build runSmoke -PsmokeRunDir=run-smoke-segments`，以及 Loader 0.19.5 + Shulker Box Tooltip 4.1.0 + Cloth Config 13.0.121 的 `build runSmoke -PcompatShulker -Ploader_version=0.19.5` 均输出 `SMOKE COMPLETE`。
- 实际 framebuffer 对比确认：旧字符串 III 改写为三个同色 I 段后，整幅图逐像素一致。改成三种颜色后，相邻文字起点距离严格等于字体字宽，基线一致，没有额外空隙。
- 真实像素验证所有段落共享 x_scale=2、y_scale=0.5，缩放后仍按总宽度居中；跨段换行保留颜色，各行从同一原点开始。极限正负偏移后，多行文字仍位于屏幕边距以内。
- 无效分段颜色拒绝本次重载，保留之前的完整快照；修复后恢复。完整 ZIP 资源重载加载两份多段装饰，NBT 条件选出一份含两种颜色的装饰，无需 PNG；停用后恢复原状态，本地 config.json 字节不被改写。
- 已查看实际演示，确认灰色前缀与红色 Artifact、金色粗体星星、换行斜体，以及样式内多色装饰与缩放。已查看潜影盒兼容截图，框内/框外预览、锁定/解锁、嵌套上下文与分割线上的两色文字正常。
- 原有偏移、分割线锚点、图片装饰与匹配机制回归通过。旧实例 10 份配置 JSON 的 SHA256 与运行前一致，未修改的旧预设仍被正确识别；默认基础 PNG、JSON 和 logo 原样保留，未新增 tooltip 贴图。

范围限制：未验证朋友服务器完整模组与资源包组合、其他 Minecraft 版本或长期多人会话。多段装饰显示固定文字，不替换原物品 lore；文字装饰不自动占据正文排版空间。

# 1.6 分割线锚点、文字、缩放与鼠标间距验证

- 验证日期：2026-09-16；Minecraft 1.20.4、Windows、JDK 17、Gradle 8.6。
- 56 项 JUnit 测试通过。新增 13 项覆盖左右等距、负偏移与旧 screen 模式、贴边前侧向判定、旧图片默认布局、独立 XY 缩放、真实分割线区域锚点、无分割线隐藏、纯文字定义、非法参数以及无 PNG 资源包。
- Loader 0.15.11 的 `build runSmoke -PsmokeRunDir=run-smoke-advanced` 和 Loader 0.19.5 + Shulker Box Tooltip 4.1.0 + Cloth Config 13.0.121 的 `build runSmoke -PcompatShulker -Ploader_version=0.19.5` 均输出 `SMOKE COMPLETE`。后者继续使用旧版配置和 9 份旧预设，结束后全部 10 份 JSON 的 SHA256 与运行前一致，仍只加载一套默认基础款。
- 真实 framebuffer 逐像素比较：offsetX=24 时，右侧向右 24 GUI 像素、左侧向左 24 GUI 像素；左侧 offsetX=-6 向右 6，screen 模式 offsetX=24 向右 24。全部为 0 差异像素，验证整框平移和左右一致间距。
- 用红、绿、蓝文字标记检查分割线左、中、右位置，验证实际 inset、缩放后宽度与共同垂直中心。标题换行后，三处标记跟随分割线移动；标题无正文或 separator.enabled=false 时三者均隐藏。
- 真实字体绘制验证文字 x_scale=2、y_scale=0.5 的宽高变化，样式内文字正常显示；非法 x_scale=0 拒绝重载并保留有效配置。正负极限整框偏移和 60 行超高 tooltip 缩放后，文字与图片装饰仍在屏幕边距内。
- 完整资源包重载加载新增示例的两项纯文字和两项图片装饰：纯文字不创建贴图，图片读取 1.5/0.75 缩放；配置文件字节不被改写。已查看四种展示场景及左右鼠标间距演示截图。
- 原有图片角落叠加、四角平移、条件匹配、优先级、去重、资源包覆盖/停用和错误恢复检查继续通过。潜影盒预览增加缩放剑与分割线文字，框内/框外、锁定/解锁和嵌套上下文回归通过，已查看兼容截图。
- 默认 PNG、基础 JSON、logo 均保持原始素材；JAR 仍只包含 default 基础贴图，新增可选资源包仅复用现有 sword.png。

范围限制：未验证朋友服务器完整模组与资源包组合、其他 Minecraft 版本或长期多人会话。贴边时鼠标间距受屏幕空间限制；装饰不参与正文排版，可用 x/y 避免内容重叠。

# 1.5 独立条件装饰验证

- 验证日期：2026-09-15；Minecraft 1.20.4、Windows、JDK 17、Gradle 8.6。
- 43 项 JUnit 测试通过。新增 10 项覆盖装饰切片与锚点、非法区域/偏移、条件验证、旧配置与旧预设迁移保留装饰规则、本地分类扫描、完整路径覆盖、只含装饰的资源包、整份规则文件覆盖、错误来源及示例 PNG 尺寸。
- 基础 Loader 0.15.11 的 `build runSmoke -PsmokeRunDir=run-smoke-decorations` 与 Loader 0.19.5 + Shulker Box Tooltip 4.1.0 + Cloth Config 13.0.121 的 `build runSmoke -PcompatShulker -Ploader_version=0.19.5` 均输出 `SMOKE COMPLETE`。
- 真实客户端验证本地独立 PNG、NBT 只读匹配、ID/tag/稀有度/NBT 的 AND 关系、多角叠加、同 ID 去重、优先级与同优先级顺序、禁用开关，以及 TooltipStyle 选中的自定义布局和原样式装饰共存。
- 同一 tooltip 添加左上角 16×16 装饰后，只在该区域内出现差异；其余整张 framebuffer 像素完全一致。名称、正文、背景、边框和分割线的其他区域保持原样。
- 四角装饰与基础款一起向右 24、向上 20 GUI 像素移动时，整幅截图逐像素平移对比为 0 差异。正负极限偏移、60 行正文缩放后，独立装饰仍位于屏幕 4 GUI 像素边距内。
- 经真实 ZIP 和完整资源重载验证：包内装饰覆盖相同本地路径、高优先级包覆盖位置、空装饰规则文件屏蔽低层、同 priority 本地装饰在上、停用移除及本地配置字节不被改写。
- 缺失本地 PNG 和非法包内装饰 JSON 保留上一份完整有效样式、装饰与规则；恢复后重新生效。结束后恢复测试配置并移除临时本地样式与装饰。
- 潜影盒框内/框外预览、偏移、锁定/解锁及嵌套上下文通过，锁定物品的独立剑装饰保持，解锁后不泄漏给普通钻石。已查看实际演示和兼容截图。
- 默认 JAR 仍只内置 default 的 PNG/样式定义。新增 sword.png 仅存在于独立装饰示例和可选资源包，与用户的 test.png 字节一致（128×128，剑区域 16×16）；未加入其他 tooltip 基础贴图。

范围限制：未验证朋友服务器完整模组与资源包组合、其他 Minecraft 版本或长期多人会话。匹配需要客户端实际收到对应物品 NBT 或分类 tag。

# 1.4 默认基础款与旧素材移除验证

- 验证日期：2026-09-15；Minecraft 1.20.4、Windows、JDK 17、Gradle 8.6。
- 33 项 JUnit 测试通过，覆盖唯一默认预设、配置校验、旧预设名称回退及自定义名称保留；原有 NBT、资源包、分类路径与偏移测试继续通过。
- `clean build runSmoke -PsmokeRunDir=run-smoke-base-only` 在全新目录、Loader 0.15.11 下通过；`build runSmoke -PcompatShulker -Ploader_version=0.19.5` 在保留 1.3 配置及 9 份旧预设 JSON 的目录通过。两次均输出 `SMOKE COMPLETE`。
- 新安装仅加载 default；旧配置仍写着 rare 默认值与 legendary/epic/rare/uncommon 规则，但运行时只加载新基础款。旧文件不被改写或删除。
- 实际客户端验证：无需本地 default.json 也可使用内置基础款；同名本地 JSON 可以覆盖参数，删除覆盖文件后恢复内置定义。测试结束恢复原配置。
- 真实 ZIP 资源包验证默认 PNG、monumenta/forest 分类样式与 offsetY=-12；资源重载、覆盖排序、规则优先级、停用移除与错误后恢复全部通过。
- 使用新基础 PNG 重跑整框位移截图比较，正负偏移与超长正文缩放后移动均为 0 差异像素；边缘限制、名称换行、分割线、收纳袋和 NBT 匹配回归通过。
- Shulker Box Tooltip 4.1.0 + Cloth Config 13.0.121 环境完成框内/框外预览、锁定/解锁、偏移、嵌套上下文与异常恢复检查。已查看默认款演示与潜影盒兼容截图。
- 发行 JAR 只含 default.json 与 default.png；示例包和自定义示例使用同一张 PNG。PNG 与用户提供的 Default.png 字节一致，基础 JSON 除 texture 引用外与 defalut.json 相同。当前源码已移除旧预设、旧 PNG 与图集生成脚本；仅保留兼容识别名称和指纹。

范围限制：未验证朋友服务器完整模组/资源包组合、其他 Minecraft 版本或长期多人会话。修改过的旧样式若仍引用已移除的 PNG，需要用户提供替代图片及对应参数。

# 1.3 整体 XY 偏移验证

- 验证日期：2026-09-15；Minecraft 1.20.4、Windows、JDK 17、Gradle 8.6。
- 30 项 JUnit 测试全部通过：验证旧 JSON 缺省为零、独立配置 X/Y、正负方向、边界值与越界值、零偏移与旧定位一致、从贴边位置移开、四周边界约束，以及资源包内偏移参数的实际读取。
- `build runSmoke` 在 Loader 0.15.11 基础环境与 Loader 0.19.5 + Shulker Box Tooltip 4.1.0 + Cloth Config 13.0.121 环境分别成功，均输出 `SMOKE COMPLETE`。
- 新增真实 framebuffer 对比：同一 tooltip 向右 24/向上 20、向左 16/向下 18 GUI 像素后，与整张基准截图相应平移的结果逐像素一致，差异像素数均为 0。比较包含背景、边框、标题、分割线、正文和装饰。
- 60 行 lore 触发整体缩小后，向右 24 GUI 像素的截图仍与基准平移逐像素一致，证明整体缩放不会再次缩小偏移数值。GUI 缩放为 2。
- 两轴分别设置正负 4096 后检查实际图像，所有可见 tooltip 像素仍位于屏幕四周 4 GUI 像素的留白以内；越界参数 4097 拒绝重载并保留先前状态，修正后恢复。
- 已查看游戏内偏移前后对照图。资源包 forest 样式的 offsetY=-12 经真实 ZIP 启用及完整资源重载后读取正确。
- 潜影盒使用带 offsetX=16、offsetY=-12 的样式，框内/框外预览、锁定/解锁和嵌套上下文回归通过；已查看实际截图。测试结束移除临时样式并恢复原配置。
- 内置默认 JSON 与 PNG 未修改，旧样式的默认位置不变；原有 NBT 匹配、资源包覆盖与分类路径测试全部保留。

未验证：朋友服务器的完整模组和资源包组合、其他 Minecraft 版本及长期多人会话。屏幕边缘仍限制实际位移，已经占满屏幕高度的 tooltip 没有继续上下移动的空间。

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
