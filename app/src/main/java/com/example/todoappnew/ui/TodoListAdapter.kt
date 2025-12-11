package com.example.todoappnew.ui

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.todoappnew.databinding.ItemTodoBinding
import com.example.todoappnew.model.TodoItem

class TodoListAdapter(
    items: MutableList<TodoItem> = mutableListOf(),
    private val onItemsChanged: (() -> Unit)? = null   // 永続化など外部への通知用
) : RecyclerView.Adapter<TodoListAdapter.TodoViewHolder>() {

    // 永続化・フィルタのベースとなる「全タスク」
    private val allItems = mutableListOf<TodoItem>().apply { addAll(items) }
    // 現在のフィルタに基づき表示するタスク
    private val displayItems = mutableListOf<TodoItem>().apply { addAll(items) }

    enum class FilterType { ALL, INCOMPLETE, COMPLETE }

    private var currentFilter: FilterType = FilterType.ALL

    inner class TodoViewHolder(val binding: ItemTodoBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val binding = ItemTodoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TodoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        val item = displayItems[position]
        val binding = holder.binding

        // リスナーを一旦解除（再利用時の誤作動防止）
        binding.cbDone.setOnCheckedChangeListener(null)

        binding.tvTitle.text = item.title
        binding.cbDone.isChecked = item.isDone
        applyDoneStyle(binding, item.isDone)

        // チェック状態変更時
        binding.cbDone.setOnCheckedChangeListener { _, isChecked ->
            val id = item.id

            // allItems 側を ID で更新
            val indexInAll = allItems.indexOfFirst { it.id == id }
            if (indexInAll != -1) {
                allItems[indexInAll] = allItems[indexInAll].copy(isDone = isChecked)
            }

            applyDoneStyle(binding, isChecked)

            // 現在のフィルタに合わせて再表示 ＋ 変更通知
            binding.root.post {
                applyFilterInternal()
                onItemsChanged?.invoke()
            }
        }
    }

    override fun getItemCount(): Int = displayItems.size

    // =========================================
    // 既存機能：追加／まとめて削除／フィルタ／取得
    // =========================================

    /** タスク追加 */
    fun addItem(item: TodoItem) {
        allItems.add(item)
        applyFilterInternal()
        onItemsChanged?.invoke()
    }

    /** チェック済みタスクが1件以上あるか */
    fun hasCheckedItems(): Boolean {
        return allItems.any { it.isDone }
    }

    /** チェック済みタスクを一括削除（既存の削除ボタン向け） */
    fun removeCheckedItems() {
        allItems.removeAll { it.isDone }
        applyFilterInternal()
        onItemsChanged?.invoke()
    }

    /** フィルタ変更 */
    fun setFilter(filter: FilterType) {
        currentFilter = filter
        applyFilterInternal()
    }

    /** 永続化用：全タスク一覧を取得 */
    fun getAllItems(): List<TodoItem> = allItems.toList()

    // =========================================
    // ★ Step 11 追加分：スワイプ対応 API
    // =========================================

    /** 現在の表示リスト上の position に対応する TodoItem を返す（スワイプ用） */
    fun getItemAt(position: Int): TodoItem? {
        return if (position in 0 until displayItems.size) {
            displayItems[position]
        } else {
            null
        }
    }

    /** ID を指定して単一タスクを削除（左スワイプ用） */
    fun deleteItemById(id: Long) {
        // allItems から該当のものを削除
        val removed = allItems.removeIf { it.id == id }
        if (removed) {
            applyFilterInternal()
            onItemsChanged?.invoke()
        }
    }

    /** ID を指定してタイトルを更新（右スワイプ編集用） */
    fun updateTitle(id: Long, newTitle: String) {
        val indexInAll = allItems.indexOfFirst { it.id == id }
        if (indexInAll != -1) {
            val old = allItems[indexInAll]
            allItems[indexInAll] = old.copy(title = newTitle)
            applyFilterInternal()
            onItemsChanged?.invoke()
        }
    }

    // =========================================
    // 内部ヘルパ
    // =========================================

    /** 完了状態に応じて TextView の見た目を変更 */
    private fun applyDoneStyle(binding: ItemTodoBinding, isDone: Boolean) {
        if (isDone) {
            binding.tvTitle.paintFlags =
                binding.tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            binding.tvTitle.alpha = 0.5f
        } else {
            binding.tvTitle.paintFlags =
                binding.tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            binding.tvTitle.alpha = 1.0f
        }
    }

    /** currentFilter に基づいて displayItems を再構成し、RecyclerView を更新 */
    private fun applyFilterInternal() {
        displayItems.clear()
        val filtered = when (currentFilter) {
            FilterType.ALL -> allItems
            FilterType.INCOMPLETE -> allItems.filter { !it.isDone }
            FilterType.COMPLETE -> allItems.filter { it.isDone }
        }
        displayItems.addAll(filtered)
        notifyDataSetChanged()
    }
}
