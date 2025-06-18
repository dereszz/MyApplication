package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File
import android.util.Log

// 資料類別，代表一個地點資訊
data class MapLocation(
    val title: String,
    val lat: Double,
    val lon: Double
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        FirebaseApp.initializeApp(this)
        val db = FirebaseFirestore.getInstance()

        Log.d("MainActivity", "Firebase initialized: ${FirebaseApp.getApps(this).isNotEmpty()}")

        // 初始化 osmdroid 設定（避免存取權限問題，使用快取資料夾）
        Configuration.getInstance().userAgentValue = packageName
        Configuration.getInstance().osmdroidBasePath = File(cacheDir.absolutePath)
        Configuration.getInstance().osmdroidTileCache = File(cacheDir.absolutePath, "tile")

        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                var locations by remember { mutableStateOf(listOf<MapLocation>()) }

                // 從 Firestore 載入資料
                LaunchedEffect(Unit) {
                    db.collection("locations").get()
                        .addOnSuccessListener { result ->
                            val fetched = result.mapNotNull { doc ->
                                val lat = doc.getDouble("lat")
                                val lon = doc.getDouble("lon")
                                val title = doc.getString("title")
                                if (lat != null && lon != null && title != null) {
                                    MapLocation(title, lat, lon)
                                } else null
                            }
                            locations = fetched
                        }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    OsmDroidMapView(
                        modifier = Modifier.padding(innerPadding),
                        locations = locations
                    )
                }
            }
        }
    }
}

@Composable
fun OsmDroidMapView(
    modifier: Modifier = Modifier,
    locations: List<MapLocation>
) {
    AndroidView(
        factory = { context ->
            MapView(context).apply {
                setMultiTouchControls(true)
                controller.setZoom(15.0)
                controller.setCenter(GeoPoint(25.033964, 121.564468)) // 預設台北101
            }
        },
        update = { mapView ->
            // 每次更新先清除舊的 marker
            mapView.overlays.clear()

            // 重新加入所有 marker
            locations.forEach { loc ->
                val marker = Marker(mapView).apply {
                    position = GeoPoint(loc.lat, loc.lon)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = loc.title
                }
                mapView.overlays.add(marker)
            }

            // 刷新地圖畫面
            mapView.invalidate()
        },
        modifier = modifier
    )
}
