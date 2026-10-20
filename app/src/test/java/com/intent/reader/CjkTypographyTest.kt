package com.intent.reader

import com.intent.reader.ui.theme.CjkTypography
import org.junit.Assert.assertEquals
import org.junit.Test

/** CJK typesetting tests (doc: boundary spaces, idempotent). */
class CjkTypographyTest {

    @Test
    fun `inserts space between han and latin`() {
        assertEquals("使用 Kotlin 开发", CjkTypography.apply("使用Kotlin开发"))
    }

    @Test
    fun `inserts space between latin and han`() {
        assertEquals("这是 Android 平台", CjkTypography.apply("这是Android平台"))
    }

    @Test
    fun `inserts space between han and digits`() {
        assertEquals("共 3 篇文章", CjkTypography.apply("共3篇文章"))
    }

    @Test
    fun `inserts space between digits and han`() {
        assertEquals("阅读 10 分钟", CjkTypography.apply("阅读10分钟"))
    }

    @Test
    fun `kana boundary gets space`() {
        assertEquals("これは Android です", CjkTypography.apply("これはAndroidです"))
    }

    @Test
    fun `hangul boundary gets space`() {
        assertEquals("이것은 Android 앱", CjkTypography.apply("이것은Android앱"))
    }

    @Test
    fun `pure latin unchanged`() {
        val t = "The quick brown fox jumps over the lazy dog 123"
        assertEquals(t, CjkTypography.apply(t))
    }

    @Test
    fun `pure cjk unchanged`() {
        val t = "设计的本质是秩序而非装饰"
        assertEquals(t, CjkTypography.apply(t))
    }

    @Test
    fun `idempotent - second pass changes nothing`() {
        val samples = listOf(
            "使用Kotlin开发",
            "阅读10分钟的好文章",
            "これはAndroidです",
            "이것은Android앱이다",
            "版本 v2 发布于 2026 年",
        )
        samples.forEach { s ->
            val once = CjkTypography.apply(s)
            assertEquals(once, CjkTypography.apply(once))
        }
    }

    @Test
    fun `full width punctuation untouched`() {
        val t = "今天，天气很好。"
        assertEquals(t, CjkTypography.apply(t))
    }

    @Test
    fun `percent and units untouched`() {
        assertEquals("增长 10%", CjkTypography.apply("增长10%"))
        assertEquals("100%中文", CjkTypography.apply("100%中文"))
    }

    @Test
    fun `empty string stays empty`() {
        assertEquals("", CjkTypography.apply(""))
    }

    @Test
    fun `multiple boundaries in one string`() {
        assertEquals(
            "在 2026 年 10 月发布 Android16",
            CjkTypography.apply("在2026年10月发布Android16"),
        )
    }
}
