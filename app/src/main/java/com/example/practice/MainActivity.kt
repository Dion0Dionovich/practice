package com.example.practice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.preference.PreferenceManager
import com.example.practice.ui.theme.PracticeTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.ConnectException
import java.net.SocketTimeoutException
import kotlin.random.Random


class MainActivity : ComponentActivity() {
    private val driversList = listOf(
        Driver("Александр Петров", "Toyota Camry", "Серебристый", 4.8, 1243, "+7 (999) 123-45-67", Color(0xFF808080)),
        Driver("Дмитрий Соколов", "Hyundai Solaris", "Белый", 4.9, 2356, "+7 (999) 234-56-78", Color.White),
        Driver("Иван Морозов", "Kia Rio", "Синий", 4.7, 892, "+7 (999) 345-67-89", Color.Blue),
        Driver("Сергей Волков", "Volkswagen Polo", "Черный", 4.85, 1567, "+7 (999) 456-78-90", Color.Black),
        Driver("Андрей Козлов", "Skoda Octavia", "Серый", 4.75, 1102, "+7 (999) 567-89-01", Color.DarkGray),
        Driver("Максим Новиков", "Ford Focus", "Красный", 4.95, 3105, "+7 (999) 678-90-12", Color.Red)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        Configuration.getInstance().load(
            applicationContext,
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
        )

        setContent {
            PracticeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFFFD966)
                ) {
                    TaxiApp(driversList = driversList)
                }
            }
        }
    }

    @Composable
    fun TaxiApp(driversList: List<Driver>) {
        var showPanel by remember { mutableStateOf(false) }
        var showDriver by remember { mutableStateOf(false) }
        var showSearching by remember { mutableStateOf(false) }
        val centerMap = remember { mutableStateOf(false) }

        var apiAvailable by remember { mutableStateOf(true) }
        var loadingData by remember { mutableStateOf(false) }

        var showErrorDialog by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf("") }

        var tariffs by remember { mutableStateOf<List<ТарифDto>>(emptyList()) }
        var cars by remember { mutableStateOf<List<АвтомобильDto>>(emptyList()) }
        var services by remember { mutableStateOf<List<УслугаDto>>(emptyList()) }
        var discounts by remember { mutableStateOf<List<СкидкаDto>>(emptyList()) }

        var fromAddress by remember { mutableStateOf("") }
        var toAddress by remember { mutableStateOf("") }
        var tariff by remember { mutableStateOf("Эконом") }

        var fromSuggestions by remember { mutableStateOf<List<AddressSuggestion>>(emptyList()) }
        var toSuggestions by remember { mutableStateOf<List<AddressSuggestion>>(emptyList()) }
        var isSearchingFrom by remember { mutableStateOf(false) }
        var isSearchingTo by remember { mutableStateOf(false) }

        var fromFocused by remember { mutableStateOf(false) }
        var toFocused by remember { mutableStateOf(false) }

        var selectedDriver by remember { mutableStateOf<Driver?>(null) }
        var tripInfo by remember { mutableStateOf<TripInfo?>(null) }

        var showMessageDialog by remember { mutableStateOf(false) }
        var dialogTitle by remember { mutableStateOf("") }
        var dialogMessage by remember { mutableStateOf("") }

        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        val geocoder = remember { GeocoderService() }

        LaunchedEffect(Unit) {
            loadingData = true
            try {
                val unsafeOkHttpClient = UnsafeOkHttpClient.getUnsafeOkHttpClient()

                val retrofit = Retrofit.Builder()
                    .baseUrl("http://10.0.2.2:5053/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(unsafeOkHttpClient)
                    .build()

                val api = retrofit.create(TaxiApiService::class.java)

                val result = withTimeoutOrNull(5000L) {
                    try {
                        tariffs = api.getТарифы()
                        cars = api.getАвтомобили()
                        services = api.getУслуги()
                        discounts = api.getСкидки()
                        apiAvailable = true
                        dialogTitle = "Успех"
                        dialogMessage = "API подключен успешно"
                        showMessageDialog = true
                    } catch (e: ConnectException) {
                        apiAvailable = false
                        errorMessage = "Ошибка подключения: ${e.message}"
                        showErrorDialog = true
                        e.printStackTrace()
                    } catch (e: SocketTimeoutException) {
                        apiAvailable = false
                        errorMessage = "Таймаут: ${e.message}"
                        showErrorDialog = true
                        e.printStackTrace()
                    } catch (e: Exception) {
                        apiAvailable = false
                        errorMessage = "Ошибка: ${e.message}"
                        showErrorDialog = true
                        e.printStackTrace()
                    }
                }

                if (result == null) {
                    apiAvailable = false
                    errorMessage = "Таймаут 5 секунд"
                    showErrorDialog = true
                }
            } catch (e: Exception) {
                apiAvailable = false
                errorMessage = "Ошибка создания клиента: ${e.message}"
                showErrorDialog = true
                e.printStackTrace()
            } finally {
                loadingData = false
            }
        }

        LaunchedEffect(fromAddress) {
            if (fromAddress.length >= 3 && fromFocused) {
                isSearchingFrom = true
                delay(500)
                fromSuggestions = if (apiAvailable) {
                    geocoder.searchAddressFromApi(fromAddress)
                } else {
                    geocoder.searchAddress(fromAddress)
                }
                isSearchingFrom = false
            } else {
                fromSuggestions = emptyList()
                isSearchingFrom = false
            }
        }

        LaunchedEffect(toAddress) {
            if (toAddress.length >= 3 && toFocused) {
                isSearchingTo = true
                delay(500)
                toSuggestions = if (apiAvailable) {
                    geocoder.searchAddressFromApi(toAddress)
                } else {
                    geocoder.searchAddress(toAddress)
                }
                isSearchingTo = false
            } else {
                toSuggestions = emptyList()
                isSearchingTo = false
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            MapView(centerMap = centerMap)

            TopBar(centerMap = centerMap, apiAvailable = apiAvailable)

            AnimatedVisibility(
                visible = showPanel && !showDriver && !showSearching && !loadingData,
                enter = slideInVertically(),
                exit = slideOutVertically(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                OrderPanel(
                    fromAddress = fromAddress,
                    onFromChange = { fromAddress = it },
                    toAddress = toAddress,
                    onToChange = { toAddress = it },
                    tariff = tariff,
                    onTariffSelect = { tariff = it },
                    fromSuggestions = fromSuggestions,
                    toSuggestions = toSuggestions,
                    isSearchingFrom = isSearchingFrom,
                    isSearchingTo = isSearchingTo,
                    fromFocused = fromFocused,
                    toFocused = toFocused,
                    onFromFocusChange = { fromFocused = it },
                    onToFocusChange = { toFocused = it },
                    onFromSuggestionClick = { suggestion ->
                        fromAddress = suggestion.shortName
                        fromSuggestions = emptyList()
                        fromFocused = false
                    },
                    onToSuggestionClick = { suggestion ->
                        toAddress = suggestion.shortName
                        toSuggestions = emptyList()
                        toFocused = false
                    },
                    onOrderClick = {
                        if (fromAddress.isNotBlank() && toAddress.isNotBlank()) {
                            showPanel = false
                            showSearching = true

                            val distance = Random.nextDouble(3.5, 15.0)
                            val basePrice = when (tariff) {
                                "Эконом" -> 300
                                "Комфорт" -> 450
                                else -> 300
                            }
                            val pricePerKm = when (tariff) {
                                "Эконом" -> 25
                                "Комфорт" -> 35
                                else -> 25
                            }
                            val finalPrice = basePrice + (distance * pricePerKm).toInt()
                            val duration = (distance * 3).toInt() + Random.nextInt(5, 15)

                            tripInfo = TripInfo(
                                fromAddress = fromAddress,
                                toAddress = toAddress,
                                distance = distance,
                                duration = duration,
                                basePrice = basePrice,
                                finalPrice = finalPrice,
                                tariff = tariff
                            )

                            scope.launch {
                                val searchTime = Random.nextLong(5000, 10000)
                                delay(searchTime)

                                selectedDriver = driversList.random()
                                showSearching = false
                                showDriver = true
//                                dialogTitle = "Успех"
//                                dialogMessage = "Водитель найден!"
//                                showMessageDialog = true
                            }

//                            dialogTitle = "Информация"
//                            dialogMessage = "Ищем водителя..."
//                            showMessageDialog = true
                        } else {
//                            dialogTitle = "Ошибка"
//                            dialogMessage = "Заполните адреса"
//                            showMessageDialog = true
                        }
                    },
                    tariffs = tariffs,
                    apiAvailable = apiAvailable
                )
            }

            AnimatedVisibility(
                visible = showSearching,
                enter = slideInVertically(),
                exit = slideOutVertically(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                SearchingDriverPanel(
                    fromAddress = fromAddress,
                    toAddress = toAddress,
                    onCancel = {
                        showSearching = false
                        showPanel = true
                    }
                )
            }

            AnimatedVisibility(
                visible = showDriver,
                enter = slideInVertically(),
                exit = slideOutVertically(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                if (selectedDriver != null && tripInfo != null) {
                    DriverPanel(
                        driver = selectedDriver!!,
                        tripInfo = tripInfo!!,
                        onCancel = {
                            showDriver = false
                            showPanel = true
                            selectedDriver = null
                            tripInfo = null
                        }
                    )
                }
            }

            if (loadingData) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFFFB800))
                }
            }

            MainButton(
                showPanel = showPanel,
                showDriver = showDriver,
                showSearching = showSearching,
                onClick = {
                    when {
                        showDriver || showSearching -> {
                            showDriver = false
                            showSearching = false
                            showPanel = true
                            selectedDriver = null
                            tripInfo = null
                        }
                        else -> showPanel = !showPanel
                    }
                }
            )

            if (showErrorDialog) {
                AlertDialog(
                    onDismissRequest = { showErrorDialog = false },
                    title = { Text("Ошибка") },
                    text = { Text(errorMessage) },
                    confirmButton = {
                        TextButton(onClick = { showErrorDialog = false }) {
                            Text("OK")
                        }
                    }
                )
            }

            if (showMessageDialog) {
                AlertDialog(
                    onDismissRequest = { showMessageDialog = false },
                    title = { Text(dialogTitle) },
                    text = { Text(dialogMessage) },
                    confirmButton = {
                        TextButton(onClick = { showMessageDialog = false }) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }

    @Composable
    fun TopBar(centerMap: MutableState<Boolean>, apiAvailable: Boolean) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .statusBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { }) {
                Icon(Icons.Default.Menu, contentDescription = "Меню", tint = Color.Black)
            }

            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Text(
                    "Здравствуйте, Павел.",
                    modifier = Modifier.padding(8.dp),
                    color = if (apiAvailable) Color.Black else Color.Gray
                )
            }

            IconButton(onClick = { centerMap.value = true }) {
                Icon(Icons.Default.MyLocation, contentDescription = "Центрировать", tint = Color.Black)
            }
        }
    }

    @Composable
    fun OrderPanel(
        fromAddress: String,
        onFromChange: (String) -> Unit,
        toAddress: String,
        onToChange: (String) -> Unit,
        tariff: String,
        onTariffSelect: (String) -> Unit,
        fromSuggestions: List<AddressSuggestion>,
        toSuggestions: List<AddressSuggestion>,
        isSearchingFrom: Boolean,
        isSearchingTo: Boolean,
        fromFocused: Boolean,
        toFocused: Boolean,
        onFromFocusChange: (Boolean) -> Unit,
        onToFocusChange: (Boolean) -> Unit,
        onFromSuggestionClick: (AddressSuggestion) -> Unit,
        onToSuggestionClick: (AddressSuggestion) -> Unit,
        onOrderClick: () -> Unit,
        tariffs: List<ТарифDto>,
        apiAvailable: Boolean
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .navigationBarsPadding(),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                TariffSelector(
                    selected = tariff,
                    onSelect = onTariffSelect,
                    tariffs = tariffs,
                    apiAvailable = apiAvailable
                )

                Spacer(modifier = Modifier.height(12.dp))

                AddressInputField(
                    value = fromAddress,
                    onValueChange = onFromChange,
                    onFocusChange = onFromFocusChange,
                    label = "Откуда",
                    placeholder = "ул. Ленина, 1",
                    leadingIcon = Icons.Default.LocationOn,
                    iconTint = Color.Green,
                    suggestions = fromSuggestions,
                    showSuggestions = fromFocused,
                    isSearching = isSearchingFrom,
                    onSuggestionClick = onFromSuggestionClick
                )

                Spacer(modifier = Modifier.height(8.dp))

                AddressInputField(
                    value = toAddress,
                    onValueChange = onToChange,
                    onFocusChange = onToFocusChange,
                    label = "Куда",
                    placeholder = "ул. Красный Путь, 1",
                    leadingIcon = Icons.Default.LocationOn,
                    iconTint = Color.Red,
                    suggestions = toSuggestions,
                    showSuggestions = toFocused,
                    isSearching = isSearchingTo,
                    onSuggestionClick = onToSuggestionClick
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onOrderClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB800))
                ) {
                    Text("Заказать", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                if (!apiAvailable) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Офлайн режим",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }

    @Composable
    fun SearchingDriverPanel(
        fromAddress: String,
        toAddress: String,
        onCancel: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFF0CC)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(60.dp),
                        strokeWidth = 4.dp,
                        color = Color(0xFFFFB800)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Поиск водителя...",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Ищем ближайшего водителя",
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFF5F5F5),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.Green)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = fromAddress.ifEmpty { "Не указан" },
                                fontSize = 14.sp,
                                maxLines = 1
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = toAddress.ifEmpty { "Не указан" },
                                fontSize = 14.sp,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB800)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Отменить поиск", color = Color.Black)
                }
            }
        }
    }

    @Composable
    fun TariffSelector(
        selected: String,
        onSelect: (String) -> Unit,
        tariffs: List<ТарифDto>,
        apiAvailable: Boolean
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            if (apiAvailable && tariffs.isNotEmpty()) {
                tariffs.forEach { tariff ->
                    TariffCard(
                        name = tariff.название,
                        price = "от ${tariff.цена}₽",
                        isSelected = selected == tariff.название,
                        onClick = { onSelect(tariff.название) }
                    )
                }
            } else {
                TariffCard(
                    name = "Эконом",
                    price = "от 300₽",
                    isSelected = selected == "Эконом",
                    onClick = { onSelect("Эконом") }
                )
                TariffCard(
                    name = "Комфорт",
                    price = "от 450₽",
                    isSelected = selected == "Комфорт",
                    onClick = { onSelect("Комфорт") }
                )
            }
        }
    }

    @Composable
    fun TariffCard(
        name: String,
        price: String,
        isSelected: Boolean,
        onClick: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .size(width = 140.dp, height = 70.dp)
                .clickable { onClick() }
                .border(
                    width = if (isSelected) 2.dp else 0.dp,
                    color = if (isSelected) Color(0xFFFFB800) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                ),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) Color(0xFFFFF0CC) else Color(0xFFF5F5F5)
            )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(name, fontWeight = FontWeight.Bold, color = Color.Black)
                Text(price, color = Color.DarkGray, fontSize = 12.sp)
            }
        }
    }

    @Composable
    fun AddressInputField(
        value: String,
        onValueChange: (String) -> Unit,
        onFocusChange: (Boolean) -> Unit,
        label: String,
        placeholder: String,
        leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
        iconTint: Color,
        suggestions: List<AddressSuggestion>,
        showSuggestions: Boolean,
        isSearching: Boolean,
        onSuggestionClick: (AddressSuggestion) -> Unit
    ) {
        Column {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(label) },
                placeholder = { Text(placeholder) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { onFocusChange(it.isFocused) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFFB800),
                    unfocusedBorderColor = Color.Gray,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                ),
                leadingIcon = { Icon(leadingIcon, null, tint = iconTint) },
                trailingIcon = {
                    if (isSearching)
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFFFFB800)
                        )
                }
            )

            if (suggestions.isNotEmpty() && showSuggestions) {
                SuggestionsList(
                    suggestions = suggestions,
                    onSuggestionClick = onSuggestionClick
                )
            }
        }
    }

    @Composable
    fun SuggestionsList(
        suggestions: List<AddressSuggestion>,
        onSuggestionClick: (AddressSuggestion) -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(top = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            LazyColumn {
                items(suggestions) { suggestion ->
                    Text(
                        text = suggestion.shortName,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSuggestionClick(suggestion) }
                            .padding(12.dp),
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                    HorizontalDivider(color = Color.LightGray)
                }
            }
        }
    }

    @Composable
    fun DriverPanel(
        driver: Driver,
        tripInfo: TripInfo,
        onCancel: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Text(
                    text = "Водитель назначен!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFB800)
                )

                Text(
                    text = "Приедет через ${tripInfo.duration} минут",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFF5F5F5),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.Green)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = tripInfo.fromAddress,
                                fontSize = 14.sp,
                                maxLines = 1
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = tripInfo.toAddress,
                                fontSize = 14.sp,
                                maxLines = 1
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = Color.LightGray.copy(alpha = 0.3f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Расстояние: ${String.format("%.1f", tripInfo.distance)} км",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "В пути: ~${tripInfo.duration} мин",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                DriverInfo(driver = driver)

                Spacer(modifier = Modifier.height(8.dp))

                PaymentInfo(
                    tripInfo = tripInfo,
                    driver = driver
                )

                Spacer(modifier = Modifier.height(24.dp))

                ActionButtons(
                    driver = driver,
                    onCancel = onCancel
                )
            }
        }
    }

    @Composable
    fun DriverInfo(driver: Driver) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFF0CC)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color(0xFFFFB800)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = driver.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${driver.car} • ${driver.carColor}",
                    color = Color.Gray,
                    fontSize = 14.sp
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(5) { index ->
                        Icon(
                            imageVector = if (index < driver.rating.toInt())
                                Icons.Filled.Star
                            else
                                Icons.Outlined.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFB800),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${driver.rating} (${driver.tripsCount} поездок)",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            thickness = 1.dp,
            color = Color.LightGray.copy(alpha = 0.5f)
        )
    }

    @Composable
    fun PaymentInfo(
        tripInfo: TripInfo,
        driver: Driver
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Способ оплаты", color = Color.Gray)
            Text(
                text = "${tripInfo.finalPrice}₽",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFFFF1AD),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Подача", fontSize = 12.sp)
                    Text(text = "${tripInfo.basePrice}₽", fontSize = 12.sp)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "${String.format("%.1f", tripInfo.distance)} км", fontSize = 12.sp)
                    Text(text = "${tripInfo.finalPrice - tripInfo.basePrice}₽", fontSize = 12.sp)
                }
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = Color.LightGray.copy(alpha = 0.3f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Итого:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(text = "${tripInfo.finalPrice}₽", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFF5F5F5),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp, 24.dp)
                        .background(Color(0xFF2D3E33), RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = "**** **** **** 8907", fontSize = 12.sp)
                    Text(text = "expires 12/28", fontSize = 10.sp, color = Color.Gray)
                }
            }
        }
    }

    @Composable
    fun ActionButtons(
        driver: Driver,
        onCancel: () -> Unit
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "Позвонить",
                    tint = Color(0xFFFFC107),
                    modifier = Modifier
                        .size(32.dp)
                )
                Spacer(modifier = Modifier.width(24.dp))
                Icon(
                    imageVector = Icons.Outlined.Chat,
                    contentDescription = "Чат",
                    tint = Color(0xFFFFC107),
                    modifier = Modifier
                        .size(32.dp)
                )
            }

            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107)),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp)
            ) {
                Text(text = "Отменить", color = Color.White, fontSize = 16.sp)
            }
        }
    }

    @Composable
    fun BoxScope.MainButton(
        showPanel: Boolean,
        showDriver: Boolean,
        showSearching: Boolean,
        onClick: () -> Unit
    ) {
        FloatingActionButton(
            onClick = onClick,
            shape = CircleShape,
            containerColor = Color(0xFFFFB800),
            contentColor = Color.Black,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = when {
                    showDriver || showSearching -> Icons.Default.Close
                    showPanel -> Icons.Default.KeyboardArrowDown
                    else -> Icons.Default.LocalTaxi
                },
                contentDescription = null
            )
        }
    }

    @Composable
    fun MapView(centerMap: MutableState<Boolean>) {
        val omskCenter = remember { GeoPoint(54.985, 73.370) }
        val omskBoundingBox = remember { BoundingBox(55.08, 73.68, 54.87, 73.12) }
        val mapViewRef = remember { mutableStateOf<MapView?>(null) }

        LaunchedEffect(centerMap.value) {
            if (centerMap.value) {
                mapViewRef.value?.controller?.animateTo(omskCenter, 16.0, 500L)
                centerMap.value = false
            }
        }

        AndroidView(
            factory = { ctx -> MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                setScrollableAreaLimitDouble(omskBoundingBox)
                minZoomLevel = 14.0
                maxZoomLevel = 18.0
                controller.setZoom(16.0)
                controller.setCenter(omskCenter)
                mapViewRef.value = this
            }},
            update = { mapViewRef.value = it },
            modifier = Modifier.fillMaxSize()
        )
    }
}
