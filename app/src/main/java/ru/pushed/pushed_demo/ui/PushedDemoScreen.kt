package ru.pushed.pushed_demo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.pushed.messaginglibrary.Status
import ru.pushed.pushed_demo.ui.components.InfoCard
import ru.pushed.pushed_demo.ui.components.LastPushCard
import ru.pushed.pushed_demo.ui.components.PrimaryButton
import ru.pushed.pushed_demo.ui.components.PulsingDot
import ru.pushed.pushed_demo.ui.components.PushedLogo
import ru.pushed.pushed_demo.ui.components.SecondaryButton
import ru.pushed.pushed_demo.ui.components.SectionLabel
import ru.pushed.pushed_demo.ui.theme.BgColor
import ru.pushed.pushed_demo.ui.theme.GreenActive
import ru.pushed.pushed_demo.ui.theme.RedOffline

@Composable
fun PushedDemoScreen(
    state: UiState,
    onCopyToken: () -> Unit,
    onCopyLogs: () -> Unit
) {
    Surface(color = BgColor, modifier = Modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 48.dp, bottom = 48.dp)
        ) {
            PushedLogo()
            Spacer(Modifier.height(14.dp))
            Text(
                "Pushed",
                fontSize      = 22.sp,
                fontWeight    = FontWeight.Bold,
                color         = Color.White,
                letterSpacing = (-0.5).sp
            )
            Text(
                "SDK DEMO",
                fontSize      = 11.sp,
                fontWeight    = FontWeight.SemiBold,
                color         = Color.White.copy(alpha = 0.3f),
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(36.dp))

            // Status
            InfoCard {
                SectionLabel("Service Status")
                Spacer(Modifier.height(10.dp))
                val isActive = state.status == Status.ACTIVE
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PulsingDot(active = isActive)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text       = when (state.status) {
                            Status.ACTIVE    -> "Active"
                            Status.OFFLINE   -> "Offline"
                            Status.NOTACTIVE -> "Not Active"
                        },
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = if (isActive) GreenActive else RedOffline
                    )
                }
            }
            Spacer(Modifier.height(10.dp))

            // Token
            InfoCard {
                SectionLabel("Client Token")
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.3f))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text       = if (state.token.isNullOrEmpty()) "Initializing…" else state.token,
                        fontSize   = 11.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color      = if (state.token.isNullOrEmpty())
                            Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.5f),
                        lineHeight = 18.sp
                    )
                }
            }
            Spacer(Modifier.height(10.dp))

            PrimaryButton(
                text    = "COPY TOKEN",
                icon    = Icons.Rounded.ContentCopy,
                enabled = !state.token.isNullOrEmpty(),
                onClick = onCopyToken
            )
            Spacer(Modifier.height(10.dp))
            SecondaryButton(
                text    = "COPY LOGS",
                icon    = Icons.Rounded.Description,
                onClick = onCopyLogs
            )
            Spacer(Modifier.height(10.dp))

            LastPushCard(title = state.lastPushTitle, body = state.lastPushBody)
        }
    }
}
