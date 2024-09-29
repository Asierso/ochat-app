package com.asierso.ochat.components

import android.content.Context
import android.view.Gravity
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.ActionBar.LayoutParams
import androidx.appcompat.content.res.AppCompatResources
import androidx.cardview.widget.CardView
import androidx.core.widget.doOnTextChanged
import com.asierso.ochat.Global
import com.asierso.ochat.MainActivity.Side
import com.asierso.ochat.R
import com.google.android.material.card.MaterialCardView

class MessageCardView(private val context: Context, private val side: Side) {
    private var loadAnimation: LinearLayout? = null
    private var card: CardView? = null
    private var text: TextView? = null
    private var isLoading = true
    private var dots = 0

    init {
        //Create card
        card = MaterialCardView(context).apply {
            cardElevation =5f
            strokeWidth = 0

            //Set card resolution
            minimumHeight = Global.getPixels(context, 70)
            setContentPadding(
                Global.getPixels(context, 10),
                Global.getPixels(context, 10),
                Global.getPixels(context, 10),
                Global.getPixels(context, 10)
            )

            //Set card design
            if (side == Side.User){
                setBackgroundDrawable(
                    AppCompatResources.getDrawable(
                        context,
                        R.drawable.user_message_balloon
                    )
                )
                setCardBackgroundColor(context.getColor(R.color.user_wrote))
            }
            else {
                setBackgroundDrawable(
                    AppCompatResources.getDrawable(
                        context,
                        R.drawable.ia_message_balloon
                    )
                )
                setCardBackgroundColor(context.getColor(R.color.ia_wrote))
            }
            //Adjust layout
            layoutParams = LinearLayout.LayoutParams(
                Global.getPixels(context, 250),
                LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, Global.getPixels(context, 10), 0, Global.getPixels(context, 10))
                gravity = if (side == Side.User) Gravity.END else Gravity.START
            }


        }

        //Create text
        text = TextView(context).apply {
            text = ""
            textSize = 16f
            setTextColor(context.getColor(if (side == Side.User) R.color.user_wrote_fore else R.color.ia_wrote_fore))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        //Add dots container
        loadAnimation = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                150,150
            )
            orientation = LinearLayout.HORIZONTAL
        }

        //Create animation dots
        for(i in 0 until 3){
            loadAnimation!!.addView(createAnimationDot())
        }

        //Add text and animation to card
        card!!.addView(loadAnimation)
        card!!.addView(text)
        text!!.doOnTextChanged { _,_,_, count -> if(count > 0) card!!.removeView(loadAnimation) }
    }

    private fun createAnimationDot() : ImageView{
        //Create dot margins
        val dotParams = LinearLayout.LayoutParams(
            15,15
        )
        dotParams.setMargins(5,5,5,5)
        dots++

        return ImageView(context).apply {
            setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.dot))
            layoutParams = dotParams

            //Set animation for dots
            animation = AnimationUtils.loadAnimation(context, R.anim.load_shade)
            animation.startOffset = (dots * 50).toLong()
        }
    }

    fun getTextComponent() : TextView{
        return text!!
    }

    fun getView() : CardView{
        return card!!
    }

    fun stopLoading(){
        loadAnimation?.removeAllViews()
    }
}
