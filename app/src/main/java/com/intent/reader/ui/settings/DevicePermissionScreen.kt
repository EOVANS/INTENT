package com.intent.reader.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.intent.reader.R
import com.intent.reader.ui.theme.IntentTheme
import com.intent.reader.ui.theme.withCjkSpacing

/** Whether the user currently lets the app post notifications. */
private fun notificationsAllowed(context: Context): Boolean =
    NotificationManagerCompat.from(context).areNotificationsEnabled()

/**
 * Runtime permissions cannot be revoked from inside an app, so switching one off hands the
 * user straight to the system page that owns the toggle.
 */
private fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    runCatching { context.startActivity(intent) }.onFailure {
        runCatching {
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null),
                )
            )
        }
    }
}

/**
 * Device & permissions: the permissions INTENT actually declares, each with what it is,
 * why the app asks for it, and what stops working when it is off. Only runtime
 * permissions get a live switch; install-time permissions are shown as fixed facts
 * instead of pretending they can be toggled here.
 */
@Composable
fun DevicePermissionScreen(onBack: () -> Unit) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    val context = LocalContext.current
    val vm = settingsVm()
    val s = vm.settings
    var notifications by remember { mutableStateOf(notificationsAllowed(context)) }

    // Re-read the state after the user leaves the system permission sheet or settings.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) notifications = notificationsAllowed(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val requestNotifications = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notifications = granted || notificationsAllowed(context)
        if (!granted) {
            Toast.makeText(context, R.string.permissions_notify_denied, Toast.LENGTH_LONG).show()
        }
    }

    SettingsScaffold(title = stringResource(R.string.settings_permissions), onBack = onBack) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.surface).padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(64.dp).background(Color(0xFF3D7BEA), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Security, contentDescription = null, tint = Color.White, modifier = Modifier.size(38.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.settings_permissions), style = t.pageTitle, fontWeight = FontWeight.Bold, color = c.text)
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.permissions_intro).withCjkSpacing(),
                style = t.bodySmall,
                color = c.textSecondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SectionLabel(stringResource(R.string.permissions_runtime_section))
        SettingsGroup {
            PermissionRow(
                icon = Icons.Filled.Notifications,
                tint = Color(0xFF31C76A),
                title = stringResource(R.string.permissions_notify_title),
                details = stringResource(R.string.permissions_notify_details),
                consequence = stringResource(R.string.permissions_notify_off),
                checked = notifications,
            ) { want ->
                if (want) {
                    val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED
                    if (granted) openNotificationSettings(context)
                    else requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    openNotificationSettings(context)
                }
            }
        }

        SectionLabel(stringResource(R.string.permissions_install_section))
        SettingsGroup {
            PermissionRow(
                icon = Icons.Filled.Wifi,
                tint = Color(0xFF45A4E8),
                title = stringResource(R.string.permissions_network_title),
                details = stringResource(R.string.permissions_network_details),
                consequence = stringResource(R.string.permissions_network_off),
                checked = true,
                locked = true,
            )
            PermissionRow(
                icon = Icons.Filled.Sync,
                tint = Color(0xFF50B7C5),
                title = stringResource(R.string.permissions_background_title),
                details = stringResource(R.string.permissions_background_details),
                consequence = stringResource(R.string.permissions_background_off),
                checked = true,
                locked = true,
            )
        }

        // App-level controls: neither is a system permission, so the switch is
        // stored in INTENT itself and describes what the app is allowed to do.
        SectionLabel(stringResource(R.string.permissions_app_section))
        SettingsGroup {
            PermissionRow(
                icon = Icons.Filled.ContentPaste,
                tint = Color(0xFF8A6BF0),
                title = stringResource(R.string.permissions_clipboard_title),
                details = stringResource(R.string.permissions_clipboard_details),
                consequence = stringResource(R.string.permissions_clipboard_off),
                checked = s.clipboardAccess,
            ) { vm.setClipboardAccess(it) }
            PermissionRow(
                icon = Icons.Filled.Widgets,
                tint = Color(0xFFE58B32),
                title = stringResource(R.string.permissions_widget_title),
                details = stringResource(R.string.permissions_widget_details),
                consequence = stringResource(R.string.permissions_widget_off),
                checked = s.widgetCreation,
            ) { vm.setWidgetCreation(it) }
        }
        Spacer(Modifier.height(10.dp))
    }
}

/**
 * One permission: icon, name, a switch on the right, then what it is / why it is asked
 * for and what changes when it is unavailable.
 */
@Composable
private fun PermissionRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    details: String,
    consequence: String,
    checked: Boolean,
    locked: Boolean = false,
    onCheckedChange: (Boolean) -> Unit = {},
) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    Column(
        Modifier
            .fillMaxWidth()
            .then(if (locked) Modifier else Modifier.clickable { onCheckedChange(!checked) })
            .padding(vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(11.dp))
            Text(
                text = title.withCjkSpacing(),
                style = t.body,
                color = c.text,
                modifier = Modifier.weight(1f),
            )
            // Install-time permissions are reported, not offered: the system grants them
            // at install and no app can hand that control to the user.
            if (locked) {
                Text(
                    text = stringResource(R.string.permissions_granted),
                    style = t.micro,
                    color = tint,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(tint.copy(alpha = 0.14f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                )
            } else {
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = c.text,
                        checkedThumbColor = c.bg,
                        uncheckedTrackColor = c.divider,
                        uncheckedThumbColor = c.bg,
                        uncheckedBorderColor = c.divider,
                    ),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = details.withCjkSpacing(),
            style = t.bodySmall,
            color = c.textSecondary,
            modifier = Modifier.padding(start = 30.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = consequence.withCjkSpacing(),
            style = t.micro,
            color = c.textTertiary,
            modifier = Modifier.padding(start = 30.dp),
        )
    }
}
