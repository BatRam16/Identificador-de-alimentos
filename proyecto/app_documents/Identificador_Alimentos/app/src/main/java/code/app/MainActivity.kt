package code.app

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.core.content.ContextCompat
import code.app.ml.LiteModelAiyVisionClassifierFoodV11
import org.tensorflow.lite.support.image.TensorImage
import java.io.IOException

class MainActivity : AppCompatActivity() {
    companion object{
        @SuppressLint("StaticFieldLeak")
        private lateinit var detector: Detector
        private const val CAMERA_RESULT = 1
        private const val GALLERY_RESULT = 2
        private const val MY_CAMERA_PERMISSION_CODE = 100
        private const val MY_GALLERY_PERMISSION_CODE = 200
        private lateinit var bitmap: Bitmap
        private var height = 350
        private var width = 350
        private var threshold = 350
    }

    private  var image: ImageView? = null
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            MY_CAMERA_PERMISSION_CODE -> {
                if ((grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Toast.makeText(this, "Camera Permission Granted", Toast.LENGTH_LONG).show()
                    val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    startActivityForResult(cameraIntent, MainActivity.CAMERA_RESULT)
                }
                else {
                    Toast.makeText(this, "Camera Permission Denied", Toast.LENGTH_LONG).show()
                }
                return
            }

            MY_GALLERY_PERMISSION_CODE -> {
                if ((grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Toast.makeText(this, "Gallery Permission Granted", Toast.LENGTH_LONG).show()
                    val galleryIntent = Intent(Intent.ACTION_PICK)
                    galleryIntent.type = "image/*"
                    startActivityForResult(galleryIntent, MainActivity.GALLERY_RESULT)
                }
                else {
                    Toast.makeText(this, "Gallery Permission Denied", Toast.LENGTH_LONG).show()
                }
            }

            else -> {

            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        detector = Detector(this)
        val camera = findViewById<Button>(R.id.B_Camara)
        val gallery = findViewById<Button>(R.id.B_Galeria)
        image = findViewById<ImageView>(R.id.Muestra_imagen)
        val detect = findViewById<Button>(R.id.B_Calculo)
        val results = findViewById<TextView>(R.id.T_Alimentos)
        val results_cal = findViewById<TextView>(R.id.Texto_calorias)
        bitmap = BitmapFactory.decodeResource(resources, R.drawable.default_image)
        bitmap = getScaledDownBitmap(bitmap, threshold, true)!!
        image!!.setImageBitmap(bitmap)
        camera.setOnClickListener{
            if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED){
                requestPermissions(arrayOf(android.Manifest.permission.CAMERA), MY_CAMERA_PERMISSION_CODE)
            }
            else{
                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(cameraIntent, MainActivity.CAMERA_RESULT)
            }
        }
        gallery.setOnClickListener{
            if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED){
                requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), MY_GALLERY_PERMISSION_CODE)
            }
            else{
                val galleryIntent = Intent(Intent.ACTION_PICK)
                galleryIntent.type = "image/*"
                startActivityForResult(galleryIntent, MainActivity.GALLERY_RESULT)
            }
        }
        detect.setOnClickListener{
            val result = detector.recognizeImage(bitmap)
            results.text = ""
            for (i in result){
                results.text = (results.text as String).plus(i.toString().plus("\n"))
                results_cal.text = ("Las calorias del alimento son: 295 calorias")
            }
            if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED){
                requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), MY_GALLERY_PERMISSION_CODE)
            }
            else{
                val galleryIntent = Intent(Intent.ACTION_PICK)
                galleryIntent.type = "image/*"
                startActivityForResult(galleryIntent, MainActivity.GALLERY_RESULT)
            }
        }
    }
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        height = image!!.height
        width = image!!.width
        if (height >= width) {
            threshold = height
        } else {
            threshold = width
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val results = findViewById<TextView>(R.id.T_Alimentos)

        if(requestCode == CAMERA_RESULT){
            if(resultCode == Activity.RESULT_OK && data !== null) {
                results.text = ""
                bitmap = data.extras!!.get("data") as Bitmap
                bitmap = getScaledDownBitmap (bitmap, threshold, true)!!
                image!!.setImageBitmap(bitmap)
            }
        }
        else if(requestCode == GALLERY_RESULT && data != null){
            results.text = ""
            val uri = data.data

            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
                bitmap = getScaledDownBitmap(bitmap, height, true)!!
                image!!.setImageBitmap(bitmap)
            }catch (e: IOException){
                e.printStackTrace()
            }


        }

    }
    private fun getScaledDownBitmap(
        bitmap: Bitmap,
        threshold: Int,
        isNecessaryToKeepOrig: Boolean,
    ): Bitmap? {
        val width = bitmap.width
        val height = bitmap.height
        var newWidth = width
        var newHeight = height
        if (width > height && width > threshold) {
            newWidth = threshold
            newHeight = (height * newWidth.toFloat() / width).toInt()
        }
        if (width in (height + 1)..threshold) {
            //the bitmap is already smaller than our required dimension, no need to resize it
            return bitmap
        }
        if (width < height && height > threshold) {
            newHeight = threshold
            newWidth = (width * newHeight.toFloat() / height).toInt()
        }
        if (height in (width + 1)..threshold) {
            //the bitmap is already smaller than our required dimension, no need to resize it
            return bitmap
        }
        if (width == height && width > threshold) {
            newWidth = threshold
            newHeight = newWidth
        }
        return if (width == height && width <= threshold) {
            //the bitmap is already smaller than our required dimension, no need to resize it
            bitmap
        } else getResizedBitmap(bitmap, newWidth, newHeight, isNecessaryToKeepOrig)
    }
    private fun getResizedBitmap(
        bm: Bitmap,
        newWidth: Int,
        newHeight: Int,
        isNecessaryToKeepOrig: Boolean,
    ): Bitmap? {
        val widthBitmap = bm.width
        val heightBitmap = bm.height
        val scaleWidth = newWidth.toFloat() / widthBitmap
        val scaleHeight = newHeight.toFloat() / heightBitmap
        // CREATE A MATRIX FOR THE MANIPULATION
        val matrix = Matrix()
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight)

        // "RECREATE" THE NEW BITMAP
        val resizedBitmap = Bitmap.createBitmap(bm, 0, 0, widthBitmap, heightBitmap, matrix, true)
        if (!isNecessaryToKeepOrig) {
            bm.recycle()
        }
        return resizedBitmap
    }
    data class Recognition(val label:String, val confidence:Float)  {

        override fun toString(): String {
            return "$label / $probabilityString"
        }

        private val probabilityString = String.format("%.1f%%", confidence * 100.0f)
    }
    class Detector(private val context: Context) {
        fun recognizeImage(bitmap: Bitmap): MutableList<Recognition> {
            val items = mutableListOf<Recognition>()
            val model = LiteModelAiyVisionClassifierFoodV11.newInstance(context)
            val image = TensorImage.fromBitmap(bitmap)
            val outputs = model.process(image).probabilityAsCategoryList.apply {
                sortByDescending { it.score }
            }.take(1)
            for (output in outputs) {
                items.add(Recognition(output.label, output.score))
            }
            model.close()
            return items

        }
    }
}