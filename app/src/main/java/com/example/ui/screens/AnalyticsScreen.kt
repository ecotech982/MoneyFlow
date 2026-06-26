package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Transaction
import com.example.viewmodel.FinanceViewModel
import java.util.Calendar

@Composable
fun AnalyticsScreen(financeViewModel: FinanceViewModel) {
    val transactions by financeViewModel.transactions.collectAsState()

    // Calculate analytics metrics
    val (expenseMap, incomeTotal, expenseTotal, maxExpenseCategory) = remember(transactions) {
        val expenseGroup = mutableMapOf<String, Double>()
        var incomeSum = 0.0
        var expenseSum = 0.0

        transactions.forEach { t ->
            if (t.type == "INCOME") {
                incomeSum += t.amount
            } else {
                expenseSum += t.amount
                expenseGroup[t.category] = (expenseGroup[t.category] ?: 0.0) + t.amount
            }
        }

        val topCategory = expenseGroup.maxByOrNull { it.value }?.key ?: "Belum ada"
        Quadruple(expenseGroup, incomeSum, expenseSum, topCategory)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .padding(bottom = 100.dp)
    ) {
        Text(
            text = "Analitik Keuangan",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 1. Budget Comparison (Income vs Expense Bars)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Dana Masuk vs Dana Keluar",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                IncomeExpenseBarChart(income = incomeTotal, expense = expenseTotal)
            }
        }

        // 2. Expense Category Breakdown (Canvas Pie Chart)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Persentase Pengeluaran",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (expenseMap.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada data pengeluaran untuk dianalisis.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .testTag("pie_chart_canvas")
                        ) {
                            CategoryPieChart(expenseMap = expenseMap, totalExpense = expenseTotal)
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            expenseMap.keys.take(5).forEach { category ->
                                val amount = expenseMap[category] ?: 0.0
                                val percent = if (expenseTotal > 0) (amount / expenseTotal * 100).toInt() else 0
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(getCategoryColor(category))
                                    )
                                    Text(
                                        text = "$category ($percent%)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Smart AI Insight Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Rekomendasi Pintar ✨",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    val insightText = if (expenseTotal == 0.0) {
                        "Catatan kamu masih bersih bulan ini! Mulailah mencatat pengeluaran untuk mendapatkan analisis keuangan yang mendalam."
                    } else if (expenseTotal > incomeTotal && incomeTotal > 0) {
                        "Peringatan: Pengeluaranmu lebih tinggi dibanding pemasukan! Kurangi pengeluaran hiburan atau kategori non-prioritas segera."
                    } else {
                        "Pengeluaran terbesar bulan ini didominasi oleh kategori **$maxExpenseCategory**. Cobalah menyisihkan dana minimal 15% dari total saldo bulananmu ke tabungan di awal bulan."
                    }
                    
                    Text(
                        text = insightText.replace("**", ""),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun IncomeExpenseBarChart(income: Double, expense: Double) {
    val total = (income + expense).takeIf { it > 0 } ?: 1.0
    val incomePercent = (income / total).toFloat()
    val expensePercent = (expense / total).toFloat()

    val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Income Bar Row
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Pemasukan", fontSize = 12.sp, color = labelColor)
                Text(
                    text = String.format("%.0f%%", incomePercent * 100),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { incomePercent },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape),
                color = Color(0xFF10B981),
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
            )
        }

        // Expense Bar Row
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Pengeluaran", fontSize = 12.sp, color = labelColor)
                Text(
                    text = String.format("%.0f%%", expensePercent * 100),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEF4444)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { expensePercent },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape),
                color = Color(0xFFEF4444),
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
            )
        }
    }
}

@Composable
fun CategoryPieChart(expenseMap: Map<String, Double>, totalExpense: Double) {
    val categories = expenseMap.keys.toList()
    val total = totalExpense.takeIf { it > 0 } ?: 1.0

    Canvas(modifier = Modifier.fillMaxSize()) {
        var startAngle = -90f
        
        categories.forEach { cat ->
            val value = expenseMap[cat] ?: 0.0
            val sweepAngle = ((value / total) * 360f).toFloat()
            val color = getCategoryColor(cat)

            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                size = Size(size.width, size.height)
            )
            startAngle += sweepAngle
        }
    }
}

fun getCategoryColor(category: String): Color {
    return when (category) {
        "Makanan" -> Color(0xFFFF7043)       // Coral Orange
        "Transportasi" -> Color(0xFF26A69A)  // Teal
        "Hiburan" -> Color(0xFFAB47BC)       // Purple
        "E-Wallet" -> Color(0xFF29B6F6)      // Light Blue
        "Gaji" -> Color(0xFF66BB6A)          // Green
        "Minuman" -> Color(0xFFFFCA28)       // Amber Yellow
        "Jajan" -> Color(0xFFEC407A)         // Pink / Rose
        else -> Color(0xFF90A4AE)            // Grey
    }
}

// Custom data container for 4 items
data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
