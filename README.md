# Netflix & Infuse Themes for Jellyfin Android TV

> Fork of [jellyfin/jellyfin-androidtv](https://github.com/jellyfin/jellyfin-androidtv) with **two new dark color themes**.

## 🎨 Themes

| Theme | 主题色 | 背景 | 名称 |
|---|---|---|---|
| **Netflix** | `#E50914` (网飞招牌红) | `#141414` (纯黑) | Netflix (black & red) |
| **Infuse** | `#FF6B1A` (Apple TV 暖橙) | `#1A1A1E` (深黑) | Infuse (orange) |

切换主题：APP 设置 → 外观 → 主题。在 `Default / Emerald / Muted purple` 之外新增两个选项。

## 📦 What's Changed

### Files Added (2)
- `app/src/main/res/values/theme_netflix.xml` — Netflix 黑红主题
- `app/src/main/res/values/theme_infuse.xml` — Infuse 橙主题

### Files Modified (5)
- `AppTheme.kt` — `enum class AppTheme` 增加 `NETFLIX`, `INFUSE`
- `ActivityThemeExtensions.kt` — `AppTheme.style` 映射加两条
- `strings.xml` × 3 语言（英文 / 简体 / 繁体）— 主题名

## 🛠️ 编译 (Build)

需要 JDK 17 + Android SDK 34+：

```bash
git clone https://github.com/bzl1982/jellyfin-androidtv.git
cd jellyfin-androidtv
./gradlew assembleTv  # 输出 app/build/outputs/apk/tv/release/*.apk
```

## 📥 安装到小米电视

```bash
adb connect 192.168.50.x   # 小米电视 IP
adb install app/build/outputs/apk/tv/release/app-tv-release.apk
```

## 🔮 路线图

- [x] P1 — 主题色（V1，含此 PR）
- [ ] P2 — 首页 hero + 横滑行（网飞首页感）
- [ ] P2 — 详情页 + 搜索 + 播放器 OSD
- [ ] P3 — 编译 APK 发布到 Releases

## 📜 License

GPL-2.0 (same as upstream jellyfin-androidtv)
