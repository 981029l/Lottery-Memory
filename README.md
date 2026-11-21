# 彩记（Caiji）— 双色球 & 大乐透个人助手（Android）

纯本地、零服务器成本的安卓彩票个人工具，基于 **Kotlin + Jetpack Compose** 构建，采用 Apple 极简交互风格，所有图标均为矢量图标（零 Emoji）。

## 核心功能

- **官方开奖同步**：直连福彩（双色球）与体彩（大乐透）官方公开接口，自动解析每期开奖号码、奖池、销量、各奖级注数与**一等奖出奖省份分布**。
- **全玩法选号**：支持**机选（1/3/5 注）**、**单式**、**复式（自动组合算注）**、**胆拖（胆拖互斥 + 后胆）**、**1–99 倍倍投**以及**大乐透追加投注（单注 ¥3）**。
- **个人票夹 & 拆单自动核对**：记录线下已购票据，开奖后精确拆单计算单式/复式/胆拖各奖级中奖注数与固定奖金总和。
- **后台开奖通知**：通过 Android `WorkManager` 后台定时拉取开奖结果，发现新开奖且命中用户记录期号时，自动弹出系统通知提醒。
- **应用内更新 & 中国地区下载加速**：位于「我的票夹」顶部统计卡下方。GitHub Actions 打包时自动注入当前仓库地址，App 启动自动静默检测 GitHub Releases 新版本，支持多条国内加速节点自动切换与直连回退，应用内流式下载 APK 并拉起系统安装。

## 交互动效实现清单

1. **Card 3D 透视 + Spring 阻尼回正**：`Modifier.tilt3D`（`graphicsLayer { rotationX; rotationY }` + `Animatable` + 随指镜面高光）
2. **旋转渐变扫光描边 + 底部呼吸弥散背光**：`HeroDrawCard`（`Brush.sweepGradient` + `InfiniteTransition` + `blur(28.dp)`）
3. **胶囊 → 选号面板 Fluid Morph**：`MorphPickerPanel`（高阻尼弹簧 `dampingRatio = 0.90f, stiffness = 260f`）
4. **Card → 详情页共享元素转场**：`SharedTransitionLayout` + `sharedBounds` + 背景连续平滑缩放
5. **橡皮筋阻尼 + 速度投射吸附底部抽屉**：`TicketSheet`（`rubberBand` 非线性阻尼 + `VelocityTracker` 投射吸附至收起/半屏/全屏）
6. **按钮按压 `scale(0.96f)` + 释放超调回弹**：`Modifier.springClickable`（`dampingRatio = 0.35f, stiffness = 420f`）

## 推送到 GitHub 自动打包 APK

本仓库已内置 GitHub Actions 工作流：[.github/workflows/android-build.yml](.github/workflows/android-build.yml)。

1. 在 GitHub 新建一个仓库（公开或私有均可）。
2. 在本目录执行推送：
   ```bash
   git init
   git add .
   git commit -m "feat: init caiji android app"
   git branch -M main
   git remote add origin https://github.com/<你的用户名>/<仓库名>.git
   git push -u origin main
   ```
3. 打开 GitHub 仓库的 **Actions** 页面，等待约 2–3 分钟构建完成后，点击本次运行底部的 **`caiji-debug-apk`** 即可下载 APK 直接装到安卓手机。
4. 如果想自动生成 **Releases** 下载页，打一个版本标签推送即可：
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```
