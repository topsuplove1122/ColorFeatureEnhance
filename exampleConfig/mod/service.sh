#!/system/bin/sh
# ColorOS Features Enhance - Service脚本 v2.1
# 负责在系统启动后将 post-fs-data.sh 创建的临时配置文件复制到应用存储目录
# 流程：post-fs-data.sh (系统配置 -> 临时目录) -> service.sh (临时目录 -> 应用存储)
# 采用"删除并重建"策略，确保目录状态一致性，避免权限和状态问题
# 注意：临时目录 temp_configs 会被保留，用于调试和状态跟踪

MODDIR=${0%/*}

# 系统配置源路径
SYSTEM_CONFIG_DIR="/my_product/etc/extension"

# 应用数据目录
APP_PACKAGE="com.itosfish.colorfeatureenhance"
APP_DATA_BASE="/data/media/0/Android/data/$APP_PACKAGE/files"
APP_CONFIG_DIR="$APP_DATA_BASE/configs"
SYSTEM_BASELINE_DIR="$APP_CONFIG_DIR/system_baseline"
USER_PATCHES_DIR="$APP_CONFIG_DIR/user_patches"
MERGED_OUTPUT_DIR="$APP_CONFIG_DIR/merged_output"

# 模块临时目录（用于中转）
MODULE_TEMP_DIR="/data/adb/cos_feat_e/temp_configs"

# 配置文件名
APP_FEATURES_FILE="com.oplus.app-features.xml"
OPLUS_FEATURES_FILE="com.oplus.oplus-feature.xml"

# 日志文件
LOG_FILE="/cache/colorfeature_enhance.log"

# 状态标记文件
STATUS_FILE="$MODULE_TEMP_DIR/service_status.txt"

# 日志函数
log_info() {
    echo "[ColorFeatureEnhance] $(date '+%Y-%m-%d %H:%M:%S') $1" >> "$LOG_FILE"
}

log_debug() {
    echo "[ColorFeatureEnhance-DEBUG] $(date '+%Y-%m-%d %H:%M:%S') $1" >> "$LOG_FILE"
}

log_error() {
    echo "[ColorFeatureEnhance-ERROR] $(date '+%Y-%m-%d %H:%M:%S') $1" >> "$LOG_FILE"
}

# ============================================================================
# 工具函数
# ============================================================================

# 检查目录是否存在
dir_exists() {
    [ -d "$1" ]
}

# 检查文件是否存在
file_exists() {
    [ -f "$1" ]
}

# 强制重新创建目录（删除后重建）
force_recreate_dir() {
    local dir="$1"
    local mode="${2:-755}"

    log_debug "强制重新创建目录: $dir (权限: $mode)"

    # 如果目录存在，先删除
    if dir_exists "$dir"; then
        log_debug "删除现有目录: $dir"
        rm -rf "$dir" 2>/dev/null
    fi

    # 重新创建目录
    mkdir -p "$dir" 2>/dev/null
    chmod "$mode" "$dir" 2>/dev/null

    if dir_exists "$dir"; then
        log_debug "目录重新创建成功: $dir"
        return 0
    else
        log_error "目录重新创建失败: $dir"
        return 1
    fi
}

# 安全创建目录（保持兼容性）
safe_mkdir() {
    force_recreate_dir "$1" "$2"
}

# 安全复制文件
safe_copy() {
    local src="$1"
    local dst="$2"
    local mode="${3:-644}"

    if ! file_exists "$src"; then
        log_debug "源文件不存在: $src"
        return 1
    fi

    log_debug "复制文件: $src -> $dst"
    cp "$src" "$dst" 2>/dev/null
    local result=$?

    if [ $result -eq 0 ] && file_exists "$dst"; then
        chmod "$mode" "$dst" 2>/dev/null
        log_debug "文件复制成功: $(basename "$dst")"
        return 0
    else
        log_error "文件复制失败: $src -> $dst (返回值: $result)"
        return 1
    fi
}

# 记录状态到文件
write_status() {
    local status="$1"
    local message="$2"
    echo "$(date '+%Y-%m-%d %H:%M:%S') $status: $message" > "$STATUS_FILE"
}

# ============================================================================
# 启动日志
# ============================================================================

log_info "=== ColorFeatureEnhance Service v2.1 启动 ==="
log_debug "模块目录: $MODDIR"
log_debug "临时配置源: $MODULE_TEMP_DIR"
log_debug "应用配置目录: $APP_CONFIG_DIR"
log_debug "功能: 临时目录 -> 应用存储"

# ============================================================================
# 核心功能函数
# ============================================================================

# 等待系统完全启动
wait_for_system_ready() {
    log_info "等待系统启动完成..."

    # 等待基本系统启动
    while [ "$(getprop sys.boot_completed)" != "1" ]; do
        sleep 3
    done
    log_debug "系统基本启动完成"

    # 等待用户解锁（检查用户0是否解锁）
    local unlock_wait=0
    local max_unlock_wait=60  # 最多等待3分钟

    while [ $unlock_wait -lt $max_unlock_wait ]; do
        if [ "$(getprop ro.crypto.state)" = "encrypted" ]; then
            # 设备加密，检查解锁状态
            if [ "$(getprop vold.decrypt)" = "trigger_restart_framework" ] || \
               [ "$(getprop service.bootanim.exit)" = "1" ]; then
                log_debug "用户已解锁"
                break
            fi
        else
            # 设备未加密，直接继续
            log_debug "设备未加密，继续执行"
            break
        fi

        log_debug "等待用户解锁... ($unlock_wait/$max_unlock_wait)"
        sleep 3
        unlock_wait=$((unlock_wait + 1))
    done

    # 额外等待确保文件系统稳定
    log_debug "等待文件系统稳定..."
    sleep 5

    log_info "系统准备就绪"
}

# 强制重新初始化应用目录结构
init_app_directories() {
    log_info "强制重新初始化应用目录结构..."

    # 如果整个配置目录存在，先完全删除
    if dir_exists "$APP_CONFIG_DIR"; then
        log_info "删除现有配置目录: $APP_CONFIG_DIR"
        rm -rf "$APP_CONFIG_DIR" 2>/dev/null
    fi

    # 重新创建完整的目录结构
    log_debug "重新创建目录结构..."
    force_recreate_dir "$APP_DATA_BASE" 755 || return 1
    force_recreate_dir "$APP_CONFIG_DIR" 755 || return 1
    force_recreate_dir "$SYSTEM_BASELINE_DIR" 755 || return 1
    force_recreate_dir "$USER_PATCHES_DIR" 755 || return 1
    force_recreate_dir "$MERGED_OUTPUT_DIR" 755 || return 1

    # 确保临时目录存在（保留现有内容，用于调试和状态跟踪）
    if ! dir_exists "$MODULE_TEMP_DIR"; then
        force_recreate_dir "$MODULE_TEMP_DIR" 755 || return 1
        log_debug "创建新的临时目录: $MODULE_TEMP_DIR"
    else
        log_debug "保留现有临时目录: $MODULE_TEMP_DIR"
        # 显示临时目录内容用于调试
        if [ -n "$(ls -A "$MODULE_TEMP_DIR" 2>/dev/null)" ]; then
            log_debug "临时目录现有内容:"
            ls -la "$MODULE_TEMP_DIR" 2>/dev/null | while read line; do
                log_debug "  $line"
            done
        else
            log_debug "临时目录为空"
        fi
    fi

    log_info "目录结构强制重新初始化完成（临时目录已保留）"
    return 0
}

# 从临时目录复制配置文件到应用存储（强制覆盖模式）
copy_temp_to_app() {
    log_info "从临时目录强制复制配置文件到应用存储（删除并重建）..."

    local success_count=0
    local total_files=0

    # 复制 app-features.xml
    local app_src="$MODULE_TEMP_DIR/$APP_FEATURES_FILE"
    local app_dst="$SYSTEM_BASELINE_DIR/$APP_FEATURES_FILE"

    if file_exists "$app_src"; then
        log_debug "发现临时配置: $app_src"
        log_debug "源文件大小: $(stat -c%s "$app_src" 2>/dev/null || echo "unknown")"

        # 如果目标文件存在，先删除
        if file_exists "$app_dst"; then
            log_debug "删除现有目标文件: $app_dst"
            rm -f "$app_dst" 2>/dev/null
        fi

        if safe_copy "$app_src" "$app_dst"; then
            success_count=$((success_count + 1))
            log_debug "目标文件大小: $(stat -c%s "$app_dst" 2>/dev/null || echo "unknown")"
        fi
        total_files=$((total_files + 1))
    else
        log_debug "临时配置不存在: $app_src"
        # 创建空的配置文件作为占位符
        echo '<?xml version="1.0" encoding="utf-8"?><extend_features></extend_features>' > "$app_dst"
        chmod 644 "$app_dst" 2>/dev/null
        log_debug "创建空的 app-features 配置文件"
    fi

    # 复制 oplus-feature.xml
    local oplus_src="$MODULE_TEMP_DIR/$OPLUS_FEATURES_FILE"
    local oplus_dst="$SYSTEM_BASELINE_DIR/$OPLUS_FEATURES_FILE"

    if file_exists "$oplus_src"; then
        log_debug "发现临时配置: $oplus_src"
        log_debug "源文件大小: $(stat -c%s "$oplus_src" 2>/dev/null || echo "unknown")"

        # 如果目标文件存在，先删除
        if file_exists "$oplus_dst"; then
            log_debug "删除现有目标文件: $oplus_dst"
            rm -f "$oplus_dst" 2>/dev/null
        fi

        if safe_copy "$oplus_src" "$oplus_dst"; then
            success_count=$((success_count + 1))
            log_debug "目标文件大小: $(stat -c%s "$oplus_dst" 2>/dev/null || echo "unknown")"
        fi
        total_files=$((total_files + 1))
    else
        log_debug "临时配置不存在: $oplus_src"
        # 创建空的配置文件作为占位符
        echo '<?xml version="1.0" encoding="utf-8"?><oplus-config></oplus-config>' > "$oplus_dst"
        chmod 644 "$oplus_dst" 2>/dev/null
        log_debug "创建空的 oplus-feature 配置文件"
    fi

    log_info "临时配置文件强制复制完成: $success_count/$total_files 成功"

    # 复制时间戳文件（如果存在）
    local temp_timestamp="$MODULE_TEMP_DIR/last_copy.txt"
    local app_timestamp="$SYSTEM_BASELINE_DIR/last_copy.txt"

    if file_exists "$temp_timestamp"; then
        log_debug "复制时间戳文件: $temp_timestamp -> $app_timestamp"
        if file_exists "$app_timestamp"; then
            rm -f "$app_timestamp" 2>/dev/null
        fi
        safe_copy "$temp_timestamp" "$app_timestamp"
    else
        # 如果临时目录没有时间戳文件，创建新的
        echo "$(date '+%Y-%m-%d %H:%M:%S')" > "$app_timestamp"
        chmod 644 "$app_timestamp" 2>/dev/null
        log_debug "创建新的时间戳文件: $app_timestamp"
    fi

    return 0
}
# 环境检查和诊断
check_environment() {
    log_debug "=== 环境检查 ==="
    log_debug "当前用户: $(id 2>/dev/null || echo "unknown")"
    log_debug "SELinux状态: $(getenforce 2>/dev/null || echo "unknown")"
    log_debug "/data/media/0 权限: $(ls -ld /data/media/0 2>/dev/null || echo "无法访问")"
    log_debug "/data/media/0/Android 权限: $(ls -ld /data/media/0/Android 2>/dev/null || echo "无法访问")"
    log_debug "/data/media/0/Android/data 权限: $(ls -ld /data/media/0/Android/data 2>/dev/null || echo "无法访问")"

    # 检查临时配置源
    log_debug "临时配置源检查:"
    log_debug "  $MODULE_TEMP_DIR 存在: $(dir_exists "$MODULE_TEMP_DIR" && echo "是" || echo "否")"
    if dir_exists "$MODULE_TEMP_DIR"; then
        log_debug "  $APP_FEATURES_FILE 存在: $(file_exists "$MODULE_TEMP_DIR/$APP_FEATURES_FILE" && echo "是" || echo "否")"
        log_debug "  $OPLUS_FEATURES_FILE 存在: $(file_exists "$MODULE_TEMP_DIR/$OPLUS_FEATURES_FILE" && echo "是" || echo "否")"
        log_debug "  last_copy.txt 存在: $(file_exists "$MODULE_TEMP_DIR/last_copy.txt" && echo "是" || echo "否")"

        # 显示临时目录内容
        log_debug "临时目录内容:"
        ls -la "$MODULE_TEMP_DIR" 2>/dev/null | while read line; do
            log_debug "    $line"
        done
    fi
}

# ============================================================================
# 主执行流程
# ============================================================================

main() {
    write_status "STARTING" "Service脚本开始执行"
    log_debug "=== 主流程开始 ==="

    # 1. 等待系统准备就绪
    wait_for_system_ready
    if [ $? -ne 0 ]; then
        write_status "ERROR" "系统启动等待失败"
        log_error "系统启动等待失败"
        return 1
    fi

    # 2. 环境检查
    check_environment

    # 3. 强制重新初始化应用目录结构
    log_info "强制重新初始化应用目录结构..."
    init_app_directories
    if [ $? -ne 0 ]; then
        write_status "ERROR" "目录强制重新初始化失败"
        log_error "应用目录强制重新初始化失败"
        return 1
    fi

    # 4. 强制复制临时配置文件到应用存储
    log_info "强制复制临时配置文件到应用存储..."
    copy_temp_to_app
    if [ $? -ne 0 ]; then
        write_status "ERROR" "临时配置文件强制复制失败"
        log_error "临时配置文件强制复制失败"
        return 1
    fi

    # 5. 最终状态检查
    log_debug "=== 最终状态检查 ==="
    if dir_exists "$SYSTEM_BASELINE_DIR"; then
        log_debug "system_baseline 目录内容:"
        ls -la "$SYSTEM_BASELINE_DIR" 2>/dev/null | while read line; do
            log_debug "  $line"
        done
    fi

    write_status "SUCCESS" "Service脚本执行完成"
    log_info "=== ColorFeatureEnhance Service v2.0 执行完成 ==="
    return 0
}

# ============================================================================
# 脚本入口
# ============================================================================

# 在后台执行主流程，避免阻塞系统启动
(
    main
    exit_code=$?
    if [ $exit_code -eq 0 ]; then
        log_info "Service脚本成功完成"
    else
        log_error "Service脚本执行失败，退出码: $exit_code"
    fi
) &
