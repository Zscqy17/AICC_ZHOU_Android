# AICC_ZHOU_Android 架构说明

本文档描述 Android 端当前实现的核心架构，重点覆盖无障碍光标、视觉推理、输入法、自由拼音和配置同步五个部分。

## 1. 设计目标

项目的核心目标不是单一键盘应用，而是一套面向无障碍场景的交互系统：

- 用摄像头捕获头部运动和面部表情
- 把视觉信号转换成 Android 光标和系统动作
- 在输入阶段提供可被无障碍能力驱动的语音键盘和自由拼音键盘
- 让输入法可以跟随指针、自由悬浮或停靠在固定区域

因此，本项目同时包含普通 Activity、AccessibilityService、InputMethodService、MediaPipe 推理线程和 JSON 词典资产。

## 2. 总体分层

### 2.1 UI 与配置层

职责：

- 展示主入口和设置入口
- 请求权限
- 管理用户配置
- 把配置变化广播给运行中的服务和输入法

主要类：

- Android/app/src/main/java/com/google/projectgameface/MainActivity.java
- Android/app/src/main/java/com/google/projectgameface/CursorBinding.java
- Android/app/src/main/java/com/google/projectgameface/VoiceKeyboardSettingsActivity.java
- Android/app/src/main/java/com/google/projectgameface/ChooseGestureActivity.java
- Android/app/src/main/java/com/google/projectgameface/GestureSizeActivity.java
- Android/app/src/main/java/com/google/projectgameface/CursorSpeed.java

### 2.2 无障碍运行时层

职责：

- 驱动虚拟光标
- 处理点击、长按、拖拽、返回主页等动作
- 对外广播当前光标位置
- 作为视觉推理结果的主消费端

主要类：

- Android/app/src/main/java/com/google/projectgameface/CursorAccessibilityService.java
- Android/app/src/main/java/com/google/projectgameface/CursorController.java
- Android/app/src/main/java/com/google/projectgameface/DispatchEventHelper.java
- Android/app/src/main/java/com/google/projectgameface/ServiceUiManager.java

### 2.3 视觉推理层

职责：

- 接收 CameraX 帧
- 调用 MediaPipe Face Landmarker
- 输出头部位置、blendshape 系数和人脸可见性

主要类：

- Android/app/src/main/java/com/google/projectgameface/FaceLandmarkerHelper.java
- Android/app/src/main/java/com/google/projectgameface/CameraHelper.java
- Android/app/src/main/java/com/google/projectgameface/CameraBoxOverlay.java
- Android/app/src/main/java/com/google/projectgameface/FullScreenCanvas.java

### 2.4 输入法运行时层

职责：

- 提供语音输入法入口
- 渲染普通键盘和自由拼音键盘
- 应用交互模式、缩放、主题和面板位置
- 根据广播与光标位置联动

主要类：

- Android/app/src/main/java/com/google/projectgameface/VoiceAccessInputMethodService.java
- Android/app/src/main/java/com/google/projectgameface/VoiceKeyboardConfig.java
- Android/app/src/main/java/com/google/projectgameface/VoiceKeyboardSettingsActivity.java

### 2.5 自由拼音数据层

职责：

- 管理自由拼音键盘布局
- 管理音节输入状态
- 查询候选词和联想词
- 从参考词典生成 Android 可用资产

主要类与资源：

- Android/app/src/main/java/com/google/projectgameface/ZiyouInputConfig.java
- Android/app/src/main/java/com/google/projectgameface/ZiyouInputState.java
- Android/app/src/main/java/com/google/projectgameface/ZiyouCandidateRepository.java
- Android/tools/generate_ziyou_assets.js
- Android/app/src/main/assets/ziyou_candidates.json
- Android/app/src/main/assets/ziyou_reference_dict.json
- Android/app/src/main/assets/ziyou_associations.json

## 3. 核心运行链路

## 3.1 从摄像头到光标

执行顺序：

1. MainActivity 打开应用并唤起 CursorAccessibilityService。
2. CursorAccessibilityService 初始化 CameraX 和 FaceLandmarkerHelper。
3. FaceLandmarkerHelper 在后台线程处理图像帧并产出头部位置、blendshape。
4. CursorAccessibilityService 把推理结果交给 CursorController。
5. CursorController 根据速度、平滑和阈值配置更新光标。
6. DispatchEventHelper 在满足触发条件时执行点击、长按、系统键或其它事件。

关键点：

- 视觉推理与 UI 分线程执行，避免卡住无障碍服务主线程。
- 服务以广播方式对外公布自身状态和光标位置，降低与页面、输入法的耦合。

## 3.2 从面部手势到输入法命令

首次启动时，MainActivity 会写入默认手势绑定，其中包含 IME 控制动作，例如：

- 开始或停止语音输入
- 切换键盘模式
- 切换键盘缩放
- 将键盘移到当前指针附近

这些动作通过广播传给 VoiceAccessInputMethodService，后者根据命令执行对应 UI 或状态变化。

相关命令常量位于：

