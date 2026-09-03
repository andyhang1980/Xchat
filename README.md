# Xchat

适用于微信的 Xposed 模块 — 微信聊天增强、语音工作台与文字转语音

基于 [WeKit](https://github.com/Ujhhgtg/WeKit) / [WAuxiliary](https://github.com/HdShare/WAuxiliary_Public) 整合重命名，并集成 [Wtonec](https://github.com/tianxing226/wtonec) 的语音工作流设计。

## 特性

- **完整的微信功能增强**（继承自 WeKit / WAuxiliary）：防撤回、防删、自动转发、批量操作、消息增强、朋友圈增强、支付增强、VoIP 增强等 190+ 功能。
- **语音工作台**（集成 Wtonec 语音工作流设计）：
  - 文字转语音（Edge TTS，免费无需 API Key，支持音色/语速/音调/音量调节、试听、生成发送）
  - 本地语音包（导入 / 试听 / 发送）
  - 长按聊天输入栏「语音」按钮打开面板
- 基于 Xposed / LSPosed，Hook + DexKit + Jetpack Compose 架构。

## 说明

**在 `Xposed 框架` 中激活本模块后并将 `com.tencent.mm` 加入作用域**，在微信自带设置中点击「Xchat 设置」即可进入设置页面。

- Android >= `10.0`
- WeChat >= `8.0.65`（目标版本 `8.0.65` ~ `8.0.71`）
- 本模块使用了隐藏 DEX 加载技术，你需要关闭 `Xposed API 调用保护` 才能加载模块功能。

## 构建

```bash
./gradlew :app:assembleRelease
```

- JDK 21
- Rust native lib（`app/src/main/rust/xchat-native`）在构建时自动编译，需要 Rust toolchain + Android NDK targets + NDK。

## 致谢

- [WeKit](https://github.com/Ujhhgtg/WeKit)
- [WAuxiliary](https://github.com/HdShare/WAuxiliary_Public)
- [Wtonec](https://github.com/tianxing226/wtonec)
- [NewMiko](https://github.com/dartcv/NewMiko/blob/archives/)
- [QAuxiliary](https://github.com/cinit/QAuxiliary)
- [FingerprintPay](https://github.com/eritpchy/FingerprintPay)
- [WADN](https://github.com/Ujhhgtg/wauxv_deobf_new) / [WAD](https://github.com/Ujhhgtg/wauxv_deobf)

## 免责声明

本项目为**非盈利性质、开源免费**的学习交流工具，仅用于技术研究、代码学习、功能测试、调试分析与个人开发实践之目的，严禁将其用于任何违反法律法规、侵犯他人合法权益、破坏平台规则、干扰正常服务、实施未经授权访问、数据抓取、批量操作、恶意自动化、隐私窃取、账号盗用、风控规避、攻击破坏或其他任何不当用途。

本项目仅供学习交流与研究参考，任何人不得将其用于非法、违规、侵权、破坏性或其他不当用途。使用即代表自担全部风险，自负全部责任。
