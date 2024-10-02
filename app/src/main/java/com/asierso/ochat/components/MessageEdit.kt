package com.asierso.ochat.components

import android.content.Context
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import android.widget.TableRow
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.setMargins
import androidx.core.widget.doOnTextChanged
import com.asierso.ochat.Global
import com.asierso.ochat.R
import com.google.android.material.card.MaterialCardView

class MessageEdit (val context: Context) {
    private var card : MaterialCardView
    private var editText : EditText

    init {
        card = MaterialCardView(context).apply {
            elevation = 0f
            radius = 80f
            strokeWidth = 0
            layoutParams = getCardResizedLayout()
            setContentPadding(
                Global.getPixels(context, 15), //Left
                Global.getPixels(context, 5), //Top
                Global.getPixels(context, 15), //Right
                Global.getPixels(context, 5) //Bottom
            )
            setCardBackgroundColor(context.getColor(R.color.ed_message_background))

        }
        editText = EditText(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
            hint="Message here!"
            textSize= Global.getPixels(context, 5).toFloat()
            background = AppCompatResources.getDrawable(context,android.R.color.transparent)
        }

        editText.doOnTextChanged { text, start, before, count -> updateSizeAtChange(text,start,before,count) }
        card.addView(editText)

    }

    private fun updateSizeAtChange(text: CharSequence?, start: Int, before: Int, count: Int){
        var lines = editText.lineCount
        lines = if (lines > 10) 10 else lines

        card.layoutParams = getCardResizedLayout(lines)
    }

    private fun getCardResizedLayout(lines : Int = 1) : LinearLayout.LayoutParams{
        return LinearLayout.LayoutParams(
            context.resources.displayMetrics.widthPixels - 230,
            Global.getPixels(context, 50) + ((lines - 1) * Global.getPixels(context, 19))
        )
    }

    fun getMessageEditCard() : MaterialCardView{
        return card
    }

    fun getText() : String{
        return editText.text.toString()
    }

    fun clear(){
        editText.setText("")
    }
}