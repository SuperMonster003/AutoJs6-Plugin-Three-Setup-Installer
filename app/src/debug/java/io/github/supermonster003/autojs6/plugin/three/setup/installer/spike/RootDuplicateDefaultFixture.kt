package io.github.supermonster003.autojs6.plugin.three.setup.installer.spike

import android.content.ComponentName
import android.content.IntentFilter
import android.os.Build
import android.os.Process
import android.system.Os
import android.util.Xml
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.RootPersistentDefaultProtocol
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.RootSystemDefaultMain
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.hidden.HiddenApiAccess
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.hidden.PackageManagerHidden
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.StringReader

/**
 * Only a supervised Debug test may use this fixed fixture. It requires exactly the four known
 * plugin policies and adds one duplicate of each, mirroring repeated platform/DPM add calls.
 * It cannot select another package, component, user, filter or duplicate an already duplicated set.
 */
object RootDuplicateDefaultFixture {
    @JvmStatic fun main(args: Array<String>) {
        check(args.size == 1)
        val token = args.single().also(RootPersistentDefaultProtocol::token)
        check(Process.myUid() == 1000 && Os.getgid() == 1000)
        check(Build.VERSION.SDK_INT == 24 && Build.SUPPORTED_ABIS.first() == "x86")
        val manager = PackageManagerHidden()
        val before = keys()
        check(before.size == 4 && before.toSet() == RootPersistentDefaultProtocol.keys) { "Only the test's exact four-policy baseline may be duplicated" }
        val component = ComponentName(RootPersistentDefaultProtocol.PACKAGE, "${RootPersistentDefaultProtocol.PACKAGE}.ui.ExternalInstallActivity")
        for (action in PackageManagerHidden.INSTALL_ACTIONS) for (scheme in listOf("content", "file")) {
            manager.persistentPreferred(PackageManagerHidden.filter(action, scheme), component, 0)
        }
        val after = keys()
        check(after.size == 8 && after.groupingBy { it }.eachCount().values.all { it == 2 }) { "The platform did not retain the expected duplicate fixture" }
        println("ROOT_DUPLICATE_FIXTURE_OK:$token:8")
    }

    private fun keys(): List<String> {
        val remote = HiddenApiAccess.service("package", "android.content.pm.IPackageManager")
        HiddenApiAccess.invoke(remote, HiddenApiAccess.method("android.content.pm.IPackageManager", "flushPackageRestrictionsAsUser", Integer.TYPE), 0)
        val source = File("/data/system/users/0/package-restrictions.xml")
        check(source.length() in 1L..4L * 1024 * 1024 && !File(source.path + ".bak").exists())
        val text = source.readText()
        check(!text.contains("<!DOCTYPE", true))
        val parser = Xml.newPullParser().apply { setInput(StringReader(text)) }
        val result = arrayListOf<String>()
        var section = false
        var component: ComponentName? = null
        var filter: IntentFilter? = null
        var sections = 0
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG) {
                if (parser.depth == 2 && parser.name == "persistent-preferred-activities") { section = true; sections++ }
                else if (section && parser.depth == 3 && parser.name == "item") {
                    component = requireNotNull(ComponentName.unflattenFromString(parser.getAttributeValue(null, "name")))
                    filter = null
                } else if (section && parser.depth == 4 && parser.name == "filter") {
                    check(filter == null)
                    filter = IntentFilter().apply { readFromXml(parser) }
                }
            } else if (parser.eventType == XmlPullParser.END_TAG) {
                if (section && parser.depth == 3 && parser.name == "item") {
                    result += requireNotNull(RootSystemDefaultMain.knownPolicyKey(requireNotNull(component), requireNotNull(filter))) {
                        "An unrelated or unknown persistent policy is protected"
                    }
                } else if (parser.depth == 2 && parser.name == "persistent-preferred-activities") section = false
            }
        }
        check(sections == 1)
        return result
    }
}
