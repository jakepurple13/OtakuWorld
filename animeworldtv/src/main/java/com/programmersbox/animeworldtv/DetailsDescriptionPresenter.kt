package com.programmersbox.animeworldtv

import androidx.leanback.widget.AbstractDetailsDescriptionPresenter
import com.programmersbox.models.ItemModel

class DetailsDescriptionPresenter : AbstractDetailsDescriptionPresenter() {

    override fun onBindDescription(
        viewHolder: AbstractDetailsDescriptionPresenter.ViewHolder,
        item: Any
    ) {
        val movie = item as ItemModel

        viewHolder.title.text = movie.title
        viewHolder.subtitle.text = movie.source.serviceName
        viewHolder.body.text = movie.description
    }
}