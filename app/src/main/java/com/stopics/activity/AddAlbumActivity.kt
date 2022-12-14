package com.stopics.activity

import android.annotation.SuppressLint
import android.app.ActionBar
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.stopics.R
import com.stopics.model.Album
import com.stopics.model.Picture
import com.stopics.storage.AlbumJSONFileStorage
import com.stopics.storage.StorageInstance
import java.io.File

class AddAlbumActivity : AppCompatActivity() {

    var pictureList = ArrayList<Picture>()
    val url = "http://51.68.95.247/gr-3-2/album.json"
    val url_image_1 = "http://51.68.95.247/gr-3-2/ski-1.png"
    val url_image_2 = "http://51.68.95.247/gr-3-2/ski-2.jpg"
    var msg : String? = ""
    var lastMsg = ""
    var cptDl = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.add_album)

        val btnAdd = findViewById<Button>(R.id.create_album)
        val btnImport = findViewById<Button>(R.id.import_album)


        btnAdd.setOnClickListener (View.OnClickListener {
            var new_id = addAlbum()
            Toast.makeText(this, R.string.create, Toast.LENGTH_SHORT).show()
            val intent = Intent(this, AlbumActivity::class.java).apply {
                putExtra("EXTRA_ID", new_id)
            }
            startActivity(intent)
        })

        btnImport.setOnClickListener {
            val queue = Volley.newRequestQueue(this)
            downloadImage(url_image_1)
            downloadImage(url_image_2)
            queue.add(request)
            queue.start()

        }

        val actionBar: ActionBar? = actionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun addAlbum(): Int {
        val title_text = findViewById<EditText>(R.id.title_add_album)
        val descr_text = findViewById<EditText>(R.id.descr_add_album)
        var storageAlbum = AlbumJSONFileStorage(this)
        var newAlbum = Album(
            1, title_text.text.toString(), true, descr_text.text.toString(),
            pictureList
        )

        return StorageInstance.get().AlbumList.jsonFileStorage.insert(newAlbum)

    }

    val request = JsonObjectRequest(
        Request.Method.GET,
        url,
        null,
        { res ->

            Log.d("JSON", res.toString())
            val json_value = AlbumJSONFileStorage(this)
            val obj = json_value.jsonToObject(res)
            val title_text = findViewById<EditText>(R.id.title_add_album)
            val descr_text = findViewById<EditText>(R.id.descr_add_album)
            title_text.setText(obj.name)
            descr_text.setText(obj.description)
            pictureList = obj.pictures_list

            //StorageInstance.get().AlbumList.jsonFileStorage.insert(obj)
        },
        { err ->
            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show()
            Log.e("ICI", "ERREUR")}
    )


    @SuppressLint("Range")
    fun downloadImage(url: String) {
        val directory = File(Environment.DIRECTORY_PICTURES)

        if(!directory.exists()){
            directory.mkdirs()
        }

        val downloadManager = this.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val downloadUri = Uri.parse(url)

        val request = DownloadManager.Request(downloadUri).apply {
            setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                .setAllowedOverRoaming(false)
                .setTitle(url.substring(url.lastIndexOf("/") + 1))
                .setDescription("")
                .setDestinationInExternalPublicDir(
                    directory.toString(),
                    url.substring(url.lastIndexOf("/") + 1)
                )

        }

        val downloadId = downloadManager.enqueue(request)
        val query = DownloadManager.Query().setFilterById(downloadId)
        Thread(Runnable {
            var downloading = true
            while (downloading) {
                val cursor: Cursor = downloadManager.query(query)
                cursor.moveToFirst()
                if(cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL){
                    downloading = false
                }
                val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                msg = statusMessage(url, directory, status)

                if(msg != lastMsg) {
                    this.runOnUiThread {
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    }
                    lastMsg = msg ?: ""
                cursor.close()

                }
            }
        }).start()
        Log.e("IMAGE","$directory" + File.separator + url.substring(
            url.lastIndexOf("/") + 1 ))


    }

    private fun statusMessage(url: String, directory: File, status: Int): String? {
        var msg = ""
        if(status == DownloadManager.STATUS_SUCCESSFUL){
            cptDl++
            Log.e("img",url.substring(url.lastIndexOf("/") + 1) )
        }
        if(cptDl < 2){
            msg = "importation en cours ..."
        }
        else {
            msg = "importation termin?? !"
        }

        return msg
    }
}