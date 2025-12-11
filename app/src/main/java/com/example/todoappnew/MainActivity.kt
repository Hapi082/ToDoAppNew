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
import androidx.recyclerview.widget.ItemTouchHelper
import android.widget.EditText
import android.widget.Toast




class MainActivity : AppCompatActivity() {

    private lateinit var adapter: TodoListAdapter
    private lateinit var tvEmpty: TextView   // ★ 追加

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView = findViewById<RecyclerView>(R.id.rvTodoList)
        val etInput = findViewById<EditText>(R.id.etInput)
        val btnAdd = findViewById<Button>(R.id.btnAdd)
        val btnDelete = findViewById<Button>(R.id.btnDelete)
        tvEmpty = findViewById(R.id.tvEmpty)

        val rgFilter = findViewById<RadioGroup>(R.id.rgFilter)
        val rbAll = findViewById<RadioButton>(R.id.rbAll)
        val rbIncomplete = findViewById<RadioButton>(R.id.rbIncomplete)
        val rbComplete = findViewById<RadioButton>(R.id.rbComplete)

        adapter = TodoListAdapter(onItemsChanged = {
            // データが変わるたびに保存
            saveTodos(adapter.getAllItems())
        })

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // =====================
// Step 12: スワイプ機能
// =====================
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(
            0,  // dragDirs（今回はドラッグ不要なので0）
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT  // swipeDirs
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false  // ドラッグ移動は非対応
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val item = adapter.getItemAt(position)

                if (item == null) {
                    // 行がなくなった場合など → 元に戻す
                    adapter.notifyItemChanged(position)
                    return
                }

                when (direction) {
                    ItemTouchHelper.LEFT -> {
                        // Step 13 で実装する削除ダイアログ呼び出し
                        confirmDeleteSingle(item, position)
                    }
                    ItemTouchHelper.RIGHT -> {
                        // Step 13 で実装する編集ダイアログ呼び出し
                        showEditDialog(item, position)
                    }
                }
            }
        }

// RecyclerView にアタッチ
        ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView)


        // 保存済みのタスクを読み込む
        val loaded = loadTodos()
        if (loaded.isNotEmpty()) {
            loaded.forEach { adapter.addItem(it) }
        }

        updateEmptyView(tvEmpty)


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

    private fun confirmDeleteSingle(item: TodoItem, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("このタスクを削除しますか？")
            .setMessage(item.title)
            .setPositiveButton("削除") { _, _ ->
                // 単一タスク削除
                adapter.deleteItemById(item.id)
                // 空表示の更新
                updateEmptyView(tvEmpty)
            }
            .setNegativeButton("キャンセル") { _, _ ->
                // スワイプされた行を元に戻す
                adapter.notifyItemChanged(position)
            }
            .setOnCancelListener {
                // 戻るキー等で閉じた場合も元に戻しておく
                adapter.notifyItemChanged(position)
            }
            .show()
    }


    private fun showEditDialog(item: TodoItem, position: Int) {
        val editText = EditText(this).apply {
            setText(item.title)
            setSelection(text.length)
        }

        AlertDialog.Builder(this)
            .setTitle("タスクを編集")
            .setView(editText)
            .setPositiveButton("保存") { dialog, _ ->
                val newTitle = editText.text.toString().trim()

                if (newTitle.isEmpty()) {
                    Toast.makeText(this, "タスクを入力してください", Toast.LENGTH_SHORT).show()
                    // スワイプされた行を元に戻す
                    adapter.notifyItemChanged(position)
                    dialog.dismiss()
                    return@setPositiveButton
                }

                // タイトル更新
                adapter.updateTitle(item.id, newTitle)
                // 件数は変わらないので empty 表示は基本そのままだが、念のため更新
                updateEmptyView(tvEmpty)
            }
            .setNegativeButton("キャンセル") { _, _ ->
                // 編集取り消し → 行を元に戻す
                adapter.notifyItemChanged(position)
            }
            .setOnCancelListener {
                // バックキー等で閉じた場合も元に戻す
                adapter.notifyItemChanged(position)
            }
            .show()
    }



    private val prefs by lazy {
        getSharedPreferences("todo_prefs", MODE_PRIVATE)
    }
    private val KEY_TODOS = "todos_json"

    /** TodoリストをJSONで保存 */
    private fun saveTodos(items: List<TodoItem>) {
        val jsonArray = org.json.JSONArray()
        for (item in items) {
            val obj = org.json.JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("isDone", item.isDone)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_TODOS, jsonArray.toString()).apply()
    }

    /** 保存済みTodoリストを読み込み */
    private fun loadTodos(): List<TodoItem> {
        val json = prefs.getString(KEY_TODOS, null) ?: return emptyList()
        val result = mutableListOf<TodoItem>()
        try {
            val jsonArray = org.json.JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val item = TodoItem(
                    id = obj.getLong("id"),
                    title = obj.getString("title"),
                    isDone = obj.getBoolean("isDone")
                )
                result.add(item)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }



    private fun updateEmptyView(tvEmpty: TextView) {
        if (adapter.itemCount == 0) {
            tvEmpty.visibility = View.VISIBLE
        } else {
            tvEmpty.visibility = View.GONE
        }
    }
}
