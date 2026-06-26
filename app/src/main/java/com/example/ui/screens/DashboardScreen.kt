package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.data.model.Transaction
import com.example.ui.theme.SoftGreenSuccess
import com.example.ui.theme.SoftRedDanger
import com.example.utils.FormatUtils
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.FinanceViewModel
import androidx.compose.ui.res.painterResource
import com.example.R
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    navController: NavController,
    financeViewModel: FinanceViewModel,
    authViewModel: AuthViewModel
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val transactions by financeViewModel.transactions.collectAsState()

    // Calculating Stats based on real-time Room values including 50/30/20 actual spend categories
    val (totalBalance, totalIncome, totalExpense, needsActual, wantsActual) = remember(transactions) {
        var balance = 0.0
        var income = 0.0
        var expense = 0.0
        var needs = 0.0
        var wants = 0.0
        
        // Find current month start timestamp
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val monthStart = cal.timeInMillis

        transactions.forEach { trans ->
            if (trans.type == "INCOME") {
                balance += trans.amount
                if (trans.date >= monthStart) {
                    income += trans.amount
                }
            } else {
                balance -= trans.amount
                if (trans.date >= monthStart) {
                    expense += trans.amount
                    // Map categories: Needs -> Makanan, Transportasi, E-Wallet
                    if (trans.category in listOf("Makanan", "Transportasi", "E-Wallet")) {
                        needs += trans.amount
                    } else {
                        // Wants -> Hiburan, Minuman, Lainnya
                        wants += trans.amount
                    }
                }
            }
        }
        listOf(balance, income, expense, needs, wants)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
    ) {
        // Welcome Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Halo, 👋",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    Text(
                        text = currentUser?.fullName ?: "Pengguna",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // Total Balance Hero Card (Dynamic Gradient)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .testTag("balance_hero_card"),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        )
                        .padding(24.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Sisa Saldo Anda",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            Text(
                                text = FormatUtils.formatRupiah(totalBalance),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        
                        Icon(
                            painter = painterResource(id = R.drawable.ic_wallet_custom),
                            contentDescription = "Dompet",
                            tint = Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.size(64.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Monthly Income Section
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = "Pemasukan",
                                    tint = SoftGreenSuccess,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Pemasukan Bulan Ini",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = FormatUtils.formatRupiah(totalIncome),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // Monthly Expense Section
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = "Pengeluaran",
                                    tint = SoftRedDanger,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Pengeluaran Bulan Ini",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = FormatUtils.formatRupiah(totalExpense),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // Aturan Pengelolaan Keuangan 50/30/20
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .testTag("card_50_30_20_method"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PieChart,
                                contentDescription = "Metode 50/30/20",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Aturan Keuangan 50/30/20",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Kelola otomatis budget bulanan sehat",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (totalIncome <= 0.0) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Saran",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = "Belum Ada Pemasukan Bulan Ini",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Saat Anda mencatat pemasukan, sistem akan langsung membagi target keuangan Anda menjadi 50% Kebutuhan, 30% Keinginan, dan 20% Tabungan.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = { navController.navigate("add_transaction") },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Catat Pemasukan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        val limitNeeds = totalIncome * 0.50
                        val limitWants = totalIncome * 0.30
                        val limitSavings = totalIncome * 0.20

                        val progressNeeds = if (limitNeeds > 0) (needsActual / limitNeeds).toFloat().coerceIn(0f, 1f) else 0f
                        val progressWants = if (limitWants > 0) (wantsActual / limitWants).toFloat().coerceIn(0f, 1f) else 0f

                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Section 1: Kebutuhan (50%)
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("50", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Kebutuhan (Needs)",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Text(
                                            text = "Makanan, Transport, E-Wallet",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${FormatUtils.formatRupiah(needsActual)} / ${FormatUtils.formatRupiah(limitNeeds)}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (needsActual > limitNeeds) SoftRedDanger else MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Terpakai ${(progressNeeds * 100).toInt()}%",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { progressNeeds },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                    color = if (needsActual > limitNeeds) SoftRedDanger else MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                )
                            }

                            // Section 2: Keinginan (30%)
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFFFB300).copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("30", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD48A00))
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Keinginan (Wants)",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Text(
                                            text = "Hiburan, Jajan, Minuman, Lainnya",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${FormatUtils.formatRupiah(wantsActual)} / ${FormatUtils.formatRupiah(limitWants)}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (wantsActual > limitWants) SoftRedDanger else Color(0xFFD48A00)
                                        )
                                        Text(
                                            text = "Terpakai ${(progressWants * 100).toInt()}%",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { progressWants },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                    color = if (wantsActual > limitWants) SoftRedDanger else Color(0xFFFFB300),
                                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                )
                            }

                            // Section 3: Tabungan (20%)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = SoftGreenSuccess.copy(alpha = 0.08f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(SoftGreenSuccess.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("20", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SoftGreenSuccess)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("Rekomendasi Tabungan & Investasi", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SoftGreenSuccess)
                                            Text("Sisihkan dana darurat Anda secara konsisten", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Text(
                                        text = FormatUtils.formatRupiah(limitSavings),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SoftGreenSuccess
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Weekly Trend Section
        item {
            Text(
                text = "Tren Keuangan Pekan Ini",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    WeeklySummaryChart(transactions = transactions)
                }
            }
        }

        // Recent Transactions Label
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transaksi Terbaru",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Recent Transactions List
        val recentTransactions = transactions.take(5)
        if (recentTransactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada transaksi dimasukkan",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            items(recentTransactions) { trans ->
                TransactionRowItem(
                    transaction = trans,
                    onClick = {
                        navController.navigate("transaction_detail/${trans.id}")
                    }
                )
            }
        }
    }
}

