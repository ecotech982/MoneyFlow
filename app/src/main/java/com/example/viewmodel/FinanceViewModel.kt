package com.example.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Transaction
import com.example.data.repository.AuthRepository
import com.example.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

class FinanceViewModel(
    private val transactionRepository: TransactionRepository,
    private val authRepository: AuthRepository,
    context: Context
) : ViewModel() {

    private val prefs: SharedPreferences = context.getSharedPreferences("moneyflow_settings", Context.MODE_PRIVATE)

    // Screen and global state
    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("dark_mode", false))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _isAutoSync = MutableStateFlow(prefs.getBoolean("auto_sync", true))
    val isAutoSync: StateFlow<Boolean> = _isAutoSync.asStateFlow()

    private val _isDailyReminder = MutableStateFlow(prefs.getBoolean("daily_reminder", true))
    val isDailyReminder: StateFlow<Boolean> = _isDailyReminder.asStateFlow()

    private val _firebaseSyncStatus = MutableStateFlow(prefs.getString("fb_sync_status", "Tersinkronisasi (Lokal)") ?: "Tersinkronisasi (Lokal)")
    val firebaseSyncStatus: StateFlow<String> = _firebaseSyncStatus.asStateFlow()

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    // Filter and search on history screen
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow("Semua") // "Harian", "Mingguan", "Bulanan", "Semua"
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    private val _uiMessage = MutableSharedFlow<String>()
    val uiMessage: SharedFlow<String> = _uiMessage.asSharedFlow()

    private var activeUserId: Int = 1

    init {
        // Collect logged in user changes
        viewModelScope.launch {
            authRepository.currentUser.collectLatest { user ->
                val userId = user?.id ?: authRepository.getLoggedUserId()
                activeUserId = if (userId != -1) userId else 1
                loadTransactionsForUser(activeUserId)
            }
        }
    }

    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        prefs.edit().putBoolean("dark_mode", enabled).apply()
    }

    fun setAutoSync(enabled: Boolean) {
        _isAutoSync.value = enabled
        prefs.edit().putBoolean("auto_sync", enabled).apply()
    }

    fun setDailyReminder(enabled: Boolean) {
        _isDailyReminder.value = enabled
        prefs.edit().putBoolean("daily_reminder", enabled).apply()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedFilter(filter: String) {
        _selectedFilter.value = filter
    }

    private fun loadTransactionsForUser(userId: Int) {
        viewModelScope.launch {
            transactionRepository.getAllTransactions(userId).collect { list ->
                _transactions.value = list
            }
        }
    }

    fun addTransaction(amount: Double, type: String, category: String, note: String, date: Long) {
        viewModelScope.launch {
            val user = authRepository.loadSavedUser()
            val userId = user?.id ?: authRepository.getLoggedUserId()
            val finalUserId = if (userId != -1) userId else 1

            val transaction = Transaction(
                amount = amount,
                type = type,
                category = category,
                note = note,
                date = date,
                userId = finalUserId
            )
            transactionRepository.insertTransaction(transaction)
            _uiMessage.emit("Transaksi berhasil disimpan!")
            
            // Trigger Firebase sync simulation if enabled
            if (_isAutoSync.value) {
                simulateFirebaseSync()
            }
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.updateTransaction(transaction)
            _uiMessage.emit("Transaksi berhasil diperbarui!")
            if (_isAutoSync.value) {
                simulateFirebaseSync()
            }
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(transaction)
            _uiMessage.emit("Transaksi berhasil dihapus!")
            if (_isAutoSync.value) {
                simulateFirebaseSync()
            }
        }
    }

    fun simulateFirebaseSync() {
        viewModelScope.launch {
            _firebaseSyncStatus.value = "Menyinkronkan data..."
            kotlinx.coroutines.delay(1200) // Startup loading delay simulation
            val currentTimestamp = Calendar.getInstance().time.toString().substring(0, 19)
            val message = "Cloud synced: $currentTimestamp"
            _firebaseSyncStatus.value = message
            prefs.edit().putString("fb_sync_status", message).apply()
            _uiMessage.emit("Tersinkronisasi dengan Firebase Cloud!")
        }
    }

    fun exportData(format: String): String {
        val header = "ID,Tipe,Nominal,Kategori,Catatan,Tanggal\n"
        val rows = _transactions.value.joinToString("\n") { t ->
            val dateStr = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date(t.date))
            "${t.id},${t.type},${t.amount},${t.category},\"${t.note}\",${dateStr}"
        }
        val csvContent = header + rows
        viewModelScope.launch {
            _uiMessage.emit("Ekspor data $format berhasil dibagikan!")
        }
        return csvContent
    }
}
