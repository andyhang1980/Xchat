# Xchat — Agent Guide

## Build

```bash
./gradlew :app:assembleDebug   # debug (uses same signing as release)
./gradlew :app:assembleRelease # release (with optimization on)
```

- JDK 21
- Rust native lib auto-compiles during build (targets: `app/src/main/rust/xchat-native`). Requires:
  Rust toolchain + Android NDK targets + NDK. `configureCargo` task auto-generates `.cargo/config.toml`
  from NDK.
- AGP 9, Gradle version catalog in `gradle/libs.versions.toml`

## Project Structure

- `app/` — main Android module, entrypoints, hooks, UI, native Rust lib
- `libs/common/annotation-scanner/` — KSP annotation processor (`@Feature` scanner)
- `libs/common/libxposed-api/` — compileOnly LibXposed API interface stubs (compileOnly since they are provided by user's Xposed framework)
- `libs/common/bsh/` — submodule: forked BeanShell interpreter with snapshot serialization (`BshSnapshot`, `BshSnapshotHelper`); snapshots are encrypted AST byte representations used by the WAuxiliary Xposed module; `app/src/main/java/io/github/xchat/utils/BshSnapshotDecompiler.kt` — decompiles encrypted BeanShell snapshot files back into Java-like source code; the AES key was recovered from WAuxiliary's decompiled source
- `libs/common/reflekt/` — submodule: reflection utility library (`dev.ujhhgtg.reflekt`)
- `libs/common/stubs/` — compileOnly stubs for WeChat and Android hidden classes
- `libs/external/comptime-kt/` — submodule: compile-time reflection utility
- `buildSrc/` — custom Gradle tasks: `GenerateMethodHashesTask` (`IResolveDex` `resolveDex` method MD5 cache), `ConfigureCargoTask` (Rust NDK linker config)

## Entry Points & Architecture

- Xposed entry: `io.github.xchat.loader.entry.lsp10x.Lsp10xUnifiedHookEntry` (libxposed 101 & 100) and legacy Xposed API (51+) entry: `io.github.xchat.loader.entry.xp51.Xp51HookEntry`
- Unified flow: `UnifiedEntryPoint.entry()` → `StartupAgent.startup()` → `WeLauncher.init()`
- Hook items annotated with `@Feature(path, description)`, auto-discovered by KSP annotation scanner at compile time
- Base classes: `SwitchFeature` (toggle on/off), `ClickableFeature` (toggle on/off with onClick event), `ApiFeature` (always-on), `BaseFeature` (abstract base, do not use directly)
- DEX analysis via DexKit with `IResolveDex` interface; method resolve body MD5-hashed for cache (
  `GenerateMethodHashesTask`)
- DEX-resolved targets DSL: `val methodTarget by dexMethod()` `val classTarget by dexClass()` delegate → `methodTarget.hookBefore { ... }`, `val method: Method = methodTarget.method`, `val clazz = classTarget.clazz`
- UI: Jetpack Compose + Material 3, dialogs written using `showComposeDialog` and `AlertDialogContent`
- Config: MMKV via `WePrefs`
- Logging: via `WeLogger`

## Key Conventions

- Package namespace: `io.github.xchat`
- Min SDK 29, target SDK 37, compile SDK 37
- Target: WeChat `com.tencent.mm`, versions 8.0.65–8.0.78. Version info in `HostInfo`
- WeChat 8.0.77+ requires `allowFailure = true` on DexKit matchers for methods that may have changed; use `isPlaceholder` checks before calling `.method`

## DexKit 适配说明

### 版本感知方法查找

`HostInfo.MMVersion` 提供版本常量：`MM_8_0_65`, `MM_8_0_70`, `MM_8_0_71`, `MM_8_0_76`, `MM_8_0_77`, `MM_8_0_78`。`isNewWeChatVersion` 快速检查是否为 8.0.77+。

### 适配策略

对于新版本微信（8.0.77+），DexKit 方法匹配可能失败。策略：
1. 所有可能变化的 `dexMethod` 添加 `allowFailure = true`
2. 在 `onEnable()` 中检查 `delegate.isPlaceholder` 再调用 `.method`
3. 使用 `runCatching` 包裹可能失败的操作
4. 提供备用反射方案

## WCX 项目集成

- WCX 项目 (https://github.com/Johnny520/wcx) 是 WeKit 的二改分支，包含更多功能
- Xchat 已完整集成 WCX 所有功能模块，共 206 个功能文件
- 已添加的 WCX 新功能包括：
  - **朋友圈**：`AntiMomentCommentsDelete`, `AutoRepostMoments`, `MomentAutomationAction/Mode/IntervalRule/ModeRule/TypeRule`, `MomentsAutomationSettings`, `MomentsKeywordFilter`, `OpenDetailsOnItemClick`, `RepostMoments`, `AutoRefresh`, `AlwaysShowInteractionEntry`
  - **联系人**：`AutoAcceptFriendRequests`, `AutoAddNearbyFriends`, `AutoDndAfterJoinGroup`, `SplitGroupCall`
  - **系统**：`ApiServer`
  - **界面美化**：`ThemeImportExport`, `CustomColorScheme`, `BottomNavBeautify`, `MePageSimplification`, `DialogBlur`, `HideRecentPage`
  - **批量操作**：`BatchAddLabel`, `BatchDbOps`, `BatchDeleteChatHistory`, `BatchDeleteFriends`, `BatchHideConversations`, `BatchMarkAsRead`, `BatchMuteConversations`, `MassSendMessage`
  - **视频号/小程序/公众号**：`DownloadMedia`, `DisableCommentSizeLimit`, `ErudaConsole`, `RemoveEmbeddedAds`, `RemoveMenuLimits`, `RemoveSplashAds`, `RemoveVideoAds`, `SkipSplash`, `SpoofHostVersion`
  - **VoIP**：`BlockVoipRingtone`, `PipVoip`, `RemoveLimitsDuringCalls`, `VirtualVoipVideo`
  - 其他：`BlacklistMode`, `AprilFools`, `KillHostProcess`, `MarkAllAsRead`, `ModuleSettings`, `NotificationsEvolved`, `JavaEngine`, `JavaHookApi`, `DecompileBeanShellSnapshot`
- WCX 特有的额外功能详见各 `features/items/*/` 子目录

## Key Conventions

- Package namespace: `io.github.xchat`

- 群聊: WeChat: chatroom; Xchat: group/群组
- 朋友圈: WeChat: sns; Xchat: moment

## Context you need

- WeChat decompiled sources: ~/coding/wechat_8074
- Decrypted WeChat main database: ./decrypted_wechat.db

## CI

- GitHub Actions: builds on push/PR to `master` (skips non-code changes)
- Artifacts automatically published to a release named "CI" + Telegram channel
