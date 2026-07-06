package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetApp(viewModel: BudgetViewModel) {
    val context = LocalContext.current
    val allProjects by viewModel.allProjects.collectAsStateWithLifecycle()
    val activeProject by viewModel.currentProject.collectAsStateWithLifecycle()
    val currentData by viewModel.currentData.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(0) } // 0 to 9 for the 10 budgets
    var activeMonthIndex by remember { mutableStateOf(0) } // Active month selected for details/inputs
    var showDownloadDialog by remember { mutableStateOf(false) }
    var bannerDismissed by remember { mutableStateOf(false) }

    val engine = remember(currentData) { BudgetEngine(currentData) }

    // Enforce Right-to-Left Layout for Arabic app
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            LogoMark()
                            Column {
                                Text(
                                    text = "Houssem Compta",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = SkyBlue
                                )
                                Text(
                                    text = activeProject?.name ?: "موازنة جديدة آلياً",
                                    fontSize = 11.sp,
                                    color = TextGray
                                )
                            }
                        }
                    },
                    actions = {
                        if (activeProject != null || currentData.salesQty.isNotEmpty()) {
                            IconButton(onClick = { showSaveDialog = true }) {
                                Icon(Icons.Default.Save, contentDescription = "حفظ المشروع", tint = SkyBlue)
                            }
                        }
                        IconButton(onClick = { viewModel.createNewProjectSession() }) {
                            Icon(Icons.Default.AddBusiness, contentDescription = "جلسة جديدة", tint = SkyBlue)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DeepBlack,
                        titleContentColor = TextWhite
                    )
                )
            },
            containerColor = DeepBlack
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(DeepBlack)
            ) {
                if (activeProject == null && allProjects.isNotEmpty() && !showCreateDialog) {
                    // Welcome & Project Selector screen
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        HeroLogoSection()

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Charcoal),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0x2200B0FF), RoundedCornerShape(16.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(15.dp)
                            ) {
                                Text(
                                    text = "المشاريع المحفوظة",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SkyBlue
                                )
                                Divider(color = DarkGray)

                                LazyColumn(
                                    modifier = Modifier.heightIn(max = 260.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(allProjects) { proj ->
                                        ProjectItemRow(
                                            project = proj,
                                            onSelect = { viewModel.selectProject(proj) },
                                            onDelete = { viewModel.deleteProject(proj.id) }
                                        )
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = { showCreateDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = SkyBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("start_wizard_button")
                        ) {
                            Icon(Icons.Default.AutoMode, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "إنشاء موازنة تقديرية آليًا",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                } else {
                    // Core Dashboard with budget sheets
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Top horizontal selector for the 10 budgets
                        BudgetCategoryTabs(
                            selectedTabIndex = activeTab,
                            onTabSelected = { activeTab = it }
                        )

                        // Main budget content container
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Spacer(modifier = Modifier.height(6.dp))

                            // Key Metric Card (Houssem Compta Elegant Dark style)
                            KeyMetricCard(engine = engine)

                            // Expandable Baseline Configurator
                            BaselineConfiguratorPanel(
                                currentData = currentData,
                                onGenerate = { data ->
                                    viewModel.updateWorkingData(data)
                                    Toast.makeText(context, "تم توليد وتحديث الموازنة بنجاح!", Toast.LENGTH_SHORT).show()
                                }
                            )

                            // Quick Monthly input card
                            MonthlyQuickInputCard(
                                currentData = currentData,
                                activeMonthIndex = activeMonthIndex,
                                onMonthSelected = { activeMonthIndex = it },
                                onDataChanged = { viewModel.updateWorkingData(it) }
                            )

                            // Export and Print Actions Row
                            ActionsRow(
                                onExportExcel = {
                                    val bName = getBudgetNameByIndex(activeTab)
                                    val path = viewModel.exportToExcel(context, engine, bName)
                                    if (path != null) {
                                        Toast.makeText(context, "تم التصدير إلى Excel بنجاح!\nالمسار: $path", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "فشل تصدير البيانات", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onPrint = {
                                    val bName = getBudgetNameByIndex(activeTab)
                                    val html = viewModel.generateFullHtmlReport(engine)
                                    viewModel.printReport(context, engine, bName, html)
                                }
                            )

                            // Render Selected Budget Component
                            BudgetSheetRenderer(
                                tabIndex = activeTab,
                                engine = engine
                            )

                            Spacer(modifier = Modifier.height(90.dp)) // Padding for floating banner
                        }
                    }
                }

                // Create wizard Dialog
                if (showCreateDialog) {
                    CreateProjectWizardDialog(
                        onDismiss = { showCreateDialog = false },
                        onGenerate = { data ->
                            viewModel.updateWorkingData(data)
                            showCreateDialog = false
                        }
                    )
                }

                // Save Project Dialog
                if (showSaveDialog) {
                    SaveProjectDialog(
                        initialName = activeProject?.name ?: "",
                        onDismiss = { showSaveDialog = false },
                        onSave = { name ->
                            viewModel.saveProject(name) {
                                Toast.makeText(context, "تم حفظ المشروع بنجاح!", Toast.LENGTH_SHORT).show()
                                showSaveDialog = false
                            }
                        }
                    )
                }

                // Floating Download Suggestion (Elegant Dark HUD overlay)
                if (!bannerDismissed) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 12.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        FloatingDownloadBanner(
                            onDownloadClick = { showDownloadDialog = true },
                            onDismiss = { bannerDismissed = true }
                        )
                    }
                }

                // Download instructions dialog
                if (showDownloadDialog) {
                    DownloadInstructionsDialog(onDismiss = { showDownloadDialog = false })
                }
            }
        }
    }
}

