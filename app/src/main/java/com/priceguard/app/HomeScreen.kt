@Composable
fun HomeScreen(onNavigateToScanner: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("PriceGuard", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(Localization.get("hdr_desc"), color = Color.Gray, textAlign = TextAlign.Center)
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(onClick = onNavigateToScanner, modifier = Modifier.fillMaxWidth()) {
            Text(Localization.get("btn_scan_start"))
        }
    }
}