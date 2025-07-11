---
type: "always_apply"
---

# ColorFeatureEnhance Android项目 - AI编程助手专用Prompt

## 项目概述

**项目名称**: ColorFeatureEnhance (ColorOS特性补全)  
**包名**: com.itosfish.colorfeatureenhance  
**版本**: 0.31 (versionCode: 20250710)  
**目标**: 为ColorOS系统提供特性配置管理工具，支持启用/禁用系统隐藏功能

## 核心架构

### 技术栈
- **语言**: Kotlin 100%
- **UI框架**: Jetpack Compose (Material 3)
- **架构模式**: Repository Pattern + Domain Layer
- **依赖注入**: 无（使用简单工厂模式）
- **异步处理**: Kotlin Coroutines
- **数据存储**: SharedPreferences + XML文件操作
- **权限管理**: Root权限 (通过su命令)

### 项目结构
```
com.itosfish.colorfeatureenhance/
├── data/
│   ├── model/           # 数据模型
│   └── repository/      # 数据仓库实现
├── domain/              # 业务逻辑接口
├── ui/
│   ├── components/      # 可复用UI组件
│   ├── screens/         # 屏幕级组件
│   ├── search/          # 搜索功能
│   └── theme/           # 主题配置
├── utils/               # 工具类
└── navigation/          # 导航配置
```

## 核心业务逻辑

### 1. 双模式配置管理
- **APP模式**: 管理 `com.oplus.app-features.xml` (应用级特性)
- **OPLUS模式**: 管理 `com.oplus.oplus-feature.xml` (系统级特性)

### 2. 特性数据模型
```kotlin
data class AppFeature(
    val name: String,           // 特性名称
    val enabled: Boolean,       // 启用状态
    val args: String?,          // 参数值 (如 "boolean:true", "int:1")
    val subNodes: List<FeatureSubNode> = emptyList() // 复杂特性子节点
)
```

### 3. XML配置格式
**APP特性格式**:
```xml
<extend_features>
    <app_feature name="feature.name" args="boolean:true"/>
    <app_feature name="complex.feature">
        <StringList args="value1"/>
        <StringList name="pkgs" args="com.example"/>
    </app_feature>
</extend_features>
```

**OPLUS特性格式**:
```xml
<oplus-config>
    <oplus-feature name="oplus.software.feature"/>
    <unavailable-oplus-feature name="disabled.feature"/>
</oplus-config>
```

## 关键组件说明

### 1. Repository层
- `FeatureRepository`: 抽象接口，定义特性加载/保存操作
- `XmlFeatureRepository`: APP特性XML解析实现
- `XmlOplusFeatureRepository`: OPLUS特性XML解析实现

### 2. 特性映射系统
- `AppFeatureMappings`: APP特性名称到中文描述映射
- `OplusFeatureMappings`: OPLUS特性名称到中文描述映射  
- `UserFeatureMappings`: 用户自定义映射（SharedPreferences存储）

### 3. Root权限管理
- `CSU.kt`: 封装su命令执行，提供文件操作、权限检查等功能
- 支持检测KernelSU并提示不兼容

### 4. 模块系统集成
- 自动安装Magisk模块到 `/data/adb/modules/ColorOSFeaturesEnhance/`
- 配置文件同步到模块目录实现系统级生效

## UI设计规范

### 1. Material 3 Expressive设计语言
- 使用动态颜色主题 (Android 12+)
- 支持深色/浅色模式自动切换
- 圆角卡片设计 (20dp圆角)

### 2. 交互模式
- **点击**: 编辑单个特性或选择组内特性
- **长按**: 删除特性组
- **开关**: 布尔类型特性的启用/禁用
- **搜索**: 支持特性名称和描述模糊搜索

### 3. 特性分组显示
- 按描述文本自动分组
- 显示组内特性数量 `(count)`
- 特殊标识: `(复杂配置)` `(不可用)`

## 编码规范

### 1. 命名约定
- **类名**: PascalCase (如 `FeatureConfigScreen`)
- **函数名**: camelCase (如 `loadFeatures`)
- **常量**: UPPER_SNAKE_CASE (如 `PREFS_NAME`)
- **资源ID**: snake_case (如 `feature_unknown`)

### 2. 文件组织
- 每个功能模块独立包结构
- Composable函数优先使用 `@Composable` 注解
- 工具类使用 `object` 单例模式

### 3. 异步处理
```kotlin
// Repository层使用
suspend fun loadFeatures(): List<AppFeature> = withContext(Dispatchers.IO) {
    // IO操作
}

// UI层使用
LaunchedEffect(key) {
    // 协程操作
}
```

