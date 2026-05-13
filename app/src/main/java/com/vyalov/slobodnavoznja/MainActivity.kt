package com.vyalov.slobodnavoznja

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    private lateinit var scheduleService: ScheduleService
    private lateinit var routeButton: Button
    private lateinit var todayButton: Button
    private lateinit var tomorrowButton: Button
    private lateinit var timeChipsContainer: LinearLayout
    private lateinit var departuresList: LinearLayout

    private var selectedDirection = Direction.Center
    private var selectedServiceDayType = currentServiceDayType()
    private var selectedTimeMode: TimeMode = TimeMode.Now
    private var selectedDepartureIndex = -1

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

            todayButton = choiceButton("Radni dan") {
                selectedServiceDayType = ServiceDayType.Weekday
                resetSelectedDeparture()
                refresh()
            }
            addView(todayButton, LinearLayout.LayoutParams(0, buttonHeight(), 1f))

            tomorrowButton = choiceButton("Vikend") {
                selectedServiceDayType = ServiceDayType.Weekend
                resetSelectedDeparture()
                refresh()
            }
            addView(tomorrowButton, LinearLayout.LayoutParams(0, buttonHeight(), 1f).apply {
                marginStart = dp(8)
            })
        }
    }

    private fun timeRow(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            timeChipsContainer = this
        }
    }

    private fun timeChipRow(presets: List<TimePreset>): View {
        return HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER

            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL

                presets.forEachIndexed { index, preset ->
                    addView(
                        timeChip(preset.label, selectedTimeMode == preset.mode) {
                            selectedTimeMode = preset.mode
                            resetSelectedDeparture()
                            refresh()
                        },
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            dp(40)
                        ).apply {
                            if (index > 0) marginStart = dp(8)
                        }
                    )
                }
            }, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ))
        }
    }

    private fun refresh() {
        if (!::departuresList.isInitialized) return

        val target = targetCalendar()
        val targetMinute = target.get(Calendar.HOUR_OF_DAY) * 60 + target.get(Calendar.MINUTE)
        val browseResult = scheduleService.browseDepartures(
            dayType = selectedServiceDayType,
            direction = selectedDirection,
            targetMinuteOfDay = targetMinute
        )
        val departures = browseResult.departures

        routeButton.text = routeLabel()

        updateChoiceState(todayButton, selectedServiceDayType == ServiceDayType.Weekday)
        updateChoiceState(tomorrowButton, selectedServiceDayType == ServiceDayType.Weekend)
        updateTimeChips()

        departuresList.removeAllViews()
        if (departures.isEmpty()) {
            selectedDepartureIndex = -1
            departuresList.addView(sectionTitle(listTitle()))
            departuresList.addView(departureListCard(emptyList()), sectionLayoutParams(topMargin = dp(8)))
            return
        }

        selectedDepartureIndex = if (selectedDepartureIndex < 0) {
            browseResult.nearestIndex.coerceIn(0, departures.lastIndex)
        } else {
            selectedDepartureIndex.coerceIn(0, departures.lastIndex)
        }
        val selectedDeparture = departures[selectedDepartureIndex]

        departuresList.addView(
            selectedDepartureCard(
                item = selectedDeparture,
                selectedIndex = selectedDepartureIndex,
                nearestIndex = browseResult.nearestIndex,
                totalDepartures = departures.size
            )
        )
        departuresList.addView(sectionTitle(listTitle()), sectionLayoutParams(topMargin = dp(18)))
        departuresList.addView(
            departureListCard(departures, selectedDepartureIndex),
            sectionLayoutParams(topMargin = dp(8))
        )
    }

    private fun selectedDepartureCard(
        item: UpcomingDeparture,
        selectedIndex: Int,
        nearestIndex: Int,
        totalDepartures: Int
    ): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBackground(
                fillColor = color(R.color.md_primary),
                radius = dp(22)
            )
            elevation = dp(2).toFloat()
            setPadding(dp(22), dp(20), dp(22), dp(20))

            addView(TextView(this@MainActivity).apply {
                text = if (selectedIndex == nearestIndex) "Sledeći autobus" else "Izabrani polazak"
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

            if (totalDepartures > 1) {
                addView(departureNavigationControls(totalDepartures), LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(14)
                })
            }
        }
    }

    private fun departureNavigationControls(totalDepartures: Int): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            addView(navigationButton("Prethodni", enabled = selectedDepartureIndex > 0) {
                selectedDepartureIndex -= 1
                refresh()
            }, LinearLayout.LayoutParams(0, buttonHeight(), 1f))

            addView(navigationButton("Sledeći", enabled = selectedDepartureIndex < totalDepartures - 1) {
                selectedDepartureIndex += 1
                refresh()
            }, LinearLayout.LayoutParams(0, buttonHeight(), 1f).apply {
                marginStart = dp(10)
            })
        }
    }

    private fun departureListCard(
        items: List<UpcomingDeparture>,
        selectedIndex: Int = 0
    ): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBackground(
                fillColor = color(R.color.md_surface),
                strokeColor = color(R.color.md_outline),
                strokeWidth = dp(1),
                radius = dp(18)
            )
            elevation = dp(1).toFloat()
            setPadding(0, dp(6), 0, dp(6))

            if (items.isEmpty()) {
                addView(emptyDeparturesMessage())
                return@apply
            }

            addView(MaxHeightScrollView(this@MainActivity, dp(250)).apply {
                isNestedScrollingEnabled = true
                addView(LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(10), 0, dp(10), 0)

                    items.forEachIndexed { index, item ->
                        if (index > 0) {
                            addView(divider())
                        }
                        addView(departureRow(item, selected = index == selectedIndex) {
                            selectedDepartureIndex = index
                            refresh()
                        })
                    }
                }, FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ))
                post {
                    scrollTo(0, (selectedIndex - 2).coerceAtLeast(0) * dp(50))
                }
            }, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ))
        }
    }

    private fun departureRow(
        item: UpcomingDeparture,
        selected: Boolean,
        onClick: () -> Unit
    ): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true
            isFocusable = true
            background = roundedBackground(
                fillColor = if (selected) color(R.color.md_surface_variant) else Color.TRANSPARENT,
                strokeColor = if (selected) color(R.color.md_primary_container) else null,
                strokeWidth = if (selected) dp(1) else 0,
                radius = dp(12)
            )
            setPadding(dp(10), dp(10), dp(10), dp(10))
            setOnClickListener { onClick() }

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
                typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                setTextColor(color(if (selected) R.color.md_primary else R.color.md_text_secondary))
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
    }

    private fun navigationButton(text: String, enabled: Boolean, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            textSize = 14f
            isAllCaps = false
            isEnabled = enabled
            alpha = if (enabled) 1f else 0.5f
            setTextColor(color(R.color.md_primary))
            background = roundedBackground(
                fillColor = color(R.color.md_primary_container),
                radius = dp(14)
            )
            setOnClickListener {
                if (enabled) {
                    onClick()
                }
            }
        }
    }

    private fun emptyDeparturesMessage(): View {
        return TextView(this).apply {
            text = "Nema polazaka"
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(color(R.color.md_text_secondary))
            setPadding(dp(16), dp(18), dp(16), dp(18))
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
            when (val mode = selectedTimeMode) {
                TimeMode.Now -> Unit
                TimeMode.Plus1h -> add(Calendar.HOUR_OF_DAY, 1)
                TimeMode.First -> {
                    set(Calendar.HOUR_OF_DAY, 3)
                    set(Calendar.MINUTE, 0)
                }
                is TimeMode.Fixed -> {
                    set(Calendar.HOUR_OF_DAY, mode.hour)
                    set(Calendar.MINUTE, mode.minute)
                }
            }
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private fun swapDirection() {
        selectedDirection = if (selectedDirection == Direction.Center) {
            Direction.Petrovaradin
        } else {
            Direction.Center
        }
        resetSelectedDeparture()
        refresh()
    }

    private fun routeLabel(): String {
        return if (selectedDirection == Direction.Center) {
            "Centar → Petrovaradin"
        } else {
            "Petrovaradin → Centar"
        }
    }

    private fun listTitle(): String {
        return if (selectedServiceDayType == ServiceDayType.Weekday) "Polasci radnim danom" else "Polasci vikendom"
    }

    private fun resetSelectedDeparture() {
        selectedDepartureIndex = -1
    }

    private fun updateTimeChips() {
        if (!::timeChipsContainer.isInitialized) return

        timeChipsContainer.removeAllViews()
        timePresetRows().forEachIndexed { index, presets ->
            timeChipsContainer.addView(timeChipRow(presets), LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                if (index > 0) topMargin = dp(8)
            })
        }
    }

    private fun timePresetRows(): List<List<TimePreset>> {
        return listOf(
            listOf(
                TimePreset("Sada", TimeMode.Now),
                TimePreset("+1 h", TimeMode.Plus1h),
                TimePreset("Prvi", TimeMode.First),
                TimePreset("Jutro", TimeMode.Fixed(7, 0))
            ),
            listOf(
                TimePreset("Podne", TimeMode.Fixed(12, 0)),
                TimePreset("Popodne", TimeMode.Fixed(16, 0)),
                TimePreset("Veče", TimeMode.Fixed(18, 0)),
                TimePreset("Kasno", TimeMode.Fixed(21, 0))
            )
        )
    }

    private fun timeChip(text: String, selected: Boolean, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            textSize = 14f
            isAllCaps = false
            minWidth = 0
            minHeight = 0
            typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            setPadding(dp(16), 0, dp(16), 0)
            setTextColor(color(if (selected) R.color.md_on_primary else R.color.md_text_primary))
            background = if (selected) {
                roundedBackground(fillColor = color(R.color.md_primary), radius = dp(20))
            } else {
                roundedBackground(
                    fillColor = color(R.color.md_surface),
                    strokeColor = color(R.color.md_outline),
                    strokeWidth = dp(1),
                    radius = dp(20)
                )
            }
            setOnClickListener { onClick() }
        }
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
        if (minutes < 0) return formatElapsed(-minutes)
        if (minutes < 60) return "za $minutes min"

        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        return if (remainingMinutes == 0) {
            "za ${hours}h"
        } else {
            "za ${hours}h ${remainingMinutes}m"
        }
    }

    private fun formatElapsed(minutes: Int): String {
        if (minutes < 60) return "pre $minutes min"

        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        return if (remainingMinutes == 0) {
            "pre ${hours}h"
        } else {
            "pre ${hours}h ${remainingMinutes}m"
        }
    }

    private fun buttonHeight(): Int = dp(48)

    private fun color(resId: Int): Int {
        return getColor(resId)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun currentServiceDayType(): ServiceDayType {
        return when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY, Calendar.SUNDAY -> ServiceDayType.Weekend
            else -> ServiceDayType.Weekday
        }
    }

    private data class TimePreset(
        val label: String,
        val mode: TimeMode
    )

    private sealed class TimeMode {
        data object Now : TimeMode()
        data object Plus1h : TimeMode()
        data object First : TimeMode()
        data class Fixed(val hour: Int, val minute: Int) : TimeMode()
    }

    private class MaxHeightScrollView(
        context: Context,
        private val maxHeightPx: Int
    ) : ScrollView(context) {
        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val cappedHeightSpec = MeasureSpec.makeMeasureSpec(maxHeightPx, MeasureSpec.AT_MOST)
            super.onMeasure(widthMeasureSpec, cappedHeightSpec)
        }
    }
}
