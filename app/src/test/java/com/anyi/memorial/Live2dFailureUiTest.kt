package com.anyi.memorial

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class Live2dFailureUiTest {
    @Test
    fun regenerateSuggestionOffersPaidButtonOnlyForPromptJobs() {
        val prompt = live2dFailureUi("background_leak", "regenerate_image", "背景被并入了人物图层。", hasSourceImage = false)
        assertEquals("背景被并入了人物图层。", prompt.message)
        assertEquals("换背景重新生成", prompt.primaryLabel)
        assertEquals("regenerate_image", prompt.primaryHint)
        assertEquals("将再消耗一次图片生成", prompt.primaryNote)
        assertEquals("直接重试", prompt.retryLabel)
        val photo = live2dFailureUi("background_leak", "regenerate_image", null, hasSourceImage = true)
        assertNull(photo.primaryLabel); assertNull(photo.primaryHint)
        assertEquals("请更换图片或修改描述后重新提交", photo.primaryNote)
        assertEquals("背景被并入了人物图层，建议换一张纯色或透明背景的图片", photo.message)
    }

    @Test
    fun legacyFailuresWithoutDiagnosisKeepTheOldWording() {
        val legacy = live2dFailureUi(null, null, "  ", hasSourceImage = false)
        assertEquals("生成失败", legacy.message); assertNull(legacy.primaryLabel); assertNull(legacy.primaryNote)
        assertEquals("重新生成", legacy.retryLabel)
        val newInput = live2dFailureUi("face_not_located", "new_input", null, hasSourceImage = true)
        assertEquals("请更换图片或修改描述后重新提交", newInput.primaryNote)
        assertEquals("没有找到清晰的正面脸部，请换一张正面、无遮挡的图片", newInput.message)
    }
}
