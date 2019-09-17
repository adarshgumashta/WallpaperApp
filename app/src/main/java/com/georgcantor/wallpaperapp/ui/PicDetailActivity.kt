package com.georgcantor.wallpaperapp.ui

import android.Manifest
import android.annotation.TargetApi
import android.app.DownloadManager
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.AndroidRuntimeException
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.georgcantor.wallpaperapp.R
import com.georgcantor.wallpaperapp.model.Hit
import com.georgcantor.wallpaperapp.model.local.db.DatabaseHelper
import com.georgcantor.wallpaperapp.ui.adapter.TagAdapter
import com.georgcantor.wallpaperapp.ui.util.UtilityMethods
import com.google.gson.Gson
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.CropCircleTransformation
import kotlinx.android.synthetic.main.fragment_detail.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.*

class PicDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PIC = "picture"
        const val ORIGIN = "caller"
    }

    private var hit: Hit? = null
    private val tags = ArrayList<String>()
    private var first = 0
    private lateinit var tagAdapter: TagAdapter
    private var file: File? = null
    private var tagTitle: TextView? = null
    private var permissionCheck: Int = 0
    private var db: DatabaseHelper? = null
    private var isCallerCollection = false
    private var pathOfFile: String? = null
    private lateinit var prefs: SharedPreferences
    private lateinit var picture: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        setContentView(R.layout.fragment_detail)

        progressAnimationView?.visibility = View.VISIBLE
        progressAnimationView?.playAnimation()
        progressAnimationView?.loop(true)
        db = DatabaseHelper(this)

        prefs = getSharedPreferences("my_prefs", Context.MODE_PRIVATE)
        picture = prefs.getString("picture", "") ?: ""

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        initView()

        fabDownload.setOnClickListener {
            checkWallpaperPermission()
            val uri = Uri.fromFile(file)
            pathOfFile = UtilityMethods.getPath(applicationContext, uri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                SetWallpaperTask().execute()
            } else {
                setAsWallpaper()
            }
        }
    }

    private fun setAsWallpaper() {
        val uri = Uri.fromFile(file)
        val intent = Intent(Intent.ACTION_ATTACH_DATA)
        intent.setDataAndType(uri, resources.getString(R.string.image_jpg))
        intent.putExtra(
            resources.getString(R.string.mimeType),
            resources.getString(R.string.image_jpg)
        )

        startActivityForResult(
            Intent.createChooser(
                intent,
                resources.getString(R.string.Set_As)
            ), 200
        )
    }

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Toast.makeText(
                context, tags[0] + resources.getString(R.string.down_complete),
                Toast.LENGTH_SHORT
            ).show()
            downloadAnimationView?.loop(false)
            downloadAnimationView?.visibility = View.GONE
        }
    }

    inner class SetWallpaperTask : AsyncTask<String, Void, Bitmap>() {
        @TargetApi(Build.VERSION_CODES.KITKAT)
        override fun doInBackground(vararg params: String): Bitmap? {
            var result: Bitmap? = null
            try {
                result = Picasso.with(applicationContext)
                    .load(hit?.imageURL)
                    .get()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            return result
        }

        @RequiresApi(Build.VERSION_CODES.KITKAT)
        override fun onPostExecute(result: Bitmap) {
            super.onPostExecute(result)

            val wallpaperManager = WallpaperManager.getInstance(baseContext)
            run {
                try {
                    startActivity(
                        Intent(
                            wallpaperManager.getCropAndSetWallpaperIntent(
                                getImageUri(
                                    result,
                                    applicationContext
                                )
                            )
                        )
                    )
                } catch (e: IllegalArgumentException) {
                    val bitmap = MediaStore.Images.Media.getBitmap(
                        contentResolver,
                        getImageUri(result, applicationContext)
                    )
                    WallpaperManager.getInstance(this@PicDetailActivity).setBitmap(bitmap)
                }
            }
            Toast.makeText(
                this@PicDetailActivity, resources.getString(R.string.wallpaper_is_install),
                Toast.LENGTH_SHORT
            ).show()

            recreate()
        }

        override fun onPreExecute() {
            super.onPreExecute()
            progressAnimationView?.visibility = View.VISIBLE
            progressAnimationView?.playAnimation()
            progressAnimationView?.loop(true)
        }
    }

    private fun getImageUri(inImage: Bitmap, inContext: Context): Uri {
        val bytes = ByteArrayOutputStream()
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(
            inContext.contentResolver,
            inImage, "Title", null
        )
        return Uri.parse(path)
    }

    private fun initView() {
        permissionCheck = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        if (intent.hasExtra(EXTRA_PIC)) {
            hit = intent.getParcelableExtra(EXTRA_PIC)
        } else {
            throw IllegalArgumentException("Detail activity must receive a Hit parcelable")
        }
        hit?.let {
            var title = it.tags
            while (title.contains(",")) {
                val element = title.substring(0, title.indexOf(","))
                tags.add(element)
                first = title.indexOf(",")
                title = title.substring(++first)
            }
            tags.add(title)
        }

        tagTitle?.text = tags[0]
        tagsRecyclerView.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL, false
        )
        tagAdapter = TagAdapter(this)
        tagAdapter.setTagList(tags)
        tagsRecyclerView.adapter = tagAdapter
        file = File(
            Environment.getExternalStoragePublicDirectory(
                "/" + resources
                    .getString(R.string.app_name)
            ), hit?.id.toString() + resources
                .getString(R.string.jpg)
        )

        if (intent.hasExtra(ORIGIN)) {
            Picasso.with(this)
                .load(file)
                .placeholder(R.drawable.plh)
                .into(detailImageView)
            isCallerCollection = true
        } else {
            Picasso.with(this)
                .load(hit?.webformatURL)
                .placeholder(R.drawable.plh)
                .into(detailImageView, object : Callback {
                    override fun onSuccess() {
                        progressAnimationView?.loop(false)
                        progressAnimationView?.visibility = View.GONE
                    }

                    override fun onError() {
                        progressAnimationView?.loop(false)
                        progressAnimationView?.visibility = View.GONE
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(hit?.pageURL)))
                        finish()
                    }
                })
        }

        nameTextView.text = hit?.user
        downloadsTextView.text = hit?.downloads.toString()
        favoritesTextView.text = hit?.favorites.toString()
        if (!UtilityMethods.isNetworkAvailable) {
            Picasso.with(this)
                .load(R.drawable.memb)
                .transform(CropCircleTransformation())
                .into(userImageView)
        } else {
            hit?.let {
                if (it.userImageURL.isNotEmpty()) {
                    Picasso.with(this)
                        .load(hit?.userImageURL)
                        .transform(CropCircleTransformation())
                        .into(userImageView)
                } else {
                    Picasso.with(this)
                        .load(R.drawable.memb)
                        .transform(CropCircleTransformation())
                        .into(userImageView)
                }
            }
        }
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        registerReceiver(downloadReceiver, filter)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_fav, menu)
        db?.let {
            if (it.containFav(hit?.previewURL.toString())) {
                menu.findItem(R.id.action_add_to_fav).setIcon(R.drawable.ic_star_red_24dp)
            }
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        db?.let { db ->
            when (item.itemId) {
                android.R.id.home -> onBackPressed()
                R.id.action_add_to_fav -> if (!db.containFav(hit?.previewURL.toString())) {
                    hit?.let { addToFavorite(it.previewURL.toString(), it.pageURL.toString(), it) }
                    item.setIcon(R.drawable.ic_star_red_24dp)
                    Toast.makeText(this, R.string.add_to_fav_toast, Toast.LENGTH_SHORT).show()
                } else {
                    db.deleteFromFavorites(hit?.previewURL.toString())
                    item.setIcon(R.drawable.ic_star_border_black_24dp)
                    Toast.makeText(this, R.string.del_from_fav_toast, Toast.LENGTH_SHORT).show()
                }
                R.id.action_share -> try {
                    val intent = Intent(Intent.ACTION_SEND)
                    intent.type = "text/plain"
                    intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
                    val sAux = hit?.imageURL
                    intent.putExtra(Intent.EXTRA_TEXT, sAux)
                    startActivity(Intent.createChooser(intent, getString(R.string.choose_share)))
                } catch (e: AndroidRuntimeException) {
                    e.printStackTrace()
                    Toast.makeText(this, "Can not share image", Toast.LENGTH_SHORT).show()
                }
                R.id.action_download -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        downloadPictureQ(hit?.imageURL ?: "")
                    } else {
                        val uri = hit?.imageURL
                        val imageUri = Uri.parse(uri)
                        downloadPicture(imageUri)
                    }
                }
                else -> {
                }
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun downloadPicture(uri: Uri): Long {
        downloadAnimationView?.visibility = View.VISIBLE
        downloadAnimationView?.playAnimation()
        downloadAnimationView?.loop(true)

        val downloadReference: Long
        val downloadManager =
            getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        var name = Environment.getExternalStorageDirectory().absolutePath
        name += "/YourDirectoryName/"

        val request = DownloadManager.Request(uri)

        try {
            request.setTitle(tags[0] + resources.getString(R.string.down))
            request.setDescription(resources.getString(R.string.down_wallpapers))
            if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                request.setDestinationInExternalPublicDir(
                    "/" + resources
                        .getString(R.string.app_name), hit?.id.toString() + resources
                        .getString(R.string.jpg)
                )
            }
        } catch (e: IllegalStateException) {
            Toast.makeText(this, R.string.something_went_wrong, Toast.LENGTH_LONG).show()
        }
        downloadReference = downloadManager.enqueue(request)

        return downloadReference
    }

    private fun downloadPictureQ(url: String) {
        downloadAnimationView?.visibility = View.VISIBLE
        downloadAnimationView?.playAnimation()
        downloadAnimationView?.loop(true)

        val name = UtilityMethods.getImageNameFromUrl(url)

        val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager?
        val request = DownloadManager.Request(Uri.parse(url))

        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
            .setAllowedOverRoaming(false)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)

        downloadManager?.enqueue(request)
        val editor = getSharedPreferences("my_prefs", Context.MODE_PRIVATE)?.edit()
        editor?.putString("picture", hit?.previewURL)
        editor?.apply()
    }

    private fun checkWallpaperPermission() {
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SET_WALLPAPER), 103)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            this.recreate()
        } else {
            val intent = Intent()
            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            val uri = Uri.fromParts("package", packageName, null)
            intent.data = uri
            startActivity(intent)
            finish()
            Toast.makeText(this, R.string.you_need_perm_toast, Toast.LENGTH_LONG).show()
        }
    }

    private fun addToFavorite(imageUrl: String, hdUrl: String, hit: Hit) {
        val gson = Gson()
        val toStoreObject = gson.toJson(hit)
        db?.insertToFavorites(imageUrl, hdUrl, toStoreObject)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.pull_in_left, R.anim.push_out_right)
    }

}
