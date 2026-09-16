# 素材与第三方依赖说明

Tooltip Studio 项目采用 MIT 协议，见 LICENSE。

## 当前贴图与基础样式

本版仅内置用户朋友制作、由用户指定作为基础款的 Default.png，资源名为 assets/tooltipstudio/textures/styles/default.png。PNG 字节与原文件完全一致。
对应 default.json 来自用户提供的 defalut.json，仅将 texture 从 local:Default.png 改为模组资源 ID，其他外观参数原样保留。素材保留原作者权属。
examples/custom-style/textures/my_style.png 和示例资源包中的 default.png 均为同一张 PNG 的原样复制，没有第二套 tooltip 图集。

1.3 及更早版本借用的极光预设、PNG 图集和生成脚本已从当前源码与发行包移除。兼容代码只保存旧名称及识别指纹，不携带旧图片或旧样式定义。

## 模组图标

assets/tooltipstudio/icon.png 与示例资源包 pack.png 使用用户提供的 logo.png，保留原始 PNG 内容与尺寸。

## 独立装饰示例

1.5 起的可选装饰资源包和本地示例（含 1.6 的 advanced-decoration-pack）使用用户提供的 test.png，保存为 sword.png，PNG 字节原样保留，素材权属归原作者。
整图 128×128，示例 JSON 只选取左上角 16×16 的剑区域；四个角共用同一张 PNG。该图片不内置于模组 JAR，不作为第二套 tooltip 背景或边框。

## 第三方依赖

MixinExtras 0.4.1 作为嵌套运行库包含在 JAR 中，遵循 MIT 许可证；其原始 LICENSE_MixinExtras 保留在内嵌 JAR 内。
Shulker Box Tooltip 与 Cloth Config 仅作为编译/测试依赖，不打包进发行模组。
