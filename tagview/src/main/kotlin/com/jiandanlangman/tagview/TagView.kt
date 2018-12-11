package com.jiandanlangman.tagview

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.ArrayList

/**
 * TagView 标签墙
 * @author liyang
 */
class TagView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    View(context, attrs, defStyleAttr) {

    private val lock = java.lang.Object()
    private val typeSettingThreadPool = Executors.newSingleThreadExecutor()

    private val tags = ArrayList<String>()
    private val paint = Paint()
    private var primaryTagColor = 0xFF333333.toInt()
    private var secondaryTagColor = 0xFF999999.toInt()
    private val drawRect = Rect()
    private val minTextSize = 4f * resources.displayMetrics.density
    private val primaryTagStrokeWidth = minTextSize / 8f
    private lateinit var random: Random
    private var bitmap: Bitmap? = null
    private var region: Region? = null


    init {
        paint.isAntiAlias = true
        paint.textAlign = Paint.Align.CENTER
        paint.style = Paint.Style.FILL_AND_STROKE
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
                drawRect.set(it.left, it.top, it.left + it.width, it.top + it.height)
                paint.textSize = it.textSize
                paint.color = it.textColor
                paint.strokeWidth = it.strokeWidth
                if (it.width >= it.height) {
                    val fontMetrics = paint.fontMetricsInt
                    val baseline = (drawRect.bottom + drawRect.top - fontMetrics.bottom - fontMetrics.top) / 2f
                    canvas.drawText(it.text, drawRect.centerX().toFloat(), baseline, paint)
                } else
                    drawVerticalTag(canvas, drawRect, it)
            }
            synchronized(lock) {
                bitmap = tempBitmap
            }
            postInvalidate()
        }
    }


    private fun primaryTypeSetting(region: Region): List<TagModel> {
        val result = ArrayList<TagModel>()
        val textMaxSize = calcMaxTextSize(width, height)
        tags.forEach {
            var textSize = if (tags.indexOf(it) == 0) textMaxSize else textMaxSize - minTextSize
            while (textSize >= minTextSize) {
                val tagModel = innerTypeSetting(region, textSize, primaryTagStrokeWidth, primaryTagColor, it)
                if (tagModel != null) {
                    result.add(tagModel)
                    break
                }
                textSize -= minTextSize
            }
        }
        return result
    }


    private fun secondaryTypeSetting(region: Region): List<TagModel> {
        val result = ArrayList<TagModel>()
        var textSize = calcMaxTextSize(width, height) - minTextSize * 2f
        if (textSize < minTextSize)
            textSize = minTextSize
        while (true) {
            var isAdded = false
            tags.forEach {
                val tagModel =
                    innerTypeSetting(region, textSize, 0f, secondaryTagColor, it)
                if (tagModel != null) {
                    isAdded = true
                    result.add(tagModel)
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
                var maxDrawTextLength = (Math.max(width, height) / minTextSize).toInt()
                var text: String? = null
                while (maxDrawTextLength >= 1 && text == null) {
                    text = tags.firstOrNull { it.length == maxDrawTextLength }
                    maxDrawTextLength--
                }
                if (text != null)
                    result.add(TagModel(rect.left, rect.top, width, height, secondaryTagColor, minTextSize, 0f, text))
            }
        }
        return result
    }


    private fun innerTypeSetting(
        region: Region,
        textSize: Float,
        strokeWidth: Float,
        textColor: Int,
        text: String
    ): TagModel? {
        var tagModel: TagModel? = null
        paint.textSize = textSize
        paint.strokeWidth = strokeWidth
        var width = paint.measureText(text).toInt()
        var height = paint.textSize.toInt()
        if (random.nextInt(3) == 2) {
            val t = width
            width = height
            height = t
        }
        val rit = RegionIterator(region)
        val rect = Rect()
        while (rit.next(rect)) {
            if (rect.width() >= width && rect.height() >= height) {
                val left: Int = if (rect.width() - width < minTextSize)
                    rect.left
                else
                    rect.left + (random.nextInt(((rect.width() - width) / minTextSize).toInt() + 1) * minTextSize).toInt()
                val top: Int = if (rect.height() - height < minTextSize)
                    rect.top
                else
                    rect.top + (random.nextInt(((rect.height() - height) / minTextSize).toInt() + 1) * minTextSize).toInt()
                region.op(Rect(left, top, left + width, top + height), Region.Op.DIFFERENCE)
                tagModel = TagModel(left, top, width, height, textColor, textSize, strokeWidth, text)
                break
            }
        }
        return tagModel
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        synchronized(lock) {
            if (bitmap != null)
                canvas.drawBitmap(bitmap!!, 0f, 0f, paint)
        }
    }


    private fun drawVerticalTag(canvas: Canvas, drawRect: Rect, tag: TagModel) {
        paint.textSize = tag.textSize
        paint.color = tag.textColor
        paint.strokeWidth = tag.strokeWidth
        val textHeight = tag.height / tag.text.length
        for (i in 0 until tag.text.length) {
            val text = tag.text.substring(i, i + 1)
            val fontMetrics = paint.fontMetricsInt
            val drawTop = drawRect.top + i * textHeight
            val drawBottom = drawTop + textHeight
            val baseline = (drawTop + drawBottom - fontMetrics.bottom - fontMetrics.top) / 2f
            canvas.drawText(text, drawRect.centerX().toFloat(), baseline, paint)
        }
    }


    private fun calcMaxTextSize(canvasWidth: Int, canvasHeight: Int): Float {
        var length = 0
        tags.map { if (it.length > length) length = it.length }
        val min = Math.min(canvasHeight, canvasWidth)
        val tempSize = ((min * .8f) / length).toInt()
        return (tempSize - (tempSize % minTextSize))
    }


    /**
     * 设置标签
     * @param 标签
     */
    fun setTags(tags: List<String>) {
        this.tags.clear()
        this.tags.addAll(tags)
        reTypeSetting()
    }


    /**
     * 重新排版
     */
    fun reTypeSetting() {
        if (tags.isNotEmpty() && width > 0 && height > 0)
            typeSetting()
    }


    /**
     * 设置填充形状
     * @param region 填充形状，形状大小不能超出控件本身大小
     */
    fun setRegion(region: Region) {
        this.region = region
        reTypeSetting()
    }


    /**
     * 设置标签颜色
     * @param primaryTagColor 主标签颜色
     * @param secondaryTagColor 填充的标签颜色
     */
    fun setColors(primaryTagColor: Int, secondaryTagColor: Int) {
        this.primaryTagColor = primaryTagColor
        this.secondaryTagColor = secondaryTagColor
        reTypeSetting()
    }


    private data class TagModel(
        val left: Int = 0, val top: Int = 0, val width: Int = 0, val height: Int = 0,
        val textColor: Int = 0, val textSize: Float = 0f, val strokeWidth: Float = 0f, val text: String = ""
    )

}