- Android/app/src/main/java/com/google/projectgameface/VoiceAccessInputMethodService.java

## 3.3 从光标到悬浮键盘位置

CursorAccessibilityService 会周期性发送当前光标位置广播。

VoiceAccessInputMethodService 收到广播后：

- 如果是 DOCKED 模式：保持面板在固定归一化位置
- 如果是 FREE 模式：用户可拖动，位置会持久化
- 如果是 FOLLOW_POINTER 模式：根据策略实时计算面板目标位置

支持的跟随策略：

- CENTER_ON_POINTER
- OFFSET_ABOVE_POINTER
- EDGE_DOCK

配置定义位于：

- Android/app/src/main/java/com/google/projectgameface/VoiceKeyboardConfig.java

## 3.4 从自由拼音输入到候选展示

执行顺序：

1. 用户在输入法中切换到自由拼音模式。
2. 点击声母键后，输入法弹出韵母选择层。
3. 用户选中韵母后，ZiyouInputState 追加一个音节到预览字符串。
4. VoiceAccessInputMethodService 调用 ZiyouCandidateRepository.getCandidates。
5. Repository 在平面候选表、裁剪版 quanpin 表中合并候选。
6. 候选展示到滚动条中，首项高亮。
7. 用户提交候选后，调用 getAssociations 查询联想词。
8. 输入法继续展示下一轮联想推荐。

当前候选策略是 Android 友好的简化版，优先保证内存和响应速度，不追求桌面端全量词典行为完全一致。

## 4. 配置存储与同步

项目大量使用 SharedPreferences 作为轻量配置中心。

### 4.1 输入法配置

VoiceKeyboardConfig 统一管理以下配置：

- 交互模式
- 缩放比例
- 跟随策略
- 主题
- 面板位置

这些配置被以下模块共同读取：

- VoiceKeyboardSettingsActivity
- VoiceAccessInputMethodService

配置变更后，设置页会发送刷新广播，让运行中的输入法即时同步。

### 4.2 光标与手势配置

无障碍服务会监听配置广播并按需更新：

- 光标速度
- 平滑参数
- 手势阈值
- 动作绑定关系

这使设置页不需要直接持有服务实例，降低生命周期耦合。

## 5. 词典资产策略

参考仓库中的词典原始体积较大，不能直接无脑装入 Android 输入法进程，因此当前采用“离线裁剪 + 运行时只读加载”的策略。

### 5.1 输入文件

- dict.json
- association.json

### 5.2 生成脚本

- Android/tools/generate_ziyou_assets.js

脚本职责：

- 过滤非法 key
- 去重候选项
- 截断单 key 候选数量
- 裁剪整体 key 数量
- 输出适合 Android assets 读取的结构

### 5.3 输出文件

- ziyou_reference_dict.json
- ziyou_associations.json

### 5.4 当前权衡

- 优点：内存可控、加载快、构建简单
- 代价：不是参考词库全量等价实现

## 6. 交互模式设计

### 6.1 DOCKED

适合稳定输入场景，键盘固定停靠在面板区域。

### 6.2 FREE

适合需要用户手动摆放键盘位置的场景。支持：

- 拖拽面板
- 位置持久化
- 一键移动到指针附近

### 6.3 FOLLOW_POINTER

适合强依赖光标上下文的输入场景。键盘可根据指针位置自动跟随，并通过策略调整具体布局。

## 7. 关键类职责表

| 类 / 文件 | 职责 |
| --- | --- |
| MainActivity | 应用入口、权限和设置导航 |
| CursorAccessibilityService | 无障碍运行时主服务 |
| CursorController | 光标位置与移动控制 |
| DispatchEventHelper | Accessibility 事件派发 |
| FaceLandmarkerHelper | MediaPipe 实时检测线程 |
| VoiceAccessInputMethodService | 语音键盘与自由拼音键盘核心 |
| VoiceKeyboardConfig | 输入法配置读写中心 |
| VoiceKeyboardSettingsActivity | 输入法设置页 |
| ZiyouInputConfig | 自由拼音布局和声母韵母映射 |
| ZiyouInputState | 拼音输入状态机 |
| ZiyouCandidateRepository | 候选词与联想词查询 |
| generate_ziyou_assets.js | 词典裁剪和资产生成 |

## 8. 当前演进方向

当前架构已经适合继续往以下方向演进：

- 更复杂的悬浮体验
  - 平滑跟随动画
  - 边缘吸附
  - 拖拽释放反馈
- 更完整的拼音候选策略
  - 更强的排序规则
  - 更接近参考词库的联想行为
  - 用户词频或上下文学习
- 更规范的国际化资源
  - values 保留英文
  - values-zh 提供中文

## 9. 构建说明

已验证的构建环境：

- JDK 17
- Android Gradle Plugin 8.13.2
- Gradle 8.13

构建命令：

```powershell
$env:JAVA_HOME='C:\Users\admin\.jdks\jbr-17.0.14'
Set-Location 'd:\Songchen\AICC_ZHOU_Android\Android'
.\gradlew.bat assembleDebug
```