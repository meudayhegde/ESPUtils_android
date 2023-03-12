package com.github.meudayhegde.fab

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.graphics.*
import android.graphics.drawable.*
import android.graphics.drawable.shapes.OvalShape
import android.graphics.drawable.shapes.Shape
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import android.os.SystemClock
import android.util.AttributeSet
import android.view.*
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.ViewGroup.MarginLayoutParams
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.content.res.AppCompatResources
import com.github.meudayhegde.esputils.R
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt


open class FloatingActionButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatImageButton(
    context, attrs, defStyleAttr
) {
    var mFabSize = 0
    var mShowShadow = false
    var mShadowColor = 0
    var mShadowRadius: Int = Util.dpToPx(context, 4f)
    var mShadowXOffset: Int = Util.dpToPx(context, 1f)
    var mShadowYOffset: Int = Util.dpToPx(context, 3f)
    private var mColorNormal = 0
    private var mColorPressed = 0
    private var mColorDisabled = 0
    private var mColorRipple = 0
    private var mIcon: Drawable? = null
    private val mIconSize: Int = Util.dpToPx(context, 24f)
    var showAnimation: Animation? = null
    var hideAnimation: Animation? = null
    private var mLabelText: String? = null
    private var mClickListener: OnClickListener? = null
    private var mBackgroundDrawable: Drawable? = null
    private var mUsingElevation = false
    private var mUsingElevationCompat = false

    // Progress
    private var mProgressBarEnabled = false
    private var mProgressWidth: Int = Util.dpToPx(context, 6f)
    private var mProgressColor = 0
    private var mProgressBackgroundColor = 0
    private var mShouldUpdateButtonPosition = false
    private var mOriginalX = -1f
    private var mOriginalY = -1f
    private var mButtonPositionSaved = false
    private var mProgressCircleBounds = RectF()
    private val mBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mProgressPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var mProgressIndeterminate = false
    private var mLastTimeAnimated: Long = 0
    private var mSpinSpeed = 195.0f //The amount of degrees per second
    private var mPausedTimeWithoutGrowing: Long = 0
    private var mTimeStartGrowing = 0.0
    private var mBarGrowingFromFront = true
    private val mBarLength = 16
    private var mBarExtraLength = 0f
    private var mCurrentProgress = 0f
    private var mTargetProgress = 0f
    private var mProgress = 0
    private var mAnimateProgress = false
    private var mShouldProgressIndeterminate = false
    private var mShouldSetProgress = false

    @get:Synchronized
    @set:Synchronized
    var max = 100

    @get:Synchronized
    var isProgressBackgroundShown = false
        private set

    private fun init(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        val attr =
            context.obtainStyledAttributes(attrs, R.styleable.FloatingActionButton, defStyleAttr, 0)
        mColorNormal = attr.getColor(R.styleable.FloatingActionButton_fab_colorNormal, -0x25bcca)
        mColorPressed = attr.getColor(R.styleable.FloatingActionButton_fab_colorPressed, -0x18afbd)
        mColorDisabled =
            attr.getColor(R.styleable.FloatingActionButton_fab_colorDisabled, -0x555556)
        mColorRipple = attr.getColor(R.styleable.FloatingActionButton_fab_colorRipple, -0x66000001)
        mShowShadow = attr.getBoolean(R.styleable.FloatingActionButton_fab_showShadow, true)
        mShadowColor = attr.getColor(R.styleable.FloatingActionButton_fab_shadowColor, 0x66000000)
        mShadowRadius = attr.getDimensionPixelSize(
            R.styleable.FloatingActionButton_fab_shadowRadius, mShadowRadius
        )
        mShadowXOffset = attr.getDimensionPixelSize(
            R.styleable.FloatingActionButton_fab_shadowXOffset, mShadowXOffset
        )
        mShadowYOffset = attr.getDimensionPixelSize(
            R.styleable.FloatingActionButton_fab_shadowYOffset, mShadowYOffset
        )
        mFabSize = attr.getInt(R.styleable.FloatingActionButton_fab_size, SIZE_NORMAL)
        mLabelText = attr.getString(R.styleable.FloatingActionButton_fab_label)
        mShouldProgressIndeterminate =
            attr.getBoolean(R.styleable.FloatingActionButton_fab_progress_indeterminate, false)
        mProgressColor =
            attr.getColor(R.styleable.FloatingActionButton_fab_progress_color, -0xff6978)
        mProgressBackgroundColor =
            attr.getColor(R.styleable.FloatingActionButton_fab_progress_backgroundColor, 0x4D000000)
        max = attr.getInt(R.styleable.FloatingActionButton_fab_progress_max, max)
        isProgressBackgroundShown =
            attr.getBoolean(R.styleable.FloatingActionButton_fab_progress_showBackground, true)
        if (attr.hasValue(R.styleable.FloatingActionButton_fab_progress)) {
            mProgress = attr.getInt(R.styleable.FloatingActionButton_fab_progress, 0)
            mShouldSetProgress = true
        }
        if (attr.hasValue(R.styleable.FloatingActionButton_fab_elevationCompat)) {
            val elevation = attr.getDimensionPixelOffset(
                R.styleable.FloatingActionButton_fab_elevationCompat, 0
            ).toFloat()
            if (isInEditMode) {
                setElevation(elevation)
            } else {
                setElevationCompat(elevation)
            }
        }
        initShowAnimation(attr)
        initHideAnimation(attr)
        attr.recycle()
        if (isInEditMode) {
            if (mShouldProgressIndeterminate) {
                setIndeterminate(true)
            } else if (mShouldSetProgress) {
                saveButtonOriginalPosition()
                setProgress(mProgress, false)
            }
        }

//        updateBackground();
        isClickable = true
    }

    private fun initShowAnimation(attr: TypedArray) {
        val resourceId = attr.getResourceId(
            R.styleable.FloatingActionButton_fab_showAnimation, R.anim.fab_scale_up
        )
        showAnimation = AnimationUtils.loadAnimation(context, resourceId)
    }

    private fun initHideAnimation(attr: TypedArray) {
        val resourceId = attr.getResourceId(
            R.styleable.FloatingActionButton_fab_hideAnimation, R.anim.fab_scale_down
        )
        hideAnimation = AnimationUtils.loadAnimation(context, resourceId)
    }

    private val circleSize: Int
        get() = resources.getDimensionPixelSize(if (mFabSize == SIZE_NORMAL) R.dimen.fab_size_normal else R.dimen.fab_size_mini)

    private fun calculateMeasuredWidth(): Int {
        var width = circleSize + calculateShadowWidth()
        if (mProgressBarEnabled) {
            width += mProgressWidth * 2
        }
        return width
    }

    private fun calculateMeasuredHeight(): Int {
        var height = circleSize + calculateShadowHeight()
        if (mProgressBarEnabled) {
            height += mProgressWidth * 2
        }
        return height
    }

    fun calculateShadowWidth(): Int {
        return if (hasShadow()) shadowX * 2 else 0
    }

    fun calculateShadowHeight(): Int {
        return if (hasShadow()) shadowY * 2 else 0
    }

    private val shadowX: Int
        get() = mShadowRadius + abs(mShadowXOffset)
    private val shadowY: Int
        get() = mShadowRadius + abs(mShadowYOffset)

    private fun calculateCenterX(): Float {
        return (measuredWidth / 2).toFloat()
    }

    private fun calculateCenterY(): Float {
        return (measuredHeight / 2).toFloat()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(calculateMeasuredWidth(), calculateMeasuredHeight())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mProgressBarEnabled) {
            if (isProgressBackgroundShown) {
                canvas.drawArc(mProgressCircleBounds, 360f, 360f, false, mBackgroundPaint)
            }
            var shouldInvalidate = false
            if (mProgressIndeterminate) {
                shouldInvalidate = true
                val deltaTime = SystemClock.uptimeMillis() - mLastTimeAnimated
                val deltaNormalized = deltaTime * mSpinSpeed / 1000.0f
                updateProgressLength(deltaTime)
                mCurrentProgress += deltaNormalized
                if (mCurrentProgress > 360f) {
                    mCurrentProgress -= 360f
                }
                mLastTimeAnimated = SystemClock.uptimeMillis()
                var from = mCurrentProgress - 90
                var to = mBarLength + mBarExtraLength
                if (isInEditMode) {
                    from = 0f
                    to = 135f
                }
                canvas.drawArc(mProgressCircleBounds, from, to, false, mProgressPaint)
            } else {
                if (mCurrentProgress != mTargetProgress) {
                    shouldInvalidate = true
                    val deltaTime =
                        (SystemClock.uptimeMillis() - mLastTimeAnimated).toFloat() / 1000
                    val deltaNormalized = deltaTime * mSpinSpeed
                    mCurrentProgress = if (mCurrentProgress > mTargetProgress) {
                        (mCurrentProgress - deltaNormalized).coerceAtLeast(mTargetProgress)
                    } else {
                        (mCurrentProgress + deltaNormalized).coerceAtMost(mTargetProgress)
                    }
                    mLastTimeAnimated = SystemClock.uptimeMillis()
                }
                canvas.drawArc(mProgressCircleBounds, -90f, mCurrentProgress, false, mProgressPaint)
            }
            if (shouldInvalidate) {
                invalidate()
            }
        }
    }

    private fun updateProgressLength(deltaTimeInMillis: Long) {
        if (mPausedTimeWithoutGrowing >= PAUSE_GROWING_TIME) {
            mTimeStartGrowing += deltaTimeInMillis.toDouble()
            if (mTimeStartGrowing > BAR_SPIN_CYCLE_TIME) {
                mTimeStartGrowing -= BAR_SPIN_CYCLE_TIME
                mPausedTimeWithoutGrowing = 0
                mBarGrowingFromFront = !mBarGrowingFromFront
            }
            val distance =
                cos((mTimeStartGrowing / BAR_SPIN_CYCLE_TIME + 1) * Math.PI).toFloat() / 2 + 0.5f
            val length = (BAR_MAX_LENGTH - mBarLength).toFloat()
            if (mBarGrowingFromFront) {
                mBarExtraLength = distance * length
            } else {
                val newLength = length * (1 - distance)
                mCurrentProgress += mBarExtraLength - newLength
                mBarExtraLength = newLength
            }
        } else {
            mPausedTimeWithoutGrowing += deltaTimeInMillis
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        saveButtonOriginalPosition()
        if (mShouldProgressIndeterminate) {
            setIndeterminate(true)
            mShouldProgressIndeterminate = false
        } else if (mShouldSetProgress) {
            setProgress(mProgress, mAnimateProgress)
            mShouldSetProgress = false
        } else if (mShouldUpdateButtonPosition) {
            updateButtonPosition()
            mShouldUpdateButtonPosition = false
        }
        super.onSizeChanged(w, h, oldw, oldh)
        setupProgressBounds()
        setupProgressBarPaints()
        updateBackground()
    }

    override fun setLayoutParams(params: ViewGroup.LayoutParams) {
        if (params is MarginLayoutParams && mUsingElevationCompat) {
            params.leftMargin += shadowX
            params.topMargin += shadowY
            params.rightMargin += shadowX
            params.bottomMargin += shadowY
        }
        super.setLayoutParams(params)
    }

    fun updateBackground() {
        val layerDrawable: LayerDrawable = if (hasShadow()) {
            LayerDrawable(
                arrayOf(
                    Shadow(), createFillDrawable(), iconDrawable
                )
            )
        } else {
            LayerDrawable(
                arrayOf(
                    createFillDrawable(), iconDrawable
                )
            )
        }
        var iconSize = -1
        if (iconDrawable != null) {
            iconSize = iconDrawable!!.intrinsicWidth.coerceAtLeast(iconDrawable!!.intrinsicHeight)
        }
        val iconOffset = (circleSize - if (iconSize > 0) iconSize else mIconSize) / 2
        var circleInsetHorizontal = if (hasShadow()) mShadowRadius + abs(mShadowXOffset) else 0
        var circleInsetVertical = if (hasShadow()) mShadowRadius + abs(mShadowYOffset) else 0
        if (mProgressBarEnabled) {
            circleInsetHorizontal += mProgressWidth
            circleInsetVertical += mProgressWidth
        }

        /*layerDrawable.setLayerInset(
                mShowShadow ? 1 : 0,
                circleInsetHorizontal,
                circleInsetVertical,
                circleInsetHorizontal,
                circleInsetVertical
        );*/layerDrawable.setLayerInset(
            if (hasShadow()) 2 else 1,
            circleInsetHorizontal + iconOffset,
            circleInsetVertical + iconOffset,
            circleInsetHorizontal + iconOffset,
            circleInsetVertical + iconOffset
        )
        setBackgroundCompat(layerDrawable)
    }

    private val iconDrawable: Drawable?
        get() = if (mIcon != null) {
            mIcon
        } else {
            ColorDrawable(Color.TRANSPARENT)
        }

    private fun createFillDrawable(): Drawable {
        val drawable = StateListDrawable()
        drawable.addState(
            intArrayOf(-android.R.attr.state_enabled), createCircleDrawable(mColorDisabled)
        )
        drawable.addState(
            intArrayOf(android.R.attr.state_pressed), createCircleDrawable(mColorPressed)
        )
        drawable.addState(intArrayOf(), createCircleDrawable(mColorNormal))
        val ripple = RippleDrawable(
            ColorStateList(arrayOf(intArrayOf()), intArrayOf(mColorRipple)), drawable, null
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

    private fun createCircleDrawable(color: Int): Drawable {
        val shapeDrawable = CircleDrawable(OvalShape())
        shapeDrawable.paint.color = color
        return shapeDrawable
    }

    private fun setBackgroundCompat(drawable: Drawable) {
        background = drawable
    }

    private fun saveButtonOriginalPosition() {
        if (!mButtonPositionSaved) {
            if (mOriginalX == -1f) {
                mOriginalX = x
            }
            if (mOriginalY == -1f) {
                mOriginalY = y
            }
            mButtonPositionSaved = true
        }
    }

    private fun updateButtonPosition() {
        val x: Float
        val y: Float
        if (mProgressBarEnabled) {
            x = if (mOriginalX > getX()) getX() + mProgressWidth else getX() - mProgressWidth
            y = if (mOriginalY > getY()) getY() + mProgressWidth else getY() - mProgressWidth
        } else {
            x = mOriginalX
            y = mOriginalY
        }
        setX(x)
        setY(y)
    }

    private fun setupProgressBarPaints() {
        mBackgroundPaint.color = mProgressBackgroundColor
        mBackgroundPaint.style = Paint.Style.STROKE
        mBackgroundPaint.strokeWidth = mProgressWidth.toFloat()
        mProgressPaint.color = mProgressColor
        mProgressPaint.style = Paint.Style.STROKE
        mProgressPaint.strokeWidth = mProgressWidth.toFloat()
    }

    private fun setupProgressBounds() {
        val circleInsetHorizontal = if (hasShadow()) shadowX else 0
        val circleInsetVertical = if (hasShadow()) shadowY else 0
        mProgressCircleBounds = RectF(
            (circleInsetHorizontal + mProgressWidth / 2).toFloat(),
            (circleInsetVertical + mProgressWidth / 2).toFloat(),
            (calculateMeasuredWidth() - circleInsetHorizontal - mProgressWidth / 2).toFloat(),
            (calculateMeasuredHeight() - circleInsetVertical - mProgressWidth / 2).toFloat()
        )
    }

    fun playShowAnimation() {
        hideAnimation!!.cancel()
        startAnimation(showAnimation)
    }

    fun playHideAnimation() {
        showAnimation!!.cancel()
        startAnimation(hideAnimation)
    }

    fun getOnClickListener(): OnClickListener? {
        return mClickListener
    }

    val labelView
        get() = getTag(R.id.fab_label) as Label?

    fun setColors(colorNormal: Int, colorPressed: Int, colorRipple: Int) {
        mColorNormal = colorNormal
        mColorPressed = colorPressed
        mColorRipple = colorRipple
    }

    fun onActionDown() {
        if (mBackgroundDrawable is StateListDrawable) {
            val drawable = mBackgroundDrawable as StateListDrawable
            drawable.state = intArrayOf(android.R.attr.state_enabled, android.R.attr.state_pressed)
        } else {
            val ripple = mBackgroundDrawable as RippleDrawable?
            ripple!!.state = intArrayOf(android.R.attr.state_enabled, android.R.attr.state_pressed)
            ripple.setHotspot(calculateCenterX(), calculateCenterY())
            ripple.setVisible(true, true)
        }
    }

    fun onActionUp() {
        if (mBackgroundDrawable is StateListDrawable) {
            val drawable = mBackgroundDrawable as StateListDrawable
            drawable.state = intArrayOf(android.R.attr.state_enabled)
        } else {
            val ripple = mBackgroundDrawable as RippleDrawable?
            ripple!!.state = intArrayOf(android.R.attr.state_enabled)
            ripple.setHotspot(calculateCenterX(), calculateCenterY())
            ripple.setVisible(true, true)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (mClickListener != null && isEnabled) {
            val label = getTag(R.id.fab_label) as Label?
            when (event.action) {
                MotionEvent.ACTION_UP -> {
                    label?.onActionUp()
                    onActionUp()
                }
                MotionEvent.ACTION_CANCEL -> {
                    label?.onActionUp()
                    onActionUp()
                }
            }
            mGestureDetector.onTouchEvent(event)
        }
        return super.onTouchEvent(event)
    }

    var mGestureDetector = GestureDetector(context, object : SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            (getTag(R.id.fab_label) as Label?)?.onActionDown()
            onActionDown()
            return super.onDown(e)
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            (getTag(R.id.fab_label) as Label?)?.onActionUp()
            onActionUp()
            return super.onSingleTapUp(e)
        }
    })

    public override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        val ss = ProgressSavedState(superState)
        ss.mCurrentProgress = mCurrentProgress
        ss.mTargetProgress = mTargetProgress
        ss.mSpinSpeed = mSpinSpeed
        ss.mProgressWidth = mProgressWidth
        ss.mProgressColor = mProgressColor
        ss.mProgressBackgroundColor = mProgressBackgroundColor
        ss.mShouldProgressIndeterminate = mProgressIndeterminate
        ss.mShouldSetProgress = mProgressBarEnabled && mProgress > 0 && !mProgressIndeterminate
        ss.mProgress = mProgress
        ss.mAnimateProgress = mAnimateProgress
        ss.mShowProgressBackground = isProgressBackgroundShown
        return ss
    }

    public override fun onRestoreInstanceState(state: Parcelable) {
        if (state !is ProgressSavedState) {
            super.onRestoreInstanceState(state)
            return
        }
        super.onRestoreInstanceState(state.superState)
        mCurrentProgress = state.mCurrentProgress
        mTargetProgress = state.mTargetProgress
        mSpinSpeed = state.mSpinSpeed
        mProgressWidth = state.mProgressWidth
        mProgressColor = state.mProgressColor
        mProgressBackgroundColor = state.mProgressBackgroundColor
        mShouldProgressIndeterminate = state.mShouldProgressIndeterminate
        mShouldSetProgress = state.mShouldSetProgress
        mProgress = state.mProgress
        mAnimateProgress = state.mAnimateProgress
        isProgressBackgroundShown = state.mShowProgressBackground
        mLastTimeAnimated = SystemClock.uptimeMillis()
    }

    private inner class CircleDrawable(s: Shape) : ShapeDrawable(s) {
        private var circleInsetHorizontal = 0
        private var circleInsetVertical = 0

        init {
            circleInsetHorizontal = if (hasShadow()) mShadowRadius + abs(mShadowXOffset) else 0
            circleInsetVertical = if (hasShadow()) mShadowRadius + abs(mShadowYOffset) else 0
            if (mProgressBarEnabled) {
                circleInsetHorizontal += mProgressWidth
                circleInsetVertical += mProgressWidth
            }
        }

        override fun draw(canvas: Canvas) {
            setBounds(
                circleInsetHorizontal,
                circleInsetVertical,
                calculateMeasuredWidth() - circleInsetHorizontal,
                calculateMeasuredHeight() - circleInsetVertical
            )
            super.draw(canvas)
        }
    }

    private inner class Shadow : Drawable() {
        private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val mErase = Paint(Paint.ANTI_ALIAS_FLAG)
        private var mRadius = 0f

        init {
            this.init()
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
            mRadius = (circleSize / 2).toFloat()
            if (mProgressBarEnabled && isProgressBackgroundShown) {
                mRadius += mProgressWidth.toFloat()
            }
        }

        override fun draw(canvas: Canvas) {
            canvas.drawCircle(calculateCenterX(), calculateCenterY(), mRadius, mPaint)
            canvas.drawCircle(calculateCenterX(), calculateCenterY(), mRadius, mErase)
        }

        override fun setAlpha(alpha: Int) {}
        override fun setColorFilter(cf: ColorFilter?) {}

        @Deprecated(
            "Deprecated in Java", ReplaceWith("PixelFormat.UNKNOWN", "android.graphics.PixelFormat")
        )
        override fun getOpacity(): Int {
            return PixelFormat.UNKNOWN
        }
    }

    internal class ProgressSavedState : BaseSavedState {
        var mCurrentProgress = 0f
        var mTargetProgress = 0f
        var mSpinSpeed = 0f
        var mProgress = 0
        var mProgressWidth = 0
        var mProgressColor = 0
        var mProgressBackgroundColor = 0
        var mProgressBarEnabled = false
        var mProgressBarVisibilityChanged = false
        var mProgressIndeterminate = false
        var mShouldProgressIndeterminate = false
        var mShouldSetProgress = false
        var mAnimateProgress = false
        var mShowProgressBackground = false

        constructor(superState: Parcelable?) : super(superState)
        private constructor(`in`: Parcel) : super(`in`) {
            mCurrentProgress = `in`.readFloat()
            mTargetProgress = `in`.readFloat()
            mProgressBarEnabled = `in`.readInt() != 0
            mSpinSpeed = `in`.readFloat()
            mProgress = `in`.readInt()
            mProgressWidth = `in`.readInt()
            mProgressColor = `in`.readInt()
            mProgressBackgroundColor = `in`.readInt()
            mProgressBarVisibilityChanged = `in`.readInt() != 0
            mProgressIndeterminate = `in`.readInt() != 0
            mShouldProgressIndeterminate = `in`.readInt() != 0
            mShouldSetProgress = `in`.readInt() != 0
            mAnimateProgress = `in`.readInt() != 0
            mShowProgressBackground = `in`.readInt() != 0
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeFloat(mCurrentProgress)
            out.writeFloat(mTargetProgress)
            out.writeInt(if (mProgressBarEnabled) 1 else 0)
            out.writeFloat(mSpinSpeed)
            out.writeInt(mProgress)
            out.writeInt(mProgressWidth)
            out.writeInt(mProgressColor)
            out.writeInt(mProgressBackgroundColor)
            out.writeInt(if (mProgressBarVisibilityChanged) 1 else 0)
            out.writeInt(if (mProgressIndeterminate) 1 else 0)
            out.writeInt(if (mShouldProgressIndeterminate) 1 else 0)
            out.writeInt(if (mShouldSetProgress) 1 else 0)
            out.writeInt(if (mAnimateProgress) 1 else 0)
            out.writeInt(if (mShowProgressBackground) 1 else 0)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Creator<ProgressSavedState> {
            override fun createFromParcel(parcel: Parcel): ProgressSavedState {
                return ProgressSavedState(parcel)
            }

            override fun newArray(size: Int): Array<ProgressSavedState?> {
                return arrayOfNulls(size)
            }
        }
    }

    /* ===== API methods ===== */
    override fun setImageDrawable(drawable: Drawable?) {
        if (mIcon !== drawable) {
            mIcon = drawable
            updateBackground()
        }
    }

    override fun setImageResource(resId: Int) {
        val drawable = AppCompatResources.getDrawable(context, resId)
        if (mIcon !== drawable) {
            mIcon = drawable
            updateBackground()
        }
    }

    override fun setOnClickListener(l: OnClickListener?) {
        super.setOnClickListener(l)
        mClickListener = l
        val label = getTag(R.id.fab_label) as View
        label.setOnClickListener {
            if (mClickListener != null) {
                mClickListener!!.onClick(this@FloatingActionButton)
            }
        }
    }

    /**
     * Sets the size of the **FloatingActionButton** and invalidates its layout.
     *
     * @param size size of the **FloatingActionButton**. Accepted values: SIZE_NORMAL, SIZE_MINI.
     */
    var buttonSize: Int
        get() = mFabSize
        set(size) {
            require(!(size != SIZE_NORMAL && size != SIZE_MINI)) { "Use @FabSize constants only!" }
            if (mFabSize != size) {
                mFabSize = size
                updateBackground()
            }
        }

    fun setColorNormalResId(colorResId: Int) {
        colorNormal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.getColor(colorResId)
        } else resources.getColor(colorResId)
    }

    var colorNormal: Int
        get() = mColorNormal
        set(color) {
            if (mColorNormal != color) {
                mColorNormal = color
                updateBackground()
            }
        }

    fun setColorPressedResId(colorResId: Int) {
        colorPressed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.getColor(colorResId)
        } else resources.getColor(colorResId)
    }

    var colorPressed: Int
        get() = mColorPressed
        set(color) {
            if (color != mColorPressed) {
                mColorPressed = color
                updateBackground()
            }
        }

    fun setColorRippleResId(colorResId: Int) {
        colorRipple = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.getColor(colorResId)
        } else resources.getColor(colorResId)
    }

    var colorRipple: Int
        get() = mColorRipple
        set(color) {
            if (color != mColorRipple) {
                mColorRipple = color
                updateBackground()
            }
        }

    fun setColorDisabledResId(colorResId: Int) {
        colorDisabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.getColor(colorResId)
        } else resources.getColor(colorResId)
    }

    var colorDisabled: Int
        get() = mColorDisabled
        set(color) {
            if (color != mColorDisabled) {
                mColorDisabled = color
                updateBackground()
            }
        }

    fun setShowShadow(show: Boolean) {
        if (mShowShadow != show) {
            mShowShadow = show
            updateBackground()
        }
    }

    fun hasShadow(): Boolean {
        return !mUsingElevation && mShowShadow
    }

    /**
     * Sets the shadow radius of the **FloatingActionButton** and invalidates its layout.
     *
     *
     * Must be specified in density-independent (dp) pixels, which are then converted into actual
     * pixels (px).
     *
     * @param shadowRadiusDp shadow radius specified in density-independent (dp) pixels
     */
    fun setShadowRadius(shadowRadiusDp: Float) {
        mShadowRadius = Util.dpToPx(context, shadowRadiusDp)
        requestLayout()
        updateBackground()
    }

    /**
     * Sets the shadow radius of the **FloatingActionButton** and invalidates its layout.
     *
     * @param dimenResId the resource identifier of the dimension
     */
    var shadowRadius: Int
        get() = mShadowRadius
        set(dimenResId) {
            val shadowRadius = resources.getDimensionPixelSize(dimenResId)
            if (mShadowRadius != shadowRadius) {
                mShadowRadius = shadowRadius
                requestLayout()
                updateBackground()
            }
        }

    /**
     * Sets the shadow x offset of the **FloatingActionButton** and invalidates its layout.
     *
     *
     * Must be specified in density-independent (dp) pixels, which are then converted into actual
     * pixels (px).
     *
     * @param shadowXOffsetDp shadow radius specified in density-independent (dp) pixels
     */
    fun setShadowXOffset(shadowXOffsetDp: Float) {
        mShadowXOffset = Util.dpToPx(context, shadowXOffsetDp)
        requestLayout()
        updateBackground()
    }

    /**
     * Sets the shadow x offset of the **FloatingActionButton** and invalidates its layout.
     *
     * @param dimenResId the resource identifier of the dimension
     */
    var shadowXOffset: Int
        get() = mShadowXOffset
        set(dimenResId) {
            val shadowXOffset = resources.getDimensionPixelSize(dimenResId)
            if (mShadowXOffset != shadowXOffset) {
                mShadowXOffset = shadowXOffset
                requestLayout()
                updateBackground()
            }
        }

    /**
     * Sets the shadow y offset of the **FloatingActionButton** and invalidates its layout.
     *
     *
     * Must be specified in density-independent (dp) pixels, which are then converted into actual
     * pixels (px).
     *
     * @param shadowYOffsetDp shadow radius specified in density-independent (dp) pixels
     */
    fun setShadowYOffset(shadowYOffsetDp: Float) {
        mShadowYOffset = Util.dpToPx(context, shadowYOffsetDp)
        requestLayout()
        updateBackground()
    }

    /**
     * Sets the shadow y offset of the **FloatingActionButton** and invalidates its layout.
     *
     * @param dimenResId the resource identifier of the dimension
     */
    var shadowYOffset: Int
        get() = mShadowYOffset
        set(dimenResId) {
            val shadowYOffset = resources.getDimensionPixelSize(dimenResId)
            if (mShadowYOffset != shadowYOffset) {
                mShadowYOffset = shadowYOffset
                requestLayout()
                updateBackground()
            }
        }

    fun setShadowColorResource(colorResId: Int) {
        val shadowColor = resources.getColor(colorResId)
        if (mShadowColor != shadowColor) {
            mShadowColor = shadowColor
            updateBackground()
        }
    }

    var shadowColor: Int
        get() = mShadowColor
        set(color) {
            if (mShadowColor != color) {
                mShadowColor = color
                updateBackground()
            }
        }

    /**
     * Checks whether **FloatingActionButton** is hidden
     *
     * @return true if **FloatingActionButton** is hidden, false otherwise
     */
    val isHidden: Boolean
        get() = visibility == INVISIBLE

    /**
     * Makes the **FloatingActionButton** to appear and sets its visibility to [.VISIBLE]
     *
     * @param animate if true - plays "show animation"
     */
    fun show(animate: Boolean) {
        if (isHidden) {
            if (animate) {
                playShowAnimation()
            }
            super.setVisibility(VISIBLE)
        }
    }

    /**
     * Makes the **FloatingActionButton** to disappear and sets its visibility to [.INVISIBLE]
     *
     * @param animate if true - plays "hide animation"
     */
    fun hide(animate: Boolean) {
        if (!isHidden) {
            if (animate) {
                playHideAnimation()
            }
            super.setVisibility(INVISIBLE)
        }
    }

    fun toggle(animate: Boolean) {
        if (isHidden) {
            show(animate)
        } else {
            hide(animate)
        }
    }

    var labelText: String?
        get() = mLabelText
        set(text) {
            mLabelText = text
            labelView?.text = text
        }
    var labelVisibility: Int
        get() {
            return this.labelView!!.visibility
        }
        set(visibility) {
            val labelView = labelView
            if (labelView != null) {
                labelView.visibility = visibility
                labelView.setHandleVisibilityChanges(visibility == VISIBLE)
            }
        }

    override fun setElevation(elevation: Float) {
        super.setElevation(elevation)
        if (!isInEditMode) {
            mUsingElevation = true
            mShowShadow = false
        }
        updateBackground()
    }

    /**
     * Sets the shadow color and radius to mimic the native elevation.
     *
     *
     * **API 21+**: Sets the native elevation of this view, in pixels. Updates margins to
     * make the view hold its position in layout across different platform versions.
     */
    fun setElevationCompat(elevation: Float) {
        mShadowColor = 0x26000000
        mShadowRadius = (elevation / 2).roundToInt()
        mShadowXOffset = 0
        mShadowYOffset = (if (mFabSize == SIZE_NORMAL) elevation else elevation / 2).roundToInt()
        super.setElevation(elevation)
        mUsingElevationCompat = true
        mShowShadow = false
        updateBackground()
        val layoutParams = layoutParams
        layoutParams?.let { setLayoutParams(it) }
    }

    /**
     *
     * Change the indeterminate mode for the progress bar. In indeterminate
     * mode, the progress is ignored and the progress bar shows an infinite
     * animation instead.
     *
     * @param indeterminate true to enable the indeterminate mode
     */
    @Synchronized
    fun setIndeterminate(indeterminate: Boolean) {
        if (!indeterminate) {
            mCurrentProgress = 0.0f
        }
        mProgressBarEnabled = indeterminate
        mShouldUpdateButtonPosition = true
        mProgressIndeterminate = indeterminate
        mLastTimeAnimated = SystemClock.uptimeMillis()
        setupProgressBounds()
        //        saveButtonOriginalPosition();
        updateBackground()
    }

    @Synchronized
    fun setProgress(_progress: Int, animate: Boolean) {
        var progress = _progress
        if (mProgressIndeterminate) return
        mProgress = progress
        mAnimateProgress = animate
        if (!mButtonPositionSaved) {
            mShouldSetProgress = true
            return
        }
        mProgressBarEnabled = true
        mShouldUpdateButtonPosition = true
        setupProgressBounds()
        saveButtonOriginalPosition()
        updateBackground()
        if (progress < 0) {
            progress = 0
        } else if (progress > max) {
            progress = max
        }
        if (progress.toFloat() == mTargetProgress) {
            return
        }
        mTargetProgress = if (max > 0) progress / max.toFloat() * 360 else 0F
        mLastTimeAnimated = SystemClock.uptimeMillis()
        if (!animate) {
            mCurrentProgress = mTargetProgress
        }
        invalidate()
    }

    @get:Synchronized
    val progress: Int
        get() = if (mProgressIndeterminate) 0 else mProgress

    @Synchronized
    fun hideProgress() {
        mProgressBarEnabled = false
        mShouldUpdateButtonPosition = true
        updateBackground()
    }

    @Synchronized
    fun setShowProgressBackground(show: Boolean) {
        isProgressBackgroundShown = show
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        val label = getTag(R.id.fab_label) as Label?
        if (label != null) {
            label.isEnabled = enabled
        }
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        val label = getTag(R.id.fab_label) as Label?
        if (label != null) {
            label.visibility = visibility
        }
    }

    /**
     * **This will clear all AnimationListeners.**
     */
    fun hideButtonInMenu(animate: Boolean) {
        if (!isHidden && visibility != GONE) {
            hide(animate)
            labelView?.hide(animate)
            hideAnimation!!.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {}
                override fun onAnimationEnd(animation: Animation) {
                    visibility = GONE
                    hideAnimation?.setAnimationListener(null)
                }

                override fun onAnimationRepeat(animation: Animation) {}
            })
        }
    }

    fun showButtonInMenu(animate: Boolean) {
        if (visibility == VISIBLE) return
        visibility = INVISIBLE
        show(animate)
        labelView?.show(animate)
    }

    /**
     * Set the label's background colors
     */
    fun setLabelColors(colorNormal: Int, colorPressed: Int, colorRipple: Int) {
        val left = labelView?.paddingLeft
        val top = labelView?.paddingTop
        val right = labelView?.paddingRight
        val bottom = labelView?.paddingBottom
        labelView?.setColors(colorNormal, colorPressed, colorRipple)
        labelView?.updateBackground()
        labelView?.setPadding(left ?: 0, top ?: 0, right ?: 0, bottom ?: 0)
    }

    fun setLabelTextColor(color: Int) {
        labelView?.setTextColor(color)
    }

    fun setLabelTextColor(colors: ColorStateList?) {
        labelView?.setTextColor(colors)
    }

    companion object {
        const val SIZE_NORMAL = 0
        const val SIZE_MINI = 1
        private val PORTER_DUFF_CLEAR: Xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        private const val PAUSE_GROWING_TIME: Long = 200
        private const val BAR_SPIN_CYCLE_TIME = 500.0
        private const val BAR_MAX_LENGTH = 270
    }

    init {
        init(context, attrs, defStyleAttr)
    }
}
