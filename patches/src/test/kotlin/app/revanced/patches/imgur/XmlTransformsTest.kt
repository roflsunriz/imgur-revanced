package app.revanced.patches.imgur

import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class XmlTransformsTest {
    @Test
    fun `ad components and metadata are removed without touching app components`() {
        val document = parse(
            """
            <manifest xmlns:android="$ANDROID_NAMESPACE">
                <application>
                    <activity android:name="com.imgur.mobile.MainActivity" />
                    <activity android:name="com.applovin.adview.AppLovinFullscreenActivity" />
                    <provider android:name="com.mobilefuse.sdk.MobileFuseSdkInitProvider" />
                    <provider android:name="com.facebook.internal.FacebookInitProvider" />
                    <meta-data android:name="com.google.android.gms.ads.APPLICATION_ID" android:value="test" />
                </application>
                <uses-permission android:name="com.google.android.gms.permission.AD_ID" />
            </manifest>
            """.trimIndent(),
        )

        removeAdManifestEntries(document.documentElement)

        val output = document.documentElement.textAndAttributes()
        assertTrue(output.contains("com.imgur.mobile.MainActivity"))
        assertFalse(output.contains("com.applovin"))
        assertFalse(output.contains("com.mobilefuse"))
        assertFalse(output.contains("com.google.android.gms.ads"))
        assertFalse(output.contains("com.facebook.internal.FacebookInitProvider"))
        assertFalse(output.contains("com.google.android.gms.permission.AD_ID"))
    }

    @Test
    fun `facebook automatic events and advertising id collection are disabled`() {
        val document = parse(
            """
            <application xmlns:android="$ANDROID_NAMESPACE">
                <meta-data android:name="com.facebook.sdk.AutoLogAppEventsEnabled" android:value="true" />
            </application>
            """.trimIndent(),
        )

        disableFacebookTracking(document.documentElement)

        val metadata = document.getElementsByTagName("meta-data")
        val values = (0 until metadata.length).associate { index ->
            val element = metadata.item(index) as Element
            element.getAttribute("android:name") to element.getAttribute("android:value")
        }
        assertEquals("false", values["com.facebook.sdk.AdvertiserIDCollectionEnabled"])
        assertEquals("false", values["com.facebook.sdk.AutoInitEnabled"])
        assertEquals("false", values["com.facebook.sdk.AutoLogAppEventsEnabled"])
    }

    @Test
    fun `all existing layout heights are collapsed`() {
        val document = parse(
            """
            <merge xmlns:android="$ANDROID_NAMESPACE">
                <FrameLayout android:layout_width="match_parent" android:layout_height="wrap_content">
                    <View android:layout_width="match_parent" android:layout_height="58dp" />
                </FrameLayout>
            </merge>
            """.trimIndent(),
        )

        setLayoutHeightToZero(document.documentElement)

        val frames = document.getElementsByTagName("FrameLayout")
        val views = document.getElementsByTagName("View")
        assertEquals("0dp", (frames.item(0) as Element).getAttribute("android:layout_height"))
        assertEquals("0dp", (views.item(0) as Element).getAttribute("android:layout_height"))
    }

    private fun parse(xml: String) = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        .parse(ByteArrayInputStream(xml.toByteArray()))

    private fun Element.textAndAttributes(): String = buildString {
        append(tagName)
        for (index in 0 until attributes.length) {
            append(attributes.item(index).nodeValue)
        }
        childNodes.asSequence().filterIsInstance<Element>().forEach { append(it.textAndAttributes()) }
    }
}
