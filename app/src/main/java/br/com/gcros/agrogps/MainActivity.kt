package br.com.gcros.agrogps

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.os.Handler
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.round
import kotlin.math.sin

class MainActivity : Activity(), LocationListener {
    private lateinit var locationManager: LocationManager
    private lateinit var statusText: TextView
    private lateinit var coordsText: TextView
    private lateinit var accuracyText: TextView
    private lateinit var deviationText: TextView
    private lateinit var widthText: TextView
    private lateinit var speedText: TextView
    private lateinit var fieldView: FieldGuidanceView
    private var currentLocation: Location? = null
    private var startPoint: Location? = null
    private var endPoint: Location? = null
    private var implementWidth = 12.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(10, 20, 17)
        window.navigationBarColor = Color.rgb(10, 20, 17)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        showSplashScreen()
    }

    private fun showSplashScreen() {
        val splash = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(28), dp(20), dp(28), dp(20))
            setBackgroundColor(Color.rgb(9, 24, 17))
        }
        splash.addView(TextView(this).apply {
            text = "🌱"
            textSize = 48f
            gravity = Gravity.CENTER
        })
        splash.addView(TextView(this).apply {
            text = "AgroGPS"
            textSize = 42f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(146, 230, 151))
            typeface = Typeface.DEFAULT_BOLD
        })
        splash.addView(TextView(this).apply {
            text = "ORIENTAÇÃO AGRÍCOLA"
            textSize = 13f
            gravity = Gravity.CENTER
            letterSpacing = 0.18f
            setTextColor(Color.rgb(224, 239, 225))
        })
        splash.addView(View(this).apply {
            setBackgroundColor(Color.rgb(126, 220, 143))
            layoutParams = LinearLayout.LayoutParams(dp(88), dp(3)).apply {
                gravity = Gravity.CENTER
                topMargin = dp(22)
                bottomMargin = dp(18)
            }
        })
        splash.addView(TextView(this).apply {
            text = "LINHAS A-B  •  VELOCIDADE  •  DESVIO"
            textSize = 10f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(139, 174, 145))
        })
        splash.addView(TextView(this).apply {
            text = "Preparando seu campo..."
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(180, 199, 183))
            setPadding(0, dp(30), 0, 0)
        })
        setContentView(splash)
        Handler(Looper.getMainLooper()).postDelayed({
            if (!isFinishing) {
                buildScreen()
                requestGps()
            }
        }, 1600)
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(10, 18, 16))
            setPadding(dp(14), dp(10), dp(14), dp(10))
        }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val titleColumn = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titleColumn.addView(TextView(this).apply {
            text = "AgroGPS"; textSize = 25f; setTextColor(Color.rgb(227, 246, 231)); typeface = Typeface.DEFAULT_BOLD
        })
        titleColumn.addView(TextView(this).apply {
            text = "ORIENTAÇÃO AGRÍCOLA  •  GPS DO CELULAR"; textSize = 10f; setTextColor(Color.rgb(130, 165, 142))
        })
        header.addView(titleColumn, LinearLayout.LayoutParams(0, -2, 1f))
        statusText = TextView(this).apply {
            text = "● BUSCANDO GPS"; textSize = 12f; setTextColor(Color.rgb(255, 194, 76)); typeface = Typeface.DEFAULT_BOLD
        }
        header.addView(statusText)
        root.addView(header)
        root.addView(space(10))

        val body = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fieldView = FieldGuidanceView()
        body.addView(fieldView, LinearLayout.LayoutParams(0, -1, 1f))

        val panel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(4), dp(2), dp(4)) }
        body.addView(panel, LinearLayout.LayoutParams(dp(235), -1))
        panel.addView(label("POSIÇÃO GPS"))
        coordsText = value("Aguardando sinal")
        panel.addView(coordsText)
        panel.addView(space(8))
        panel.addView(label("PRECISÃO INDICADA"))
        accuracyText = value("-- m")
        panel.addView(accuracyText)
        panel.addView(space(8))
        panel.addView(label("DESVIO DA LINHA"))
        deviationText = value("-- m")
        panel.addView(deviationText)
        panel.addView(space(8))
        panel.addView(label("VELOCIDADE"))
        speedText = value("0,0 km/h")
        panel.addView(speedText)
        panel.addView(space(8))
        panel.addView(label("LARGURA DO IMPLEMENTO"))
        widthText = value("12,0 m")
        panel.addView(widthText)
        panel.addView(actionButton("−  Diminuir largura", true) {
            implementWidth = (implementWidth - 0.5).coerceAtLeast(1.0)
            updateWidth()
        })
        panel.addView(space(4))
        panel.addView(actionButton("+  Aumentar largura", true) {
            implementWidth = (implementWidth + 0.5).coerceAtMost(60.0)
            updateWidth()
        })
        panel.addView(space(10))
        panel.addView(label("CRIAR LINHA GUIA"))
        panel.addView(space(4))
        panel.addView(actionButton("1. MARCAR INÍCIO") {
            currentLocation?.let {
                startPoint = Location(it)
                endPoint = null
                fieldView.invalidate()
                Toast.makeText(this, "Início marcado. Conduza em linha reta e marque o fim.", Toast.LENGTH_LONG).show()
                updateDeviation()
            } ?: noGps()
        })
        panel.addView(space(4))
        panel.addView(actionButton("2. MARCAR FIM") {
            currentLocation?.let {
                if (startPoint == null) {
                    Toast.makeText(this, "Marque primeiro o início.", Toast.LENGTH_SHORT).show()
                } else if (distanceMeters(startPoint!!, it) < 3.0) {
                    Toast.makeText(this, "Avance pelo menos 3 metros antes de marcar o fim.", Toast.LENGTH_LONG).show()
                } else {
                    endPoint = Location(it)
                    fieldView.invalidate()
                    updateDeviation()
                    Toast.makeText(this, "Linha base criada!", Toast.LENGTH_SHORT).show()
                }
            } ?: noGps()
        })
        panel.addView(space(4))
        panel.addView(actionButton("APAGAR LINHA", true) {
            startPoint = null
            endPoint = null
            deviationText.text = "-- m"
            fieldView.invalidate()
        })
        panel.addView(space(8))
        panel.addView(TextView(this).apply {
            text = "Use em área aberta. As linhas são orientação aproximada, não substituem um sistema agrícola de precisão."
            textSize = 10f; setTextColor(Color.rgb(150, 173, 158))
        })
        root.addView(body, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun requestGps() {
        val fine = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 100)
            return
        }
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0.3f, this, Looper.getMainLooper())
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let { onLocationChanged(it) }
                statusText.text = "● GPS ATIVO"
                statusText.setTextColor(Color.rgb(112, 224, 139))
            } else {
                statusText.text = "● ATIVE O GPS"
                statusText.setTextColor(Color.rgb(255, 194, 76))
            }
        } catch (_: SecurityException) {
            statusText.text = "● PERMISSÃO NECESSÁRIA"
        }
    }

    override fun onLocationChanged(location: Location) {
        currentLocation = location
        coordsText.text = String.format(Locale("pt", "BR"), "%.6f\n%.6f", location.latitude, location.longitude)
        accuracyText.text = if (location.hasAccuracy()) String.format(Locale("pt", "BR"), "± %.1f m", location.accuracy) else "Calculando..."
        speedText.text = String.format(Locale("pt", "BR"), "%.1f km/h", if (location.hasSpeed()) location.speed * 3.6f else 0f)
        statusText.text = "● GPS ATIVO"
        statusText.setTextColor(Color.rgb(112, 224, 139))
        updateDeviation()
        fieldView.invalidate()
    }

    private fun updateDeviation() {
        val a = startPoint
        val b = endPoint
        val p = currentLocation
        if (a == null || b == null || p == null) {
            deviationText.text = if (a != null && b == null) "Marque o fim" else "-- m"
            return
        }
        val lat0 = Math.toRadians(a.latitude)
        val eastB = Math.toRadians(b.longitude - a.longitude) * 6371000.0 * cos(lat0)
        val northB = Math.toRadians(b.latitude - a.latitude) * 6371000.0
        val eastP = Math.toRadians(p.longitude - a.longitude) * 6371000.0 * cos(lat0)
        val northP = Math.toRadians(p.latitude - a.latitude) * 6371000.0
        val len = hypot(eastB, northB)
        if (len < 0.1) return
        // A linha A-B define a família de passadas; mostrar desvio até a passada paralela mais próxima.
        val signedFromBase = (eastB * northP - northB * eastP) / len
        val nearestPass = round(signedFromBase / implementWidth) * implementWidth
        val signedDeviation = signedFromBase - nearestPass
        deviationText.text = String.format(Locale("pt", "BR"), "%+.1f m", signedDeviation)
    }

    private fun distanceMeters(a: Location, b: Location): Double {
        val result = FloatArray(1)
        Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, result)
        return result[0].toDouble()
    }

    @Deprecated("Deprecated in Android")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) { statusText.text = "● GPS ATIVO" }
    override fun onProviderDisabled(provider: String) { statusText.text = "● ATIVE O GPS" }

    override fun onPause() {
        super.onPause()
        try { locationManager.removeUpdates(this) } catch (_: Exception) {}
    }
    override fun onResume() {
        super.onResume()
        if (::locationManager.isInitialized && ::statusText.isInitialized) requestGps()
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.any { it == PackageManager.PERMISSION_GRANTED }) requestGps()
    }

    private fun updateWidth() {
        widthText.text = String.format(Locale("pt", "BR"), "%.1f m", implementWidth)
        fieldView.invalidate()
    }
    private fun label(s: String) = TextView(this).apply {
        text = s; textSize = 10f; setTextColor(Color.rgb(126, 166, 138)); typeface = Typeface.DEFAULT_BOLD
    }
    private fun value(s: String) = TextView(this).apply {
        text = s; textSize = 16f; setTextColor(Color.rgb(236, 245, 238)); typeface = Typeface.DEFAULT_BOLD
    }
    private fun space(h: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(h)) }
    private fun actionButton(s: String, muted: Boolean = false, action: () -> Unit) = Button(this).apply {
        text = s; textSize = 10f; isAllCaps = false
        setTextColor(if (muted) Color.rgb(209, 225, 214) else Color.rgb(10, 28, 18))
        backgroundTintList = ColorStateList.valueOf(if (muted) Color.rgb(49, 69, 56) else Color.rgb(126, 220, 143))
        setOnClickListener { action() }
    }
    private fun noGps() { Toast.makeText(this, "Aguardando posição GPS", Toast.LENGTH_SHORT).show() }
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()

    inner class FieldGuidanceView : View(this@MainActivity) {
        private fun paint(color: Int, stroke: Float = 0f, style: Paint.Style = Paint.Style.FILL) =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                strokeWidth = stroke
                this.style = style
                strokeCap = Paint.Cap.ROUND
            }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            canvas.drawRoundRect(RectF(0f, 0f, w, h), dp(12).toFloat(), dp(12).toFloat(),
                paint(Color.rgb(22, 43, 33)))
            val grid = paint(Color.rgb(42, 70, 51), dp(1).toFloat())
            var gx = 0f
            while (gx < w) { canvas.drawLine(gx, 0f, gx, h, grid); gx += dp(32).toFloat() }
            var gy = 0f
            while (gy < h) { canvas.drawLine(0f, gy, w, gy, grid); gy += dp(32).toFloat() }

            val field = Path().apply {
                moveTo(w * .08f, h * .17f)
                lineTo(w * .86f, h * .12f)
                lineTo(w * .94f, h * .81f)
                lineTo(w * .15f, h * .88f)
                close()
            }
            canvas.drawPath(field, paint(Color.rgb(35, 77, 47)))

            val a = startPoint
            val b = endPoint
            if (a != null && b != null) {
                val lat0 = Math.toRadians(a.latitude)
                val eastB = Math.toRadians(b.longitude - a.longitude) * 6371000.0 * cos(lat0)
                val northB = Math.toRadians(b.latitude - a.latitude) * 6371000.0
                val len = hypot(eastB, northB)
                if (len > 0.1) {
                    val ux = eastB / len
                    val uy = northB / len
                    val centerX = w * 0.50f
                    val centerY = h * 0.50f
                    val scale = (minOf(w * 0.62f, h * 0.58f).toDouble() / maxOf(len, implementWidth * 5.0))
                    val halfLenPx = (len * scale / 2.0).toFloat()
                    val alongX = ux.toFloat()
                    val alongY = -uy.toFloat()
                    val baseStartX = centerX - alongX * halfLenPx
                    val baseStartY = centerY - alongY * halfLenPx
                    val baseEndX = centerX + alongX * halfLenPx
                    val baseEndY = centerY + alongY * halfLenPx
                    canvas.drawLine(baseStartX, baseStartY, baseEndX, baseEndY,
                        paint(Color.rgb(255, 196, 66), dp(3).toFloat()))

                    val lanePaint = paint(Color.rgb(119, 198, 130), dp(2).toFloat())
                    for (offset in -4..4) {
                        if (offset == 0) continue
                        val sidePx = (offset * implementWidth * scale).toFloat()
                        val sideX = uy.toFloat() * sidePx
                        val sideY = ux.toFloat() * sidePx
                        canvas.drawLine(baseStartX + sideX, baseStartY + sideY,
                            baseEndX + sideX, baseEndY + sideY, lanePaint)
                    }

                    currentLocation?.let { p ->
                        val eastP = Math.toRadians(p.longitude - a.longitude) * 6371000.0 * cos(lat0)
                        val northP = Math.toRadians(p.latitude - a.latitude) * 6371000.0
                        val along = eastP * ux + northP * uy
                        val cross = eastP * uy - northP * ux
                        val vehicleX = centerX + (along * ux - cross * uy).toFloat() * scale.toFloat()
                        val vehicleY = centerY - (along * uy + cross * ux).toFloat() * scale.toFloat()
                        canvas.drawCircle(vehicleX, vehicleY, dp(10).toFloat(), paint(Color.rgb(9, 18, 13)))
                        canvas.drawCircle(vehicleX, vehicleY, dp(6).toFloat(), paint(Color.WHITE))
                        canvas.drawCircle(vehicleX, vehicleY, dp(3).toFloat(), paint(Color.rgb(255, 196, 66)))
                    }
                }
            } else {
                val lines = paint(Color.rgb(100, 165, 104), dp(2).toFloat())
                for (i in 0..8) {
                    val xx = w * (0.13f + i * 0.09f)
                    canvas.drawLine(xx, h * 0.24f, xx + w * 0.08f, h * 0.78f, lines)
                }
            }
            canvas.drawText("VISÃO DE ORIENTAÇÃO", dp(14).toFloat(), dp(23).toFloat(),
                paint(Color.WHITE).apply { textSize = dp(12).toFloat(); typeface = Typeface.DEFAULT_BOLD })
            val subtitle = if (a != null && b != null) "Linha base e passadas paralelas aproximadas"
                else "Marque início e fim da passada"
            canvas.drawText(subtitle, dp(14).toFloat(), h - dp(14).toFloat(),
                paint(Color.rgb(188, 214, 190)).apply { textSize = dp(10).toFloat() })
        }
    }
}
