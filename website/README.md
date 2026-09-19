# 安忆官网

安忆 Android App 的静态官网，入口文件是 `index.html`。纯静态，没有构建步骤，
没有 JavaScript —— 站内全部是 HTML 与 CSS，CSP 里 `script-src 'none'`。

## 两个部署目标

同一份源码目前同时服务两个地址：

| 目标 | 地址 | 说明 |
| --- | --- | --- |
| Cloudflare Pages | `https://anyi-memorial-site.pages.dev` | 主要访问入口，国内外都能打开 |
| 腾讯云 CVM Nginx | `http://anyi.anyibj.cn` | 备用入口，仅 HTTP，未配 TLS |

因为两边的路径都是相对的（`./styles.css`、`./assets/...`），同一份文件可以直接互用。

## 发布到 Cloudflare Pages

已登录的 wrangler 会自动带上 Pages 项目的凭证，直接发布即可：

```bash
cd website && npx wrangler@latest pages deploy . --project-name anyi-memorial-site --branch main
```

`wrangler.toml` 里已经写好 `name` 与 `pages_build_output_dir`，在 `website/` 目录下执行时
也可以省掉 `--project-name`。

安全响应头与静态资源缓存由 [_headers](./_headers) 声明，Pages 会在边缘自动应用。

## 发布到腾讯云 CVM

```bash
sudo rsync -av website/ /var/www/anyi-memorial-site/
```

Nginx 示例配置见 [examples/nginx-site.conf](./examples/nginx-site.conf)。

## 本地预览

```bash
cd website && python3 -m http.server 8080
```

然后打开 `http://localhost:8080/`。直接双击 `index.html` 也能看，但相对路径的资源
在 `file://` 下偶尔会被浏览器拦，起个本地服务更稳。

## 有哪些内容

- 首屏：产品定位 + 三张手机截图
- 核心功能：云端纪念馆、AI 陪伴、动态形象与语音、绿色追忆、人文社区与互助、个人设置
- 语音陪伴：说明音色选择、口型跟随、语音条留存
- 截图：四张来自线上最新版 App 的真实截图
- 下载：APK 直链 + 扫码二维码
- 隐私：四份法律文档入口

## 需要同步维护的地方

改产品时，官网这几处容易忘：

1. `index.html` 里的功能卡文案，要与 App 实际菜单一致。
2. `assets/` 下的截图，来自真机/模拟器截图，产品改版后需要重截。
3. `assets/download-qr.png` 指向 `https://api.anyibj.cn/downloads/anyi-memorial-latest.apk`，
   API 域名或下载路径变更时需要重新生成。
4. 底部邮箱 `544908186@qq.com` 是客服联系方式，与后端法律页面保持一致。
