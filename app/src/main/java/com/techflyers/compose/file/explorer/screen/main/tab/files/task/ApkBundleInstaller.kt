package com.techflyers.compose.file.explorer.screen.main.tab.files.task

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.os.Environment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import com.techflyers.compose.file.explorer.App.Companion.globalClass
import com.techflyers.compose.file.explorer.screen.main.tab.files.shizuku.ShizukuManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

/**
 * Handles installation of single APKs and split APK bundles (.xapk, .apkm, .apks).
 *
 * Supports:
 * - Standard PackageInstaller session API (no extra privileges)
 * - Optional Shizuku / root privileged install when available and preference enabled
 *
 * Bundles are ZIP archives containing:
 * - Multiple .apk files (base + splits)
 * - Optional .obb files
 */
object ApkBundleInstaller {

    var isInstalling by mutableStateOf(false)
        private set
    var installProgress by mutableFloatStateOf(0f)
        private set
    var installStatus by mutableStateOf("")
        private set

    /**
     * Install an APK bundle file (.xapk / .apkm / .apks) or a regular .apk.
     *
     * @param context Activity context (needed for PackageInstaller session / intents)
     * @param bundlePath Path to the file
     */
    suspend fun install(context: Context, bundlePath: String) {
        isInstalling = true
        installProgress = 0f
        installStatus = "Preparing…"

        val source = File(bundlePath)
        if (!source.exists()) {
            installStatus = "Error: file not found"
            globalClass.showMsg("Installation failed: file not found")
            isInstalling = false
            return
        }

        val usePrivileged = globalClass.preferencesManager.preferShizukuForApkInstall &&
            ShizukuManager.isPrivileged

        try {
            if (isApkBundle(source.extension)) {
                installBundle(context, source, usePrivileged)
            } else if (source.extension.equals("apk", true)) {
                installProgress = 0.2f
                installStatus = if (usePrivileged) "Installing via Shizuku…" else "Installing APK…"
                if (usePrivileged) {
                    installSingleApkPrivileged(source)
                } else {
                    installSingleApk(context, source)
                }
                installProgress = 1.0f
                installStatus = "Installation started"
            } else {
                throw Exception("Unsupported file type: ${source.extension}")
            }
        } catch (e: Exception) {
            installStatus = "Error: ${e.message}"
            globalClass.showMsg("Installation failed: ${e.message}")
            globalClass.logger.logError(e)
        } finally {
            isInstalling = false
        }
    }

