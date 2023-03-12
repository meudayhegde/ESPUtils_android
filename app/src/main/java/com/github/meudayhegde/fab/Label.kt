package com.github.meudayhegde.fab

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.*
import android.graphics.drawable.shapes.RoundRectShape
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.animation.Animation
import kotlin.math.abs

class Label : androidx.appcompat.widget.AppCompatTextView {
    private var mShadowRadius = 0
    private var mShadowXOffset = 0
    private var mShadowYOffset = 0
    private var mShadowColor = 0
    private var mBackgroundDrawable: Drawable? = null
    private var mShowShadow = true
    private var mRawWidth = 0
    private var mRawHeight = 0
    private var mColorNormal = 0
    private var mColorPressed = 0
    private var mColorRipple = 0
    private var mCornerRadius = 0
    private var mFab: FloatingActionButton? = null
    private var mShowAnimation: Animation? = null
    private var mHideAnimation: Animation? = null
    private var mUsingStyle = false
    var mHandleVisibilityChanges = true

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(calculateMeasuredWidth(), calculateMeasuredHeight())
    }

    private fun calculateMeasuredWidth(): Int {
        if (mRawWidth == 0) {
            mRawWidth = measuredWidth
        }
        return measuredWidth + calculateShadowWidth()
    }

    private fun calculateMeasuredHeight(): Int {
        if (mRawHeight == 0) {
            mRawHeight = measuredHeight
        }
        return measuredHeight + calculateShadowHeight()
    }

    fun calculateShadowWidth(): Int {
        return if (mShowShadow) mShadowRadius + abs(mShadowXOffset) else 0
    }

    fun calculateShadowHeight(): Int {
        return if (mShowShadow) mShadowRadius + abs(mShadowYOffset) else 0
    }

    fun updateBackground() {
        val layerDrawable: LayerDrawable
        if (mShowShadow) {
            layerDrawable = LayerDrawable(
                arrayOf(
                    Shadow(),
                    createFillDrawable()
                )
            )
            val leftInset = mShadowRadius + abs(mShadowXOffset)
            val topInset = mShadowRadius + abs(mShadowYOffset)
            val rightInset = mShadowRadius + abs(mShadowXOffset)
            val bottomInset = mShadowRadius + abs(mShadowYOffset)
            layerDrawable.setLayerInset(
                1,
                leftInset,
                topInset,
                rightInset,
                bottomInset
            )
        } else {
            layerDrawable = LayerDrawable(
                arrayOf(
                    createFillDrawable()
                )
            )
        }
        setBackgroundCompat(layerDrawable)
    }

    private fun createFillDrawable(): Drawable {
        val drawable = StateListDrawable()
        drawable.addState(intArrayOf(android.R.attr.state_pressed), createRectDrawable(mColorPressed))
        drawable.addState(intArrayOf(), createRectDrawable(mColorNormal))
        val ripple = RippleDrawable(
            ColorStateList(arrayOf(intArrayOf()), intArrayOf(mColorRipple)),
            drawable,
            null
        )
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setOval(0, 0, view.width, view.height)
            }
        }
        clipToOutline = true
        mBackgroundDrawable = ripple
        return ripple
    }

    private fun createRectDrawable(color: Int): Drawable {
        val shape = RoundRectShape(
            floatArrayOf(
                mCornerRadius.toFloat(),
                mCornerRadius.toFloat(),
                mCornerRadius.toFloat(),
                mCornerRadius.toFloat(),
                mCornerRadius.toFloat(),
                mCornerRadius.toFloat(),
                mCornerRadius.toFloat(),
                mCornerRadius
                    .toFloat()
            ),
            null,
            null
        )
        val shapeDrawable = ShapeDrawable(shape)
        shapeDrawable.paint.color = color
        return shapeDrawable
    }

    private fun setShadow(fab: FloatingActionButton) {
        mShadowColor = fab.shadowColor
        mShadowRadius = fab.shadowRadius
        mShadowXOffset = fab.shadowXOffset
        mShadowYOffset = fab.shadowYOffset
        mShowShadow = fab.hasShadow()
    }

    private fun setBackgroundCompat(drawable: Drawable) {
            background = drawable
    }

    private fun playShowAnimation() {
        if (mShowAnimation != null) {
            mHideAnimation!!.cancel()
            startAnimation(mShowAnimation)
        }
    }

    private fun playHideAnimation() {
        if (mHideAnimation != null) {
            mShowAnimation!!.cancel()
            startAnimation(mHideAnimation)
        }
    }

    fun onActionDown() {
        if (mUsingStyle) {
            mBackgroundDrawable = background
        }
        if (mBackgroundDrawable is StateListDrawable) {
            val drawable = mBackgroundDrawable as StateListDrawable
            drawable.state = intArrayOf(android.R.attr.state_pressed)
        } else if (mBackgroundDrawable is RippleDrawable) {
            val ripple = mBackgroundDrawable as RippleDrawable
            ripple.state = intArrayOf(android.R.attr.state_enabled, android.R.attr.state_pressed)
            ripple.setHotspot((measuredWidth / 2).toFloat(), (measuredHeight / 2).toFloat())
            ripple.setVisible(true, true)
        }
        //        setPressed(true);
    }

    fun onActionUp() {
        if (mUsingStyle) {
            mBackgroundDrawable = background
        }
        if (mBackgroundDrawable is StateListDrawable) {
            val drawable = mBackgroundDrawable as StateListDrawable
            drawable.state = intArrayOf()
        } else if (mBackgroundDrawable is RippleDrawable) {
            val ripple = mBackgroundDrawable as RippleDrawable
            ripple.state = intArrayOf()
            ripple.setHotspot((measuredWidth / 2).toFloat(), (measuredHeight / 2).toFloat())
            ripple.setVisible(true, true)
        }
        //        setPressed(false);
    }

    fun setFab(fab: FloatingActionButton) {
        mFab = fab
        setShadow(fab)
    }

    fun setShowShadow(show: Boolean) {
        mShowShadow = show
    }

    fun setCornerRadius(cornerRadius: Int) {
        mCornerRadius = cornerRadius
    }

    fun setColors(colorNormal: Int, colorPressed: Int, colorRipple: Int) {
        mColorNormal = colorNormal
        mColorPressed = colorPressed
        mColorRipple = colorRipple
    }

    fun show(animate: Boolean) {
        if (animate) {
            playShowAnimation()
        }
        visibility = VISIBLE
    }

    fun hide(animate: Boolean) {
        if (animate) {
            playHideAnimation()
        }
        visibility = INVISIBLE
    }

    fun setShowAnimation(showAnimation: Animation?) {
        mShowAnimation = showAnimation
    }

    fun setHideAnimation(hideAnimation: Animation?) {
        mHideAnimation = hideAnimation
    }

    fun setUsingStyle(usingStyle: Boolean) {
        mUsingStyle = usingStyle
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (mFab == null || mFab?.getOnClickListener() == null || !mFab!!.isEnabled) {
            return super.onTouchEvent(event)
        }
        when (event.action) {
            MotionEvent.ACTION_UP -> {
                onActionUp()
                mFab?.onActionUp()
            }
            MotionEvent.ACTION_CANCEL -> {
                onActionUp()
                mFab?.onActionUp()
            }
        }
        mGestureDetector.onTouchEvent(event)
        return super.onTouchEvent(event)
    }

    fun setHandleVisibilityChanges(handle: Boolean) {
        mHandleVisibilityChanges = handle
    }

    fun isHandleVisibilityChanges(): Boolean {
        return mHandleVisibilityChanges
    }

    var mGestureDetector = GestureDetector(context, object : SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            onActionDown()
            if (mFab != null) {
                mFab?.onActionDown()
            }
            return super.onDown(e)
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            onActionUp()
            if (mFab != null) {
                mFab?.onActionUp()
            }
            return super.onSingleTapUp(e)
        }
    })

    private inner class Shadow : Drawable() {
        private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val mErase = Paint(Paint.ANTI_ALIAS_FLAG)

        init {
            init()
        }

        private fun init() {
            setLayerType(LAYER_TYPE_SOFTWARE, null)
            mPaint.style = Paint.Style.FILL
            mPaint.color = mColorNormal
            mErase.xfermode = PORTER_DUFF_CLEAR
            if (!isInEditMode) {
                mPaint.setShadowLayer(
                    mShadowRadius.toFloat(),
                    mShadowXOffset.toFloat(),
                    mShadowYOffset.toFloat(),
                    mShadowColor
                )
            }
        }

        override fun draw(canvas: Canvas) {
            val shadowRect = RectF(
                (
                        mShadowRadius + abs(mShadowXOffset)).toFloat(),
                (
                        mShadowRadius + abs(mShadowYOffset)).toFloat(),
                mRawWidth.toFloat(),
                mRawHeight
                    .toFloat()
            )
            canvas.drawRoundRect(
                shadowRect,
                mCornerRadius.toFloat(),
                mCornerRadius.toFloat(),
                mPaint
            )
            canvas.drawRoundRect(
                shadowRect,
                mCornerRadius.toFloat(),
                mCornerRadius.toFloat(),
                mErase
            )
        }

        override fun setAlpha(alpha: Int) {}
        override fun setColorFilter(cf: ColorFilter?) {}
        @Deprecated("Deprecated in Java",
            ReplaceWith("PixelFormat.UNKNOWN", "android.graphics.PixelFormat")
        )
        override fun getOpacity(): Int {
            return PixelFormat.UNKNOWN
        }
    }

    companion object {
        private val PORTER_DUFF_CLEAR: Xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }
}
