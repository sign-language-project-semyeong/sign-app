package com.bro.signtalk.ui.custom

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class SideIndexView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val indexList = arrayOf("★", "ㄱ", "ㄴ", "ㄷ", "ㄹ", "ㅁ", "ㅂ", "ㅅ", "ㅇ", "ㅈ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ", "#")
    private val paint = Paint().apply {
        color = Color.GRAY
        textSize = 30f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    // [핵심] 브로가 손가락으로 누를 때마다 어떤 글자인지 알려주는 리스너!
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val itemHeight = height / indexList.size
        for (i in indexList.indices) {
            val yPos = (itemHeight * i) + (itemHeight / 2f)
            canvas.drawText(indexList[i], width / 2f, yPos, paint)
        }
    }



    // SideIndexView.kt 내부 onTouchEvent 수정
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val itemHeight = height / indexList.size
        val index = (event.y / itemHeight).toInt().coerceIn(0, indexList.size - 1)

        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                // [팩폭] 누르고 있을 때만 글자 쏴준다!
                onIndexTouchListener?.invoke(indexList[index], true)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // [핵심] 손 떼면 숨기라고 신호 보내라 이말이야 유남생?!?
                onIndexTouchListener?.invoke(indexList[index], false)
            }
        }
        return true
    }

    // 리스너 정의도 살짝 바꿔라 브로! (글자, 표시여부)
    var onIndexTouchListener: ((String, Boolean) -> Unit)? = null
}

