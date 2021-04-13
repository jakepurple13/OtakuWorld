package com.programmersbox.mangaworld

import android.content.Context
import android.graphics.Bitmap
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.helpfulutils.layoutInflater
import com.programmersbox.mangaworld.databinding.MangaGalleryItemBinding
import com.programmersbox.models.ItemModel
import com.programmersbox.thirdpartyutils.into
import com.programmersbox.uiviews.BaseListFragment
import com.programmersbox.uiviews.ItemListAdapter


class MangaGalleryAdapter(context: Context, baseListFragment: BaseListFragment) :
    ItemListAdapter<GalleryHolder>(context, baseListFragment) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryHolder =
        GalleryHolder(MangaGalleryItemBinding.inflate(context.layoutInflater, parent, false))

    override fun GalleryHolder.onBind(item: ItemModel, position: Int) {

        bind(item)
        itemView.setOnClickListener { onClick(it, item) }
        Glide.with(context)
            .asBitmap()
            //.load(item.imageUrl)
            .load(item.imageUrl)
            //.override(360, 480)
            .fitCenter()
            .transform(RoundedCorners(15))
            //.fallback(R.drawable.manga_world_round_logo)
            //.placeholder(R.drawable.manga_world_round_logo)
            //.error(R.drawable.manga_world_round_logo)
            .into<Bitmap> {
                resourceReady { image, _ ->
                    //cover.setImageBitmap(image.glowEffect(10, title.currentTextColor) ?: image)
                    cover.setImageBitmap(image)
                    /*if (context.usePalette) {
                        swatch = image.getPalette().vibrantSwatch
                    }*/
                }
            }
    }

    override val currentList: MutableList<DbModel> get() = mutableListOf()
    override val previousList: MutableList<DbModel> get() = mutableListOf()
    override fun update(list: List<DbModel>, check: (ItemModel, DbModel) -> Boolean) {}

}

class GalleryHolder(private val binding: MangaGalleryItemBinding) : RecyclerView.ViewHolder(binding.root) {
    val cover = binding.galleryListCover
    val title = binding.galleryListTitle
    val layout = binding.galleryListLayout

    fun bind(item: ItemModel) {
        binding.model = item
        binding.executePendingBindings()

    }
}