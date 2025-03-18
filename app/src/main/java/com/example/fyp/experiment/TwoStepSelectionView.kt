package com.example.fyp.experiment

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

class TwoStepSelectionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val targetPoints = mutableListOf<PointF>()
    private var currentTarget: PointF? = null
    private var currentTargetIndex = -1
    private var selectionPhase = SelectionPhase.NONE
    private var zoomCenter: PointF? = null
    private var zoomRadius = 150f
    private var zoomScale = 2.0f
    private var failedAttempts = 0
    private var selectionStartTime = 0L
    private var onSelectionListener: ((Boolean, Int, Int, Long) -> Unit)? = null

    private enum class SelectionPhase {
        NONE,
        FIRST_STEP,
        SECOND_STEP
    }

    init {
        // Generate random target points
        generateTargetPoints(10)
    }

    private fun generateTargetPoints(count: Int) {
        targetPoints.clear()
        val padding = 50f
        for (i in 0 until count) {
            targetPoints.add(PointF(
                Random.nextFloat() * (width - 2 * padding) + padding,
                Random.nextFloat() * (height - 2 * padding) + padding
            ))
        }
    }

    fun startNewTrial() {
        selectionPhase = SelectionPhase.FIRST_STEP
        currentTargetIndex = Random.nextInt(targetPoints.size)
        currentTarget = targetPoints[currentTargetIndex]
        failedAttempts = 0
        selectionStartTime = System.currentTimeMillis()
        invalidate()
    }

    fun setOnSelectionListener(listener: (Boolean, Int, Int, Long) -> Unit) {
        onSelectionListener = listener
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        generateTargetPoints(10)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw background
        canvas.drawColor(Color.WHITE)

        // Draw all points
        paint.style = Paint.Style.FILL
        targetPoints.forEachIndexed { index, point ->
            paint.color = if (index == currentTargetIndex) Color.RED else Color.BLUE
            canvas.drawCircle(point.x, point.y, 10f, paint)
        }

        // Draw zoom area if in second phase
        zoomCenter?.let { center ->
            if (selectionPhase == SelectionPhase.SECOND_STEP) {
                // Draw darkened background
                paint.color = Color.argb(100, 0, 0, 0)
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

                // Create magnified view
                canvas.save()
                canvas.clipPath(Path().apply {
                    addCircle(center.x, center.y, zoomRadius, Path.Direction.CW)
                })

                // Draw magnified content
                canvas.translate(center.x, center.y)
                canvas.scale(zoomScale, zoomScale, 0f, 0f)
                canvas.translate(-center.x, -center.y)

                // Redraw points in magnified view
                canvas.drawColor(Color.WHITE)
                targetPoints.forEachIndexed { index, point ->
                    paint.color = if (index == currentTargetIndex) Color.RED else Color.BLUE
                    canvas.drawCircle(point.x, point.y, 10f / zoomScale, paint)
                }

                canvas.restore()

                // Draw zoom area border
                paint.style = Paint.Style.STROKE
                paint.color = Color.BLACK
                paint.strokeWidth = 2f
                canvas.drawCircle(center.x, center.y, zoomRadius, paint)
            }
        }

        // Draw touch guidance
        paint.color = Color.BLACK
        paint.textSize = 30f
        paint.textAlign = Paint.Align.CENTER
        val instruction = when (selectionPhase) {
            SelectionPhase.FIRST_STEP -> "Tap near the red target"
            SelectionPhase.SECOND_STEP -> "Now tap precisely on the target"
            SelectionPhase.NONE -> ""
        }
        canvas.drawText(instruction, width / 2f, height - 50f, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN || selectionPhase == SelectionPhase.NONE) {
            return true
        }

        val touchX = event.x
        val touchY = event.y

        when (selectionPhase) {
            SelectionPhase.FIRST_STEP -> {
                // Check if touch is roughly near the target
                currentTarget?.let { target ->
                    val distance = calculateDistance(touchX, touchY, target.x, target.y)
                    if (distance <= 100f) {
                        // Success - move to precise selection
                        selectionPhase = SelectionPhase.SECOND_STEP
                        zoomCenter = PointF(touchX, touchY)
                    } else {
                        // Failed attempt
                        failedAttempts++
                    }
                }
            }
            SelectionPhase.SECOND_STEP -> {
                // Transform touch point based on zoom
                zoomCenter?.let { center ->
                    val transformedX = center.x + (touchX - center.x) / zoomScale
                    val transformedY = center.y + (touchY - center.y) / zoomScale

                    currentTarget?.let { target ->
                        val distance = calculateDistance(transformedX, transformedY, target.x, target.y)
                        val success = distance <= 20f
                        
                        // Notify listener of the result
                        val timeTaken = System.currentTimeMillis() - selectionStartTime
                        onSelectionListener?.invoke(success, currentTargetIndex, failedAttempts, timeTaken)
                        
                        // Reset for next trial
                        selectionPhase = SelectionPhase.NONE
                        zoomCenter = null
                    }
                }
            }
            else -> {}
        }

        invalidate()
        return true
    }

    private fun calculateDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2
        val dy = y1 - y2
        return sqrt(dx * dx + dy * dy)
    }
} 