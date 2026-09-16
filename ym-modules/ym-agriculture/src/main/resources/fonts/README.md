# 溯源 PDF 标签中文字体

标签 PDF 使用 [Noto Sans SC](https://fonts.google.com/noto/specimen/Noto+Sans+SC)（SIL Open Font License 1.1），通过 `PDType0Font` 嵌入，不依赖服务器安装字体。

## 文件

| 文件 | 说明 |
|------|------|
| `NotoSansSC-Regular.ttf` | 正文、批次号、固定文案 |
| `NotoSansSC-Bold.ttf` | 商品名（可选；缺失时与 Regular 相同） |

仅提供 `NotoSansSC-Regular.ttf` 即可，商品名加粗会回退为同一字体。

## 获取字体

从 [noto-fonts](https://github.com/googlefonts/noto-fonts/tree/main/hinted/ttf/NotoSansSC) 下载 `NotoSansSC-Regular.ttf`、`NotoSansSC-Bold.ttf` 放入本目录。

或在本机已安装 Noto Sans SC 时（Windows 常见为 `NotoSansSC-VF.ttf`），复制并重命名为上述文件名。

构建后应出现在：`target/classes/fonts/NotoSansSC-Regular.ttf`。
