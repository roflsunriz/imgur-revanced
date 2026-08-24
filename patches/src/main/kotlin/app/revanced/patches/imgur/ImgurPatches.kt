package app.revanced.patches.imgur

import app.revanced.patcher.firstMethod
import app.revanced.patcher.firstMethodOrNull
import app.revanced.patcher.extensions.ExternalLabel
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import org.w3c.dom.Element

private const val IMGUR_PACKAGE = "com.imgur.mobile"
private const val EXTENSION = "Lapp/revanced/extension/imgur/ImgurExtension;"

@Suppress("unused")
val imgurResourcesPatch = resourcePatch(
) {
    compatibleWith(IMGUR_PACKAGE)

    apply {
        document("AndroidManifest.xml").use { document ->
            removeAdManifestEntries(document.documentElement)
            val application = document.documentElement.childNodes.asSequence()
                .filterIsInstance<Element>()
                .first { it.tagName == "application" }
            disableFacebookTracking(application)
        }

        document("res/values/dimens.xml").use { document ->
            val dimensions = document.getElementsByTagName("dimen")
            for (index in 0 until dimensions.length) {
                val element = dimensions.item(index) as? Element ?: continue
                if (element.getAttribute("name") in setOf(
                        "sticky_ad_container_height",
                        "sticky_ad_height",
                        "feed_banner_ad_height_250",
                        "insertable_ad_container_margin_top",
                    )
                ) {
                    element.textContent = "0dp"
                }
            }
        }

        get("res/layout/view_sticky_ad.xml").takeIf { it.exists() }?.let {
            document("res/layout/view_sticky_ad.xml").use { document ->
                setLayoutHeightToZero(document.documentElement)
            }
        }

        document("res/xml/settings_pref.xml").use { document ->
            val root = document.documentElement
            val existing = root.childNodes.asSequence().filterIsInstance<Element>().any {
                it.androidAttribute("app:key", APP_NAMESPACE, "key") == "imgur_revanced_settings"
            }
            if (!existing) {
                val preference = document.createElement("Preference").apply {
                    setAttributeNS(APP_NAMESPACE, "app:fragment", "app.revanced.extension.imgur.ImgurSettingsFragment")
                    setAttributeNS(APP_NAMESPACE, "app:iconSpaceReserved", "false")
                    setAttributeNS(APP_NAMESPACE, "app:key", "imgur_revanced_settings")
                    setAttributeNS(APP_NAMESPACE, "app:title", "Imgur ReVanced")
                }
                val signOut = root.childNodes.asSequence().filterIsInstance<Element>().firstOrNull {
                    it.androidAttribute("app:key", APP_NAMESPACE, "key").contains("signout", ignoreCase = true)
                }
                root.insertBefore(preference, signOut)
            }
        }
        addImgurSettingsResources()
    }
}