@Composable
fun LogoMark() {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(
                Brush.radialGradient(listOf(SkyBlueLight, SkyBlue)),
                CircleShape
            )
            .border(2.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "HC",
            color = Color.Black,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
fun HeroLogoSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Aesthetic custom logo with Canvas circular design
        Box(
            modifier = Modifier
                .size(110.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(SkyBlue, DeepBlack, SkyBlueLight, SkyBlue)
                    ),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8f)
                )
            }
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Charcoal, CircleShape)
                    .border(1.dp, SkyBlueDark, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "HC",
                    color = SkyBlue,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Houssem Compta",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "نظام الموازنات التقديرية الذكية بالجزائر",
            fontSize = 13.sp,
            color = SkyBlueLight,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ProjectItemRow(
    project: Project,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CharcoalLight)
            .clickable { onSelect() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.BusinessCenter, contentDescription = null, tint = SkyBlue)
            Column {
                Text(
                    text = project.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Text(
                    text = java.text.DateFormat.getDateInstance().format(java.util.Date(project.createdAt)),
                    fontSize = 11.sp,
                    color = TextGray
                )
            }
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = RedError)
        }
    }
}

@Composable
fun BudgetCategoryTabs(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    val categories = listOf(
        "📊 المبيعات",
        "🛒 المشتريات",
        "🏭 الإنتاج",
        "👷 اليد العاملة",
        "📦 المواد الأولية",
        "💰 المصاريف",
        "💵 الخزينة",
        "📈 قائمة الدخل",
        "⚖️ الميزانية",
        "🧮 النسب المالية"
    )

    ScrollableTabRow(
        selectedTabIndex = selectedTabIndex,
        containerColor = DeepBlack,
        contentColor = SkyBlue,
        edgePadding = 12.dp,
        divider = { Divider(color = DarkGray) },
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                color = SkyBlue
            )
        }
    ) {
        categories.forEachIndexed { index, name ->
            Tab(
                selected = selectedTabIndex == index,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        text = name,
                        fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp
                    )
                }
            )
        }
    }
}

