package com.anyi.memorial

/** What the studio shows for a failed generation job, derived only from server whitelisted fields. */
internal data class Live2dFailureUi(
    val message: String,
    val primaryLabel: String?,
    val primaryHint: String?,
    val primaryNote: String?,
    val retryLabel: String
)

internal fun live2dFailureUi(diagnosisCode: String?, suggestion: String?, summary: String?, hasSourceImage: Boolean): Live2dFailureUi {
    val fallback = when (diagnosisCode) {
        "provider_unavailable" -> "生成服务暂时不可用，请稍后重试"
        "background_leak" -> "背景被并入了人物图层，建议换一张纯色或透明背景的图片"
        "face_not_located" -> "没有找到清晰的正面脸部，请换一张正面、无遮挡的图片"
        "expression_failed" -> "表情素材生成不合格，请重试"
        "rig_unstable" -> "动作检查未通过，建议换一张四肢完整、背景干净的图片"
        "budget_exhausted" -> "自动修复次数已用完，请换一张图片或修改描述后重新提交"
        else -> "生成失败"
    }
    val message = summary?.trim()?.takeIf { it.isNotEmpty() } ?: fallback
    // A paid regeneration only makes sense for prompt jobs; a photo job needs a new photo instead.
    val regenerate = suggestion == "regenerate_image" && !hasSourceImage
    return Live2dFailureUi(
        message = message,
        primaryLabel = if (regenerate) "换背景重新生成" else null,
        primaryHint = if (regenerate) "regenerate_image" else null,
        primaryNote = if (regenerate) "将再消耗一次图片生成" else if (suggestion == "new_input" || (suggestion == "regenerate_image" && hasSourceImage)) "请更换图片或修改描述后重新提交" else null,
        retryLabel = if (regenerate) "直接重试" else "重新生成"
    )
}
