# 安忆 HarmonyOS 原生版

此目录是独立的 HarmonyOS Stage 模型工程，使用 ArkTS、ArkUI 和 HarmonyOS 系统 Kit 实现，不依赖 Android 兼容层，也不是 Web 套壳。

## 技术基线

- HarmonyOS 6.0 Release / API 20
- DevEco Studio 6.0 Release 或更高版本
- ArkTS + ArkUI
- Network Kit：REST API 和图片上传
- AssetStoreKit：Bearer Token 安全存储
- ArkData Preferences：非敏感用户资料持久化
- Media Library Kit：系统图库选择器
- 手机和平板设备

采用 API 20 是为了使用当前可发布的稳定版 SDK。HarmonyOS 7 / API 26 的 `PATCH` 支持尚不作为本工程基线，因此后端同时提供语义相同的 `PUT` 更新接口。

## 已迁移功能

| 模块 | HarmonyOS 原生实现 |
| --- | --- |
| 账号 | 云端健康检查、注册、登录、会话恢复、登录过期处理 |
| 云端纪念馆 | 创建/编辑纪念馆、系统图库选图、照片上传、鲜花、蜡烛、敬香、苹果、榴莲解锁与供奉 |
| 人文社区 | 动态列表、图片动态、发布、点赞、删除、评论、义工招募与报名 |
| 管理员 | 义工报名列表、通过/拒绝审核、Web 管理后台入口 |
| 个人中心 | 昵称与头像更新、隐私/协议/AI 说明、退出、账号注销 |
| 多端 | 手机/平板自适应内容宽度，共用 Android 端的云端数据 |

AI 陪伴页已从鸿蒙端移除：它只调用后端早已不存在的 `/app/digital-human/*` 接口（生产返回 404），并从 `/app/config` 读取一个后端已不再返回的 `digitalHuman` 字段，因此页面无法工作。当前鸿蒙端保留纪念馆、人文社区、个人中心三个标签页；AI 陪伴仍在 Android 端提供，两端共用同一套云端数据。

Android 端的微信 OpenSDK 登录没有直接复制到鸿蒙工程。账号密码登录可直接使用；如需华为账号一键登录，应在 AppGallery Connect 创建应用、配置 Account Kit，并在后端增加授权码换取安忆 Token 的接口后接入，避免在客户端伪造登录或保存平台密钥。

## 打开和运行

1. 安装 DevEco Studio 6.0 Release 或更高版本，并在 SDK Manager 安装 HarmonyOS 6.0.0(20) SDK。
2. 在 DevEco Studio 中选择 **Open Project**，打开本 `huawei-harmonyos` 目录。
3. 在 **Project Structure > Signing Configs** 中启用自动签名或配置发布证书。
4. 选择 `entry` 模块和 HarmonyOS 模拟器/真机，点击 Run。
5. 发布构建使用 **Build > Build App(s)/Hap(s) > Build Hap(s)**。

首次把鸿蒙客户端连到生产环境前，需要重新部署本仓库的后端，使新增的 `PUT /me`、`PUT /memorials/:id` 和 `PUT /community/volunteer/applications/:id` 生效：

```powershell
cd D:\Desktop\anyiapp2\backend
npm ci
npm run check
npm run server:build
```

随后按 `backend/TENCENT_DEPLOY.md` 的现有流程部署。

## 配置

API 地址在以下文件中集中维护：

```text
entry/src/main/ets/common/Constants.ets
```

默认地址为：

```text
https://api.anyibj.cn
```

生产发布前请同时确认：

- AppGallery Connect 中的包名为 `com.anyi.memorial`。
- 隐私政策和用户协议已替换为最终审核版本。
- 签名、图标、截图和应用分级信息已完成。
- 线上后端已部署 HarmonyOS 所需的 `PUT` 兼容路由。
- 榴莲供品当前沿用项目已有的测试解锁接口；正式支付上线前应接入 Payment Kit/IAP Kit 和服务端验签。

## 目录结构

```text
huawei-harmonyos/
├─ AppScope/                         # 应用级配置和图标
├─ entry/src/main/module.json5       # 模块、设备与权限声明
├─ entry/src/main/ets/
│  ├─ entryability/                  # UIAbility 入口
│  ├─ common/                        # API 地址、主题与常量
│  ├─ models/                        # 云端数据模型
│  ├─ services/                      # 网络、会话和系统图库
│  ├─ components/                    # 通用 ArkUI 组件
│  └─ pages/                         # 登录、主框架与四个业务页
└─ entry/src/main/resources/         # 复用的安忆视觉素材
```
