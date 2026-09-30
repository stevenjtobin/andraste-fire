package com.andraste.tablet.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.andraste.tablet.tools.ToolCategory
import com.andraste.tablet.tools.ToolRegistry
import com.andraste.tablet.ui.components.CategoryCard
import com.andraste.tablet.ui.components.IceniHeader
import com.andraste.tablet.ui.theme.IceniDeepGreen
import com.andraste.tablet.ui.theme.IceniTextMuted
import com.andraste.tablet.ui.theme.IceniTypography

@Composable
fun HomeScreen(onCategorySelected: (ToolCategory) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IceniDeepGreen)
    ) {
        IceniHeader(
            title = "Andraste",
            subtitle = "Iceni Cyberdeck — Fire Tablet Edition"
        )

        Text(
            text = "${ToolRegistry.all.size} tools  ·  ${ToolCategory.entries.size} categories",
            style = IceniTypography.bodySmall,
            color = IceniTextMuted,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(ToolCategory.entries) { cat ->
                val count = ToolRegistry.byCategory(cat).size
                CategoryCard(category = cat, count = count) {
                    onCategorySelected(cat)
                }
            }
        }
    }
}
