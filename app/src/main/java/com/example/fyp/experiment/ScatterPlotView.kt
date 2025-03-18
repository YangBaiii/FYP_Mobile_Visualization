package com.example.fyp.experiment

import android.content.Context
import android.graphics.*
import android.text.TextPaint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.*
import kotlin.random.Random

class ScatterPlotView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        isAntiAlias = true
    }
    private val textPaint = TextPaint().apply {
        color = Color.BLACK
        textSize = 30f
        isAntiAlias = true
    }

    // Chart data
    private val dataPoints = mutableListOf<DataPoint>()
    private val visiblePoints = mutableListOf<DataPoint>()
    private var selectedPoint: DataPoint? = null
    
    // Chart dimensions
    private val chartPadding = 50f
    private var chartWidth = 0f
    private var chartHeight = 0f
    private var scaleX = 1f
    private var scaleY = 1f
    private var offsetX = 0f
    private var offsetY = 0f

    // Adaptive parameters
    companion object {
        private const val BASE_TAP_ZONE_RADIUS = 30f
        private const val MAX_TAP_ZONE_RADIUS = 150f
        private const val MIN_TAP_ZONE_RADIUS = 20f
        private const val MAX_ZOOM_LEVEL = 5f
        private const val MIN_ZOOM_LEVEL = 1f
        private const val ZOOM_STEP = 0.5f
        private const val MAGNIFIER_RADIUS = 100f
    }

    private val performanceHistory = mutableMapOf<Int, MutableList<Float>>() // Point index -> list of distances
    private val regionPerformance = mutableMapOf<Int, Float>() // Region index -> average success rate
    private val pointDensity = mutableMapOf<Int, Float>() // Region index -> point density
    private val regions = 5 // Number of regions to divide the chart into
    private var currentZoomLevel = MIN_ZOOM_LEVEL
    private var failedAttempts = 0
    private val maxFailedAttempts = 3
    private var magnifierCenter: PointF? = null
    private var magnifierBitmap: Bitmap? = null

    // Experiment callbacks
    private var onPointSelected: ((DataPoint, Int, Int) -> Unit)? = null
    private var onFailedAttempt: ((Int) -> Unit)? = null

    data class DataPoint(
        val x: Float,
        val y: Float,
        val price: Float,
        val timestamp: String,
        val index: Int,
        var tapZoneRadius: Float = BASE_TAP_ZONE_RADIUS
    )

    init {
        generateSampleData()
        calculatePointDensity()
    }

    private fun generateSampleData() {
        val basePrice = 100f
        var currentPrice = basePrice
        val timePoints = 50

        for (i in 0 until timePoints) {
            // Generate random price movement
            val change = Random.nextFloat() * 10f - 5f // Random change between -5 and +5
            currentPrice += change
            currentPrice = currentPrice.coerceIn(50f, 150f) // Keep price between 50 and 150

            dataPoints.add(DataPoint(
                x = i.toFloat(),
                y = currentPrice,
                price = currentPrice,
                timestamp = "T${i + 1}",
                index = i
            ))
        }
        visiblePoints.addAll(dataPoints)
    }

    private fun calculatePointDensity() {
        val regionWidth = dataPoints.size / regions.toFloat()
        for (i in 0 until regions) {
            val startX = i * regionWidth
            val endX = (i + 1) * regionWidth
            val pointsInRegion = dataPoints.count { it.x in startX..endX }
            pointDensity[i] = pointsInRegion / regionWidth
        }
    }

    private fun getRegionIndex(x: Float): Int {
        val regionWidth = dataPoints.size / regions.toFloat()
        return (x / regionWidth).toInt().coerceIn(0, regions - 1)
    }

    private fun updateTapZoneRadius(point: DataPoint, distance: Float, success: Boolean) {
        val regionIndex = getRegionIndex(point.x)
        val regionDensity = pointDensity[regionIndex] ?: 1f
        val regionSuccessRate = regionPerformance[regionIndex] ?: 0.5f

        performanceHistory.getOrPut(point.index) { mutableListOf() }.add(distance)

        var newRadius = point.tapZoneRadius

        if (success) {
            // On success, slightly reduce radius based on performance
            val avgDistance = performanceHistory[point.index]?.average()?.toFloat() ?: distance
            newRadius = (newRadius * (1f - (avgDistance / MAX_TAP_ZONE_RADIUS) * 0.1f))
                .coerceAtLeast(MIN_TAP_ZONE_RADIUS)
        } else {
            // On failure, increase radius based on multiple factors
            val densityFactor = 1f + (regionDensity / 10f)
            val performanceFactor = 1f + (1f - regionSuccessRate) // Worse performance = larger increase
            newRadius = (newRadius * densityFactor * performanceFactor)
                .coerceAtMost(MAX_TAP_ZONE_RADIUS)
        }

        point.tapZoneRadius = newRadius

        // Update region performance
        val currentRegionSuccess = regionPerformance[regionIndex] ?: 0.5f
        regionPerformance[regionIndex] = (currentRegionSuccess * 0.7f + (if (success) 1f else 0f) * 0.3f)
    }

    private fun handleZoom(touchX: Float, touchY: Float) {
        currentZoomLevel = (currentZoomLevel + ZOOM_STEP).coerceAtMost(MAX_ZOOM_LEVEL)
        magnifierCenter = PointF(touchX, touchY)
        createMagnifierBitmap()
        invalidate()
    }

    private fun createMagnifierBitmap() {
        magnifierBitmap?.recycle()
        magnifierBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(magnifierBitmap!!)
        
        // Draw background
        canvas.drawColor(Color.WHITE)
        
        // Draw grid with adjusted scale
        drawGrid(canvas)
        
        // Draw price line with adjusted scale
        drawPriceLine(canvas)
        
        // Draw data points with adjusted scale
        drawDataPoints(canvas)
        
        // Draw selected point if any
        selectedPoint?.let { point ->
            drawSelectedPoint(canvas, point)
        }
    }

    private fun drawMagnifier(canvas: Canvas, center: PointF) {
        magnifierBitmap?.let { bitmap ->
            // Create magnifier effect
            val magnifierPaint = Paint().apply {
                isAntiAlias = true
                maskFilter = BlurMaskFilter(10f, BlurMaskFilter.Blur.NORMAL)
            }

            // Calculate source and destination rectangles
            val srcRect = Rect(
                (center.x - MAGNIFIER_RADIUS / currentZoomLevel).toInt(),
                (center.y - MAGNIFIER_RADIUS / currentZoomLevel).toInt(),
                (center.x + MAGNIFIER_RADIUS / currentZoomLevel).toInt(),
                (center.y + MAGNIFIER_RADIUS / currentZoomLevel).toInt()
            ).apply {
                // Ensure the rectangle stays within bitmap bounds
                left = left.coerceIn(0, bitmap.width)
                top = top.coerceIn(0, bitmap.height)
                right = right.coerceIn(0, bitmap.width)
                bottom = bottom.coerceIn(0, bitmap.height)
            }

            val dstRect = RectF(
                center.x - MAGNIFIER_RADIUS,
                center.y - MAGNIFIER_RADIUS,
                center.x + MAGNIFIER_RADIUS,
                center.y + MAGNIFIER_RADIUS
            )

            // Draw magnifier circle with blur effect
            canvas.drawCircle(center.x, center.y, MAGNIFIER_RADIUS, magnifierPaint)
            
            // Draw magnified content
            canvas.save()
            canvas.clipPath(Path().apply {
                addCircle(center.x, center.y, MAGNIFIER_RADIUS, Path.Direction.CW)
            })
            
            // Draw the magnified bitmap content
            canvas.drawBitmap(bitmap, srcRect, dstRect, null)
            canvas.restore()

            // Draw magnifier border
            paint.style = Paint.Style.STROKE
            paint.color = Color.BLUE
            paint.strokeWidth = 2f
            canvas.drawCircle(center.x, center.y, MAGNIFIER_RADIUS, paint)

            // Find and display data point under magnifier
            val magnifiedPoint = findClosestPoint(center.x, center.y)
            magnifiedPoint?.let { point ->
                val pointX = chartPadding + point.x * scaleX
                val pointY = height - chartPadding - (point.y - dataPoints.minOf { it.y }) * scaleY
                val distance = calculateDistance(center.x, center.y, pointX, pointY)

                if (distance <= MAGNIFIER_RADIUS) {
                    // Draw data point in magnifier
                    paint.style = Paint.Style.FILL
                    paint.color = Color.RED
                    canvas.drawCircle(pointX, pointY, 6f, paint)

                    // Draw price label with adjusted size for magnification
                    textPaint.color = Color.RED
                    textPaint.textSize = 30f * currentZoomLevel
                    val priceText = "$${String.format("%.2f", point.price)}"
                    val timeText = point.timestamp
                    
                    // Draw price above the point
                    canvas.drawText(
                        priceText,
                        pointX - textPaint.measureText(priceText) / 2,
                        pointY - 15f * currentZoomLevel,
                        textPaint
                    )
                    
                    // Draw timestamp below the point
                    canvas.drawText(
                        timeText,
                        pointX - textPaint.measureText(timeText) / 2,
                        pointY + 25f * currentZoomLevel,
                        textPaint
                    )
                }
            }
        }
    }

    private fun drawZoomIndicator(canvas: Canvas) {
        textPaint.color = Color.BLUE
        textPaint.textSize = 40f
        canvas.drawText(
            "Zoom Level: ${String.format("%.1f", currentZoomLevel)}x",
            chartPadding,
            chartPadding - 10f,
            textPaint
        )
    }

    private fun drawGrid(canvas: Canvas) {
        paint.style = Paint.Style.STROKE
        paint.color = Color.LTGRAY
        paint.strokeWidth = 1f

        // Draw vertical lines
        val timeStep = 10
        for (i in 0..dataPoints.size step timeStep) {
            val x = chartPadding + i * scaleX
            canvas.drawLine(x, chartPadding, x, height - chartPadding, paint)
            
            // Draw time labels
            if (i < dataPoints.size) {
                canvas.drawText(
                    dataPoints[i].timestamp,
                    x - 15f,
                    height - chartPadding + 30f,
                    textPaint
                )
            }
        }

        // Draw horizontal lines
        val priceStep = 20f
        val minPrice = dataPoints.minOf { it.y }
        val maxPrice = dataPoints.maxOf { it.y }
        for (price in minPrice.toInt()..maxPrice.toInt() step priceStep.toInt()) {
            val y = height - chartPadding - (price - minPrice) * scaleY
            canvas.drawLine(chartPadding, y, width - chartPadding, y, paint)
            
            // Draw price labels
            canvas.drawText(
                "$price",
                chartPadding - 40f,
                y + 10f,
                textPaint
            )
        }
    }

    private fun drawPriceLine(canvas: Canvas) {
        paint.style = Paint.Style.STROKE
        paint.color = Color.BLUE
        paint.strokeWidth = 2f

        val path = Path()
        visiblePoints.forEachIndexed { index, point ->
            val x = chartPadding + point.x * scaleX
            val y = height - chartPadding - (point.y - dataPoints.minOf { it.y }) * scaleY

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        canvas.drawPath(path, paint)
    }

    private fun drawDataPoints(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        paint.color = Color.BLUE

        visiblePoints.forEach { point ->
            val x = chartPadding + point.x * scaleX
            val y = height - chartPadding - (point.y - dataPoints.minOf { it.y }) * scaleY
            canvas.drawCircle(x, y, 4f, paint)
        }
    }

    private fun drawSelectedPoint(canvas: Canvas, point: DataPoint) {
        val x = chartPadding + point.x * scaleX
        val y = height - chartPadding - (point.y - dataPoints.minOf { it.y }) * scaleY

        // Draw tap zone
        paint.style = Paint.Style.STROKE
        paint.color = Color.RED
        paint.strokeWidth = 2f
        canvas.drawCircle(x, y, point.tapZoneRadius, paint)

        // Draw selected point
        paint.style = Paint.Style.FILL
        paint.color = Color.RED
        canvas.drawCircle(x, y, 6f, paint)

        // Draw price label
        textPaint.color = Color.RED
        canvas.drawText(
            "$${String.format("%.2f", point.price)}",
            x + 10f,
            y - 10f,
            textPaint
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        chartWidth = w - 2 * chartPadding
        chartHeight = h - 2 * chartPadding
        updateScaling()
    }

    private fun updateScaling() {
        val priceRange = dataPoints.maxOf { it.y } - dataPoints.minOf { it.y }
        val timeRange = dataPoints.size - 1f
        
        scaleX = chartWidth / timeRange
        scaleY = chartHeight / priceRange
    }

    override fun onDraw(canvas: Canvas) {
        try {
            super.onDraw(canvas)
            
            // Draw background
            canvas.drawColor(Color.WHITE)
            
            // Draw grid
            drawGrid(canvas)
            
            // Draw price line
            drawPriceLine(canvas)
            
            // Draw data points
            drawDataPoints(canvas)
            
            // Draw selected point and zoom area
            selectedPoint?.let { point ->
                drawSelectedPoint(canvas, point)
            }

            // Draw magnifier if active
            magnifierCenter?.let { center ->
                drawMagnifier(canvas, center)
            }

            // Draw zoom level indicator
            if (magnifierCenter != null) {
                drawZoomIndicator(canvas)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        try {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    val touchX = event.x
                    val touchY = event.y

                    // Find the closest point
                    val closestPoint = findClosestPoint(touchX, touchY)
                    if (closestPoint != null) {
                        val pointX = chartPadding + closestPoint.x * scaleX
                        val pointY = height - chartPadding - (closestPoint.y - dataPoints.minOf { it.y }) * scaleY
                        val distance = calculateDistance(touchX, touchY, pointX, pointY)

                        if (distance <= closestPoint.tapZoneRadius) {
                            // Successful tap
                            selectedPoint = closestPoint
                            updateTapZoneRadius(closestPoint, distance, true)
                            onPointSelected?.invoke(closestPoint, 0, (closestPoint.tapZoneRadius / BASE_TAP_ZONE_RADIUS).toInt())
                            magnifierCenter = null
                            invalidate()
                            return true
                        } else {
                            // Failed tap
                            failedAttempts++
                            if (failedAttempts >= maxFailedAttempts) {
                                // Handle zooming
                                handleZoom(touchX, touchY)
                                failedAttempts = 0
                            }
                            updateTapZoneRadius(closestPoint, distance, false)
                            onFailedAttempt?.invoke(failedAttempts)
                            invalidate()
                            return true
                        }
                    }
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    // Update magnifier position
                    magnifierCenter?.let {
                        magnifierCenter = PointF(event.x, event.y)
                        invalidate()
                    }
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    // Clear magnifier
                    magnifierCenter = null
                    magnifierBitmap?.recycle()
                    magnifierBitmap = null
                    currentZoomLevel = MIN_ZOOM_LEVEL
                    invalidate()
                    return true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return super.onTouchEvent(event)
    }

    private fun findClosestPoint(x: Float, y: Float): DataPoint? {
        val touchX = (x - chartPadding) / scaleX
        val touchY = (height - chartPadding - y) / scaleY + dataPoints.minOf { it.y }

        return visiblePoints.minByOrNull { point ->
            calculateDistance(touchX, touchY, point.x, point.y)
        }
    }

    private fun calculateDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return try {
            val dx = x2 - x1
            val dy = y2 - y1
            sqrt(dx * dx + dy * dy)
        } catch (e: Exception) {
            e.printStackTrace()
            Float.MAX_VALUE
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        magnifierBitmap?.recycle()
        magnifierBitmap = null
    }

    fun setOnPointSelectedListener(listener: (DataPoint, Int, Int) -> Unit) {
        onPointSelected = listener
    }

    fun setOnFailedAttemptListener(listener: (Int) -> Unit) {
        onFailedAttempt = listener
    }
} 