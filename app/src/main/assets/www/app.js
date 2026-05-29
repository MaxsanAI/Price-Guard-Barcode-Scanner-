// Web Product Barcode Scanner Client Engine
document.addEventListener("DOMContentLoaded", () => {
  // Elements References
  const btnScanStart = document.getElementById("btn-scan-start");
  const btnScanStop = document.getElementById("btn-scan-stop");
  const btnScanLabel = document.getElementById("btn-scan-label");
  const scanIcon = document.getElementById("scan-icon");
  const badgeDot = document.getElementById("status-dot");
  const badgeText = document.getElementById("status-text");
  const viewfinderOverlay = document.getElementById("viewfinder-overlay");
  const sensorPlaceholder = document.getElementById("sensor-placeholder");
  
  const cameraSelectorContainer = document.getElementById("camera-selector-container");
  const cameraSelect = document.getElementById("camera-select");
  
  // Results Elements References
  const resultsCard = document.getElementById("results-card");
  const resultsTimestamp = document.getElementById("result-timestamp");
  const resultsIdle = document.getElementById("results-idle");
  const resultsLoading = document.getElementById("results-loading");
  const resultsData = document.getElementById("results-data");
  const resultsError = document.getElementById("results-error");
  const resultsErrorMsg = document.getElementById("results-error-msg");
  
  // Product details
  const prodImg = document.getElementById("prod-img");
  const prodImgPlaceholder = document.getElementById("prod-img-placeholder");
  const prodBrand = document.getElementById("prod-brand");
  const prodName = document.getElementById("prod-name");
  const prodBarcode = document.getElementById("prod-barcode");
  const prodPrice = document.getElementById("prod-price");
  const prodNutriScoreBadge = document.getElementById("prod-nutriscore-badge");
  const prodNutriScoreLabel = document.getElementById("prod-nutriscore-label");
  const prodCategories = document.getElementById("prod-categories");

  let html5QrCode = null;
  let isScanning = false;
  let lastScannedBarcode = "";

  // 1. Initialize Html5Qrcode instance
  try {
    html5QrCode = new Html5Qrcode("reader");
  } catch (err) {
    console.error("Html5Qrcode init failure:", err);
    updateStatus("Error", "bg-rose-500");
  }

  // Populate device cameras if accessible
  if (typeof Html5Qrcode !== "undefined") {
    Html5Qrcode.getCameras()
      .then(devices => {
        if (devices && devices.length > 0) {
          cameraSelectorContainer.classList.remove("hidden");
          cameraSelect.innerHTML = "";
          devices.forEach((device, index) => {
            const option = document.createElement("option");
            option.value = device.id;
            // Provide descriptive label or generate fallback fallback
            option.text = device.label || `Camera ${index + 1}`;
            cameraSelect.appendChild(option);
          });
          // Auto select back camera if named appropriately
          const backCam = devices.find(d => d.label.toLowerCase().includes("back") || d.label.toLowerCase().includes("environment"));
          if (backCam) {
            cameraSelect.value = backCam.id;
          }
        }
      })
      .catch(err => {
        console.warn("Unable to enumerate native cameras:", err);
      });
  }

  // Update visual status badge helper
  function updateStatus(text, dotColorClass) {
    badgeText.textContent = text;
    badgeDot.className = `w-2 h-2 rounded-full ${dotColorClass}`;
  }

  // Play premium audio feedback bip on scan
  function playBeepSound() {
    try {
      const audioCtx = new (window.AudioContext || window.webkitAudioContext)();
      if (!audioCtx) return;
      const oscillator = audioCtx.createOscillator();
      const gainNode = audioCtx.createGain();
      oscillator.connect(gainNode);
      gainNode.connect(audioCtx.destination);
      oscillator.type = "sine";
      oscillator.frequency.value = 880; // High frequency brief pip
      gainNode.gain.setValueAtTime(0.08, audioCtx.currentTime);
      oscillator.start();
      oscillator.stop(audioCtx.currentTime + 0.12);
    } catch (e) {
      console.warn("Audio Context beep denied/unavailable:", e);
    }
  }

  // Generate deterministic product prices to avoid raw random fluctuations (scanning same item shows same price)
  function calculateProductPrice(barcode) {
    if (!barcode) return "$2.99";
    // Basic hash calculations
    let sum = 0;
    for (let i = 0; i < barcode.length; i++) {
      sum += barcode.charCodeAt(i) * (i + 1);
    }
    const seed = (sum % 15) + 1.0;
    const cents = (sum % 99);
    // Format elegantly based on seed values
    const currency = barcode.startsWith("3") || barcode.startsWith("50") ? "€" : "$";
    return `${currency}${seed}.${cents.toString().padStart(2, "0")}`;
  }

  // Convert Nutri-Score letters into matching color scheme badges
  function styleNutriScore(scoreLetter) {
    const letter = (scoreLetter || "").toLowerCase().trim();
    prodNutriScoreBadge.className = "w-7 h-7 rounded-lg flex items-center justify-center font-black text-sm text-white";
    
    if (letter === "a") {
      prodNutriScoreBadge.classList.add("bg-emerald-600");
      prodNutriScoreBadge.textContent = "A";
      prodNutriScoreLabel.textContent = "Excellent nutrition profile";
    } else if (letter === "b") {
      prodNutriScoreBadge.classList.add("bg-green-500");
      prodNutriScoreBadge.textContent = "B";
      prodNutriScoreLabel.textContent = "Good nutrition profile";
    } else if (letter === "c") {
      prodNutriScoreBadge.classList.add("bg-amber-500");
      prodNutriScoreBadge.textContent = "C";
      prodNutriScoreLabel.textContent = "Moderate nutrition profile";
    } else if (letter === "d") {
      prodNutriScoreBadge.classList.add("bg-orange-500");
      prodNutriScoreBadge.textContent = "D";
      prodNutriScoreLabel.textContent = "Lower nutrition quality";
    } else if (letter === "e") {
      prodNutriScoreBadge.classList.add("bg-rose-600");
      prodNutriScoreBadge.textContent = "E";
      prodNutriScoreLabel.textContent = "Poor nutrition details";
    } else {
      prodNutriScoreBadge.classList.add("bg-slate-700");
      prodNutriScoreBadge.textContent = "?";
      prodNutriScoreLabel.textContent = "No nutrition score available";
    }
  }

  // Fetch Open Food Facts API Handler
  async function handleFetchedProduct(barcode) {
    if (!barcode) return;
    
    // Switch to status states
    resultsIdle.classList.add("hidden");
    resultsData.classList.add("hidden");
    resultsError.classList.add("hidden");
    resultsLoading.classList.remove("hidden");
    
    // Smooth scroll the viewport screen down to the results panel if necessary
    resultsCard.scrollIntoView({ behavior: 'smooth', block: 'end' });

    try {
      const response = await fetch(`https://world.openfoodfacts.org/api/v0/product/${barcode}.json`);
      if (!response.ok) {
        throw new Error(`HTTP network response failed: ${response.status}`);
      }
      const data = await response.json();
      
      resultsLoading.classList.add("hidden");
      resultsTimestamp.textContent = new Date().toLocaleTimeString();

      if (data && data.status === 1 && data.product) {
        const product = data.product;
        
        // Product Metadata Extraction
        prodBrand.textContent = (product.brands || "General Brands").toUpperCase();
        prodName.textContent = product.product_name || product.product_name_en || "Unnamed Product";
        prodBarcode.textContent = `UPC/EAN: ${barcode}`;
        
        // Deterministic price generator
        prodPrice.textContent = calculateProductPrice(barcode);
        
        // Nutri-Score calculation
        styleNutriScore(product.nutriscore_grade);
        
        // Category taxonomy
        prodCategories.textContent = product.categories || "Grocery, Miscellaneous";
        
        // Load Image
        const imageSrc = product.image_url || product.image_front_url || product.image_small_url || "";
        if (imageSrc) {
          prodImg.src = imageSrc;
          prodImg.classList.remove("hidden");
          prodImgPlaceholder.classList.add("hidden");
        } else {
          prodImg.classList.add("hidden");
          prodImgPlaceholder.classList.remove("hidden");
        }
        
        // Display info
        resultsData.classList.remove("hidden");
        resultsCard.classList.add("border-emerald-500/20", "bg-slate-900/40");
      } else {
        // Product Not Found
        resultsErrorMsg.innerHTML = `Could not locate product details for code: <strong class="font-mono text-rose-400 block mt-1">${barcode}</strong> Please make sure the code represents standard valid food and snack barcodes.`;
        resultsError.classList.remove("hidden");
        resultsCard.classList.remove("border-emerald-500/20", "bg-slate-900/40");
      }
    } catch (err) {
      console.error("Open Food Facts fetch failure:", err);
      resultsLoading.classList.add("hidden");
      resultsErrorMsg.textContent = `Server response error: ${err.message}. Please check your internet connection status.`;
      resultsError.classList.remove("hidden");
      resultsCard.classList.remove("border-emerald-500/20");
    }
  }

  // Successful Barcode Scan Event
  function onScanSuccess(decodedText, decodedResult) {
    // Avoid double/triple trigger loops for identical scan back-to-back within short period
    if (decodedText === lastScannedBarcode && Date.now() - handleSuccessCooldown < 3000) {
      return;
    }
    
    handleSuccessCooldown = Date.now();
    lastScannedBarcode = decodedText;
    
    playBeepSound();
    
    // Auto-stop scanning to conserve battery and allow viewer focus
    stopScanningAction();
    
    // Begin API Lookup
    handleFetchedProduct(decodedText);
  }
  
  let handleSuccessCooldown = 0;

  // Unhandled background scanning failures (noisy default, can safely ignore)
  function onScanFailure(error) {
    // We suppress console spamting for frame analysis noise since that's expected behavior in camera loops
  }

  // Core Function: Start scanning stream
  async function startScanningAction() {
    if (!html5QrCode) return;
    
    // Smooth transition
    sensorPlaceholder.classList.add("hidden");
    viewfinderOverlay.classList.remove("hidden");
    
    btnScanStart.classList.add("hidden");
    btnScanStop.classList.remove("hidden");
    
    const config = {
      fps: 15,
      qrbox: (videoWidth, videoHeight) => {
        const size = Math.floor(Math.min(videoWidth, videoHeight) * 0.70);
        return { width: size, height: size };
      }
    };

    // Pick camera device target
    let cameraTarget = { facingMode: "environment" };
    if (cameraSelect.value) {
      cameraTarget = { deviceId: { exact: cameraSelect.value } };
    }

    try {
      await html5QrCode.start(
        cameraTarget,
        config,
        onScanSuccess,
        onScanFailure
      );
      isScanning = true;
      updateStatus("Scanning", "bg-emerald-400 shadow-[0_0_8px_#34d399]");
    } catch (e) {
      console.error("Error starting camera stream:", e);
      alert("Camera Access Error: Please ensure you grant camera permissions, close other camera-active apps, and select a valid lens.");
      stopScanningAction();
    }
  }

  // Core Function: Stop scanning stream
  async function stopScanningAction() {
    if (!html5QrCode || !isScanning) {
      // Just fallback visual cleanups
      btnScanStart.classList.remove("hidden");
      btnScanStop.classList.add("hidden");
      sensorPlaceholder.classList.remove("hidden");
      viewfinderOverlay.classList.add("hidden");
      isScanning = false;
      updateStatus("Ready", "bg-slate-500");
      return;
    }

    try {
      await html5QrCode.stop();
    } catch (err) {
      console.warn("Failed to stop scan target cleanly:", err);
    }
    
    isScanning = false;
    btnScanStart.classList.remove("hidden");
    btnScanStop.classList.add("hidden");
    sensorPlaceholder.classList.remove("hidden");
    viewfinderOverlay.classList.add("hidden");
    updateStatus("Ready", "bg-slate-500");
  }

  // Click Action Listeners
  btnScanStart.addEventListener("click", () => {
    startScanningAction();
  });

  btnScanStop.addEventListener("click", () => {
    stopScanningAction();
  });
  
  // Clean restart if user switches camera device during active scan
  cameraSelect.addEventListener("change", () => {
    if (isScanning) {
      stopScanningAction().then(() => {
        startScanningAction();
      });
    }
  });

});
