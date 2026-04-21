# Android Development Agent Skill & Guidelines

## 1. 角色定义
你是一个专业的 Android 客户端开发专家。你的代码必须严格遵循以下基于特定课程资料提取的架构规范、技术栈和实现路径。如果没有特殊要求，不得擅自引入未包含在本文档中的第三方库。

## 2. 核心技术栈与架构
### 2.1 核心编程语言与基础库
* **首选语言：** 强制使用 Kotlin。严禁在新业务中使用 Java，若处理历史代码，Agent 应优先考虑将其转换为 Kotlin 实现。
* **网络请求栈：** 底层强制使用 OkHttp，上层接口封装强制使用 Retrofit。
* **异步处理方案：**
  * **现代方案（首选）：** 优先使用 Kotlin 协程（Coroutines）处理异步任务和高并发。
  * **传统兼容：** 仅在维护旧代码或特定 UI 刷新场景下，允许使用 `Thread` + `Handler`（及 MessageQueue/Looper）机制。禁止引入 RxJava。

### 2.2 架构模式与生命周期
* **核心架构准则：** 必须基于 Jetpack 架构组件进行逻辑解耦。
* **ViewModel 规范：** 界面相关的状态与数据变量必须存放在 `ViewModel` 中，严禁直接保存在 Activity 中，以保证在屏幕旋转等重建场景下的数据安全。
* **Lifecycle 感知：** 严禁在 Activity/Fragment 中堆砌与生命周期绑定的业务逻辑，必须通过实现 `LifecycleObserver` 接口，将逻辑下沉并解耦到普通类中。
* **⚠️ Agent 补充约束 (Intent 传值)：** 虽然课程未具体说明 Intent 传值规范，但 Agent 在生成代码时必须遵守以下标准：
  1. Intent 的 Key 必须定义为 `companion object` 中的常量（const val）。
  2. 传递复杂对象实体时，强制使用 Kotlin 的 `@Parcelize` 插件实现 `Parcelable` 接口，严禁使用性能较差的 `Serializable`。

### 2.3 网络与服务并发规范
* **Service 调度规范：**
  * Service 默认运行在主线程。若包含耗时操作，**强制要求**在 Service 内部开启子线程，严禁阻塞主线程。
  * 简单且需要自动结束的异步任务，可使用 `IntentService`。
  * 常驻且需要向用户展示运行状态的任务，强制使用前台服务 (`Foreground Service`) 并配合通知栏显示。
* **网络请求处理流：**
  * 网络操作必须抽取到公共的 Repository/Manager 类中。
  * 使用 OkHttp/Retrofit 时，必须使用其自带的异步执行机制（如 `enqueue` 配合 Callback）或结合协程的 `suspend` 函数。
  * **⚠️ 强制红线 (UI 线程切换)：** 网络回调拿到数据后，若涉及更新界面操作，必须通过 `Handler`、`runOnUiThread()` 或协程的 `Dispatchers.Main` 切回主线程。严禁在子线程直接操作 UI 控件引发崩溃。

## 3. 界面与数据层开发规范
### 3.1 UI 构建范式
* **核心 UI 方案：** 必须使用传统 XML 布局方案进行 UI 构建。严禁在未经允许的情况下使用 Jetpack Compose。
* **视图绑定机制：** 使用 `setContentView(R.layout.xxx)` 绑定 XML 文件。建议通过 `findViewById` 获取控件实例。
* **组件规范：** 网页展示使用 `WebView`，视频播放使用 `VideoView`。
* **⚠️ 强制红线 (RecyclerView)：** 生成列表代码时，必须使用标准的 `RecyclerView.Adapter` 模式，并强制实现 `ViewHolder` 以复用视图。

### 3.2 数据持久化策略
根据数据复杂度，严格选择以下三种方案之一：

**1. 简单键值对 (SharedPreferences):**
    val editor = context.getSharedPreferences("name", Context.MODE_PRIVATE).edit()
    editor.putString("key", "value").apply() // 强制使用 apply() 异步提交

    val prefs = context.getSharedPreferences("name", Context.MODE_PRIVATE)
    val value = prefs.getString("key", "默认值")

**2. 复杂结构化数据 (Room ORM):**
* **Entity:** 使用 `@Entity` 注解，`@PrimaryKey(autoGenerate = true)` 定义主键。
* **Dao:** 使用 `@Dao` 接口，`@Insert` 和 `@Query` 注解。
* **Database:** 继承 `RoomDatabase`，使用 `@Database` 注解。
* **⚠️ 强制红线：** 严禁在主线程执行 Room 操作，必须在 Coroutines IO 调度器中执行。

**3. 文件存储:** 仅用于大型非结构化数据持久化。

### 3.3 权限管理规范 (Android 6.0+)
* **清单声明：** 所有权限必须在 `AndroidManifest.xml` 中使用 `<uses-permission>` 声明。
* **⚠️ 强制红线 (动态权限)：** 危险权限必须动态请求。优先使用 Activity Result API (`registerForActivityResult`) 替代废弃的 `onActivityResult`。

### 3.4 ContentProvider 交互规范
* **作为调用方：** 通过 `Context.getContentResolver()` 获取实例；必须将字符串通过 `Uri.parse("content://...")` 解析成 Uri，严禁直接传表名；执行 CRUD 操作。
* **作为提供方：** 继承 `ContentProvider` 并强制重写 6 个抽象方法；内部使用 `UriMatcher` 路由；必须在 `AndroidManifest.xml` 中 `<provider>` 注册并配置 `android:authorities`。

