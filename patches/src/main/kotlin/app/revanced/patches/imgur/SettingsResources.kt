package app.revanced.patches.imgur

import app.revanced.patcher.patch.ResourcePatchContext

private data class SettingsTranslation(
    val directLinksTitle: String,
    val directLinksSummary: String,
    val hideDiscoverTitle: String,
    val hideSearchTitle: String,
    val hideNotificationsTitle: String,
    val hideTabsSummary: String,
    val linkCopied: String,
)

private val translations = mapOf(
    "values" to SettingsTranslation(
        "Use direct image links",
        "Use the direct image link instead of the album link when copying and sharing text",
        "Hide Discover",
        "Hide Search",
        "Hide Notifications",
        "Space the remaining tabs evenly",
        "Direct link copied",
    ),
    "values-ja" to SettingsTranslation(
        "画像の直リンクを使用",
        "コピーとテキスト共有でアルバムリンクの代わりに画像の直リンクを使用します",
        "Discoverを非表示",
        "Searchを非表示",
        "Notificationsを非表示",
        "非表示タブの空間を残さず、表示中のタブを均等に配置します",
        "直リンクをコピーしました",
    ),
    "values-zh-rCN" to SettingsTranslation(
        "使用图片直链", "复制和文本分享时使用图片直链，而不是相册链接",
        "隐藏 Discover", "隐藏 Search", "隐藏 Notifications", "均匀排列仍显示的标签页", "已复制直链",
    ),
    "values-hi" to SettingsTranslation(
        "सीधा चित्र लिंक उपयोग करें", "कॉपी और टेक्स्ट साझा करने में एल्बम लिंक की जगह सीधा लिंक उपयोग करें",
        "Discover छिपाएँ", "Search छिपाएँ", "Notifications छिपाएँ", "दिखने वाले टैब समान दूरी पर रखें", "सीधा लिंक कॉपी किया गया",
    ),
    "values-es" to SettingsTranslation(
        "Usar enlaces directos", "Usa el enlace directo de la imagen al copiar y compartir texto",
        "Ocultar Discover", "Ocultar Search", "Ocultar Notifications", "Distribuye uniformemente las pestañas visibles", "Enlace directo copiado",
    ),
    "values-fr" to SettingsTranslation(
        "Utiliser les liens directs", "Utilise le lien direct de l’image lors de la copie et du partage de texte",
        "Masquer Discover", "Masquer Search", "Masquer Notifications", "Répartit uniformément les onglets visibles", "Lien direct copié",
    ),
    "values-ar" to SettingsTranslation(
        "استخدام روابط الصور المباشرة", "استخدام رابط الصورة المباشر عند النسخ والمشاركة النصية",
        "إخفاء Discover", "إخفاء Search", "إخفاء Notifications", "توزيع علامات التبويب الظاهرة بالتساوي", "تم نسخ الرابط المباشر",
    ),
    "values-pt-rBR" to SettingsTranslation(
        "Usar links diretos", "Usa o link direto da imagem ao copiar e compartilhar texto",
        "Ocultar Discover", "Ocultar Search", "Ocultar Notifications", "Distribui igualmente as abas visíveis", "Link direto copiado",
    ),
    "values-bn" to SettingsTranslation(
        "ছবির সরাসরি লিংক ব্যবহার করুন", "কপি ও টেক্সট শেয়ারে অ্যালবাম লিংকের বদলে সরাসরি লিংক ব্যবহার করুন",
        "Discover লুকান", "Search লুকান", "Notifications লুকান", "দৃশ্যমান ট্যাবগুলো সমানভাবে সাজান", "সরাসরি লিংক কপি হয়েছে",
    ),
    "values-ru" to SettingsTranslation(
        "Использовать прямые ссылки", "Использует прямую ссылку на изображение при копировании и отправке текста",
        "Скрыть Discover", "Скрыть Search", "Скрыть Notifications", "Равномерно размещает видимые вкладки", "Прямая ссылка скопирована",
    ),
    "values-ur" to SettingsTranslation(
        "تصویر کا براہ راست لنک استعمال کریں", "کاپی اور متن شیئر کرتے وقت البم کے بجائے براہ راست لنک استعمال کریں",
        "Discover چھپائیں", "Search چھپائیں", "Notifications چھپائیں", "نظر آنے والے ٹیب برابر فاصلے پر رکھیں", "براہ راست لنک کاپی ہو گیا",
    ),
)

internal fun ResourcePatchContext.addImgurSettingsResources() {
    get("res/xml/imgur_revanced_preferences.xml").writeText(
        """
            <?xml version="1.0" encoding="utf-8"?>
            <PreferenceScreen xmlns:app="http://schemas.android.com/apk/res-auto">
                <SwitchPreferenceCompat
                    app:defaultValue="true"
                    app:iconSpaceReserved="false"
                    app:key="direct_links"
                    app:singleLineTitle="false"
                    app:summary="@string/imgur_revanced_direct_links_summary"
                    app:title="@string/imgur_revanced_direct_links_title" />
                <SwitchPreferenceCompat
                    app:defaultValue="true"
                    app:iconSpaceReserved="false"
                    app:key="hide_discover"
                    app:singleLineTitle="false"
                    app:summary="@string/imgur_revanced_hide_tabs_summary"
                    app:title="@string/imgur_revanced_hide_discover_title" />
                <SwitchPreferenceCompat
                    app:defaultValue="true"
                    app:iconSpaceReserved="false"
                    app:key="hide_search"
                    app:singleLineTitle="false"
                    app:summary="@string/imgur_revanced_hide_tabs_summary"
                    app:title="@string/imgur_revanced_hide_search_title" />
                <SwitchPreferenceCompat
                    app:defaultValue="true"
                    app:iconSpaceReserved="false"
                    app:key="hide_notifications"
                    app:singleLineTitle="false"
                    app:summary="@string/imgur_revanced_hide_tabs_summary"
                    app:title="@string/imgur_revanced_hide_notifications_title" />
            </PreferenceScreen>
        """.trimIndent(),
    )

    translations.forEach { (directory, strings) ->
        val resourceDirectory = get("res/$directory").apply { mkdirs() }
        resourceDirectory.resolve("imgur_revanced_strings.xml").writeText(
            """
                <?xml version="1.0" encoding="utf-8"?>
                <resources>
                    <string name="imgur_revanced_direct_links_title">${strings.directLinksTitle.xmlEscape()}</string>
                    <string name="imgur_revanced_direct_links_summary">${strings.directLinksSummary.xmlEscape()}</string>
                    <string name="imgur_revanced_hide_discover_title">${strings.hideDiscoverTitle.xmlEscape()}</string>
                    <string name="imgur_revanced_hide_search_title">${strings.hideSearchTitle.xmlEscape()}</string>
                    <string name="imgur_revanced_hide_notifications_title">${strings.hideNotificationsTitle.xmlEscape()}</string>
                    <string name="imgur_revanced_hide_tabs_summary">${strings.hideTabsSummary.xmlEscape()}</string>
                    <string name="imgur_revanced_link_copied">${strings.linkCopied.xmlEscape()}</string>
                </resources>
            """.trimIndent(),
        )
    }
}

private fun String.xmlEscape() = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
