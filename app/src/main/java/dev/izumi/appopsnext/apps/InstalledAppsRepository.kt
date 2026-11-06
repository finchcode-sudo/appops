package dev.izumi.appopsnext.apps

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import dev.izumi.appopsnext.apps.model.InstalledApp
import java.io.File
import java.text.Collator
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Lists the installed packages quickly:
 * - the last list is kept on disk and shown at once on the next start;
 * - a label is only resolved again when the package was updated or the language changed;
 * - labels that must be resolved are loaded on several threads.
 */
class InstalledAppsRepository(
    context: Context,
) {
    private val packageManager = context.packageManager
    private val cacheFile = File(context.noBackupFilesDir, "installed-apps-v1.json")

    private val cacheMutex = Mutex()
    private var cached: List<InstalledApp>? = null
    private var cachedLocale: Locale? = null
    private var cachedAtNanos = 0L

    /** The list saved by the last load, or null when there is none for this language. */
    suspend fun loadCachedApps(): List<InstalledApp>? = withContext(Dispatchers.IO) {
        readDisk()?.let { (tag, apps) ->
            if (tag == Locale.getDefault().toLanguageTag()) apps else null
        }
    }

    suspend fun loadInstalledApps(forceRefresh: Boolean = false): List<InstalledApp> =
        withContext(Dispatchers.IO) {
            cacheMutex.withLock {
                val locale = Locale.getDefault()
                val now = System.nanoTime()
                val previous = cached
                if (!forceRefresh && previous != null && cachedLocale == locale &&
                    now - cachedAtNanos < 60_000_000_000L) return@withLock previous

                val known = readDisk()
                    ?.takeIf { it.first == locale.toLanguageTag() }
                    ?.second
                    ?.associateBy { it.packageName }
                    .orEmpty()
                val packages = packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))

                val apps = coroutineScope {
                    packages
                        .chunked(CHUNK_SIZE)
                        .map { chunk ->
                            async(Dispatchers.Default) {
                                chunk.mapNotNull { info -> toInstalledApp(info, known[info.packageName]) }
                            }
                        }
                        .awaitAll()
                        .flatten()
                }
                val labelCollator = Collator.getInstance(locale)
                val loaded = apps.sortedWith { left, right -> labelCollator.compare(left.label, right.label) }
                cached = loaded
                cachedLocale = locale
                cachedAtNanos = System.nanoTime()
                writeDisk(locale.toLanguageTag(), loaded)
                loaded
            }
        }

    private fun toInstalledApp(packageInfo: PackageInfo, previous: InstalledApp?): InstalledApp? {
        val applicationInfo = packageInfo.applicationInfo ?: return null
        val isSystem = applicationInfo.flags.hasAnyFlag(
            ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP,
        )
        val label = if (previous != null && previous.lastUpdateTimeMillis == packageInfo.lastUpdateTime) {
            previous.label
        } else {
            runCatching { applicationInfo.loadLabel(packageManager).toString() }
                .getOrDefault(applicationInfo.packageName)
                .ifBlank { applicationInfo.packageName }
        }
        return InstalledApp(
            label = label,
            packageName = applicationInfo.packageName,
            uid = applicationInfo.uid,
            isSystemApp = isSystem,
            firstInstallTimeMillis = packageInfo.firstInstallTime,
            lastUpdateTimeMillis = packageInfo.lastUpdateTime,
        )
    }

    private fun readDisk(): Pair<String, List<InstalledApp>>? = runCatching {
        if (!cacheFile.exists()) return null
        val root = JSONObject(cacheFile.readText())
        val array = root.getJSONArray("apps")
        val apps = ArrayList<InstalledApp>(array.length())
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            apps += InstalledApp(
                label = o.getString("l"),
                packageName = o.getString("p"),
                uid = o.getInt("u"),
                isSystemApp = o.getBoolean("s"),
                firstInstallTimeMillis = o.optLong("f").takeIf { o.has("f") },
                lastUpdateTimeMillis = o.optLong("t").takeIf { o.has("t") },
            )
        }
        root.getString("locale") to apps
    }.getOrNull()

    private fun writeDisk(localeTag: String, apps: List<InstalledApp>) {
        runCatching {
            val array = JSONArray()
            apps.forEach { app ->
                array.put(
                    JSONObject().apply {
                        put("l", app.label)
                        put("p", app.packageName)
                        put("u", app.uid)
                        put("s", app.isSystemApp)
                        app.firstInstallTimeMillis?.let { put("f", it) }
                        app.lastUpdateTimeMillis?.let { put("t", it) }
                    },
                )
            }
            val tmp = File(cacheFile.parentFile, cacheFile.name + ".tmp")
            tmp.writeText(JSONObject().put("locale", localeTag).put("apps", array).toString())
            tmp.renameTo(cacheFile)
        }
    }

    private fun Int.hasAnyFlag(flags: Int): Boolean = this and flags != 0

    private companion object {
        const val CHUNK_SIZE = 40
    }
}
