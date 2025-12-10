package com.example.todoappnew.ui

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.todoappnew.R
import com.example.todoappnew.model.TodoItem

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: TodoListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView = findViewById<RecyclerView>(R.id.rvTodoList)
        val etInput = findViewById<EditText>(R.id.etInput)
        val btnAdd = findViewById<Button>(R.id.btnAdd)
        val btnDelete = findViewById<Button>(R.id.btnDelete)
        val tvEmpty = findViewById<TextView>(R.id.tvEmpty)

        val rgFilter = findViewById<RadioGroup>(R.id.rgFilter)
        val rbAll = findViewById<RadioButton>(R.id.rbAll)
        val rbIncomplete = findViewById<RadioButton>(R.id.rbIncomplete)
        val rbComplete = findViewById<RadioButton>(R.id.rbComplete)

        adapter = TodoListAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        updateEmptyView(tvEmpty)

        // 追加ボタン
        btnAdd.setOnClickListener {
            val text = etInput.text.toString().trim()

            if (text.isEmpty()) {
                Toast.makeText(this, "タスクを入力してください", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newItem = TodoItem(
                id = System.currentTimeMillis(),
                title = text,
                isDone = false
            )

            adapter.addItem(newItem)
            etInput.text.clear()
            updateEmptyView(tvEmpty)
        }

        // 削除ボタン（チェック済みタスクを一括削除）
        btnDelete.setOnClickListener {
            if (!adapter.hasCheckedItems()) {
                Toast.makeText(this, "削除対象のタスクがありません", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AlertDialog.Builder(this)
                .setTitle("完了したタスクを削除しますか？")
                .setMessage("チェックが付いているタスクをすべて削除します")
                .setPositiveButton("削除") { _, _ ->
                    adapter.removeCheckedItems()
                    updateEmptyView(tvEmpty)
                }
                .setNegativeButton("キャンセル", null)
                .show()
        }

        // フィルタ切り替え
        rgFilter.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                rbAll.id -> adapter.setFilter(TodoListAdapter.FilterType.ALL)
                rbIncomplete.id -> adapter.setFilter(TodoListAdapter.FilterType.INCOMPLETE)
                rbComplete.id -> adapter.setFilter(TodoListAdapter.FilterType.COMPLETE)
            }
            updateEmptyView(tvEmpty)
        }
    }

    private fun updateEmptyView(tvEmpty: TextView) {
        if (adapter.itemCount == 0) {
            tvEmpty.visibility = View.VISIBLE
        } else {
            tvEmpty.visibility = View.GONE
        }
    }
}
