package com.github.meudayhegde.fab

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.*
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import com.github.meudayhegde.esputils.R
import kotlin.math.abs


class FloatingActionMenu @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    ViewGroup(context, attrs, defStyleAttr) {
    private val mOpenAnimatorSet = AnimatorSet()
    private val mCloseAnimatorSet = AnimatorSet()
    var iconToggleAnimatorSet: AnimatorSet? = null
    private var mButtonSpacing: Int = Util.dpToPx(getContext(), 0f)
    private var mMenuButton: FloatingActionButton? = null
    private var mMaxButtonWidth = 0
    private var mLabelsMargin: Int = Util.dpToPx(getContext(), 0f)
    private val mLabelsVerticalOffset: Int = Util.dpToPx(getContext(), 0f)
    private var mButtonsCount = 0

    /* ===== API methods ===== */
    var isOpened = false
        private set
    private var mIsMenuOpening = false
    private val mUiHandler = Handler(Looper.getMainLooper())
    private var mLabelsShowAnimation = 0
    private var mLabelsHideAnimation = 0
    private var mLabelsPaddingTop: Int = Util.dpToPx(getContext(), 4f)
    private var mLabelsPaddingRight: Int = Util.dpToPx(getContext(), 8f)
    private var mLabelsPaddingBottom: Int = Util.dpToPx(getContext(), 4f)
    private var mLabelsPaddingLeft: Int = Util.dpToPx(getContext(), 8f)
    private var mLabelsTextColor: ColorStateList? = null
    private var mLabelsTextSize = 0f
    private var mLabelsCornerRadius: Int = Util.dpToPx(getContext(), 6f)
    private var mLabelsShowShadow = false
    private var mLabelsColorNormal = 0
    private var mLabelsColorPressed = 0
    private var mLabelsColorRipple = 0
    private var mMenuShowShadow = false
    private var mMenuShadowColor = 0
    private var mMenuShadowRadius = 4f
    private var mMenuShadowXOffset = 1f
    private var mMenuShadowYOffset = 3f
    private var mMenuColorNormal = 0
    private var mMenuColorPressed = 0
    private var mMenuColorRipple = 0
    private var mIcon: Drawable? = null
    var animationDelayPerItem = 0
    private var mOpenInterpolator: Interpolator? = null
    private var mCloseInterpolator: Interpolator? = null
    private var mIsAnimated = true
    private var mLabelsSingleLine = false
    private var mLabelsEllipsize = 0
    private var mLabelsMaxLines = 0
    private var mMenuFabSize = 0
    private var mLabelsStyle = 0
    private var mCustomTypefaceFromFont: Typeface? = null
    var isIconAnimated = true
    var menuIconView: ImageView? = null
        private set
    private var mMenuButtonShowAnimation: Animation? = null
    private var mMenuButtonHideAnimation: Animation? = null
    private var mImageToggleShowAnimation: Animation? = null
    private var mImageToggleHideAnimation: Animation? = null
    private var mIsMenuButtonAnimationRunning = false
    private var mIsSetClosedOnTouchOutside = false
    private var mOpenDirection = 0
    private var mToggleListener: OnMenuToggleListener? = null
    private var mShowBackgroundAnimator: ValueAnimator? = null
    private var mHideBackgroundAnimator: ValueAnimator? = null
    private var mBackgroundColor = 0
    private var mLabelsPosition = 0
    private var mLabelsContext: Context
    private var mMenuLabelText: String? = null
    private var mUsingMenuLabel = false

    interface OnMenuToggleListener {
        fun onMenuToggle(opened: Boolean)
    }

