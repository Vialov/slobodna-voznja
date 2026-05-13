package com.vyalov.slobodnavoznja

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.TimePicker
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    private lateinit var scheduleService: ScheduleService
    private lateinit var routeButton: Button
    private lateinit var todayButton: Button
    private lateinit var tomorrowButton: Button
    private lateinit var nowButton: Button
    private lateinit var plus30Button: Button
    private lateinit var plus1hButton: Button
    private lateinit var chooseTimeButton: Button
    private lateinit var timePicker: TimePicker
    private lateinit var departuresList: LinearLayout

    private var selectedDirection = Direction.Center
    private var selectedDayOffset = 0
    private var selectedTimeMode = TimeMode.Now

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.app_name)
        scheduleService = ScheduleService(TimetableRepository(this).load())
        setContentView(buildContent())
        refresh()
    }

    private fun buildContent(): View {
        return ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(color(R.color.md_background))
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(16), dp(16), dp(24))
                setBackgroundColor(color(R.color.md_background))

                addView(routeRow())
                addView(dayRow(), LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(14)
                })
                addView(timeRow(), LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(14)
                })

                timePicker = TimePicker(this@MainActivity).apply {
                    setIs24HourView(true)
                    visibility = View.GONE
                    setOnTimeChangedListener { _, _, _ -> refresh() }
                }
                addView(timePicker)

                departuresList = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                }
                addView(departuresList, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(20)
                })
            }, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ))
        }
    }

    private fun routeRow(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = roundedBackground(
                fillColor = color(R.color.md_surface),
                strokeColor = color(R.color.md_outline),
                strokeWidth = dp(1),
                radius = dp(20)
            )
            elevation = dp(2).toFloat()
            setPadding(dp(8), dp(8), dp(8), dp(8))

            routeButton = Button(this@MainActivity).apply {
                textSize = 16f
                isAllCaps = false
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(color(R.color.md_text_primary))
                background = roundedBackground(
                    fillColor = Color.TRANSPARENT,
                    radius = dp(14)
                )
                setOnClickListener {
                    swapDirection()
                }
            }
            addView(routeButton, LinearLayout.LayoutParams(0, buttonHeight(), 1f))

            addView(Button(this@MainActivity).apply {
                text = "↔"
                textSize = 18f
                isAllCaps = false
                setTextColor(color(R.color.md_primary))
                background = roundedBackground(
                    fillColor = color(R.color.md_primary_container),
                    radius = dp(14)
                )
                setOnClickListener {
                    swapDirection()
                }
            }, LinearLayout.LayoutParams(buttonHeight(), buttonHeight()).apply {
                marginStart = dp(8)
            })
        }
    }

    private fun dayRow(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = roundedBackground(
                fillColor = color(R.color.md_surface_variant),
                strokeColor = color(R.color.md_outline),
                strokeWidth = dp(1),
                radius = dp(18)
            )
            setPadding(dp(4), dp(4), dp(4), dp(4))

            todayButton = choiceButton("Danas") {
                selectedDayOffset = 0
                refresh()
            }
            addView(todayButton, LinearLayout.LayoutParams(0, buttonHeight(), 1f))

            tomorrowButton = choiceButton("Sutra") {
                selectedDayOffset = 1
                refresh()
            }
            addView(tomorrowButton, LinearLayout.LayoutParams(0, buttonHeight(), 1f).apply {
                marginStart = dp(8)
            })
        }
    }

    private fun timeRow(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL

            nowButton = choiceButton("Sada") {
                selectedTimeMode = TimeMode.Now
                refresh()
            }
            addView(nowButton, LinearLayout.LayoutParams(0, buttonHeight(), 1f))

            plus30Button = choiceButton("+30 min") {
                selectedTimeMode = TimeMode.Plus30
                refresh()
            }
            addView(plus30Button, LinearLayout.LayoutParams(0, buttonHeight(), 1f).apply {
                marginStart = dp(8)
            })

            plus1hButton = choiceButton("+1 h") {
                selectedTimeMode = TimeMode.Plus1h
                refresh()
            }
            addView(plus1hButton, LinearLayout.LayoutParams(0, buttonHeight(), 1f).apply {
                marginStart = dp(8)
            })

            chooseTimeButton = choiceButton("Izaberi vreme") {
                selectedTimeMode = TimeMode.Custom
                refresh()
            }
            addView(chooseTimeButton, LinearLayout.LayoutParams(0, buttonHeight(), 1.4f).apply {
                marginStart = dp(8)
            })
        }
    }

    private fun refresh() {
        if (!::departuresList.isInitialized) return

        val target = targetCalendar()
        val dayOfWeek = target.get(Calendar.DAY_OF_WEEK)
        val targetMinute = target.get(Calendar.HOUR_OF_DAY) * 60 + target.get(Calendar.MINUTE)
        val departures = scheduleService.nextDepartures(
            dayOfWeek = dayOfWeek,
            direction = selectedDirection,
            targetMinuteOfDay = targetMinute
        )

        routeButton.text = routeLabel()
        timePicker.visibility = if (selectedTimeMode == TimeMode.Custom) View.VISIBLE else View.GONE

        updateChoiceState(todayButton, selectedDayOffset == 0)
        updateChoiceState(tomorrowButton, selectedDayOffset == 1)
        updateChoiceState(nowButton, selectedTimeMode == TimeMode.Now)
        updateChoiceState(plus30Button, selectedTimeMode == TimeMode.Plus30)
        updateChoiceState(plus1hButton, selectedTimeMode == TimeMode.Plus1h)
        updateChoiceState(chooseTimeButton, selectedTimeMode == TimeMode.Custom)

        departuresList.removeAllViews()
        if (departures.isEmpty()) {
            departuresList.addView(sectionTitle("Najbliži polasci ${dayLabel()}"))
            departuresList.addView(departureListCard(emptyList()), sectionLayoutParams(topMargin = dp(8)))
            return
        }

        departuresList.addView(nextBusCard(departures.first()))

        val remainingDepartures = departures.drop(1)
        if (remainingDepartures.isNotEmpty()) {
            departuresList.addView(sectionTitle("Još polazaka ${dayLabel()}"), sectionLayoutParams(topMargin = dp(18)))
            departuresList.addView(departureListCard(remainingDepartures), sectionLayoutParams(topMargin = dp(8)))
        }
    }

    private fun nextBusCard(item: UpcomingDeparture): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBackground(
                fillColor = color(R.color.md_primary),
                radius = dp(22)
            )
            elevation = dp(2).toFloat()
            setPadding(dp(22), dp(20), dp(22), dp(20))

            addView(TextView(this@MainActivity).apply {
                text = "Sledeći autobus"
                textSize = 17f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(color(R.color.md_on_primary))
            })

            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.BOTTOM
                setPadding(0, dp(12), 0, 0)

                addView(TextView(this@MainActivity).apply {
                    text = item.departure.label
                    textSize = 44f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(color(R.color.md_on_primary))
                }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

                addView(TextView(this@MainActivity).apply {
                    text = formatWait(item.waitMinutes)
                    textSize = 18f
                    typeface = Typeface.DEFAULT_BOLD
                    gravity = Gravity.END
                    setTextColor(color(R.color.md_primary_container))
                    setPadding(dp(12), 0, 0, dp(7))
                })
            })
        }
    }

    private fun departureListCard(items: List<UpcomingDeparture>): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBackground(
                fillColor = color(R.color.md_surface),
                strokeColor = color(R.color.md_outline),
                strokeWidth = dp(1),
                radius = dp(18)
            )
            elevation = dp(1).toFloat()
            setPadding(dp(16), dp(6), dp(16), dp(6))

            items.forEachIndexed { index, item ->
                if (index > 0) {
                    addView(divider())
                }
                addView(departureRow(item))
            }
        }
    }

    private fun departureRow(item: UpcomingDeparture): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(10), 0, dp(10))

            addView(TextView(this@MainActivity).apply {
                text = item.departure.label
                textSize = 22f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(color(R.color.md_text_primary))
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

            addView(TextView(this@MainActivity).apply {
                text = formatWait(item.waitMinutes)
                textSize = 16f
                gravity = Gravity.END
                setTextColor(color(R.color.md_text_secondary))
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
    }

    private fun sectionTitle(text: String): View {
        return TextView(this).apply {
            this.text = text
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(color(R.color.md_text_primary))
            setPadding(dp(4), 0, dp(4), 0)
        }
    }

    private fun divider(): View {
        return View(this).apply {
            setBackgroundColor(color(R.color.md_outline))
        }.also {
            it.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1)
            )
        }
    }

    private fun sectionLayoutParams(topMargin: Int): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            this.topMargin = topMargin
        }
    }

    private fun targetCalendar(): Calendar {
        return Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, selectedDayOffset)
            when (selectedTimeMode) {
                TimeMode.Now -> Unit
                TimeMode.Plus30 -> add(Calendar.MINUTE, 30)
                TimeMode.Plus1h -> add(Calendar.HOUR_OF_DAY, 1)
                TimeMode.Custom -> {
                    set(Calendar.HOUR_OF_DAY, timePicker.hour)
                    set(Calendar.MINUTE, timePicker.minute)
                }
            }
        }
    }

    private fun swapDirection() {
        selectedDirection = if (selectedDirection == Direction.Center) {
            Direction.Petrovaradin
        } else {
            Direction.Center
        }
        refresh()
    }

    private fun routeLabel(): String {
        return if (selectedDirection == Direction.Center) {
            "Centar → Petrovaradin"
        } else {
            "Petrovaradin → Centar"
        }
    }

    private fun dayLabel(): String {
        return if (selectedDayOffset == 0) "danas" else "sutra"
    }

    private fun choiceButton(text: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            textSize = 14f
            isAllCaps = false
            minWidth = 0
            minHeight = 0
            setPadding(dp(8), 0, dp(8), 0)
            setOnClickListener { onClick() }
        }
    }

    private fun updateChoiceState(button: Button, selected: Boolean) {
        button.isSelected = selected
        button.typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        button.alpha = 1f
        button.setTextColor(color(if (selected) R.color.md_on_primary else R.color.md_text_primary))
        button.background = if (selected) {
            roundedBackground(fillColor = color(R.color.md_primary), radius = dp(14))
        } else {
            roundedBackground(
                fillColor = color(R.color.md_surface),
                strokeColor = color(R.color.md_outline),
                strokeWidth = dp(1),
                radius = dp(14)
            )
        }
    }

    private fun roundedBackground(
        fillColor: Int,
        strokeColor: Int? = null,
        strokeWidth: Int = 0,
        radius: Int
    ) = GradientDrawable().apply {
        setColor(fillColor)
        if (strokeColor != null && strokeWidth > 0) {
            setStroke(strokeWidth, strokeColor)
        }
        cornerRadius = radius.toFloat()
    }

    private fun formatWait(minutes: Int): String {
        if (minutes < 60) return "za $minutes min"

        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        return if (remainingMinutes == 0) {
            "za ${hours}h"
        } else {
            "za ${hours}h ${remainingMinutes}m"
        }
    }

    private fun buttonHeight(): Int = dp(48)

    private fun color(resId: Int): Int {
        return getColor(resId)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private enum class TimeMode {
        Now,
        Plus30,
        Plus1h,
        Custom
    }
}