// Expander to tweak baseline inputs instantly
@Composable
fun BaselineConfiguratorPanel(
    currentData: ProjectData,
    onGenerate: (ProjectData) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    // Baseline States
    var baseQty by remember { mutableStateOf(currentData.salesQty.firstOrNull()?.toString() ?: "1000") }
    var growth by remember { mutableStateOf(currentData.salesCollectionCashPercent.toString()) } // placeholder state for slider
    var price by remember { mutableStateOf(currentData.unitPrice.firstOrNull()?.toString() ?: "150") }
    var rmCost by remember { mutableStateOf(currentData.rawMaterialCost.firstOrNull()?.toString() ?: "15") }
    var laborRate by remember { mutableStateOf(currentData.laborRate.firstOrNull()?.toString() ?: "20") }
    var varOH by remember { mutableStateOf(currentData.variableOverheadRate.firstOrNull()?.toString() ?: "5") }
    var fixedOH by remember { mutableStateOf(currentData.fixedOverhead.firstOrNull()?.toString() ?: "8000") }
    var fixedSA by remember { mutableStateOf(currentData.fixedSAndA.firstOrNull()?.toString() ?: "4000") }
    var depr by remember { mutableStateOf(currentData.depreciation.firstOrNull()?.toString() ?: "1500") }
    var initCash by remember { mutableStateOf(currentData.initialCash.toString()) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Charcoal),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x2200B0FF), RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = SkyBlue)
                    Text(
                        "المدخلات العامة والمولد الآلي",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SkyBlue
                    )
                }
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = TextGray
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ModernNumberInput(
                            value = baseQty,
                            onValueChange = { baseQty = it },
                            label = "كمية مبيعات البداية",
                            suffix = "وحدة",
                            modifier = Modifier.weight(1f)
                        )
                        ModernNumberInput(
                            value = price,
                            onValueChange = { price = it },
                            label = "سعر البيع المقدر",
                            suffix = "دج",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ModernNumberInput(
                            value = rmCost,
                            onValueChange = { rmCost = it },
                            label = "سعر شراء المادة الأولية",
                            suffix = "دج/كغ",
                            modifier = Modifier.weight(1f)
                        )
                        ModernNumberInput(
                            value = laborRate,
                            onValueChange = { laborRate = it },
                            label = "تكلفة أجر الساعة",
                            suffix = "دج",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ModernNumberInput(
                            value = fixedOH,
                            onValueChange = { fixedOH = it },
                            label = "المصاريف الثابتة للشهر",
                            suffix = "دج",
                            modifier = Modifier.weight(1f)
                        )
                        ModernNumberInput(
                            value = depr,
                            onValueChange = { depr = it },
                            label = "الاهتلاك الشهري (أعباء)",
                            suffix = "دج",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ModernNumberInput(
                            value = initCash,
                            onValueChange = { initCash = it },
                            label = "سيولة أول المدة المقدرة",
                            suffix = "دج",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Button(
                        onClick = {
                            val bQty = baseQty.toDoubleOrNull() ?: 1000.0
                            val pr = price.toDoubleOrNull() ?: 150.0
                            val rm = rmCost.toDoubleOrNull() ?: 15.0
                            val lb = laborRate.toDoubleOrNull() ?: 20.0
                            val fOH = fixedOH.toDoubleOrNull() ?: 8000.0
                            val fSA = fixedSA.toDoubleOrNull() ?: 4000.0
                            val dp = depr.toDoubleOrNull() ?: 1500.0
                            val csh = initCash.toDoubleOrNull() ?: 50000.0
                            
                            val freshData = ProjectData.createDefault(
                                baseSalesQty = bQty,
                                growthPercent = 3.0,
                                price = pr,
                                rmCost = rm,
                                laborRateVal = lb,
                                varOverhead = varOH.toDoubleOrNull() ?: 5.0,
                                fixedOH = fOH,
                                fixedSA = fSA,
                                depr = dp,
                                initCashVal = csh
                            )
                            onGenerate(freshData)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SkyBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text("إعادة توليد وتحديث آلي", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Quick Month Picker & Editor
@Composable
fun MonthlyQuickInputCard(
    currentData: ProjectData,
    activeMonthIndex: Int,
    onMonthSelected: (Int) -> Unit,
    onDataChanged: (ProjectData) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Charcoal),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x2200B0FF), RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                "تعديل مدخلات شهر معين بالتحديد",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = SkyBlue,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            // 12 Months picker
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ALGERIAN_MONTHS.forEachIndexed { idx, name ->
                    val isSelected = activeMonthIndex == idx
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) SkyBlue else CharcoalLight)
                            .border(1.dp, if (isSelected) SkyBlueLight else DarkGray, RoundedCornerShape(8.dp))
                            .clickable { onMonthSelected(idx) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = name,
                            color = if (isSelected) Color.Black else TextWhite,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Values to edit for selected month
            val monthName = ALGERIAN_MONTHS[activeMonthIndex]
            var mSalesQty by remember(activeMonthIndex, currentData) {
                mutableStateOf(currentData.salesQty[activeMonthIndex].toString())
            }
            var mPrice by remember(activeMonthIndex, currentData) {
                mutableStateOf(currentData.unitPrice[activeMonthIndex].toString())
            }
            var mRmCost by remember(activeMonthIndex, currentData) {
                mutableStateOf(currentData.rawMaterialCost[activeMonthIndex].toString())
            }

            Text(
                text = "مدخلات شهر $monthName التقديرية :",
                fontSize = 13.sp,
                color = TextGray,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ModernNumberInput(
                    value = mSalesQty,
                    onValueChange = {
                        mSalesQty = it
                        val dVal = it.toDoubleOrNull() ?: 0.0
                        val updatedSales = currentData.salesQty.toMutableList()
                        updatedSales[activeMonthIndex] = dVal
                        onDataChanged(currentData.copy(salesQty = updatedSales))
                    },
                    label = "كمية مبيعات الشهر",
                    suffix = "وحدة",
                    modifier = Modifier.weight(1f)
                )

                ModernNumberInput(
                    value = mPrice,
                    onValueChange = {
                        mPrice = it
                        val dVal = it.toDoubleOrNull() ?: 0.0
                        val updatedPrices = currentData.unitPrice.toMutableList()
                        updatedPrices[activeMonthIndex] = dVal
                        onDataChanged(currentData.copy(unitPrice = updatedPrices))
                    },
                    label = "سعر البيع للشهر",
                    suffix = "دج",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ModernNumberInput(
                    value = mRmCost,
                    onValueChange = {
                        mRmCost = it
                        val dVal = it.toDoubleOrNull() ?: 0.0
                        val updatedRm = currentData.rawMaterialCost.toMutableList()
                        updatedRm[activeMonthIndex] = dVal
                        onDataChanged(currentData.copy(rawMaterialCost = updatedRm))
                    },
                    label = "تكلفة شراء كغ مادة أولية",
                    suffix = "دج/كغ",
                    modifier = Modifier.weight(1.5f)
                )
            }
        }
    }
}

@Composable
fun ModernNumberInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suffix: String = "دج",
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextGray, fontSize = 11.sp) },
        suffix = { Text(suffix, color = SkyBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SkyBlue,
            unfocusedBorderColor = DarkGray,
            focusedTextColor = TextWhite,
            unfocusedTextColor = TextWhite,
            focusedContainerColor = CharcoalLight,
            unfocusedContainerColor = Charcoal
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.testTag("number_input_field")
    )
}

@Composable
fun ActionsRow(
    onExportExcel: () -> Unit,
    onPrint: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onExportExcel,
            colors = ButtonDefaults.buttonColors(containerColor = CharcoalLight),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.FileDownload, contentDescription = null, tint = SkyBlue)
            Spacer(modifier = Modifier.width(6.dp))
            Text("تصدير Excel", color = TextWhite, fontSize = 13.sp)
        }

        Button(
            onClick = onPrint,
            colors = ButtonDefaults.buttonColors(containerColor = SkyBlue),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.LocalPrintshop, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(6.dp))
            Text("طباعة و PDF", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

// Dialog for Wizard Baseline
@Composable
fun CreateProjectWizardDialog(
    onDismiss: () -> Unit,
    onGenerate: (ProjectData) -> Unit
) {
    var baseQty by remember { mutableStateOf("1000") }
    var price by remember { mutableStateOf("150") }
    var rmCost by remember { mutableStateOf("15") }
    var laborRate by remember { mutableStateOf("20") }
    var initCash by remember { mutableStateOf("50000") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Charcoal),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                Text(
                    "مولد الموازنة التقديرية الآلي",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = SkyBlue,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "أدخل المعطيات الأساسية ليقوم التطبيق بتوليد موازنات تقديرية لـ 12 شهرًا بالكامل آليًا.",
                    fontSize = 12.sp,
                    color = TextGray,
                    textAlign = TextAlign.Center
                )

                ModernNumberInput(value = baseQty, onValueChange = { baseQty = it }, label = "كمية مبيعات البداية")
                ModernNumberInput(value = price, onValueChange = { price = it }, label = "سعر البيع المقدر للوحدة")
                ModernNumberInput(value = rmCost, onValueChange = { rmCost = it }, label = "سعر شراء كغ المادة الأولية")
                ModernNumberInput(value = laborRate, onValueChange = { laborRate = it }, label = "تكلفة أجر الساعة للعمال")
                ModernNumberInput(value = initCash, onValueChange = { initCash = it }, label = "السيولة النقدية المتوفرة حالياً")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("إلغاء", color = TextGray)
                    }
                    Button(
                        onClick = {
                            val data = ProjectData.createDefault(
                                baseSalesQty = baseQty.toDoubleOrNull() ?: 1000.0,
                                growthPercent = 3.0,
                                price = price.toDoubleOrNull() ?: 150.0,
                                rmCost = rmCost.toDoubleOrNull() ?: 15.0,
                                laborRateVal = laborRate.toDoubleOrNull() ?: 20.0,
                                initCashVal = initCash.toDoubleOrNull() ?: 50000.0
                            )
                            onGenerate(data)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SkyBlue),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text("توليد الموازنات", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Dialog for Save project
@Composable
fun SaveProjectDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName.ifEmpty { "موازنة تقديرية جديدة" }) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Charcoal),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                Text(
                    "حفظ المشروع",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = SkyBlue
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم المشروع", color = TextGray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SkyBlue,
                        unfocusedBorderColor = DarkGray,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("إلغاء", color = TextGray)
                    }
                    Button(
                        onClick = { onSave(name) },
                        colors = ButtonDefaults.buttonColors(containerColor = SkyBlue),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text("حفظ", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun BudgetSheetRenderer(
    tabIndex: Int,
    engine: BudgetEngine
) {
    when (tabIndex) {
        0 -> SalesBudgetSheet(engine)
        1 -> PurchasesBudgetSheet(engine)
        2 -> ProductionBudgetSheet(engine)
        3 -> LaborBudgetSheet(engine)
        4 -> RawMaterialsBudgetSheet(engine)
        5 -> ExpensesBudgetSheet(engine)
        6 -> TreasuryBudgetSheet(engine)
        7 -> IncomeStatementBudgetSheet(engine)
        8 -> BalanceSheetBudgetSheet(engine)
        9 -> RatiosAnalysisSheet(engine)
    }
}

@Composable
fun SalesBudgetSheet(engine: BudgetEngine) {
    BudgetTableCard(
        title = "📊 الموازنة التقديرية للمبيعات",
        rows = listOf(
            TableRowData("الكمية المباعة (وحدة)", engine.data.salesQty),
            TableRowData("سعر البيع الأحادي (دج)", engine.data.unitPrice),
            TableRowData("إجمالي رقم الأعمال (دج)", engine.salesRevenue, isHighlighted = true),
            TableRowData("المتحصلات التقديرية (دج)", engine.salesCollections)
        )
    )
}

@Composable
fun ProductionBudgetSheet(engine: BudgetEngine) {
    BudgetTableCard(
        title = "🏭 موازنة الإنتاج التقديرية",
        rows = listOf(
            TableRowData("المبيعات المستهدفة", engine.data.salesQty),
            TableRowData("+ مخزون آخر المدة المرغوب فيه", engine.endingFG),
            TableRowData("إجمالي الاحتياجات", engine.totalFGNeeded),
            TableRowData("- مخزون أول المدة للغلق", engine.beginningFG),
            TableRowData("الكمية الواجب إنتاجها", engine.productionQty, isHighlighted = true)
        )
    )
}

@Composable
fun RawMaterialsBudgetSheet(engine: BudgetEngine) {
    BudgetTableCard(
        title = "📦 موازنة المواد الأولية للإنتاج",
        rows = listOf(
            TableRowData("الكمية الواجب إنتاجها", engine.productionQty),
            TableRowData("معامل الاستهلاك للوحدة (كغ)", List(12) { engine.data.rawMaterialPerUnit }),
            TableRowData("إجمالي احتياجات المواد الأولية (كغ)", engine.rawMaterialNeeded, isHighlighted = true)
        )
    )
}

@Composable
fun PurchasesBudgetSheet(engine: BudgetEngine) {
    BudgetTableCard(
        title = "🛒 الموازنة التقديرية لمشتريات المواد",
        rows = listOf(
            TableRowData("احتياج الإنتاج من المواد (كغ)", engine.rawMaterialNeeded),
            TableRowData("+ مخزون آخر المدة المرغوب (كغ)", engine.endingRM),
            TableRowData("إجمالي الاحتياجات الكلية (كغ)", engine.totalRMNeeded),
            TableRowData("- مخزون أول المدة للغلق", engine.beginningRM),
            TableRowData("الكمية الواجب شراؤها (كغ)", engine.rawMaterialToPurchase, isHighlighted = true),
            TableRowData("تكلفة شراء الكغ الواحد (دج)", engine.data.rawMaterialCost),
            TableRowData("تكلفة الشراء الإجمالية (دج)", engine.purchasesCost, isHighlighted = true),
            TableRowData("المدفوعات للموردين (دج)", engine.purchasesPayments)
        )
    )
}

@Composable
fun LaborBudgetSheet(engine: BudgetEngine) {
    BudgetTableCard(
        title = "👷 موازنة اليد العاملة المباشرة",
        rows = listOf(
            TableRowData("الكمية المقدرة لإنتاجها", engine.productionQty),
            TableRowData("ساعات العمل اللازمة للوحدة", List(12) { engine.data.laborHoursPerUnit }),
            TableRowData("إجمالي الساعات المطلوبة", engine.laborHours),
            TableRowData("معدل أجر الساعة (دج)", engine.data.laborRate),
            TableRowData("إجمالي تكاليف اليد العاملة (دج)", engine.laborCost, isHighlighted = true)
        )
    )
}

@Composable
fun ExpensesBudgetSheet(engine: BudgetEngine) {
    BudgetTableCard(
        title = "💰 موازنة المصاريف والأعباء التقديرية",
        rows = listOf(
            TableRowData("الأعباء المتغيرة التقديرية (دج)", engine.variableOverheadCost),
            TableRowData("التكاليف الثابتة لشهر العمل", engine.data.fixedOverhead),
            TableRowData("أعباء البيع والتسويق والإدارة", engine.data.fixedSAndA),
            TableRowData("إجمالي المصاريف التقديرية (دج)", engine.totalOverheadAndSAndA, isHighlighted = true),
            TableRowData("- منها اهتلاكات التثبيتات", engine.data.depreciation),
            TableRowData("المدفوعات المصاريف النقدية (دج)", engine.cashExpenses)
        )
    )
}

@Composable
fun TreasuryBudgetSheet(engine: BudgetEngine) {
    BudgetTableCard(
        title = "💵 موازنة الخزينة والنقدية التقديرية",
        rows = listOf(
            TableRowData("رصيد أول المدة (دج)", engine.cashBeginning),
            TableRowData("+ المقبوضات (مبيعات ومتحصلات)", engine.salesCollections),
            TableRowData("- مدفوعات المشتريات", engine.purchasesPayments),
            TableRowData("- مدفوعات الأجور لليد العاملة", engine.laborCost),
            TableRowData("- المدفوعات النقدية للأعباء", engine.cashExpenses),
            TableRowData("صافي التدفق النقدي (دج)", engine.cashNetFlow, isHighlighted = true),
            TableRowData("رصيد آخر المدة النهائي (دج)", engine.cashEnding, isHighlighted = true)
        )
    )
}

@Composable
fun IncomeStatementBudgetSheet(engine: BudgetEngine) {
    BudgetTableCard(
        title = "📈 جدول حسابات النتائج التقديري (قائمة الدخل)",
        rows = listOf(
            TableRowData("رقم الأعمال التقديري (دج)", engine.salesRevenue),
            TableRowData("تكلفة المبيعات المقدرة (COGS)", engine.cogs),
            TableRowData("هامش الربح الإجمالي التقديري", engine.grossMargin, isHighlighted = true),
            TableRowData("الأعباء العملياتية (التشغيلية)", engine.operatingExpenses),
            TableRowData("النتيجة العملياتية الصافية", engine.netOperatingIncome, isHighlighted = true),
            TableRowData("الضرائب التقديرية للدورة (IBS)", engine.ibsTax),
            TableRowData("النتيجة الصافية للدورة (دج)", engine.netIncome, isHighlighted = true)
        )
    )
}

@Composable
fun BalanceSheetBudgetSheet(engine: BudgetEngine) {
    // We can display the balance sheet for the active month or December (as typical final balance sheet)
    var selectedBsMonth by remember { mutableStateOf(11) } // Default December
    val bs = engine.getBalanceSheet(selectedBsMonth)

    Card(
        colors = CardDefaults.cardColors(containerColor = Charcoal),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
            Text(
                "⚖️ الميزانية التقديرية (نهاية ${ALGERIAN_MONTHS[selectedBsMonth]})",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = SkyBlue
            )

            // Month Selector for Balance Sheet
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ALGERIAN_MONTHS.forEachIndexed { idx, name ->
                    val isSelected = selectedBsMonth == idx
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) SkyBlue else CharcoalLight)
                            .clickable { selectedBsMonth = idx }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = name,
                            color = if (isSelected) Color.Black else TextWhite,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Divider(color = DarkGray)

            Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
                // Assets Section
                Text("الأصول (Assets)", fontWeight = FontWeight.Bold, color = SkyBlueLight, fontSize = 14.sp)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    BalanceSheetRow("الأصول غير الجارية الصافية (التثبيتات)", bs.fixedAssetsNet)
                    BalanceSheetRow("الأصول الجارية: مخزون المادة الأولية", bs.rawMaterialInv)
                    BalanceSheetRow("الأصول الجارية: مخزون المنتجات التامة", bs.finishedGoodsInv)
                    BalanceSheetRow("الأصول الجارية: الزبائن والحقوق", bs.accountsReceivable)
                    BalanceSheetRow("الخزينة (رصيد الخزينة)", bs.cash, isHighlight = true)
                    Divider(color = DarkGray)
                    BalanceSheetRow("إجمالي الأصول المقدرة", bs.totalAssets, isHighlight = true, isTotal = true)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Liabilities & Equity
                Text("الخصوم ورؤوس الأموال (Liabilities & Equity)", fontWeight = FontWeight.Bold, color = SkyBlueLight, fontSize = 14.sp)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    BalanceSheetRow("رأس المال الاجتماعي الصافي", bs.capital)
                    BalanceSheetRow("النتائج الاحتياطية السابقة المتراكمة", bs.retainedEarnings)
                    BalanceSheetRow("النتيجة الصافية للسنة المالية التقديرية", bs.currentNetIncome, isHighlight = true)
                    BalanceSheetRow("ديون الموردين (الحسابات الدائنة)", bs.accountsPayable)
                    BalanceSheetRow("الضرائب والرسوم المستحقة IBS", bs.taxesPayable)
                    Divider(color = DarkGray)
                    BalanceSheetRow("إجمالي الخصوم ورؤوس الأموال", bs.totalLiabilitiesAndEquity, isHighlight = true, isTotal = true)
                }
            }
        }
    }
}

