package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.data.model.Transaction
import com.example.viewmodel.FinanceViewModel
import androidx.compose.ui.graphics.Color
import java.util.Calendar

@Composable
fun HistoryScreen(navController: NavController, financeViewModel: FinanceViewModel) {
    val transactions by financeViewModel.transactions.collectAsState()
    val searchQuery by financeViewModel.searchQuery.collectAsState()
    val selectedFilter by financeViewModel.selectedFilter.collectAsState()

    // Interactive Filter logic
    val filteredTransactions = remember(transactions, searchQuery, selectedFilter) {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        
        // Start date calculations depending on chips
        val filterTimeBoundary: Long = when (selectedFilter) {
            "Harian" -> {
                calendar.apply {
                    timeInMillis = now
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
            "Mingguan" -> {
                calendar.apply {
                    timeInMillis = now
                    add(Calendar.DAY_OF_YEAR, -7)
                }.timeInMillis
            }
            "Bulanan" -> {
                calendar.apply {
                    timeInMillis = now
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis
            }
            else -> 0L // "Semua"
        }

        transactions.filter { item ->
            val matchesFilter = item.date >= filterTimeBoundary
            val matchesSearch = item.note.contains(searchQuery, ignoreCase = true) ||
                    item.category.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesSearch
        }
    }

    var selectedHistoryTab by remember { mutableStateOf(0) } // 0 = Semua, 1 = Pemasukan, 2 = Pengeluaran

    val finalTransactions = remember(filteredTransactions, selectedHistoryTab) {
        when (selectedHistoryTab) {
            1 -> filteredTransactions.filter { it.type == "INCOME" }
            2 -> filteredTransactions.filter { it.type == "EXPENSE" }
            else -> filteredTransactions
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Riwayat Transaksi",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Search Box (Premium Styled)
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { financeViewModel.setSearchQuery(it) },
            placeholder = { Text("Cari catatan atau kategori...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("search_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            singleLine = true
        )

        // Split Tabs Row
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val tabs = listOf("Semua", "Pemasukan", "Pengeluaran")
                tabs.forEachIndexed { index, label ->
                    val isTabSelected = selectedHistoryTab == index
                    val tabBg = if (isTabSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                    val tabTextCol = if (isTabSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    
                    Button(
                        onClick = { selectedHistoryTab = index },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = tabBg,
                            contentColor = tabTextCol
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Scrollable Filter Chips row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val chips = listOf("Semua", "Harian", "Mingguan", "Bulanan")
            chips.forEach { item ->
                val isSelected = selectedFilter == item
                FilterChip(
                    selected = isSelected,
                    onClick = { financeViewModel.setSelectedFilter(item) },
                    label = { 
                        Text(
                            text = item,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        ) 
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // List display
        if (finalTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Tidak ada transaksi ditemukan",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "Ganti kata pencarian atau filter Anda",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(finalTransactions, key = { it.id }) { item ->
                    TransactionRowItem(
                        transaction = item,
                        onClick = {
                            navController.navigate("transaction_detail/${item.id}")
                        }
                    )
                }
            }
        }
    }
}
