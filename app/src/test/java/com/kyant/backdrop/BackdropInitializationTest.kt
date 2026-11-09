package com.kyant.backdrop

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import org.junit.Assert.assertEquals
import org.junit.Test

class BackdropInitializationTest {
    @Test
    fun `effects wait for measured bounds and wait again after detach`() {
        val scope = object : BackdropEffectScopeImpl() {
            override val shape: Shape = RoundedCornerShape(50)
        }
        var calls = 0
        val effect: BackdropEffectScope.() -> Unit = {
            // Same geometry read that crashed the lens on node attachment.
            assertEquals(64f, size.minDimension)
            calls++
        }
        scope.apply(effect)
        scope.size = Size.Zero
        scope.apply(effect)
        scope.size = Size(300f, 0f)
        scope.apply(effect)
        assertEquals(0, calls)
        scope.size = Size(300f, 64f)
        scope.apply(effect)
        assertEquals(1, calls)
        scope.reset()
        scope.apply(effect)
        assertEquals(1, calls)
        scope.size = Size(400f, 64f)
        scope.apply(effect)
        assertEquals(2, calls)
    }
}