@Composable
fun RatiosAnalysisSheet(engine: BudgetEngine) {
    var selectedRatioMonth by remember { mutableStateOf(11) }
    val ratios = engine.getFinancialRatios(selectedRatioMonth)

    Card(
        colors = CardDefaults.cardColors(containerColor = Charcoal),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x2200B0FF), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
            Text(
                "🧮 تحليل النسب والمؤشرات المالية (شهر ${ALGERIAN_MONTHS[selectedRatioMonth]})",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = SkyBlue
            )

            // Month Selector for Ratios
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ALGERIAN_MONTHS.forEachIndexed { idx, name ->
                    val isSelected = selectedRatioMonth == idx
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) SkyBlue else CharcoalLight)
                            .clickable { selectedRatioMonth = idx }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = name,
                            color = if (isSelected) Color.Black else TextWhite,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Divider(color = DarkGray)

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                RatioCardItem(
                    title = "نسبة السيولة العامة (Current Ratio)",
                    value = String.format(Locale.US, "%.2f", ratios.currentRatio),
                    description = "تقيس قدرة الأصول قصيرة الأجل على سداد الديون المستحقة.",
                    benchmark = "المعيار: > 1.0 (النسبة الحالية ممتازة)"
                )

                RatioCardItem(
                    title = "نسبة السيولة السريعة (Quick Ratio)",
                    value = String.format(Locale.US, "%.2f", ratios.quickRatio),
                    description = "توضح مقدرة السداد بدون الحاجة لتسييل مخزونات البضائع.",
                    benchmark = "المعيار: > 0.8 (قدرة ممتازة على الملاءمة)"
                )

                RatioCardItem(
                    title = "هامش الربح الإجمالي (Gross Profit Margin %)",
                    value = String.format(Locale.US, "%.1f", ratios.grossProfitMargin) + "%",
                    description = "النسبة المتبقية من رقم الأعمال بعد طرح التكاليف المباشرة.",
                    benchmark = "المعيار: أعلى من 20% يعني كفاءة إنتاجية عالية"
                )

                RatioCardItem(
                    title = "هامش الربح الصافي للشركة (Net Margin %)",
                    value = String.format(Locale.US, "%.1f", ratios.netProfitMargin) + "%",
                    description = "معدل العائد المحقق للربح الصافي من كل دينار من الأعمال.",
                    benchmark = "المعيار: الاستقرار أعلى من 10% ممتاز للشركة"
                )
            }
        }
    }
}

