package com.tienda

import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.tienda.util.DbConstants.DB_NAME
import com.tienda.util.DbConstants.TABLE_PRODUCTOS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class TransferActivity : AppCompatActivity() {

    private lateinit var btnImportar: MaterialButton
    private lateinit var btnExportar: MaterialButton

    // IMPORTAR: elegir .db origen
    private val importTableLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { importarTabla(it, TABLE_PRODUCTOS) }
        }

    // EXPORTAR: crear archivo .db destino
    private val exportTableLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
            uri?.let { exportarTabla(it, TABLE_PRODUCTOS) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_transfer)
        setSupportActionBar(findViewById(R.id.topAppBar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        //Contenido NO debajo de la status bar
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = ContextCompat.getColor(this, R.color.green_500)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        btnImportar = findViewById(R.id.btnImportarTabla)
        btnExportar = findViewById(R.id.btnExportarTabla)

        btnImportar.setOnClickListener { onClickImportarTabla() }
        btnExportar.setOnClickListener { onClickExportarTabla() }


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun onClickImportarTabla() {
        importTableLauncher.launch(arrayOf("application/octet-stream", "application/x-sqlite3", "*/*"))
    }

    private fun onClickExportarTabla() {
        val defaultName = "tabla_${TABLE_PRODUCTOS}_${System.currentTimeMillis()}.db"
        exportTableLauncher.launch(defaultName)
    }

    // IMPORTAR: copia datos de una tabla desde un .db externo a tu BD
    private fun importarTabla(uri: Uri, tableName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val tmpFile = File(cacheDir, "import.db")
            try {
                // Copiar .db origen a temporal
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tmpFile).use { output -> input.copyTo(output) }
                }

                val dstDbFile = getDatabasePath(DB_NAME)
                val db = SQLiteDatabase.openDatabase(dstDbFile.path, null, SQLiteDatabase.OPEN_READWRITE)

                db.beginTransaction()
                try {
                    // Adjuntar BD origen
                    db.execSQL("ATTACH DATABASE '${tmpFile.absolutePath}' AS src")

                    // Asegura estructura (si no existe crea vacía)
                    db.execSQL("CREATE TABLE IF NOT EXISTS \"$tableName\" AS SELECT * FROM src.\"$tableName\" WHERE 0")

                    // Reemplazar datos
                    db.execSQL("DELETE FROM \"$tableName\"")
                    db.execSQL("INSERT INTO \"$tableName\" SELECT * FROM src.\"$tableName\"")

                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                    // NO llamamos DETACH: cerrar la conexión hace el detach implícitamente.
                    db.close()
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@TransferActivity, "Importación de $tableName completa ✅", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@TransferActivity, "Error al importar: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                try { tmpFile.delete() } catch (_: Exception) {}
            }
        }
    }

    // EXPORTAR: crea un .db nuevo con SOLO esa tabla y sus datos
    private fun exportarTabla(targetUri: Uri, tableName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val tmpFile = File(cacheDir, "one_table_export.db")
            if (tmpFile.exists()) tmpFile.delete()

            try {
                val dst = SQLiteDatabase.openOrCreateDatabase(tmpFile, null)
                dst.beginTransaction()
                try {
                    val srcPath = getDatabasePath(DB_NAME).absolutePath
                    dst.execSQL("ATTACH DATABASE '$srcPath' AS src")

                    // Crea tabla en destino con datos en 1 paso
                    dst.execSQL("CREATE TABLE \"$tableName\" AS SELECT * FROM src.\"$tableName\"")

                    dst.setTransactionSuccessful()
                } finally {
                    dst.endTransaction()
                    // Igual: NO llamamos DETACH; cerrar cierra y detacha.
                    dst.close()
                }
                // Escribir el archivo exportado al URI elegido
                contentResolver.openOutputStream(targetUri)?.use { out ->
                    FileInputStream(tmpFile).use { it.copyTo(out) }
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@TransferActivity, "Exportación de $tableName completa ✅", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@TransferActivity, "Error al exportar: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                try { tmpFile.delete() } catch (_: Exception) {}
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish();
        return true
    }
}