@Composable
fun TransactionRowItem(transaction: Transaction, onClick: () -> Unit) {
    val categoryIcon = getCategoryIcon(transaction.category)
    val colorAccent = if (transaction.type == "INCOME") SoftGreenSuccess else SoftRedDanger
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colorAccent.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon,
                    contentDescription = transaction.category,
                    tint = colorAccent,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.note.ifBlank { transaction.category },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(transaction.date)),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Text(
                text = (if (transaction.type == "INCOME") "+" else "-") + FormatUtils.formatRupiah(transaction.amount),
                color = colorAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}

fun getCategoryIcon(category: String): ImageVector {
    return when (category) {
        "Makanan" -> Icons.Default.Restaurant
        "Transportasi" -> Icons.Default.DirectionsCar
        "Hiburan" -> Icons.Default.VideogameAsset
        "E-Wallet" -> Icons.Default.AccountBalanceWallet
        "Gaji" -> Icons.Default.MonetizationOn
        "Minuman" -> Icons.Default.LocalCafe
        "Jajan" -> Icons.Default.Fastfood
        else -> Icons.Default.Category
    }
}

@Composable
fun WeeklySummaryChart(transactions: List<Transaction>) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    
    // Group transactions of last 7 days and build chart data representation
    val chartData = remember(transactions) {
        val days = Array(7) { "" }
        val values = FloatArray(7) { 0f }
        
        // Let's populate days of week (backwards)
        val sdf = SimpleDateFormat("EEE", Locale("in", "ID"))
        for (i in 6 downTo 0) {
            val dateCal = Calendar.getInstance()
            dateCal.add(Calendar.DAY_OF_YEAR, -i)
            days[6 - i] = sdf.format(dateCal.time)
            
            // Calculate sum for that day of absolute amount increments
            dateCal.set(Calendar.HOUR_OF_DAY, 0)
            dateCal.set(Calendar.MINUTE, 0)
            dateCal.set(Calendar.SECOND, 0)
            dateCal.set(Calendar.MILLISECOND, 0)
            val startOfDay = dateCal.timeInMillis
            
            val endOfDay = startOfDay + 24 * 60 * 60 * 1000
            
            val daySum = transactions.filter {
                it.date in startOfDay until endOfDay
            }.sumOf { if (it.type == "EXPENSE") it.amount else 0.0 }
            
            values[6 - i] = daySum.toFloat()
        }
        Pair(days, values)
    }

    val daysLabel = chartData.first
    val barValues = chartData.second
    val maxExpense = barValues.maxOrNull()?.takeIf { it > 0 } ?: 100000f

    Column {
        Text(
            text = "Total Pengeluaran Harian (Seminggu)",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = labelColor
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            val totalBars = 7
            val spaceBetween = size.width / totalBars
            val barWidth = 32.dp.toPx()
            val canvasHeight = size.height - 20.dp.toPx()

            for (i in 0 until totalBars) {
                val valueRatio = barValues[i] / maxExpense
                val barHeight = canvasHeight * valueRatio
                val xOffset = i * spaceBetween + (spaceBetween - barWidth) / 2
                val yOffset = canvasHeight - barHeight

                // Draw background bar
                drawRoundRect(
                    color = primaryColor.copy(alpha = 0.05f),
                    topLeft = Offset(xOffset, 0f),
                    size = Size(barWidth, canvasHeight),
                    cornerRadius = CornerRadius(6.dp.toPx())
                )

                if (barHeight > 0) {
                    // Draw visual bar filled
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(primaryColor, secondaryColor)
                        ),
                        topLeft = Offset(xOffset, yOffset),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(6.dp.toPx())
                    )
                }

                // Draw days of week text labels
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = labelColor.toArgb()
                        textSize = 10.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }
                    drawText(
                        daysLabel[i],
                        xOffset + barWidth / 2,
                        size.height,
                        paint
                    )
                }
            }
        }
    }
}
