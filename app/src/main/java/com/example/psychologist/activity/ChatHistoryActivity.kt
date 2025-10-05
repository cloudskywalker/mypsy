package com.example.psychologist.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.psychologist.adapter.ConversationAdapter
import com.example.psychologist.database.AppDatabase
import com.example.psychologist.database.repository.ConversationRepository
import com.example.psychologist.databinding.ActivityChatHistoryBinding
import com.example.psychologist.viewmodel.ConversationViewModel
import com.example.psychologist.viewmodel.ConversationViewModelFactory

class ChatHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatHistoryBinding
    private lateinit var adapter: ConversationAdapter

    // 使用 viewModels 委托来获取 ViewModel
    private val viewModel: ConversationViewModel by viewModels {
        val database = AppDatabase.getDatabase(this)
        val conversationRepository = ConversationRepository(database.conversationDao())
        ConversationViewModelFactory(conversationRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = ConversationAdapter(
            onConversationClick = { conversation ->
                val intent = Intent().apply {
                    putExtra("conversationId", conversation.id)
                }
                setResult(RESULT_OK, intent)
                finish()
            },
            onConversationDelete = { conversation ->
                viewModel.deleteConversation(conversation)
            }
        )

        binding.recyclerViewConversations.apply {
            layoutManager = LinearLayoutManager(this@ChatHistoryActivity)
            adapter = this@ChatHistoryActivity.adapter
        }
    }

    private fun setupClickListeners() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.fabNewConversation.setOnClickListener {
            val intent = Intent().apply {
                putExtra("newConversation", true)
            }
            setResult(RESULT_OK, intent)
            finish()
        }
    }

    private fun observeViewModel() {
        viewModel.conversations.observe(this) { conversations ->
            adapter.submitList(conversations)
            binding.emptyState.visibility = if (conversations.isEmpty()) View.VISIBLE else View.GONE
        }
    }
}