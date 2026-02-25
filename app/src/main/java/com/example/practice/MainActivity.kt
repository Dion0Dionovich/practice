package com.example.practice

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.practice.ui.theme.PracticeTheme
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Configuration.getInstance().load(
            applicationContext,
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(applicationContext)
        )

        setContent {
            PracticeTheme {
                var isPanelVisible by remember { mutableStateOf(false) }
                val centerMap = remember { mutableStateOf(false) }

                // Состояния для данных заказа
                var addressFrom by remember { mutableStateOf("") }
                var addressTo by remember { mutableStateOf("") }
                var selectedTariff by remember { mutableStateOf("Эконом") }

                Surface(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        OmskMap(centerMap = centerMap)
                        HeaderPanel(centerMap)

                        AnimatedVisibility(
                            visible = isPanelVisible,
                            enter = slideInVertically(initialOffsetY = { it }),
                            exit = slideOutVertically(targetOffsetY = { it }),
                            modifier = Modifier.align(Alignment.BottomCenter)
                        ) {
                            OrderPanel(
                                addressFrom = addressFrom,
                                onFromChange = { addressFrom = it },
                                addressTo = addressTo,
                                onToChange = { addressTo = it },
                                selectedTariff = selectedTariff,
                                onTariffSelect = { selectedTariff = it },
                                onOrderClick = {
                                    processOrder(addressFrom, addressTo, selectedTariff)
                                }
                            )
                        }

                        FloatingActionButton(
                            onClick = { isPanelVisible = !isPanelVisible },
                            shape = CircleShape,
                            containerColor = Color.White,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                                .offset(y = if (isPanelVisible) (-360).dp else 0.dp)
                        ) {
                            Icon(
                                imageVector = if (isPanelVisible) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                contentDescription = null
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun HeaderPanel(centerMap: MutableState<Boolean>) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(Icons.Default.Menu, contentDescription = "Menu", modifier = Modifier.size(32.dp))
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            ) {
                Text(
                    text = "Здравствуйте, Павел.",
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                    fontSize = 16.sp, fontWeight = FontWeight.Medium
                )
            }
            IconButton(onClick = { centerMap.value = true }) {
                Icon(Icons.Default.LocationOn, contentDescription = "Location", modifier = Modifier.size(32.dp))
            }
        }
    }

    // Метод обработки данных (Валидация + Вывод)
    private fun processOrder(from: String, to: String, tariff: String) {
        if (from.isBlank() || to.isBlank()) {
            Toast.makeText(this, "Пожалуйста, заполните все адреса!", Toast.LENGTH_SHORT).show()
        } else if (from.length < 3 || to.length < 3) {
            Toast.makeText(this, "Адрес слишком короткий", Toast.LENGTH_SHORT).show()
        } else {
            // Вывод итоговых данных
            val message = "Заказ принят!\nОткуда: $from\nКуда: $to\nТариф: $tariff"
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    @Composable
    fun OrderPanel(
        addressFrom: String, onFromChange: (String) -> Unit,
        addressTo: String, onToChange: (String) -> Unit,
        selectedTariff: String, onTariffSelect: (String) -> Unit,
        onOrderClick: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().height(360.dp),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Выбор тарифа с обводкой
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TariffCard("Эконом", "300р.", selectedTariff == "Эконом") { onTariffSelect("Эконом") }
                    TariffCard("Комфорт", "450р.", selectedTariff == "Комфорт") { onTariffSelect("Комфорт") }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Поля ввода адреса
                OutlinedTextField(
                    value = addressFrom,
                    onValueChange = onFromChange,
                    label = { Text("Откуда") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = addressTo,
                    onValueChange = onToChange,
                    label = { Text("Куда") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = onOrderClick,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text("Заказать", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    @Composable
    fun TariffCard(name: String, price: String, isSelected: Boolean, onClick: () -> Unit) {
        Card(
            modifier = Modifier
                .size(width = 140.dp, height = 90.dp)
                .clickable { onClick() }
                .border(
                    width = if (isSelected) 3.dp else 0.dp,
                    color = if (isSelected) Color.Black else Color.Transparent,
                    shape = RoundedCornerShape(12.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFD700)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(name, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                Text(price, color = Color.Black.copy(alpha = 0.7f))
            }
        }
    }

    @Composable
    fun OmskMap(centerMap: MutableState<Boolean>) {
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
                controller.setZoom(16.0)
                controller.setCenter(omskCenter)
                mapViewRef.value = this
            }},
            update = { mapViewRef.value = it }
        )
    }
}
