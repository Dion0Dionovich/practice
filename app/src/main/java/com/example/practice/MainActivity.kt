package com.example.practice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
            androidx.preference.PreferenceManager.
            getDefaultSharedPreferences(applicationContext)
        )

        setContent {
            PracticeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize().systemBarsPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Флаг для управления возвратом в центр города
                    val centerMap = remember { mutableStateOf(false) }

                    Box(modifier = Modifier.fillMaxSize()) {
                        // Передача состояния в карту
                        OmskMap(centerMap = centerMap)

                        // Горизонтальный слой панели управления
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Иконка меню
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                modifier = Modifier.size(32.dp)
                            )
                            // Текст приветствие
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                modifier = Modifier.padding(horizontal = 12.dp),
                            ) {
                                Text(
                                    text = "Здравствуйте, Пользователь.",
                                    modifier = Modifier.padding(
                                        vertical = 8.dp,
                                        horizontal = 12.dp
                                    ),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Black
                                )
                            }

                            // Навигация
                            IconButton(
                                onClick = {
                                    // Поднятие флага для перемещение в центр города
                                    centerMap.value = true
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Location",
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun OmskMap(centerMap: MutableState<Boolean>) {
        // Гео-позиция города Омска
        val omskCenter = remember { GeoPoint(54.985, 73.370) }

        // Границы перемещения видемой области в пределах города
        val omskBoundingBox = remember {
            BoundingBox(
                55.08,
                73.68,
                54.87,
                73.12
            )
        }

        // Ссылка на MapView
        val mapViewRef = remember { mutableStateOf<MapView?>(null) }

        // Эффект для обработки возврата к центру
        LaunchedEffect(centerMap.value) {
            if (centerMap.value) {
                mapViewRef.value?.let { mapView ->
                    // Анимированный возврат к центру
                    mapView.controller.animateTo(
                        omskCenter,
                        mapView.zoomLevelDouble,
                        500L  // Длительность анимации 0,5 сек
                    )
                }
                // Сброс флага отвечающий за перемещение видемой области в центр города
                centerMap.value = false
            }
        }

        AndroidView(
            factory = { ctx -> MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                setScrollableAreaLimitDouble(omskBoundingBox)

                minZoomLevel = 12.0  // Минимальное значение зума
                maxZoomLevel = 18.0  // Максимальное значение зума

                controller.setZoom(16.0)  // Установка значения зума
                controller.setCenter(omskCenter)  // Переход к городу Омск

                // Сохранение ссылки на MapView
                mapViewRef.value = this
            }},
            update = { mapView ->
                // Обновление ссылки если нужно
                mapViewRef.value = mapView
            }
        )
    }
}