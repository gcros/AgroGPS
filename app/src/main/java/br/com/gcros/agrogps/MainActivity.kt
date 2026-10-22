package br.com.gcros.agrogps

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Color
import android.graphics.RectF
import android.widget.FrameLayout
import android.widget.Toast
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : Activity(), LocationListener {
    private lateinit var locationManager: LocationManager
    private lateinit var statusText: TextView
    private lateinit var coordsText: TextView
    private lateinit var accuracyText: TextView
    private lateinit var fieldView: FieldGuidanceView
    private var currentLocation: Location? = null
    private var startPoint: Pair<Double, Double>? = null
    private var endPoint: Pair<Double, Double>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(10, 20, 17)
        window.navigationBarColor = Color.rgb(10, 20, 17)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        buildScreen()
        requestGps()
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(10, 18, 16))
            setPadding(dp(14), dp(10), dp(14), dp(10))
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val titleColumn = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titleColumn.addView(TextView(this).apply {
            text = "AgroGPS"
            textSize = 25f
            setTextColor(Color.rgb(227, 246, 231))
            typeface = Typeface.DEFAULT_BOLD
        })
        titleColumn.addView(TextView(this).apply {
            text = "ORIENTAÇÃO DE CAMPO  •  MODO CELULAR"
            textSize = 10f
            setTextColor(Color.rgb(130, 165, 142))
        })
        header.addView(titleColumn, LinearLayout.LayoutParams(0, -2, 1f))
        statusText = TextView(this).apply {
            text = "●  BUSCANDO GPS"
            textSize = 12f
            setTextColor(Color.rgb(255, 194, 76))
            typeface = Typeface.DEFAULT_BOLD
        }
        header.addView(statusText)
        root.addView(header)
        root.addView(space(10))

        val body = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fieldView = FieldGuidanceView()
        body.addView(fieldView, LinearLayout.LayoutParams(0, -1, 1f))

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(2), dp(4))
        }
        panel.layoutParams = LinearLayout.LayoutParams(dp(220), -1)
        panel.addView(label("POSIÇÃO ATUAL"))
        coordsText = value("Aguardando sinal...")
        panel.addView(coordsText)
        panel.addView(space(10))
        panel.addView(label("PRECISÃO ESTIMADA"))
        accuracyText = value("-- m")
        panel.addView(accuracyText)
        panel.addView(space(14))
        panel.addView(label("CRIAR LINHA GUIA"))
        panel.addView(space(5))
        panel.addView(actionButton("MARCAR INÍCIO") {
            currentLocation?.let {
                startPoint = it.latitude to it.longitude
                Toast.makeText(this, "Início da linha marcado", Toast.LENGTH_SHORT).show()
                fieldView.invalidate()
            } ?: noGps()
        })
        panel.addView(space(6))
        panel.addView(actionButton("MARCAR FIM") {
            currentLocation?.let {
                endPoint = it.latitude to it.longitude
                Toast.makeText(this, "Linha guia criada", Toast.LENGTH_SHORT).show()
                fieldView.invalidate()
            } ?: noGps()
        })
        panel.addView(space(6))
        panel.addView(actionButton("NOVA LINHA", true) {
            startPoint = null
            endPoint = null
            fieldView.invalidate()
        })
        panel.addView(space(12))
        panel.addView(TextView(this).apply {
            text = "GPS DO CELULAR\nPrecisão depende do aparelho, céu aberto e sinal disponível."
            textSize = 11f
            setTextColor(Color.rgb(150, 173, 158))
        })
        body.addView(panel)
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
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 700, 0.5f, this, Looper.getMainLooper())
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let { onLocationChanged(it) }
                statusText.text = "●  GPS ATIVO"
                statusText.setTextColor(Color.rgb(112, 224, 139))
            } else {
                statusText.text = "●  ATIVE O GPS"
            }
        } catch (_: SecurityException) {
            statusText.text = "●  PERMISSÃO NECESSÁRIA"
        }
    }

    override fun onLocationChanged(location: Location) {
        currentLocation = location
        coordsText.text = String.format(java.util.Locale.US, "%.6f\n%.6f", location.latitude, location.longitude)
        accuracyText.text = if (location.hasAccuracy()) "± %.1f m".format(location.accuracy) else "Calculando..."
        statusText.text = "●  GPS ATIVO"
        statusText.setTextColor(Color.rgb(112, 224, 139))
        fieldView.invalidate()
    }

    @Deprecated("Deprecated in Android")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) { statusText.text = "●  GPS ATIVO" }
    override fun onProviderDisabled(provider: String) { statusText.text = "●  ATIVE O GPS" }

    override fun onPause() {
        super.onPause()
        try { locationManager.removeUpdates(this) } catch (_: Exception) {}
    }

    override fun onResume() {
        super.onResume()
        if (::locationManager.isInitialized) requestGps()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.any { it == PackageManager.PERMISSION_GRANTED }) requestGps()
    }

    private fun label(s: String) = TextView(this).apply {
        text = s; textSize = 10f; setTextColor(Color.rgb(126, 166, 138)); typeface = Typeface.DEFAULT_BOLD
    }
    private fun value(s: String) = TextView(this).apply {
        text = s; textSize = 16f; setTextColor(Color.rgb(236, 245, 238)); typeface = Typeface.DEFAULT_BOLD
    }
    private fun space(h: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(h)) }
    private fun actionButton(s: String, muted: Boolean = false, action: () -> Unit) = Button(this).apply {
        text = s; textSize = 11f; isAllCaps = false
        setTextColor(if (muted) Color.rgb(209, 225, 214) else Color.rgb(10, 28, 18))
        setBackgroundTintList(android.content.res.ColorStateList.valueOf(if (muted) Color.rgb(49, 69, 56) else Color.rgb(126, 220, 143)))
        setOnClickListener { action() }
    }
    private fun noGps() { Toast.makeText(this, "Aguardando posição GPS", Toast.LENGTH_SHORT).show() }
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    
    inner class FieldGuidanceView : View(this@MainActivity) {
        private val bg = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat(); val h = height.toFloat()
            canvas.drawRoundRect(RectF(0f, 0f, w, h), dp(12).toFloat(), dp(12).toFloat(), bg.apply { color = Color.rgb(22, 43, 33) })
            val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(42, 70, 51); strokeWidth = dp(1).toFloat() }
            val step = dp(32).toFloat()
            var x = 0f
            while (x < w) { canvas.drawLine(x, 0f, x, h, grid); x += step }
            var y = 0f
            while (y < h) { canvas.drawLine(0f, y, w, y, grid); y += step }
            // Campo ilustrativo e linhas paralelas de orientação.
            val field = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(35, 77, 47); style = Paint.Style.FILL }
            val polygon = Path().apply {
                moveTo(w * .12f, h * .18f); lineTo(w * .78f, h * .10f)
                lineTo(w * .91f, h * .78f); lineTo(w * .22f, h * .88f); close()
            }
            canvas.drawPath(polygon, field)
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(111, 178, 111); strokeWidth = dp(2).toFloat() }
            for (i in 0..9) {
                val xx = w * (.20f + i * .065f)
                canvas.drawLine(xx, h * .22f, xx + w * .09f, h * .81f, linePaint)
            }
            val guidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 196, 66); strokeWidth = dp(3).toFloat() }
            canvas.drawLine(w * .48f, h * .18f, w * .57f, h * .79f, guidePaint)
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL }
            val cx = w * .52f; val cy = h * .50f
            canvas.drawCircle(cx, cy, dp(10).toFloat(), p.apply { color = Color.rgb(12, 22, 17) })
            canvas.drawCircle(cx, cy, dp(6).toFloat(), p.apply { color = Color.rgb(255, 196, 66) })
            canvas.drawLine(cx, cy - dp(19), cx, cy + dp(19), p.apply { color = Color.WHITE; strokeWidth = dp(2).toFloat() })
            canvas.drawLine(cx - dp(19), cy, cx + dp(19), cy, p)
            val caption = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = dp(12).toFloat(); typeface = Typeface.DEFAULT_BOLD }
            canvas.drawText("VISÃO DO CAMPO", dp(14).toFloat(), dp(24).toFloat(), caption)
            val sub = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(188, 214, 190); textSize = dp(10).toFloat() }
            canvas.drawText("Linhas ilustrativas • sem mapa carregado", dp(14).toFloat(), h - dp(14), sub)
        }
    }
}
