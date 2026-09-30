package app.revanced.patches.imgur

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION = "Lapp/revanced/extension/imgur/ImgurExtension;"

internal fun rewriteImageLinkReads(method: MutableMethod): Int {
    val instructions = requireNotNull(method.implementation).instructions.toList()
    var replacements = 0
    instructions.forEachIndexed { index, instruction ->
        val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
        if (reference?.definingClass == "Lcom/imgur/mobile/common/model/ImageItem;" &&
            reference.name == "getLink" && reference.parameterTypes.isEmpty() &&
            reference.returnType == "Ljava/lang/String;"
        ) {
            val invoke = requireNotNull(instruction as? Instruction35c)
            require(instruction.opcode == Opcode.INVOKE_VIRTUAL && invoke.registerCount == 1) {
                "Unsupported image URL invocation in ${method.definingClass}->${method.name}"
            }
            method.replaceInstruction(
                index,
                "invoke-static {v${invoke.registerC}}, $EXTENSION->imageDirectUrl(Ljava/lang/Object;)Ljava/lang/String;",
            )
            replacements++
        }
    }
    return replacements
}

internal fun hookPostPermalink(method: MutableMethod) {
    val instructions = requireNotNull(method.implementation).instructions.toList()
    val urlIndex = instructions.indexOfFirst {
        val reference = (it as? ReferenceInstruction)?.reference as? MethodReference
        ((reference?.definingClass == "Lcom/imgur/mobile/util/ImgurUrlUtils;" &&
            reference.name == "getUrlFromId") ||
            (reference?.definingClass == "Lcom/imgur/mobile/util/CommentUtils;" &&
                reference.name == "buildImgurLink")) && reference?.returnType == "Ljava/lang/String;"
    }
    require(urlIndex >= 0 && instructions[urlIndex + 1].opcode == Opcode.MOVE_RESULT_OBJECT) {
        "Post permalink does not expose the stock URL"
    }
    val urlRegister = (instructions[urlIndex + 1] as OneRegisterInstruction).registerA
    method.addInstructions(
        urlIndex + 2,
        """
            invoke-static {p0, v$urlRegister}, $EXTENSION->selectPermalinkUrl(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/String;
            move-result-object v$urlRegister
        """.trimIndent(),
    )
}
