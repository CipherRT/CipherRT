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
import androidx.compose.ui.unit.sp

val CardShape = RoundedCornerShape(20.dp)
val ButtonShape = RoundedCornerShape(18.dp)

/** Card look from the mockups: #1A1C20 fill, 1 dp #2C2F35 border, rounded corners. */
@Composable
fun Modifier.cardSurface(shape: androidx.compose.ui.graphics.Shape = CardShape, color: Color = AppTheme.colors.card): Modifier =
    this.clip(shape).background(color).border(1.dp, AppTheme.colors.border, shape)
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
                style = AppTheme.type.sectionHeader,
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
                Icon(Icons.AutoMirrored.Rounded.ArrowBackIos, contentDescription = "Back to $backLabel", tint = AppTheme.colors.label, modifier = Modifier.size(20.dp))
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
        Column(Modifier.fillMaxWidth().cardSurface(color = containerColor), content = content)
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
fun IconTile(icon: ImageVector, @Suppress("UNUSED_PARAMETER") color: Color, size: Dp = 32.dp) {
    // Neutral on purpose: the accent colour is kept for buttons, done sets, streak and PRs.
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size * 0.3f)).background(AppTheme.colors.chip),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = AppTheme.colors.chipText, modifier = Modifier.size(size * 0.58f))
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
            checkedTrackColor = AppTheme.colors.accent,
            checkedThumbColor = AppTheme.colors.onAccent,
            checkedBorderColor = Color.Transparent,
            uncheckedTrackColor = AppTheme.colors.fill,
            uncheckedThumbColor = AppTheme.colors.secondaryLabel,
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
            .heightIn(min = 56.dp)
            .clip(ButtonShape)
            .background(bg)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = if (enabled) textOn(color) else AppTheme.colors.tertiaryLabel, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text,
            style = AppTheme.type.headline.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold),
            color = if (enabled) textOn(color) else AppTheme.colors.tertiaryLabel,
        )
    }
}

/** Tinted button: light accent background with accent text. */
@Composable
fun TintedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = AppTheme.colors.label,
    icon: ImageVector? = null,
    compact: Boolean = false,
) {
    // Secondary button: outlined card, as in the mockups.
    val shape = if (compact) RoundedCornerShape(12.dp) else ButtonShape
    Row(
        modifier
            .heightIn(min = if (compact) 44.dp else 52.dp)
            .cardSurface(shape)
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
    tint: Color = AppTheme.colors.label,
    size: Dp = 40.dp,
) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(size).cardSurface(CircleShape), contentAlignment = Alignment.Center) {
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
            .height(44.dp)
            .cardSurface(RoundedCornerShape(14.dp))
            .padding(4.dp)
    ) {
        options.forEach { option ->
            val isSel = option == selected
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSel) AppTheme.colors.fill else Color.Transparent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(),
                        role = Role.Tab,
                    ) { onSelect(option) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(option),
                    style = AppTheme.type.subhead.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isSel) AppTheme.colors.label else AppTheme.colors.secondaryLabel,
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
            style = AppTheme.type.number.copy(fontSize = 18.sp),
            color = AppTheme.colors.label,
            modifier = Modifier.padding(end = 12.dp),
        )
        Row(
            Modifier.height(44.dp).clip(RoundedCornerShape(12.dp)).background(AppTheme.colors.fill),
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
        Modifier.width(48.dp).height(44.dp).clickable(enabled = enabled, role = Role.Button, onClick = onClick),
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
            .height(48.dp)
            .cardSurface(RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp),
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
            .heightIn(min = 40.dp)
            .then(if (selected) Modifier.clip(CircleShape).background(AppTheme.colors.accent) else Modifier.cardSurface(CircleShape))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = AppTheme.type.subhead.copy(fontWeight = FontWeight.Medium),
            color = if (selected) AppTheme.colors.onAccent else AppTheme.colors.label,
        )
    }
}

/** Tiny coloured label, e.g. "Optional" or "PR". */
@Composable
fun Badge(text: String, color: Color = AppTheme.colors.chipText) {
    val neutral = color == AppTheme.colors.chipText || color == AppTheme.colors.secondaryLabel
    val isAccent = color == AppTheme.colors.accent
    Text(
        text,
        style = AppTheme.type.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
        color = when { isAccent -> AppTheme.colors.onAccent; neutral -> AppTheme.colors.chipText; else -> color },
        modifier = Modifier
            .clip(CircleShape)
            .background(when { isAccent -> color; neutral -> AppTheme.colors.chip; else -> color.copy(alpha = 0.16f) })
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/** Circle with the profile's initial. */
@Composable
fun ProfileAvatar(name: String, onClick: (() -> Unit)? = null, size: Dp = 34.dp) {
    val color = AppTheme.colors.chip
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
                style = AppTheme.type.headline.copy(fontFamily = GroteskFamily, fontSize = AppTheme.type.headline.fontSize * (size.value / 34f)),
                color = AppTheme.colors.label,
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
        modifier.cardSurface(RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(value, style = AppTheme.type.number, color = valueColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        Text(label, style = AppTheme.type.caption, color = AppTheme.colors.secondaryLabel, maxLines = 1)
    }
}

/** A plain card container (no header) used for hero blocks. */
@Composable
fun Card(modifier: Modifier = Modifier, padding: PaddingValues = PaddingValues(16.dp), content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = ScreenPadding.dp).cardSurface(RoundedCornerShape(22.dp)).padding(padding),
        content = content,
    )
}

/** Round check button used to tick off sets. */
@Composable
fun CheckCircle(checked: Boolean, onClick: () -> Unit, size: Dp = 36.dp) {
    val bg by animateColorAsState(if (checked) AppTheme.colors.accent else Color.Transparent, label = "check")
    val shape = RoundedCornerShape(10.dp)
    Box(
        Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).clickable(role = Role.Checkbox, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(size)
                .clip(shape)
                .background(bg)
                .then(if (!checked) Modifier.border(2.dp, AppTheme.colors.outline, shape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = "Done",
                    tint = AppTheme.colors.onAccent,
                    modifier = Modifier.size(size * 0.6f),
                )
            }
        }
    }
}

/** Readable text colour on top of a filled colour (dark on the bright accents). */
@Composable
fun textOn(color: Color): Color =
    if (color.luminanceValue() > 0.4f) AppTheme.colors.onAccent else AppTheme.colors.label

private fun Color.luminanceValue(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue
