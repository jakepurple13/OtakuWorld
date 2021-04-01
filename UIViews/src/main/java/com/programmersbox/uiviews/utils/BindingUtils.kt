package com.programmersbox.uiviews.utils

import android.content.res.ColorStateList
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.ItemModel
import com.programmersbox.models.SwatchInfo
import com.programmersbox.uiviews.R

@BindingAdapter("coverImage")
fun loadImage(view: ImageView, imageUrl: String?) {
    Glide.with(view)
        .load(imageUrl)
        .override(360, 480)
        /*.placeholder(R.drawable.manga_world_round_logo)
        .error(R.drawable.manga_world_round_logo)
        .fallback(R.drawable.manga_world_round_logo)*/
        .transform(RoundedCorners(15))
        .into(view)
}

@BindingAdapter("otherNames")
fun otherNames(view: TextView, names: List<String>?) {
    view.text = names?.joinToString("\n\n")
}

@BindingAdapter("genreList", "swatch")
fun loadGenres(view: ChipGroup, genres: List<String>?, swatchInfo: SwatchInfo?) {
    genres?.forEach {
        view.addView(Chip(view.context).apply {
            text = it
            isCheckable = false
            isClickable = false
            swatchInfo?.rgb?.let { setTextColor(it) }
            swatchInfo?.bodyColor?.let { chipBackgroundColor = ColorStateList.valueOf(it) }
        })
    }
}

@BindingAdapter("titleColor")
fun titleColor(view: TextView, swatchInfo: SwatchInfo?) {
    swatchInfo?.titleColor?.let { view.setTextColor(it) }
}

@BindingAdapter("bodyColor")
fun bodyColor(view: TextView, swatchInfo: SwatchInfo?) {
    swatchInfo?.bodyColor?.let { view.setTextColor(it) }
}

@BindingAdapter("linkColor")
fun linkColor(view: TextView, swatchInfo: SwatchInfo?) {
    swatchInfo?.bodyColor?.let { view.setLinkTextColor(it) }
}

@BindingAdapter("optionTint")
fun optionTint(view: MaterialButton, swatchInfo: SwatchInfo?) {
    swatchInfo?.rgb?.let { view.strokeColor = ColorStateList.valueOf(it) }
}

@BindingAdapter("checkedButtonTint")
fun buttonTint(view: CheckBox, swatchInfo: SwatchInfo?) {
    swatchInfo?.bodyColor?.let { view.buttonTintList = ColorStateList.valueOf(it) }
    swatchInfo?.bodyColor?.let { view.setTextColor(it) }
}

@BindingAdapter("uploadedText")
fun uploadedText(view: TextView, chapterModel: ChapterModel) {
    /*if (
        chapterModel.uploadedTime != null &&
        chapterModel.uploadedTime?.isDateBetween(System.currentTimeMillis() - 8.days.inMilliseconds.toLong(), System.currentTimeMillis()) == true
    ) {
        view.setTimeAgo(chapterModel.uploadedTime!!, showSeconds = true, autoUpdate = false)
    } else {

    }*/
    view.text = chapterModel.uploaded
}