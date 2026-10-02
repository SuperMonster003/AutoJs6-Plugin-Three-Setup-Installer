package io.github.supermonster003.autojs6.plugin.three.setup.installer.policy

import android.content.Context
import java.util.UUID

/** Separate from default options: corrupt policy never becomes an empty, permissive blacklist. */
internal object InstallSafetyPreferences {
    private const val FILE = "installation_policy"
    private const val REVISION = "revision"
    private const val PACKAGES = "packages"
    private const val SHARED_USERS = "shared_users"

    @Synchronized fun read(context: Context): InstallSafetyPolicy = runCatching {
        val saved = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val revision = saved.getString(REVISION, null)
        val packages = saved.getString(PACKAGES, null)
        val sharedUsers = saved.getString(SHARED_USERS, null)
        // Android may turn malformed preferences XML into an empty map. Only an absent file
        // means an unconfigured policy; our explicit reset always writes all three fields.
        val file = java.io.File(context.applicationInfo.dataDir, "shared_prefs/$FILE.xml")
        if (revision == null && packages == null && sharedUsers == null && (file.exists() || java.io.File(file.path + ".bak").exists()))
            InstallSafetyPolicy("unreadable", readable = false)
        else InstallSafetyPolicy.decode(revision, packages, sharedUsers)
    }.getOrElse { InstallSafetyPolicy("unreadable", readable = false) }

    /** Compare before saving so a second settings window cannot overwrite a newer rule list. */
    @Synchronized fun save(context: Context, expected: InstallSafetyPolicy, packages: Set<String>, sharedUsers: Set<String>): Boolean {
        require(packages.size + sharedUsers.size <= InstallSafetyPolicy.MAX_RULES)
        require((packages + sharedUsers).all(InstallSafetyPolicy::validIdentifier))
        val packageJson = InstallSafetyPolicy.encode(packages)
        val sharedJson = InstallSafetyPolicy.encode(sharedUsers)
        require(packageJson.length <= 65_536 && sharedJson.length <= 65_536)
        if (read(context) != expected) return false
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putString(REVISION, UUID.randomUUID().toString())
            .putString(PACKAGES, packageJson)
            .putString(SHARED_USERS, sharedJson)
            .commit()
    }
}
