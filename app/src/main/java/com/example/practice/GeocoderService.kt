package com.example.practice

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit


// Класс для работы с адресами с помощью Nominatim API
class GeocoderService {
    // Создания клиент соединения
    private val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()


    // Поиск адресов по запросу. Принимает query (поисковый запрос)
    // и возращает список предложений адресов
    suspend fun searchAddress(query: String): List<AddressSuggestion> {
        // Проверка длины адреса. Если меньше 3 символов, то вернуть пустой список
        if (query.length < 3) return emptyList()

        return withContext(Dispatchers.IO) {
            try {
                // Ограничивание поиска городом Омском
                val url = "https://nominatim.openstreetmap.org/search?q=$query," +
                        " Омск&format=json&limit=10&addressdetails=1&countrycodes=ru&bounded=" +
                        "1&viewbox=73.12,55.08,73.68,54.87"

                // Создается ответ
                val request = Request.Builder()
                        .url(url)
                        .header("User-Agent", "TaxiApp/1.0")
                        .header("Accept-Language", "ru")
                        .build()

                // Формируется список json формата
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: return@withContext emptyList()
                val json = JSONArray(body)

                // Парсинг результата с помощью списка json
                val results = parseAddresses(json)
                results.distinctBy { it.shortName }

                // При любой ошибки возращается пустой список
            } catch (_: Exception) {
                emptyList()
            }
        }
    }


    // Парсинг список json от Nominatim API
    private fun parseAddresses(json: JSONArray): MutableList<AddressSuggestion> {
        val results = mutableListOf<AddressSuggestion>()

        // Проход по списку
        for (i in 0 until json.length()) {
            // Получение адреса
            val item = json.getJSONObject(i)
            val displayName = item.getString("display_name")

            // Проверка существования адреса в городе Омске
            if (displayName.contains("Омск", ignoreCase = true) ||
                    displayName.contains("Omsk", ignoreCase = true)) {
                // Если есть, то добавляется в результат
                val shortName = extractShortName(item)
                results.add(AddressSuggestion(displayName, shortName))
            }
        }

        // Если список пуст, то берутся первые результаты
        if (results.isEmpty()) {
            for (i in 0 until json.length()) {
                val item = json.getJSONObject(i)
                val displayName = item.getString("display_name")
                val shortName = displayName.split(",")[0].trim()
                results.add(AddressSuggestion(displayName, shortName))
            }
        }

        return results  // Возращает результат
    }


    // Извлекает короткое название адреса из json
    private fun extractShortName(item: JSONObject): String {
        val address = item.optJSONObject("address")

        return if (address != null) {
            val road = address.optString("road", "")
            val house = address.optString("house_number", "")
            val suburb = address.optString("suburb", "")

            when {
                // Если есть и улица и номер дома
                road.isNotEmpty() && house.isNotEmpty() -> "$road, $house"
                // Если есть только улица
                road.isNotEmpty() -> road
                // Если есть только район
                suburb.isNotEmpty() -> suburb
                // В крайнем случае берется первая часть полного названия
                else -> item.getString("display_name").split(",")[0].trim()
            }
        } else {
            // Если нет детальной информации об адресе
            item.getString("display_name").split(",")[0].trim()
        }
    }

    suspend fun searchAddressFromApi(query: String): List<AddressSuggestion> {
        return searchAddress(query)
    }
}
