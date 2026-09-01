package com.stocktracker.app

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.Executors

data class StockQuote(
    val symbol: String,
    val price: Double,
    val changePercent: Double,
    val timestamp: Long
)

class MarketDataService {

    private val client = OkHttpClient()
    private val executor = Executors.newSingleThreadExecutor()

    fun getBelQuote(
        onSuccess: (StockQuote) -> Unit,
        onError: (String) -> Unit
    ) {
        executor.execute {
            try {
                val url =
                    "https://query1.finance.yahoo.com/v8/finance/chart/BEL.NS?range=1d&interval=1m"

                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0")
                    .build()

                client.newCall(request).execute().use { response ->

                    if (!response.isSuccessful) {
                        onError("Network error: ${response.code}")
                        return@use
                    }

                    val body = response.body?.string()
                        ?: run {
                            onError("Empty response")
                            return@use
                        }

                    val root = JSONObject(body)
                    val result =
                        root.getJSONObject("chart")
                            .getJSONArray("result")
                            .getJSONObject(0)

                    val meta = result.getJSONObject("meta")

                    val price = meta.optDouble(
                        "regularMarketPrice",
                        0.0
                    )

                    val previousClose = meta.optDouble(
                        "previousClose",
                        price
                    )

                    val changePercent =
                        if (previousClose != 0.0)
                            ((price - previousClose) / previousClose) * 100
                        else 0.0

                    val timestamp =
                        System.currentTimeMillis()

                    onSuccess(
                        StockQuote(
                            symbol = "BEL",
                            price = price,
                            changePercent = changePercent,
                            timestamp = timestamp
                        )
                    )
                }

            } catch (e: Exception) {
                onError(
                    e.message ?: "Unknown error"
                )
            }
        }
    }
}
