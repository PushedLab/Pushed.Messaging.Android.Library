package ru.pushed.pushed_demo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.pushed.pushed_demo.ui.theme.CardBg
import ru.pushed.pushed_demo.ui.theme.CardBorder
import ru.pushed.pushed_demo.ui.theme.Coral

@Composable
fun LastPushCard(title: String, body: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Coral.copy(alpha = 0.6f),
                            Coral.copy(alpha = 0.6f),
                            Color.Transparent
                        )
                    )
                )
        )
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Coral.copy(alpha = 0.12f))
                    .border(1.dp, Coral.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Box(Modifier.size(5.dp).background(Coral, CircleShape))
                Spacer(Modifier.width(6.dp))
                Text(
                    "LAST PUSH",
                    fontSize      = 10.sp,
                    fontWeight    = FontWeight.Bold,
                    color         = Coral,
                    letterSpacing = 0.8.sp
                )
            }
            Spacer(Modifier.height(14.dp))

            if (title.isEmpty() && body.isEmpty()) {
                Text(
                    "No messages yet",
                    fontSize  = 14.sp,
                    color     = Color.White.copy(alpha = 0.18f),
                    modifier  = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                if (title.isNotEmpty()) {
                    Text(
                        text       = title,
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White,
                        lineHeight = 22.sp
                    )
                    Spacer(Modifier.height(5.dp))
                }
                if (body.isNotEmpty()) {
                    Text(
                        text       = body,
                        fontSize   = 14.sp,
                        color      = Color.White.copy(alpha = 0.45f),
                        lineHeight = 21.sp
                    )
                }
            }
        }
    }
}
