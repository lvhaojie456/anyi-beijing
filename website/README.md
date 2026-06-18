# 安忆官网

这是安忆 Android App 的静态官网，入口文件是 `index.html`。

## 本地预览

直接打开：

```text
D:\Desktop\anyiapp2\website\index.html
```

## 部署到腾讯云 CVM Nginx

示例目录：

```bash
sudo mkdir -p /var/www/anyi-memorial-site
sudo rsync -av /opt/anyiapp2/website/ /var/www/anyi-memorial-site/
```

Nginx 示例文件：

```text
D:\Desktop\anyiapp2\website\examples\nginx-site.conf
```

配置 HTTPS 后，把域名解析到腾讯云 CVM 公网 IP。

## 下载链接

当前 APK 下载链接由后端域名提供：

```text
https://api.anyibj.cn/downloads/anyi-memorial-latest.apk
```
