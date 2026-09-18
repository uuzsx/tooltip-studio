# Tooltip Studio 1.7 独立装饰资源包

Fabric / Minecraft 1.20.4。安装 Tooltip Studio 1.7 后，将本 ZIP 放入 resourcepacks 并启用。

- 云杉木门：左上角添加小剑。
- 任意物品的 NBT `Monumenta.Location = "forest"`：左上角添加小剑。
- 任意物品的 NBT `TooltipDecorations = "all_corners"`：四角添加小剑。

未命中不添加装饰。多条规则命中同一个装饰 ID 时只画一次。该包不包含任何基础样式 JSON，也不修改当前 tooltip 的背景、边框和分割线。

PNG 原样使用用户提供的 test.png，整图为 128×128，剑区域为左上角 16×16。四个角演示同一张剑，各角可分别改 texture、region、anchor、x、y。

装饰位于 assets/tooltipstudio/decorations/sword/，规则在 assets/tooltipstudio/rules/independent_decorations.json。
使用 Tooltip Studio 自己的 JSON 格式。需要完整本地配置与参数说明时，查看项目的 examples/independent-decorations/README.md。