    private suspend fun installBundle(context: Context, bundleFile: File, usePrivileged: Boolean) {
        installStatus = "Extracting bundle…"

        val tempDir = File(context.cacheDir, "apk_bundle_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        val apkFiles = mutableListOf<File>()
        val obbFiles = mutableListOf<File>()

        withContext(Dispatchers.IO) {
            ZipInputStream(FileInputStream(bundleFile)).use { zis ->
                var entry = zis.nextEntry
                var totalEntries = 0
                while (entry != null) {
                    totalEntries++
                    val outFile = File(tempDir, entry.name)
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { fos ->
                            val buffer = ByteArray(8192)
                            var len: Int
                            while (zis.read(buffer).also { len = it } > 0) {
                                fos.write(buffer, 0, len)
                            }
                        }
                        when {
                            outFile.extension.equals("apk", true) -> apkFiles.add(outFile)
                            outFile.extension.equals("obb", true) -> obbFiles.add(outFile)
                        }
                    }
                    installProgress = 0.3f * (totalEntries.toFloat() / maxOf(totalEntries, 1))
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }

        if (apkFiles.isEmpty()) {
            throw Exception("No APK files found in the bundle")
        }

        // Copy OBB files if any
        if (obbFiles.isNotEmpty()) {
            installStatus = "Copying OBB files…"
            installProgress = 0.4f
            for (obb in obbFiles) {
                val obbName = obb.name
                val parts = obbName.split(".")
                if (parts.size >= 4) {
                    val packageName = parts.drop(2).dropLast(1).joinToString(".")
                    val obbDir = File(
                        Environment.getExternalStorageDirectory(),
                        "Android/obb/$packageName"
                    )
                    obbDir.mkdirs()
                    withContext(Dispatchers.IO) {
                        obb.copyTo(File(obbDir, obb.name), overwrite = true)
                    }
                }
            }
        }

        installProgress = 0.5f

        if (apkFiles.size == 1) {
            installStatus = if (usePrivileged) "Installing via Shizuku…" else "Installing APK…"
            if (usePrivileged) {
                installSingleApkPrivileged(apkFiles.first())
            } else {
                installSingleApk(context, apkFiles.first())
            }
        } else {
            installStatus = if (usePrivileged) "Installing split APKs via Shizuku…" else "Installing split APKs…"
            if (usePrivileged) {
                installSplitApksPrivileged(apkFiles)
            } else {
                installSplitApks(context, apkFiles)
            }
        }

        installProgress = 1.0f
        installStatus = "Installation started"

        // Clean up temp directory
        withContext(Dispatchers.IO) {
            tempDir.deleteRecursively()
        }
    }

    private fun installSingleApk(context: Context, apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Privileged single-APK install via Shizuku/root using pm install.
     */
    private suspend fun installSingleApkPrivileged(apkFile: File) {
        withContext(Dispatchers.IO) {
            val path = apkFile.absolutePath.replace("'", "'\\''")
            val result = ShizukuManager.executeCommand("pm install -r -t --user 0 '$path'")
                ?: throw Exception("Privileged shell unavailable")
            if (result.exitCode != 0) {
                val err = result.stderr.ifBlank { result.stdout }.ifBlank { "exit ${result.exitCode}" }
                throw Exception("pm install failed: $err")
            }
        }
    }

    private suspend fun installSplitApks(context: Context, apkFiles: List<File>) {
        withContext(Dispatchers.IO) {
            val packageInstaller = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
            val totalSize = apkFiles.sumOf { it.length() }
            params.setSize(totalSize)

            val sessionId = packageInstaller.createSession(params)
            val session = packageInstaller.openSession(sessionId)

            try {
                for ((index, apkFile) in apkFiles.withIndex()) {
                    installStatus = "Writing APK ${index + 1}/${apkFiles.size}…"
                    installProgress = 0.5f + (0.4f * index / apkFiles.size)

                    FileInputStream(apkFile).use { input ->
                        session.openWrite(apkFile.name, 0, apkFile.length()).use { output ->
                            val buffer = ByteArray(8192)
                            var len: Int
                            while (input.read(buffer).also { len = it } > 0) {
                                output.write(buffer, 0, len)
                            }
                            session.fsync(output)
                        }
                    }
                }

                val intent = Intent(context, context::class.java)
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    sessionId,
                    intent,
                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                session.commit(pendingIntent.intentSender)
            } catch (e: Exception) {
                session.abandon()
                throw e
            }
        }
    }

    /**
     * Privileged split-APK install via Shizuku/root using pm install-create / install-write / install-commit.
     */
    private suspend fun installSplitApksPrivileged(apkFiles: List<File>) {
        withContext(Dispatchers.IO) {
            val totalSize = apkFiles.sumOf { it.length() }

            // Create session
            val createOut = ShizukuManager.executeCommand(
                "pm install-create -S $totalSize --user 0"
            ) ?: throw Exception("Privileged shell unavailable")
            if (createOut.exitCode != 0) {
                throw Exception("pm install-create failed: ${createOut.stderr.ifBlank { createOut.stdout }}")
            }
            // Output is usually: Success: created install session [123456]
            val sessionId = Regex("""\[(\d+)]""").find(createOut.stdout)?.groupValues?.get(1)
                ?: throw Exception("Could not parse session id from: ${createOut.stdout}")

            try {
                for ((index, apkFile) in apkFiles.withIndex()) {
                    installStatus = "Writing APK ${index + 1}/${apkFiles.size} (privileged)…"
                    installProgress = 0.5f + (0.4f * index / apkFiles.size)

                    val path = apkFile.absolutePath.replace("'", "'\\''")
                    val name = apkFile.name.replace("'", "'\\''")
                    val size = apkFile.length()
                    val writeCmd = "pm install-write -S $size $sessionId '$name' < '$path'"
                    // Use cat redirection via shell
                    val writeOut = ShizukuManager.executeCommand(
                        "pm install-write -S $size $sessionId '$name' < '$path'"
                    ) ?: throw Exception("install-write failed (no shell)")
                    if (writeOut.exitCode != 0) {
                        // Fallback: some environments need cat
                        val writeOut2 = ShizukuManager.executeCommand(
                            "cat '$path' | pm install-write -S $size $sessionId '$name'"
                        )
                        if (writeOut2 == null || writeOut2.exitCode != 0) {
                            val err = (writeOut2?.stderr ?: writeOut.stderr)
                                .ifBlank { writeOut2?.stdout ?: writeOut.stdout }
                            throw Exception("pm install-write failed: $err")
                        }
                    }
                }

                installStatus = "Committing session…"
                val commitOut = ShizukuManager.executeCommand("pm install-commit $sessionId")
                    ?: throw Exception("install-commit failed (no shell)")
                if (commitOut.exitCode != 0) {
                    throw Exception("pm install-commit failed: ${commitOut.stderr.ifBlank { commitOut.stdout }}")
                }
            } catch (e: Exception) {
                ShizukuManager.executeCommand("pm install-abandon $sessionId")
                throw e
            }
        }
    }

    /**
     * Check if a file is an APK bundle format.
     */
    fun isApkBundle(extension: String): Boolean {
        return extension.lowercase() in setOf("xapk", "apkm", "apks")
    }
}
