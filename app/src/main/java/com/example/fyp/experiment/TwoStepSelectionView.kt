package com.example.fyp.experiment

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.sqrt
import kotlin.random.Random

class TwoStepSelectionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val locationPoints = mutableListOf<LocationPoint>()
    private var currentTarget: LocationPoint? = null
    private var currentTargetIndex = -1
    private var selectionPhase = SelectionPhase.NONE
    private var zoomCenter: PointF? = null
    private var zoomRadius = 150f
    private var zoomScale = 2.0f
    private var failedAttempts = 0
    private var selectionStartTime = 0L
    private var onSelectionListener: ((Boolean, Int, Int, Long) -> Unit)? = null
    private var lastTouchPoint: PointF? = null

    // Visual parameters
    private val gridSpacing = 50f
    private val roadColor = Color.LTGRAY
    private val parkColor = Color.rgb(200, 255, 200)
    private val waterColor = Color.rgb(200, 200, 255)
    private val fingerRadius = 30f  // Simulated finger size
    private val targetRadius = 15f  // Target point size

    data class LocationPoint(
        val x: Float,
        val y: Float,
        val name: String,
        val type: PointType
    )

    enum class PointType {
        RESTAURANT,
        CAFE,
        SHOP,
        LANDMARK
    }

    private enum class SelectionPhase {
        NONE,
        FIRST_STEP,
        SECOND_STEP
    }

    init {
        generateMapPoints(15)
        setupPaints()
    }

    private fun setupPaints() {
        textPaint.apply {
            textSize = 24f
            color = Color.BLACK
            textAlign = Paint.Align.CENTER
        }
    }

    private fun generateMapPoints(count: Int) {
        locationPoints.clear()
        val padding = 100f
        val pointTypes = PointType.values()
        val locationNames = listOf(
            "Golden Dragon", "Cafe Paris", "Central Mall", "City Park",
            "Ocean View", "Star Plaza", "Green Market", "Blue Harbor",
            "Sun Tower", "Moon Garden", "Cloud Nine", "Royal Palace",
            "Diamond Square", "Crystal Point", "Silver Lake"
        )

        for (i in 0 until count) {
            locationPoints.add(
                LocationPoint(
                    Random.nextFloat() * (width - 2 * padding) + padding,
                    Random.nextFloat() * (height - 2 * padding) + padding,
                    locationNames[i],
                    pointTypes[Random.nextInt(pointTypes.size)]
                )
            )
        }
    }

    fun startNewTrial() {
        selectionPhase = SelectionPhase.FIRST_STEP
        currentTargetIndex = Random.nextInt(locationPoints.size)
        currentTarget = locationPoints[currentTargetIndex]
        failedAttempts = 0
        selectionStartTime = System.currentTimeMillis()
        invalidate()
    }

    fun setOnSelectionListener(listener: (Boolean, Int, Int, Long) -> Unit) {
        onSelectionListener = listener
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        generateMapPoints(15)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw map background
        drawMapBackground(canvas)
        
        // Draw grid (roads)
        drawRoads(canvas)

        // Draw points
        drawLocations(canvas)

        // Draw zoom area if in second phase
        zoomCenter?.let { center ->
            if (selectionPhase == SelectionPhase.SECOND_STEP) {
                drawZoomArea(canvas, center)
            }
        }

        // Draw finger indicator
        lastTouchPoint?.let { point ->
            drawFingerIndicator(canvas, point)
        }

        // Draw instructions
        drawInstructions(canvas)
    }

    private fun drawFingerIndicator(canvas: Canvas, point: PointF) {
        // Draw finger touch area
        paint.style = Paint.Style.STROKE
        paint.color = Color.RED
        paint.strokeWidth = 2f
        canvas.drawCircle(point.x, point.y, fingerRadius, paint)

        // Draw finger center point
        paint.style = Paint.Style.FILL
        paint.color = Color.RED
        canvas.drawCircle(point.x, point.y, 5f, paint)
    }

    private fun drawMapBackground(canvas: Canvas) {
        // Draw base color
        canvas.drawColor(parkColor)
        
        // Draw some water features
        paint.color = waterColor
        canvas.drawCircle(width * 0.2f, height * 0.3f, 100f, paint)
        canvas.drawRect(width * 0.7f, 0f, width.toFloat(), height * 0.4f, paint)
    }

    private fun drawRoads(canvas: Canvas) {
        paint.color = roadColor
        paint.strokeWidth = 3f

        // Draw horizontal roads
        for (i in 0..height.toInt() step gridSpacing.toInt()) {
            canvas.drawLine(0f, i.toFloat(), width.toFloat(), i.toFloat(), paint)
        }

        // Draw vertical roads
        for (i in 0..width.toInt() step gridSpacing.toInt()) {
            canvas.drawLine(i.toFloat(), 0f, i.toFloat(), height.toFloat(), paint)
        }
    }

    private fun drawLocations(canvas: Canvas) {
        locationPoints.forEachIndexed { index, point ->
            val isTarget = index == currentTargetIndex
            
            // Draw target area
            if (isTarget) {
                paint.style = Paint.Style.STROKE
                paint.color = Color.RED
                paint.strokeWidth = 2f
                canvas.drawCircle(point.x, point.y, 100f, paint)
            }
            
            // Draw point
            paint.style = Paint.Style.FILL
            paint.color = when (point.type) {
                PointType.RESTAURANT -> if (isTarget) Color.RED else Color.rgb(255, 100, 100)
                PointType.CAFE -> if (isTarget) Color.RED else Color.rgb(200, 100, 0)
                PointType.SHOP -> if (isTarget) Color.RED else Color.rgb(100, 100, 255)
                PointType.LANDMARK -> if (isTarget) Color.RED else Color.rgb(200, 200, 0)
            }
            canvas.drawCircle(point.x, point.y, targetRadius, paint)

            // Draw point border
            paint.style = Paint.Style.STROKE
            paint.color = Color.BLACK
            paint.strokeWidth = 2f
            canvas.drawCircle(point.x, point.y, targetRadius, paint)

            // Draw location name
            if (isTarget || selectionPhase == SelectionPhase.SECOND_STEP) {
                textPaint.color = if (isTarget) Color.RED else Color.BLACK
                canvas.drawText(point.name, point.x, point.y - 20f, textPaint)
            }
        }
    }

    private fun drawZoomArea(canvas: Canvas, center: PointF) {
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

        // Redraw map content in magnified view
        drawMapBackground(canvas)
        drawRoads(canvas)
        drawLocations(canvas)

        canvas.restore()

        // Draw zoom area border
        paint.style = Paint.Style.STROKE
        paint.color = Color.BLACK
        paint.strokeWidth = 3f
        canvas.drawCircle(center.x, center.y, zoomRadius, paint)
    }

    private fun drawInstructions(canvas: Canvas) {
        textPaint.textSize = 36f
        textPaint.color = Color.BLACK
        
        val instruction = when (selectionPhase) {
            SelectionPhase.FIRST_STEP -> {
                "Step 1: Tap anywhere in the red circle (100px radius)\n" +
                "Target: ${currentTarget?.name}"
            }
            SelectionPhase.SECOND_STEP -> {
                "Step 2: Now tap precisely on the red dot (20px radius)\n" +
                "Target: ${currentTarget?.name}"
            }
            SelectionPhase.NONE -> ""
        }
        
        // Draw instructions with line breaks
        instruction.split("\n").forEachIndexed { index, line ->
            canvas.drawText(line, width / 2f, height - 100f + (index * 40f), textPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN || selectionPhase == SelectionPhase.NONE) {
            return true
        }

        val touchX = event.x
        val touchY = event.y
        lastTouchPoint = PointF(touchX, touchY)

        when (selectionPhase) {
            SelectionPhase.FIRST_STEP -> {
                currentTarget?.let { target ->
                    val distance = calculateDistance(touchX, touchY, target.x, target.y)
                    if (distance <= 100f) {
                        selectionPhase = SelectionPhase.SECOND_STEP
                        zoomCenter = PointF(touchX, touchY)
                        invalidate() // Force redraw to show zoom view
                    } else {
                        failedAttempts++
                        invalidate() // Force redraw to update failed attempts
                    }
                }
            }
            SelectionPhase.SECOND_STEP -> {
                zoomCenter?.let { center ->
                    val transformedX = center.x + (touchX - center.x) / zoomScale
                    val transformedY = center.y + (touchY - center.y) / zoomScale

                    currentTarget?.let { target ->
                        val distance = calculateDistance(transformedX, transformedY, target.x, target.y)
                        val success = distance <= 20f
                        
                        val timeTaken = System.currentTimeMillis() - selectionStartTime
                        onSelectionListener?.invoke(success, currentTargetIndex, failedAttempts, timeTaken)
                        
                        selectionPhase = SelectionPhase.NONE
                        zoomCenter = null
                        lastTouchPoint = null
                        invalidate() // Force redraw to clear the view
                    }
                }
            }
            else -> {}
        }

        return true
    }

    private fun calculateDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2
        val dy = y1 - y2
        return sqrt(dx * dx + dy * dy)
    }
} 