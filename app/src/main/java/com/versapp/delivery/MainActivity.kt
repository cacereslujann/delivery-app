package com.versapp.delivery

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.radiobutton.MaterialRadioButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import android.widget.RadioGroup
import java.text.NumberFormat
import java.util.Locale

// App de delivery para probar VersApp. Todo es de ejemplo: no hay servidor ni pagos.
// Pantallas: Inicio (restaurantes) → Menú → Carrito → Seguimiento del pedido.
class MainActivity : AppCompatActivity() {

    data class Producto(val nombre: String, val detalle: String, val precio: Int)
    data class Restaurante(
        val nombre: String, val categoria: String, val emoji: String,
        val minutos: String, val envio: Int, val puntaje: String, val menu: List<Producto>
    )

    private val restaurantes = listOf(
        Restaurante("La Esquina del Lomito", "Lomitos", "🥪", "25-35 min", 8000, "4.7", listOf(
            Producto("Lomito árabe", "Carne, tomate, lechuga y salsa de ajo", 35000),
            Producto("Lomito completo", "Con huevo, jamón y queso", 42000),
            Producto("Papas fritas", "Porción grande", 15000),
            Producto("Gaseosa 500 ml", "Cola, naranja o pomelo", 9000),
        )),
        Restaurante("Pizzería Don Carlo", "Pizzas", "🍕", "30-45 min", 10000, "4.5", listOf(
            Producto("Pizza muzzarella", "8 porciones", 55000),
            Producto("Pizza napolitana", "Tomate, ajo y albahaca", 62000),
            Producto("Fainá", "2 porciones", 12000),
            Producto("Cerveza 1 L", "Bien fría", 18000),
        )),
        Restaurante("Burger Norte", "Hamburguesas", "🍔", "20-30 min", 7000, "4.8", listOf(
            Producto("Hamburguesa clásica", "Carne, cheddar y pepinillos", 32000),
            Producto("Doble bacon", "Doble carne y panceta", 45000),
            Producto("Aros de cebolla", "Con salsa barbacoa", 16000),
            Producto("Batido de frutilla", "500 ml", 17000),
        )),
        Restaurante("Sabor Paraguayo", "Comida típica", "🫓", "35-50 min", 9000, "4.9", listOf(
            Producto("Chipa guasu", "Porción", 18000),
            Producto("Sopa paraguaya", "Porción", 15000),
            Producto("Vori vori", "Plato hondo", 28000),
            Producto("Mbeju", "Con queso Paraguay", 14000),
        )),
        Restaurante("Dulce Tentación", "Postres", "🍰", "15-25 min", 6000, "4.6", listOf(
            Producto("Torta de chocolate", "Porción", 20000),
            Producto("Helado 1/4 kg", "Dos gustos", 22000),
            Producto("Alfajores x3", "De maicena", 15000),
        )),
    )

    private val categorias = listOf("Todos") + restaurantes.map { it.categoria }.distinct()
    private var categoria = "Todos"
    private var restauranteActual: Restaurante? = null
    private val carrito = linkedMapOf<Producto, Int>()     // producto → cantidad
    private var pantalla = "inicio"
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var contenido: FrameLayout

