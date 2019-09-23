package com.jiandanlangman.tagview

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.ArrayList
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * TagView 标签墙
 * @author Jiandanlangman
 */
class TagView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {

    private val typeSettingThreadPool = Executors.newSingleThreadExecutor()

    private val tags = ArrayList<String>()
    private val paint = Paint()
    private val drawRect = Rect()
    private val minTextSize = 4f * resources.displayMetrics.density
    private var primaryTextColor = 0xFF333333.toInt()
    private var secondaryTextColor = 0xFF999999.toInt()
    private var maxTextSize = 24f * resources.displayMetrics.density
    private lateinit var random: Random
    private var bitmap: Bitmap? = null


    init {
        paint.isAntiAlias = true
        paint.textAlign = Paint.Align.CENTER
        paint.style = Paint.Style.FILL_AND_STROKE
        if (attrs != null) {
            val typeArray = context.obtainStyledAttributes(attrs, R.styleable.TagView)
            maxTextSize = typeArray.getDimensionPixelSize(R.styleable.TagView_textSize, maxTextSize.toInt()).toFloat()
            primaryTextColor = typeArray.getColor(R.styleable.TagView_primaryTextColor, primaryTextColor)
            secondaryTextColor = typeArray.getColor(R.styleable.TagView_secondaryTextColor, secondaryTextColor)
            typeArray.recycle()
        }
    }


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        reTypeSetting()
    }


    private fun typeSetting() {
        typeSettingThreadPool.execute {
            random = Random()
            val region = Region(0, 0, width, height)
            val tagModels = ArrayList<TagModel>()
            tagModels.addAll(primaryTypeSetting(region))
            tagModels.addAll(secondaryTypeSetting(region))
            tagModels.addAll(edgeTypeSetting(region))
            val tempBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(tempBitmap)
            tagModels.forEach {
                drawRect.set(it.rect)
                paint.textSize = it.textSize
                paint.color = it.textColor
                paint.isFakeBoldText = it.isFakeBoldText
                if (it.rect.width() >= it.rect.height()) {
                    val fontMetrics = paint.fontMetricsInt
                    val baseline = (drawRect.bottom + drawRect.top - fontMetrics.bottom - fontMetrics.top) / 2f
                    canvas.drawText(it.text, drawRect.centerX().toFloat(), baseline, paint)
                } else
                    drawVerticalTag(canvas, drawRect, it)
            }
            post {
                bitmap = tempBitmap
                invalidate()
            }
        }
    }


    private fun primaryTypeSetting(region: Region): List<TagModel> {
        val result = ArrayList<TagModel>()
        tags.forEach {
            var textSize = maxTextSize
            while (textSize >= minTextSize) {
                val orientation = if (random.nextInt(100) < 50) Orientation.HORIZONTAL else Orientation.VERTICAL
                val sourceRect = if (result.isEmpty()) Rect() else result.last().rect
                var tagModel = typeSetting(region, it, textSize, primaryTextColor, true, orientation, sourceRect)
                if (tagModel == null)
                    tagModel = typeSetting(region, it, textSize, primaryTextColor, true, if (orientation == Orientation.HORIZONTAL) Orientation.VERTICAL else Orientation.HORIZONTAL, sourceRect)
                if (tagModel == null)
                    textSize -= minTextSize
                else
                    result.add(tagModel)
                break
            }
        }
        return result
    }


    private fun secondaryTypeSetting(region: Region): List<TagModel> {
        val result = ArrayList<TagModel>()
        var textSize = maxTextSize - minTextSize * 2f
        if (textSize < minTextSize)
            textSize = minTextSize
        while (true) {
            var isAdded = false
            tags.forEach {
                val orientation = if (random.nextInt(100) < 50) Orientation.HORIZONTAL else Orientation.VERTICAL
                val sourceRect = if (result.isEmpty()) Rect() else result.last().rect
                var tagModel = typeSetting(region, it, textSize, secondaryTextColor, false, orientation, sourceRect)
                if (tagModel == null)
                    tagModel = typeSetting(region, it, textSize, secondaryTextColor, false, if (orientation == Orientation.HORIZONTAL) Orientation.VERTICAL else Orientation.HORIZONTAL, sourceRect)
                if (tagModel != null) {
                    result.add(tagModel)
                    isAdded = true
                }
            }
            if (!isAdded)
                textSize -= minTextSize
            if (textSize < minTextSize)
                break
        }
        return result
    }


    private fun edgeTypeSetting(region: Region): List<TagModel> {
        val result = ArrayList<TagModel>()
        val itr = RegionIterator(region)
        val rect = Rect()
        while (itr.next(rect)) {
            val width = rect.width()
            val height = rect.height()
            if (width >= minTextSize && height >= minTextSize) {
                var maxDrawTextLength = (width.coerceAtLeast(height) / minTextSize).toInt()
                var text: String? = null
                while (maxDrawTextLength >= 1 && text == null) {
                    text = tags.firstOrNull { it.length == maxDrawTextLength }
                    maxDrawTextLength--
                }
                if (text != null)
                    result.add(TagModel(Rect(rect.left, rect.top, rect.left + width, rect.top + height), secondaryTextColor, minTextSize, false, text))
            }
        }
        return result
    }


    private fun measureText(text: String, textSize: Float, isFakeBoldText: Boolean, orientation: Orientation): Size {
        paint.textSize = textSize
        paint.isFakeBoldText = isFakeBoldText
        var width = paint.measureText(text).toInt()
        var height = paint.textSize.toInt()
        if (orientation == Orientation.VERTICAL) {
            val t = width
            width = height
            height = t
        }
        return Size(width, height)
    }

    private fun typeSetting(region: Region, text: String, textSize: Float, textColor: Int, isFakeBoldText: Boolean, orientation: Orientation, sourceRect: Rect): TagModel? {
        val size = measureText(text, textSize, isFakeBoldText, orientation)
        val sourceRectCenterX = sourceRect.centerX().toDouble()
        val sourceRectCenterY = sourceRect.centerY().toDouble()
        val rit = RegionIterator(region)
        val rect = Rect()
        val resultRect = Rect(sourceRect)
        while (rit.next(rect))
            if (rect.width() >= size.width && rect.height() >= size.height) {
                if (sourceRect.isEmpty) {
                    if (rect.width() >= resultRect.width() && rect.height() >= resultRect.height())
                        resultRect.set(rect)
                } else {
                    val x = rect.centerX()
                    val y = rect.centerY()
                    val resultRectCenterX = resultRect.centerX()
                    val resultRectCenterY = resultRect.centerY()
                    if (sqrt((x - sourceRectCenterX).pow(2) + (y - sourceRectCenterY).pow(2)) >= sqrt((resultRectCenterX - sourceRectCenterX).pow(2) + (resultRectCenterY - sourceRectCenterY).pow(2)))
                        resultRect.set(rect)
                }
            }
        if (!resultRect.isEmpty && resultRect != sourceRect && resultRect.width() >= size.width && resultRect.height() >= size.height)
            return TagModel(opRegion(region, size.width, size.height, resultRect), textColor, textSize, isFakeBoldText, text)
        return null
    }

    private fun opRegion(region: Region, width: Int, height: Int, rect: Rect): Rect {
        val left: Int = if (rect.width() - width < minTextSize)
            rect.left
        else
            rect.left + (random.nextInt(((rect.width() - width) / minTextSize).toInt()) * minTextSize).toInt()
        val top: Int = if (rect.height() - height < minTextSize)
            rect.top
        else
            rect.top + (random.nextInt(((rect.height() - height) / minTextSize).toInt()) * minTextSize).toInt()
        val r = Rect(left, top, left + width, top + height)
        region.op(r, Region.Op.DIFFERENCE)
        return r
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (bitmap != null)
            canvas.drawBitmap(bitmap!!, 0f, 0f, paint)
    }


    private fun drawVerticalTag(canvas: Canvas, drawRect: Rect, tag: TagModel) {
        paint.textSize = tag.textSize
        paint.color = tag.textColor
        paint.isFakeBoldText = tag.isFakeBoldText
        val textHeight = tag.rect.height() / tag.text.length
        for (i in tag.text.indices) {
            val text = tag.text.substring(i, i + 1)
            val fontMetrics = paint.fontMetricsInt
            val drawTop = drawRect.top + i * textHeight
            val drawBottom = drawTop + textHeight
            val baseline = (drawTop + drawBottom - fontMetrics.bottom - fontMetrics.top) / 2f
            canvas.drawText(text, drawRect.centerX().toFloat(), baseline, paint)
        }
    }


    /**
     * 设置标签
     * @param tags 标签
     */
    fun setTags(tags: List<String>) {
        this.tags.clear()
        this.tags.addAll(tags)
        reTypeSetting()
    }

    /**
     * 设置标签
     * @param tags 标签
     */
    fun setTags(vararg tags: String) = setTags(tags.toList())

    /**
     * 重新排版
     */
    fun reTypeSetting() {
        if (tags.isNotEmpty() && width > 0 && height > 0)
            typeSetting()
    }


    /**
     * 设置标签颜色
     * @param primaryTagColor 主标签颜色
     * @param secondaryTagColor 填充的标签颜色
     */
    fun setTextColor(primaryTagColor: Int, secondaryTagColor: Int) {
        this.primaryTextColor = primaryTagColor
        this.secondaryTextColor = secondaryTagColor
        reTypeSetting()
    }


    /**
     * 设置文字大小
     * @param textSize 文字大小
     */
    fun setTextSize(textSize: Float) {
        maxTextSize = if (textSize < minTextSize) minTextSize else textSize
        reTypeSetting()
    }


    private data class TagModel(val rect: Rect, val textColor: Int = 0, val textSize: Float = 0f,
                                val isFakeBoldText: Boolean = false, val text: String = ""
    )

    private data class Size(val width: Int, val height: Int)

    private enum class Orientation {
        VERTICAL,
        HORIZONTAL
    }

}