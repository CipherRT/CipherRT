package com.gymplanner.app.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val CardShape = RoundedCornerShape(12.dp)
val ButtonShape = RoundedCornerShape(14.dp)
const val ScreenPadding = 16

/** Big bold title at the top of each tab, with an optional small line above it. */
@Composable
fun LargeTitle(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Column(
        modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                eyebrow?.uppercase() ?: "",
                style = AppTheme.type.footnote.copy(fontWeight = FontWeight.SemiBold),
                color = AppTheme.colors.secondaryLabel,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp), content = trailing)
        }
        Text(title, style = AppTheme.type.largeTitle, color = AppTheme.colors.label, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** Top bar for pushed screens: "‹ Back" on the left, a centred title, actions on the right. */
@Composable
fun NavBar(
    title: String,
    onBack: (() -> Unit)?,
    backLabel: String = "Back",
    actions: @Composable RowScope.() -> Unit = {},
) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(AppTheme.colors.background)
            .statusBarsPadding()
            .height(52.dp)
    ) {
        if (onBack != null) {
            Row(
                Modifier
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button, onClick = onBack)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBackIos, contentDescription = "Back", tint = AppTheme.colors.accent, modifier = Modifier.size(20.dp))
                Text(backLabel, style = AppTheme.type.body, color = AppTheme.colors.accent, maxLines = 1)
            }
        }
        Text(
            title,
            style = AppTheme.type.headline,
            color = AppTheme.colors.label,
            modifier = Modifier.align(Alignment.Center).widthIn(max = 220.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Row(
            Modifier.align(Alignment.CenterEnd).padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = actions,
        )
    }
}

/** Plain accent-coloured text button, like "Edit" or "Done" in an iOS nav bar. */
@Composable
fun TextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    bold: Boolean = false,
    color: Color = AppTheme.colors.accent,
) {
    Text(
        text,
        style = AppTheme.type.body.copy(fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal),
        color = if (enabled) color else AppTheme.colors.tertiaryLabel,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
    )
}

/** iOS "inset grouped" list section with optional header and footer text. */
@Composable
fun GroupedSection(
    modifier: Modifier = Modifier,
    header: String? = null,
    footer: String? = null,
    headerAction: (@Composable () -> Unit)? = null,
    containerColor: Color = AppTheme.colors.card,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth().padding(horizontal = ScreenPadding.dp)) {
        if (header != null || headerAction != null) {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, bottom = 6.dp, top = 4.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    header?.uppercase() ?: "",
                    style = AppTheme.type.sectionHeader,
                    color = AppTheme.colors.secondaryLabel,
                    modifier = Modifier.weight(1f),
                )
                headerAction?.invoke()
            }
        }
        Column(Modifier.fillMaxWidth().clip(CardShape).background(containerColor), content = content)
        if (footer != null) {
            Text(
                footer,
                style = AppTheme.type.footnote,
                color = AppTheme.colors.secondaryLabel,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp),
            )
        }
    }
}

/** Thin separator inset from the left, between rows in a section. */
@Composable
fun RowDivider(startIndent: Dp = 16.dp) {
    HorizontalDivider(Modifier.padding(start = startIndent), thickness = 0.5.dp, color = AppTheme.colors.separator)
}

/** Rounded coloured square with a white symbol — like the icons in iOS Settings. */
@Composable
fun IconTile(icon: ImageVector, color: Color, size: Dp = 30.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size * 0.24f)).background(color),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.62f))
    }
}

/** A standard list row. Shows a chevron when tappable, unless a trailing slot is given. */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    icon: ImageVector? = null,
    iconColor: Color = AppTheme.colors.accent,
    titleColor: Color = AppTheme.colors.label,
    showChevron: Boolean = true,
    onClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .heightIn(min = 52.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading(); Spacer(Modifier.width(12.dp))
        } else if (icon != null) {
            IconTile(icon, iconColor); Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = AppTheme.type.body, color = titleColor, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = AppTheme.type.footnote, color = AppTheme.colors.secondaryLabel, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (value != null) {
            Spacer(Modifier.width(8.dp))
            Text(value, style = AppTheme.type.body, color = AppTheme.colors.secondaryLabel, maxLines = 1)
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp)); trailing()
        } else if (onClick != null && showChevron) {
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = AppTheme.colors.tertiaryLabel,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/** Row with an iOS-style green switch. The whole row is the tap target. */
@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconColor: Color = AppTheme.colors.accent,
) {
    ListRow(
        title = title,
        subtitle = subtitle,
        icon = icon,
        iconColor = iconColor,
        onClick = { onCheckedChange(!checked) },
        trailing = { AppSwitch(checked, onCheckedChange) },
    )
}