    init {
        init(context, attrs)
        mLabelsContext = context
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        val attr = context.obtainStyledAttributes(attrs, R.styleable.FloatingActionMenu, 0, 0)
        mButtonSpacing = attr.getDimensionPixelSize(
            R.styleable.FloatingActionMenu_menu_buttonSpacing,
            mButtonSpacing
        )
        mLabelsMargin = attr.getDimensionPixelSize(
            R.styleable.FloatingActionMenu_menu_labels_margin,
            mLabelsMargin
        )
        mLabelsPosition =
            attr.getInt(R.styleable.FloatingActionMenu_menu_labels_position, LABELS_POSITION_LEFT)
        mLabelsShowAnimation = attr.getResourceId(
            R.styleable.FloatingActionMenu_menu_labels_showAnimation,
            if (mLabelsPosition == LABELS_POSITION_LEFT) R.anim.fab_slide_in_from_right else R.anim.fab_slide_in_from_left
        )
        mLabelsHideAnimation = attr.getResourceId(
            R.styleable.FloatingActionMenu_menu_labels_hideAnimation,
            if (mLabelsPosition == LABELS_POSITION_LEFT) R.anim.fab_slide_out_to_right else R.anim.fab_slide_out_to_left
        )
        mLabelsPaddingTop = attr.getDimensionPixelSize(
            R.styleable.FloatingActionMenu_menu_labels_paddingTop,
            mLabelsPaddingTop
        )
        mLabelsPaddingRight = attr.getDimensionPixelSize(
            R.styleable.FloatingActionMenu_menu_labels_paddingRight,
            mLabelsPaddingRight
        )
        mLabelsPaddingBottom = attr.getDimensionPixelSize(
            R.styleable.FloatingActionMenu_menu_labels_paddingBottom,
            mLabelsPaddingBottom
        )
        mLabelsPaddingLeft = attr.getDimensionPixelSize(
            R.styleable.FloatingActionMenu_menu_labels_paddingLeft,
            mLabelsPaddingLeft
        )
        mLabelsTextColor =
            attr.getColorStateList(R.styleable.FloatingActionMenu_menu_labels_textColor)
        // set default value if null same as for textview
        if (mLabelsTextColor == null) {
            mLabelsTextColor = ColorStateList.valueOf(Color.WHITE)
        }
        mLabelsTextSize = attr.getDimension(
            R.styleable.FloatingActionMenu_menu_labels_textSize,
            resources.getDimension(R.dimen.labels_text_size)
        )
        mLabelsCornerRadius = attr.getDimensionPixelSize(
            R.styleable.FloatingActionMenu_menu_labels_cornerRadius,
            mLabelsCornerRadius
        )
        mLabelsShowShadow =
            attr.getBoolean(R.styleable.FloatingActionMenu_menu_labels_showShadow, true)
        mLabelsColorNormal =
            attr.getColor(R.styleable.FloatingActionMenu_menu_labels_colorNormal, -0xcccccd)
        mLabelsColorPressed =
            attr.getColor(R.styleable.FloatingActionMenu_menu_labels_colorPressed, -0xbbbbbc)
        mLabelsColorRipple =
            attr.getColor(R.styleable.FloatingActionMenu_menu_labels_colorRipple, 0x66FFFFFF)
        mMenuShowShadow = attr.getBoolean(R.styleable.FloatingActionMenu_menu_showShadow, true)
        mMenuShadowColor =
            attr.getColor(R.styleable.FloatingActionMenu_menu_shadowColor, 0x66000000)
        mMenuShadowRadius =
            attr.getDimension(R.styleable.FloatingActionMenu_menu_shadowRadius, mMenuShadowRadius)
        mMenuShadowXOffset =
            attr.getDimension(R.styleable.FloatingActionMenu_menu_shadowXOffset, mMenuShadowXOffset)
        mMenuShadowYOffset =
            attr.getDimension(R.styleable.FloatingActionMenu_menu_shadowYOffset, mMenuShadowYOffset)
        mMenuColorNormal = attr.getColor(R.styleable.FloatingActionMenu_menu_colorNormal, -0x25bcca)
        mMenuColorPressed =
            attr.getColor(R.styleable.FloatingActionMenu_menu_colorPressed, -0x18afbd)
        mMenuColorRipple =
            attr.getColor(R.styleable.FloatingActionMenu_menu_colorRipple, -0x66000001)
        animationDelayPerItem =
            attr.getInt(R.styleable.FloatingActionMenu_menu_animationDelayPerItem, 50)
        mIcon = attr.getDrawable(R.styleable.FloatingActionMenu_menu_icon)
        if (mIcon == null) {
            mIcon = AppCompatResources.getDrawable(context, R.drawable.icon_add)
        }
        mLabelsSingleLine =
            attr.getBoolean(R.styleable.FloatingActionMenu_menu_labels_singleLine, false)
        mLabelsEllipsize = attr.getInt(R.styleable.FloatingActionMenu_menu_labels_ellipsize, 0)
        mLabelsMaxLines = attr.getInt(R.styleable.FloatingActionMenu_menu_labels_maxLines, -1)
        mMenuFabSize = attr.getInt(
            R.styleable.FloatingActionMenu_menu_fab_size,
            FloatingActionButton.SIZE_NORMAL
        )
        mLabelsStyle = attr.getResourceId(R.styleable.FloatingActionMenu_menu_labels_style, 0)
        val customFont = attr.getString(R.styleable.FloatingActionMenu_menu_labels_customFont)
        try {
            if (!TextUtils.isEmpty(customFont)) {
                mCustomTypefaceFromFont = Typeface.createFromAsset(getContext().assets, customFont)
            }
        } catch (ex: RuntimeException) {
            throw IllegalArgumentException("Unable to load specified custom font: $customFont", ex)
        }
        mOpenDirection = attr.getInt(R.styleable.FloatingActionMenu_menu_openDirection, OPEN_UP)
        mBackgroundColor =
            attr.getColor(R.styleable.FloatingActionMenu_menu_backgroundColor, Color.TRANSPARENT)
        if (attr.hasValue(R.styleable.FloatingActionMenu_menu_fab_label)) {
            mUsingMenuLabel = true
            mMenuLabelText = attr.getString(R.styleable.FloatingActionMenu_menu_fab_label)
        }
        if (attr.hasValue(R.styleable.FloatingActionMenu_menu_labels_padding)) {
            val padding =
                attr.getDimensionPixelSize(R.styleable.FloatingActionMenu_menu_labels_padding, 0)
            initPadding(padding)
        }
        mOpenInterpolator = OvershootInterpolator()
        mCloseInterpolator = AnticipateInterpolator()
        mLabelsContext = ContextThemeWrapper(getContext(), mLabelsStyle)
        initBackgroundDimAnimation()
        createMenuButton()
        initMenuButtonAnimations(attr)
        attr.recycle()
    }

