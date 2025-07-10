# ColorOS特性补全可视化编辑器

一个用于可视化编辑 ColorOS 特性开关（如 `com.oplus.app-features.xml` 和 `com.oplus.oplus-feature.xml`）的工具，使用 Kotlin + Jetpack Compose 开发，并遵循 Material 3 设计规范。

## 主要功能

- **双模式编辑**：支持 `app-features` 和 `oplus-features` 两种配置模式，可在界面上轻松切换。
- **动态加载**：自动从应用私有目录加载并解析 XML 配置文件。
- **可视化编辑**：以列表形式清晰地展示所有特性及其启用状态，相同描述的特性会自动分组。
- **实时修改**：通过开关即可修改特性状态，并立即写回 XML 文件。
- **添加/删除特性**：通过悬浮按钮（FAB）可以添加新特性；长按特性卡片即可删除。
- **搜索功能**：提供悬浮按钮和顶部搜索栏，支持按名称和描述快速查找特性，并高亮显示匹配项。
- **自定义描述**：用户添加的特性描述会被优先使用并持久化保存，实现了灵活的“名称-描述”映射。
- **国际化**：内置中、英文双语支持。

## 使用说明

1.  将你的特性配置文件（`com.oplus.app-features.xml` 或 `com.oplus.oplus-feature.xml`）放置到以下路径：
    `/sdcard/Android/data/com.itosfish.colorfeatureenhance/files/`
2.  启动应用，通过顶部的切换按钮选择对应的模式即可进行查看和编辑。

## 代码结构

```
com.itosfish.colorfeatureenhance
├── data
│   ├── model/                  # 数据模型 (AppFeature, App/Oplus-FeatureMappings, UserFeatureMappings)
│   └── repository/             # 数据仓库实现 (XmlFeatureRepository, XmlOplusFeatureRepository)
├── domain
│   └── FeatureRepository.kt    # 业务逻辑接口，定义了数据操作的契约
├── navigation
│   └── Navigation.kt           # 应用导航逻辑
├── ui
│   ├── components/             # 可复用UI组件 (TopAppBar, SearchBar, HighlightedText, FABs)
│   ├── search/                 # 搜索相关逻辑 (SearchLogic)
│   └── FeatureConfigScreen.kt  # 核心UI界面，负责展示、分组和操作特性列表
├── utils
│   └── DialogUtil.kt           # 对话框工具类 (关于、添加、编辑、删除确认)
├── FeatureMode.kt              # APP/OPLUS 模式枚举
└── MainActivity.kt             # 应用主入口
```

## 开发规范

### 1. 代码架构

项目采用 Clean Architecture 的变体，将代码分为三层：

-   **UI层 (`ui/`)**：负责界面渲染和用户交互，不包含业务逻辑。
-   **领域层 (`domain/`)**：定义核心业务规则和接口 (`FeatureRepository`)。
-   **数据层 (`data/`)**：负责数据的获取、解析和持久化 (`Xml...Repository`, `...Mappings`)。

### 2. UI规范

-   **界面风格**: 遵循 Material 3 设计语言，使用 `Card`, `FloatingActionButton`, `AlertDialog`, `SearchBar` 等组件。
-   **组件化**: 可复用组件存放于 `ui/components/`，完整页面存放于 `ui/screens/` (当前为`FeatureConfigScreen.kt`)。
-   **预览**: 所有重要 Composable 函数都应提供 `@Preview`。

### 3. 数据处理

-   **XML处理**: `Xml...Repository` 负责对应模式下 XML 文件的读写操作。
-   **用户映射**: `UserFeatureMappings` 使用 `SharedPreferences` 持久化用户自定义的描述，支持两种模式。
-   **异步操作**: 所有 I/O 操作均在 `Dispatchers.IO` 协程中执行，避免阻塞主线程。

### 4. 命名与约定

-   **命名**: 遵循标准的 Kotlin 命名规范（驼峰式、帕斯卡式等）。
-   **代码注释**: 对复杂逻辑和公共API提供清晰的中文 KDoc 注释。
-   **Dialog**: 对话框逻辑统一放在 `utils/DialogUtil.kt` 中，便于管理和复用。
