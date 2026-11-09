package com.gymplanner.app.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymplanner.app.AppContainer
import com.gymplanner.app.core.data.TemplateInfo
import com.gymplanner.app.core.model.Goal
import com.gymplanner.app.core.model.UnitSystem
import com.gymplanner.app.core.model.Units
import com.gymplanner.app.core.ui.AppTheme
import com.gymplanner.app.core.ui.CardShape
import com.gymplanner.app.core.ui.GroupedSection
import com.gymplanner.app.core.ui.ListRow
import com.gymplanner.app.core.ui.NumberPadSheet
import com.gymplanner.app.core.ui.PrimaryButton
import com.gymplanner.app.core.ui.RowDivider
import com.gymplanner.app.core.ui.TextAction
import com.gymplanner.app.core.ui.appViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class OnboardingViewModel(private val c: AppContainer) : ViewModel() {
    val templates: List<TemplateInfo> = c.plans.templates()
    var saving by mutableStateOf(false)
        private set

    fun finish(
        name: String, heightCm: Double, weightKg: Double, units: UnitSystem, goal: Goal,
        template: TemplateInfo?, onDone: () -> Unit,
    ) {
        if (saving) return
        saving = true
        viewModelScope.launch {
            val id = c.profiles.create(name, heightCm, weightKg, units, goal, makeCurrent = false)
            if (template != null) c.plans.importPlan(id, template.plan, makeActive = true)
            else c.plans.createEmpty(id, "My Plan", makeActive = true)
            c.profiles.switchTo(id)
            onDone()
        }
    }
}

private enum class Step { WELCOME, NAME, UNITS, BODY, GOAL, START }

/**
 * First launch (or "Add profile"): a few short, friendly questions — one per screen.
 * Nothing is pre-filled, so the app works for anyone.
 */
@Composable
fun OnboardingScreen(isFirstProfile: Boolean, onDone: () -> Unit, onCancel: (() -> Unit)?) {
    val vm = appViewModel { OnboardingViewModel(it) }
    val steps = remember(isFirstProfile) { if (isFirstProfile) Step.entries.toList() else Step.entries.drop(1) }
    var stepIndex by rememberSaveable { mutableStateOf(0) }
    val step = steps[stepIndex]

    var name by rememberSaveable { mutableStateOf("") }
    var units by rememberSaveable { mutableStateOf(UnitSystem.METRIC) }
    var heightCm by rememberSaveable { mutableStateOf<Double?>(null) }
    var weightKg by rememberSaveable { mutableStateOf<Double?>(null) }
    var goal by rememberSaveable { mutableStateOf<Goal?>(null) }
    // -1 = build my own, otherwise template index
    var startChoice by rememberSaveable { mutableStateOf(if (vm.templates.isNotEmpty()) 0 else -1) }

    val canContinue = when (step) {
        Step.WELCOME, Step.UNITS -> true
        Step.NAME -> name.isNotBlank()
        Step.BODY -> (heightCm ?: 0.0) > 0 && (weightKg ?: 0.0) > 0
        Step.GOAL -> goal != null
        Step.START -> !vm.saving
    }

    fun back() { if (stepIndex > 0) stepIndex-- else onCancel?.invoke() }
    BackHandler(enabled = stepIndex > 0 || onCancel != null) { back() }

    Column(
        Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Top bar: back / cancel and a progress bar
        if (step != Step.WELCOME) {
            Row(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(90.dp)) {
                    if (stepIndex > 0) {
                        Row(
                            Modifier.clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button) { back() }.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBackIos, "Back", tint = AppTheme.colors.accent, modifier = Modifier.size(20.dp))
                            Text("Back", style = AppTheme.type.body, color = AppTheme.colors.accent)
                        }
                    } else if (onCancel != null) {
                        TextAction("Cancel", onCancel)
                    }
                }
                val progress by animateFloatAsState((stepIndex + 1f) / steps.size, label = "progress")
                Box(Modifier.weight(1f).height(4.dp).clip(CircleShape).background(AppTheme.colors.fill)) {
                    Box(Modifier.fillMaxWidth(progress).height(4.dp).clip(CircleShape).background(AppTheme.colors.accent))
                }
                Spacer(Modifier.width(90.dp))
            }
        }

        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                (slideInHorizontally { if (forward) it else -it } + fadeIn()) togetherWith
                    (slideOutHorizontally { if (forward) -it / 3 else it / 3 } + fadeOut())
            },
            modifier = Modifier.weight(1f),
            label = "step",
        ) { s ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                when (s) {
                    Step.WELCOME -> WelcomeStep()
                    Step.NAME -> NameStep(name, { name = it }, onSubmit = { if (name.isNotBlank()) stepIndex++ })
                    Step.UNITS -> UnitsStep(units) { units = it }
                    Step.BODY -> BodyStep(units, heightCm, weightKg, { heightCm = it }, { weightKg = it })
                    Step.GOAL -> GoalStep(goal) { goal = it }
                    Step.START -> StartStep(vm.templates, startChoice) { startChoice = it }
                }
            }
        }

        PrimaryButton(
            text = when (step) {
                Step.WELCOME -> "Get Started"
                Step.START -> if (vm.saving) "Setting up…" else "Let’s Go"
                else -> "Continue"
            },
            enabled = canContinue,
            onClick = {
                if (step == Step.START) {
                    vm.finish(
                        name = name, heightCm = heightCm ?: 0.0, weightKg = weightKg ?: 0.0, units = units,
                        goal = goal ?: Goal.MAINTAIN, template = vm.templates.getOrNull(startChoice), onDone = onDone,
                    )
                } else stepIndex++
            },
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun StepTitle(title: String, subtitle: String) {
    Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 24.dp)) {
        Text(title, style = AppTheme.type.title1, color = AppTheme.colors.label)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, style = AppTheme.type.body, color = AppTheme.colors.secondaryLabel)
    }
}

