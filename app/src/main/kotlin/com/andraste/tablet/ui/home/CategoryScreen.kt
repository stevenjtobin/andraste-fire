package com.andraste.tablet.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.andraste.tablet.tools.ToolCategory
import com.andraste.tablet.tools.ToolDef
import com.andraste.tablet.tools.ToolRegistry
import com.andraste.tablet.ui.components.IceniHeader
import com.andraste.tablet.ui.components.ToolRow
import com.andraste.tablet.ui.theme.IceniDeepGreen

@Composable
fun CategoryScreen(
    category: ToolCategory,
    onBack: () -> Unit,
    onToolSelected: (ToolDef) -> Unit
) {
    val tools = ToolRegistry.byCategory(category)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IceniDeepGreen)
    ) {
        IceniHeader(
            title = category.displayName,
            subtitle = "${tools.size} tools",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(tools) { tool ->
                ToolRow(tool = tool) { onToolSelected(tool) }
            }
        }
    }
}
