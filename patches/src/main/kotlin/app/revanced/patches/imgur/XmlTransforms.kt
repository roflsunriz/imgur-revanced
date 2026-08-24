package app.revanced.patches.imgur

import org.w3c.dom.Element
import org.w3c.dom.Node

internal const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
internal const val APP_NAMESPACE = "http://schemas.android.com/apk/res-auto"

private val adComponentPrefixes = listOf(
    "ai.medialab.medialabads2.",
    "com.amazon.device.ads.",
    "com.applovin.",
    "com.facebook.ads.",
    "com.google.android.gms.ads.",
    "com.mbridge.",
    "com.mobilefuse.sdk.",
    "com.moloco.sdk.",
    "com.mopub.",
    "com.smartadserver.",
    "com.safedk.",
)

private val adComponentNames = setOf(
    "com.facebook.internal.FacebookInitProvider",
)

private val adPermissionNames = setOf(
    "android.permission.ACCESS_ADSERVICES_AD_ID",
    "android.permission.ACCESS_ADSERVICES_ATTRIBUTION",
    "android.permission.ACCESS_ADSERVICES_TOPICS",
    "com.google.android.gms.permission.AD_ID",
)

internal fun removeAdManifestEntries(root: Element) {
    val children = root.childNodes.asSequence().toList()
    children.forEach { child ->
        val element = child as? Element ?: return@forEach
        val name = element.androidAttribute("android:name", ANDROID_NAMESPACE, "name")
        val shouldRemove = when (element.tagName) {
            "activity", "activity-alias", "provider", "receiver", "service" ->
                name in adComponentNames || adComponentPrefixes.any(name::startsWith)

            "uses-permission" -> name in adPermissionNames

            "meta-data" -> {
                val lowerName = name.lowercase()
                adComponentPrefixes.any(name::startsWith) || listOf(
                    "admob",
                    "ads.application_id",
                    "applovin",
                    "mobilefuse",
                    "moloco",
                ).any(lowerName::contains)
            }

            else -> false
        }
        if (shouldRemove) {
            root.removeChild(element)
        } else {
            removeAdManifestEntries(element)
        }
    }
}

internal fun disableFacebookTracking(application: Element) {
    listOf(
        "com.facebook.sdk.AdvertiserIDCollectionEnabled",
        "com.facebook.sdk.AutoInitEnabled",
        "com.facebook.sdk.AutoLogAppEventsEnabled",
    ).forEach { name ->
        val existing = application.childNodes.asSequence().filterIsInstance<Element>().firstOrNull {
            it.tagName == "meta-data" &&
                it.androidAttribute("android:name", ANDROID_NAMESPACE, "name") == name
        }
        val metadata = existing ?: application.ownerDocument.createElement("meta-data").also(application::appendChild)
        metadata.setAttribute("android:name", name)
        metadata.setAttribute("android:value", "false")
    }
}

internal fun setLayoutHeightToZero(element: Element) {
    if (element.hasAttribute("android:layout_height")) {
        element.setAttribute("android:layout_height", "0dp")
    } else if (element.hasAttributeNS(ANDROID_NAMESPACE, "layout_height")) {
        element.setAttributeNS(ANDROID_NAMESPACE, "android:layout_height", "0dp")
    }
    element.childNodes.asSequence().filterIsInstance<Element>().forEach(::setLayoutHeightToZero)
}

internal fun Element.androidAttribute(
    prefixedName: String,
    namespace: String,
    localName: String,
): String = getAttribute(prefixedName).ifEmpty { getAttributeNS(namespace, localName) }

internal fun org.w3c.dom.NodeList.asSequence(): Sequence<Node> = sequence {
    for (index in 0 until length) {
        yield(item(index))
    }
}
