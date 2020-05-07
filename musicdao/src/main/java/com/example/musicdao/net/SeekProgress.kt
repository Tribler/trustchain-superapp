package com.example.musicdao.net

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.TableLayout
import android.widget.TableRow

const val defaultName = "ProgressIndicatorView"

class SeekProgress(context: Context) : TableRow(context) {
    fun createSquares(amount: Int) {
        this.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        if (amount > 0) {
            val squareWidth = (this.parent as TableLayout).width / amount
            for (x in 1..amount) {
                this.addView(createSquare(x, squareWidth - 1))
                this.addView(View(context).apply { layoutParams = LayoutParams(1, squareWidth - 1) })
            }
        }
    }

    fun setSquareDownloaded(index: Int) {
        val square = this.findViewWithTag<View>(defaultName + index)
        square?.setBackgroundColor(Color.BLUE)
    }

    private fun createSquare(index: Int, width: Int): View {
        return View(context).apply {
            setBackgroundColor(Color.BLACK)
            layoutParams = LayoutParams(width, width)
            tag = defaultName + index
        }
    }
}
