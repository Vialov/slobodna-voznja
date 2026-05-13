package com.vyalov.slobodnavoznja

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.TimePicker
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    private lateinit var scheduleService: ScheduleService
    private lateinit var directionGroup: RadioGroup
    private lateinit var modeGroup: RadioGroup
    private lateinit var dayGroup: RadioGroup
    private lateinit var timePicker: TimePicker
    private lateinit var resultText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scheduleService = ScheduleService(TimetableRepository(this).load())
        setContentView(buildContent())
        updateModeVisibility()
        showNearestNow()
    }

    private fun buildContent(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }

        root.addView(title("Слободна вожња"))

        directionGroup = RadioGroup(this).apply {
            orientation = RadioGroup.HORIZONTAL
            gravity = Gravity.CENTER
            addView(radio(Direction.Center.title, ID_DIRECTION_CENTER, checked = true))
            addView(radio(Direction.Petrovaradin.title, ID_DIRECTION_PETROVARADIN))
        }
        root.addView(directionGroup)

        modeGroup = RadioGroup(this).apply {
            orientation = RadioGroup.HORIZONTAL
            gravity = Gravity.CENTER
            addView(radio("Сада", ID_MODE_NOW, checked = true))
            addView(radio("Време", ID_MODE_TIME))
            setOnCheckedChangeListener { _, _ -> updateModeVisibility() }
        }
        root.addView(modeGroup)

        dayGroup = RadioGroup(this).apply {
            orientation = RadioGroup.HORIZONTAL
            gravity = Gravity.CENTER
            addView(radio("Данас", ID_DAY_TODAY, checked = true))
            addView(radio("Сутра", ID_DAY_TOMORROW))
        }
        root.addView(dayGroup)

        timePicker = TimePicker(this).apply {
            setIs24HourView(true)
        }
        root.addView(timePicker)

        root.addView(Button(this).apply {
            text = "Пронађи"
            setOnClickListener {
                if (isNowMode()) showNearestNow() else showAroundSelectedTime()
            }
        })

        resultText = TextView(this).apply {
            textSize = 22f
            gravity = Gravity.CENTER
            setPadding(0, dp(24), 0, 0)
        }
        root.addView(resultText)

        return root
    }

    private fun updateModeVisibility() {
        val visible = if (isNowMode()) View.GONE else View.VISIBLE
        if (::dayGroup.isInitialized) dayGroup.visibility = visible
        if (::timePicker.isInitialized) timePicker.visibility = visible
    }

    private fun showNearestNow() {
        val calendar = Calendar.getInstance()
        val result = scheduleService.nearestNow(
            currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK),
            currentMinuteOfDay = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE),
            direction = selectedDirection()
        )
        resultText.text = "Следећи полазак ${result.relativeDay.title}\n${result.departure.label}"
    }

    private fun showAroundSelectedTime() {
        val calendar = Calendar.getInstance()
        if (dayGroup.checkedRadioButtonId == ID_DAY_TOMORROW) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val departures = scheduleService.aroundTime(
            dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK),
            direction = selectedDirection(),
            targetMinuteOfDay = timePicker.hour * 60 + timePicker.minute
        )
        resultText.text = departures.joinToString(
            separator = "\n",
            prefix = "Поласци око ${formatTime(timePicker.hour, timePicker.minute)}\n"
        ) { it.label }
    }

    private fun selectedDirection(): Direction {
        return if (directionGroup.checkedRadioButtonId == ID_DIRECTION_PETROVARADIN) {
            Direction.Petrovaradin
        } else {
            Direction.Center
        }
    }

    private fun isNowMode(): Boolean = modeGroup.checkedRadioButtonId == ID_MODE_NOW

    private fun title(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 26f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(16))
        }
    }

    private fun radio(text: String, id: Int, checked: Boolean = false): RadioButton {
        return RadioButton(this).apply {
            this.id = id
            this.text = text
            this.isChecked = checked
        }
    }

    private fun formatTime(hour: Int, minute: Int): String {
        return "%02d:%02d".format(hour, minute)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    companion object {
        private const val ID_DIRECTION_CENTER = 1001
        private const val ID_DIRECTION_PETROVARADIN = 1002
        private const val ID_MODE_NOW = 2001
        private const val ID_MODE_TIME = 2002
        private const val ID_DAY_TODAY = 3001
        private const val ID_DAY_TOMORROW = 3002
    }
}
