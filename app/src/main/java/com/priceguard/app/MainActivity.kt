package com.priceguard.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.priceguard.app.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.navigationBars
                ) { innerPadding ->
                    BarcodeScannerApp(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun BarcodeScannerApp(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: AppViewModel = viewModel()
    
    // Check camera permission
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF020617)) // Clean slate-950 dark background
    ) {
        if (hasCameraPermission) {
            NativeScannerScreen(viewModel = viewModel)
        } else {
            PermissionFallbackScreen(
                onRequestPermission = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                },
                onOpenSettings = {
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            )
        }
    }
}

@Composable
fun NativeScannerScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val historyItems by viewModel.historyItems.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()

    // Base color configurations (consistent with Obsidian dark theme)
    val emeraldAccent = Color(0xFF10B981)
    val slate900 = Color(0xFF0F172A)
    val slate800 = Color(0xFF1E293B)
    val textMuted = Color(0xFF94A3B8)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        // App Header Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
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
                        .clip(RoundedCornerShape(12.dp))
                        .background(emeraldAccent.copy(alpha = 0.1f))
                        .border(1.dp, emeraldAccent.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "PriceGuard Icon",
                        tint = emeraldAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "PriceGuard",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = "Global coverage",
                            tint = emeraldAccent.copy(alpha = 0.8f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Text(
                        text = Localization.get("hdr_desc"),
                        fontSize = 11.sp,
                        color = textMuted
                    )
                }
            }

            // Real-time Status Badge
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(slate900)
                    .border(1.dp, slate800, CircleShape)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val pulseColor = if (isScanning) emeraldAccent else textMuted
                    val pulseAnim = if (isScanning) {
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        infiniteTransition.animateFloat(
                            initialValue = 0.3f,
                            targetValue = 1.0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = EaseInOutSine),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulseAlpha"
                        ).value
                    } else 1.0f

                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(pulseColor.copy(alpha = pulseAnim))
                    )
                    Text(
                        text = if (isScanning) Localization.get("scanning") else Localization.get("ready"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isScanning) emeraldAccent else textMuted
                    )
                }
            }
        }

        // Camera ViewFinder Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(slate900)
                .border(1.dp, slate800, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (isScanning) {
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    onBarcodeScanned = { barcode ->
                        viewModel.stopScanning()
                        viewModel.fetchProduct(barcode)
                    }
                )

                // Laser Overlay decoration
                val infiniteTransition = rememberInfiniteTransition(label = "laser")
                val laserOffset = infiniteTransition.animateFloat(
                    initialValue = 0.15f,
                    targetValue = 0.85f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2500, easing = EaseInOutQuad),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "laserOffset"
                ).value

                // Draw overlay bounds
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .aspectRatio(1f)
                        .border(1.5.dp, emeraldAccent.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                ) {
                    // Frame corners
                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(laserOffset)
                        ) {
                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .align(Alignment.BottomCenter)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color.Transparent,
                                                emeraldAccent,
                                                Color.Transparent
                                            )
                                        )
                                    )
                                    .drawBehind {
                                        drawCircle(
                                            color = emeraldAccent,
                                            radius = 4.dp.toPx(),
                                            center = Offset(size.width / 2, size.height / 2)
                                        )
                                    }
                            )
                        }
                    }
                }

                Text(
                    text = Localization.get("view_finder_text"),
                    fontSize = 11.sp,
                    color = emeraldAccent,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 20.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            } else {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(slate800.copy(alpha = 0.5f))
                            .border(1.dp, slate800, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Camera,
                            contentDescription = "Camera standby",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = Localization.get("cam_off_title"),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = Localization.get("cam_off_desc"),
                        fontSize = 11.sp,
                        color = textMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Trigger Scan Button Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!isScanning) {
                Button(
                    onClick = { viewModel.startScanning() },
                    colors = ButtonDefaults.buttonColors(containerColor = emeraldAccent),
                    contentPadding = PaddingValues(vertical = 15.dp),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan icon",
                            tint = Color(0xFF020617)
                        )
                        Text(
                            text = Localization.get("btn_scan_start"),
                            color = Color(0xFF020617),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                Button(
                    onClick = { viewModel.stopScanning() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    contentPadding = PaddingValues(vertical = 15.dp),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.StopCircle,
                            contentDescription = "Stop icon",
                            tint = Color.White
                        )
                        Text(
                            text = Localization.get("btn_scan_stop"),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Dynamic State Result card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = slate900),
            border = BorderStroke(1.dp, slate800),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                // Shield header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .drawBehind {
                            drawLine(
                                color = slate800,
                                start = Offset(0f, size.height + 12.dp.toPx()),
                                end = Offset(size.width, size.height + 12.dp.toPx()),
                                strokeWidth = 1.dp.toPx()
                            )
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = "Protection status",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = Localization.get("lbl_scan_info"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = textMuted,
                            letterSpacing = 1.sp
                        )
                    }

                    val dateStr = remember(uiState) {
                        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    }
                    Text(
                        text = if (uiState is ScanUiState.Success) dateStr else "—",
                        fontSize = 10.sp,
                        color = textMuted,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (val state = uiState) {
                    is ScanUiState.Idle -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Searching placeholder",
                                tint = slate800,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = Localization.get("lbl_scan_prompt"),
                                fontSize = 11.sp,
                                color = textMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    is ScanUiState.Loading -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = emeraldAccent,
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = Localization.get("lbl_loading_fetch"),
                                fontSize = 11.sp,
                                color = emeraldAccent,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    is ScanUiState.Success -> {
                        ProductDetailsView(item = state.item, viewModel = viewModel)
                    }

                    is ScanUiState.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Error icon",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = Localization.get("lbl_op_alert"),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = state.message,
                                fontSize = 11.sp,
                                color = textMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Scan History Section
        HistorySection(viewModel = viewModel, historyItems = historyItems)
    }
}

@Composable
fun ProductDetailsView(item: HistoryItem, viewModel: AppViewModel) {
    val textMuted = Color(0xFF94A3B8)
    val emeraldAccent = Color(0xFF10B981)
    val slate800 = Color(0xFF1E293B)

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Upper section view
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Coil AsyncImage
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .border(1.dp, slate800, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (item.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = "Product picture",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = "Product logo placeholder",
                        tint = slate800,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = item.brand.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = emeraldAccent,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = item.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "UPC/EAN: ${item.barcode}",
                    fontSize = 11.sp,
                    color = textMuted,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Mid section Info Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Market Price Estimator Card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.3f))
                    .border(1.dp, slate800.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = Localization.get("lbl_price_val"),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = textMuted,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = item.price,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(emeraldAccent.copy(alpha = 0.1f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = Localization.get("lbl_est"),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = emeraldAccent
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = Localization.get("lbl_price_basis"),
                        fontSize = 8.sp,
                        color = textMuted.copy(alpha = 0.7f),
                        lineHeight = 10.sp
                    )
                }
            }

            // Nutri Score Health Grade Card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.3f))
                    .border(1.dp, slate800.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = Localization.get("lbl_nutri_val"),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = textMuted,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val score = item.nutriscore.lowercase().trim()
                    val (badgeColor, healthLabel) = when (score) {
                        "a" -> Color(0xFF0284C7) to Localization.get("health_healthy")
                        "b" -> Color(0xFF10B981) to Localization.get("health_healthy")
                        "c" -> Color(0xFFF59E0B) to Localization.get("health_moderate")
                        "d" -> Color(0xFFF97316) to Localization.get("health_caution")
                        "e" -> Color(0xFFEF4444) to Localization.get("health_caution")
                        else -> Color(0xFF64748B) to Localization.get("unknown_grade")
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(badgeColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.nutriscore.uppercase(),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                        Text(
                            text = healthLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Additives and Processing warning panel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.3f))
                .border(1.dp, slate800.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Localization.get("lbl_health_security"),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = textMuted,
                        letterSpacing = 0.5.sp
                    )

                    // Overall status badge helper
                    val overallCaution = item.novaGroup >= 4 || item.additivesCount > 5
                    val statusColor = if (overallCaution) Color(0xFFEF4444) else emeraldAccent
                    val statusLabel = if (overallCaution) Localization.get("health_caution") else Localization.get("health_healthy")

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Text(
                            text = statusLabel,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // NOVA group indicator
                    val novaText = when (item.novaGroup) {
                        1 -> Localization.get("health_unprocessed")
                        2 -> Localization.get("health_culinary")
                        3 -> Localization.get("health_processed2")
                        4 -> Localization.get("health_ultra")
                        else -> Localization.get("no_data")
                    }
                    val novaColor = when (item.novaGroup) {
                        1 -> Color(0xFF0284C7)
                        2 -> Color(0xFF10B981)
                        3 -> Color(0xFFF59E0B)
                        4 -> Color(0xFFEF4444)
                        else -> Color(0xFF64748B)
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.03f))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(novaColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (item.novaGroup > 0) item.novaGroup.toString() else "-",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Column {
                            Text(text = Localization.get("lbl_nova_title"), fontSize = 8.sp, color = textMuted)
                            Text(text = novaText, fontSize = 9.sp, fontWeight = FontWeight.Medium, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    // Additives indicator
                    val additivesColor = when {
                        item.additivesCount == 0 -> Color(0xFF10B981)
                        item.additivesCount <= 2 -> Color(0xFF0284C7)
                        item.additivesCount <= 5 -> Color(0xFFF59E0B)
                        else -> Color(0xFFEF4444)
                    }
                    val additivesAlert = when {
                        item.additivesCount == 0 -> Localization.get("none")
                        item.additivesCount <= 2 -> "Low count"
                        item.additivesCount <= 5 -> "Moderate"
                        else -> "High alert!"
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.03f))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(additivesColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.additivesCount.toString(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Column {
                            Text(text = Localization.get("lbl_additives_title"), fontSize = 8.sp, color = textMuted)
                            Text(text = additivesAlert, fontSize = 9.sp, fontWeight = FontWeight.Medium, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        // Retailer Pricing Comparison Matrix
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.3f))
                .border(1.dp, slate800.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Localization.get("lbl_comparison_matrix"),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = textMuted,
                        letterSpacing = 0.5.sp
                    )

                    // Consumer ratings logic based on barcode modifier
                    val mult = viewModel.getMultiplierForBarcode(item.barcode)
                    val (badgeText, badgeColor) = when {
                        mult < 0.95 -> Localization.get("deal_great") to Color(0xFF10B981)
                        mult > 1.15 -> Localization.get("deal_overpriced") to Color(0xFFEF4444)
                        else -> Localization.get("deal_fair") to Color(0xFFF59E0B)
                    }
                    Box(
                        modifier = Modifier
                            .border(1.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            .background(badgeColor.copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Compile comparison list items
                val rawValue = try {
                    item.price.replace("[^\\d.]".toRegex(), "").toDoubleOrNull() ?: 4.99
                } catch (e: Exception) {
                    4.99
                }
                val isSuffix = item.price.contains("din")
                val currencySymbol = when {
                    item.price.contains("din") -> " din"
                    item.price.contains("€") -> "€"
                    item.price.contains("£") -> "£"
                    item.price.contains("¥") -> "¥"
                    item.price.contains("C$") -> "C$"
                    item.price.contains("A$") -> "A$"
                    else -> "$"
                }

                viewModel.getSupermarkets().forEach { pair ->
                    val shopName = pair.first
                    val shopMultiplier = pair.second
                    val finalPriceFloat = rawValue * shopMultiplier
                    
                    val finalFormattedPrice = if (isSuffix) {
                        "${Math.round(finalPriceFloat)}$currencySymbol"
                    } else {
                        "$currencySymbol${String.format(Locale.US, "%.2f", finalPriceFloat)}"
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.02f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(textMuted.copy(alpha = 0.6f))
                            )
                            Text(
                                text = shopName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal,
                                color = textMuted
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val percentage = Math.round((shopMultiplier - 1.0) * 100)
                            val (pctText, pctColor) = when {
                                percentage < 0 -> "-${Math.abs(percentage)}%" to emeraldAccent
                                percentage > 0 -> "+$percentage%" to textMuted
                                else -> "0%" to textMuted
                            }

                            Text(
                                text = pctText,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = pctColor,
                                modifier = Modifier
                                    .background(pctColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )

                            Text(
                                text = finalFormattedPrice,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Tag taxonomy container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.02f))
                .border(1.dp, slate800.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            Column {
                Text(
                    text = Localization.get("lbl_taxonomies"),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = textMuted,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.categories,
                    fontSize = 11.sp,
                    color = textMuted,
                    lineHeight = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun HistorySection(viewModel: AppViewModel, historyItems: List<HistoryItem>) {
    val textMuted = Color(0xFF94A3B8)
    val slate800 = Color(0xFF1E293B)
    val redAccent = Color(0xFFEF4444)

    var showClearConfirm by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "History indicator",
                    tint = Color(0xFF2DD4BF),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = Localization.get("lbl_history_title"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
            }

            if (historyItems.isNotEmpty()) {
                TextButton(
                    onClick = { showClearConfirm = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = redAccent)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear all logo",
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = Localization.get("btn_clear_text"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Log list items
        if (historyItems.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.01f))
                    .border(1.dp, slate800.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = "Empty list logo",
                    tint = Color(0xFF2DD4BF).copy(alpha = 0.3f),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = Localization.get("lbl_empty_hist"),
                    fontSize = 11.sp,
                    color = textMuted,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                historyItems.take(15).forEach { item ->
                    HistoryRowItem(item = item, viewModel = viewModel)
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            containerColor = Color(0xFF0F172A),
            title = {
                Text(
                    text = Localization.get("btn_clear_text") + "?",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 16.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearHistory()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = redAccent)
                ) {
                    Text(text = "Confirm", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearConfirm = false }
                ) {
                    Text(text = "Cancel", color = textMuted)
                }
            }
        )
    }
}

@Composable
fun HistoryRowItem(item: HistoryItem, viewModel: AppViewModel) {
    val textMuted = Color(0xFF94A3B8)
    val slate800 = Color(0xFF1E293B)
    val slate900 = Color(0xFF0F172A)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(slate900.copy(alpha = 0.6f))
            .border(1.dp, slate800.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .clickable {
                viewModel.fetchProduct(item.barcode, saveToHistory = false)
            }
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Coil image loaded status
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.3f))
                .border(1.dp, slate800.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (item.imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = "History thumb",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = "Placeholder",
                    tint = slate800,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = item.brand.uppercase(),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF10B981)
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = item.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.barcode,
                fontSize = 10.sp,
                color = textMuted,
                fontFamily = FontFamily.Monospace
            )
        }

        // Right values section
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = item.price,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(2.dp))

            // Score micro visual helper
            val score = item.nutriscore.lowercase().trim()
            val scoreColor = when (score) {
                "a" -> Color(0xFF0284C7)
                "b" -> Color(0xFF10B981)
                "c" -> Color(0xFFF59E0B)
                "d" -> Color(0xFFF97316)
                "e" -> Color(0xFFEF4444)
                else -> Color(0xFF64748B)
            }
            Text(
                text = if (item.nutriscore.isNotEmpty() && score != "?") "NUTRI ${item.nutriscore.uppercase()}" else "NO DATA",
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                color = scoreColor,
                letterSpacing = 0.5.sp
            )
        }

        // Row individual delete
        IconButton(
            onClick = { viewModel.deleteItem(item.id) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete item",
                tint = textMuted.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onBarcodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = androidx.camera.core.Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, BarcodeAnalyzer(onBarcodeScanned))
                    }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(context))

            previewView
        },
        modifier = modifier
    )
}

@Composable
fun PermissionFallbackScreen(
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val textMuted = Color(0xFF94A3B8)
    val emeraldAccent = Color(0xFF10B981)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x20EF4444), Color(0x05EF4444))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Permission Required",
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Camera Permission Required",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 20.sp
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Barcode scanning is executed fully locally on your device camera stream. Please grant camera access to scan products.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = textMuted,
                lineHeight = 20.sp
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(containerColor = emeraldAccent),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                text = "Grant Permission",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF020617)
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onOpenSettings,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = emeraldAccent),
            border = BorderStroke(1.dp, emeraldAccent.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                text = "Open App Settings",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = emeraldAccent
                )
            )
        }
    }
}
