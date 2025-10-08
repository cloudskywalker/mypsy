package com.example.psychologist.adapter

import android.content.Context
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.psychologist.R
import com.example.psychologist.database.entity.Message
import com.example.psychologist.util.DateUtils
import com.example.psychologist.util.MarkdownUtils

class ChatAdapter(
    private val onEditMessage: (Message) -> Unit,
    private val onDeleteMessage: (Message) -> Unit,
    private val recyclerView: RecyclerView? = null
) : ListAdapter<Message, RecyclerView.ViewHolder>(DiffCallback()) {

    companion object {
        private const val TYPE_USER = 1
        private const val TYPE_ASSISTANT = 2
    }

    class DiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position).sender) {
            "user" -> TYPE_USER
            else -> TYPE_ASSISTANT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_USER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_sent, parent, false)
                UserMessageViewHolder(view, onEditMessage, onDeleteMessage)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_received, parent, false)
                AssistantMessageViewHolder(view)
            }
        }
    }

    private var streamingMessageId: Long = -1

    // 其他代码保持不变...

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is UserMessageViewHolder -> holder.bind(message)
            is AssistantMessageViewHolder -> {
                holder.bind(message)
                // 使用消息ID而不是位置来跟踪流式消息
                if (message.isStreaming) {
                    streamingMessageId = message.id
                }
            }
        }
    }

    fun updateStreamingMessage(content: String) {
        if (streamingMessageId != -1L) {
            val currentList = currentList.toMutableList()
            // 通过ID找到消息的位置
            val position = currentList.indexOfFirst { it.id == streamingMessageId }
            if (position != -1) {
                val message = currentList[position]
                val updatedMessage = message.copy(content = content)
                currentList[position] = updatedMessage
                submitList(currentList) {
                    notifyItemChanged(position)
                    // 如果需要滚动到最后一条消息
                    if (position == currentList.size - 1) {
                        recyclerView?.scrollToPosition(currentList.size - 1)
                    }
                }
            }
        }
    }

    fun finishStreaming() {
        streamingMessageId = -1
    }

    class UserMessageViewHolder(
        itemView: View,
        private val onEditMessage: (Message) -> Unit,
        private val onDeleteMessage: (Message) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.text_message_body)
        private val messageTime: TextView = itemView.findViewById(R.id.text_message_time)
        private val messageStatus: ImageView = itemView.findViewById(R.id.text_message_status)
        private val messageMenu: ImageView = itemView.findViewById(R.id.message_menu)

        fun bind(message: Message) {
            messageText.text = message.content
            messageTime.text = DateUtils.formatMessageTime(message.timestamp)

            // 根据消息长度动态调整最大宽度
            val maxWidth = if (message.content.length > 100) {
                // 长消息使用较大宽度
                320.dpToPx(itemView.context)
            } else {
                // 短消息使用较小宽度
                240.dpToPx(itemView.context)
            }
            messageText.maxWidth = maxWidth

            // 设置消息状态
            when (message.status) {
                "sending" -> messageStatus.setImageResource(R.drawable.ic_sending)
                "sent" -> messageStatus.setImageResource(R.drawable.ic_sent)
                "error" -> messageStatus.setImageResource(R.drawable.ic_error)
            }

            // 设置菜单点击事件
            messageMenu.setOnClickListener {
                showPopupMenu(it, message)
            }
        }

        // 扩展函数：dp转px
        private fun Int.dpToPx(context: Context): Int {
            return (this * context.resources.displayMetrics.density).toInt()
        }

        private fun showPopupMenu(view: View, message: Message) {
            val popup = PopupMenu(view.context, view)
            popup.menuInflater.inflate(R.menu.menu_message, popup.menu)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_edit -> {
                        onEditMessage(message)
                        true
                    }
                    R.id.menu_delete -> {
                        onDeleteMessage(message)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    class AssistantMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.text_message_body)
        private val messageTime: TextView = itemView.findViewById(R.id.text_message_time)

        fun bind(message: Message) {
//            // 确保文本正确换行
//            messageText.text = message.content
            // 使用 Markdown 渲染 AI 消息
            MarkdownUtils.renderMarkdown(messageText, message.content)
            messageTime.text = DateUtils.formatMessageTime(message.timestamp)

            // 通过代码设置 LinkMovementMethod，使链接可点击
            messageText.movementMethod = LinkMovementMethod.getInstance()
        }
    }
}