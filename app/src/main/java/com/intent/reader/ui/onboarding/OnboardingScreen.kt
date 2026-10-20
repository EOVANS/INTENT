package com.intent.reader.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.intent.reader.R
import com.intent.reader.ui.theme.IntentDimens
import com.intent.reader.ui.theme.IntentTheme
import com.intent.reader.ui.theme.withCjkSpacing
import kotlinx.coroutines.launch

/**
 * Onboarding: 4 pages (doc 4.1.1) — brand / quote / feed / sources+name+permission+start.
 */
@Composable
fun OnboardingScreen(
    onFinish: (userName: String) -> Unit,
) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val pagerState = rememberPagerState(pageCount = { 4 })
    var userName by remember { mutableStateOf("") }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result irrelevant; user can start regardless */ }

    fun requestNotif() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(c.bg)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            when (page) {
                0 -> BrandPage()
                1 -> FeaturePage(
                    indexLabel = "01",
                    title = stringResource(R.string.onboarding_quote_title),
                    desc = stringResource(R.string.onboarding_quote_desc),
                    note = stringResource(R.string.onboarding_quote_note),
                )
                2 -> FeaturePage(
                    indexLabel = "02",
                    title = stringResource(R.string.onboarding_feed_title),
                    desc = stringResource(R.string.onboarding_feed_desc),
                    note = null,
                )
                else -> SetupPage(
                    userName = userName,
                    onNameChange = { userName = it },
                    onRequestNotif = { requestNotif() },
                )
            }
        }

        // Top bar: brand mark + skip
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = IntentDimens.pageHMarginPhone.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "INTENT",
                style = t.meta,
                fontWeight = FontWeight.SemiBold,
                color = c.text,
                letterSpacing = 3.sp,
            )
            Text(
                text = stringResource(R.string.onboarding_skip),
                style = t.button,
                color = c.textSecondary,
                modifier = Modifier
                    .clickable {
                        scope.launch { pagerState.scrollToPage(3) }
                    }
                    .padding(8.dp),
            )
        }

        // Bottom: progress dots + next / start
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = IntentDimens.pageHMarginPhone.dp, vertical = 24.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 20.dp),
            ) {
                repeat(4) { i ->
                    val active = pagerState.currentPage == i
                    Box(
                        Modifier
                            .width(if (active) 28.dp else 8.dp)
                            .height(8.dp)
                            .background(
                                if (active) c.text else c.divider,
                                RoundedCornerShape(4.dp),
                            )
                    )
                }
            }
            val last = pagerState.currentPage == 3
            Button(
                onClick = {
                    if (last) {
                        onFinish(userName.trim())
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = c.text,
                    contentColor = c.bg,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text(
                    text = if (last) stringResource(R.string.onboarding_start)
                    else stringResource(R.string.onboarding_next),
                    style = t.button,
                )
            }
        }
    }
}

/** Page 0: brand — huge serif INTENT + slogan. */
@Composable
private fun BrandPage() {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = IntentDimens.pageHMarginPhone.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "INTENT",
            fontFamily = com.intent.reader.ui.theme.IntentFonts.serifHeader,
            fontWeight = FontWeight.Bold,
            fontSize = if (LocalConfiguration.current.screenWidthDp >= 600) 96.sp else 56.sp,
            color = c.text,
            letterSpacing = 2.sp,
        )
        Spacer(Modifier.height(16.dp))
        Box(
            Modifier
                .width(48.dp)
                .height(3.dp)
                .background(c.accent)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.onboarding_slogan).withCjkSpacing(),
            style = t.body,
            color = c.textSecondary,
        )
        Text(
            text = stringResource(R.string.onboarding_slogan_zh).withCjkSpacing(),
            style = t.body,
            color = c.textSecondary,
        )
    }
}

/** Pages 1-2: numbered feature intro. */
@Composable
private fun FeaturePage(indexLabel: String, title: String, desc: String, note: String?) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = IntentDimens.pageHMarginPhone.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = indexLabel,
            style = t.pageTitle,
            color = c.accent,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = title.withCjkSpacing(),
            style = t.pageTitle,
            color = c.text,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = desc.withCjkSpacing(),
            style = t.body,
            color = c.textSecondary,
        )
        if (note != null) {
            Spacer(Modifier.height(20.dp))
            Text(
                text = note.withCjkSpacing(),
                style = t.meta,
                color = c.textTertiary,
            )
        }
    }
}

/** Page 3: sources pitch + username + notification permission. */
@Composable
private fun SetupPage(
    userName: String,
    onNameChange: (String) -> Unit,
    onRequestNotif: () -> Unit,
) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = IntentDimens.pageHMarginPhone.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "03",
            style = t.pageTitle,
            color = c.accent,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.onboarding_sources_title).withCjkSpacing(),
            style = t.pageTitle,
            color = c.text,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.onboarding_sources_desc).withCjkSpacing(),
            style = t.body,
            color = c.textSecondary,
        )
        Spacer(Modifier.height(32.dp))
        TextField(
            value = userName,
            onValueChange = onNameChange,
            placeholder = {
                Text(stringResource(R.string.onboarding_name_hint), color = c.textTertiary)
            },
            singleLine = true,
            shape = RoundedCornerShape(IntentDimens.radiusInput.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = c.surface,
                unfocusedContainerColor = c.surface,
                focusedIndicatorColor = c.text,
                unfocusedIndicatorColor = c.divider,
                cursorColor = c.text,
                focusedTextColor = c.text,
                unfocusedTextColor = c.text,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.onboarding_notify_btn),
            style = t.button,
            color = c.textSecondary,
            modifier = Modifier
                .clickable(onClick = onRequestNotif)
                .padding(vertical = 8.dp),
        )
        Spacer(Modifier.height(96.dp))
    }
}
