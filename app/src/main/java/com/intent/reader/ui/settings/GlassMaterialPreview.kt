package com.intent.reader.ui.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intent.reader.R
import com.intent.reader.ui.components.navigationSurface
import com.intent.reader.ui.theme.IntentTheme
import com.intent.reader.ui.theme.withCjkSpacing
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/** The preview records only its artwork, so the glass never samples itself. */
@Composable
internal fun GlassMaterialPreview(transparency: Float, adaptive: Boolean, motto: String) {
    val supported = Build.VERSION.SDK_INT >= 33
    val foreground = if (IntentTheme.colors.isDark) Color.White else Color(0xFF17211D)
    val backdrop = if (supported) rememberLayerBackdrop() else null
    Box(Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(28.dp))) {
        Column(
            Modifier.fillMaxSize()
                .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                .background(Color(0xFFE8E8E3)),
        ) {
            // The artwork and copy scroll together; the glass pill row stays pinned
            // to the bottom so the blur can be judged against moving text.
            Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
                Box(
                    Modifier.fillMaxWidth().height(170.dp)
                        .background(Brush.linearGradient(listOf(Color(0xFFB5D8EF), Color(0xFF739A98), Color(0xFF31534E)))),
                ) {
                    Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(55.dp)
                        .background(Brush.horizontalGradient(listOf(Color(0xFF304D39), Color(0xFF78915B), Color(0xFF254B44)))))
                    Box(Modifier.align(Alignment.TopEnd).padding(top = 24.dp, end = 28.dp)
                        .size(66.dp).clip(RoundedCornerShape(50)).background(Color(0xBBF9E9BC)))
                }
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Text(stringResource(R.string.glass_preview_title), color = Color(0xFF141716), fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(stringResource(R.string.glass_preview_subtitle), color = Color(0xFF58615D), fontSize = 14.sp, maxLines = 2)
                    Text(
                        motto.withCjkSpacing(),
                        color = Color(0xFF58615D),
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    Text(
                        stringResource(R.string.glass_preview_body).withCjkSpacing(),
                        color = Color(0xFF3F4A46),
                        fontSize = 13.sp,
                        lineHeight = 21.sp,
                        modifier = Modifier.padding(top = 14.dp),
                    )
                    // Let the tail of the copy scroll clear of the pinned glass bar.
                    Spacer(Modifier.height(96.dp))
                }
            }
        }
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(52.dp).navigationSurface(supported, backdrop, transparency > 0.72f, adaptive, true, 0f, transparency = transparency), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Tune, contentDescription = null, tint = foreground)
            }
            Row(Modifier.weight(1f).height(52.dp).navigationSurface(supported, backdrop, transparency > 0.72f, adaptive, true, 0f, transparency = transparency).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = foreground)
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.glass_preview_search), color = foreground, fontSize = 16.sp)
            }
        }
    }
}