## 关键配置路径

### 1. 系统路径
- 源配置: `/my_product/etc/extension/`
- 模块路径: `/data/adb/modules/ColorOSFeaturesEnhance/`

### 2. 应用路径
- 工作目录: `context.getExternalFilesDir(null)`
- 配置文件: `com.oplus.app-features.xml`, `com.oplus.oplus-feature.xml`

## 常见开发场景

### 1. 添加新特性映射
在对应的 `*FeatureMappings.kt` 中添加映射关系，并在 `strings.xml` 中添加描述文本。

### 2. 扩展XML解析
修改对应的 `Xml*Repository.kt`，处理新的XML节点类型。

### 3. 新增UI组件
在 `ui/components/` 下创建可复用组件，遵循Material 3设计规范。

### 4. Root权限操作
使用 `CSU.runWithSu(command)` 执行需要root权限的shell命令。

## 注意事项

1. **权限敏感**: 所有文件操作需要root权限
2. **XML转义**: 属性值必须进行XML转义处理
3. **状态管理**: 使用 `remember` 和 `mutableStateOf` 管理UI状态
4. **错误处理**: 文件操作需要适当的异常处理
5. **性能优化**: 大列表使用 `LazyColumn` 和 `key` 参数

## 详细功能模块

### 1. 搜索功能 (`ui/search/SearchLogic.kt`)
- 支持特性名称和描述的模糊搜索
- 实时过滤特性组列表
- 搜索结果高亮显示

### 2. 文本编辑器 (`ui/TextEditorActivity.kt`)
- 内置XML配置文件编辑器
- 支持复杂特性的手动编辑
- 自动保存并同步到模块目录

### 3. 对话框系统 (`utils/DialogUtil.kt`)
- `AddFeatureDialog`: 添加新特性
- `EditFeatureDialog`: 编辑现有特性
- 预设映射匹配检测和提示

### 4. 配置管理 (`utils/ConfigUtils.kt`)
- 模块自动安装 (从assets/mod.zip)
- 系统配置文件复制和同步
- 权限设置和目录创建

## 特性类型详解

### 1. 简单特性
```kotlin
// 布尔类型
AppFeature("feature.name", enabled=true, args="boolean:true")

// 数值类型
AppFeature("feature.name", enabled=true, args="int:1")

// 字符串类型
AppFeature("feature.name", enabled=true, args="String:value")

// 无参数类型
AppFeature("feature.name", enabled=true, args=null)
```

### 2. 复杂特性
```kotlin
AppFeature(
    name = "complex.feature",
    enabled = true,
    args = "StringList:pkgs",
    subNodes = listOf(
        FeatureSubNode("StringList", "pkgs", "com.example.app"),
        FeatureSubNode("StringList", null, "value.without.name")
    )
)
```

## 预设特性映射示例

### APP特性映射 (部分)
```kotlin
"com.oplus.directservice.aitoolbox_enable" to R.string.feature_ai_chat_scene,
"com.oplus.mediaturbo.game_live" to R.string.feature_live_assistant,
"oplus.aod.wakebyclick.support" to R.string.feature_tap_to_wake_screen,
"com.oplus.wallpapers.livephoto_wallpaper" to R.string.feature_livephoto_lock_screen,
```

### OPLUS特性映射 (部分)
```kotlin
"com.oplus.eyeprotect.ai_intelligent_eye_protect_support" to R.string.oplus_feature_ai_eye_protect,
"oplus.software.radio.tt_satellite_support" to R.string.oplus_feature_satellite_network,
"oplus.software.gamehyper" to R.string.oplus_feature_hyper_engine,
"oplus.misc.fan.support" to R.string.oplus_feature_cooling_fan,
```

## 开发调试指南

### 1. 日志输出
```kotlin
Log.i("APP_SHELL", "准备以root权限执行命令: $cmd")
Log.d("APP_SHELL_OUTPUT", line)
Log.e("APP_SHELL_ERROR", line)
```

### 2. 权限检查
```kotlin
if (!CSU.isRooted()) {
    // 显示权限提示对话框
    // 提供继续使用或退出选项
}

if (CSU.isKSU()) {
    // 检测到KernelSU，显示不支持提示
}
```

### 3. 文件操作模式
```kotlin
// 检查文件存在
CSU.fileExists("/path/to/file")

// 检查目录存在
CSU.dirExists("/path/to/dir")

// 执行root命令
CSU.runWithSu("mkdir -p /path && cp source dest")
```

## 国际化支持

