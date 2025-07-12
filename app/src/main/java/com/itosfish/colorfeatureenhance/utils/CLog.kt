package com.itosfish.colorfeatureenhance.utils

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * 统一日志管理系统
 * 提供标准的Android日志方法，同时将日志存储到内存中以便导出
 */
object CLog {
    
    // 日志条目数据类
    data class LogEntry(
        val timestamp: Long,
        val level: String,
        val tag: String,
        val message: String,
        val throwable: Throwable? = null
    )
    
    // 使用线程安全的队列存储日志
    private val logQueue = ConcurrentLinkedQueue<LogEntry>()
    
    // 最大日志条目数，防止内存溢出
    private const val MAX_LOG_ENTRIES = 5000
    
    // 日志时间格式
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    
    /**
     * 信息级别日志
     */
    fun i(tag: String, message: String) {
        Log.i(tag, message)
        addLogEntry("INFO", tag, message)
    }
    
    /**
     * 错误级别日志
     */
    fun e(tag: String, message: String) {
        Log.e(tag, message)
        addLogEntry("ERROR", tag, message)
    }
    
    /**
     * 错误级别日志（带异常）
     */
    fun e(tag: String, message: String, throwable: Throwable) {
        Log.e(tag, message, throwable)
        addLogEntry("ERROR", tag, message, throwable)
    }
    
    /**
     * 调试级别日志
     */
    fun d(tag: String, message: String) {
        Log.d(tag, message)
        addLogEntry("DEBUG", tag, message)
    }
    
    /**
     * 警告级别日志
     */
    fun w(tag: String, message: String) {
        Log.w(tag, message)
        addLogEntry("WARN", tag, message)
    }
    
    /**
     * 警告级别日志（带异常）
     */
    fun w(tag: String, message: String, throwable: Throwable) {
        Log.w(tag, message, throwable)
        addLogEntry("WARN", tag, message, throwable)
    }
    
    /**
     * 添加日志条目到内存队列
     */
    private fun addLogEntry(level: String, tag: String, message: String, throwable: Throwable? = null) {
        val entry = LogEntry(
            timestamp = System.currentTimeMillis(),
            level = level,
            tag = tag,
            message = message,
            throwable = throwable
        )
        
        logQueue.offer(entry)
        
        // 如果超过最大条目数，移除最旧的条目
        while (logQueue.size > MAX_LOG_ENTRIES) {
            logQueue.poll()
        }
    }
    
    /**
     * 获取所有日志条目
     */
    fun getAllLogs(): List<LogEntry> {
        return logQueue.toList()
    }
    
    /**
     * 获取格式化的日志文本
     */
    fun getFormattedLogs(): String {
        val logs = getAllLogs()
        if (logs.isEmpty()) {
            return "暂无日志记录"
        }
        
        val stringBuilder = StringBuilder()
        stringBuilder.append("ColorFeatureEnhance 日志导出\n")
        stringBuilder.append("导出时间: ${dateFormat.format(Date())}\n")
        stringBuilder.append("日志条目数: ${logs.size}\n")
        stringBuilder.append("=".repeat(50) + "\n\n")
        
        logs.forEach { entry ->
            val timestamp = dateFormat.format(Date(entry.timestamp))
            stringBuilder.append("[$timestamp] [${entry.level}] ${entry.tag}: ${entry.message}\n")
            
            // 如果有异常信息，添加异常堆栈
            entry.throwable?.let { throwable ->
                stringBuilder.append("异常信息: ${throwable.message}\n")
                stringBuilder.append("异常堆栈:\n")
                throwable.stackTrace.forEach { stackElement ->
                    stringBuilder.append("  at $stackElement\n")
                }
                stringBuilder.append("\n")
            }
        }
        
        return stringBuilder.toString()
    }
    
    /**
     * 清空日志记录
     */
    fun clearLogs() {
        logQueue.clear()
    }
    
    /**
     * 获取当前日志条目数量
     */
    fun getLogCount(): Int {
        return logQueue.size
    }
}
