package com.gymplanner.app.feature.plan

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.gymplanner.app.core.model.AppJson
import com.gymplanner.app.core.model.PlanFile
import java.io.File

/** Writes a plan to a `.gymplan.json` file and opens the Android share sheet. */
object PlanShare {
    fun share(context: Context, file: PlanFile) {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = file.plan.name.replace(Regex("[^A-Za-z0-9 _-]"), "").trim().ifBlank { "plan" }.replace(' ', '-')
        val out = File(dir, "$safeName.gymplan.json")
        out.writeText(AppJson.encodeToString(PlanFile.serializer(), file))
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", out)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "${file.plan.name} — gym plan")
            putExtra(Intent.EXTRA_TEXT, "Here’s my gym plan “${file.plan.name}”. Open the file with Gym Planner to add it.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Share plan"))
    }

    fun readText(context: Context, uri: Uri): String =
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: error("Couldn’t open the file.")
}
