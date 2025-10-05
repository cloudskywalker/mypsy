package com.example.psychologist.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.psychologist.R
import com.example.psychologist.adapter.ChatAdapter
import com.example.psychologist.database.AppDatabase
import com.example.psychologist.database.entity.Message
import com.example.psychologist.database.repository.ConversationRepository
import com.example.psychologist.database.repository.MessageRepository
import com.example.psychologist.databinding.ActivityMainBinding
import com.example.psychologist.util.NetworkUtils
import com.example.psychologist.util.ThemeUtils
import com.example.psychologist.viewmodel.ChatViewModel
import com.example.psychologist.viewmodel.ChatViewModelFactory
import com.google.android.material.snackbar.Snackbar
import android.content.pm.PackageManager
import androidx.appcompat.widget.SwitchCompat
import kotlinx.coroutines.flow.first

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var chatAdapter: ChatAdapter

    // 使用 viewModels 委托来获取 ViewModel
    private val viewModel: ChatViewModel by viewModels {
        val database = AppDatabase.getDatabase(this)
        val messageRepository = MessageRepository(database.messageDao())
        val conversationRepository = ConversationRepository(database.conversationDao())
        ChatViewModelFactory(messageRepository, conversationRepository)
    }

    companion object {
        private const val REQUEST_CODE_CHAT_HISTORY = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
        checkNetworkState()

        // 设置适配器引用
        viewModel.setAdapter(chatAdapter)
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(
            onEditMessage = { message ->
                showEditDialog(message)
            },
            onDeleteMessage = { message ->
                viewModel.deleteMessage(message.id)
            },
            recyclerView = binding.recyclerViewMessages // 传递RecyclerView引用
        )

        binding.recyclerViewMessages.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(this@MainActivity).apply {
                stackFromEnd = true
            }
            addItemDecoration(DividerItemDecoration(this@MainActivity, DividerItemDecoration.VERTICAL))
        }
    }

    private fun setupClickListeners() {
        binding.buttonSend.setOnClickListener {
            val message = binding.editTextMessage.text.toString().trim()
            if (message.isNotEmpty()) {
                viewModel.sendMessage(message)
                binding.editTextMessage.setText("")
            }
        }
    }

    private fun observeViewModel() {
        viewModel.messages.observe(this) { messages ->
            chatAdapter.submitList(messages) {
                binding.recyclerViewMessages.scrollToPosition(messages.size - 1)
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        // 观察事件
        viewModel.snackbarEvent.observe(this) { event ->
            event?.getContentIfNotHandled()?.let { message ->
                Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEditDialog(message: Message) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_message, null)
        val editText = dialogView.findViewById<EditText>(R.id.edit_text_message)
        editText.setText(message.content)

        AlertDialog.Builder(this)
            .setTitle("编辑消息")
            .setView(dialogView)
            .setPositiveButton("保存") { _, _ ->
                val newContent = editText.text.toString().trim()
                if (newContent.isNotEmpty()) {
                    viewModel.editMessage(message.id, newContent)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun checkNetworkState() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Snackbar.make(binding.root, "网络不可用，请检查网络连接", Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_CHAT_HISTORY && resultCode == RESULT_OK) {
            data?.let {
                if (it.getBooleanExtra("newConversation", false)) {
                    // 创建新对话
                    viewModel.createNewConversation()
                } else {
                    val conversationId = it.getLongExtra("conversationId", -1)
                    if (conversationId != -1L) {
                        // 加载选中的对话
                        viewModel.loadConversation(conversationId)
                    }
                }
            }
        }
    }

    // 添加菜单创建和处理方法
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_history -> {
                // 打开对话历史
                val intent = Intent(this, ChatHistoryActivity::class.java)
                startActivityForResult(intent, REQUEST_CODE_CHAT_HISTORY)
                true
            }
            R.id.action_settings -> {
                // 打开设置
                showSettingsDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showSettingsDialog() {
        val items = arrayOf("主题设置", "通知设置", "清除聊天记录", "关于应用")

        AlertDialog.Builder(this)
            .setTitle("设置")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> showThemeSettings()
                    1 -> showNotificationSettings()
                    2 -> confirmClearChatHistory()
                    3 -> showAboutDialog()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showNotificationSettings() {
        val notificationPrefs = getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
        val isEnabled = notificationPrefs.getBoolean("notifications_enabled", true)

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_notification_settings, null)
        val switch = dialogView.findViewById<SwitchCompat>(R.id.switch_notifications)
        switch.isChecked = isEnabled

        AlertDialog.Builder(this)
            .setTitle("通知设置")
            .setView(dialogView)
            .setPositiveButton("保存") { dialog, _ ->
                val isChecked = switch.isChecked
                notificationPrefs.edit().putBoolean("notifications_enabled", isChecked).apply()
                Snackbar.make(binding.root, "通知设置已保存", Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun confirmClearChatHistory() {
        AlertDialog.Builder(this)
            .setTitle("清除聊天记录")
            .setMessage("确定要清除所有聊天记录吗？此操作不可恢复。")
            .setPositiveButton("清除") { _, _ ->
                viewModel.clearAllConversations()
                Snackbar.make(binding.root, "聊天记录已清除", Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showAboutDialog() {
        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            "1.0.0"
        }

        AlertDialog.Builder(this)
            .setTitle("关于应用")
            .setMessage("Psychologist AI聊天助手\n版本: $versionName\n\n基于大模型的智能心理聊天应用，为您提供专业的心理支持和帮助。")
            .setPositiveButton("确定", null)
            .show()
    }

    private fun showThemeSettings() {
        val themes = arrayOf("浅色模式", "深色模式", "跟随系统")
        val currentTheme = ThemeUtils.getSavedTheme(this)

        AlertDialog.Builder(this)
            .setTitle("主题设置")
            .setSingleChoiceItems(themes, currentTheme) { dialog, which ->
                // 立即应用主题更改预览
                ThemeUtils.applyTheme(which)
            }
            .setPositiveButton("应用") { dialog, which ->
                // 保存主题设置
                val selectedTheme = (dialog as AlertDialog).listView.checkedItemPosition
                ThemeUtils.saveTheme(this, selectedTheme)
                // 重新启动Activity以完全应用主题
                recreate()
            }
            .setNegativeButton("取消") { dialog, which ->
                // 恢复之前的主题设置
                ThemeUtils.applyTheme(currentTheme)
            }
            .setOnCancelListener {
                // 恢复之前的主题设置
                ThemeUtils.applyTheme(currentTheme)
            }
            .show()
    }
}