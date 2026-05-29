package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.theme.MyApplicationTheme

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
    
    // List of permissions we want our app to request and register with the device
    val requiredPermissions = remember {
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    // Track permission states
    var permissionStates by remember {
        mutableStateOf(
            requiredPermissions.associateWith { permission ->
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    // Permission request launcher for multiple permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionStates = results
    }

    // Perform initial permission request if anything is missing
    LaunchedEffect(Unit) {
        val needsRequest = requiredPermissions.any {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needsRequest) {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    val hasCameraPermission = permissionStates[Manifest.permission.CAMERA] == true

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF020617)) // Match slate-950
    ) {
        if (hasCameraPermission) {
            // Display Web scanning container
            ScannerWebView()
        } else {
            // Display elegant Material 3 permission request card fallback
            PermissionFallbackScreen(
                onRequestPermission = {
                    permissionLauncher.launch(requiredPermissions)
                }
            )
        }
    }
}

@Composable
fun ScannerWebView() {
    var webViewError by remember { mutableStateOf<String?>(null) }
    
    if (webViewError != null) {
        WebViewErrorScreen(error = webViewError!!)
    } else {
        AndroidView(
            factory = { context ->
                try {
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        
                        // Configure custom WebView Client for standard navigations
                        webViewClient = WebViewClient()
                        
                        // Auto-authorize HTML5 WebRTC Camera Permission Requests inside standard app
                        webChromeClient = object : WebChromeClient() {
                            override fun onPermissionRequest(request: PermissionRequest) {
                                try {
                                    val requestedResources = request.resources
                                    val allowedResources = mutableListOf<String>()
                                    if (requestedResources != null) {
                                        for (res in requestedResources) {
                                            if (res == PermissionRequest.RESOURCE_VIDEO_CAPTURE) {
                                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                                    allowedResources.add(res)
                                                }
                                            }
                                        }
                                    }
                                    if (allowedResources.isNotEmpty()) {
                                        request.grant(allowedResources.toTypedArray())
                                    } else {
                                        request.deny()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    try {
                                        request.deny()
                                    } catch (ignored: Exception) {}
                                }
                            }
                        }
                        
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            allowFileAccess = true
                            allowContentAccess = true
                            mediaPlaybackRequiresUserGesture = false
                            
                            // Optimization settings
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            cacheMode = WebSettings.LOAD_DEFAULT
                        }
                        
                        // Load local index assets containing HTML5-QRCode and style definitions
                        loadUrl("file:///android_asset/www/index.html")
                    }
                } catch (t: Throwable) {
                    t.printStackTrace()
                    webViewError = t.localizedMessage ?: "Failed to initialize Android System WebView"
                    android.view.View(context)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun WebViewErrorScreen(error: String) {
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
                contentDescription = "System Error",
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "WebView Initialization Failure",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 20.sp
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "An error occurred while loading the Android System WebView: $error\n\nPlease ensure Android System WebView is installed and enabled on your device.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color(0xFF94A3B8), // slate-400
                lineHeight = 20.sp
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun PermissionFallbackScreen(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Material Icon Background Base
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
                contentDescription = "Camera Access Required",
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
                color = Color(0xFF94A3B8), // slate-400
                lineHeight = 20.sp
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF10B981) // Match emerald-500
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                text = "Grant Permission",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF020617) // Slate-950 matching
                )
            )
        }
    }
}
