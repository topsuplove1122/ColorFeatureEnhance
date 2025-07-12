---
type: "always_apply"
---


此项目为ColorOS特性补全可视化编辑器

此项目需要Root权限，主要使用kotlin编写，UI方面使用Jetpack Compose，UI风格为最新的Material 3 Expressive风格
采用基线+补丁配置管理架构，支持OTA更新，内置统一CLog日志系统，支持中英文双语界面
1. 代码架构
项目采用分层架构设计，遵循以下原则：
UI层：负责界面渲染，不包含业务逻辑 (ui/)
业务逻辑层：处理应用功能实现，不依赖于具体UI实现 (domain/)
数据层：负责数据获取、持久化等操作 (data/)
配置管理层：处理配置文件合并与补丁管理 (config/)
工具层：提供通用工具类和系统集成 (utils/)
2. UI组件规范
UI组件目录结构
ui/components/：存放可复用的UI组件 (SearchBar, HighlightedText, TopAppBarComponent等)
ui/screens/：存放完整页面 (FeatureConfigScreen等)
ui/search/：存放搜索相关功能 (SearchLogic等)
ui/theme/：存放主题相关定义
没有的需要先询问我，不要自己决定

Compose编写规则
使用@Composable注解标记所有可组合函数
预览函数使用@Preview注解，便于开发时查看
组件应支持通过参数自定义，而非硬编码
3. 业务逻辑规范
业务逻辑代码放在domain包下 (FeatureRepository接口等)
按功能模块组织代码结构
使用接口定义功能，实现与调用分离
异步操作使用协程处理
4. 数据处理规范
数据模型放在data/model下 (AppFeature, OplusFeature, FeatureGroup等)
数据仓库实现放在data/repository下 (XmlFeatureRepository, XmlOplusFeatureRepository等)
遵循单一数据源原则
5. 配置管理规范
配置管理核心代码放在config包下 (ConfigMergeManager等)
采用基线+补丁模式，系统配置与用户修改分离
支持三层目录结构：system_baseline, user_patches, merged_output
使用JSON格式存储补丁文件，支持ADD/MODIFY/REMOVE操作
6. 命名规范
变量命名：驼峰命名法，如autoSetRules
类名：首字母大写的驼峰命名法，如AutoSetRules
常量：全大写，下划线分隔，如MAX_RETRY_COUNT
函数命名：动词开头，描述其行为，如getConfig()、saveSettings()
7. 日志规范
统一使用CLog系统替代原生Log，支持内存存储和导出
日志级别：CLog.i(), CLog.e(), CLog.d(), CLog.w()
重要操作必须记录日志，便于问题排查
8. 其他开发约定
新增功能时，确保其他功能、引用不受影响
代码应有适当中文注释，尤其是复杂逻辑
修改公共组件前，应评估对使用该组件的地方的影响
使用Dialog时，使用原生MaterialDialog
界面风格遵循Material 3 Expressive设计风格
支持Root权限检测，不支持原版KernelSU
国际化支持中英文双语界面

在编写代码时，请确保代码的结构清晰，易于维护。
如果你有不确定的地方，你需要先询问我而不是自己进行决定

在开始一个新任务前，积极使用相关工具并由我审核、修改并最终明确确认后才开始代码编辑

在编写代码时，请确保代码的结构清晰，易于维护。
如果你有不确定的地方，你需要先询问我而不是自己进行决定

在开始一个新任务前，尽可能使用todo_tool并由我审核、修改并最终明确确认后才开始代码编辑，除非该任务极其简单
积极使用web search

本项目中所有需要MainActivity的context的地方，一律使用app即可。如Toast.makeText(app,"text",xxxxx)