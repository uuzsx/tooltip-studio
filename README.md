## 创建资源包
使用Tooltip Studio并借助资源包来添加新的Tooltip样式时，资源包的对应格式应如下：

├─ pack.mcmeta
├─ pack.png
└─ assets/tooltipstudio/
   ├─ styles/
   │  └─ ...
   ├─ decorations
   │  └─ ...
   ├─ rules/
   │  └─ ...
   └─ textures/
      └─ ...

在`styles`文件夹中，会放置tooltip框样式定义 .json 文件
在`decoration`文件夹中，会放置tooltip装饰组件定义 .json 文件
在`rules`文件夹中，会放置tooltip规则匹配 .json 文件
在`textures`文件夹中，会放置创建tooltip或者装饰样式时所需要的材质贴图文件

## 创建Tooltip样式、装饰以及匹配规则
# Tooltip 样式
一个最基础的Tooltip样式定义文件如下：

```json
{
    "texture": "tooltipstudio:textures/styles/default.png",
    "textureWidth": 128,
    "textureHeight": 128,
    "offsetX": 0,
    "offsetY": 0,
    "background": {
        "u": 32,
        "v": 0,
        "width": 16,
        "height": 16
    },
    "frame": {
        "region": {
            "u": 0,
            "v": 0,
            "width": 16,
            "height": 16
        },
        "left": 2,
        "top": 2,
        "right": 2,
        "bottom": 2
    },
    "separator": {
        "enabled": true,
        "region": {
            "u": 0,
            "v": 17,
            "width": 16,
            "height": 2
        },
        "leftCap": 2,
        "rightCap": 2,
        "inset": 0,
        "marginTop": 3,
        "marginBottom": 4
    },
    "padding": {
        "left": 9,
        "top": 9,
        "right": 9,
        "bottom": 9
    },
    "minWidth": 100,
    "maxWidth": 320,
    "decorations": [
        {
            "region": {
                "u": 63,
                "v": 37,
                "width": 16,
                "height": 27
            },
            "anchor": "BOTTOM_RIGHT",
            "x": 8,
            "y": 3,
            "x_scale": 0.5,
            "y_scale": 0.5,
            "foreground": true
        }
    ]
}
```
以下是各字段的说明：
| 字段 | 含义 |
|---|---|
| `texture` | 创建Tooltip样式时所需要的材质的路径，其格式为 `<命名空间>:textures/xxx/xxx.png` |
| `textureWidth` | 所使用材质的宽度，必须与`texture`所引用的材质宽度一致 |
| `textureHeifht` | 所使用材质的高度，必须与`texture`所引用的材质宽度一致 |
| `offsetX` | Tooltip显示位置的X轴偏移 |
| `offsetY` | Tooltip显示位置的Y轴偏移 |
| `background` | Tooltip的背景框所使用的材质区域。 `u`和`v`参数可以确定覆盖区域的起始坐标，`{u:0,v:0}`意味着起始坐标为材质的左上角。`width`与`height`参数则用于确定覆盖区域的长和宽 |
| `frame` | Tooltip边框所使用的材质区域。 `region`中填写的参数与`background`中的一致，用以确定材质使用区域。`left`、`right`、`top`、`bottom`参数用于确定边框的四个角的宽度。用于连接四个角的边框部分会以延展的方式渲染，材质的中间部分则会直接被抛弃。 |
| `separator` | Tooltip中用于分割`名称`与`Lore文本`的分割线。`enable`参数能决定是否使用分割线，为布尔值。`region`中填写的参数与`background`中的一致，用以确定材质使用区域。`leftCap`与`leftCap`参数用于确定分割线的左右宽度，中间部分在渲染时会延展渲染。`inset`参数用于确定分割线相对正文区域左右端的缩进距离。`marginTop`、`marginBottom`分别用于确定分割线距离上方名称和下方文本的宽度 |
| `padding` | 用于确定边框距离文本的宽度，分别使用`left`、`right`、`top`、`bottom`四个参数确定上下左右四个方向的距离 |
| `minWidth` | 用于确定Tooltip渲染时的最小宽度 |
| `maxWidth` | 用于确定Tooltip渲染时的最大宽度 |
| `decorations` | 装饰物，为数组，最多容纳`64`个装饰。每个装饰物中，`region`中填写的参数与`background`中的一致，用以确定材质使用区域。`anchor`参数用于确定装饰的锚点位置，目前支持`TOP`、`TOP_LEFT`、`TOP_RIGHT`、`CENTER`、`LEFT`、`RIGHT`、`BOTTOM`、`BOTTOM_LEFT`、`BOTTOM_RIGHT`、`SEPARATOR_CENTER`、`SEPARATOR_LEFT`、`SEPARATOR_RIGHT`。`x`和`y`参数用于确定装饰的偏移。`x_scale`和`y_scale`参数可以控制装饰物的缩放。`foreground`参数为布尔值，`false`在文字前绘制，`true`在文字前绘制。各装饰的渲染按照在数组内的顺序叠加渲染。 |

