package com.georgcantor.wallpaperapp.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.app.WallpaperManager
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.georgcantor.wallpaperapp.R
import com.georgcantor.wallpaperapp.model.CommonPic
import com.georgcantor.wallpaperapp.model.local.db.DatabaseHelper
import com.georgcantor.wallpaperapp.ui.adapter.TagAdapter
import com.georgcantor.wallpaperapp.ui.util.*
import com.georgcantor.wallpaperapp.viewmodel.DetailsViewModel
import com.squareup.picasso.Picasso
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_detail.*
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.core.parameter.parametersOf
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.*

class DetailsActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PIC = "picture"
        const val PREF_BOOLEAN = "is_set_wall"
        const val MY_PREFS = "my_prefs"
        const val SIZE_IN_BYTES = 9999999
    }

    private var pic: CommonPic? = null
    private val tags = ArrayList<String>()
    private var first = 0
    private lateinit var tagAdapter: TagAdapter
    private var file: File? = null
    private var tagTitle: TextView? = null
    private var permissionCheck: Int = 0
    private var db: DatabaseHelper? = null
    private var pathOfFile: String? = null
    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var viewModel: DetailsViewModel

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        setContentView(R.layout.activity_detail)

        viewModel = getViewModel { parametersOf() }
        progressAnimationView?.showAnimation()
        db = DatabaseHelper(this)

        editor = getSharedPreferences(MY_PREFS, Context.MODE_PRIVATE).edit()
        prefs = getSharedPreferences(MY_PREFS, Context.MODE_PRIVATE)

        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(this, R.color.colorPrimary)))

        initView()

        fabSetWall.setOnClickListener {
            if (this.isNetworkAvailable()) {
                if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                    val uri = Uri.fromFile(file)
                    pathOfFile = UtilityMethods.getPath(applicationContext, uri)
                    progressAnimationView?.showAnimation()
                    setWallAsync()
                } else {
                    editor.putBoolean(PREF_BOOLEAN, true)
                    editor.apply()
                    checkSavingPermission()
                }
            } else {
                longToast(getString(R.string.no_internet))
            }
        }
    }

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                shortToast(tags[0] + getString(R.string.down_complete))
                downloadAnimationView?.hideAnimation()
            } catch (e: IndexOutOfBoundsException) {
                downloadAnimationView?.hideAnimation()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun setWallAsync() {
        progressAnimationView?.showAnimation()

        val disposable = getBitmapAsync()?.subscribe({
            val wallpaperManager = WallpaperManager.getInstance(baseContext)
            it?.let { bitmap ->
                getImageUri(bitmap, applicationContext).subscribe({ uri ->
                    try {
                        startActivity(
                            Intent(
                                wallpaperManager.getCropAndSetWallpaperIntent(
                                    uri
                                )
                            )
                        )
                    } catch (e: IllegalArgumentException) {
                        try {
                            it.let { bitMap ->
                                getImageUri(bitMap, applicationContext).subscribe({ uri ->
                                    val bitmap2 = MediaStore.Images.Media.getBitmap(
                                        contentResolver,
                                        uri
                                    )
                                    WallpaperManager.getInstance(this@DetailsActivity)
                                        .setBitmap(bitmap2)
                                }, {
                                    shortToast(getString(R.string.something_went_wrong))
                                })
                            }
                        } catch (e: OutOfMemoryError) {
                            shortToast(getString(R.string.something_went_wrong))
                        }
                    }
                }, { throwable ->
                    longToast(throwable.message.toString())
                })
            }
            shortToast(getString(R.string.wallpaper_is_install))
            recreate()
        }, {
            longToast(getString(R.string.something_went_wrong))
        })

        disposable?.let(DisposableManager::add)
    }

    private fun getBitmapAsync(): Observable<Bitmap?>? {
        return Observable.fromCallable {
            var result: Bitmap? = null
            try {
                result = Picasso.with(applicationContext)
                    .load(pic?.imageURL)
                    .get()
            } catch (e: IOException) {
                shortToast(getString(R.string.something_went_wrong))
            }
            result
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    private fun getImageUri(inImage: Bitmap, inContext: Context): Observable<Uri> {
        return Observable.fromCallable {
            val bytes = ByteArrayOutputStream()
            inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
            val path = MediaStore.Images.Media.insertImage(
                inContext.contentResolver,
                inImage, "Title", null
            )
            Uri.parse(path)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    @SuppressLint("CheckResult")
    private fun initView() {
        permissionCheck = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        if (intent.hasExtra(EXTRA_PIC)) {
            pic = intent.getParcelableExtra(EXTRA_PIC)
        } else {
            shortToast(getString(R.string.something_went_wrong))
        }
        pic?.let {
            var title = it.tags
            while (title?.contains(",") == true) {
                val element = title.substring(0, title.indexOf(","))
                tags.add(element)
                first = title.indexOf(",")
                title = title.substring(++first)
            }
            title?.let(tags::add)
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
            ), pic?.id.toString() + resources
                .getString(R.string.jpg)
        )

        pic?.let { pic ->
            if (isNetworkAvailable()) {
                val disposable = viewModel.imageSize(pic)
                    .subscribe({ size ->
                        loadImage(
                                if (size < SIZE_IN_BYTES) pic.fullHDURL ?: "" else pic.url ?: "",
                                resources.getDrawable(R.drawable.plh),
                                detailImageView,
                                progressAnimationView
                        )
                    }, {
                        shortToast(getString(R.string.something_went_wrong))
                    })

                DisposableManager.add(disposable)
            } else {
                longToast(getString(R.string.no_internet))
                pic.fullHDURL?.let {
                    loadImage(
                        it,
                        resources.getDrawable(R.drawable.plh),
                        detailImageView,
                        progressAnimationView
                    )
                }
            }

            loadCircleImage(
                    if (pic.userImageURL?.isNotEmpty() == true) pic.userImageURL ?: "" else pic.url ?: "",
                    userImageView
            )
        }
        nameTextView.text = pic?.user
        downloadsTextView.text = pic?.downloads.toString()
        favoritesTextView.text = pic?.favorites.toString()

        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        registerReceiver(downloadReceiver, filter)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_fav, menu)
        db?.let {
            if (it.containFav(pic?.url.toString())) {
                menu.findItem(R.id.action_add_to_fav).setIcon(R.drawable.ic_star_red_24dp)
            }
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            R.id.action_add_to_fav -> {
                pic?.let(viewModel::setFavoriteStatus)
                if (viewModel.isImageFavorite.value == true) {
                    item.setIcon(R.drawable.ic_star_red_24dp)
                } else {
                    item.setIcon(R.drawable.ic_star_border)
                }
            }
            R.id.action_share -> pic?.url?.let(viewModel::share)
            R.id.action_download -> startDownloading()
        }

        return super.onOptionsItemSelected(item)
    }

    private fun startDownloading() {
        if (!this.isNetworkAvailable()) {
            shortToast(getString(R.string.no_internet))
            return
        }
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                pic?.url?.let { viewModel.downloadPictureQ(it, downloadAnimationView) }
            } else {
                pic?.let { viewModel.downloadPicture(it, tags, downloadAnimationView) }
            }
        } else {
            checkSavingPermission()
        }
    }

    private fun checkSavingPermission() {
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            val requestCode = 102
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                requestCode
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            val isSetWall = prefs.getBoolean(PREF_BOOLEAN, false)
            if (isSetWall) setWallAsync() else pic?.let { viewModel.downloadPicture(it, tags, downloadAnimationView) }
        } else {
            val intent = Intent()
            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            val uri = Uri.fromParts("package", packageName, null)
            intent.data = uri
            startActivity(intent)
            finish()
            longToast(getString(R.string.you_need_perm_toast))
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.pull_in_left, R.anim.push_out_right)
    }

    public override fun onDestroy() {
        try {
            unregisterReceiver(downloadReceiver)
        } catch (e: Exception) {
            shortToast(getString(R.string.something_went_wrong))
        }
        DisposableManager.dispose()
        super.onDestroy()
    }

}
