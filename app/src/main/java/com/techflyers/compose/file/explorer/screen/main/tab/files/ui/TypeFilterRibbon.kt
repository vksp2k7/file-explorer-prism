package com.techflyers.compose.file.explorer.screen.main.tab.files.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.techflyers.compose.file.explorer.screen.main.tab.files.FilesTab
import com.techflyers.compose.file.explorer.screen.main.tab.files.holder.VirtualFileHolder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypeFilterRibbon(tab: FilesTab) {
    if (tab.activeFolder is VirtualFileHolder) return
    if (tab.isLoading && tab.activeFolderContent.isEmpty()) return

    val options = listOf(
        null to "All",
        "image" to "Images",
        "video" to "Videos",
        "audio" to "Audio",
        "document" to "Docs",
        "archive" to "Archives"
    )

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(options, key = { it.first ?: "all" }) { (key, label) ->
            val selected = tab.typeFilter == key
            FilterChip(
                selected = selected,
                onClick = {
                    tab.typeFilter = key
                    tab.updateContentSnapshot()
                },
                label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                leadingIcon = if (selected) {
                    { Icon(Icons.Rounded.Done, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}