@Composable
private fun WelcomeStep() {
    Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(72.dp))
        Box(
            Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF3A9BFF), Color(0xFF0060DF)))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.FitnessCenter, null, tint = Color.White, modifier = Modifier.size(56.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("Welcome to\nGym Planner", style = AppTheme.type.largeTitle, color = AppTheme.colors.label, textAlign = TextAlign.Center)
        Spacer(Modifier.height(36.dp))
        Feature(Icons.Rounded.EditNote, AppTheme.colors.accent, "Plan your week", "Build your own split or start from a template.")
        Feature(Icons.Rounded.FitnessCenter, AppTheme.colors.green, "Log every set", "Last session’s numbers are right there beside you.")
        Feature(Icons.Rounded.MonitorWeight, AppTheme.colors.orange, "Track your weight", "Add today’s weight in two taps and watch the trend.")
        Feature(Icons.Rounded.Lock, AppTheme.colors.indigo, "Private by design", "No account, no internet. Everything stays on this phone.")
    }
}

@Composable
private fun Feature(icon: ImageVector, color: Color, title: String, text: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(36.dp))
        Spacer(Modifier.width(18.dp))
        Column {
            Text(title, style = AppTheme.type.headline, color = AppTheme.colors.label)
            Text(text, style = AppTheme.type.subhead, color = AppTheme.colors.secondaryLabel)
        }
    }
}

@Composable
private fun NameStep(name: String, onChange: (String) -> Unit, onSubmit: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    StepTitle("What should we call you?", "Your name is only used on this phone, to tell profiles apart.")
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(CardShape)
            .background(AppTheme.colors.card)
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        if (name.isEmpty()) Text("Name", style = AppTheme.type.title3, color = AppTheme.colors.tertiaryLabel)
        BasicTextField(
            value = name,
            onValueChange = { if (it.length <= 30) onChange(it) },
            singleLine = true,
            textStyle = AppTheme.type.title3.copy(color = AppTheme.colors.label),
            cursorBrush = SolidColor(AppTheme.colors.accent),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { onSubmit() }),
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
        )
    }
}

@Composable
private fun UnitsStep(units: UnitSystem, onSelect: (UnitSystem) -> Unit) {
    StepTitle("Which units do you use?", "You can change this any time in Settings.")
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ChoiceCard("Kilograms & centimetres", "kg · cm", units == UnitSystem.METRIC) { onSelect(UnitSystem.METRIC) }
        ChoiceCard("Pounds & inches", "lb · ft/in", units == UnitSystem.IMPERIAL) { onSelect(UnitSystem.IMPERIAL) }
    }
}

@Composable
private fun ChoiceCard(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(CardShape)
            .background(AppTheme.colors.card)
            .border(2.dp, if (selected) AppTheme.colors.accent else Color.Transparent, CardShape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = AppTheme.type.headline, color = AppTheme.colors.label)
            Text(subtitle, style = AppTheme.type.subhead, color = AppTheme.colors.secondaryLabel)
        }
        Icon(
            if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
            contentDescription = if (selected) "Selected" else null,
            tint = if (selected) AppTheme.colors.accent else AppTheme.colors.tertiaryLabel,
            modifier = Modifier.size(28.dp),
        )
    }
}

