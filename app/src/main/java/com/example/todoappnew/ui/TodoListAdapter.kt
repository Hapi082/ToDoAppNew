package com.example.todoappnew.ui

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.todoappnew.databinding.ItemTodoBinding
import com.example.todoappnew.model.TodoItem

class TodoListAdapter(
    items: MutableList<TodoItem> = mutableListOf()
) : RecyclerView.Adapter<TodoListAdapter.TodoViewHolder>() {

    // 全データ
    private val allItems = mutableListOf<TodoItem>().apply { addAll(items) }
    // 表示用データ
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

        // リスナーを一旦外す（再利用対策）
        binding.cbDone.setOnCheckedChangeListener(null)

        binding.tvTitle.text = item.title
        binding.cbDone.isChecked = item.isDone
        applyDoneStyle(binding, item.isDone)

        // チェック変更時
        binding.cbDone.setOnCheckedChangeListener { _, isChecked ->
            val id = item.id

            // allItems 側を更新
            val indexInAll = allItems.indexOfFirst { it.id == id }
            if (indexInAll != -1) {
                allItems[indexInAll] = allItems[indexInAll].copy(isDone = isChecked)
            }

            // 見た目更新
            applyDoneStyle(binding, isChecked)

            // 現在のフィルタ条件に合わせて再表示
            binding.root.post {
                applyFilterInternal()
            }
        }
    }

    override fun getItemCount(): Int = displayItems.size

    /** タスク追加 */
    fun addItem(item: TodoItem) {
        allItems.add(item)
        applyFilterInternal()
    }

    /** チェック済みタスクが存在するか */
    fun hasCheckedItems(): Boolean {
        return allItems.any { it.isDone }
    }

    /** チェック済みタスクを一括削除 */
    fun removeCheckedItems() {
        allItems.removeAll { it.isDone }
        applyFilterInternal()
    }

    /** フィルタ種別を変更 */
    fun setFilter(filter: FilterType) {
        currentFilter = filter
        applyFilterInternal()
    }

    /** 完了状態に応じた見た目変更 */
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

    /** 現在のフィルタ条件に従って displayItems を作り直す */
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
