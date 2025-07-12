# ColorFeatureEnhance 云端配置功能说明

## 概述

ColorFeatureEnhance 应用现已支持云端配置功能，可以从远程服务器下载特性描述的多语言映射，实现特性描述的云端管理和动态更新。

## 功能特点

### 1. 三级优先级查询链
- **最高优先级**: CloudFeatureMappings (云端多语言配置)
- **中等优先级**: App/OplusFeatureMappings (应用内置资源)
- **最低优先级**: UserFeatureMappings (用户自定义单语言覆盖)

### 2. 多语言支持
- 支持中文 (`zh`) 和英文 (`en`) 双语言描述
- 自动根据设备语言选择对应描述
- 语言回退机制：目标语言不存在时自动回退到英文

### 3. 编辑界面智能限制
- 当特性存在云端配置时，编辑对话框中的描述输入框将被禁用
- 禁用行为与现有的内置预设描述匹配逻辑保持一致
- 云端配置具有最高优先级，会覆盖用户自定义描述

### 4. 静默更新机制
- 应用启动时异步检查云端配置更新
- 网络请求失败时静默忽略，不影响应用正常运行
- 更新过程不阻塞主界面加载

## 云端配置文件格式

### JSON 结构
```json
{
  "feature_key": {
    "zh": "中文描述",
    "en": "English description"
  },
  "another_feature": {
    "zh": "另一个特性的中文描述",
    "en": "Another feature English description"
  }
}
```

### 字段说明
- **feature_key**: 特性名称，必须与系统配置文件中的特性名称完全匹配
- **zh**: 中文描述（可选，但至少要有一种语言的描述）
- **en**: 英文描述（可选，但至少要有一种语言的描述）

### 特殊前缀规则
对于 oplus 特性，建议遵循以下前缀规则：
- `[软]`: oplus.software 开头的特性
- `[硬]`: oplus.hardware 开头的特性

## 技术实现

### 核心类说明

#### CloudFeatureMappings
- **职责**: 云端配置的本地持久化存储
- **存储方式**: SharedPreferences
- **存储格式**: `feature_key → {"zh":"中文描述","en":"English description"}`
- **核心方法**: `getDescription(feature: String, lang: String): String?`

#### RemoteConfigManager
- **职责**: 云端配置下载、校验和解析
- **网络库**: OkHttp 4.12.0
- **超时设置**: 连接超时 10s，读取超时 30s
- **错误处理**: 静默处理网络错误，记录日志但不中断应用

### 配置更新流程
1. 应用启动时异步检查云端配置
2. 下载远程 JSON 配置文件
3. 验证 JSON 格式和数据有效性
4. 解析并保存到本地 SharedPreferences
5. 更新版本标识（使用时间戳）

### 描述查询流程
1. 首先查询云端配置映射 (CloudFeatureMappings)
2. 如果不存在，查询应用内置资源映射 (App/OplusFeatureMappings)
3. 如果仍不存在，查询用户自定义映射 (UserFeatureMappings)
4. 最终回退到特性名称本身

## 配置部署

### 1. 准备配置文件
参考 `docs/cloud-config-example.json` 创建配置文件

### 2. 部署到服务器
将 JSON 文件部署到可公开访问的 HTTP/HTTPS 服务器

### 3. 配置 URL
在 `RemoteConfigManager` 中修改 `DEFAULT_CONFIG_URL` 常量：
```kotlin
private const val DEFAULT_CONFIG_URL = "https://your-server.com/path/to/mappings.json"
```

### 4. 验证配置
- 确保 JSON 格式正确
- 验证特性名称与系统配置匹配
- 测试网络访问性

## 编辑界面行为

### 描述输入框禁用条件
描述输入框在以下情况下会被禁用（只读状态）：
1. **内置预设描述匹配**：当特性描述与应用内置预设描述完全匹配时
2. **云端配置存在**：当特性存在云端配置描述时（新增功能）

### 禁用状态的视觉表现
- 输入框变为灰色（disabled 状态）
- 输入框设置为只读（readOnly = true）
- 用户无法修改描述内容
- 确认按钮仍然可用，但不会保存用户自定义映射

### 优先级覆盖行为
- 云端配置会覆盖用户之前保存的自定义描述
- 当云端配置被删除后，会回退到内置预设描述（如果存在）
- 最后才会使用用户自定义描述

## 使用示例

### 添加新特性描述
```json
{
  "com.oplus.new_awesome_feature": {
    "zh": "超棒的新功能",
    "en": "Awesome New Feature"
  }
}
```

### 更新现有特性描述
```json
{
  "com.oplus.directservice.aitoolbox_enable": {
    "zh": "AI 聊天场景增强版",
    "en": "Enhanced AI Chat Scene"
  }
}
```

## 注意事项

### 1. 网络权限
应用已添加必要的网络权限：
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### 2. 错误处理
- 网络请求失败不会影响应用正常运行
- JSON 解析错误会记录日志但不中断流程
- 配置验证失败时会跳过无效条目

### 3. 性能考虑
- 云端配置检查在后台线程执行
- 本地缓存避免重复网络请求
- 配置更新不阻塞主界面

### 4. 安全考虑
- 建议使用 HTTPS 协议
- 配置文件应定期审核
- 避免在配置中包含敏感信息

## 调试和监控

### 日志标签
- `CloudFeatureMappings`: 本地存储相关日志
- `RemoteConfigManager`: 网络请求和解析相关日志
- `MainActivity`: 应用启动和集成相关日志

### 常用调试命令
```kotlin
// 获取云端配置统计信息
val stats = RemoteConfigManager.getInstance(context).getConfigStats()

// 清除云端配置缓存
RemoteConfigManager.getInstance(context).clearCache()

// 手动触发配置更新
val result = RemoteConfigManager.getInstance(context).checkAndUpdateConfig()
```

## 版本兼容性

- **最低 Android 版本**: Android 14 (API 34)
- **推荐 Android 版本**: Android 15+ (API 35+)
- **网络库版本**: OkHttp 4.12.0
- **序列化库**: kotlinx.serialization 1.7.3

## 更新日志

### v0.54.1 (增强版)
- **修正优先级逻辑**：云端配置现在具有最高优先级，覆盖用户自定义描述
- **编辑界面限制**：当特性存在云端配置时，编辑对话框中的描述输入框被禁用
- **智能映射管理**：云端配置存在时不保存用户自定义映射，避免冲突
- **一致性改进**：云端配置禁用行为与内置预设描述匹配逻辑保持一致

### v0.54
- 初始实现云端配置功能
- 支持多语言描述映射
- 实现三级优先级查询链
- 添加静默更新机制