@Composable
fun AppSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedTrackColor = AppTheme.colors.green,
            checkedThumbColor = Color.White,
            checkedBorderColor = Color.Transparent,
            uncheckedTrackColor = AppTheme.colors.fill,
            uncheckedThumbColor = Color.White,
            uncheckedBorderColor = Color.Transparent,
        ),
        thumbContent = null,
    )
}

/** Filled, full-width primary button (50 dp tall — easy to hit mid-set). */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = AppTheme.colors.accent,
    icon: ImageVector? = null,
) {
    val bg by animateColorAsState(if (enabled) color else AppTheme.colors.fill, label = "btn")
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .clip(ButtonShape)
            .background(bg)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = if (enabled) Color.White else AppTheme.colors.tertiaryLabel, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = AppTheme.type.headline, color = if (enabled) Color.White else AppTheme.colors.tertiaryLabel)
    }
}

/** Tinted button: light accent background with accent text. */
@Composable
fun TintedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = AppTheme.colors.accent,
    icon: ImageVector? = null,
    compact: Boolean = false,
) {
    Row(
        modifier
            .heightIn(min = if (compact) 36.dp else 50.dp)
            .clip(if (compact) CircleShape else ButtonShape)
            .background(color.copy(alpha = 0.15f))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = if (compact) 14.dp else 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = color, modifier = Modifier.size(if (compact) 18.dp else 20.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = if (compact) AppTheme.type.subhead.copy(fontWeight = FontWeight.SemiBold) else AppTheme.type.headline, color = color)
    }
}

/** Circular icon button with a soft fill (used in title bars). */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color = AppTheme.colors.accent,
    size: Dp = 36.dp,
) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(size).clip(CircleShape).background(AppTheme.colors.fill), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(size * 0.58f))
        }
    }
}

/** iOS segmented control. */
@Composable
fun <T> SegmentedControl(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: (T) -> String = { it.toString() },
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(AppTheme.colors.fill)
            .padding(2.dp)
    ) {
        options.forEach { option ->
            val isSel = option == selected
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .height(32.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (isSel) (if (AppTheme.colors.isDark) Color(0xFF636366) else Color.White) else Color.Transparent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(),
                        role = Role.Tab,
                    ) { onSelect(option) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(option),
                    style = AppTheme.type.footnote.copy(fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Medium),
                    color = AppTheme.colors.label,
                    maxLines = 1,
                )
            }
        }
    }
}

/** iOS stepper: "−  value  +" with large hit areas. */
@Composable
fun Stepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    step: Int = 1,
    format: (Int) -> String = { it.toString() },
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            format(value),
            style = AppTheme.type.body.copy(fontWeight = FontWeight.SemiBold),
            color = AppTheme.colors.label,
            modifier = Modifier.padding(end = 12.dp),
        )
        Row(
            Modifier.height(34.dp).clip(RoundedCornerShape(9.dp)).background(AppTheme.colors.fill),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepperHalf("−", enabled = value - step >= range.first) { onValueChange((value - step).coerceIn(range)) }
            Box(Modifier.width(0.5.dp).height(18.dp).background(AppTheme.colors.separator))
            StepperHalf("+", enabled = value + step <= range.last) { onValueChange((value + step).coerceIn(range)) }
        }
    }
}

@Composable
private fun StepperHalf(symbol: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.width(48.dp).height(34.dp).clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            symbol,
            style = AppTheme.type.title3.copy(fontWeight = FontWeight.Normal),
            color = if (enabled) AppTheme.colors.label else AppTheme.colors.tertiaryLabel,
        )
    }
}