    private fun initMenuButtonAnimations(attr: TypedArray) {
        val showResId = attr.getResourceId(
            R.styleable.FloatingActionMenu_menu_fab_show_animation,
            R.anim.fab_scale_up
        )
        setMenuButtonShowAnimation(AnimationUtils.loadAnimation(context, showResId))
        mImageToggleShowAnimation = AnimationUtils.loadAnimation(context, showResId)
        val hideResId = attr.getResourceId(
            R.styleable.FloatingActionMenu_menu_fab_hide_animation,
            R.anim.fab_scale_down
        )
        setMenuButtonHideAnimation(AnimationUtils.loadAnimation(context, hideResId))
        mImageToggleHideAnimation = AnimationUtils.loadAnimation(context, hideResId)
    }

    private fun initBackgroundDimAnimation() {
        val maxAlpha = Color.alpha(mBackgroundColor)
        val red = Color.red(mBackgroundColor)
        val green = Color.green(mBackgroundColor)
        val blue = Color.blue(mBackgroundColor)
        mShowBackgroundAnimator = ValueAnimator.ofInt(0, maxAlpha)
        mShowBackgroundAnimator?.duration = ANIMATION_DURATION
        mShowBackgroundAnimator?.addUpdateListener { animation ->
            val alpha = animation.animatedValue as Int
            setBackgroundColor(Color.argb(alpha, red, green, blue))
        }
        mHideBackgroundAnimator = ValueAnimator.ofInt(maxAlpha, 0)
        mHideBackgroundAnimator?.duration = ANIMATION_DURATION
        mHideBackgroundAnimator?.addUpdateListener { animation ->
            val alpha = animation.animatedValue as Int
            setBackgroundColor(Color.argb(alpha, red, green, blue))
        }
    }

    private val isBackgroundEnabled: Boolean
        get() = mBackgroundColor != Color.TRANSPARENT

    private fun initPadding(padding: Int) {
        mLabelsPaddingTop = padding
        mLabelsPaddingRight = padding
        mLabelsPaddingBottom = padding
        mLabelsPaddingLeft = padding
    }

    private fun createMenuButton() {
        mMenuButton = FloatingActionButton(context)
        mMenuButton?.mShowShadow = mMenuShowShadow
        if (mMenuShowShadow) {
            mMenuButton?.mShadowRadius = Util.dpToPx(context, mMenuShadowRadius)
            mMenuButton?.mShadowXOffset =
                Util.dpToPx(context, mMenuShadowXOffset)
            mMenuButton?.mShadowYOffset =
                Util.dpToPx(context, mMenuShadowYOffset)
        }
        mMenuButton?.setColors(mMenuColorNormal, mMenuColorPressed, mMenuColorRipple)
        mMenuButton?.mShadowColor = mMenuShadowColor
        mMenuButton?.mFabSize = mMenuFabSize
        mMenuButton?.updateBackground()
        mMenuButton?.labelText = mMenuLabelText
        menuIconView = ImageView(context)
        menuIconView!!.setImageDrawable(mIcon)
        addView(mMenuButton, super.generateDefaultLayoutParams())
        addView(menuIconView)
        createDefaultIconAnimation()
    }

