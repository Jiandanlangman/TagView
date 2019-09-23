# TagView
仿腾讯QQ个人标签墙，云标签墙
该控件使用Kotlin编写，所以你的项目必须接入Kotlin才能正常使用
### 实现原理
采用求补集的方式切割矩形
### 效果预览
![ScreenRecord](https://github.com/Jiandanlangman/TagView/blob/master/screenshot.jpg)

### 接入方式
- 下载源码，将名为tagview的module加入到你的工程，或者将module打包成aar格式
- maven方式接入
    编辑你Project的build.gradle文件
    ```
    allprojects {
        repositories {
            google()
            jcenter()
            mavenCentral()
            maven { url "http://101.132.235.215/repor" } //加入这一行
        }
    }
    ```
    然后编辑你app module的build.gradle文件，在dependencies节点下加入
    ```
    implementation "com.jiandanlangman:tagview:1.0.1@aar"
    ```

### 主要API说明   
    ```
    //设置标签
    fun setTags(tags: List<String>)
    fun setTags(vararg tags: String)

    //重新排版
    fun reTypeSetting()

    //设置标签颜色
    fun setTextColor(primaryTagColor: Int, secondaryTagColor: Int)

    //设置标签文字大小
    fun setTextSize(textSize: Float)
    ```
标签颜色和文字大小还支持在xml布局文件中直接设置，具体设置方法参考demo
