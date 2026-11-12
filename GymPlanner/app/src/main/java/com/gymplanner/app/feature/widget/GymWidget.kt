package com.gymplanner.app.feature.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.gymplanner.app.GymApp
import com.gymplanner.app.MainActivity
import com.gymplanner.app.R
import com.gymplanner.app.core.ui.Dates
import com.gymplanner.app.feature.today.DoneWorkout
import com.gymplanner.app.feature.today.StreakLogic
import com.gymplanner.app.feature.today.TodayLogic
import com.gymplanner.app.feature.weighin.WeighInActivity
import com.gymplanner.app.feature.weighin.WeighInScheduler
import kotlinx.coroutines.flow.first
import java.time.LocalDate

private data class WidgetData(val profileId: Long?, val streak: Int, val next: String?, val accent: Color)

/** Home-screen widget: current streak, next day in the rotation, and a "+ Weight" shortcut. */
class GymWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = load(context)
        provideContent { Content(data) }
    }

    private suspend fun load(context: Context): WidgetData {
        val c = (context.applicationContext as GymApp).container
        val accent = c.prefs.accent.value.color
        val profile = c.profiles.currentProfile.first() ?: return WidgetData(null, 0, null, accent)
        val sessions = c.workouts.finishedSessions(profile.id).first()
        val history = sessions.map { DoneWorkout(it.planDayId, it.dayName, Dates.toLocalDate(it.startedAt), it.startedAt) }
        val cardioDays = if (profile.cardioCountsTowardTarget) c.track.cardio(profile.id).first().map { LocalDate.ofEpochDay(it.date) } else emptyList()
        val streak = StreakLogic.compute(history.map { it.date } + cardioDays, profile.weeklyTarget, LocalDate.now())
        val plan = c.plans.activePlan(profile.id).first()
        val next = plan?.let { TodayLogic.nextDay(c.plans.getDays(it.id), history, LocalDate.now())?.name }
        return WidgetData(profile.id, streak.current, next, accent)
    }

    @Composable
    private fun Content(d: WidgetData) {
        val label = ColorProvider(Color(0xFFF4F5F6))
        val muted = ColorProvider(Color(0xFFA1A7B0))
        Column(
            GlanceModifier
                .fillMaxSize()
                .background(Color(0xFF1A1C20))
                .cornerRadius(22.dp)
                .padding(14.dp)
                .clickable(actionStartActivity<MainActivity>()),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(ImageProvider(R.drawable.ic_flame), contentDescription = null, modifier = GlanceModifier.size(18.dp), colorFilter = androidx.glance.ColorFilter.tint(ColorProvider(d.accent)))
                Spacer(GlanceModifier.width(6.dp))
                Text(
                    if (d.streak == 1) "1-week streak" else "${d.streak}-week streak",
                    style = TextStyle(color = label, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                )
            }
            Spacer(GlanceModifier.height(8.dp))
            Text("NEXT UP", style = TextStyle(color = muted, fontSize = 11.sp, fontWeight = FontWeight.Medium))
            Text(
                d.next ?: if (d.profileId == null) "Open to set up" else "Add a plan",
                style = TextStyle(color = label, fontSize = 22.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
            Spacer(GlanceModifier.defaultWeight())
            if (d.profileId != null) {
                Box(
                    GlanceModifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(d.accent)
                        .cornerRadius(14.dp)
                        .clickable(
                            actionStartActivity(
                                Intent().setClassName("com.gymplanner.app", WeighInActivity::class.java.name)
                                    .putExtra(WeighInScheduler.EXTRA_PROFILE, d.profileId)
                                    .putExtra(WeighInScheduler.EXTRA_PREVIEW, true)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("+ Weight", style = TextStyle(color = ColorProvider(Color(0xFF0E0F11)), fontSize = 15.sp, fontWeight = FontWeight.Bold))
                }
            }
        }
    }

    companion object {
        /** Redraws any widgets on the home screen (after a workout, a profile switch, …). */
        suspend fun refresh(context: Context) {
            if (GlanceAppWidgetManager(context).getGlanceIds(GymWidget::class.java).isNotEmpty()) GymWidget().updateAll(context)
        }
    }
}


class GymWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GymWidget()
}
