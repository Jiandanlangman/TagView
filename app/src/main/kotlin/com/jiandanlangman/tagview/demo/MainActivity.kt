package com.jiandanlangman.tagview.demo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.jiandanlangman.tagview.TagView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val tagView = findViewById<TagView>(R.id.tagView)
        tagView.setColors(0xFF333333.toInt(), 0xFF999999.toInt())
        val tags = ArrayList<String>()
        tags.add("小清新")
        tags.add("萌萌哒")
        tags.add("鸡头")
        tags.add("只爱英剧")
        tags.add("大叔")
        tags.add("铲屎官")
        tags.add("铲屎官")
        tagView.setTags(tags)
        findViewById<View>(R.id.button).setOnClickListener { tagView.reTypeSetting() }
    }
}
