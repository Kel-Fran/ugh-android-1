package com.example.taskmanager

import android.os.Bundle
import android.provider.CalendarContract
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.taskmanager.ui.theme.TaskManagerTheme

enum class Priority(val value: Int) {
    LOW(1),
    MEDIUM(2),
    HIGH(3);

    companion object {
        fun fromValue(value: Int): Priority? {
            return Priority.entries.firstOrNull { it.value == value }
        }
    }
}

enum class FilterType { ALL, COMPLETE, INCOMPLETE }

data class Task(
    val id: Int,
    val name: String,
    val category: String,
    val priority: Priority,
    val done: Boolean,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TaskManagerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TaskManagerScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun TaskManagerScreen(modifier: Modifier = Modifier) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(Priority.LOW) }
    var nameHasError by remember { mutableStateOf(false) }
    var categoryHasError by remember { mutableStateOf(false) }
    var nextId by remember { mutableStateOf(0) }

    val tasks = remember { mutableStateListOf<Task>() }
    var filter by remember { mutableStateOf(FilterType.ALL) }

    val ctx = LocalContext.current

    Column(
        modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Feladatkezelő")
        Text("Szervezd meg a napi teendőidet!")

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("name") },
            singleLine = true
        )
        if (nameHasError) Text("name must not be empty")
        OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            label = { Text("category") },
            singleLine = true
        )
        if (categoryHasError) Text("category must not be empty")
        Slider(
            value = priority.value.toFloat(),
            onValueChange = { priority = Priority.fromValue(it.toInt()) ?: Priority.LOW },
            valueRange = 1f..3f
        )

        TextButton(
            {
                nameHasError = name.isBlank()
                categoryHasError = category.isBlank()

                if (nameHasError || categoryHasError) Toast.makeText(
                    ctx, "check your inputs",
                    Toast.LENGTH_SHORT
                ).show()
                else {
                    tasks.add(Task(nextId++, name, category, priority, false))
                    name = ""
                    category = ""
                    priority = Priority.LOW
                    Toast.makeText(ctx, "added task", Toast.LENGTH_SHORT).show()
                }
            }
        ) { Text("add task") }

        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            for (type in FilterType.entries) {
                RadioButton(type == filter, { filter = type })
                Text(type.toString())
            }
        }

        LazyColumn(
            Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items = tasks.filter {
                when (filter) {
                    FilterType.ALL -> true
                    FilterType.COMPLETE -> it.done
                    FilterType.INCOMPLETE -> !it.done
                }
            }, key = { it.id }) { TaskCard(it, tasks) }
        }
    }
}

@Composable
fun TaskCard(it: Task, tasks: SnapshotStateList<Task>) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(Priority.LOW) }
    var nameHasError by remember { mutableStateOf(false) }
    var categoryHasError by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Column() {
                Text(it.name)
                Text(it.category)
            }

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0, 0, 0)),
                contentAlignment = Alignment.Center
            ) { Text(it.priority.toString()) }
            Column() {
                Text(if (it.done) "done" else "todo")
                TextButton({
                    val idx = tasks.indexOf(it)
                    tasks.removeAt(idx)
                    tasks.add(
                        idx,
                        Task(it.id, it.name, it.category, it.priority, !it.done)
                    )
                }) { Text(if (it.done) "unmark complete" else "mark complete") }
            }
            IconButton({
                showEditDialog = true
                name = it.name
                priority = it.priority
                category = it.category
            }) { Icon(painterResource(R.drawable.ic_edit), "edit") }
            IconButton({ showDeleteDialog = true }) {
                Icon(
                    painterResource(R.drawable.ic_delete),
                    "delete"
                )
            }
        }
    }
    if (showDeleteDialog) AlertDialog(
        onDismissRequest = { showDeleteDialog = false },
        title = { Text("delete task") },
        text = { Text("are you sure you want to delete \"${it.name}\" task") },
        confirmButton = {
            TextButton(onClick = {
                showDeleteDialog = false
                tasks.remove(it)
            }) { Text("delete") }
        },
        dismissButton = {
            TextButton(onClick = {
                showDeleteDialog = false
                name = it.name
                category = it.category
                priority = it.priority
                nameHasError = false
                categoryHasError = false
            }) { Text("cancel") }
        })

    if (showEditDialog) AlertDialog(
        onDismissRequest = {showEditDialog = false},
        title = {Text("edit task")},
        text = {
            Column() {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("name") },
                    singleLine = true
                )
                if (nameHasError) Text("name must not be empty")
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("category") },
                    singleLine = true
                )
                if (categoryHasError) Text("category must not be empty")
                Slider(
                    value = priority.value.toFloat(),
                    onValueChange = { priority = Priority.fromValue(it.toInt()) ?: Priority.LOW },
                    valueRange = 1f..3f
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                nameHasError = name.isBlank()
                categoryHasError = category.isBlank()

                if (!nameHasError && !categoryHasError) {
                    showEditDialog = false
                    val idx = tasks.indexOf(it)
                    tasks.removeAt(idx)
                    tasks.add(idx, Task(it.id, name, category, priority, it.done))
                }
            }) { Text("edit") }
        },
        dismissButton = {
            TextButton(onClick = {
                showEditDialog = false
            }) { Text("cancel") }
        }
    )
}