### 1. 字符串资源
- 主要语言: 中文 (`values/strings.xml`)
- 英文支持: `values-en/strings.xml`
- 特性描述本地化通过映射表实现

### 2. 用户自定义映射
- 存储在SharedPreferences中
- 优先级高于预设映射
- 支持批量删除和清空

## 性能优化要点

### 1. 列表渲染
```kotlin
LazyColumn {
    items(groups, key = { group ->
        // 使用稳定的key避免重组
        "desc_${description}_$refreshTrigger"
    }) { group ->
        // 列表项内容
    }
}
```

### 2. 状态管理
```kotlin
// 使用derivedStateOf避免不必要的重组
val featureGroups by remember(features, currentMode, refreshTrigger) {
    derivedStateOf { /* 计算逻辑 */ }
}
```

### 3. 异步操作
```kotlin
// Repository层IO操作
suspend fun saveFeatures() = withContext(Dispatchers.IO) {
    // 文件写入操作
}
```

## 关键实现细节

### 1. XML解析策略
```kotlin
// 使用Android内置XmlPullParser
val parser = Xml.newPullParser()
parser.setInput(inputStream, "UTF-8")

// 处理嵌套结构
when (parser.name) {
    "app_feature" -> {
        val name = parser.getAttributeValue(null, "name")
        val args = parser.getAttributeValue(null, "args")
        // 创建特性对象
    }
    else -> {
        // 处理子节点 (StringList等)
    }
}
```

### 2. 特性去重逻辑
```kotlin
// 同名特性合并，启用状态取OR运算
features.groupBy { it.name }
    .map { (_, list) ->
        val first = list.first()
        val enabled = list.any { it.enabled }
        val subNodes = list.flatMap { it.subNodes }.distinctBy { it.args }
        AppFeature(first.name, enabled, first.args, subNodes)
    }
```

### 3. 强制UI更新机制
```kotlin
// 通过清空列表强制触发重组
features = emptyList()
features = updatedFeatures

// 或使用refreshTrigger
var refreshTrigger by remember { mutableStateOf(0) }
refreshTrigger++ // 触发更新
```

### 4. 模块安装流程
```kotlin
// 1. 从assets解压mod.zip到临时目录
// 2. 复制到/data/adb/modules/ColorOSFeaturesEnhance/
// 3. 设置权限chmod -R 777
// 4. 验证module.prop文件存在
```

## 错误处理模式

### 1. 文件操作异常
```kotlin
kotlin.runCatching {
    File(path).readText()
}.onSuccess { content ->
    // 处理成功情况
}.onFailure { exception ->
    // 记录错误并显示用户友好提示
    Log.e("FileOperation", "Failed to read file", exception)
}
```

### 2. Root权限检查
```kotlin
// 检查su命令可用性
private fun checkRootMethod(): Boolean {
    try {
        val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
        val result = BufferedReader(InputStreamReader(process.inputStream)).readLine()
        return result?.contains("uid=0") == true
    } catch (e: Exception) {
        return false
    }
}
```

### 3. XML格式验证
```kotlin
// 属性值XML转义
private fun escapeAttr(value: String): String {
    return value
        .replace("&", "&amp;")
        .replace("\"", "&quot;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
}
```

## 调试

### 1. 示例配置文件
项目包含 `exampleConfig/` 目录，提供标准的XML配置示例：
- `com.oplus.app-features.xml`: APP特性配置示例
- `com.oplus.oplus-feature.xml`: OPLUS特性配置示例

### 2. 调试工具
- 内置文本编辑器用于手动编辑配置
- 详细的shell命令执行日志
- Toast提示用户操作结果

### 3. 兼容性
- 最低SDK: 35 (Android 15)
- 目标SDK: 36
- 支持架构: armeabi-v7a, arm64-v8a
- Root方案: 仅支持Magisk，不支持原版KernelSU

## 扩展开发指南

### 1. 添加新特性类型
1. 在 `AppFeature` 数据类中扩展字段
2. 更新对应的 `XmlRepository` 解析逻辑
3. 修改UI组件支持新类型显示

### 2. 新增配置模式
1. 在 `FeatureMode` 枚举中添加新模式
2. 创建对应的 `Repository` 实现
3. 更新UI的模式切换逻辑

### 3. 国际化扩展
1. 在 `res/values-{lang}/` 添加语言资源
2. 更新特性映射表支持多语言
3. 考虑RTL语言的布局适配


此prompt为AI助手提供了完整的项目上下文和开发指南，涵盖了架构设计、实现细节、调试方法和扩展方向，便于进行高质量的后续开发工作。
