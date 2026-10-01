package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.app.Instrumentation
import android.os.ParcelFileDescriptor
import android.util.Xml
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

/** Compare every preferred record without running an intent-resolution query that may mutate it. */
internal object NotificationFixtureAudit {
    fun preferred(instrumentation: Instrumentation): String {
        val xml = ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand("dumpsys package preferred-xml --full"))
            .bufferedReader().use { it.readText() }
        val parser = Xml.newPullParser().apply { setInput(StringReader(xml)); nextTag() }
        fun element(): String {
            check(parser.eventType == XmlPullParser.START_TAG)
            val name = parser.name
            val attributes = (0 until parser.attributeCount).associate { parser.getAttributeName(it) to parser.getAttributeValue(it) }.toSortedMap()
            val children = mutableListOf<String>()
            while (true) {
                when (parser.next()) {
                    XmlPullParser.START_TAG -> children += element()
                    XmlPullParser.END_TAG -> break
                    XmlPullParser.TEXT -> check(parser.text.isBlank())
                    XmlPullParser.END_DOCUMENT -> error("Incomplete preferred XML")
                }
            }
            return JsonObject().apply {
                addProperty("name", name)
                add("attributes", JsonObject().apply { attributes.forEach { (key, value) -> addProperty(key, value) } })
                add("children", JsonArray().apply { children.sorted().forEach(::add) })
            }.toString()
        }
        check(parser.name == "preferred-activities")
        return element()
    }
}