# 装饰样式
1. 贴图装饰
贴图装饰的定义格式与Tooltip定义格式中的Decoration定义类似
```json
{
    "texture": "tooltipstudio:textures/styles/default.png",
    "textureWidth": 128,
    "textureHeight": 128,
    "region": {
        "u": 63,
        "v": 37,
        "width": 16,
        "height": 27
    },
    "anchor": "BOTTOM_RIGHT",
    "x": 8,
    "y": 3,
    "x_scale": 0.5,
    "y_scale": 0.5,
    "foreground": true
}
```
具体可以查看Tooltip 样式定义部分中的`decoration`部分。贴图装饰定义需要单独的`texture`引用及其宽度高度参数

2. 文本装饰
文本装饰的第一种定义格式如下：
```json
{
    "type": "text",
    "text": "Bow",
    "color": "#cf854c",
    "bold": true,
    "italic": false,
    "shadow": true,
    "anchor": "SEPARATOR_CENTER",
    "x": 0,
    "y": -3,
    "foreground": true,
    "x_scale": 0.65,
    "y_scale": 0.65
}
```
文本装饰所使用的参数与贴图装饰大体一致，这里特别说明一下文本装饰所需要的参数
| 字段 | 含义 |
|---|---|
| `type` | 装饰类型，不存在时默认使用`texture`，想使用文本装饰则需要设置为`text` |
| `text` | 你想使用的文本内容 |
| `color` | 文本所使用的颜色，颜色代码格式为十六进制格式 |
| `bold` | 布尔值，决定文本是否是粗体 |
| `italic` | 布尔值，决定文本是否是斜体 |
| `shadow` | 布尔值，决定文本是否有阴影 |

文本装饰还有第二种格式
```json
{
    "type": "text",
    "color": "#AAAAAA",
    "segments": [
        {"text": "text1"},
        {"text": "text2", "color": "#FFD24A", "bold": true},
        {"text": "text3", "color": "#C06A38", "italic": true}
    ],
    "color": "#cf854c",
    "bold": true,
    "italic": false,
    "shadow": true,
    "anchor": "SEPARATOR_CENTER",
    "x": 0,
    "y": -3,
    "foreground": true,
    "x_scale": 0.65,
    "y_scale": 0.65
}
```
将`text`参数替换为`segments`参数，可以支持多段文本的拼合
对于内部已写`color`、`bold`和`italic`参数的文本，不受外部参数的影响

文本装饰同样可以放入Tooltip定义格式的`decorations`中

# 匹配规则
以下是一个最基础的匹配规则的定义文件
```json
{
    "schemaVersion": 1,
    "rules": [
        {
            "style": "valley/light_blue",
            "priority": 300,
            "nbt": {
                "Monumenta.Location": "lightblue",
                "Monumenta.Tier": "rare"
            }
        }
    ],
    "decorationRules": [
        {
            "decorations": [
                "valley/light_blue/item_type/wand"
            ],
            "priority":200,
            "items": [
                "minecraft:enchanted_book"
            ],
            "rarities": [
                "epic"
            ],
            "tags": [
                "minecraft:swords"
            ]
        }
    ]
}
```
`rules`和`decorationRules`分别控制对`Tooltip样式`和`装饰样式`的规则匹配，这两个参数均是数组
`rules`中的`style`参数确定匹配成功后所使用的Tooltip样式，若填写为`xxx/xxx`，则视为路径`tooltipstudio:styles/xxx/xxx`
`decorationRules`中的`decoration`参数确定匹配成功后所使用的装饰样式，若填写为`xxx/xxx`，则视为路径`tooltipstudio:decorations/xxx/xxx`
`priority`参数确定匹配的优先级，在一件物品同时满足多个匹配规则的情况下，优先使用参数高的匹配项
匹配规则目前支持4种：
| 字段 | 含义 |
|---|---|
| `nbt` | 对物品nbt标签的匹配 |
| `items` | 对物品id的匹配 |
| `rarities` | 对物品稀有度的匹配 |
| `tags` | 对物品标签的匹配 |

## 命令
# 重载
使用指令`/tooltipstudio reload`重新加载各种文件
# 查看
`/tooltipstudio decorations`列出已有的装饰样式
`/tooltipstudio list`列出已有的Tooltip样式