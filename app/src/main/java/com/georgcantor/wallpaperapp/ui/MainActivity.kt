package com.georgcantor.wallpaperapp.ui

import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.design.widget.TabLayout
import android.support.v4.view.GravityCompat
import android.support.v4.view.ViewPager
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.georgcantor.wallpaperapp.R
import com.georgcantor.wallpaperapp.ui.adapter.PagerAdapter
import com.georgcantor.wallpaperapp.ui.util.UtilityMethods

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var doubleTap = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        if (!UtilityMethods.isNetworkAvailable()) {
            Snackbar.make(findViewById<View>(android.R.id.content),
                    resources.getString(R.string.check_internet), Snackbar.LENGTH_LONG).show()
        }
        val myTitle = toolbar.getChildAt(0) as TextView

        val typeface = Typeface.createFromAsset(assets, "fonts/Montserrat-Regular.ttf")
        if (Build.VERSION.SDK_INT >= 16) {
            myTitle.typeface = Typeface.create("cursive", Typeface.NORMAL)
        } else {
            myTitle.setTypeface(typeface, Typeface.BOLD)
        }
        val toggle = ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer.setDrawerListener(toggle)
        toggle.syncState()

        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)
        navigationView.itemIconTintList = null
        val viewPager = findViewById<ViewPager>(R.id.viewPager)
        viewPager.adapter = PagerAdapter(supportFragmentManager)
        viewPager.offscreenPageLimit = 3
        val tabLayout = findViewById<TabLayout>(R.id.sliding_tabs)
        tabLayout.setupWithViewPager(viewPager)
    }

    public override fun onStart() {
        super.onStart()
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.KITKAT) {
            val searchItem = menu?.findItem(R.id.action_search)
            searchItem?.isVisible = false
        }

        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_search -> {
                openSearchActivity()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun openSearchActivity() {
        startActivity(Intent(this, SearchActivity::class.java))
        overridePendingTransition(R.anim.pull_in_right, R.anim.push_out_left)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_aston -> {
                val intentAston = Intent(this, FetchActivity::class.java)
                intentAston.putExtra(FetchActivity.FETCH_TYPE,
                        resources.getString(R.string.aston_walp))
                this.startActivity(intentAston)
                overridePendingTransition(R.anim.pull_in_right, R.anim.push_out_left)
            }
            R.id.nav_bentley -> {
                val intentBentley = Intent(this, FetchActivity::class.java)
                intentBentley.putExtra(FetchActivity.FETCH_TYPE,
                        resources.getString(R.string.bentley_walp))
                this.startActivity(intentBentley)
                overridePendingTransition(R.anim.pull_in_right, R.anim.push_out_left)
            }
            R.id.nav_porsche -> {
                val intentPorsche = Intent(this, FetchActivity::class.java)
                intentPorsche.putExtra(FetchActivity.FETCH_TYPE,
                        resources.getString(R.string.porsche))
                this.startActivity(intentPorsche)
                overridePendingTransition(R.anim.pull_in_right, R.anim.push_out_left)
            }
            R.id.nav_audi -> {
                val intentAudi = Intent(this, FetchActivity::class.java)
                intentAudi.putExtra(FetchActivity.FETCH_TYPE,
                        resources.getString(R.string.audi))
                this.startActivity(intentAudi)
                overridePendingTransition(R.anim.pull_in_right, R.anim.push_out_left)
            }
            R.id.nav_bugatti -> {
                val intentBugatti = Intent(this, FetchActivity::class.java)
                intentBugatti.putExtra(FetchActivity.FETCH_TYPE,
                        resources.getString(R.string.bugatti))
                this.startActivity(intentBugatti)
                overridePendingTransition(R.anim.pull_in_right, R.anim.push_out_left)
            }
            R.id.nav_mclaren -> {
                val intentMclaren = Intent(this, FetchActivity::class.java)
                intentMclaren.putExtra(FetchActivity.FETCH_TYPE,
                        resources.getString(R.string.mclaren_walp))
                this.startActivity(intentMclaren)
                overridePendingTransition(R.anim.pull_in_right, R.anim.push_out_left)
            }
            R.id.nav_ferrari -> {
                val intentFerrari = Intent(this, FetchActivity::class.java)
                intentFerrari.putExtra(FetchActivity.FETCH_TYPE,
                        resources.getString(R.string.ferrari))
                this.startActivity(intentFerrari)
                overridePendingTransition(R.anim.pull_in_right, R.anim.push_out_left)
            }
            R.id.nav_lambo -> {
                val intentLambo = Intent(this, FetchActivity::class.java)
                intentLambo.putExtra(FetchActivity.FETCH_TYPE,
                        resources.getString(R.string.lamborghini))
                this.startActivity(intentLambo)
                overridePendingTransition(R.anim.pull_in_right, R.anim.push_out_left)
            }
            R.id.nav_favorites -> {
                startActivity(Intent(this@MainActivity, FavoriteActivity::class.java))
                overridePendingTransition(R.anim.pull_in_right, R.anim.push_out_left)
            }
            R.id.nav_about -> {
                val intentAbout = Intent(this, AboutActivity::class.java)
                this.startActivity(intentAbout)
                overridePendingTransition(R.anim.pull_in_right, R.anim.push_out_left)
            }
            R.id.nav_license -> {
                val intentLicense = Intent(this, LicenseActivity::class.java)
                this.startActivity(intentLicense)
                overridePendingTransition(R.anim.pull_in_right, R.anim.push_out_left)
            }
        }
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        drawer.closeDrawer(GravityCompat.START)

        return true
    }

    override fun onBackPressed() {
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        when {
            drawer.isDrawerOpen(GravityCompat.START) -> drawer.closeDrawer(GravityCompat.START)
            doubleTap -> super.onBackPressed()
            else -> {
                Toast.makeText(this, this.resources.getString(R.string.press_back),
                        Toast.LENGTH_SHORT).show()
                doubleTap = true
                val handler = Handler()
                handler.postDelayed({ doubleTap = false }, 2000)
            }
        }
    }
}