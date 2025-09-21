package com.tienda

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TableLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import com.tienda.util.DbConstants.DB_NAME
import com.tienda.util.DbConstants.DB_VERSION
import com.tienda.util.ImageUtils.Companion.decodeBase64ToBitmap
import java.text.NumberFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {
    var tlProductos: TableLayout? = null
    var etBuscar: TextInputEditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        //Contenido NO debajo de la status bar
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = ContextCompat.getColor(this, R.color.green_500)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        tlProductos = findViewById(R.id.tlProductos)
        etBuscar = findViewById(R.id.etBuscar)

        // Cargar productos al inicio
        cargarProductos("");

        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        toolbar.setNavigationOnClickListener {
            startActivity(Intent(this, NuevoProductoActivity::class.java))
        }


        etBuscar?.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val filtro = etBuscar?.text?.toString()?.trim().orEmpty()
                tlProductos?.removeAllViews()
                cargarProductos(filtro)
                // Ocultar teclado
                (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .hideSoftInputFromWindow(v.windowToken, 0)
                true
            } else false
        }

        etBuscar?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val filtro = s?.toString()?.trim().orEmpty()
                tlProductos?.removeAllViews()
                cargarProductos(filtro)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    fun cargarProductos(filtro: String) {
        val con = SQLite(this, DB_NAME, null, DB_VERSION)
        val baseDatos = con.readableDatabase
        val query: String
        val args: Array<String>?
        if (filtro.isEmpty()) {
            query = "SELECT id_producto,nombre,precio,imagen,unidad_medida FROM productos ORDER BY nombre ASC"
            args = null
        } else {
            query = "SELECT id_producto,nombre,precio,imagen,unidad_medida FROM productos WHERE nombre LIKE ? ORDER BY nombre ASC"
            args = arrayOf("%$filtro%")
        }

        val fila = baseDatos.rawQuery(query, args)
        if (fila.moveToFirst()) {
            do {
                val registro = LayoutInflater.from(this).inflate(R.layout.item_table_layout_producto, null, false)
                val tvNombre = registro.findViewById<TextView>(R.id.tvNombre)
                val tvPrecio = registro.findViewById<TextView>(R.id.tvPrecio)
                val ivImagen = registro.findViewById<ImageView>(R.id.ivImagen)
                val unidadMedida = registro.findViewById<TextView>(R.id.tvUnidadMedida)

                val idProducto = fila.getInt(0)
                tvNombre.text = fila.getString(1)
                val precioDouble = fila.getString(2).toDoubleOrNull() ?: 0.0
                val formatoMoneda = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
                tvPrecio.text = formatoMoneda.format(precioDouble)
                //Decodificar Base64 de 'imagen'
                val base64 = fila.getString(3) ?: ""
                unidadMedida.text= fila.getString(4)

                if (base64.isNotEmpty()) {
                    val bmp = decodeBase64ToBitmap(base64)
                    if (bmp != null) {
                        ivImagen.setImageBitmap(bmp)
                    }
                }

                //Guardar id en el propio TextView o en la fila completa
                registro.tag = idProducto

                // Manejar click en el nombre (o en toda la fila)
                registro.setOnClickListener {
                    val id = registro.tag as Int
                    val intent = Intent(this, DetalleProductoActivity::class.java)
                    intent.putExtra("ID_PRODUCTO", id)
                    startActivity(intent)
                }
                tlProductos?.addView(registro)
            } while (fila.moveToNext())
        } else {
            val emptyView = TextView(this)
            emptyView.text = " No hay productos encontrados"
            tlProductos?.addView(emptyView)
        }
       // fila.close()
       // baseDatos.close()
    }

    override fun onResume() {
        super.onResume()
        tlProductos?.removeAllViews()
        cargarProductos("")
    }

}
