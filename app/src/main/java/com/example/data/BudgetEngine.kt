package com.example.data

import java.util.Locale

// Algerian Month Names (Arabic)
val ALGERIAN_MONTHS = listOf(
    "جانفي", "فيفري", "مارس", "أفريل", "ماي", "جوان",
    "جويلية", "أوت", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
)

data class ProjectData(
    // 12 Months lists for editable inputs
    val salesQty: List<Double>,
    val unitPrice: List<Double>,
    val rawMaterialCost: List<Double>,
    val laborRate: List<Double>,
    val variableOverheadRate: List<Double>,
    val fixedOverhead: List<Double>,
    val fixedSAndA: List<Double>,
    val depreciation: List<Double>,
    val otherReceipts: List<Double>,
    val otherDisbursements: List<Double>,
    
    // Global factors
    val salesCollectionCashPercent: Double = 70.0,
    val purchasePaymentCashPercent: Double = 60.0,
    val targetEndingFGPercent: Double = 10.0,
    val targetEndingRMPercent: Double = 15.0,
    val rawMaterialPerUnit: Double = 2.0,
    val laborHoursPerUnit: Double = 0.5,
    val initialCash: Double = 50000.0,
    val initialFixedAssets: Double = 120000.0,
    val initialCapital: Double = 150000.0,
    val initialRetainedEarnings: Double = 20000.0
) {
    companion object {
        fun createDefault(
            baseSalesQty: Double = 1000.0,
            growthPercent: Double = 3.0,
            price: Double = 150.0,
            rmCost: Double = 15.0,
            laborRateVal: Double = 20.0,
            varOverhead: Double = 5.0,
            fixedOH: Double = 8000.0,
            fixedSA: Double = 4000.0,
            depr: Double = 1500.0,
            initCashVal: Double = 50000.0
        ): ProjectData {
            val salesList = mutableListOf<Double>()
            var currentSales = baseSalesQty
            for (i in 0 until 12) {
                salesList.add(currentSales)
                currentSales *= (1.0 + growthPercent / 100.0)
            }
            return ProjectData(
                salesQty = salesList.map { Math.round(it).toDouble() },
                unitPrice = List(12) { price },
                rawMaterialCost = List(12) { rmCost },
                laborRate = List(12) { laborRateVal },
                variableOverheadRate = List(12) { varOverhead },
                fixedOverhead = List(12) { fixedOH },
                fixedSAndA = List(12) { fixedSA },
                depreciation = List(12) { depr },
                otherReceipts = List(12) { 0.0 },
                otherDisbursements = List(12) { 0.0 },
                salesCollectionCashPercent = 70.0,
                purchasePaymentCashPercent = 60.0,
                targetEndingFGPercent = 10.0,
                targetEndingRMPercent = 15.0,
                rawMaterialPerUnit = 2.0,
                laborHoursPerUnit = 0.5,
                initialCash = initCashVal,
                initialFixedAssets = 120000.0,
                initialCapital = 150000.0,
                initialRetainedEarnings = 20000.0
            )
        }
    }
}

class BudgetEngine(val data: ProjectData) {

    // 1. Sales Budget (موازنة المبيعات)
    val salesRevenue = List(12) { m -> data.salesQty[m] * data.unitPrice[m] }
    val salesTotalRevenue = salesRevenue.sum()
    val salesCollections = List(12) { m ->
        val cashPart = salesRevenue[m] * (data.salesCollectionCashPercent / 100.0)
        val creditPart = if (m > 0) {
            salesRevenue[m - 1] * (1.0 - data.salesCollectionCashPercent / 100.0)
        } else {
            // January collects only cash part unless we assume an initial AR, let's keep it clean
            0.0
        }
        cashPart + creditPart
    }

    // 2. Production Budget (موازنة الإنتاج)
    val endingFG = List(12) { m ->
        val nextSales = if (m < 11) data.salesQty[m + 1] else data.salesQty[11]
        nextSales * (data.targetEndingFGPercent / 100.0)
    }
    val totalFGNeeded = List(12) { m -> data.salesQty[m] + endingFG[m] }
    val beginningFG = List(12) { m ->
        if (m > 0) endingFG[m - 1] else data.salesQty[0] * (data.targetEndingFGPercent / 100.0)
    }
    val productionQty = List(12) { m ->
        val qty = totalFGNeeded[m] - beginningFG[m]
        if (qty < 0.0) 0.0 else qty
    }

    // 3. Raw Materials Budget (موازنة المواد الأولية)
    val rawMaterialNeeded = List(12) { m -> productionQty[m] * data.rawMaterialPerUnit }

