package com.example.psychologist.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.psychologist.R
import com.example.psychologist.database.entity.Conversation
import com.example.psychologist.util.DateUtils

class ConversationAdapter(
    private val onConversationClick: (Conversation) -> Unit,
    private val onConversationDelete: (Conversation) -> Unit
) : ListAdapter<Conversation, ConversationAdapter.ConversationViewHolder>(DiffCallback()) {

    class DiffCallback : DiffUtil.ItemCallback<Conversation>() {
        override fun areItemsTheSame(oldItem: Conversation, newItem: Conversation): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Conversation, newItem: Conversation): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conversation, parent, false)
        return ConversationViewHolder(view, onConversationClick, onConversationDelete)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ConversationViewHolder(
        itemView: View,
        private val onConversationClick: (Conversation) -> Unit,
        private val onConversationDelete: (Conversation) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val titleTextView: TextView = itemView.findViewById(R.id.text_conversation_title)
        private val timeTextView: TextView = itemView.findViewById(R.id.text_conversation_time)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.button_delete)

        fun bind(conversation: Conversation) {
            titleTextView.text = conversation.title
            timeTextView.text = DateUtils.formatConversationTime(conversation.updatedAt)

            itemView.setOnClickListener {
                onConversationClick(conversation)
            }

            deleteButton.setOnClickListener {
                onConversationDelete(conversation)
            }
        }
    }
}