@Suppress("unused")
val imgurReVancedPatch = bytecodePatch(
    name = "Imgur ReVanced",
    description = "Improve Imgur links and navigation while disabling advertisements.",
) {
    compatibleWith(IMGUR_PACKAGE)
    dependsOn(imgurResourcesPatch)
    extendWith("extensions/imgur.rve")

    apply {
        val applicationOnCreate = firstMethod {
            definingClass == "Lcom/imgur/mobile/ImgurApplication;" &&
                name == "onCreate" &&
                parameterTypes.isEmpty() &&
                returnType == "V"
        }
        applicationOnCreate.addInstructions(
            1,
            "invoke-static {p0}, $EXTENSION->initialize(Landroid/content/Context;)V",
        )

        val modernStartupActivity = "Lcom/imgur/mobile/newpostdetail/GridAndFeedNavActivity;"
        val modernStartupConstructor = firstMethod {
            definingClass == modernStartupActivity &&
                name == "<init>" &&
                parameterTypes.isEmpty() &&
                returnType == "V"
        }
        val startupConstructorImplementation = requireNotNull(modernStartupConstructor.implementation)
        val homeDestinationIndex = startupConstructorImplementation.instructions.indexOfFirst { instruction ->
            if (instruction.opcode != Opcode.IPUT_OBJECT) {
                return@indexOfFirst false
            }
            val reference = (instruction as? ReferenceInstruction)?.reference as? FieldReference
            reference?.definingClass == modernStartupActivity &&
                reference.name == "homeDestination" &&
                reference.type == "Lcom/imgur/mobile/common/navigation/NavDestination;"
        }
        if (homeDestinationIndex >= 0) {
            firstMethod(modernStartupConstructor).addInstructions(
                homeDestinationIndex + 1,
                "invoke-static {p0}, $EXTENSION->configureStartupDestination(Ljava/lang/Object;)V",
            )
        }

        buildList {
            if (homeDestinationIndex < 0) {
                add(modernStartupActivity)
            }
            add("Lcom/imgur/mobile/gallery/feed/GridAndFeedActivity;")
        }.forEach { legacyActivityClass ->
            val legacyOnCreate = firstMethodOrNull {
                definingClass == legacyActivityClass &&
                    name == "onCreate" &&
                    parameterTypes.singleOrNull() == "Landroid/os/Bundle;" &&
                    returnType == "V"
            } ?: return@forEach
            val implementation = requireNotNull(legacyOnCreate.implementation)
            require(implementation.registerCount > 2) {
                "${legacyOnCreate.definingClass} onCreate has no local register for startup redirect"
            }
            val superOnCreateIndex = implementation.instructions.indexOfFirst { instruction ->
                val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
                (instruction.opcode == Opcode.INVOKE_SUPER ||
                    instruction.opcode == Opcode.INVOKE_SUPER_RANGE) &&
                    reference?.name == "onCreate" &&
                    reference.parameterTypes.singleOrNull() == "Landroid/os/Bundle;" &&
                    reference.returnType == "V"
            }
            require(superOnCreateIndex >= 0) {
                "${legacyOnCreate.definingClass} onCreate does not call super.onCreate"
            }
            val continueLegacyStartup = implementation.instructions.elementAt(superOnCreateIndex + 1)
            firstMethod(legacyOnCreate).addInstructionsWithLabels(
                superOnCreateIndex + 1,
                """
                    invoke-static {p0}, $EXTENSION->redirectLegacyStartupToProfile(Landroid/app/Activity;)Z
                    move-result v0
                    if-eqz v0, :imgur_revanced_continue_legacy_startup
                    return-void
                """.trimIndent(),
                ExternalLabel("imgur_revanced_continue_legacy_startup", continueLegacyStartup),
            )
        }

        firstMethodOrNull {
            definingClass == "Lcom/imgur/mobile/profile/PostFilterViewModel;" &&
                name == "<init>" &&
                parameterTypes.isEmpty() &&
                returnType == "V"
        }?.let { constructor ->
            val implementation = requireNotNull(constructor.implementation)
            val returnIndex = implementation.instructions.indexOfLast { it.opcode == Opcode.RETURN_VOID }
            require(returnIndex >= 0) { "PostFilterViewModel constructor has no return instruction" }
            constructor.addInstructions(
                returnIndex,
                """
                    sget-object v0, Lcom/imgur/mobile/profile/PostFilter;->ALL:Lcom/imgur/mobile/profile/PostFilter;
                    invoke-virtual {p0, v0}, Lcom/imgur/mobile/profile/PostFilterViewModel;->setSelectedFilter(Lcom/imgur/mobile/profile/PostFilter;)V
                """.trimIndent(),
            )
        }

        val adsModuleConstructor = firstMethod {
            definingClass == "Lcom/imgur/mobile/di/modules/AdsModule;" &&
                name == "<init>" &&
                returnType == "V"
        }
        val adsConstructorImplementation = requireNotNull(adsModuleConstructor.implementation)
        val objectConstructorIndex = adsConstructorImplementation.instructions.indexOfFirst { instruction ->
            val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
            reference?.definingClass == "Ljava/lang/Object;" && reference.name == "<init>"
        }
        require(objectConstructorIndex >= 0) { "AdsModule does not call Object.<init>" }
        adsModuleConstructor.addInstructions(objectConstructorIndex + 1, "return-void")

        firstMethodOrNull {
            definingClass == "Lcom/safedk/android/internal/DexBridge;" &&
                name == "appClassOnCreateBefore" &&
                returnType == "V"
        }?.addInstructions(0, "return-void")

        firstMethodOrNull {
            definingClass == "Lcom/google/android/gms/ads/identifier/AdvertisingIdClient;" &&
                name == "getAdvertisingIdInfo" &&
                parameterTypes.size == 1 &&
                returnType == "Lcom/google/android/gms/ads/identifier/AdvertisingIdClient\$Info;"
        }?.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return-object v0
            """.trimIndent(),
        )

        firstMethodOrNull {
            definingClass == "Lcom/imgur/mobile/ImgurApplication2;" &&
                name == "initComScore" &&
                parameterTypes.isEmpty() &&
                returnType == "V"
        }?.addInstructions(0, "return-void")

        listOf("init", "identifyUser").forEach { methodName ->
            firstMethodOrNull {
                definingClass == "Lcom/imgur/mobile/engine/ads/banner/ImgurBannerProvider;" &&
                    name == methodName &&
                    returnType == "V"
            }?.addInstructions(0, "return-void")
        }

        classDefs.asSequence()
            .filter { it.type.startsWith("Lcom/imgur/mobile/common/AdsFeatureFlags") }
            .flatMap { it.methods.asSequence() }
            .filter { it.returnType == "Z" && it.implementation != null }
            .toList()
            .forEach { method ->
                firstMethod(method).addInstructions(
                    0,
                    """
                        const/4 v0, 0x0
                        return v0
                    """.trimIndent(),
                )
            }

        classDefs.firstOrNull {
            it.type == "Lai/medialab/medialabads2/banners/MediaLabAdView;"
        }?.methods?.filter { it.implementation != null }?.forEach { method ->
            val mutableMethod = firstMethod(method)
            when {
                method.returnType == "V" && method.name in setOf(
                    "addCustomTargetingValue",
                    "addFriendlyObstruction",
                    "clearCustomTargetingValues",
                    "clearFriendlyObstructions",
                    "destroy",
                    "initialize",
                    "loadAd",
                    "pause",
                    "removeCustomTargetingValue",
                    "removeFriendlyObstruction",
                    "resume",
                ) -> mutableMethod.addInstructions(0, "return-void")

                method.returnType == "Z" && method.name in setOf(
                    "getInitialized",
                    "isLoading",
                    "showPreloadedAd",
                ) -> mutableMethod.addInstructions(
                    0,
                    """
                        const/4 v0, 0x0
                        return v0
                    """.trimIndent(),
                )

                method.returnType == "F" && method.name == "getAdaptiveHeightDp" ->
                    mutableMethod.addInstructions(
                        0,
                        """
                            const/4 v0, 0x0
                            return v0
                        """.trimIndent(),
                    )
            }
        }

        classDefs.firstOrNull {
            it.type == "Lai/medialab/medialabads2/MediaLabAds;"
        }?.methods?.filter { it.implementation != null }?.forEach { method ->
            val mutableMethod = firstMethod(method)
            when {
                method.returnType == "Z" -> mutableMethod.addInstructions(
                    0,
                    """
                        const/4 v0, 0x0
                        return v0
                    """.trimIndent(),
                )

                method.returnType == "V" && method.name == "initialize" ->
                    mutableMethod.addInstructions(0, "return-void")

                method.returnType == "Lai/medialab/medialabads2/storage/AdFreeStatus;" &&
                    method.name == "getGetAdFreeStatus" -> mutableMethod.addInstructions(
                        0,
                        """
                            new-instance v0, Lai/medialab/medialabads2/storage/AdFreeStatus;
                            const-wide/16 v1, 0x0
                            invoke-direct {v0, v1, v2, v1, v2}, Lai/medialab/medialabads2/storage/AdFreeStatus;-><init>(JJ)V
                            return-object v0
                        """.trimIndent(),
                    )
            }
        }

        val feedImageLongClick = firstMethod {
            definingClass == "Lcom/imgur/mobile/feed/userfeed/BaseFeedItemImageSubPresenter;" &&
                name == "onImageLongClick" &&
                parameterTypes.size == 2 &&
                parameterTypes[0] == "Lcom/imgur/mobile/gallery/inside/models/ImageViewModel;" &&
                parameterTypes[1] == "I" &&
                returnType == "V"
        }
        feedImageLongClick.addInstructions(
            0,
            """
                invoke-virtual {p1}, Lcom/imgur/mobile/gallery/inside/models/ImageViewModel;->getImageItem()Lcom/imgur/mobile/common/model/ImageItem;
                move-result-object v0
                invoke-virtual {v0}, Lcom/imgur/mobile/common/model/ImageItem;->getLink()Ljava/lang/String;
                move-result-object v0
                invoke-static {v0}, $EXTENSION->copyFeedImageLink(Ljava/lang/String;)V
                return-void
            """.trimIndent(),
        )

        firstMethodOrNull {
            definingClass == "Lcom/imgur/mobile/profile/ProfilePostsAdapter\$ProfilePostViewHolder;" &&
                name == "bind" &&
                parameterTypes.singleOrNull() == "Lcom/imgur/mobile/search/PostViewModel;" &&
                returnType == "V"
        }?.let { bindMethod ->
            val implementation = requireNotNull(bindMethod.implementation)
            val returnIndex = implementation.instructions.indexOfLast { it.opcode == Opcode.RETURN_VOID }
            require(returnIndex >= 0) { "Profile post bind method has no return instruction" }
            bindMethod.addInstructions(
                returnIndex,
                """
                    iget-object v0, p0, Lcom/imgur/mobile/profile/ProfilePostsAdapter${'$'}ProfilePostViewHolder;->itemView:Lcom/imgur/mobile/common/ui/view/grid/BaseGridItemView;
                    invoke-static {v0, p1}, $EXTENSION->bindProfilePostLongPress(Landroid/view/View;Ljava/lang/Object;)V
                """.trimIndent(),
            )
        }

        val shareDirectImageLink = firstMethod {
            definingClass == "Lcom/imgur/mobile/common/ui/share/ShareUtils\$Companion;" &&
                name == "shareDirectImageLink" &&
                parameterTypes.firstOrNull() == "Landroid/content/Context;" &&
                parameterTypes.count { it == "Ljava/lang/String;" } >= 5 &&
                returnType == "V"
        }
        val copyUrlParameter = if (shareDirectImageLink.parameterTypes.size >= 12) 6 else 5
        val directUrlParameter = copyUrlParameter + 1
        shareDirectImageLink.addInstructions(
            0,
            """
                move-object/from16 v0, p3
                move-object/from16 v1, p$directUrlParameter
                invoke-static {v0, v1}, $EXTENSION->selectShareUrl(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
                move-result-object v0
                move-object/from16 p3, v0
                move-object/from16 p$copyUrlParameter, v0
            """.trimIndent(),
        )

        val adjustBottomBarItems = firstMethod {
            definingClass == "Lcom/imgur/mobile/common/ui/view/BottomBarLayout;" &&
                name == "adjustItems" &&
                parameterTypes.isEmpty() &&
                returnType == "V"
        }
        adjustBottomBarItems.addInstructions(
            0,
            "invoke-static {p0}, $EXTENSION->applyNavigationVisibility(Landroid/view/ViewGroup;)V",
        )
    }
}