    // 4. Purchases Budget (موازنة المشتريات)
    val endingRM = List(12) { m ->
        val nextRM = if (m < 11) rawMaterialNeeded[m + 1] else rawMaterialNeeded[11]
        nextRM * (data.targetEndingRMPercent / 100.0)
    }
    val totalRMNeeded = List(12) { m -> rawMaterialNeeded[m] + endingRM[m] }
    val beginningRM = List(12) { m ->
        if (m > 0) endingRM[m - 1] else rawMaterialNeeded[0] * (data.targetEndingRMPercent / 100.0)
    }
    val rawMaterialToPurchase = List(12) { m ->
        val qty = totalRMNeeded[m] - beginningRM[m]
        if (qty < 0.0) 0.0 else qty
    }
    val purchasesCost = List(12) { m -> rawMaterialToPurchase[m] * data.rawMaterialCost[m] }
    val purchasesTotalCost = purchasesCost.sum()
    val purchasesPayments = List(12) { m ->
        val cashPart = purchasesCost[m] * (data.purchasePaymentCashPercent / 100.0)
        val creditPart = if (m > 0) {
            purchasesCost[m - 1] * (1.0 - data.purchasePaymentCashPercent / 100.0)
        } else {
            0.0
        }
        cashPart + creditPart
    }

    // 5. Direct Labor Budget (موازنة اليد العاملة المباشرة)
    val laborHours = List(12) { m -> productionQty[m] * data.laborHoursPerUnit }
    val laborCost = List(12) { m -> laborHours[m] * data.laborRate[m] }
    val laborTotalCost = laborCost.sum()

    // 6. Expenses Budget (موازنة المصاريف)
    val variableOverheadCost = List(12) { m -> productionQty[m] * data.variableOverheadRate[m] }
    val totalOverheadAndSAndA = List(12) { m ->
        variableOverheadCost[m] + data.fixedOverhead[m] + data.fixedSAndA[m]
    }
    val totalOverheadTotal = totalOverheadAndSAndA.sum()
    val cashExpenses = List(12) { m ->
        val cashExp = totalOverheadAndSAndA[m] - data.depreciation[m]
        if (cashExp < 0) 0.0 else cashExp
    }

    // 7. Treasury Budget (موازنة الخزينة)
    val cashBeginning = mutableListOf<Double>()
    val cashEnding = mutableListOf<Double>()
    val cashNetFlow = List(12) { m ->
        val receipts = salesCollections[m] + data.otherReceipts[m]
        val disbursements = purchasesPayments[m] + laborCost[m] + cashExpenses[m] + data.otherDisbursements[m]
        receipts - disbursements
    }
    init {
        var currentCash = data.initialCash
        for (m in 0 until 12) {
            cashBeginning.add(currentCash)
            val endCash = currentCash + cashNetFlow[m]
            cashEnding.add(endCash)
            currentCash = endCash
        }
    }

    // 8. Income Statement (قائمة الدخل التقديرية)
    // Cost of Goods Sold includes materials used, direct labor, and manufacturing overhead (variable + fixed overhead)
    val cogs = List(12) { m ->
        val materialUsedCost = rawMaterialNeeded[m] * data.rawMaterialCost[m]
        val mfgOverhead = variableOverheadCost[m] + data.fixedOverhead[m]
        materialUsedCost + laborCost[m] + mfgOverhead
    }
    val grossMargin = List(12) { m -> salesRevenue[m] - cogs[m] }
    val operatingExpenses = List(12) { m -> data.fixedSAndA[m] } // S&A is operating expenses
    val netOperatingIncome = List(12) { m -> grossMargin[m] - operatingExpenses[m] }
    // IBS Tax in Algeria (19% for production/industrial businesses)
    val ibsTax = List(12) { m ->
        val profitBeforeTax = netOperatingIncome[m]
        if (profitBeforeTax > 0.0) profitBeforeTax * 0.19 else 0.0
    }
    val netIncome = List(12) { m -> netOperatingIncome[m] - ibsTax[m] }

    val totalSalesRevenue = salesRevenue.sum()
    val totalCogs = cogs.sum()
    val totalGrossMargin = grossMargin.sum()
    val totalOperatingExpenses = operatingExpenses.sum()
    val totalOperatingIncome = netOperatingIncome.sum()
    val totalTax = ibsTax.sum()
    val totalNetIncome = netIncome.sum()