/** Single-line text field styled as a row inside a grouped section. */
@Composable
fun FieldRow(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    singleLine: Boolean = true,
    capitalize: Boolean = true,
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
    ) {
        if (label != null) {
            Text(label, style = AppTheme.type.body, color = AppTheme.colors.label, modifier = Modifier.width(96.dp))
        }
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, style = AppTheme.type.body, color = AppTheme.colors.tertiaryLabel)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                minLines = if (singleLine) 1 else 2,
                textStyle = AppTheme.type.body.copy(color = AppTheme.colors.label),
                cursorBrush = SolidColor(AppTheme.colors.accent),
                keyboardOptions = KeyboardOptions(
                    capitalization = if (capitalize) KeyboardCapitalization.Sentences else KeyboardCapitalization.None,
                    imeAction = if (singleLine) ImeAction.Done else ImeAction.Default,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Rounded search bar like the one in iOS lists. */
@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier, placeholder: String = "Search") {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenPadding.dp)
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(AppTheme.colors.fill)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, null, tint = AppTheme.colors.secondaryLabel, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) Text(placeholder, style = AppTheme.type.body, color = AppTheme.colors.secondaryLabel)
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = AppTheme.type.body.copy(color = AppTheme.colors.label),
                cursorBrush = SolidColor(AppTheme.colors.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Selectable chip / pill (filters). */
@Composable
fun Pill(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .heightIn(min = 34.dp)
            .clip(CircleShape)
            .background(if (selected) AppTheme.colors.accent else AppTheme.colors.fill)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = AppTheme.type.subhead.copy(fontWeight = FontWeight.Medium),
            color = if (selected) Color.White else AppTheme.colors.label,
        )
    }
}

/** Tiny coloured label, e.g. "Optional" or "PR". */
@Composable
fun Badge(text: String, color: Color) {
    Text(
        text,
        style = AppTheme.type.caption.copy(fontWeight = FontWeight.Bold),
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/** Circle with the profile's initial. */
@Composable
fun ProfileAvatar(name: String, onClick: (() -> Unit)? = null, size: Dp = 34.dp) {
    val colors = listOf(AppTheme.colors.accent, AppTheme.colors.green, AppTheme.colors.orange, AppTheme.colors.purple, AppTheme.colors.pink, AppTheme.colors.teal)
    val color = colors[(name.hashCode() and 0x7fffffff) % colors.size]
    Box(
        Modifier
            .size(if (onClick != null) 44.dp else size)
            .clip(CircleShape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(size).clip(CircleShape).background(color),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                name.trim().firstOrNull()?.uppercase() ?: "?",
                style = AppTheme.type.headline.copy(fontSize = AppTheme.type.headline.fontSize * (size.value / 34f)),
                color = Color.White,
            )
        }
    }
}

/** Centred message for empty screens, with an optional button. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = AppTheme.colors.tertiaryLabel, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        Text(title, style = AppTheme.type.title3, color = AppTheme.colors.label, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(message, style = AppTheme.type.subhead, color = AppTheme.colors.secondaryLabel, textAlign = TextAlign.Center)
        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            TintedButton(actionText, onAction)
        }
    }
}

/** A tile with a big number and a label (weekly summary, stats). */
@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = AppTheme.colors.label) {
    Column(
        modifier.clip(CardShape).background(AppTheme.colors.card).padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(label, style = AppTheme.type.footnote, color = AppTheme.colors.secondaryLabel, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(value, style = AppTheme.type.title2, color = valueColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** A plain card container (no header) used for hero blocks. */
@Composable
fun Card(modifier: Modifier = Modifier, padding: PaddingValues = PaddingValues(16.dp), content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = ScreenPadding.dp).clip(CardShape).background(AppTheme.colors.card).padding(padding),
        content = content,
    )
}

/** Round check button used to tick off sets. */
@Composable
fun CheckCircle(checked: Boolean, onClick: () -> Unit, size: Dp = 40.dp) {
    val bg by animateColorAsState(if (checked) AppTheme.colors.green else Color.Transparent, label = "check")
    Box(
        Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Checkbox, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(bg)
                .then(if (!checked) Modifier.border(2.dp, AppTheme.colors.tertiaryLabel, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = "Done",
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.6f),
                )
            }
        }
    }
}