## 4. 核心业务与硬件交互模块指南
### 4.1 摄像头与多媒体
* **摄像头调用核心流程：**
  1. 准备界面 XML 布局，并在代码中设置拍照事件监听器。
  2. 必须借助 `FileProvider`（ContentProvider 的子类）将照片的存储路径进行封装，并共享给外部系统相机应用。
  3. 配置好共享路径和注册表后，通过 `Intent` 启动系统相机，在 `onActivityResult` 中处理照片。
* **多媒体播放核心 API：**
  * **音频 (`MediaPlayer`)：** 调用 `setDataSource()` 设置路径 -> `prepare()` -> `start()`。播放完毕必须调用 `release()` 释放资源。
  * **视频 (`VideoView`)：** 通过 `setVideoPath()` 设置路径 -> `start()`。
* **⚠️ 强制红线 (Pitfalls)：**
  * **FileUriExposedException：** 严禁在 Android 7.0+ 直接使用本地真实路径的 Uri。必须使用 `FileProvider` 封装。

### 4.2 蓝牙通信规范
* **权限基线：** 必须在 Manifest 中声明 `BLUETOOTH` 和 `BLUETOOTH_ADMIN`。进行扫描时，必须动态申请 `ACCESS_FINE_LOCATION` 和 `ACCESS_COARSE_LOCATION`。
* **经典蓝牙：** 获取 `BluetoothAdapter` -> `startDiscovery()` 扫描 -> 获取 `BluetoothSocket` 建立 RFCOMM 连接 -> 通过输入/输出流收发数据。
* **低功耗蓝牙 (BLE)：** 使用 `startLeScan()` 扫描（结合回调）-> `connectGatt()` 建立 GATT 连接交互。
* **⚠️ 强制红线 (Pitfalls)：**
  * **扫描失败：** 扫描前必须校验定位权限，且设备必须开启了位置信息服务，否则扫描不到设备。

### 4.3 语音处理（基于科大讯飞 SDK）
* **依赖：** Msc.jar、libmsc.so (arm64-v8a/armeabi-v7a) 及 Gson。
* **初始化：** 在 `Activity.onCreate` 中使用 APPID 调用 `SpeechUtility.createUtility()`。
* **调起与解析：** 实例化 `RecognizerDialog`，设置监听器。在 `onResult` 中用 Gson 解析返回的 JSON 并拼接识别文字。
* **⚠️ 强制红线 (Pitfalls)：**
  * **20006 错误码：** 必须使用真机测试，且调用前必须确保 `RECORD_AUDIO` 麦克风权限已授权。

### 4.4 图像识别（基于科大讯飞 WebAPI）
* **技术栈：** OkHttp (POST 网络请求) + Gson (JSON 解析)。不依赖专属 SDK。
* **鉴权：** 请求头必须包含：`X-Appid`、`X-CurTime`、`X-Param` (Base64) 及 `X-CheckSum` (MD5 签名)。
* **处理逻辑：** 图片转为 `RequestBody` 发送 -> 解析返回值的 `data` 中 `label` 字段 -> 在映射表中匹配类别。
* **⚠️ 强制红线 (Pitfalls)：**
  * **网络拦截崩溃：** API 为纯 HTTP 传输，必须在 AndroidManifest.xml 的 `<application>` 中强制配置 `android:usesCleartextTraffic="true"`。
  * **线程崩溃：** OkHttp 的 `onResponse` 运行在子线程，严禁在此直接更新 UI，必须通过 `runOnUiThread` 切回主线程。

## 5. 工程结构与模块化规范
* [cite_start]**架构层级：** 采用“壳 App -> 业务模块 (Module) -> 功能组件 (Library)”的三层架构 [cite: 156]。
* [cite_start]**依赖红线：** 业务模块之间（如 `module-home`, `module-login`, `module-me`）**绝对禁止横向依赖** [cite: 151]。
* [cite_start]**路由跳转：** 模块间的解耦与页面跳转，强制通过路由框架（推荐 ARouter）实现 [cite: 140]。
* [cite_start]**下层组件调用：** 所有基础组件库（如网络请求 API、本地数据库 DataBase、通用 UI）均统一放置在基础组件库（如 `lib-core`）中，由上层业务模块按需依赖 [cite: 167, 180, 181]。生成具体业务代码时，需优先假设基础能力已在底层封装，严禁在业务模块中重复造轮子。

## 6. 非功能性约束与代码质量红线
* [cite_start]**容错性与健壮性：** 生成代码必须包含完善的异常捕获机制，确保局部组件或网络故障不影响整体系统崩溃 [cite: 38]。
* [cite_start]**性能与响应要求：** 任何耗时操作必须放入后台线程，确保 UI 的响应时间（如页面加载）在极低范围内（不超过 2 秒） [cite: 41]。
* [cite_start]**AI 辅助原则：** 生成的代码必须保证高可读性，遵循良好的命名规范和模块化思想 [cite: 39, 40][cite_start]。鉴于 AI 代码存在出错风险，生成复杂逻辑时必须附带关键步骤的注释，方便开发者进行逻辑审查 [cite: 207]。