    private fun createDefaultIconAnimation() {
        val collapseAngle: Float
        val expandAngle: Float
        if (mOpenDirection == OPEN_UP) {
            collapseAngle =
                if (mLabelsPosition == LABELS_POSITION_LEFT) OPENED_PLUS_ROTATION_LEFT else OPENED_PLUS_ROTATION_RIGHT
            expandAngle =
                if (mLabelsPosition == LABELS_POSITION_LEFT) OPENED_PLUS_ROTATION_LEFT else OPENED_PLUS_ROTATION_RIGHT
        } else {
            collapseAngle =
                if (mLabelsPosition == LABELS_POSITION_LEFT) OPENED_PLUS_ROTATION_RIGHT else OPENED_PLUS_ROTATION_LEFT
            expandAngle =
                if (mLabelsPosition == LABELS_POSITION_LEFT) OPENED_PLUS_ROTATION_RIGHT else OPENED_PLUS_ROTATION_LEFT
        }
        val collapseAnimator = ObjectAnimator.ofFloat(
            menuIconView,
            "rotation",
            collapseAngle,
            CLOSED_PLUS_ROTATION
        )
        val expandAnimator = ObjectAnimator.ofFloat(
            menuIconView,
            "rotation",
            CLOSED_PLUS_ROTATION,
            expandAngle
        )
        mOpenAnimatorSet.play(expandAnimator)
        mCloseAnimatorSet.play(collapseAnimator)
        mOpenAnimatorSet.interpolator = mOpenInterpolator
        mCloseAnimatorSet.interpolator = mCloseInterpolator
        mOpenAnimatorSet.duration = ANIMATION_DURATION
        mCloseAnimatorSet.duration = ANIMATION_DURATION
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var width: Int
        var height = 0
        mMaxButtonWidth = 0
        var maxLabelWidth = 0
        measureChildWithMargins(menuIconView, widthMeasureSpec, 0, heightMeasureSpec, 0)
        for (i in 0 until mButtonsCount) {
            val child = getChildAt(i)
            if (child.visibility == GONE || child === menuIconView) continue
            measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0)
            mMaxButtonWidth = mMaxButtonWidth.coerceAtLeast(child.measuredWidth)
        }
        for (i in 0 until mButtonsCount) {
            var usedWidth = 0
            val child = getChildAt(i)
            if (child.visibility == GONE || child === menuIconView) continue
            usedWidth += child.measuredWidth
            height += child.measuredHeight
            val label = child.getTag(R.id.fab_label) as Label?
            if (label != null) {
                val labelOffset =
                    (mMaxButtonWidth - child.measuredWidth) / if (mUsingMenuLabel) 1 else 2
                val labelUsedWidth: Int =
                    child.measuredWidth + label.calculateShadowWidth() + mLabelsMargin + labelOffset
                measureChildWithMargins(
                    label,
                    widthMeasureSpec,
                    labelUsedWidth,
                    heightMeasureSpec,
                    0
                )
                usedWidth += label.measuredWidth
                maxLabelWidth = maxLabelWidth.coerceAtLeast(usedWidth + labelOffset)
            }
        }
        width =
            mMaxButtonWidth.coerceAtLeast(maxLabelWidth + mLabelsMargin) + paddingLeft + paddingRight
        height += mButtonSpacing * (mButtonsCount - 1) + paddingTop + paddingBottom
        height = adjustForOvershoot(height)
        if (layoutParams.width == LayoutParams.MATCH_PARENT) {
            width = getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
        }
        if (layoutParams.height == LayoutParams.MATCH_PARENT) {
            height = getDefaultSize(suggestedMinimumHeight, heightMeasureSpec)
        }
        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val buttonsHorizontalCenter =
            if (mLabelsPosition == LABELS_POSITION_LEFT) r - l - mMaxButtonWidth / 2 - paddingRight else mMaxButtonWidth / 2 + paddingLeft
        val openUp = mOpenDirection == OPEN_UP
        val menuButtonTop =
            if (openUp) b - t - mMenuButton!!.measuredHeight - paddingBottom else paddingTop
        val menuButtonLeft: Int = buttonsHorizontalCenter - mMenuButton!!.measuredWidth / 2
        mMenuButton?.layout(
            menuButtonLeft, menuButtonTop, menuButtonLeft + mMenuButton!!.measuredWidth,
            menuButtonTop + mMenuButton!!.measuredHeight
        )
        val imageLeft = buttonsHorizontalCenter - menuIconView!!.measuredWidth / 2
        val imageTop: Int =
            menuButtonTop + mMenuButton!!.getMeasuredHeight() / 2 - menuIconView!!.measuredHeight / 2
        menuIconView!!.layout(
            imageLeft, imageTop, imageLeft + menuIconView!!.measuredWidth,
            imageTop + menuIconView!!.measuredHeight
        )
        var nextY =
            if (openUp) menuButtonTop + mMenuButton!!.measuredHeight + mButtonSpacing else menuButtonTop
        for (i in mButtonsCount - 1 downTo 0) {
            val child = getChildAt(i)
            if (child === menuIconView) continue
            val fab: FloatingActionButton =
                child as FloatingActionButton
            if (fab.visibility == GONE) continue
            val childX: Int = buttonsHorizontalCenter - fab.measuredWidth / 2
            val childY = if (openUp) nextY - fab.measuredHeight - mButtonSpacing else nextY
            if (fab !== mMenuButton) {
                fab.layout(
                    childX, childY, childX + fab.measuredWidth,
                    childY + fab.measuredHeight
                )
                if (!mIsMenuOpening) {
                    fab.hide(false)
                }
            }
            val label = fab.getTag(R.id.fab_label) as View?
            if (label != null) {
                val labelsOffset =
                    (if (mUsingMenuLabel) mMaxButtonWidth / 2 else fab.measuredWidth / 2) + mLabelsMargin
                val labelXNearButton =
                    if (mLabelsPosition == LABELS_POSITION_LEFT) buttonsHorizontalCenter - labelsOffset else buttonsHorizontalCenter + labelsOffset
                val labelXAwayFromButton =
                    if (mLabelsPosition == LABELS_POSITION_LEFT) labelXNearButton - label.measuredWidth else labelXNearButton + label.measuredWidth
                val labelLeft =
                    if (mLabelsPosition == LABELS_POSITION_LEFT) labelXAwayFromButton else labelXNearButton
                val labelRight =
                    if (mLabelsPosition == LABELS_POSITION_LEFT) labelXNearButton else labelXAwayFromButton
                val labelTop: Int = childY - mLabelsVerticalOffset + (fab.measuredHeight
                        - label.measuredHeight) / 2
                label.layout(labelLeft, labelTop, labelRight, labelTop + label.measuredHeight)
                if (!mIsMenuOpening) {
                    label.visibility = INVISIBLE
                }
            }
            nextY =
                if (openUp) childY - mButtonSpacing else childY + child.measuredHeight + mButtonSpacing
        }
    }

    private fun adjustForOvershoot(dimension: Int): Int {
        return (dimension * 0.03 + dimension).toInt()
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        bringChildToFront(mMenuButton)
        bringChildToFront(menuIconView)
        mButtonsCount = childCount
        createLabels()
    }

    private fun createLabels() {
        for (i in 0 until mButtonsCount) {
            if (getChildAt(i) === menuIconView) continue
            val fab: FloatingActionButton =
                getChildAt(i) as FloatingActionButton
            if (fab.getTag(R.id.fab_label) != null) continue
            addLabel(fab)
            if (fab === mMenuButton) {
                mMenuButton!!.setOnClickListener { toggle(mIsAnimated) }
            }
        }
    }

    private fun addLabel(fab: FloatingActionButton) {
        val text: String? = fab.labelText
        if (TextUtils.isEmpty(text)) return
        val label = Label(mLabelsContext)
        label.isClickable = true
        label.setFab(fab)
        label.setShowAnimation(AnimationUtils.loadAnimation(context, mLabelsShowAnimation))
        label.setHideAnimation(AnimationUtils.loadAnimation(context, mLabelsHideAnimation))
        if (mLabelsStyle > 0) {
            label.setTextAppearance(context, mLabelsStyle)
            label.setShowShadow(false)
            label.setUsingStyle(true)
        } else {
            label.setColors(mLabelsColorNormal, mLabelsColorPressed, mLabelsColorRipple)
            label.setShowShadow(mLabelsShowShadow)
            label.setCornerRadius(mLabelsCornerRadius)
            if (mLabelsEllipsize > 0) {
                setLabelEllipsize(label)
            }
            label.maxLines = mLabelsMaxLines
            label.updateBackground()
            label.setTextSize(TypedValue.COMPLEX_UNIT_PX, mLabelsTextSize)
            label.setTextColor(mLabelsTextColor)
            var left = mLabelsPaddingLeft
            var top = mLabelsPaddingTop
            if (mLabelsShowShadow) {
                left += fab.shadowRadius + abs(fab.shadowXOffset)
                top += fab.shadowRadius + abs(fab.shadowYOffset)
            }
            label.setPadding(
                left,
                top,
                mLabelsPaddingLeft,
                mLabelsPaddingTop
            )
            if (mLabelsMaxLines < 0 || mLabelsSingleLine) {
                label.isSingleLine = mLabelsSingleLine
            }
        }
        if (mCustomTypefaceFromFont != null) {
            label.typeface = mCustomTypefaceFromFont
        }
        label.text = text
        label.setOnClickListener(fab.getOnClickListener())
        addView(label)
        fab.setTag(R.id.fab_label, label)
    }

    private fun setLabelEllipsize(label: Label) {
        when (mLabelsEllipsize) {
            1 -> label.ellipsize = TextUtils.TruncateAt.START
            2 -> label.ellipsize = TextUtils.TruncateAt.MIDDLE
            3 -> label.ellipsize = TextUtils.TruncateAt.END
            4 -> label.ellipsize = TextUtils.TruncateAt.MARQUEE
        }
    }

    override fun generateLayoutParams(attrs: AttributeSet): MarginLayoutParams {
        return MarginLayoutParams(context, attrs)
    }

    override fun generateLayoutParams(p: LayoutParams): MarginLayoutParams {
        return MarginLayoutParams(p)
    }

    override fun generateDefaultLayoutParams(): MarginLayoutParams {
        return MarginLayoutParams(
            MarginLayoutParams.WRAP_CONTENT,
            MarginLayoutParams.WRAP_CONTENT
        )
    }

    override fun checkLayoutParams(p: LayoutParams): Boolean {
        return p is MarginLayoutParams
    }

    private fun hideMenuButtonWithImage(animate: Boolean) {
        if (!isMenuButtonHidden) {
            mMenuButton!!.hide(animate)
            if (animate) {
                menuIconView!!.startAnimation(mImageToggleHideAnimation)
            }
            menuIconView!!.visibility = INVISIBLE
            mIsMenuButtonAnimationRunning = false
        }
    }

    private fun showMenuButtonWithImage(animate: Boolean) {
        if (isMenuButtonHidden) {
            mMenuButton!!.show(animate)
            if (animate) {
                menuIconView!!.startAnimation(mImageToggleShowAnimation)
            }
            menuIconView!!.visibility = VISIBLE
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (mIsSetClosedOnTouchOutside) {
            var handled = false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> handled = isOpened
                MotionEvent.ACTION_UP -> {
                    close(mIsAnimated)
                    handled = true
                }
            }
            return handled
        }
        return super.onTouchEvent(event)
    }

    fun toggle(animate: Boolean) {
        if (isOpened) {
            close(animate)
        } else {
            open(animate)
        }
    }

    fun open(animate: Boolean) {
        if (!isOpened) {
            if (isBackgroundEnabled) {
                mShowBackgroundAnimator!!.start()
            }
            if (isIconAnimated) {
                if (iconToggleAnimatorSet != null) {
                    iconToggleAnimatorSet!!.start()
                } else {
                    mCloseAnimatorSet.cancel()
                    mOpenAnimatorSet.start()
                }
            }
            var delay = 0
            var counter = 0
            mIsMenuOpening = true
            for (i in childCount - 1 downTo 0) {
                val child = getChildAt(i)
                if (child is FloatingActionButton && child.visibility != GONE) {
                    counter++
                    mUiHandler.postDelayed(object : Runnable {
                        override fun run() {
                            if (isOpened) return
                            if (child !== mMenuButton) {
                                child.show(animate)
                            }
                            val label = child.getTag(R.id.fab_label) as Label?
                            if (label != null && label.isHandleVisibilityChanges()) {
                                label.show(animate)
                            }
                        }
                    }, delay.toLong())
                    delay += animationDelayPerItem
                }
            }
            mUiHandler.postDelayed({
                isOpened = true
                if (mToggleListener != null) {
                    mToggleListener!!.onMenuToggle(true)
                }
            }, (++counter * animationDelayPerItem).toLong())
        }
    }

    fun close(animate: Boolean) {
        if (isOpened) {
            if (isBackgroundEnabled) {
                mHideBackgroundAnimator!!.start()
            }
            if (isIconAnimated) {
                if (iconToggleAnimatorSet != null) {
                    iconToggleAnimatorSet!!.start()
                } else {
                    mCloseAnimatorSet.start()
                    mOpenAnimatorSet.cancel()
                }
            }
            var delay = 0
            var counter = 0
            mIsMenuOpening = false
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                if (child is FloatingActionButton && child.visibility != GONE) {
                    counter++
                    mUiHandler.postDelayed(object : Runnable {
                        override fun run() {
                            if (!isOpened) return
                            if (child !== mMenuButton) {
                                child.hide(animate)
                            }
                            val label = child.getTag(R.id.fab_label) as Label?
                            if (label != null && label.isHandleVisibilityChanges()) {
                                label.hide(animate)
                            }
                        }
                    }, delay.toLong())
                    delay += animationDelayPerItem
                }
            }
            mUiHandler.postDelayed({
                isOpened = false
                if (mToggleListener != null) {
                    mToggleListener!!.onMenuToggle(false)
                }
            }, (++counter * animationDelayPerItem).toLong())
        }
    }

    /**
     * Sets the [android.view.animation.Interpolator] for **FloatingActionButton's** icon animation.
     *
     * @param interpolator the Interpolator to be used in animation
     */
    fun setIconAnimationInterpolator(interpolator: Interpolator?) {
        mOpenAnimatorSet.interpolator = interpolator
        mCloseAnimatorSet.interpolator = interpolator
    }

    fun setIconAnimationOpenInterpolator(openInterpolator: Interpolator?) {
        mOpenAnimatorSet.interpolator = openInterpolator
    }

    fun setIconAnimationCloseInterpolator(closeInterpolator: Interpolator?) {
        mCloseAnimatorSet.interpolator = closeInterpolator
    }

    /**
     * Sets whether open and close actions should be animated
     *
     * @param animated if **false** - menu items will appear/disappear instantly without any animation
     */
    var isAnimated: Boolean
        get() = mIsAnimated
        set(animated) {
            mIsAnimated = animated
            mOpenAnimatorSet.duration = if (animated) ANIMATION_DURATION else 0.toLong()
            mCloseAnimatorSet.duration = if (animated) ANIMATION_DURATION else 0.toLong()
        }

    fun setOnMenuToggleListener(listener: OnMenuToggleListener?) {
        mToggleListener = listener
    }

    fun setMenuButtonShowAnimation(showAnimation: Animation?) {
        mMenuButtonShowAnimation = showAnimation
        mMenuButton!!.showAnimation = showAnimation
    }

    fun setMenuButtonHideAnimation(hideAnimation: Animation?) {
        mMenuButtonHideAnimation = hideAnimation
        mMenuButton!!.hideAnimation = hideAnimation
    }

    val isMenuHidden: Boolean
        get() = visibility == INVISIBLE
    val isMenuButtonHidden: Boolean
        get() = mMenuButton!!.isHidden

    /**
     * Makes the whole [.FloatingActionMenu] to appear and sets its visibility to [.VISIBLE]
     *
     * @param animate if true - plays "show animation"
     */
    fun showMenu(animate: Boolean) {
        if (isMenuHidden) {
            if (animate) {
                startAnimation(mMenuButtonShowAnimation)
            }
            visibility = VISIBLE
        }
    }

    /**
     * Makes the [.FloatingActionMenu] to disappear and sets its visibility to [.INVISIBLE]
     *
     * @param animate if true - plays "hide animation"
     */
    fun hideMenu(animate: Boolean) {
        if (!isMenuHidden && !mIsMenuButtonAnimationRunning) {
            mIsMenuButtonAnimationRunning = true
            if (isOpened) {
                close(animate)
                mUiHandler.postDelayed({
                    if (animate) {
                        startAnimation(mMenuButtonHideAnimation)
                    }
                    visibility = INVISIBLE
                    mIsMenuButtonAnimationRunning = false
                }, (animationDelayPerItem * mButtonsCount).toLong())
            } else {
                if (animate) {
                    startAnimation(mMenuButtonHideAnimation)
                }
                visibility = INVISIBLE
                mIsMenuButtonAnimationRunning = false
            }
        }
    }

    fun toggleMenu(animate: Boolean) {
        if (isMenuHidden) {
            showMenu(animate)
        } else {
            hideMenu(animate)
        }
    }

    /**
     * Makes the [FloatingActionButton] to appear inside the [.FloatingActionMenu] and
     * sets its visibility to [.VISIBLE]
     *
     * @param animate if true - plays "show animation"
     */
    fun showMenuButton(animate: Boolean) {
        if (isMenuButtonHidden) {
            showMenuButtonWithImage(animate)
        }
    }

    /**
     * Makes the [FloatingActionButton] to disappear inside the [.FloatingActionMenu] and
     * sets its visibility to [.INVISIBLE]
     *
     * @param animate if true - plays "hide animation"
     */
    fun hideMenuButton(animate: Boolean) {
        if (!isMenuButtonHidden && !mIsMenuButtonAnimationRunning) {
            mIsMenuButtonAnimationRunning = true
            if (isOpened) {
                close(animate)
                mUiHandler.postDelayed(
                    { hideMenuButtonWithImage(animate) },
                    (animationDelayPerItem * mButtonsCount).toLong()
                )
            } else {
                hideMenuButtonWithImage(animate)
            }
        }
    }

    fun toggleMenuButton(animate: Boolean) {
        if (isMenuButtonHidden) {
            showMenuButton(animate)
        } else {
            hideMenuButton(animate)
        }
    }

    fun setClosedOnTouchOutside(close: Boolean) {
        mIsSetClosedOnTouchOutside = close
    }

    fun setMenuButtonColorNormalResId(colorResId: Int) {
        mMenuColorNormal = resources.getColor(colorResId)
        mMenuButton!!.setColorNormalResId(colorResId)
    }

    var menuButtonColorNormal: Int
        get() = mMenuColorNormal
        set(color) {
            mMenuColorNormal = color
            mMenuButton!!.colorNormal = color
        }

    fun setMenuButtonColorPressedResId(colorResId: Int) {
        mMenuColorPressed = resources.getColor(colorResId)
        mMenuButton!!.setColorPressedResId(colorResId)
    }

    var menuButtonColorPressed: Int
        get() = mMenuColorPressed
        set(color) {
            mMenuColorPressed = color
            mMenuButton!!.colorPressed = color
        }

    fun setMenuButtonColorRippleResId(colorResId: Int) {
        mMenuColorRipple = resources.getColor(colorResId)
        mMenuButton!!.setColorRippleResId(colorResId)
    }

    var menuButtonColorRipple: Int
        get() = mMenuColorRipple
        set(color) {
            mMenuColorRipple = color
            mMenuButton!!.colorRipple = color
        }

    fun addMenuButton(fab: FloatingActionButton) {
        addView(fab, mButtonsCount - 2)
        mButtonsCount++
        addLabel(fab)
    }

    fun removeMenuButton(fab: FloatingActionButton) {
        removeView(fab.labelView)
        removeView(fab)
        mButtonsCount--
    }

    fun addMenuButton(fab: FloatingActionButton, _index: Int) {
        var index = _index
        val size = mButtonsCount - 2
        if (index < 0) {
            index = 0
        } else if (index > size) {
            index = size
        }
        addView(fab, index)
        mButtonsCount++
        addLabel(fab)
    }

    fun removeAllMenuButtons() {
        close(true)
        val viewsToRemove: MutableList<FloatingActionButton> = ArrayList()
        for (i in 0 until childCount) {
            val v = getChildAt(i)
            if (v !== mMenuButton && v !== menuIconView && v is FloatingActionButton) {
                viewsToRemove.add(v)
            }
        }
        for (v in viewsToRemove) {
            removeMenuButton(v)
        }
    }

    var menuButtonLabelText: String?
        get() = mMenuLabelText
        set(text) {
            mMenuButton!!.labelText = text
        }

    fun setOnMenuButtonClickListener(clickListener: OnClickListener?) {
        mMenuButton!!.setOnClickListener(clickListener)
    }

    fun setOnMenuButtonLongClickListener(longClickListener: OnLongClickListener?) {
        mMenuButton!!.setOnLongClickListener(longClickListener)
    }

    companion object {
        private const val ANIMATION_DURATION = 300L
        private const val CLOSED_PLUS_ROTATION = 0f
        private const val OPENED_PLUS_ROTATION_LEFT = -90f - 45f
        private const val OPENED_PLUS_ROTATION_RIGHT = 90f + 45f
        private const val OPEN_UP = 0
        private const val OPEN_DOWN = 1
        private const val LABELS_POSITION_LEFT = 0
        private const val LABELS_POSITION_RIGHT = 1
    }
}
