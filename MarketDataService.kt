package com.stocktracker.app

import android.app.*
import android.os.Bundle
import android.graphics.Color
import android.widget.*
import java.util.Locale

class MainActivity : Activity() {

    data class Trade(
        val name: String,
        val qty: Int,
        val entry: Double,
        var exit: Double? = null
    )

    data class Investment(
        val name: String,
        val type: String,
        val qty: Double,
        val avg: Double
    )

    private val trades = mutableListOf<Trade>()
    private val investments = mutableListOf<Investment>()

    private lateinit var content: LinearLayout
    private val marketDataService = MarketDataService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        home()
    }

    private fun screen(title: String) {
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(24, 50, 24, 12)

        val head = TextView(this)
        head.text = title
        head.textSize = 27f
        head.setTextColor(Color.rgb(15, 65, 120))
        root.addView(head)

        content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL

        val scroll = ScrollView(this)
        scroll.addView(content)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        val nav = LinearLayout(this)

        listOf(
            "Home",
            "Alerts",
            "Trades",
            "Portfolio",
            "Tips"
        ).forEach { x ->

            val b = Button(this)
            b.text = x
            b.textSize = 11f

            b.setOnClickListener {
                when (x) {
                    "Home" -> home()
                    "Alerts" -> alerts()
                    "Trades" -> trades()
                    "Portfolio" -> portfolio()
                    "Tips" -> tips()
                }
            }

            nav.addView(
                b,
                LinearLayout.LayoutParams(0, -2, 1f)
            )
        }

        root.addView(nav)

        setContentView(root)
    }

    private fun card(s: String) {
        val t = TextView(this)
        t.text = s
        t.textSize = 17f
        t.setPadding(18, 20, 18, 20)
        content.addView(t)
    }

    private fun btn(s: String, f: () -> Unit) {
        val b = Button(this)
        b.text = s
        b.setOnClickListener { f() }
        content.addView(b)
    }

    private fun home() {
        screen("STOCK TRACKER")

        card(
            "📊 MARKET\n\n" +
            "Nifty 200 • Gold • Silver\n\n" +
            "Market data connection: READY"
        )

        card(
            "💼 PORTFOLIO\n\n" +
            "Add investments to start tracking."
        )

        card(
            "🔔 SWING ALERTS\n\n" +
            "Market scanner is being connected."
        )

        card(
            "💡 TIPS\n\n" +
            "Portfolio analysis will activate after you add holdings."
        )
    }

    private fun alerts() {
        screen("🔔 SWING ALERTS")

        card(
            "MARKET DATA TEST\n\n" +
            "Testing BEL market price..."
        )

        btn("🔄 CHECK BEL PRICE") {
            checkBelPrice()
        }
    }

    private fun checkBelPrice() {

        Toast.makeText(
            this,
            "Fetching BEL market data...",
            Toast.LENGTH_SHORT
        ).show()

        marketDataService.getQuote(
            "BEL",

            onSuccess = { quote ->

                runOnUiThread {

                    content.removeAllViews()

                    card("🔔 SWING ALERTS")

                    card(
                        "BEL — Bharat Electronics\n\n" +
                        "Price: ₹${fmt(quote.price)}\n" +
                        "Change: ${fmt(quote.changePercent)}%\n\n" +
                        "Data received successfully ✅"
                    )

                    card(
                        "Scanner Status\n\n" +
                        "Market connection: WORKING\n" +
                        "Stock test: BEL\n" +
                        "Next: Technical analysis"
                    )

                    btn("🔄 REFRESH BEL") {
                        checkBelPrice()
                    }
                }
            },

            onError = { error ->

                runOnUiThread {

                    content.removeAllViews()

                    card("🔔 SWING ALERTS")

                    card(
                        "Unable to fetch BEL data ❌\n\n" +
                        "Reason:\n$error"
                    )

                    btn("🔄 TRY AGAIN") {
                        checkBelPrice()
                    }
                }
            }
        )
    }

    private fun trades() {
        screen("💼 MY TRADES")

        btn("＋ ADD TRADE") {
            addTrade()
        }

        if (trades.isEmpty()) {
            card("No trades yet.")
        }

        trades.forEachIndexed { i, t ->

            val price = t.exit ?: t.entry
            val pnl = (price - t.entry) * t.qty

            card(
                "${t.name}\n\n" +
                "Qty: ${t.qty}\n" +
                "Entry: ₹${fmt(t.entry)}\n" +
                "P&L: ₹${fmt(pnl)}"
            )

            if (t.exit == null) {
                btn("Close ${t.name}") {
                    closeTrade(i)
                }
            }
        }
    }

    private fun addTrade() {

        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL

        val n = EditText(this)
        n.hint = "Stock name"

        val q = EditText(this)
        q.hint = "Quantity"
        q.inputType = 2

        val e = EditText(this)
        e.hint = "Entry price"
        e.inputType = 8194

        box.addView(n)
        box.addView(q)
        box.addView(e)

        AlertDialog.Builder(this)
            .setTitle("Add Trade")
            .setView(box)
            .setPositiveButton("Save") { _, _ ->

                val qty =
                    q.text.toString().toIntOrNull() ?: 0

                val entry =
                    e.text.toString().toDoubleOrNull() ?: 0.0

                if (
                    n.text.isNotBlank() &&
                    qty > 0 &&
                    entry > 0
                ) {

                    trades.add(
                        Trade(
                            n.text.toString()
                                .uppercase(Locale.US),
                            qty,
                            entry
                        )
                    )

                    trades()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun closeTrade(i: Int) {

        val e = EditText(this)
        e.hint = "Closing price"
        e.inputType = 8194

        AlertDialog.Builder(this)
            .setTitle("Close ${trades[i].name}")
            .setView(e)
            .setPositiveButton("Close") { _, _ ->

                e.text.toString()
                    .toDoubleOrNull()
                    ?.let {
                        trades[i].exit = it
                        trades()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun portfolio() {

        screen("📊 PORTFOLIO")

        btn("＋ ADD INVESTMENT") {
            addInvestment()
        }

        if (investments.isEmpty()) {

            card(
                "YOUR PORTFOLIO IS EMPTY\n\n" +
                "Indian Stocks • Mutual Funds • Gold • Silver • US Investments"
            )
        }

        investments.forEach {

            card(
                "${it.name}\n" +
                "${it.type}\n\n" +
                "Qty: ${fmt(it.qty)} • " +
                "Avg: ₹${fmt(it.avg)}"
            )
        }
    }

    private fun addInvestment() {

        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL

        val n = EditText(this)
        n.hint = "Investment name"

        val t = EditText(this)
        t.hint = "Type (Stock / MF / Gold / Silver / US)"

        val q = EditText(this)
        q.hint = "Quantity"
        q.inputType = 8194

        val a = EditText(this)
        a.hint = "Average price"
        a.inputType = 8194

        box.addView(n)
        box.addView(t)
        box.addView(q)
        box.addView(a)

        AlertDialog.Builder(this)
            .setTitle("Add Investment")
            .setView(box)
            .setPositiveButton("Save") { _, _ ->

                val qty =
                    q.text.toString().toDoubleOrNull() ?: 0.0

                val avg =
                    a.text.toString().toDoubleOrNull() ?: 0.0

                if (
                    n.text.isNotBlank() &&
                    qty > 0 &&
                    avg > 0
                ) {

                    investments.add(
                        Investment(
                            n.text.toString(),
                            t.text.toString(),
                            qty,
                            avg
                        )
                    )

                    portfolio()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun tips() {

        screen("💡 TIPS")

        if (investments.isEmpty()) {

            card("Add your portfolio first.")

        } else {

            card(
                "Portfolio review foundation\n\n" +
                "Future analysis:\n" +
                "HOLD • REVIEW • POTENTIAL SWITCH\n\n" +
                "Factors:\n" +
                "Business quality\n" +
                "Growth\n" +
                "Debt\n" +
                "Valuation\n" +
                "Sector outlook\n" +
                "Relative strength"
            )
        }
    }

    private fun fmt(x: Double): String =
        String.format(Locale.US, "%.2f", x)
}
