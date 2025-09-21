package com.tienda

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import com.google.android.material.button.MaterialButton
import com.tienda.util.DbConstants.DB_NAME
import com.tienda.util.DbConstants.DB_VERSION
import com.tienda.util.ImageUtils
import com.tienda.util.ImageUtils.Companion.getDefaultImageBase64

class NuevoProductoActivity : AppCompatActivity() {
    private lateinit var txtNombre: EditText
    private lateinit var  txtDescripcion: EditText
    private lateinit   var txtPrecio: EditText
    private var imgPreview: ImageView? = null
    private var imagenBase64: String? = null
    private lateinit var spUnidad: Spinner



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_nuevo_producto)
        setSupportActionBar(findViewById(R.id.topAppBar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = ContextCompat.getColor(this, R.color.green_500)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        txtNombre= findViewById(R.id.txtNombre)
        txtDescripcion = findViewById(R.id.txtDescripcion)
        txtPrecio = findViewById(R.id.txtPrecio)
        imgPreview = findViewById(R.id.imgPreview)
        spUnidad = findViewById(R.id.spUnidad)

        // FAB: abrir TransferActivity
        findViewById<ImageView>(R.id.btnTransfer).setOnClickListener {
            startActivity(Intent(this, TransferActivity::class.java))
        }

        //Seleccionar imagen
        findViewById<View>(R.id.cardAddImage).setOnClickListener {
            pickImage.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        val adapterUnidad = ArrayAdapter.createFromResource(
            this,
            R.array.unidades_medida,
            android.R.layout.simple_spinner_item
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spUnidad.adapter = adapterUnidad


        findViewById<MaterialButton>(R.id.btnCancelar).setOnClickListener {
            vaciarCampos()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    fun insertar(view: View) {
        val con = SQLite(this, DB_NAME, null, DB_VERSION)
        val baseDatos = con.writableDatabase

        val nombre = txtNombre?.text.toString().trim()
        val descripcion = txtDescripcion?.text.toString().trim()
        val precio = txtPrecio.text.toString().replace(",", ".").toDoubleOrNull()


        if (nombre.isEmpty()) {
            txtNombre.error = "Requerido"
            txtNombre.requestFocus()
            return
        }
        if (precio == null) {
            txtPrecio.error = "Precio inválido"
            txtPrecio.requestFocus()
            return
        }

        val unidad = spUnidad.selectedItem?.toString();

        // Usa la seleccionada o la default
        val imgB64 = imagenBase64 ?: getDefaultImageBase64(this);

        val registro = ContentValues().apply {
            put("nombre", nombre)
            put("descripcion", descripcion)
            put("precio", precio)
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            put("fecha_actualizacion", sdf.format(Date()))
            put("imagen", imgB64)
            put("estatus", 1)
            put("unidad_medida", unidad)
        }

        baseDatos.insert("productos", null, registro)
        baseDatos.close()

        vaciarCampos();

        Toast.makeText(this, "Producto registrado", Toast.LENGTH_SHORT).show()
    }

    fun vaciarCampos() {
        txtNombre.setText("")
        txtDescripcion.setText("")
        txtPrecio.setText("")
        spUnidad.setSelection(0)
        finish();
    }

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            // Vista previa
            imgPreview?.setImageURI(it)
            // Mostrarla (con un suave fade-in opcional)
            imgPreview?.apply {
                alpha = 0f
                visibility = View.VISIBLE
                animate().alpha(1f).setDuration(180).start()
            }
            imagenBase64 = ImageUtils.loadBitmapFromUri(contentResolver, it)
                ?.let { bmp -> ImageUtils.resizeBitmap(bmp) }
                ?.let { bmp -> ImageUtils.bitmapToBytes(bmp) }
                ?.let { bytes -> ImageUtils.encodeToBase64(bytes) }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish();
        return true
    }

}

