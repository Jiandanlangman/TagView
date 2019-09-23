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
        tagView.setTags("乐群", "聪慧", "追剧", "世界上最快的男人", "大长腿", "双眼皮", "教你打扮", "情感陪护")
        findViewById<View>(R.id.button).setOnClickListener { tagView.reTypeSetting() }
    }
}
