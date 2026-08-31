# AI 陪伴界面参考

本次 Android AI 陪伴重做只参考公开项目的交互结构，界面和业务代码在安忆项目内重新实现，没有复制第三方源码，也没有引入聊天 SDK。

- [Android Compose Samples / Jetchat](https://github.com/android/compose-samples/tree/main/Jetchat)：参考固定顶部栏、消息 `LazyColumn`、底部输入区和稳定消息 ID 的组织方式。
- [chengdongqing/WeChat](https://github.com/chengdongqing/WeChat)：参考微信式会话行的信息层级，包括头像、姓名、最后消息、时间和分割线。
- [GetStream / stream-chat-android](https://github.com/GetStream/stream-chat-android)：只参考 loading、empty、发送状态和头像占位的状态拆分思路，未引入其 SDK 或源码。

安忆当前实现位于 `app/src/main/java/com/anyi/memorial/AiCompanionScreen.kt`，网络契约位于 `app/src/main/java/com/anyi/memorial/network/AnyiApiClient.kt`。
