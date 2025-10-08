package com.example.psychologist.util

import android.content.Context
import android.text.Spanned
import android.widget.TextView
import io.noties.markwon.Markwon
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.ImagesPlugin
import io.noties.markwon.linkify.LinkifyPlugin

object MarkdownUtils {
    private var markwon: Markwon? = null

    fun initialize(context: Context) {
        if (markwon == null) {
            markwon = Markwon.builder(context)
                .usePlugin(HtmlPlugin.create())
                .usePlugin(LinkifyPlugin.create())
                .usePlugin(StrikethroughPlugin.create())
                .usePlugin(TablePlugin.create(context))
                .usePlugin(TaskListPlugin.create(context))
                .usePlugin(ImagesPlugin.create())
                .usePlugin(object : io.noties.markwon.AbstractMarkwonPlugin() {
                    override fun configureTheme(builder: MarkwonTheme.Builder) {
                        builder
                            .codeTextColor(context.getColor(android.R.color.black))
                            .codeBackgroundColor(context.getColor(android.R.color.darker_gray))
                            .blockQuoteColor(context.getColor(android.R.color.holo_blue_light))
                            .linkColor(context.getColor(android.R.color.holo_blue_dark))
                            .blockMargin(20)
                            .headingTextSizeMultipliers(floatArrayOf(1.6f, 1.5f, 1.4f, 1.3f, 1.2f, 1.1f))
                    }
                })
                .build()
        }
    }

    fun renderMarkdown(textView: TextView, markdown: String) {
        val processedMarkdown = optimizedMarkdownPreprocess(markdown)
        markwon?.setMarkdown(textView, processedMarkdown)
        textView.movementMethod = android.text.method.LinkMovementMethod.getInstance()
    }

    fun renderMarkdown(markdown: String): Spanned? {
        val processedMarkdown = optimizedMarkdownPreprocess(markdown)
        return markwon?.toMarkdown(processedMarkdown)
    }

    /**
     * 优化的 Markdown 预处理
     * 完善有序列表和分割线处理
     */
    private fun optimizedMarkdownPreprocess(content: String): String {
        if (content.isBlank()) return content

        val lines = content.lines()
        val processedLines = mutableListOf<String>()

        var inCodeBlock = false

        for (line in lines) {
            var processedLine = line

            // 检查代码块开始/结束
            if (line.trim().startsWith("```")) {
                inCodeBlock = !inCodeBlock
                processedLines.add(processedLine)
                continue
            }

            // 如果在代码块中，不做处理
            if (inCodeBlock) {
                processedLines.add(processedLine)
                continue
            }

            // 处理分割线 - 确保格式正确
            if (isValidThematicBreak(processedLine)) {
                processedLine = normalizeThematicBreak(processedLine)
                processedLines.add(processedLine)
                continue
            }

            // 处理列表项
            processedLine = processOptimizedListItem(processedLine)

            processedLines.add(processedLine)
        }

        // 处理换行 - 将单个换行转换为硬换行
        var result = processedLines.joinToString("\n")
        result = result.replace(Regex("(?<!\\n)\\n(?!\\n)"), "  \n")

        return result
    }

    /**
     * 检查是否是有效的分割线
     */
    private fun isValidThematicBreak(line: String): Boolean {
        val trimmed = line.trim()

        if (trimmed.isEmpty()) return false

        // 检查是否只包含分割线字符（- * _）和空格
        val contentOnly = trimmed.replace(Regex("""\s"""), "")

        // 如果去除空格后不是纯分割线字符，不是分割线
        if (!contentOnly.matches(Regex("""^[-*_]+$"""))) {
            return false
        }

        // 分割线必须至少3个字符
        if (contentOnly.length < 3) {
            return false
        }

        // 检查是否所有字符相同（标准分割线要求）
        val firstChar = contentOnly[0]
        if (!contentOnly.all { it == firstChar }) {
            return false
        }

        return true
    }

    /**
     * 标准化分割线格式
     * 确保分割线能够被 Markwon 正确识别和渲染
     */
    private fun normalizeThematicBreak(line: String): String {
        val trimmed = line.trim()

        // 提取分割线字符类型
        val firstChar = trimmed.replace(Regex("""\s"""), "")[0]

        // 创建标准化的分割线 - 使用三个相同的字符
        return "$firstChar$firstChar$firstChar"
    }

    /**
     * 优化的列表项处理
     * 完善有序列表数字前后没有空格的情况识别
     */
    private fun processOptimizedListItem(line: String): String {
        var processed = line

        // 检查是否是列表项（支持嵌套）
        if (isOptimizedListItem(processed)) {
            processed = fixOptimizedListItemFormat(processed)
        }

        return processed
    }