private enum class BodyField { HEIGHT, FEET, INCHES, WEIGHT }

@Composable
private fun BodyStep(
    units: UnitSystem,
    heightCm: Double?,
    weightKg: Double?,
    onHeight: (Double) -> Unit,
    onWeight: (Double) -> Unit,
) {
    var editing by remember { mutableStateOf<BodyField?>(null) }
    StepTitle("Height and weight", "Used for your weight chart and BMI. Your weight becomes the first entry on the chart.")

    val totalIn = heightCm?.let { (it / Units.CM_PER_IN).roundToInt() }
    GroupedSection {
        if (units == UnitSystem.METRIC) {
            ListRow("Height", value = heightCm?.let { "${it.roundToInt()} cm" } ?: "Add", onClick = { editing = BodyField.HEIGHT })
        } else {
            ListRow("Height — feet", value = totalIn?.let { "${it / 12} ft" } ?: "Add", onClick = { editing = BodyField.FEET })
            RowDivider()
            ListRow("Height — inches", value = totalIn?.let { "${it % 12} in" } ?: "Add", onClick = { editing = BodyField.INCHES })
        }
        RowDivider()
        ListRow("Current weight", value = weightKg?.let { Units.formatWeight(it, units) } ?: "Add", onClick = { editing = BodyField.WEIGHT })
    }

    when (editing) {
        BodyField.HEIGHT -> NumberPadSheet(
            title = "Height", initialValue = heightCm?.let { it.roundToInt().toDouble() }, unit = "cm", allowDecimal = false,
            maxValue = 260.0, onDismiss = { editing = null }, onConfirm = { onHeight(it); editing = null },
        )
        BodyField.FEET -> NumberPadSheet(
            title = "Height (feet)", initialValue = totalIn?.let { (it / 12).toDouble() }, unit = "ft", allowDecimal = false,
            maxValue = 8.0, onDismiss = { editing = null },
            onConfirm = { ft -> onHeight((ft * 12 + (totalIn ?: 0) % 12) * Units.CM_PER_IN); editing = BodyField.INCHES },
        )
        BodyField.INCHES -> NumberPadSheet(
            title = "Height (inches)", initialValue = totalIn?.let { (it % 12).toDouble() }, unit = "in", allowDecimal = false,
            maxValue = 11.0, allowZero = true, onDismiss = { editing = null },
            onConfirm = { inch -> onHeight((((totalIn ?: 0) / 12) * 12 + inch) * Units.CM_PER_IN); editing = null },
        )
        BodyField.WEIGHT -> NumberPadSheet(
            title = "Current weight", initialValue = weightKg?.let { Units.kgToDisplay(it, units) }, unit = units.weightUnit,
            maxValue = if (units == UnitSystem.METRIC) 400.0 else 880.0,
            onDismiss = { editing = null }, onConfirm = { onWeight(Units.displayToKg(it, units)); editing = null },
        )
        null -> Unit
    }
}

@Composable
private fun GoalStep(goal: Goal?, onSelect: (Goal) -> Unit) {
    StepTitle("What’s your goal?", "This just helps describe your progress. You can change it later.")
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Goal.entries.forEach { g -> ChoiceCard(g.label, g.blurb, goal == g) { onSelect(g) } }
    }
}

@Composable
private fun StartStep(templates: List<TemplateInfo>, choice: Int, onChoice: (Int) -> Unit) {
    StepTitle("How do you want to start?", "Pick a ready-made plan to edit, or start with a blank one.")
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        templates.forEachIndexed { i, t ->
            ChoiceCard(
                title = t.plan.name,
                subtitle = listOf(t.plan.description, t.plan.days.joinToString(" · ") { it.name })
                    .filter { it.isNotBlank() }.joinToString("\n"),
                selected = choice == i,
            ) { onChoice(i) }
        }
        ChoiceCard("Build my own", "Start with an empty plan and add your own days.", choice == -1) { onChoice(-1) }
        Row(Modifier.padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Insights, null, tint = AppTheme.colors.secondaryLabel, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                "Every exercise, set and rep can be changed afterwards.",
                style = AppTheme.type.footnote.copy(fontWeight = FontWeight.Normal, fontSize = 13.sp),
                color = AppTheme.colors.secondaryLabel,
            )
        }
    }
}
