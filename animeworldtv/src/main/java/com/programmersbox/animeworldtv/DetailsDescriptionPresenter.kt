package com.programmersbox.animeworldtv

import androidx.leanback.widget.AbstractDetailsDescriptionPresenter
import com.programmersbox.models.InfoModel

class DetailsDescriptionPresenter(private val model: InfoModel?) : AbstractDetailsDescriptionPresenter() {

    override fun onBindDescription(
        viewHolder: AbstractDetailsDescriptionPresenter.ViewHolder,
        item: Any
    ) {
        val movie = model//item as ItemModel

        viewHolder.title.text = movie?.title
        viewHolder.subtitle.text = movie?.genres?.joinToString("\t")//movie?.source?.serviceName
        viewHolder.body.text = movie?.description

    }
}