@Composable
fun BalanceSheetRow(label: String, value: Double, isHighlight: Boolean = false, isTotal: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .let { mod ->
                if (isTotal) {
                    mod.background(Brush.horizontalGradient(listOf(SkyBlueDark, Charcoal)))
                } else if (isHighlight) {
                    mod.background(CharcoalLight)
                } else {
                    mod
                }
            }
            .padding(vertical = 8.dp, horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = if (isTotal) 13.sp else 12.sp,
            fontWeight = if (isHighlight || isTotal) FontWeight.Bold else FontWeight.Normal,
            color = if (isTotal) Color.White else if (isHighlight) SkyBlueLight else TextWhite
        )
        Text(
            text = String.format(Locale.US, "%,.0f دج", value),
            fontSize = if (isTotal) 14.sp else 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isTotal) Color.White else if (isHighlight) GreenSuccess else TextWhite
        )
    }
}

@Composable
fun RatioCardItem(title: String, value: String, description: String, benchmark: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CharcoalLight)
            .border(1.dp, DarkGray, RoundedCornerShape(10.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontWeight = FontWeight.Bold, color = SkyBlue, fontSize = 13.sp)
            Text(value, fontWeight = FontWeight.Black, color = GreenSuccess, fontSize = 18.sp)
        }
        Text(description, color = TextGray, fontSize = 11.sp)
        Text(benchmark, color = SkyBlueLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

// Reusable Tabular Card for Budgets
@Composable
fun BudgetTableCard(
    title: String,
    rows: List<TableRowData>
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Charcoal),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x2200B0FF), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = SkyBlue,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Horizontal scrolling grid
            Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                Column {
                    // Header Month row
                    Row(
                        modifier = Modifier
                            .background(CharcoalLight)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(modifier = Modifier.width(180.dp)) {
                            Text("البيان", color = TextGray, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        ALGERIAN_MONTHS.forEach { name ->
                            Box(modifier = Modifier.width(90.dp), contentAlignment = Alignment.Center) {
                                Text(name, color = TextGray, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                        Box(modifier = Modifier.width(110.dp), contentAlignment = Alignment.Center) {
                            Text("المجموع", color = SkyBlue, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    // Value rows
                    rows.forEach { rowData ->
                        Row(
                            modifier = Modifier
                                .background(if (rowData.isHighlighted) CharcoalLight else Color.Transparent)
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.width(180.dp)) {
                                Text(
                                    text = rowData.label,
                                    color = if (rowData.isHighlighted) SkyBlueLight else TextWhite,
                                    fontWeight = if (rowData.isHighlighted) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            }
                            rowData.values.forEach { v ->
                                Box(modifier = Modifier.width(90.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = String.format(Locale.US, "%,.0f", v),
                                        color = if (rowData.isHighlighted) GreenSuccess else TextWhite,
                                        fontWeight = if (rowData.isHighlighted) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            // Total column
                            Box(modifier = Modifier.width(110.dp), contentAlignment = Alignment.Center) {
                                val sum = rowData.values.sum()
                                Text(
                                    text = if (rowData.label.contains("سعر") || rowData.label.contains("معامل")) "-" else String.format(Locale.US, "%,.0f", sum),
                                    color = if (rowData.isHighlighted) GreenSuccess else SkyBlue,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Divider(color = DarkGray.copy(alpha = 0.3f))
                    }
                }
            }
        }
    }
}

data class TableRowData(
    val label: String,
    val values: List<Double>,
    val isHighlighted: Boolean = false
)

fun getBudgetNameByIndex(index: Int): String {
    return when (index) {
        0 -> "المبيعات"
        1 -> "المشتريات"
        2 -> "الإنتاج"
        3 -> "اليد العاملة"
        4 -> "المواد الأولية"
        5 -> "المصاريف"
        6 -> "الخزينة"
        7 -> "قائمة الدخل"
        8 -> "الميزانية"
        else -> "التحليل المالي"
    }
}

@Composable
fun KeyMetricCard(engine: BudgetEngine) {
    val totalExpectedTreasury = engine.cashEnding.lastOrNull() ?: 0.0
    val initialCash = engine.data.initialCash
    val percentGrowth = if (initialCash > 0) {
        ((totalExpectedTreasury - initialCash) / initialCash * 100.0)
    } else {
        0.0
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(CharcoalLight, Charcoal)
                )
            )
            .border(1.dp, Color(0x3300B0FF), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        // Subtle decorative background glow
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 16.dp, y = (-16).dp)
                .size(90.dp)
                .background(Color(0x1200B0FF), CircleShape)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "إجمالي رصيد الخزينة المتوقع بنهاية السنة",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextGray
            )

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = String.format(Locale.US, "%,.0f", totalExpectedTreasury),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = SkyBlue
                )
                Text(
                    text = "د.ج",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = SkyBlueDark,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(
                            if (percentGrowth >= 0) Color(0x2210B981) else Color(0x22EF4444),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (percentGrowth >= 0) "↑" else "↓",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (percentGrowth >= 0) GreenSuccess else RedError
                    )
                }
                Text(
                    text = if (percentGrowth >= 0) {
                        String.format(Locale.US, "%.1f%% زيادة متوقعة عن السيولة الأولية", percentGrowth)
                    } else {
                        String.format(Locale.US, "%.1f%% انخفاض متوقع عن السيولة الأولية", Math.abs(percentGrowth))
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (percentGrowth >= 0) GreenSuccess else RedError
                )
            }
        }
    }
}

@Composable
fun FloatingDownloadBanner(onDownloadClick: () -> Unit, onDismiss: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xE600B0FF)), // SkyBlue with alpha 0.9
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp)
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "تطبيق Houssem Compta جاهز للتحميل والتثبيت الآن",
                    color = Color.Black,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = onDownloadClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text(
                        text = "رابط التحميل",
                        color = SkyBlue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق",
                        tint = Color.Black.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DownloadInstructionsDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Charcoal),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(1.dp, Color(0x3300B0FF), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color(0x1A00B0FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Android,
                        contentDescription = null,
                        tint = SkyBlue,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "تنزيل وتثبيت تطبيق Houssem Compta",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "التطبيق متوفر كنسخة أندرويد أصلية (APK) جاهزة للتثبيت على هاتفك الذكي بالكامل وبدون قيود.",
                    fontSize = 12.sp,
                    color = TextGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Divider(color = DarkGray)

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "خطوات الحصول على ملف APK للتطبيق المالي:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SkyBlue
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("1.", color = SkyBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(
                            "انقر على قائمة الإعدادات (أعلى اليمين في واجهة AI Studio).",
                            color = TextWhite,
                            fontSize = 12.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("2.", color = SkyBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(
                            "اختر 'Export project' لتحميل الكود المصدري كاملاً، أو 'Generate APK' لبناء حزمة التثبيت مباشرة.",
                            color = TextWhite,
                            fontSize = 12.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("3.", color = SkyBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(
                            "قم بتثبيت التطبيق على جهاز أندرويد الخاص بك لتستمتع بكافة مميزات موازنة المبيعات، المشتريات، الخزينة، والميزانية آلياً بنسبة 100%!",
                            color = TextWhite,
                            fontSize = 12.sp
                        )
                    }
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = SkyBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text("فهمت، شكراً لك", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

