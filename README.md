# AI 自习室

一个基于 Android 的端侧学习状态监测项目，聚焦自习场景下的人体姿态、专注状态与学习记录展示。

当前版本使用 ML Kit 在本地完成视觉分析，不依赖云端图像上传。

## 功能概览

- 学习过程中的姿态与专注状态分析
- 学习报告与历史记录查看
- 本地 OCR 学习辅助能力
- 深色 / 浅色主题切换
- 端侧推理，默认不上传用户图像

## 技术栈

- Kotlin
- Android View XML
- Jetpack ViewModel / Lifecycle / Room
- CameraX
- ML Kit Pose Detection / Face Detection / Text Recognition
- Retrofit + OkHttp

## 项目结构

详细目录说明见 `docs/project-structure.md`。

核心路径：
- `app/src/main/java/com/example/end_side/`: 主业务代码
- `app/src/main/res/`: 界面资源与主题配置
- `app/src/main/assets/`: 运行时静态资源
- `docs/`: 对外可公开的补充文档

## 运行要求

- Android Studio 最新稳定版
- Android SDK 36
- JDK 11
- 最低 Android 版本：API 29

## 本地运行

1. 使用 Android Studio 打开项目根目录。
2. 确认 `local.properties` 仅包含本机 `sdk.dir`。
3. 等待 Gradle Sync 完成。
4. 连接真机或启动模拟器。
5. 运行 `app` 模块。

## 隐私说明

- 项目默认采用端侧视觉分析。
- 开源版本不包含已弃用的 Paddle Lite 模型与相关历史实现。
- 提交代码前请不要将 `local.properties`、签名文件、测试隐私数据或任何个人身份信息加入版本库。

## Screenshots

截图占位说明见 `docs/screenshots/README.md`。

## 贡献

欢迎提交 Issue 和 Pull Request。贡献流程见 `CONTRIBUTING.md`。

## License

本项目采用 Apache License 2.0，详见 `LICENSE`。