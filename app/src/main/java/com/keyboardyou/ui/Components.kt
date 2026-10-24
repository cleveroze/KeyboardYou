package com.keyboardyou.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.keyboardyou.theme.KeyShape
import com.keyboardyou.theme.MorphShape
import com.keyboardyou.theme.PolarShape

class GroupScope {
    val items = mutableListOf<@Composable () -> Unit>()
    fun item(content: @Composable () -> Unit) {
        items += content
    }
}

/** Группа настроек в стиле Android 16: связанные карточки с крупными внешними скруглениями. */
@Composable
fun SettingsGroup(title: String? = null, build: GroupScope.() -> Unit) {
    val items = GroupScope().apply(build).items
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        if (title != null) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 12.dp, top = 18.dp, bottom = 8.dp),
            )
        }
        items.forEachIndexed { i, item ->
            val top = if (i == 0) 26.dp else 6.dp
            val bottom = if (i == items.lastIndex) 26.dp else 6.dp
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(top, top, bottom, bottom),
                modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
            ) { item() }
        }
    }
}

@Composable
fun IconBadge(icon: ImageVector, container: Color = MaterialTheme.colorScheme.secondaryContainer, content: Color = MaterialTheme.colorScheme.onSecondaryContainer, shape: KeyShape = KeyShape.COOKIE_9, size: Int = 40) {
    Box(
        Modifier.size(size.dp).clip(PolarShape(shape)).background(container),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = content, modifier = Modifier.size((size * 0.5f).dp)) }
}

@Composable
fun SettingRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            IconBadge(icon)
            Spacer(Modifier.width(16.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

@Composable
fun SwitchRow(title: String, subtitle: String? = null, icon: ImageVector? = null, checked: Boolean, onChange: (Boolean) -> Unit) {
    SettingRow(title, subtitle, icon, onClick = { onChange(!checked) }) {
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            thumbContent = if (checked) {
                { Icon(Icons.Rounded.Check, null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
            } else null,
        )
    }
}

@Composable
fun SliderRow(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    icon: ImageVector? = null,
    label: (Float) -> String,
    onChange: (Float) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                IconBadge(icon)
                Spacer(Modifier.width(16.dp))
            }
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer) {
                Text(
                    label(value),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        }
        Slider(value = value, onValueChange = onChange, valueRange = range, steps = steps)
    }
}

@Composable
fun <T> SegmentedRow(title: String?, options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        if (title != null) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 10.dp))
        }
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, option ->
                SegmentedButton(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    shape = SegmentedButtonDefaults.itemShape(i, options.size),
                ) { Text(label(option), maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        }
    }
}

/** Ряд выбора формы — как «Свои формы» в Material 3 Expressive. */
@Composable
fun ShapePicker(selected: KeyShape, onSelect: (KeyShape) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(KeyShape.entries) { shape ->
            val isSelected = shape == selected
            val scale by animateFloatAsState(if (isSelected) 1.12f else 1f, spring(dampingRatio = 0.4f, stiffness = 400f), label = "shape")
            val color by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                label = "shapeColor",
            )
            val transition = rememberInfiniteTransition(label = "spin")
            val rot by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart), label = "rot")
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(60.dp)
                        .then(
                            if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp))
                            else Modifier
                        )
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onSelect(shape) },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(42.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                rotationZ = if (isSelected) rot else 0f
                            }
                            .clip(PolarShape(shape))
                            .background(color)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(shape.title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Логотип: форма бесконечно перетекает из одной в другую. */
@Composable
fun MorphingLogo(modifier: Modifier = Modifier, size: Int = 96, color: Color = MaterialTheme.colorScheme.primary, content: @Composable () -> Unit = {}) {
    val transition = rememberInfiniteTransition(label = "logo")
    val t by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(2600), RepeatMode.Reverse), label = "t")
    val rot by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(14000, easing = LinearEasing)), label = "r")
    Box(
        modifier
            .size(size.dp)
            .graphicsLayer { rotationZ = rot }
            .clip(MorphShape(0.9f, KeyShape.COOKIE_9, t))
            .background(color),
        contentAlignment = Alignment.Center,
    ) { Box(Modifier.graphicsLayer { rotationZ = -rot }) { content() } }
}

@Composable
fun PressableCard(
    modifier: Modifier = Modifier,
    color: Color,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(28.dp),
    onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, spring(dampingRatio = 0.5f, stiffness = 500f), label = "card")
    Surface(
        color = color,
        shape = shape,
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .clickable(interactionSource = interaction, indication = androidx.compose.material3.ripple(), onClick = onClick),
    ) {
        Column(Modifier.padding(18.dp), content = content)
    }
}
