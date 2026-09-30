package app.revanced.patches.imgur

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod
import app.revanced.patcher.extensions.addInstructions
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LinkHooksTest {
    @Test
    fun `image read rewrite preserves result register and unrelated URL reads`() {
        val method = method("getMediaLink", 3).apply {
            addInstructions(
                """
                    invoke-virtual {v1}, Lcom/imgur/mobile/common/model/ImageItem;->getLink()Ljava/lang/String;
                    move-result-object v0
                    invoke-virtual {v1}, Lcom/imgur/mobile/common/model/ImageItem;->getShareLink()Ljava/lang/String;
                    move-result-object v1
                    return-object v0
                """.trimIndent(),
            )
        }
        assertEquals(1, rewriteImageLinkReads(method))
        val instructions = method.implementation!!.instructions.toList()
        assertEquals(Opcode.INVOKE_STATIC, instructions[0].opcode)
        assertEquals(1, (instructions[0] as Instruction35c).registerC)
        assertEquals("imageDirectUrl", reference(instructions[0]).name)
        assertEquals(Opcode.MOVE_RESULT_OBJECT, instructions[1].opcode)
        assertEquals("getShareLink", reference(instructions[2]).name)
        assertEquals(3, method.implementation!!.registerCount)
    }

    @Test
    fun `permalink hook selects from the bound post without changing clipboard helper`() {
        val method = method("copyLink", 6).apply {
            addInstructions(
                """
                    invoke-static {v0, v1, v2}, Lcom/imgur/mobile/util/ImgurUrlUtils;->getUrlFromId(Ljava/lang/String;ZZ)Ljava/lang/String;
                    move-result-object v0
                    invoke-static {v1, v0, v2}, Lcom/imgur/mobile/common/kotlin/ContextExtensionsKt;->copyToClipboard(Landroid/content/Context;Ljava/lang/String;Z)V
                    return-void
                """.trimIndent(),
            )
        }
        hookPostPermalink(method)
        val instructions = method.implementation!!.instructions.toList()
        val selection = instructions[2] as Instruction35c
        assertEquals("selectPermalinkUrl", reference(selection).name)
        assertEquals(5, selection.registerC) // p0, the holder, is never overwritten.
        assertEquals(0, selection.registerD)
        assertEquals(Opcode.MOVE_RESULT_OBJECT, instructions[3].opcode)
        assertEquals("copyToClipboard", reference(instructions[4]).name)
    }

    @Test
    fun `changed permalink structure fails instead of silently skipping settings`() {
        assertFailsWith<IllegalArgumentException> { hookPostPermalink(method("copyLink", 2)) }
    }

    @Test
    fun `legacy permalink keeps its post URL builder and clipboard service`() {
        val method = method("copyLink", 6).apply {
            addInstructions(
                """
                    invoke-static {v0, v1, v2, v3}, Lcom/imgur/mobile/util/CommentUtils;->buildImgurLink(Ljava/lang/String;ZZZ)Ljava/lang/String;
                    move-result-object v4
                    invoke-static {v0, v4}, Lcom/imgur/mobile/common/ui/clipboard/ClipboardHelperService;->sendCopyTextAndShowToastIntent(Landroid/content/Context;Ljava/lang/String;)V
                    return-void
                """.trimIndent(),
            )
        }
        hookPostPermalink(method)
        val instructions = method.implementation!!.instructions.toList()
        assertEquals("buildImgurLink", reference(instructions[0]).name)
        val selection = instructions[2] as Instruction35c
        assertEquals(5, selection.registerC)
        assertEquals(4, selection.registerD)
        assertEquals("sendCopyTextAndShowToastIntent", reference(instructions[4]).name)
    }

    private fun method(name: String, registers: Int) = MutableMethod(
        ImmutableMethod(
            "Ltest/Holder;", name, emptyList(), "V", 1, emptySet(), emptySet(),
            ImmutableMethodImplementation(registers, emptyList(), emptyList(), emptyList()),
        ),
    )

    private fun reference(instruction: com.android.tools.smali.dexlib2.iface.instruction.Instruction) =
        (instruction as ReferenceInstruction).reference as MethodReference
}
