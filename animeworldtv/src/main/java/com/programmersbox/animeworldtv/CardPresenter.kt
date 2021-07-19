package com.programmersbox.animeworldtv

import android.graphics.drawable.Drawable
import android.util.Log
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import com.bumptech.glide.Glide
import com.programmersbox.helpfulutils.layoutInflater
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.ItemModel
import kotlin.properties.Delegates

/**
 * A CardPresenter is used to generate Views and bind Objects to them on demand.
 * It contains an ImageCardView.
 */
class CardPresenter : Presenter() {
    private var mDefaultCardImage: Drawable? = null
    private var sSelectedBackgroundColor: Int by Delegates.notNull()
    private var sDefaultBackgroundColor: Int by Delegates.notNull()

    override fun onCreateViewHolder(parent: ViewGroup): Presenter.ViewHolder {
        Log.d(TAG, "onCreateViewHolder")

        sDefaultBackgroundColor = ContextCompat.getColor(parent.context, R.color.default_background)
        sSelectedBackgroundColor = ContextCompat.getColor(parent.context, R.color.selected_background)
        mDefaultCardImage = ContextCompat.getDrawable(parent.context, R.mipmap.ic_launcher)

        return Presenter.ViewHolder(
            parent.context.layoutInflater.inflate(R.layout.tv_item, parent, false).also {

                it.isFocusable = true
                it.isFocusableInTouchMode = true
                //updateCardBackgroundColor(cardView, false)
                (it as? CustomImageCardView)?.updateCardBackgroundColor(false)

            }
        )
    }

    override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any) {
        val movie = item as ItemModel
        val cardView = viewHolder.view as ImageCardView

        Log.d(TAG, "onBindViewHolder")
        cardView.titleText = movie.title
        cardView.contentText = movie.source.serviceName
        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
        Glide.with(viewHolder.view.context)
            .load(movie.imageUrl)
            .centerCrop()
            .placeholder(mDefaultCardImage)
            .error(mDefaultCardImage)
            .into(cardView.mainImageView)
    }

    override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder) {
        Log.d(TAG, "onUnbindViewHolder")
        val cardView = viewHolder.view as ImageCardView
        // Remove references to images so that the garbage collector can free up memory
        cardView.badgeImage = null
        cardView.mainImage = null
    }

    private fun updateCardBackgroundColor(view: ImageCardView, selected: Boolean) {
        val color = if (selected) sSelectedBackgroundColor else sDefaultBackgroundColor
        // Both background colors should be set because the view"s background is temporarily visible
        // during animations.
        view.setBackgroundColor(color)
        view.setInfoAreaBackgroundColor(color)
    }

    companion object {
        private val TAG = "CardPresenter"

        private val CARD_WIDTH = 313
        private val CARD_HEIGHT = 176
    }
}

class EpisodePresenter : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): Presenter.ViewHolder {

        return Presenter.ViewHolder(
            parent.context.layoutInflater.inflate(R.layout.episode_item, parent, false).also {

                it.isFocusable = true
                it.isFocusableInTouchMode = true

            }
        )
    }

    override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any) {
        val movie = item as ChapterModel
        val cardView = viewHolder.view.findViewById<TextView>(R.id.episode_name)
        cardView.text = movie.name
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder?) {

    }
}