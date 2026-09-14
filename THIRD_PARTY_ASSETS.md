# 临时示例素材

9 套样式的边框与装饰读取自用户明确指定的本地目录：

`D:/极光/src/main/resources/assets/theaurorian2/textures/gui/tooltips`

这里只进行 PNG 图集打包和必要的边框区域提取，没有修改原项目。
图集中的深色背景像素由打包脚本生成；默认分割线复用边框顶部的像素条。
输出路径为 `src/main/resources/assets/tooltipstudio/textures/styles/*.png`。

极光示例图片保留原始权利归属；本项目未授予这些图片新的公开分发许可。
后续可将每套 PNG 替换成自己的素材，并更新对应 JSON 中的区域坐标。

MixinExtras 0.4.1 作为嵌套运行库包含在 JAR 中，遵循 MIT 许可证；其原始 LICENSE_MixinExtras 保留在内嵌 JAR 内。Shulker Box Tooltip 与 Cloth Config 仅作为编译/测试依赖，不打包进发行模组。