    private val gs = NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-PY"))
    private fun precio(n: Int) = "Gs. ${gs.format(n)}"
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun color(id: Int) = ContextCompat.getColor(this, id)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        contenido = FrameLayout(this).apply { id = View.generateViewId(); setBackgroundColor(color(R.color.fondo)) }
        setContentView(contenido)
        ViewCompat.setOnApplyWindowInsetsListener(contenido) { v, insets ->
            val s = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(s.left, s.top, s.right, s.bottom)
            insets
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (pantalla) {
                    "menu" -> mostrarInicio()
                    "carrito" -> restauranteActual?.let { mostrarMenu(it) } ?: mostrarInicio()
                    "seguimiento" -> { handler.removeCallbacksAndMessages(null); mostrarInicio() }
                    else -> finish()
                }
            }
        })
        mostrarInicio()
    }

    override fun onDestroy() { handler.removeCallbacksAndMessages(null); super.onDestroy() }

    // ── Piezas de interfaz ───────────────────────────────────────────────────
    private fun texto(t: String, tam: Float = 15f, negrita: Boolean = false, col: Int = R.color.texto) =
        TextView(this).apply {
            text = t; textSize = tam; setTextColor(color(col))
            if (negrita) setTypeface(typeface, Typeface.BOLD)
        }

    private fun columna(pad: Int = 0) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(pad), dp(pad), dp(pad), dp(pad))
    }

    private fun fila() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }

    private fun tarjeta(hijo: View) = MaterialCardView(this).apply {
        radius = dp(14).toFloat(); cardElevation = 0f; strokeWidth = dp(1); strokeColor = Color.parseColor("#E5E7EB")
        setCardBackgroundColor(Color.WHITE)
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = dp(10) }
        addView(hijo)
    }

    private fun boton(t: String, alClic: () -> Unit) = MaterialButton(this).apply {
        text = t; isAllCaps = false; textSize = 16f; cornerRadius = dp(12)
        setPadding(dp(16), dp(14), dp(16), dp(14))
        setOnClickListener { alClic() }
    }

    private fun circulo(emoji: String) = TextView(this).apply {
        text = emoji; textSize = 28f; gravity = Gravity.CENTER
        background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(color(R.color.naranja_claro)) }
        layoutParams = LinearLayout.LayoutParams(dp(56), dp(56)).apply { marginEnd = dp(14) }
    }

    private fun encabezado(titulo: String, subtitulo: String?, volver: Boolean) = columna().apply {
        setBackgroundColor(color(R.color.naranja)); setPadding(dp(20), dp(18), dp(20), dp(18))
        if (volver) addView(texto("← Volver", 14f, col = R.color.white).apply {
            setPadding(0, 0, 0, dp(8)); setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        })
        addView(texto(titulo, 24f, true, R.color.white))
        if (subtitulo != null) addView(texto(subtitulo, 14f, col = R.color.white).apply { alpha = 0.9f })
    }

    private fun versionApp(): String {
        val info = packageManager.getPackageInfo(packageName, 0)
        return "Versión ${info.versionName} (${info.longVersionCode})"
    }

    // Pantalla = encabezado fijo + contenido con scroll + barra inferior opcional.
    private fun armar(cabeza: View, cuerpo: View, abajo: View?) {
        val raiz = columna()
        raiz.addView(cabeza)
        raiz.addView(ScrollView(this).apply {
            isFillViewport = true
            addView(cuerpo)
        }, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        if (abajo != null) raiz.addView(abajo)
        contenido.removeAllViews()
        contenido.addView(raiz, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
    }

    private fun barraCarrito(): View? {
        val cant = carrito.values.sum()
        if (cant == 0) return null
        val total = carrito.entries.sumOf { it.key.precio * it.value }
        return columna(12).apply {
            setBackgroundColor(Color.WHITE)
            addView(boton("Ver carrito · $cant ${if (cant == 1) "producto" else "productos"} · ${precio(total)}") { mostrarCarrito() })
        }
    }

    // ── Inicio: lista de restaurantes ────────────────────────────────────────
    private fun mostrarInicio() {
        pantalla = "inicio"
        val cuerpo = columna(16)
        cuerpo.addView(ChipGroup(this).apply {
            isSingleSelection = true; isSelectionRequired = true
            for (c in categorias) addView(Chip(this@MainActivity).apply {
                text = c; isCheckable = true; isChecked = c == categoria
                setOnClickListener { categoria = c; mostrarInicio() }
            })
            setPadding(0, 0, 0, dp(8))
        })
        for (r in restaurantes.filter { categoria == "Todos" || it.categoria == categoria }) {
            val f = fila().apply { setPadding(dp(14), dp(14), dp(14), dp(14)) }
            f.addView(circulo(r.emoji))
            f.addView(columna().apply {
                addView(texto(r.nombre, 17f, true))
                addView(texto("${r.categoria} · ★ ${r.puntaje}", 13f, col = R.color.texto_suave))
                addView(texto("${r.minutos} · Envío ${precio(r.envio)}", 13f, col = R.color.texto_suave))
            }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            cuerpo.addView(tarjeta(f).apply { setOnClickListener { mostrarMenu(r) } })
        }
        cuerpo.addView(texto(versionApp(), 12f, col = R.color.texto_suave).apply {
            gravity = Gravity.CENTER; setPadding(0, dp(16), 0, dp(8))
        })
        armar(encabezado("¿Qué vas a pedir hoy?", "Entregamos en Asunción y alrededores", false), cuerpo, barraCarrito())
    }

    // ── Menú de un restaurante ───────────────────────────────────────────────
    private fun mostrarMenu(r: Restaurante) {
        pantalla = "menu"
        if (restauranteActual != r) carrito.clear()        // un pedido es de un solo local
        restauranteActual = r
        val cuerpo = columna(16)
        for (p in r.menu) {
            val f = fila().apply { setPadding(dp(14), dp(12), dp(10), dp(12)) }
            f.addView(columna().apply {
                addView(texto(p.nombre, 16f, true))
                addView(texto(p.detalle, 13f, col = R.color.texto_suave))
                addView(texto(precio(p.precio), 15f, true, R.color.naranja).apply { setPadding(0, dp(4), 0, 0) })
            }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            val cant = carrito[p] ?: 0
            if (cant > 0) {
                f.addView(boton("−") { cambiar(p, -1); mostrarMenu(r) }.apply { minWidth = 0; minimumWidth = 0 })
                f.addView(texto("$cant", 16f, true).apply { setPadding(dp(10), 0, dp(10), 0) })
            }
            f.addView(boton("+") { cambiar(p, 1); mostrarMenu(r) }.apply { minWidth = 0; minimumWidth = 0 })
            cuerpo.addView(tarjeta(f))
        }
        armar(encabezado("${r.emoji} ${r.nombre}", "${r.minutos} · ★ ${r.puntaje}", true), cuerpo, barraCarrito())
    }

    private fun cambiar(p: Producto, delta: Int) {
        val n = (carrito[p] ?: 0) + delta
        if (n <= 0) carrito.remove(p) else carrito[p] = n
    }

    // ── Carrito y confirmación ───────────────────────────────────────────────
    private fun mostrarCarrito() {
        pantalla = "carrito"
        val r = restauranteActual ?: return mostrarInicio()
        val subtotal = carrito.entries.sumOf { it.key.precio * it.value }
        val cuerpo = columna(16)

        val lista = columna(14)
        for ((p, n) in carrito) lista.addView(fila().apply {
            addView(texto("$n × ${p.nombre}"), LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            addView(texto(precio(p.precio * n)))
            setPadding(0, dp(4), 0, dp(4))
        })
        lista.addView(View(this).apply { setBackgroundColor(Color.parseColor("#E5E7EB")) },
            LinearLayout.LayoutParams(MATCH_PARENT, dp(1)).apply { topMargin = dp(8); bottomMargin = dp(8) })
        for ((t, v, b) in listOf(Triple("Subtotal", subtotal, false), Triple("Envío", r.envio, false), Triple("Total", subtotal + r.envio, true)))
            lista.addView(fila().apply {
                addView(texto(t, if (b) 17f else 15f, b), LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
                addView(texto(precio(v), if (b) 17f else 15f, b))
            })
        cuerpo.addView(tarjeta(lista))

        val campo = TextInputEditText(this).apply { setText("Av. Mcal. López 1234") }
        cuerpo.addView(TextInputLayout(this).apply {
            hint = "Dirección de entrega"; addView(campo)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = dp(10) }
        })

        cuerpo.addView(texto("Forma de pago", 15f, true).apply { setPadding(0, dp(4), 0, 0) })
        val pagos = RadioGroup(this)
        for ((i, t) in listOf("Efectivo", "Tarjeta al recibir", "Transferencia").withIndex())
            pagos.addView(MaterialRadioButton(this).apply { id = 100 + i; text = t; isChecked = i == 0 })
        cuerpo.addView(pagos)

        val abajo = columna(12).apply {
            setBackgroundColor(Color.WHITE)
            addView(boton("Confirmar pedido · ${precio(subtotal + r.envio)}") {
                val dir = campo.text?.toString()?.trim().orEmpty()
                if (dir.isEmpty()) { campo.error = "Escribí a dónde llevamos el pedido"; return@boton }
                mostrarSeguimiento(r, dir, subtotal + r.envio)
            })
        }
        armar(encabezado("Tu pedido", r.nombre, true), cuerpo, abajo)
    }

    // ── Seguimiento (simulado con un temporizador) ───────────────────────────
    private fun mostrarSeguimiento(r: Restaurante, direccion: String, total: Int) {
        pantalla = "seguimiento"
        val numero = (1000..9999).random()
        val pasos = listOf("Pedido recibido", "Preparando tu pedido", "En camino", "¡Entregado!")
        val cuerpo = columna(16)
        val barra = LinearProgressIndicator(this).apply { max = pasos.size - 1; isIndeterminate = false; trackCornerRadius = dp(4) }
        val estado = texto(pasos[0], 22f, true)
        val lineas = pasos.map { texto("○  $it", 15f, col = R.color.texto_suave).apply { setPadding(0, dp(6), 0, dp(6)) } }

        val tarjetaEstado = columna(18).apply {
            addView(texto("Pedido #$numero · ${r.nombre}", 13f, col = R.color.texto_suave))
            addView(estado.apply { setPadding(0, dp(6), 0, dp(12)) })
            addView(barra)
            addView(View(this@MainActivity), LinearLayout.LayoutParams(MATCH_PARENT, dp(12)))
            lineas.forEach { addView(it) }
        }
        cuerpo.addView(tarjeta(tarjetaEstado))
        cuerpo.addView(tarjeta(columna(14).apply {
            addView(texto("Entrega en", 13f, col = R.color.texto_suave))
            addView(texto(direccion, 15f, true))
            addView(texto("Total: ${precio(total)}", 15f).apply { setPadding(0, dp(6), 0, 0) })
        }))

        carrito.clear()
        val abajo = columna(12).apply { setBackgroundColor(Color.WHITE); addView(boton("Hacer otro pedido") { onBackPressedDispatcher.onBackPressed() }) }
        armar(encabezado("Seguimiento", "Te avisamos cuando llegue", false), cuerpo, abajo)

        fun marcar(i: Int) {
            estado.text = pasos[i]
            barra.setProgressCompat(i, true)
            lineas.forEachIndexed { j, l ->
                l.text = "${if (j <= i) "●" else "○"}  ${pasos[j]}"
                l.setTextColor(color(if (j < i) R.color.verde else if (j == i) R.color.naranja else R.color.texto_suave))
            }
            if (i == pasos.lastIndex) estado.setTextColor(color(R.color.verde))
            else handler.postDelayed({ marcar(i + 1) }, 4000)
        }
        marcar(0)
    }
}
