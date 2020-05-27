package com.bradev.tcr

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.Volley
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import dmax.dialog.SpotsDialog
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var imageBitmap: Bitmap
    private lateinit var imageUri: Uri
    private lateinit var dialog: AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dialog = SpotsDialog.Builder().setContext(this).setMessage("Processing...").build()

        load_btn.setOnClickListener {
            CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .start(this)
        }

        btn_edit.setOnClickListener {
            CropImage.activity(imageUri)
                .setGuidelines(CropImageView.Guidelines.ON)
                .setCropShape(CropImageView.CropShape.RECTANGLE)
                .start(this)
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tValue.text = progress.toString()
                threshold(imageBitmap, progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val data = getMatrix()
                uploadImg(data)
            }

        })

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode === CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(data)
            if (resultCode === Activity.RESULT_OK) {
                imageUri = result.uri
                imageBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)
                imageBitmap = Bitmap.createScaledBitmap(imageBitmap, 28, 28, false)
                seekBar.progress = 100
                help_message.visibility = View.GONE
                mainLayout.visibility = View.VISIBLE
                threshold(imageBitmap)
                uploadImg(getMatrix())
            } else if (resultCode === CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                val error = result.error
            }
        }
    }

    private fun threshold(bitmap: Bitmap, thresh: Int = 100): Bitmap {
        val h = bitmap.height
        val w = bitmap.width
        val newBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        for (i in 0 until w) {
            for (j in 0 until h) {
                val pixel = bitmap.getPixel(i, j)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                val p = (r + g + b) / 3
                if (p > thresh) {
                    newBitmap.setPixel(i, j, Color.argb(255, 0, 0, 0))
                } else {
                    newBitmap.setPixel(i, j, Color.argb(255, 255, 255, 255))
                }
            }
        }
        imageView.setImageBitmap(newBitmap)
        return newBitmap
    }

    private fun getMatrix(): String {
        val bitmap = (imageView.drawable as BitmapDrawable).bitmap

        var matrix = Array(28) { IntArray(28) }
        var string = ""
        for (i in 0..27) {
            for (j in 0..27) {
                val p = bitmap.getPixel(i, j)
                matrix[i][j] = Color.red(p) / 255
            }
            string += matrix[i].joinToString() + ", "
        }
        Log.d("dataToSend: ", string.replace("\\s".toRegex(), "").dropLast(1))
        return string.replace("\\s".toRegex(), "").dropLast(1)
    }

    private fun uploadImg(data: String) {
        dialog.show()
        val url = "http://tifinagh.herokuapp.com/api"
        val request = UploadImage(Request.Method.POST, url, Response.Listener {
            Log.d("ResponseListener", it)
            val jsonObject = JSONObject(it)
            val labels = jsonObject.getJSONArray("label")
            val proba = jsonObject.getDouble("probability")
            if (proba >= 50) {
                textResult.text = "label: " + labels[0].toString() + "\nprobability: " + proba + "%"
                tv_label.text = labels[0].toString()
                tv_label_tifinagh.text = labels[1].toString()
                tv_label_arabic.text = labels[2].toString()
                tv_label_latin.text = labels[3].toString()
            } else {
                Toast.makeText(applicationContext, "Unknown character", Toast.LENGTH_LONG).show()
            }
            dialog.cancel()
        }, Response.ErrorListener {
            Toast.makeText(applicationContext, "Connection problem", Toast.LENGTH_LONG).show()
            Log.e("VolleyError: ", it.message)
            dialog.cancel()
        })
        request.data = data
        Volley.newRequestQueue(this).add(request)
    }

}

