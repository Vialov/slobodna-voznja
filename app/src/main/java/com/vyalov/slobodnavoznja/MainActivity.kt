package com.vyalov.slobodnavoznja

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
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
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
                setPadding(screenHorizontalPadding(), 0, screenHorizontalPadding(), dp(if (isCompactLayout()) 20 else 24))
                setBackgroundColor(color(R.color.md_background))

                addView(appHeader())
                addView(dayRow(), LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(if (isCompactLayout()) 4 else 6)
                })
                addView(timeRow(), LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(if (isCompactLayout()) 8 else 10)
                })

                departuresList = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                }
                addView(departuresList, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(if (isCompactLayout()) 12 else 16)
                })
            }, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ))
        }
    }

    private fun appHeader(): View {
        val compact = isCompactLayout()
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val topContentPadding = dp(if (compact) 4 else 6)
            setPadding(0, topContentPadding, 0, 0)
            ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
                val statusBarTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
                view.setPadding(0, statusBarTop + topContentPadding, 0, 0)
                insets
            }
            ViewCompat.requestApplyInsets(this)

            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL

                addView(TextView(this@MainActivity).apply {
                    text = getString(R.string.app_name)
                    textSize = if (compact) 22f else 24f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(color(R.color.md_text_primary))
                    includeFontPadding = false
                }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            }, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(if (compact) 34 else 40)
            ))

            addView(FrameLayout(this@MainActivity).apply {
                addView(CityTransitIllustrationView(this@MainActivity), FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    dp(if (compact) 84 else 102)
                ).apply {
                    gravity = Gravity.TOP
                })

                addView(routeRow(), FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.TOP
                    topMargin = dp(if (compact) 54 else 68)
                })
            }, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(if (compact) 110 else 134)
            ))
        }
    }

    private fun routeRow(): View {
        val compact = isCompactLayout()
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = roundedBackground(
                fillColor = color(R.color.md_surface),
                strokeColor = color(R.color.md_outline),
                strokeWidth = dp(1),
                radius = dp(24)
            )
            elevation = dp(2).toFloat()
            setPadding(dp(if (compact) 12 else 16), dp(if (compact) 6 else 8), dp(if (compact) 8 else 10), dp(if (compact) 6 else 8))

            routeButton = Button(this@MainActivity).apply {
                textSize = if (compact) 18f else 21f
                isAllCaps = false
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER_VERTICAL or Gravity.START
                minWidth = 0
                minHeight = 0
                setPadding(0, 0, dp(8), 0)
                setTextColor(color(R.color.md_text_primary))
                background = roundedBackground(
                    fillColor = Color.TRANSPARENT,
                    radius = dp(14)
                )
                setOnClickListener {
                    swapDirection()
                }
            }
            addView(routeButton, LinearLayout.LayoutParams(0, dp(if (compact) 44 else 48), 1f))

            addView(Button(this@MainActivity).apply {
                text = "↔"
                textSize = if (compact) 20f else 22f
                isAllCaps = false
                contentDescription = "Promeni smer"
                minWidth = 0
                minHeight = 0
                setPadding(0, 0, 0, 0)
                setTextColor(color(R.color.md_primary))
                background = roundedBackground(
                    fillColor = color(R.color.md_primary_container),
                    radius = dp(if (compact) 22 else 24)
                )
                setOnClickListener {
                    swapDirection()
                }
            }, LinearLayout.LayoutParams(dp(if (compact) 44 else 48), dp(if (compact) 44 else 48)).apply {
                marginStart = dp(if (compact) 6 else 8)
            })
        }
    }

    private fun dayRow(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = roundedBackground(
                fillColor = "#F7F2FF".toColorInt(),
                strokeColor = color(R.color.md_outline),
                strokeWidth = dp(1),
                radius = dp(20)
            )
            setPadding(dp(3), dp(3), dp(3), dp(3))

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
                marginStart = dp(4)
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
        val compact = isCompactLayout()
        return HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            clipToPadding = false
            setPadding(dp(1), 0, dp(1), 0)

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
                            dp(if (compact) 40 else 44)
                        ).apply {
                            if (index > 0) marginStart = dp(if (compact) 2 else 4)
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
        departuresList.addView(sectionTitle(listTitle()), sectionLayoutParams(topMargin = dp(14)))
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
        val compact = isCompactLayout()
        return FrameLayout(this).apply {
            background = gradientBackground(
                startColor = "#5E45B8".toColorInt(),
                endColor = "#9B72F2".toColorInt(),
                radius = dp(24)
            )
            elevation = dp(2).toFloat()
            minimumHeight = dp(if (compact) 166 else 184)

            addView(HeroBusIllustrationView(this@MainActivity), FrameLayout.LayoutParams(
                dp(if (compact) 126 else 156),
                dp(if (compact) 100 else 118)
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                topMargin = dp(if (compact) 14 else 16)
                marginEnd = dp(if (compact) 6 else 10)
            })

            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(if (compact) 14 else 18), dp(if (compact) 14 else 16), dp(if (compact) 14 else 18), dp(if (compact) 14 else 16))

                addView(LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.BOTTOM

                    addView(LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.VERTICAL

                        addView(TextView(this@MainActivity).apply {
                            text = if (selectedIndex == nearestIndex) "Sledeći autobus" else "Izabrani polazak"
                            textSize = if (compact) 15f else 16f
                            typeface = Typeface.DEFAULT_BOLD
                            setTextColor(color(R.color.md_on_primary))
                        })

                        addView(TextView(this@MainActivity).apply {
                            text = item.departure.label
                            textSize = if (compact) 38f else 42f
                            typeface = Typeface.DEFAULT_BOLD
                            includeFontPadding = false
                            setTextColor(color(R.color.md_on_primary))
                        }, LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            topMargin = dp(if (compact) 5 else 7)
                        })

                        if (shouldShowRelativeTime()) {
                            addView(TextView(this@MainActivity).apply {
                                text = formatWait(item.waitMinutes)
                                textSize = if (compact) 15f else 16f
                                typeface = Typeface.DEFAULT_BOLD
                                setTextColor(color(R.color.md_primary_container))
                            }, LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                topMargin = dp(2)
                            })
                        }
                    }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

                    addView(View(this@MainActivity), LinearLayout.LayoutParams(dp(if (compact) 96 else 122), dp(1)))
                }, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ))

                if (totalDepartures > 1) {
                    addView(departureNavigationControls(totalDepartures), LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = dp(if (compact) 8 else 10)
                    })
                }
            }, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ))
        }
    }

    private fun departureNavigationControls(totalDepartures: Int): View {
        val compact = isCompactLayout()
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            addView(navigationButton(
                text = "←  Prethodni",
                enabled = selectedDepartureIndex > 0,
                contentDescription = "Prethodni polazak"
            ) {
                selectedDepartureIndex -= 1
                refresh()
            }, LinearLayout.LayoutParams(0, dp(if (compact) 42 else 44), 1f))

            addView(navigationButton(
                text = "Sledeći  →",
                enabled = selectedDepartureIndex < totalDepartures - 1,
                contentDescription = "Sledeći polazak"
            ) {
                selectedDepartureIndex += 1
                refresh()
            }, LinearLayout.LayoutParams(0, dp(if (compact) 42 else 44), 1f).apply {
                marginStart = dp(if (compact) 8 else 10)
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
            setPadding(0, dp(5), 0, dp(5))

            if (items.isEmpty()) {
                addView(emptyDeparturesMessage())
                return@apply
            }

            addView(MaxHeightScrollView(this@MainActivity, dp(if (isCompactLayout()) 230 else 250)).apply {
                isNestedScrollingEnabled = true
                val listContent = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(if (isCompactLayout()) 6 else 8), 0, dp(if (isCompactLayout()) 6 else 8), 0)

                    items.forEachIndexed { index, item ->
                        if (index > 0) {
                            addView(divider())
                        }
                        addView(departureRow(item, selected = index == selectedIndex) {
                            selectedDepartureIndex = index
                            refresh()
                        })
                    }
                }
                addView(listContent, FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ))
                post {
                    val selectedRow = listContent.getChildAt(selectedIndex * 2) ?: return@post
                    val centeredScrollY = selectedRow.top - (height - selectedRow.height) / 2
                    val maxScrollY = (listContent.height - height).coerceAtLeast(0)
                    scrollTo(0, centeredScrollY.coerceIn(0, maxScrollY))
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
        val compact = isCompactLayout()
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true
            isFocusable = true
            background = roundedBackground(
                fillColor = if (selected) "#F1EAFF".toColorInt() else Color.TRANSPARENT,
                strokeColor = if (selected) "#E4D9FA".toColorInt() else null,
                strokeWidth = if (selected) dp(1) else 0,
                radius = dp(12)
            )
            minimumHeight = dp(if (compact) 48 else 52)
            setPadding(dp(if (compact) 10 else 12), dp(if (compact) 7 else 8), dp(if (compact) 8 else 10), dp(if (compact) 7 else 8))
            setOnClickListener { onClick() }

            addView(TextView(this@MainActivity).apply {
                text = item.departure.label
                textSize = if (compact) 20f else 21f
                typeface = Typeface.DEFAULT_BOLD
                includeFontPadding = false
                setTextColor(color(if (selected) R.color.md_primary else R.color.md_text_primary))
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

            if (shouldShowRelativeTime()) {
                addView(TextView(this@MainActivity).apply {
                    text = formatWait(item.waitMinutes)
                    textSize = if (compact) 15f else 16f
                    gravity = Gravity.END
                    typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                    setTextColor(color(if (selected) R.color.md_primary else R.color.md_text_secondary))
                }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            }

            addView(TextView(this@MainActivity).apply {
                text = "›"
                textSize = if (compact) 26f else 28f
                gravity = Gravity.CENTER
                includeFontPadding = false
                setTextColor(color(if (selected) R.color.md_primary else R.color.md_text_secondary))
            }, LinearLayout.LayoutParams(dp(if (compact) 22 else 24), LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                marginStart = dp(4)
            })
        }
    }

    private fun navigationButton(
        text: String,
        enabled: Boolean,
        contentDescription: String,
        onClick: () -> Unit
    ): Button {
        val compact = isCompactLayout()
        return Button(this).apply {
            this.text = text
            this.contentDescription = contentDescription
            textSize = if (compact) 10.5f else 11.5f
            isAllCaps = false
            isEnabled = enabled
            alpha = if (enabled) 1f else 0.45f
            minWidth = 0
            minHeight = 0
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(if (compact) 6 else 8), 0, dp(if (compact) 6 else 8), 0)
            setTextColor(color(R.color.md_primary))
            background = roundedBackground(
                fillColor = "#F5EFFF".toColorInt(),
                radius = dp(14)
            )
            setOnClickListener {
                if (enabled) {
                    onClick()
                }
            }
        }
    }

    private fun gradientBackground(
        startColor: Int,
        endColor: Int,
        radius: Int
    ) = GradientDrawable(
        GradientDrawable.Orientation.TL_BR,
        intArrayOf(startColor, endColor)
    ).apply {
        cornerRadius = radius.toFloat()
    }

    private fun emptyDeparturesMessage(): View {
        return TextView(this).apply {
            text = getString(R.string.empty_departures)
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
                if (index > 0) topMargin = dp(if (isCompactLayout()) 6 else 8)
            })
        }
    }

    private fun timePresetRows(): List<List<TimePreset>> {
        val firstRow = listOf(
            TimePreset("Sada", TimeMode.Now),
            TimePreset("+1 h", TimeMode.Plus1h),
            TimePreset("Prvi", TimeMode.First),
            TimePreset("Jutro", TimeMode.Fixed(7, 0))
        )
        val secondRow = listOf(
            TimePreset("Podne", TimeMode.Fixed(12, 0)),
            TimePreset("Popodne", TimeMode.Fixed(16, 0)),
            TimePreset("Veče", TimeMode.Fixed(18, 0)),
            TimePreset("Kasno", TimeMode.Fixed(21, 0))
        )

        return listOf(firstRow, secondRow)
    }

    private fun timeChip(text: String, selected: Boolean, onClick: () -> Unit): Button {
        val compact = isCompactLayout()
        return Button(this).apply {
            this.text = text
            textSize = if (compact) 13f else 14f
            isAllCaps = false
            minWidth = 0
            minHeight = 0
            typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            elevation = if (selected) dp(1).toFloat() else 0f
            setPadding(dp(if (compact) 5 else 8), 0, dp(if (compact) 5 else 8), 0)
            setTextColor(color(if (selected) R.color.md_on_primary else R.color.md_text_primary))
            background = if (selected) {
                roundedBackground(fillColor = "#6E55CC".toColorInt(), radius = dp(if (compact) 20 else 22))
            } else {
                roundedBackground(
                    fillColor = "#FFFCFF".toColorInt(),
                    strokeColor = "#E9E0F7".toColorInt(),
                    strokeWidth = dp(1),
                    radius = dp(if (compact) 20 else 22)
                )
            }
            setOnClickListener { onClick() }
        }
    }

    private fun choiceButton(text: String, onClick: () -> Unit): Button {
        val compact = isCompactLayout()
        return Button(this).apply {
            this.text = text
            textSize = if (compact) 13f else 14f
            isAllCaps = false
            minWidth = 0
            minHeight = 0
            setPadding(dp(if (compact) 6 else 8), 0, dp(if (compact) 6 else 8), 0)
            setOnClickListener { onClick() }
        }
    }

    private fun updateChoiceState(button: Button, selected: Boolean) {
        button.isSelected = selected
        button.typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        button.alpha = 1f
        button.elevation = 0f
        button.setTextColor(color(if (selected) R.color.md_on_primary else R.color.md_text_primary))
        button.background = if (selected) {
            roundedBackground(fillColor = "#6E55CC".toColorInt(), radius = dp(if (isCompactLayout()) 16 else 17))
        } else {
            roundedBackground(
                fillColor = Color.TRANSPARENT,
                radius = dp(if (isCompactLayout()) 16 else 17)
            )
        }
    }

    private fun shouldShowRelativeTime(): Boolean {
        return selectedTimeMode == TimeMode.Now
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

    private fun buttonHeight(): Int = dp(if (isCompactLayout()) 42 else 48)

    private fun color(resId: Int): Int {
        return getColor(resId)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun screenHorizontalPadding(): Int {
        return dp(if (isCompactLayout()) 12 else 16)
    }

    private fun isCompactLayout(): Boolean {
        return resources.configuration.screenWidthDp < 390
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

    private class HeroBusIllustrationView(context: Context) : View(context) {
        private val density = resources.displayMetrics.density
        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        private val rect = RectF()

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            val w = width.toFloat()
            val h = height.toFloat()
            val roadY = h * 0.82f

            fillPaint.color = Color.argb(22, 255, 255, 255)
            canvas.drawCircle(w * 0.90f, h * 0.64f, dp(16).toFloat(), fillPaint)
            rect.set(w * 0.87f, h * 0.66f, w * 0.93f, h * 0.90f)
            canvas.drawRoundRect(
                rect,
                dp(12).toFloat(),
                dp(12).toFloat(),
                fillPaint
            )

            strokePaint.color = Color.argb(62, 255, 255, 255)
            strokePaint.strokeWidth = dp(2).toFloat()
            canvas.drawLine(w * 0.12f, roadY, w * 0.96f, roadY, strokePaint)
            canvas.drawLine(w * 0.42f, roadY + dp(9), w * 0.66f, roadY + dp(9), strokePaint)

            drawBusStop(canvas, w * 0.88f, h * 0.21f)
            drawBus(canvas, w * 0.23f, h * 0.24f, w * 0.48f, h * 0.57f)
        }

        private fun drawBus(canvas: Canvas, left: Float, top: Float, width: Float, height: Float) {
            fillPaint.color = Color.argb(52, 255, 255, 255)
            rect.set(left, top, left + width, top + height)
            canvas.drawRoundRect(
                rect,
                dp(12).toFloat(),
                dp(12).toFloat(),
                fillPaint
            )

            fillPaint.color = Color.argb(44, 94, 69, 184)
            rect.set(left + dp(12), top + dp(9), left + width - dp(12), top + height * 0.48f)
            canvas.drawRoundRect(
                rect,
                dp(5).toFloat(),
                dp(5).toFloat(),
                fillPaint
            )

            fillPaint.color = Color.argb(80, 255, 255, 255)
            rect.set(left + width * 0.34f, top + dp(5), left + width * 0.66f, top + dp(10))
            canvas.drawRoundRect(
                rect,
                dp(4).toFloat(),
                dp(4).toFloat(),
                fillPaint
            )

            fillPaint.color = Color.argb(95, 255, 255, 255)
            canvas.drawCircle(left + width * 0.20f, top + height - dp(7), dp(4).toFloat(), fillPaint)
            canvas.drawCircle(left + width * 0.80f, top + height - dp(7), dp(4).toFloat(), fillPaint)

            strokePaint.color = Color.argb(48, 255, 255, 255)
            strokePaint.strokeWidth = dp(1).toFloat()
            canvas.drawLine(left + dp(10), top + height * 0.66f, left + width - dp(10), top + height * 0.66f, strokePaint)
        }

        private fun drawBusStop(canvas: Canvas, cx: Float, top: Float) {
            strokePaint.color = Color.argb(76, 255, 255, 255)
            strokePaint.strokeWidth = dp(2).toFloat()
            canvas.drawLine(cx, top + dp(26), cx, top + dp(68), strokePaint)

            fillPaint.color = Color.argb(30, 255, 255, 255)
            canvas.drawCircle(cx, top + dp(17), dp(14).toFloat(), fillPaint)

            strokePaint.color = Color.argb(105, 255, 255, 255)
            strokePaint.strokeWidth = dp(2).toFloat()
            rect.set(cx - dp(7), top + dp(12), cx + dp(7), top + dp(21))
            canvas.drawRoundRect(
                rect,
                dp(2).toFloat(),
                dp(2).toFloat(),
                strokePaint
            )
        }

        private fun dp(value: Int): Int {
            return (value * density).toInt()
        }
    }

    private class CityTransitIllustrationView(context: Context) : View(context) {
        private val density = resources.displayMetrics.density
        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        private val windowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        private val path = Path()
        private val rect = RectF()

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            val w = width.toFloat()
            val h = height.toFloat()
            val baseline = h * 0.78f

            fillPaint.color = "#F1ECFF".toColorInt()
            path.reset()
            path.moveTo(0f, baseline)
            path.cubicTo(w * 0.16f, h * 0.42f, w * 0.31f, h * 0.72f, w * 0.48f, h * 0.48f)
            path.cubicTo(w * 0.65f, h * 0.26f, w * 0.76f, h * 0.70f, w, h * 0.48f)
            path.lineTo(w, h)
            path.lineTo(0f, h)
            path.close()
            canvas.drawPath(path, fillPaint)

            drawCloud(canvas, w * 0.70f, h * 0.20f, 1.0f)
            drawCloud(canvas, w * 0.38f, h * 0.30f, 0.72f)

            fillPaint.color = "#D9CEF6".toColorInt()
            drawBuilding(canvas, w * 0.72f, baseline - dp(60), dp(34), dp(60), topTower = true)
            drawBuilding(canvas, w * 0.84f, baseline - dp(44), dp(58), dp(44), topTower = false)
            drawBuilding(canvas, w * 0.08f, baseline - dp(50), dp(26), dp(50), topTower = true)

            strokePaint.color = "#B9A8E8".toColorInt()
            strokePaint.strokeWidth = dp(3).toFloat()
            val bridgeTop = baseline - dp(36)
            val bridgeBottom = baseline - dp(4)
            canvas.drawLine(w * 0.05f, bridgeBottom, w * 0.48f, bridgeBottom, strokePaint)
            rect.set(w * 0.06f, bridgeTop, w * 0.25f, bridgeBottom + dp(24))
            canvas.drawArc(rect, 190f, 150f, false, strokePaint)
            rect.set(w * 0.22f, bridgeTop - dp(8), w * 0.43f, bridgeBottom + dp(22))
            canvas.drawArc(rect, 200f, 140f, false, strokePaint)
            repeat(6) { index ->
                val x = w * 0.10f + index * w * 0.06f
                canvas.drawLine(x, bridgeTop + dp(12), x, bridgeBottom, strokePaint)
            }

            fillPaint.color = "#8064D7".toColorInt()
            val busLeft = w * 0.52f
            val busTop = baseline - dp(36)
            rect.set(busLeft, busTop, busLeft + dp(96), busTop + dp(34))
            canvas.drawRoundRect(rect, dp(8).toFloat(), dp(8).toFloat(), fillPaint)

            fillPaint.color = "#EFE9FF".toColorInt()
            repeat(4) { index ->
                val left = busLeft + dp(10) + index * dp(19)
                rect.set(left, busTop + dp(7), left + dp(14), busTop + dp(18))
                canvas.drawRoundRect(
                    rect,
                    dp(2).toFloat(),
                    dp(2).toFloat(),
                    fillPaint
                )
            }

            fillPaint.color = "#4F378B".toColorInt()
            canvas.drawCircle(busLeft + dp(20), busTop + dp(34), dp(5).toFloat(), fillPaint)
            canvas.drawCircle(busLeft + dp(76), busTop + dp(34), dp(5).toFloat(), fillPaint)

            strokePaint.color = "#D3C7F2".toColorInt()
            strokePaint.strokeWidth = dp(2).toFloat()
            canvas.drawLine(0f, baseline + dp(3), w, baseline + dp(3), strokePaint)
        }

        private fun drawCloud(canvas: Canvas, cx: Float, cy: Float, scale: Float) {
            fillPaint.color = "#E3D9FA".toColorInt()
            canvas.drawCircle(cx - dp((26 * scale).toInt()), cy + dp((8 * scale).toInt()), dp((14 * scale).toInt()).toFloat(), fillPaint)
            canvas.drawCircle(cx, cy, dp((20 * scale).toInt()).toFloat(), fillPaint)
            canvas.drawCircle(cx + dp((24 * scale).toInt()), cy + dp((8 * scale).toInt()), dp((15 * scale).toInt()).toFloat(), fillPaint)
            canvas.drawRect(
                cx - dp((38 * scale).toInt()),
                cy + dp((8 * scale).toInt()),
                cx + dp((42 * scale).toInt()),
                cy + dp((20 * scale).toInt()),
                fillPaint
            )
        }

        private fun drawBuilding(
            canvas: Canvas,
            left: Float,
            top: Float,
            width: Int,
            height: Int,
            topTower: Boolean
        ) {
            rect.set(left, top, left + width, top + height)
            canvas.drawRoundRect(
                rect,
                dp(4).toFloat(),
                dp(4).toFloat(),
                fillPaint
            )
            if (topTower) {
                path.reset()
                path.moveTo(left + width / 2f, top - dp(18))
                path.lineTo(left + dp(5), top + dp(2))
                path.lineTo(left + width - dp(5), top + dp(2))
                path.close()
                canvas.drawPath(path, fillPaint)
            }

            windowPaint.color = "#F7F3FF".toColorInt()
            val columns = (width / dp(16)).coerceAtLeast(1)
            repeat(columns) { column ->
                repeat(2) { row ->
                    val x = left + dp(8) + column * dp(16)
                    val y = top + dp(12) + row * dp(18)
                    rect.set(x, y, x + dp(6), y + dp(8))
                    canvas.drawRoundRect(
                        rect,
                        dp(2).toFloat(),
                        dp(2).toFloat(),
                        windowPaint
                    )
                }
            }
        }

        private fun dp(value: Int): Int {
            return (value * density).toInt()
        }
    }
}
