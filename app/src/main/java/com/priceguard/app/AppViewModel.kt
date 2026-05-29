package com.priceguard.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Currency
import java.util.Locale

sealed interface ScanUiState {
    object Idle : ScanUiState
    object Loading : ScanUiState
    data class Success(val item: HistoryItem) : ScanUiState
    data class Error(val message: String) : ScanUiState
}

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = HistoryRepository(database.historyDao())

    val historyItems: StateFlow<List<HistoryItem>> = repository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    var uiState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
        private set

    var isScanning = MutableStateFlow(false)
        private set

    private val api = OpenFoodFactsApi.create()

    fun startScanning() {
        isScanning.value = true
    }

    fun stopScanning() {
        isScanning.value = false
    }

    fun setUiState(state: ScanUiState) {
        uiState.value = state
    }

    fun getCountryCode(): String {
        return (Locale.getDefault().country ?: "").uppercase()
    }

    fun getCurrencyCode(): String {
        return try {
            Currency.getInstance(Locale.getDefault()).currencyCode ?: "USD"
        } catch (e: Exception) {
            "USD"
        }
    }

    fun calculatePrice(barcode: String): String {
        if (barcode.isEmpty()) return "$2.99"
        var sum = 0
        for (i in barcode.indices) {
            sum += barcode[i].code * (i + 1)
        }
        val baseValue = ((sum % 12) + 1.25) + ((sum % 99) / 100.0)
        return formatPrice(baseValue)
    }

    fun formatPrice(baseUSD: Double): String {
        val currency = getCurrencyCode()
        val country = getCountryCode()

        return when {
            currency == "RSD" || country == "RS" -> {
                val rsdPrice = Math.round(baseUSD * 110)
                "$rsdPrice din"
            }
            currency == "EUR" || listOf("DE", "FR", "IT", "ES", "NL", "GR", "PT", "HR", "SI").contains(country) -> {
                val eurPrice = String.format(Locale.US, "%.2f", baseUSD * 0.92)
                "€$eurPrice"
            }
            currency == "GBP" || country == "GB" -> {
                val gbpPrice = String.format(Locale.US, "%.2f", baseUSD * 0.78)
                "£$gbpPrice"
            }
            currency == "JPY" || country == "JP" -> {
                val jpyPrice = Math.round(baseUSD * 155)
                "¥$jpyPrice"
            }
            currency == "CAD" || country == "CA" -> {
                val cadPrice = String.format(Locale.US, "%.2f", baseUSD * 1.36)
                "C$$cadPrice"
            }
            currency == "AUD" || country == "AU" -> {
                val audPrice = String.format(Locale.US, "%.2f", baseUSD * 1.51)
                "A$$audPrice"
            }
            else -> {
                val usdPrice = String.format(Locale.US, "%.2f", baseUSD)
                "$$usdPrice"
            }
        }
    }

    fun getSupermarkets(): List<Pair<String, Double>> {
        val country = getCountryCode()
        return when {
            country == "RS" || Locale.getDefault().language == "sr" -> listOf(
                "Lidl Srbija" to 0.90,
                "Univerexport" to 0.94,
                "Maxi Supermarketi" to 1.02,
                "Idea / Mercator" to 1.06
            )
            country == "GB" -> listOf(
                "Aldi UK" to 0.88,
                "Tesco" to 0.96,
                "Sainsbury's" to 1.04,
                "Marks & Spencer" to 1.20
            )
            listOf("DE", "FR", "ES", "IT", "HR", "SI").contains(country) -> listOf(
                "Lidl Market" to 0.87,
                "Aldi Süd/Nord" to 0.89,
                "Carrefour / Mercadona" to 1.01,
                "Edeka / Rewe Express" to 1.15
            )
            country == "US" || country == "CA" -> listOf(
                "Aldi US" to 0.86,
                "Walmart Grocery" to 0.95,
                "Target Fresh" to 1.03,
                "Whole Foods Market" to 1.24
            )
            else -> listOf(
                "Local Discounter" to 0.88,
                "Global E-Com Market" to 0.98,
                "Metro / Costco" to 1.02,
                "Convenience Store" to 1.22
            )
        }
    }

    fun getMultiplierForBarcode(barcode: String): Double {
        if (barcode.isEmpty()) return 1.0
        val lastCharVal = barcode.lastOrNull()?.code ?: 0
        return 0.85 + (lastCharVal % 4) * 0.12
    }

    fun fetchProduct(barcode: String, saveToHistory: Boolean = true) {
        viewModelScope.launch {
            uiState.value = ScanUiState.Loading
            try {
                val response = withContext(Dispatchers.IO) {
                    api.getProduct(barcode)
                }
                if (response.isSuccessful && response.body()?.status == 1) {
                    val p = response.body()?.product
                    if (p != null) {
                        val brand = p.brands ?: "General Brands"
                        val name = p.productName ?: "Unnamed Product"
                        val price = calculatePrice(barcode)
                        val score = p.nutriscoreGrade ?: "?"
                        val categories = p.categories ?: "Grocery, Miscellaneous"
                        val imageUrl = p.imageUrl ?: ""
                        val nova = p.novaGroup ?: 0
                        val additives = p.additivesCount ?: 0

                        val item = HistoryItem(
                            barcode = barcode,
                            name = name,
                            brand = brand,
                            imageUrl = imageUrl,
                            price = price,
                            nutriscore = score,
                            categories = categories,
                            novaGroup = nova,
                            additivesCount = additives,
                            countryCode = getCountryCode()
                        )

                        if (saveToHistory) {
                            withContext(Dispatchers.IO) {
                                repository.insert(item)
                            }
                        }
                        uiState.value = ScanUiState.Success(item)
                    } else {
                        uiState.value = ScanUiState.Error(Localization.get("not_found"))
                    }
                } else {
                    uiState.value = ScanUiState.Error(Localization.get("not_found"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                uiState.value = ScanUiState.Error(e.localizedMessage ?: "Network error occurred")
            }
        }
    }

    fun deleteItem(id: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.delete(id)
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.clearAll()
            }
        }
    }
}
