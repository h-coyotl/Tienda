package com.tienda

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.button.MaterialButton
import com.tienda.util.DbConstants.DB_NAME
import com.tienda.util.DbConstants.DB_VERSION
import com.tienda.util.FormatoFecha.Companion.formatearFechaMX
import com.tienda.util.FormatoFecha.Companion.obtenerTimestamp
import com.tienda.util.ImageUtils
import com.tienda.util.ImageUtils.Companion.decodeBase64ToBitmap
import com.tienda.util.ImageUtils.Companion.getDefaultImageBase64


class DetalleProductoActivity : AppCompatActivity() {

    private lateinit var etNombre: EditText
    private lateinit var etDescripcion: EditText
    private lateinit var etPrecio: EditText
    private lateinit var etFecha: TextView
    private var idProducto: Int = -1
    private lateinit var imgPreview: ImageView
    private var imagenBase64: String? = null
    private lateinit var spUnidad: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_detalle_producto)
        setSupportActionBar(findViewById(R.id.topAppBar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        //Contenido NO debajo de la status bar
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = ContextCompat.getColor(this, R.color.green_500)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        etNombre = findViewById(R.id.etDetalleNombre)
        etDescripcion = findViewById(R.id.etDetalleDescripcion)
        etPrecio = findViewById(R.id.etDetallePrecio)
        imgPreview = findViewById(R.id.imgPreview)
        spUnidad = findViewById(R.id.spUnidad)

        // Inicializar Spinner de unidad de medida
        val spinnerUnidad = findViewById<Spinner>(R.id.spUnidad)
        val adapterUnidad = ArrayAdapter.createFromResource(
            this,
            R.array.unidades_medida,
            android.R.layout.simple_spinner_item
        )
        adapterUnidad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerUnidad.adapter = adapterUnidad

        //Seleccionar imagen
        findViewById<View>(R.id.cardAddImage).setOnClickListener {
            pickImage.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        idProducto = intent.getIntExtra("ID_PRODUCTO", -1)
        if (idProducto == -1) {
            finish()
            return
        }

        cargarProducto(idProducto);

        findViewById<MaterialButton>(R.id.btnGuardar).setOnClickListener {
            guardarCambios(idProducto)
        }

        findViewById<MaterialButton>(R.id.btnEliminar)?.setOnClickListener {
            confirmarEliminar()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun cargarProducto(idProducto: Int) {
        val con = SQLite(this, DB_NAME, null, DB_VERSION)
        val db = con.readableDatabase
        val fila = db.rawQuery(
            "SELECT nombre,descripcion,precio,imagen,unidad_medida FROM productos " +
                    "WHERE id_producto = ?",arrayOf(idProducto.toString())
        )
        if (fila.moveToFirst()) {
            etNombre.setText(fila.getString(0) ?: "")
            etDescripcion.setText(fila.getString(1) ?: "")
            val precioDouble = fila.getString(2)?.toDoubleOrNull() ?: 0.0
            etPrecio.setText(precioDouble.toString())
            val base64 = fila.getString(3) ?: ""

            if (base64.isNotEmpty()) {
                decodeBase64ToBitmap(base64)?.let(imgPreview::setImageBitmap)
            }
            val unidad = fila.getString(4)
            (spUnidad.adapter as? ArrayAdapter<String>)?.let { ad ->
                val pos = ad.getPosition(unidad)
                if (pos >= 0) spUnidad.setSelection(pos, false)
            }
        }
        fila.close()
        db.close()
    }

    private fun guardarCambios(idProducto: Int) {
        // Validaciones simples
        val nombre = etNombre.text.toString().trim()
        val descripcion = etDescripcion.text.toString().trim()
        val precioDouble = etPrecio.text.toString().replace(",", ".").toDoubleOrNull()
        if (nombre.isEmpty()) {
            etNombre.error = "Requerido"
            etNombre.requestFocus()
            return
        }
        if (precioDouble == null) {
            etPrecio.error = "Precio inválido"
            etPrecio.requestFocus()
            return
        }
        val fechaActual = obtenerTimestamp();
        val unidad = spUnidad.selectedItem?.toString()?.trim();

        val con = SQLite(this, DB_NAME, null, DB_VERSION)
        val db = con.writableDatabase
        val values = ContentValues().apply {
            put("nombre", nombre)
            put("descripcion", descripcion)
            put("precio", precioDouble)
            put("fecha_actualizacion", fechaActual)
            put("unidad_medida", unidad)
        }
        val filas = db.update(
            "productos",
            values,
            "id_producto = ?",
            arrayOf(idProducto.toString())
        )
        db.close()

        if (filas > 0) {
            Toast.makeText(this, "Producto actualizado", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun confirmarGuardarSoloImagen() {
        AlertDialog.Builder(this)
            .setTitle("Actualizar imagen")
            .setMessage("¿Deseas guardar la nueva imagen para este producto?")
            .setPositiveButton("Guardar") { _, _ -> actualizarSoloImagen() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun actualizarSoloImagen() {
        // Usa la seleccionada o la default
        val imgB64 = imagenBase64 ?: getDefaultImageBase64(this);
        val con = SQLite(this, DB_NAME, null, DB_VERSION)
        val db = con.writableDatabase
        val values = ContentValues().apply {
            put("imagen", imgB64)
        }
        val filas = db.update(
            "productos",
            values,
            "id_producto = ?",
            arrayOf(idProducto.toString())
        )
        db.close()
        if (filas > 0) {
            Toast.makeText(this, "Imagen actualizada", Toast.LENGTH_SHORT).show()
            setResult(Activity.RESULT_OK)
            finish()
        } else {
            Toast.makeText(this, "No se pudo actualizar la imagen", Toast.LENGTH_SHORT).show()
        }
    }

    fun eliminar(view: View) {
        confirmarEliminar()
    }

    private fun confirmarEliminar() {
        AlertDialog.Builder(this)
            .setTitle("Eliminar producto")
            .setMessage("¿Seguro que deseas eliminar este producto?")
            .setPositiveButton("Eliminar") { _, _ ->
                eliminarProducto()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarProducto() {
        val con = SQLite(this, DB_NAME, null, DB_VERSION)
        val db = con.writableDatabase
        val filas = db.delete(
            "productos",
            "id_producto = ?",
            arrayOf(idProducto.toString())
        )
        db.close()
        if (filas > 0) {
            Toast.makeText(this, "Producto eliminado", Toast.LENGTH_SHORT).show()
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            // Vista previa
            imgPreview.setImageURI(it)
            // Mostrarla (con un suave fade-in opcional)
            imgPreview.apply {
                alpha = 0f
                visibility = View.VISIBLE
                animate().alpha(1f).setDuration(180).start()
            }
            imagenBase64 = ImageUtils.loadBitmapFromUri(contentResolver, it)
                ?.let { bmp -> ImageUtils.resizeBitmap(bmp) }
                ?.let { bmp -> ImageUtils.bitmapToBytes(bmp) }
                ?.let { bytes -> ImageUtils.encodeToBase64(bytes) }
        }
        confirmarGuardarSoloImagen();
    }

    override fun onSupportNavigateUp(): Boolean {
        finish();
        return true
    }

}