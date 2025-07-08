# ColorOS特性补全可视化编辑器

本项目为ColorOS特性补全可视化编辑器，使用Kotlin + Jetpack Compose开发，采用Material 3 Expressive设计风格。

## 项目代码结构

```
com.itosfish.colorfeatureenhance/
├── MainActivity.kt              # 应用入口，负责加载主界面
├── ui/                          # UI相关代码
│   ├── theme/                   # 主题相关
│   │   ├── Color.kt             # 颜色定义
│   │   ├── Theme.kt             # 主题定义
│   │   └── Type.kt              # 文字样式
│   ├── components/              # 可复用UI组件
│   │   ├── TopAppBarComponent.kt # 顶部应用栏组件
│   │   └── AboutDialogUtil.kt   # 对话框工具
│   └── FeatureConfigScreen.kt   # 特性配置页面
├── data/                        # 数据层代码（模型、仓库等）
├── domain/                      # 领域层（业务逻辑）
└── utils/                       # 工具类
```

## 开发规范

### 1. 代码架构

项目采用UI和业务逻辑分离的设计思路，遵循以下原则：

- **UI层**：负责界面渲染，不包含业务逻辑
- **业务逻辑层**：处理应用功能实现，不依赖于具体UI实现
- **数据层**：负责数据获取、持久化等操作

### 2. UI组件规范

#### UI组件目录结构
- `ui/components/`：存放可复用的UI组件
- `ui/screens/`：存放完整页面
- `ui/theme/`：存放主题相关定义

#### Compose编写规则
- 使用`@Composable`注解标记所有可组合函数
- 参数应包含`modifier: Modifier = Modifier`以支持灵活布局
- 预览函数使用`@Preview`注解，便于开发时查看
- 组件应支持通过参数自定义，而非硬编码

### 3. 业务逻辑规范

- 业务逻辑代码放在`domain`包下
- 按功能模块组织代码结构
- 使用接口定义功能，实现与调用分离
- 异步操作使用协程处理

### 4. 数据处理规范

- 数据模型放在`data/model`下
- 数据操作相关代码（如仓库）放在`data/repository`下
- 遵循单一数据源原则
- 数据变更通过Flow或LiveData通知UI层

### 5. 命名规范

- 变量命名：驼峰命名法，如`autoSetRules`
- 类名：首字母大写的驼峰命名法，如`AutoSetRules`
- 常量：全大写，下划线分隔，如`MAX_RETRY_COUNT`
- 函数命名：动词开头，描述其行为，如`getConfig()`、`saveSettings()`

### 6. 其他开发约定

- 新增功能时，确保其他功能、引用不受影响
- 代码应有适当注释，尤其是复杂逻辑
- 修改公共组件前，应评估对使用该组件的地方的影响
- 使用Dialog时，使用原生MaterialAlertDialog
- 界面风格遵循Material 3 Expressive设计风格

## 特别说明

本应用需要Root权限才能运行完整功能。

## 贡献流程

在新增功能或修改代码前，请确保：

1. 了解现有代码结构和开发规范
2. 遵循既定的代码风格和架构设计
3. 充分测试新增功能，确保不影响现有功能
4. 如有不确定的地方，请先咨询项目负责人 