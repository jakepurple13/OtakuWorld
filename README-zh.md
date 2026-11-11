# 🌐 支持的语言

[English](README.md)
- 🇨🇳 中文
- 🇺🇸 English
# 御宅族世界

 ![Build Status](https://github.com/jakepurple13/OtakuWorld/actions/workflows/build_check.yaml/badge.svg)
 ![Build Status](https://github.com/jakepurple13/OtakuWorld/actions/workflows/nightly_release.yaml/badge.svg)
 ![Build Status](https://github.com/jakepurple13/OtakuWorld/actions/workflows/main_release.yml/badge.svg)
 
 [Discord 链接](https://discord.gg/MhhHMWqryg) 现在有一个 discord 了！！！由于这是我第一次这样做，所以可能会有一些变动和修改。

Nightly 版本请看[这里](https://github.com/jakepurple13/OtakuWorld/releases/tag/nightly)

我的作品的综合版本
 [MangaWorld](https://github.com/jakepurple13/MangaWorld) and [AnimeWorld](https://github.com/jakepurple13/AnimeWorld)
 应用程序。这是因为两个应用程序之间有大量的共享元素，所以我将它们合并在一起，这样就有了两个独立的应用程序，但有很多相同的基本逻辑。

MangaWorld、AnimeWorld 和 NovelWorld 可在 Android 和桌面端（Windows、macOS、Linux）上运行。

这些应用本身不包含任何来源。来源是扩展，可以在应用内的“扩展”页面安装。该页面提供目录书架、已安装扩展，以及带安装进度的扩展详情。

## 扩展

- **MangaWorld：** 已安装的 [Mihon](https://mihon.app/)/Tachiyomi 扩展可直接加载，无需桥接应用。在“扩展”中添加 Mihon
  仓库 URL 后，该仓库的签名密钥会把其扩展标记为可信。应用内浏览和安装同时支持旧版 `index.min.json` 和 Mihon 新的
  `index_v2` 格式。桌面端也可以安装和使用 Mihon 扩展（不支持 Cloudflare 保护的来源；来源设置仅限 Android）。
- **AnimeWorld：** [Aniyomi](https://aniyomi.org/) 扩展 APK 和
  [CloudStream](https://github.com/recloudstream/cloudstream) `.cs3` 插件可直接加载，无需桥接应用。仓库必须先被信任，其扩展才会加载。
  两者在桌面端也可使用（不支持 Cloudflare 保护的来源；需要 Android 资源或 WebView 的插件以及扩展设置仅限 Android）。
- **JavaScript/TypeScript 扩展：** 一种沙盒化的扩展格式（QuickJs），在所有应用的 Android 和桌面端上运行。
  参见 [编写 JS/TS 扩展](docs/js_ts_extension.md)。

## 云同步

云同步使用你自己的 [Supabase](https://supabase.com) 项目。在应用中登录后，可以为每台设备选择要同步的表。
即使使用同一账号登录，MangaWorld、AnimeWorld 和 NovelWorld 的数据也彼此独立。Supabase 配置可以通过二维码在设备之间共享。
设置方法见 [docs/supabase/SETUP.md](docs/supabase/SETUP.md)。在按应用区分同步之前创建的项目，需要运行一次
`docs/supabase/migration_add_app_column.sql`。

Firebase 同步和自定义服务器同步已不再使用。

## 目 录
   * [扩展](#扩展)
   * [云同步](#云同步)
   * [截图](#截图)
      * [漫画世界](#漫画世界)
      * [动漫世界](#动漫世界)
      * [动漫世界电视](#动漫世界电视)
      * [小说世界](#小说世界)
      * [宅文化管理者](#宅文化管理者)
   * [特征](#特征)
   * [AnimeWorldTV 和宅文化管理者](#animeworldtv-和宅文化管理者)
   * [本地构建](#本地构建)
   * [桌面应用](#桌面应用)
   * [问题](#问题)
   * [拉取请求](#拉取请求)

## [最新版本](https://github.com/jakepurple13/OtakuWorld/releases/latest)

# 截图
<p align="center">
  <img src="/mangaworld/src/main/ic_launcher-playstore.png" width="32%"/>
  <img src="/animeworld/src/main/ic_launcher-playstore.png" width="32%"/>
  <img src="/novelworld/src/main/ic_launcher-playstore.png" width="32%"/>
  <img src="/otakumanager/src/main/ic_launcher-playstore.png" width="32%"/>
</p>

### 漫画世界
<p align="center">
  <img src="/ss/mw_recent.png" width="32%"/>
  <img src="/ss/mw_all.png" width="32%"/>
  <img src="/ss/mw_details.png" width="32%"/>
  <img src="/ss/mw_notifications.png" width="32%"/>
  <img src="/ss/mw_top_settings.png" width="32%"/>
  <img src="/ss/mw_more_settings.png" width="32%"/>
</p>

### 动漫世界
<p align="center">
  <img src="/ss/aw_recent.png" width="32%"/>
  <img src="/ss/aw_all.png" width="32%"/>
  <img src="/ss/aw_details.png" width="32%"/>
  <img src="/ss/aw_history.png" width="32%"/>
  <img src="/ss/aw_top_settings.png" width="32%"/>
  <img src="/ss/aw_more_settings.png" width="32%"/>
</p>

### 动漫世界电视
<p align="center">
  <img src="/ss/tv_ss_homescreen.png" width="32%"/>
  <img src="/ss/tv_ss_details.png" width="32%"/>
  <img src="/ss/tv_ss_episodes.png" width="32%"/>
  <img src="/ss/tv_ss_settings.png" width="32%"/>
  <img src="/ss/tv_ss_search.png" width="32%"/>
  <img src="/ss/tv_ss_favorites.png" width="32%"/>
</p>

### 小说世界
<p align="center">
  <img src="/ss/nw_recent.png" width="32%"/>
  <img src="/ss/nw_all.png" width="32%"/>
  <img src="/ss/nw_details.png" width="32%"/>
  <img src="/ss/nw_global_search.png" width="32%"/>
  <img src="/ss/nw_favorites.png" width="32%"/>
  <img src="/ss/nw_settings.png" width="32%"/>
</p>

### 宅文化管理者
_目前未构建。参见 [AnimeWorldTV 和宅文化管理者](#animeworldtv-和宅文化管理者)。_
<p align="center">
  <img src="/ss/otakumanager_ss_1.png" width="32%"/>
  <img src="/ss/otakumanager_ss_2.png" width="32%"/>
  <img src="/ss/otakumanager_ss_3.png" width="32%"/>
</p>

# 特征

### 共享特征
- 登录后，通过你自己的 Supabase 项目在设备之间同步收藏、历史、列表以及已读章节/已看剧集
- 收藏作品以接收更新提醒。更新通知会列出每部作品的新章节或新剧集，“更新检查错误”页面显示失败项，可重试和分享。可选择更新检查跳过哪些来源
- 收藏页分为“概览”和“书库”两个标签页，支持筛选、A–Z 快速跳转，以及对标题、简介和笔记的全文搜索
- 自定义列表：封面、排序、网格/列表视图、筛选、多选，以及在列表之间移动作品
- 为任意作品添加笔记
- 通知时间线，支持多选，并提供可滑动的“补看”卡片组
- 历史按天分组，支持搜索、按来源筛选和撤销
- 全局搜索：每个来源有独立的结果页，滚动时持续加载
- 详情页章节多选：批量标记已读/未读、书签、下载或删除
- 快捷操作（“最近”和“设置”页的闪电按钮；桌面端为 Cmd/Ctrl+K）：检查更新、扫描二维码、后台任务、更新检查错误、每日概览、全局搜索、列表和已保存的通知
- 每日概览：回顾昨天的阅读或观看，包含按小时统计的图表。摘要文字可由设备端或本地 AI 模型生成（Android 上为 Gemini Nano 或 Gemma，桌面端为 Ollama）
- 备份与恢复向导：精确选择要备份或恢复的内容，可细到单个列表
- 个人词典/术语表：在设置中管理条目，或在 MangaWorld 阅读时快速添加
- 通过带分享卡片的二维码分享作品，并在应用中打开分享链接
- 账号统计、活动计时，以及显示构建信息的“关于”页面
- 简介翻译：Android 上可下载离线语言；桌面端使用 Ollama 或任何兼容 OpenAI 的服务器
- AI 聊天，支持本地模型管理
- 带撤销/重试操作的应用内横幅、配色方案、模糊样式、生物识别锁，以及包含“提示与手势”页面的引导流程
- 仅限 Android：主屏幕小部件、屏保（Daydream）、Credential Manager 登录
- 仅限桌面端：原生标题栏、键盘快捷键（Cmd/Ctrl+/ 显示列表），以及 Nightly 或 Stable 渠道的应用内更新

### 漫画世界
- 从 Mihon/Tachiyomi 扩展及其他已安装扩展阅读漫画
- 内置阅读器，支持列表和翻页模式、章节书签，以及快速添加到词典
- 下载章节（桌面端可选择下载文件夹）

### 动漫世界
- 从 Aniyomi 扩展、CloudStream 插件及其他已安装扩展播放和下载动漫
- 内置视频播放器，提供 Classic、Floating 和 Theater 三种样式、+1:30 跳过按钮，以及手势调节亮度/音量
- 全屏剧集加载页面，带来源选择器和“重试 / 换一个来源”
- 字幕：轨道选择、带实时预览的样式设置，以及播放器内快捷面板
- 剧集下载，包括 HLS（AES-128 和 fMP4）流，离线播放时保留字幕
- 下载页面：实时进度、可重试的失败下载，以及“释放空间”页面
- 投屏到 Chromecast 和 Google TV，带遥控页面、迷你播放器和电视端字幕（仅限 Android）
- 桌面端：应用内置 libVLC 播放器、渐进式/HLS/DASH 下载、本地视频库文件夹，以及使用 whisper.cpp 生成字幕

### 小说世界
- 在 Android 和桌面端的内置阅读器中阅读已安装扩展的小说

### AnimeWorldTV 和宅文化管理者

AnimeWorldTV、宅文化管理者（Otaku Manager）和 `app` 同步管理器已在 `settings.gradle.kts` 中被注释掉，目前不会构建。
以下截图和说明仅供参考。

动漫世界电视：
- 从多个不同的视频网站上播放动漫、电视剧和电影
- 在内置视频播放器中观看
- 将收藏夹与AnimeWorld同步
- 包含跳过片头（直接跳过90秒）
- 仅适用于Android TV设备

宅文化管理者：
- 查看所有OtakuWorld应用程序中的所有收藏夹。需要您登录
- 使用纯粹的Jetpack Compose编写。没有任何视图的XML

#### 安装或更新 AnimeWorldTV 的指南
1. 下载 animeworldtv-release.apk

 如果使用 `adb`:

2.
```sh
adb install animeworldtv-release.apk
```

 如果不使用 `adb`:

2. 按照 [Android Authority][aa] 的步骤操作

# 本地构建
确保将构建变体更改为 `noFirebase` 变体（默认即是）。除此之外，在进行本地运行或构建时不需要做任何其他更改！

```bash
./gradlew :mangaworld:assembleNoFirebaseDebug
./gradlew :animeworld:assembleNoFirebaseDebug
./gradlew :novelworld:assembleNoFirebaseDebug
./gradlew test
```

构建使用 Kotlin 2.4.20、Gradle 9.7.1 和 Java 21。

# 桌面应用

三个应用都有 JVM/桌面 Compose 构建版本。打包版本可在
[nightly 版本](https://github.com/jakepurple13/OtakuWorld/releases/tag/nightly)和稳定版本中获取：macOS 为 ZIP，
Windows 为 NSIS 安装程序，Linux 为 AppImage。桌面构建目前未签名。应用启动时会检查更新；可在“设置 › 更新”中选择
Nightly 或 Stable 渠道。

## MangaWorld 桌面版

```bash
./gradlew :mangaworld:desktop:run
```

**可用：** 浏览、搜索、收藏、漫画列表/详情、内置阅读器阅读章节、下载（可在平台设置中配置下载目录）、自定义列表，
以及来自 Mihon 仓库的 Mihon 扩展（不支持 Cloudflare 保护的来源）。

**暂不支持：** Mihon 来源设置。

## AnimeWorld 桌面版

```bash
./gradlew :animeworld:desktop:run
```

**可用：** 浏览、搜索、收藏、剧集列表/详情、Aniyomi 扩展和 CloudStream 插件、应用内视频播放器（内置 libVLC，无需安装
VLC）及键盘快捷键（在播放器中按 `?`）、字幕、渐进式/HLS/DASH 流的剧集下载、在下载页面显示本地视频文件夹、“释放空间”页面，
以及使用 whisper.cpp 为已下载剧集生成字幕（设置 › 字幕）。

**暂不支持：** Chromecast/投屏、Aniyomi/CloudStream 扩展设置，以及需要 Android 资源或 WebView 的插件。

## NovelWorld 桌面版

```bash
./gradlew :novelworld:desktop:run
```

**可用：** 浏览、搜索、收藏、内置阅读器阅读章节，以及已安装的扩展。

**暂不支持：** 下载（NovelWorld 目前在所有平台上都不支持下载）。

# 问题
如果遇到任何问题，请使用以下详细信息创建一个问题请求


- 简短描述
- 所采取的步骤
- 设备
- Android版本
- 预期行为
- 实际行为
- 问题是否严重影响使用
- 其他可能有助于解决问题的额外信息
# 拉取请求

果你想为应用程序添加新的信息源或者有改进建议时应该采取的步骤

- 创建一个新的分支
- 进行修改
- 推送修改并创建新的PR（Pull Request）
- 将我（@jakepurple13）添加为审查者
- 这些步骤通常用于开发团队协作，帮助提升应用程序的功能和质量

[//]: # (参考链接)
  [aa]: <https://www.androidauthority.com/sideloading-apps-on-android-tv-1189896/>

- 本翻译由ChatGPT 3.5提供翻译支持,如果翻译错误，请(@kwxmxb) 进行更改或者变更翻译
