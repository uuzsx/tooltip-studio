# 动态装饰与单独贴图（1.8）

适用于 Minecraft 1.20.4 Fabric，需要 Tooltip Studio 1.8。

## 直接试用

把 `tooltip-studio-animated-decoration-pack-1.8+1.20.4.zip` 放进游戏的 `resourcepacks` 文件夹并启用，鼠标移到普通木棍上：左上角和右上角出现循环闪动的青色星光。
建议先只启用这一份 Tooltip Studio 示例包，避免其他示例规则抢先匹配。

- 左上角来自 `styles/animated/demo.json` 的 `decorations`，引用独立 PNG。
- 右上角来自 `decorations/animated/spark.json`，由 `decorationRules` 额外叠加。
- 背景、边框、分割线仍是内置 default，没有新增基础款。
- 星光是本项目生成的简单测试图，不包含参考 GIF 的火焰素材。

## 在样式的 decorations 中使用另一张贴图

把以下对象放入现有样式的 `decorations` 数组：

```json
{
  "texture": "tooltipstudio:textures/decorations/spark_strip.png",
  "textureWidth": 16,
  "textureHeight": 128,
  "region": {"u": 0, "v": 0, "width": 16, "height": 16},
  "animation": {"frames": 8, "frameTime": 2, "direction": "vertical"},
  "anchor": "TOP_LEFT",
  "x": -6,
  "y": -6,
  "foreground": true
}
```

PNG 放在资源包的 `assets/tooltipstudio/textures/decorations/spark_strip.png`。
`textureWidth`、`textureHeight` 填整张 PNG 的实际尺寸；`region` 选取第一帧。

只想单独引用一张静态贴图时，删除 `animation` 即可。
不填写装饰的 `texture`、`textureWidth`、`textureHeight`，就继续使用样式自身的贴图与尺寸，旧 JSON 无需修改。
填写 `texture` 时，宽高也必须一起填写；每个装饰都能选择不同的贴图，也可以共用同一张。
若将这个对象单独保存到 `decorations/`，同样支持，再通过 `decorationRules` 引用它的 ID。

## 如何制作动画 PNG

当前支持 **PNG 序列帧**，不直接播放 GIF/APNG，也不读取 `.png.mcmeta` 的 animation。
把 GIF 导出为同尺寸的帧，合成一张透明 PNG 即可。导出时需先合成每帧完整画面，保留透明背景。

例如每帧 16×16、共 8 帧：

- 纵向排列：整张 PNG 为 16×128，`direction` 填 `vertical`（默认）。
- 横向排列：整张 PNG 为 128×16，`direction` 填 `horizontal`。

帧紧挨着排列，中间不留空隙；第一帧也可以从图集里的非零 `u`、`v` 开始，后续每帧按单帧宽或高顺延。暂不支持网格排布、指定帧序列、逐帧时长或插值。

| 字段 | 用法 |
| --- | --- |
| `animation.frames` | 必填，1～256 帧；所有帧都必须落在 PNG 范围内 |
| `animation.frameTime` | 每帧持续的时间，单位为 1/20 秒；默认 2（100 毫秒），范围 1～1200 |
| `animation.direction` | `vertical` 或 `horizontal`；默认 `vertical` |
| `region.width` / `height` | 单帧大小，也是缩放前的装饰显示大小 |
| `textureWidth` / `textureHeight` | 整张 PNG 的实际大小；每边最多 4096 像素 |

`frameTime: 1` 是每秒 20 帧，`2` 是每秒 10 帧，`4` 是每秒 5 帧。
动画按真实经过时间持续循环，暂停菜单中也会播放；移动鼠标、换物品或重载不会从头重新播放。相同帧数和帧时长的装饰共享播放节奏。

原有 `anchor`、`x`、`y`、`x_scale`、`y_scale`、`foreground` 均可继续使用；分割线锚点也支持。动画只切换取图区域，不挤动名字、lore 或面板。文本装饰不支持 `animation`。

## 本地配置与热重载

本地样式可使用 `"texture": "local:decorations/spark_strip.png"`，PNG 放入 `config/tooltipstudio/textures/decorations/spark_strip.png`。
资源包中的 JSON 必须使用资源 ID，不能引用 `local:`。

本地 JSON/PNG 修改后运行 `/tooltipstudio reload`；资源包修改后按 F3+T。贴图缺失、尺寸不符或动画帧越界会拒绝本次重载，保留上一份有效配置。
