package com.example.fyp.experiment

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.sqrt
import kotlin.random.Random

class AdSelectionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var currentTrial = 0
    private var failedAttempts = 0
    private var selectionStartTime = 0L
    private var onSelectionListener: ((Boolean, Int, Int, Long) -> Unit)? = null
    private var lastTouchPoint: PointF? = null
    private var showSuccessMessage = false
    private var successStartTime = 0L
    private val successDuration = 1500L // 1.5 seconds

    // Visual parameters
    private val fingerRadius = 30f  // Simulated finger size
    private val closeButtonSize = 40f
    private val clickableAreaSize = 200f  // Reduced for more focused area
    private val cornerRadius = 16f
    private val shadowRadius = 8f
    private val margin = 40f  // Standard margin for text
    private val buttonHeight = 60f
    private val buttonWidth = 200f

    // Ad content
    private val adTitle = "Special Offer!"
    private val adDescription = "Limited time deal - Don't miss out!"
    private val buttonText = "Click here for more"
    private val productImage = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888).apply {
        val canvas = Canvas(this)
        // Draw gradient background
        val gradient = LinearGradient(0f, 0f, 400f, 400f,
            Color.rgb(240, 240, 240), Color.rgb(220, 220, 220), Shader.TileMode.CLAMP)
        paint.shader = gradient
        canvas.drawRect(0f, 0f, 400f, 400f, paint)
        paint.shader = null
        
        // Draw placeholder for product image
        paint.color = Color.rgb(200, 200, 200)
        canvas.drawRect(50f, 50f, 350f, 350f, paint)
        
        // Draw placeholder text
        paint.color = Color.WHITE
        textPaint.textSize = 48f
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("Add Product Image Here", 200f, 200f, textPaint)
        
        // Draw placeholder icon
        paint.color = Color.rgb(180, 180, 180)
        canvas.drawCircle(200f, 200f, 40f, paint)
        paint.color = Color.WHITE
        textPaint.textSize = 40f
        canvas.drawText("+", 200f, 220f, textPaint)
    }

    private enum class TrialType {
        STANDARD,           // Normal ad with close button
        IMAGE_LINK,        // Clickable image that leads to Amazon
        HIGHLIGHTED        // Highlighted clickable area
    }

    private var currentType = TrialType.STANDARD
    private var isClickable = true

    init {
        setupPaints()
    }

    private fun setupPaints() {
        textPaint.apply {
            textSize = 32f
            color = Color.BLACK
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
    }

    fun startNewTrial() {
        currentType = TrialType.values()[currentTrial % TrialType.values().size]
        failedAttempts = 0
        selectionStartTime = System.currentTimeMillis()
        isClickable = true
        showSuccessMessage = false
        invalidate()
    }

    fun setCurrentTrial(trial: Int) {
        currentTrial = trial
        currentType = TrialType.values()[currentTrial % TrialType.values().size]
        invalidate()
    }

    fun setOnSelectionListener(listener: (Boolean, Int, Int, Long) -> Unit) {
        onSelectionListener = listener
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw background
        canvas.drawColor(Color.WHITE)

        // Draw trial type indicator
        drawTrialTypeIndicator(canvas)

        // Draw ad content
        drawAdContent(canvas)

        // Draw finger indicator
        lastTouchPoint?.let { point ->
            drawFingerIndicator(canvas, point)
        }

        // Draw instructions
        drawInstructions(canvas)

        // Draw results
        drawResults(canvas)

        // Draw success message if needed
        if (showSuccessMessage) {
            drawSuccessMessage(canvas)
        }
    }

    private fun drawTrialTypeIndicator(canvas: Canvas) {
        // Draw trial type indicator with modern styling
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(240, 240, 240)
        canvas.drawRoundRect(40f, 80f, width - 40f, 160f, cornerRadius, cornerRadius, paint)

        textPaint.textSize = 36f
        textPaint.color = Color.rgb(51, 51, 51)
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.isFakeBoldText = true

        val trialType = when (currentType) {
            TrialType.STANDARD -> "Trial 1: Standard Ad (Close button only)"
            TrialType.IMAGE_LINK -> "Trial 2: Clickable Image (Leads to Amazon)"
            TrialType.HIGHLIGHTED -> "Trial 3: Highlighted Clickable Area"
        }
        canvas.drawText(trialType, 60f, 120f, textPaint)
    }

    private fun drawSuccessMessage(canvas: Canvas) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - successStartTime > successDuration) {
            showSuccessMessage = false
            return
        }

        // Draw semi-transparent background with blur effect
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(200, 255, 255, 255)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // Draw success message with modern styling
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(52, 199, 89)
        canvas.drawRoundRect(
            width/2f - 200f, height/2f - 100f,
            width/2f + 200f, height/2f + 100f,
            cornerRadius, cornerRadius, paint
        )

        // Draw success text
        textPaint.color = Color.WHITE
        textPaint.textSize = 48f
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.isFakeBoldText = true
        canvas.drawText("Success!", width / 2f, height / 2f, textPaint)
        
        textPaint.textSize = 32f
        canvas.drawText("Trial ${currentTrial + 1} Complete", width / 2f, height / 2f + 50f, textPaint)
    }

    private fun drawAdContent(canvas: Canvas) {
        val centerX = width / 2f
        val centerY = height / 2f

        // Draw ad background with shadow
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.setShadowLayer(shadowRadius, 0f, 2f, Color.argb(50, 0, 0, 0))
        canvas.drawRoundRect(50f, 50f, width - 50f, height - 50f, cornerRadius, cornerRadius, paint)
        paint.clearShadowLayer()

        // Draw close button with shadow
        paint.setShadowLayer(shadowRadius, 0f, 2f, Color.argb(50, 0, 0, 0))
        paint.color = Color.rgb(240, 240, 240)
        canvas.drawCircle(width - 70f, 70f, closeButtonSize, paint)
        paint.clearShadowLayer()
        paint.color = Color.GRAY
        textPaint.textSize = closeButtonSize
        canvas.drawText("×", width - 70f, 70f + closeButtonSize/3, textPaint)

        // Draw product image with shadow
        paint.setShadowLayer(shadowRadius, 0f, 2f, Color.argb(50, 0, 0, 0))
        canvas.drawBitmap(productImage, centerX - 200f, centerY - 250f, null)
        paint.clearShadowLayer()

        // Draw ad title with gradient (left-aligned)
        val titleGradient = LinearGradient(margin, centerY - 350f, margin + 400f, centerY - 350f,
            Color.rgb(51, 51, 51), Color.rgb(102, 102, 102), Shader.TileMode.CLAMP)
        textPaint.shader = titleGradient
        textPaint.textSize = 48f
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(adTitle, margin, centerY - 350f, textPaint)
        textPaint.shader = null

        // Draw ad description (left-aligned)
        textPaint.color = Color.rgb(102, 102, 102)
        textPaint.textSize = 36f
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(adDescription, margin, centerY - 300f, textPaint)

        // Draw "Click here for more" button
        val buttonY = centerY + 50f
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(0, 150, 255)
        canvas.drawRoundRect(
            centerX - buttonWidth/2, buttonY - buttonHeight/2,
            centerX + buttonWidth/2, buttonY + buttonHeight/2,
            cornerRadius, cornerRadius, paint
        )
        
        textPaint.color = Color.WHITE
        textPaint.textSize = 24f
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(buttonText, centerX, buttonY + 8f, textPaint)

        // Draw clickable area based on trial type
        when (currentType) {
            TrialType.STANDARD -> {
                // Draw close button highlight
                paint.style = Paint.Style.STROKE
                paint.color = Color.rgb(255, 59, 48)  // Red color for close button
                paint.strokeWidth = 3f
                canvas.drawCircle(width - 70f, 70f, closeButtonSize + 10f, paint)
                
                // Draw instruction text
                textPaint.color = Color.rgb(255, 59, 48)
                textPaint.textSize = 24f
                textPaint.textAlign = Paint.Align.CENTER
                canvas.drawText("Click here to close", width - 70f, 70f + closeButtonSize + 30f, textPaint)
            }
            TrialType.IMAGE_LINK -> {
                // Draw clickable image area with highlight
                paint.style = Paint.Style.STROKE
                paint.color = Color.rgb(0, 150, 255)  // Blue color for clickable area
                paint.strokeWidth = 3f
                canvas.drawRect(
                    centerX - clickableAreaSize/2, centerY - 250f,
                    centerX + clickableAreaSize/2, centerY - 250f + clickableAreaSize,
                    paint
                )
                
                // Draw instruction text
                textPaint.color = Color.rgb(0, 150, 255)
                textPaint.textSize = 24f
                textPaint.textAlign = Paint.Align.CENTER
                canvas.drawText("Click here to visit Amazon", centerX, centerY - 250f + clickableAreaSize + 30f, textPaint)
            }
            TrialType.HIGHLIGHTED -> {
                // Draw highlighted clickable area with glow effect
                paint.setShadowLayer(20f, 0f, 0f, Color.argb(100, 0, 150, 255))
                paint.style = Paint.Style.FILL
                paint.color = Color.argb(30, 0, 150, 255)  // Light blue with transparency
                canvas.drawRect(
                    centerX - clickableAreaSize/2, centerY - 250f,
                    centerX + clickableAreaSize/2, centerY - 250f + clickableAreaSize,
                    paint
                )
                paint.clearShadowLayer()
                
                // Draw border
                paint.style = Paint.Style.STROKE
                paint.color = Color.rgb(0, 150, 255)
                paint.strokeWidth = 3f
                canvas.drawRect(
                    centerX - clickableAreaSize/2, centerY - 250f,
                    centerX + clickableAreaSize/2, centerY - 250f + clickableAreaSize,
                    paint
                )
                
                // Draw instruction text
                textPaint.color = Color.rgb(0, 150, 255)
                textPaint.textSize = 24f
                textPaint.textAlign = Paint.Align.CENTER
                canvas.drawText("Click the highlighted area", centerX, centerY - 250f + clickableAreaSize + 30f, textPaint)
            }
        }
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

    private fun drawInstructions(canvas: Canvas) {
        // Draw instructions with modern styling
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(240, 240, 240)
        canvas.drawRoundRect(40f, height - 120f, width - 40f, height - 40f, cornerRadius, cornerRadius, paint)

        textPaint.textSize = 32f
        textPaint.color = Color.rgb(51, 51, 51)
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.isFakeBoldText = true
        
        val instruction = when (currentType) {
            TrialType.STANDARD -> "Tap the X button to close the ad"
            TrialType.IMAGE_LINK -> "Tap the product image to visit Amazon"
            TrialType.HIGHLIGHTED -> "Tap the highlighted area to proceed"
        }
        
        canvas.drawText(instruction, width / 2f, height - 70f, textPaint)
    }

    private fun drawResults(canvas: Canvas) {
        // Draw results with modern styling
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(240, 240, 240)
        canvas.drawRoundRect(40f, 20f, 300f, 80f, cornerRadius, cornerRadius, paint)

        textPaint.textSize = 28f
        textPaint.color = Color.rgb(51, 51, 51)
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.isFakeBoldText = true

        val results = "Failed attempts: $failedAttempts"
        canvas.drawText(results, 60f, 60f, textPaint)

        if (failedAttempts > 0) {
            textPaint.color = Color.rgb(255, 59, 48)
            val message = when (failedAttempts) {
                1 -> "Try again!"
                2 -> "Be more precise!"
                else -> "Watch your finger size!"
            }
            canvas.drawText(message, 60f, 100f, textPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN) {
            return true
        }

        val touchX = event.x
        val touchY = event.y
        lastTouchPoint = PointF(touchX, touchY)

        val centerX = width / 2f
        val centerY = height / 2f

        // Check if close button was tapped
        val closeButtonRect = RectF(
            width - 110f, 30f,
            width - 30f, 110f
        )

        // Check if clickable area was tapped
        val clickableRect = RectF(
            centerX - clickableAreaSize/2, centerY - 250f,
            centerX + clickableAreaSize/2, centerY - 250f + clickableAreaSize
        )

        // Check if "Click here for more" button was tapped
        val moreButtonRect = RectF(
            centerX - buttonWidth/2, centerY + 50f - buttonHeight/2,
            centerX + buttonWidth/2, centerY + 50f + buttonHeight/2
        )

        when (currentType) {
            TrialType.STANDARD -> {
                if (closeButtonRect.contains(touchX, touchY) && isClickable) {
                    val success = true
                    val timeTaken = System.currentTimeMillis() - selectionStartTime
                    onSelectionListener?.invoke(success, currentTrial, failedAttempts, timeTaken)
                    isClickable = false
                    showSuccessMessage = true
                    successStartTime = System.currentTimeMillis()
                    invalidate()
                } else {
                    failedAttempts++
                    invalidate()
                }
            }
            TrialType.IMAGE_LINK, TrialType.HIGHLIGHTED -> {
                if (clickableRect.contains(touchX, touchY) && isClickable) {
                    val success = true
                    val timeTaken = System.currentTimeMillis() - selectionStartTime
                    onSelectionListener?.invoke(success, currentTrial, failedAttempts, timeTaken)
                    isClickable = false
                    showSuccessMessage = true
                    successStartTime = System.currentTimeMillis()
                    invalidate()
                } else {
                    failedAttempts++
                    invalidate()
                }
            }
        }

        return true
    }

    private fun calculateDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2
        val dy = y1 - y2
        return sqrt(dx * dx + dy * dy)
    }
} 