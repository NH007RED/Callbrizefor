package com.nh.callbrize.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nh.callbrize.domain.Card
import com.nh.callbrize.settings.Avatars
import com.nh.callbrize.ui.CardBlack
import com.nh.callbrize.ui.CardRed
import com.nh.callbrize.ui.Gold
import com.nh.callbrize.ui.GoldBrush
import com.nh.callbrize.ui.GoldDim
import com.nh.callbrize.ui.GoldLight
import com.nh.callbrize.ui.GoldRim
import com.nh.callbrize.ui.GlassBrush
import com.nh.callbrize.ui.Navy
import com.nh.callbrize.ui.PanelBrush
import com.nh.callbrize.ui.SoftText

@Composable
fun GoldButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    subtitle: String? = null
) {
    val shape = RoundedCornerShape(14.dp)
    val bg: Brush = if (enabled) GoldBrush else SolidColor(Color(0xFF3A4468))
    val fg = if (enabled) Navy else Color(0xFFB8C0DD)
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .shadow(if (enabled) 8.dp else 0.dp, shape, ambientColor = Gold, spotColor = Gold)
            .clip(shape)
            .background(bg)
            .border(1.dp, Color(0x66FFFFFF), shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text, color = fg, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, textAlign = TextAlign.Center)
            if (subtitle != null) Text(subtitle, color = fg.copy(alpha = 0.8f), fontSize = 11.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun OutlineGoldButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    subtitle: String? = null
) {
    val shape = RoundedCornerShape(14.dp)
    val fg = if (enabled) Gold else Color(0xFFA6AFD0)
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(shape)
            .background(GlassBrush)
            .border(1.2.dp, if (enabled) Gold else Color(0xFF55608A), shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text, color = fg, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, textAlign = TextAlign.Center)
            if (subtitle != null) Text(subtitle, color = fg.copy(alpha = 0.8f), fontSize = 11.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun GlassPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .fillMaxWidth()
            .shadow(6.dp, shape)
            .clip(shape)
            .background(PanelBrush)
            .border(1.dp, GoldRim, shape)
            .padding(12.dp),
        content = content
    )
}

/** Standard screen layout with a back button, a serif title, a gold rule and a scrollable body. */
@Composable
fun ScreenFrame(
    title: String,
    onBack: (() -> Unit)?,
    scroll: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(GlassBrush)
                        .border(1.dp, GoldDim, CircleShape).clickable(onClick = onBack)
                        .semantics { contentDescription = "Back" },
                    contentAlignment = Alignment.Center
                ) { Text("←", color = Gold, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
            }
            Text(
                title, color = Gold, fontSize = 20.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = if (onBack != null) 12.dp else 8.dp)
            )
        }
        Box(
            Modifier.fillMaxWidth().height(1.dp)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, Gold, Color.Transparent)))
        )
        val body = if (scroll) Modifier.weight(1f).verticalScroll(rememberScrollState()) else Modifier.weight(1f)
        Column(
            body.fillMaxWidth().padding(top = 14.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            content()
        }
    }
}

@Composable
fun AvatarBubble(index: Int, size: Dp, active: Boolean = false, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size)
            .shadow(if (active) 12.dp else 3.dp, CircleShape, ambientColor = Gold, spotColor = Gold)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Color(0xFF34479B), Color(0xFF111C44))))
            .border(if (active) 3.dp else 1.5.dp, if (active) SolidColor(GoldLight) else GoldRim, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(Avatars.emoji(index), fontSize = (size.value * 0.5f).sp)
    }
}

@Composable
fun PlayingCard(
    card: Card,
    width: Dp,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    val color = if (card.suit.isRed) CardRed else CardBlack
    val shape = RoundedCornerShape(width * 0.12f)
    val face = card.rank.value in 11..13
    val glow = if (highlight) Gold else Color.Black
    Box(
        modifier
            .width(width)
            .height(width * 1.4f)
            .shadow(if (highlight) 10.dp else 3.dp, shape, ambientColor = glow, spotColor = glow)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFE6EAF3))))
            .border(if (highlight) 2.5.dp else 1.dp, if (highlight) Gold else Color(0xFFB4BCD3), shape)
            .semantics { contentDescription = card.spoken }
    ) {
        Column(Modifier.padding(start = width * 0.08f, top = width * 0.03f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(card.rank.label, color = color, fontSize = (width.value * 0.25f).sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(card.suit.symbol, color = color, fontSize = (width.value * 0.21f).sp, maxLines = 1)
        }
        if (face) {
            Column(
                Modifier.align(Alignment.Center).padding(top = width * 0.1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(card.rank.label, color = color, fontSize = (width.value * 0.46f).sp,
                    fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black, maxLines = 1)
                Text(card.suit.symbol, color = color, fontSize = (width.value * 0.3f).sp, maxLines = 1)
            }
        } else {
            Text(
                card.suit.symbol, color = color, fontSize = (width.value * 0.55f).sp,
                modifier = Modifier.align(Alignment.Center).padding(top = width * 0.1f)
            )
        }
        Column(
            Modifier.align(Alignment.BottomEnd)
                .padding(end = width * 0.08f, bottom = width * 0.03f)
                .graphicsLayer { rotationZ = 180f },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(card.rank.label, color = color, fontSize = (width.value * 0.2f).sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(card.suit.symbol, color = color, fontSize = (width.value * 0.17f).sp, maxLines = 1)
        }
    }
}

@Composable
fun CardBack(width: Dp, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(width * 0.14f)
    Box(
        modifier.width(width).height(width * 1.4f)
            .shadow(2.dp, shape)
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFF2B3F94), Color(0xFF0E1744))))
            .border(1.dp, Gold, shape),
        contentAlignment = Alignment.Center
    ) {
        Text("◆", color = Gold.copy(alpha = 0.85f), fontSize = (width.value * 0.5f).sp)
    }
}

@Composable
fun BodyText(text: String, modifier: Modifier = Modifier, bold: Boolean = false) {
    Text(
        text, modifier = modifier, color = SoftText, fontSize = 14.sp, lineHeight = 20.sp,
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal
    )
}

@Composable
fun SectionTitle(text: String) {
    Text(text, color = GoldLight, fontSize = 15.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
}
