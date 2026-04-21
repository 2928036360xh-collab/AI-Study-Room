package com.example.end_side.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.end_side.R
import com.example.end_side.data.entity.WordItem
import com.example.end_side.util.TimeUtils

/**
 * 生词本列表适配器
 * 使用标准 RecyclerView.Adapter + ViewHolder 模式
 */
class WordListAdapter(
    private val onDeleteClick: (WordItem) -> Unit,
    private val onItemClick: (WordItem) -> Unit
) : ListAdapter<WordItem, WordListAdapter.WordViewHolder>(WordDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_word, parent, false)
        return WordViewHolder(view)
    }

    override fun onBindViewHolder(holder: WordViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class WordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvWord: TextView = itemView.findViewById(R.id.tv_word)
        private val tvTranslation: TextView = itemView.findViewById(R.id.tv_translation)
        private val tvAddTime: TextView = itemView.findViewById(R.id.tv_add_time)
        private val btnDelete: ImageView = itemView.findViewById(R.id.btn_delete_word)

        fun bind(word: WordItem) {
            tvWord.text = word.word
            tvTranslation.text = word.translation.ifBlank { "点击查询释义" }
            tvAddTime.text = TimeUtils.formatTimestamp(word.addTime)

            btnDelete.setOnClickListener { onDeleteClick(word) }
            itemView.setOnClickListener { onItemClick(word) }
        }
    }

    class WordDiffCallback : DiffUtil.ItemCallback<WordItem>() {
        override fun areItemsTheSame(oldItem: WordItem, newItem: WordItem) =
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: WordItem, newItem: WordItem) =
            oldItem == newItem
    }
}
