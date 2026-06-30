package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ui.theme.SoftRedDanger
import com.example.utils.FormatUtils
import com.example.viewmodel.FinanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    navController: NavController,
    financeViewModel: FinanceViewModel
) {
    val transactions by financeViewModel.transactions.collectAsState()
    val myDebt by financeViewModel.myDebt.collectAsState()
    val receivables by financeViewModel.receivables.collectAsState()

    val initialCash by financeViewModel.initialCash.collectAsState()
    val initialEWallet by financeViewModel.initialEWallet.collectAsState()
    val initialBca by financeViewModel.initialBca.collectAsState()
    val initialBri by financeViewModel.initialBri.collectAsState()
    val initialDanamon by financeViewModel.initialDanamon.collectAsState()
    val initialOther by financeViewModel.initialOther.collectAsState()

    // Dynamically calculate balances
    val walletBalances = remember(transactions, initialCash, initialEWallet, initialBca, initialBri, initialDanamon, initialOther) {
        var cash = initialCash
        var ewallet = initialEWallet
        var bca = initialBca
        var bri = initialBri
        var danamon = initialDanamon
        var other = initialOther

        transactions.forEach { t ->
            val amt = t.amount
            if (t.walletAccount != "Piutang") {
                if (t.type == "INCOME") {
                    // Ignore Piutang because it is unpaid
                    if (t.category != "Piutang") {
                        when (t.walletAccount) {
                            "Tunai" -> cash += amt
                            "E-Wallet" -> ewallet += amt
                            "BCA", "Rekening 1" -> bca += amt
                            "BRI", "Rekening 2" -> bri += amt
                            "Danamon", "Rekening 3" -> danamon += amt
                            else -> other += amt
                        }
                    }
                } else {
                    when (t.walletAccount) {
                        "Tunai" -> cash -= amt
                        "E-Wallet" -> ewallet -= amt
                        "BCA", "Rekening 1" -> bca -= amt
                        "BRI", "Rekening 2" -> bri -= amt
                        "Danamon", "Rekening 3" -> danamon -= amt
                        else -> other -= amt
                    }
                }
            }
        }
        mapOf(
            "Tunai" to cash,
            "E-Wallet" to ewallet,
            "Rekening 1" to bca,
            "Rekening 2" to bri,
            "Rekening 3" to danamon,
            "Lainnya" to other
        )
    }

    val totalHutang = remember(transactions) {
        transactions.filter { 
            it.type == "EXPENSE" && (it.category == "Cicilan/Hutang" || it.category == "Cicilan/Hutang(Produktif)" || it.category == "Cicilan/Hutang(Konsumtif)") 
        }.sumOf { it.amount }
    }
    val totalPiutang = remember(transactions) {
        transactions.filter { (it.type == "INCOME" && it.category == "Piutang") || it.walletAccount == "Piutang" }.sumOf { it.amount }
    }

    val totalCashBalance = walletBalances.values.sum()
    val totalPortfolio = totalCashBalance + totalPiutang - totalHutang

    var showEditPortfolioDialog by remember { mutableStateOf(false) }
    var editCash by remember { mutableStateOf("") }
    var editEWallet by remember { mutableStateOf("") }
    var editBca by remember { mutableStateOf("") }
    var editBri by remember { mutableStateOf("") }
    var editDanamon by remember { mutableStateOf("") }
    var editOther by remember { mutableStateOf("") }
    var editDebt by remember { mutableStateOf("") }
    var editReceivables by remember { mutableStateOf("") }

    if (showEditPortfolioDialog) {
        AlertDialog(
            onDismissRequest = { showEditPortfolioDialog = false },
            title = { Text("Atur Saldo & Portofolio", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Saldo Awal Akun:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    OutlinedTextField(value = editCash, onValueChange = { editCash = it }, label = { Text("Tunai (Rp)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(10.dp))
                    OutlinedTextField(value = editEWallet, onValueChange = { editEWallet = it }, label = { Text("E-Wallet (Rp)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(10.dp))
                    OutlinedTextField(value = editBca, onValueChange = { editBca = it }, label = { Text("Rekening 1 (Rp)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(10.dp))
                    OutlinedTextField(value = editBri, onValueChange = { editBri = it }, label = { Text("Rekening 2 (Rp)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(10.dp))
                    OutlinedTextField(value = editDanamon, onValueChange = { editDanamon = it }, label = { Text("Rekening 3 (Rp)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(10.dp))
                    OutlinedTextField(value = editOther, onValueChange = { editOther = it }, label = { Text("Bank Lainnya (Rp)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(10.dp))
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("Kewajiban & Investasi:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    OutlinedTextField(value = editDebt, onValueChange = { editDebt = it }, label = { Text("Hutang Saya (Rp)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(10.dp))
                    OutlinedTextField(value = editReceivables, onValueChange = { editReceivables = it }, label = { Text("Piutang Orang (Rp)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(10.dp))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        financeViewModel.updateInitialBalances(
                            cash = editCash.toDoubleOrNull() ?: 0.0,
                            eWallet = editEWallet.toDoubleOrNull() ?: 0.0,
                            bca = editBca.toDoubleOrNull() ?: 0.0,
                            bri = editBri.toDoubleOrNull() ?: 0.0,
                            danamon = editDanamon.toDoubleOrNull() ?: 0.0,
                            other = editOther.toDoubleOrNull() ?: 0.0
                        )
                        financeViewModel.updatePortfolio(
                            debt = editDebt.toDoubleOrNull() ?: 0.0,
                            receivables = editReceivables.toDoubleOrNull() ?: 0.0,
                            gold = 0.0 // Gold removed
                        )
                        showEditPortfolioDialog = false
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditPortfolioDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wallet & Rekening", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = {
                            editCash = initialCash.toInt().toString()
                            editEWallet = initialEWallet.toInt().toString()
                            editBca = initialBca.toInt().toString()
                            editBri = initialBri.toInt().toString()
                            editDanamon = initialDanamon.toInt().toString()
                            editOther = initialOther.toInt().toString()
                            editDebt = myDebt.toInt().toString()
                            editReceivables = receivables.toInt().toString()
                            showEditPortfolioDialog = true
                        }
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Atur Saldo")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Net Balance / Portfolio Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Total Saldo Bersih",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = FormatUtils.formatRupiah(totalPortfolio),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    
                    Spacer(modifier = Modifier.height(18.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Total Kas/Rekening", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                            Text(FormatUtils.formatRupiah(totalCashBalance), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Hutang vs Piutang", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                            val netLending = receivables - myDebt
                            Text(
                                text = (if (netLending >= 0) "+" else "") + FormatUtils.formatRupiah(netLending),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Wallet accounts list
            Text(
                text = "Daftar Akun & Dompet",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            val accounts = listOf(
                Triple("Tunai", walletBalances["Tunai"] ?: 0.0, Icons.Default.Payments),
                Triple("E-Wallet", walletBalances["E-Wallet"] ?: 0.0, Icons.Default.Wallet),
                Triple("Piutang", totalPiutang, Icons.Default.TrendingUp),
                Triple("Rekening 1", walletBalances["Rekening 1"] ?: 0.0, Icons.Default.AccountBalance),
                Triple("Rekening 2", walletBalances["Rekening 2"] ?: 0.0, Icons.Default.AccountBalance),
                Triple("Rekening 3", walletBalances["Rekening 3"] ?: 0.0, Icons.Default.AccountBalance),
                Triple("Lainnya", walletBalances["Lainnya"] ?: 0.0, Icons.Default.AccountBalance)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                accounts.forEach { (name, bal, icon) ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                              ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Column {
                                    Text(
                                        text = name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    val subText = when(name) {
                                        "Rekening 1" -> "Bank Utama (BCA)"
                                        "Rekening 2" -> "Bank Kedua (BRI)"
                                        "Rekening 3" -> "Bank Ketiga (Danamon)"
                                        "Piutang" -> "Piutang Orang"
                                        else -> "Dompet Aktif"
                                    }
                                    Text(
                                        text = subText,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = FormatUtils.formatRupiah(bal),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (bal >= 0) MaterialTheme.colorScheme.onSurface else SoftRedDanger
                            )
                        }
                    }
                }
            }

            // Section for Obligations & Lending (Hutang & Piutang)
            Text(
                text = "Kewajiban & Piutang",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Debt card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AssignmentLate,
                            contentDescription = null,
                            tint = SoftRedDanger,
                            modifier = Modifier.size(24.dp)
                        )
                        Text("Hutang Saya", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = FormatUtils.formatRupiah(totalHutang),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SoftRedDanger
                        )
                    }
                }

                // Receivables card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AssignmentTurnedIn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text("Piutang Orang", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = FormatUtils.formatRupiah(totalPiutang),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
