package com.itosfish.colorfeatureenhance.utils

import android.util.Log
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.itosfish.colorfeatureenhance.MainActivity.Companion.app
import com.itosfish.colorfeatureenhance.R
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader


object CSU {
    /**
     * 判断指定文件是否存在。
     *
     * @param filePath 要检查的文件路径。
     * @return 如果文件存在则返回 true，否则返回 false。
     */
    fun fileExists(filePath: String): Boolean {
        // 构建 Shell 命令：
        // 使用 if 语句和 [ -e ] 来检查文件是否存在
        // 如果存在，输出 "exists"，否则输出 "not exists"
        val command = "if [ -e \"$filePath\" ]; then echo \"exists\"; else echo \"not exists\"; fi"

        // 使用 runWithSu 执行命令并获取输出
        val output = runWithSu(command).trim()

        // 根据输出判断文件是否存在
        return output == "exists"
    }

    /**
     * 判断目录是否存在。
     * @param dirPath 目录路径
     * @return true 存在，false 不存在
     */
    fun dirExists(dirPath: String): Boolean {
        val command = "if [ -d \"$dirPath\" ]; then echo \"exists\"; else echo \"not exists\"; fi"
        val output = runWithSu(command).trim()
        return output == "exists"
    }

    /**
     * 判断是否使用 KernelSU（通过检测 /data/adb/ksu/modules.img 是否存在）
     */
    fun isKSU(): Boolean {
        return fileExists("/data/adb/ksu/modules.img")
    }

    // 检查是否具有root权限
    fun isRooted(): Boolean {
        return checkRootMethod()
    }

    private fun checkRootMethod(): Boolean {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val result = reader.readLine()
            return result != null && result.contains("uid=0")  // 0 是 root 的 UID
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    /**
     * 执行具有超级用户权限的 Shell 命令，并返回命令的输出结果。
     *
     * @param cmd 要执行的 Shell 命令，可以包含多个命令，以分号或换行符分隔。
     * @return Shell 命令的输出结果。
     */
    fun runWithSu(cmd: String): String {
        val output = StringBuilder()
        try {
            Log.i("APP_SHELL", "准备以root权限执行命令: $cmd")
            // 启动 su 进程
            val process = Runtime.getRuntime().exec("su")

            // 向 su 进程发送命令
            runShellScript(process, cmd)

            // 读取标准输出
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    Log.d("APP_SHELL_OUTPUT", line!!)
                    output.append(line).append("\n")
                }
            }

            // 可选：读取错误输出
            BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    Log.e("APP_SHELL_ERROR", line!!)
                }
            }

            // 等待进程结束
            val exitCode = process.waitFor()
            Log.i("APP_SHELL", "命令执行结束，退出码: $exitCode")
        } catch (e: Exception) {
            Log.e("APP_SHELL", "执行 Shell 命令失败: ${e.message}")
        }
        Log.i("APP_SHELL", "命令输出: $output")
        return output.toString()
    }

    fun checkRoot() {
        val isRooted = CSU.isRooted()
        if (!isRooted) {
            Log.i("MainActivity", "Device not rooted")
            MaterialAlertDialogBuilder(app)
                .setTitle(R.string.root_permission_title)
                .setMessage(R.string.root_permission_message)
                .setCancelable(false)
                .setNegativeButton(
                    R.string.root_permission_exit
                ) { _, _ ->
                    app.finish()
                }
                .setNeutralButton(R.string.root_permission_continue) { _, _ ->
                    Toast.makeText(app, R.string.root_permission_warning, Toast.LENGTH_LONG)
                        .show()
                }
                .show()
        }
    }

    /**
     * 向 Process 的输出流中写入 Shell 命令。
     *
     * @param process 需要写入命令的 Process 对象。
     * @param cmd 要执行的 Shell 命令，可以包含多个命令，以分号或换行符分隔。
     */
    private fun runShellScript(process: Process, cmd: String) {
        try {
            DataOutputStream(process.outputStream).use { outputStream ->
                Log.i("APP_SHELL", "Cmd :$cmd")
                // 写入命令，可以包含多个命令，用分号或换行符分隔
                outputStream.writeBytes("$cmd\n")
                // 写入 exit 命令以结束 su 会话
                outputStream.writeBytes("exit\n")
                outputStream.flush()
            }
        } catch (e: Exception) {
            Log.e("APP_SHELL", "运行 Shell 脚本失败，错误信息: ${e.message}")
        }
    }
}