    /**
     * 优化的列表项识别
     */
    private fun isOptimizedListItem(line: String): Boolean {
        // 移除前导空格以检查列表标记
        val content = line.trimStart()

        if (content.isEmpty()) return false

        // 有序列表：数字 + 点/括号/顿号
        // 支持所有可能的空格情况
        val orderedPattern = Regex("""^\d+\s*[\.\)、]\s*.*""")

        // 无序列表：- * +
        val unorderedPattern = Regex("""^[-*+]\s*.*""")

        // 任务列表：- [ ] 或 - [x] 等
        val taskPattern = Regex("""^[-*+]\s*\[[ x]?\].*""")

        return content.matches(orderedPattern) ||
                content.matches(unorderedPattern) ||
                content.matches(taskPattern)
    }

    /**
     * 优化的列表项格式修复
     * 完善有序列表数字前后没有空格的情况处理
     */
    private fun fixOptimizedListItemFormat(line: String): String {
        var processed = line

        // 保存原始缩进（用于嵌套列表）
        val indent = line.takeWhile { it.isWhitespace() }
        val content = line.substring(indent.length)

        // 处理有序列表 - 完善所有空格情况
        val orderedResult = processOptimizedOrderedListItem(content)
        if (orderedResult != content) {
            return indent + orderedResult
        }

        // 处理无序列表
        val unorderedResult = processOptimizedUnorderedListItem(content)
        if (unorderedResult != content) {
            return indent + unorderedResult
        }

        // 处理任务列表
        val taskResult = processOptimizedTaskListItem(content)
        if (taskResult != content) {
            return indent + taskResult
        }

        return processed
    }

    /**
     * 优化的有序列表项处理
     * 完善有序列表数字前后没有空格的情况识别
     */
    private fun processOptimizedOrderedListItem(content: String): String {
        var processed = content

        // 情况1: 前后都没有空格 - "1.内容"
        val pattern1 = Regex("""^(\d+)([\.\)、])(\S.*)""")
        processed = pattern1.replace(processed) { match ->
            val (number, delimiter, restContent) = match.destructured
            "$number$delimiter $restContent"
        }

        // 情况2: 前有空格后没有 - "1. 内容" (标准格式，不需要处理)
        // 这种情况已经是标准格式，不需要修改

        // 情况3: 前没有空格后有 - "1. 内容" (实际上与情况2相同)
        // 这种情况也是标准格式，不需要修改

        // 情况4: 前后都有空格 - "1 . 内容" (不常见，但需要处理)
        val pattern4 = Regex("""^(\d+)\s+([\.\)、])\s+(\S.*)""")
        processed = pattern4.replace(processed) { match ->
            val (number, delimiter, restContent) = match.destructured
            "$number$delimiter $restContent"
        }

        // 特殊情况: 数字后有点/括号/顿号，但后面直接跟的是空格或换行
        // 例如: "1. " 或 "1.  " 等
        val emptyContentPattern = Regex("""^(\d+)([\.\)、])\s*$""")
        processed = emptyContentPattern.replace(processed) { match ->
            val (number, delimiter) = match.destructured
            "$number$delimiter "
        }

        return processed
    }

    /**
     * 优化的无序列表项处理
     */
    private fun processOptimizedUnorderedListItem(content: String): String {
        var processed = content

        // 情况1: 符号后没有空格 - "-内容"
        val pattern1 = Regex("""^([-*+])(\S.*)""")
        processed = pattern1.replace(processed) { match ->
            val (bullet, restContent) = match.destructured
            "$bullet $restContent"
        }

        // 特殊情况: 符号后直接跟的是空格或换行
        // 例如: "- " 或 "-  " 等
        val emptyContentPattern = Regex("""^([-*+])\s*$""")
        processed = emptyContentPattern.replace(processed) { match ->
            val (bullet) = match.destructured
            "$bullet "
        }

        return processed
    }

    /**
     * 优化的任务列表项处理
     */
    private fun processOptimizedTaskListItem(content: String): String {
        var processed = content

        // 情况1: 完全没有空格 - "-[]内容"
        val pattern1 = Regex("""^([-*+])\[([ x]?)\](\S.*)""")
        processed = pattern1.replace(processed) { match ->
            val (bullet, status, restContent) = match.destructured
            val checkedStatus = if (status.isBlank()) " " else status
            "$bullet [$checkedStatus] $restContent"
        }

        // 情况2: 方括号内有空格，但其他位置没有 - "-[ ]内容"
        val pattern2 = Regex("""^([-*+])\[\s([ x]?)\s\](\S.*)""")
        processed = pattern2.replace(processed) { match ->
            val (bullet, status, restContent) = match.destructured
            val checkedStatus = if (status.isBlank()) " " else status
            "$bullet [$checkedStatus] $restContent"
        }

        // 特殊情况: 任务列表后没有内容
        // 例如: "-[]" 或 "-[ ]" 等
        val emptyContentPattern = Regex("""^([-*+])\[([ x]?)\]\s*$""")
        processed = emptyContentPattern.replace(processed) { match ->
            val (bullet, status) = match.destructured
            val checkedStatus = if (status.isBlank()) " " else status
            "$bullet [$checkedStatus] "
        }

        return processed
    }
}