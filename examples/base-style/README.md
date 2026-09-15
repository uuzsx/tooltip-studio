# Tooltip Studio 1.4：只保留默认基础款

Fabric / Minecraft 1.20.4。新版只内置 default 一套样式，使用朋友提供的 Default.png 与 defalut.json；PNG 原样内置，JSON 仅调整贴图引用。旧极光图集、预设和图集生成脚本均已移除。

## 安装和升级

移除旧版 JAR，放入 tooltip-studio-1.4+1.20.4.jar，同一实例只保留一个版本。新配置的 defaultStyle 为 default，rules 为空。
旧实例不需要删除 config 文件夹：未修改的旧内置样式在原贴图缺失时会跳过，已经不存在的旧预设名称在规则、defaultStyle 和 TooltipStyle 中会自动回退到 default。
这个兼容处理仅发生在内存中，不修改或删除用户文件。自定义样式与有效同名资源包样式仍按原配置使用。

也可以手工把 config/tooltipstudio/config.json 中的对应项改成：

```json
"defaultStyle": "default"
```

保存后执行 `/tooltipstudio reload`。这是修改已有字段的片段，不要用它替换整个配置。
已经改动过的旧样式不会被自动替换；若仍引用已删除的旧 PNG，请更新为自己的图片及相应切片参数。

## 自定义基础款

default 内置可用，无需手动放 JSON。想调整参数时，把源码中的 defaults/styles/default.json 复制为 config/tooltipstudio/styles/default.json，即可覆盖内置定义。
texture 使用 `tooltipstudio:textures/styles/default.png` 可直接引用内置图片；也可把自己的 PNG 放入 config/tooltipstudio/textures/，使用 local:文件名.png。
资源包可放 assets/tooltipstudio/styles/default.json 或 assets/tooltipstudio/textures/styles/default.png。覆盖顺序为资源包样式 → 本地样式 → 内置 default。
删除本地 default.json 后恢复内置基础款，不会自动重建文件。本地修改后执行 reload，资源包修改后按 F3+T。

背景、细边框与分割线均使用提供的原始参数；名称居中，有正文时才显示分割线。要向上移动，可在样式最外层添加 `"offsetY": -12`。
分类目录、NBT 匹配、单图自定义、资源包和 XY 偏移等功能全部保留；只移除了旧素材。