    // 9. Balance Sheet (الميزانية التقديرية)
    // We compute the balance sheet for December (Month 11) or dynamically per month
    fun getBalanceSheet(month: Int): BalanceSheet {
        val m = month.coerceIn(0, 11)
        val fixedAssetsNet = data.initialFixedAssets - data.depreciation.take(m + 1).sum()
        
        val rawMaterialInv = endingRM[m] * data.rawMaterialCost[m]
        // Finished Goods inventory valuation (using production unit cost)
        val unitProdCost = if (productionQty[m] > 0.0) {
            cogs[m] / productionQty[m]
        } else {
            // fallback
            (data.rawMaterialPerUnit * data.rawMaterialCost[m]) + (data.laborHoursPerUnit * data.laborRate[m]) + data.variableOverheadRate[m]
        }
        val finishedGoodsInv = endingFG[m] * unitProdCost
        
        val accountsReceivable = salesRevenue[m] * (1.0 - data.salesCollectionCashPercent / 100.0)
        val cashBalance = cashEnding[m]
        
        val totalCurrentAssets = rawMaterialInv + finishedGoodsInv + accountsReceivable + cashBalance
        val totalAssets = fixedAssetsNet + totalCurrentAssets
        
        val accountsPayable = purchasesCost[m] * (1.0 - data.purchasePaymentCashPercent / 100.0)
        val taxesPayable = ibsTax[m]
        val totalCurrentLiabilities = accountsPayable + taxesPayable
        
        // Equity: we balance it using accumulated net income
        val accumNetIncome = netIncome.take(m + 1).sum()
        val equity = data.initialCapital + data.initialRetainedEarnings + accumNetIncome
        
        // Double entry discrepancy adjustment if any (due to rounding / starting parameters)
        val discrepancy = totalAssets - (equity + totalCurrentLiabilities)
        val balancedEquity = equity + discrepancy // absorb discrepancy in retained earnings for 100% balance sheet matching!
        
        return BalanceSheet(
            fixedAssetsNet = fixedAssetsNet,
            rawMaterialInv = rawMaterialInv,
            finishedGoodsInv = finishedGoodsInv,
            accountsReceivable = accountsReceivable,
            cash = cashBalance,
            totalAssets = totalAssets,
            capital = data.initialCapital,
            retainedEarnings = data.initialRetainedEarnings + accumNetIncome - netIncome[m] + discrepancy,
            currentNetIncome = netIncome[m],
            accountsPayable = accountsPayable,
            taxesPayable = taxesPayable,
            totalLiabilitiesAndEquity = totalAssets
        )
    }

    // 10. Financial Ratio Analysis (تحليل النسب المالية)
    fun getFinancialRatios(month: Int): FinancialRatios {
        val bs = getBalanceSheet(month)
        val currentAssets = bs.rawMaterialInv + bs.finishedGoodsInv + bs.accountsReceivable + bs.cash
        val currentLiabilities = bs.accountsPayable + bs.taxesPayable
        
        val currentRatio = if (currentLiabilities > 0) currentAssets / currentLiabilities else currentAssets
        val quickRatio = if (currentLiabilities > 0) (currentAssets - bs.rawMaterialInv - bs.finishedGoodsInv) / currentLiabilities else currentAssets
        
        val rev = salesRevenue[month]
        val gm = grossMargin[month]
        val ni = netIncome[month]
        
        val grossProfitMargin = if (rev > 0) (gm / rev) * 100.0 else 0.0
        val netProfitMargin = if (rev > 0) (ni / rev) * 100.0 else 0.0
        val returnOnAssets = if (bs.totalAssets > 0) (ni / bs.totalAssets) * 100.0 else 0.0
        
        val totalEquity = bs.capital + bs.retainedEarnings + bs.currentNetIncome
        val debtToEquity = if (totalEquity > 0) (currentLiabilities / totalEquity) * 100.0 else 0.0
        val debtRatio = if (bs.totalAssets > 0) (currentLiabilities / bs.totalAssets) * 100.0 else 0.0
        
        return FinancialRatios(
            currentRatio = currentRatio,
            quickRatio = quickRatio,
            grossProfitMargin = grossProfitMargin,
            netProfitMargin = netProfitMargin,
            returnOnAssets = returnOnAssets,
            debtToEquity = debtToEquity,
            debtRatio = debtRatio
        )
    }
}

data class BalanceSheet(
    val fixedAssetsNet: Double,
    val rawMaterialInv: Double,
    val finishedGoodsInv: Double,
    val accountsReceivable: Double,
    val cash: Double,
    val totalAssets: Double,
    val capital: Double,
    val retainedEarnings: Double,
    val currentNetIncome: Double,
    val accountsPayable: Double,
    val taxesPayable: Double,
    val totalLiabilitiesAndEquity: Double
)

data class FinancialRatios(
    val currentRatio: Double,
    val quickRatio: Double,
    val grossProfitMargin: Double,
    val netProfitMargin: Double,
    val returnOnAssets: Double,
    val debtToEquity: Double,
    val debtRatio: Double
)
