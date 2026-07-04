# 2D 数字人运维

## 巡检

本地或服务器都可以执行：

```bash
cd /opt/anyiapp2/backend
npm run digital-human:smoke -- https://api.anyibj.cn
```

脚本会依次检查：

- API `/health`
- App 配置 `/app/config`
- 数字人状态 `/app/digital-human/status`
- 数字人页面 `/vtuber/`
- 最新 APK 下载 `/downloads/anyi-memorial-latest.apk`

全部通过时会输出多行 `PASS`。任一环节失败时会输出 `FAIL` 并返回非 0 退出码。

## 临时关闭入口

如果数字人服务异常但主 App 还要继续使用，可以在服务器关闭入口：

```bash
cd /opt/anyiapp2/backend
sudo sed -i 's/^VTUBER_ENABLED=.*/VTUBER_ENABLED=false/' .env
sudo systemctl restart anyi-memorial-api
curl https://api.anyibj.cn/app/config
```

App 会提示“2D 数字人暂时维护中”，不会打开 WebView。

## 恢复入口

```bash
cd /opt/anyiapp2/backend
sudo sed -i 's/^VTUBER_ENABLED=.*/VTUBER_ENABLED=true/' .env
sudo systemctl restart anyi-memorial-api
npm run digital-human:smoke -- https://api.anyibj.cn
```

## 常用状态命令

```bash
systemctl is-active anyi-memorial-api
systemctl is-active open-llm-vtuber
systemctl is-active nginx
journalctl -u open-llm-vtuber -n 80 --no-pager
journalctl -u anyi-memorial-api -n 80 --no-pager
```
