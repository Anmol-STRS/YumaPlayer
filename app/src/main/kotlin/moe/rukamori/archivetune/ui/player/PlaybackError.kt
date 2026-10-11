/*
 * YumaPlayer (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)

package moe.rukamori.archivetune.ui.player

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.PlaybackException
import moe.rukamori.archivetune.MainActivity
import moe.rukamori.archivetune.R
import moe.rukamori.archivetune.ui.screens.buildLoginRoute
import moe.rukamori.archivetune.ui.settings.SettingsDimensions

@Composable
fun PlaybackErrorDialog(
    error: PlaybackException,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onLogin: (() -> Unit)? = null,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(
                    horizontal = SettingsDimensions.ScreenBottomPadding,
                    vertical = SettingsDimensions.ScreenBottomPadding,
                ),
            contentAlignment = Alignment.Center,
        ) {
            val dialogWidth = minOf(PlaybackErrorDialogMaxWidth, maxWidth)
            Surface(
                modifier = Modifier
                    .width(dialogWidth)
                    .heightIn(max = maxHeight),
                shape = RoundedCornerShape(SettingsDimensions.BottomSheetCornerRadius),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
            ) {
                PlaybackError(
                    error = error,
                    onRetry = onRetry,
                    onDismiss = onDismiss,
                    onLogin = onLogin,
                )
            }
        }
    }
}

@Composable
fun PlaybackError(
    error: PlaybackException,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    onLogin: (() -> Unit)? = null,
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val fallbackUnknown = stringResource(R.string.error_unknown)
    val fallbackNoInternet = stringResource(R.string.error_no_internet)
    val fallbackTimeout = stringResource(R.string.error_timeout)
    val fallbackNoStream = stringResource(R.string.error_no_stream)
    val fallbackMalformedStream = stringResource(R.string.error_malformed_stream)
    val retryText = stringResource(R.string.retry)
    val dismissText = stringResource(R.string.dismiss)
    val copyText = stringResource(R.string.copy)
    val copiedText = stringResource(R.string.copied)
    val loginText = stringResource(R.string.login)
    val detailsText = stringResource(R.string.details)
    val codeLabel = stringResource(R.string.playback_error_code)
    val httpLabel = stringResource(R.string.playback_error_http)
    val messageLabel = stringResource(R.string.playback_error_message)
    val causeLabel = stringResource(R.string.playback_error_cause)

    val errorInfo = remember(error) { error.toPlaybackErrorInfo() }
    val httpCode = errorInfo.httpCode
    val title =
        when (errorInfo.kind) {
            PlaybackErrorKind.LoginRefreshRequired -> stringResource(R.string.playback_login_refresh_required)
            PlaybackErrorKind.ConfirmationRequired -> stringResource(R.string.playback_confirmation_required)
            PlaybackErrorKind.NoInternet -> fallbackNoInternet
            PlaybackErrorKind.Timeout -> fallbackTimeout
            PlaybackErrorKind.NoStream -> fallbackNoStream
            PlaybackErrorKind.MalformedStream -> fallbackMalformedStream
            else -> fallbackUnknown
        }
    val reason =
        when (errorInfo.kind) {
            PlaybackErrorKind.LoginRefreshRequired -> {
                stringResource(R.string.playback_requires_youtube_music_login_refresh)
            }

            PlaybackErrorKind.ConfirmationRequired -> {
                stringResource(R.string.playback_requires_youtube_music_confirmation)
            }

            PlaybackErrorKind.NoInternet -> {
                fallbackNoInternet
            }

            PlaybackErrorKind.Timeout -> {
                fallbackTimeout
            }

            PlaybackErrorKind.NoStream -> {
                fallbackNoStream
            }

            PlaybackErrorKind.MalformedStream -> {
                fallbackMalformedStream
            }

            PlaybackErrorKind.Decoder -> {
                "$fallbackUnknown ($codeLabel ${error.errorCode})"
            }

            PlaybackErrorKind.Http -> {
                "$fallbackUnknown ($httpLabel $httpCode)"
            }

            PlaybackErrorKind.Unknown -> {
                error.cause?.message?.takeIf { it.isNotBlank() }
                    ?: error.message?.takeIf { it.isNotBlank() }
                    ?: fallbackUnknown
            }
        }

    val details =
        remember(error, reason, httpCode, codeLabel, httpLabel, messageLabel, causeLabel) {
            buildPlaybackErrorDetails(
                error = error,
                reason = reason,
                httpCode = httpCode,
                codeLabel = codeLabel,
                httpLabel = httpLabel,
                messageLabel = messageLabel,
                causeLabel = causeLabel,
            )
        }

    val recoveryUrl = errorInfo.loginRecoveryUrl

    val handleLogin: () -> Unit = remember(onLogin, onDismiss, context, recoveryUrl) {
        {
            onDismiss()
            if (onLogin != null) {
                onLogin()
            } else {
                val intent = Intent(context, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    putExtra("navigate_to", buildLoginRoute(recoveryUrl))
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                try {
                    context.startActivity(intent)
                } catch (_: Exception) {
                }
            }
        }
    }

    val onCopyClick =
        remember(clipboard, context, details, copiedText) {
            {
                clipboard.setText(AnnotatedString(details))
                Toast.makeText(context, copiedText, Toast.LENGTH_SHORT).show()
            }
        }

    val isAuthError = errorInfo.kind == PlaybackErrorKind.LoginRefreshRequired ||
        errorInfo.kind == PlaybackErrorKind.ConfirmationRequired

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PlaybackErrorHeader(
            title = title,
            reason = reason,
            isAuthError = isAuthError,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PlaybackErrorDetails(
                label = detailsText,
                details = details,
                copyText = copyText,
                onCopyClick = onCopyClick,
            )
        }

        PlaybackErrorActions(
            kind = errorInfo.kind,
            retryText = retryText,
            dismissText = dismissText,
            loginText = loginText,
            onRetryClick = onRetry,
            onDismissClick = onDismiss,
            onLoginClick = handleLogin,
        )
    }
}

@Composable
private fun PlaybackErrorHeader(
    title: String,
    reason: String,
    isAuthError: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            shape = RoundedCornerShape(SettingsDimensions.LibrarySmallRadius),
            color = if (isAuthError) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            },
            modifier = Modifier.size(SettingsDimensions.BannerIconSize),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                Icon(
                    painter = painterResource(
                        if (isAuthError) R.drawable.login else R.drawable.ic_about
                    ),
                    contentDescription = null,
                    tint = if (isAuthError) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    },
                    modifier = Modifier.size(SettingsDimensions.BannerIconInnerSize),
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = reason,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PlaybackErrorDetails(
    label: String,
    details: String,
    copyText: String,
    onCopyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(SettingsDimensions.LibraryCardRadius),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = onCopyClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.select_all),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = copyText,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }

            Text(
                text = details,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                softWrap = true,
                overflow = TextOverflow.Clip,
            )
        }
    }
}

@Composable
private fun PlaybackErrorActions(
    kind: PlaybackErrorKind,
    retryText: String,
    dismissText: String,
    loginText: String,
    onRetryClick: () -> Unit,
    onDismissClick: () -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when {
            kind == PlaybackErrorKind.LoginRefreshRequired || kind == PlaybackErrorKind.ConfirmationRequired -> {
                Button(
                    onClick = onLoginClick,
                    modifier = Modifier.fillMaxWidth(),
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(text = loginText)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismissClick) {
                        Text(text = dismissText)
                    }
                    OutlinedButton(onClick = onRetryClick) {
                        Text(text = retryText)
                    }
                }
            }

            else -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismissClick) {
                        Text(text = dismissText)
                    }
                    Button(onClick = onRetryClick) {
                        Text(text = retryText)
                    }
                }
            }
        }
    }
}

private fun buildPlaybackErrorDetails(
    error: PlaybackException,
    reason: String,
    httpCode: Int?,
    codeLabel: String,
    httpLabel: String,
    messageLabel: String,
    causeLabel: String,
): String =
    buildString {
        appendLine(reason)
        appendLine("$codeLabel: ${error.errorCode}")
        if (httpCode != null) appendLine("$httpLabel: $httpCode")

        val rootMessage = error.message?.trim().orEmpty()
        if (rootMessage.isNotBlank() && rootMessage != reason) {
            appendLine()
            appendLine("$messageLabel: $rootMessage")
        }

        var throwable: Throwable? = error.cause
        var depth = 0
        while (throwable != null && depth < PlaybackErrorMaxCauseDepth) {
            val name = throwable.javaClass.simpleName.ifBlank { throwable.javaClass.name }
            val message = throwable.message?.trim().orEmpty()
            appendLine()
            appendLine("$causeLabel: $name${if (message.isNotBlank()) ": $message" else ""}")
            throwable = throwable.cause
            depth++
        }
    }.trim()

private val PlaybackErrorDialogMaxWidth: Dp = 420.dp
private const val PlaybackErrorMaxCauseDepth = 6
