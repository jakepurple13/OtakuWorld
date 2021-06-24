package com.programmersbox.uiviews.utils

import android.content.res.ColorStateList
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.programmersbox.helpfulutils.changeDrawableColor
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.SwatchInfo

@BindingAdapter("coverImage", "logoId")
fun loadImage(view: ImageView, imageUrl: String?, logoId: Int) {
    Glide.with(view)
        .load(imageUrl)
        .override(360, 480)
        .placeholder(logoId)
        .error(logoId)
        .fallback(logoId)
        .transform(RoundedCorners(15))
        .into(view)
}

@BindingAdapter("otherNames")
fun otherNames(view: TextView, names: List<String>?) {
    view.text = names?.joinToString("\n\n")
}

@BindingAdapter("genreList", "swatch")
fun loadGenres(view: ChipGroup, genres: List<String>?, swatchInfo: SwatchInfo?) {
    view.removeAllViews()
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

@BindingAdapter("toolbarColors")
fun toolbarColors(view: Toolbar, swatchInfo: SwatchInfo?) {
    swatchInfo?.titleColor?.let {
        view.setTitleTextColor(it)
        view.navigationIcon?.changeDrawableColor(it)
        view.setSubtitleTextColor(it)
        view.overflowIcon?.changeDrawableColor(it)
    }
    swatchInfo?.rgb?.let { view.setBackgroundColor(it) }
}

@BindingAdapter("collapsingToolbarColors")
fun collapsingToolbarColors(view: CollapsingToolbarLayout, swatchInfo: SwatchInfo?) {
    swatchInfo?.titleColor?.let { view.setCollapsedTitleTextColor(it) }
    swatchInfo?.rgb?.let {
        view.setBackgroundColor(it)
        //view.setExpandedTitleColor(it)
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

@BindingAdapter("startButtonColor")
fun startButtonColor(view: MaterialButton, swatchInfo: SwatchInfo?) {
    swatchInfo?.bodyColor?.let { view.iconTint = ColorStateList.valueOf(it) }
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