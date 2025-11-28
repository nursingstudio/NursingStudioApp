package com.example.nursingstudio

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var topAppBar: MaterialToolbar

    companion object {
        private const val PROFILE_PREF = "profile_prefs"
        private const val KEY_PROFILE_IMAGE = "profile_image_base64"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Find views
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        topAppBar = findViewById(R.id.topAppBar)

        // Top app bar hamburger click
        topAppBar.setNavigationOnClickListener {
            drawerLayout.open()
        }

        // ---------- Drawer HEADER setup ----------
        val headerView = navigationView.getHeaderView(0)
        val imgHeaderProfile = headerView.findViewById<ImageView>(R.id.imgHeaderProfile)
        val tvHeaderName = headerView.findViewById<TextView>(R.id.tvHeaderName)
        val tvHeaderMobile = headerView.findViewById<TextView>(R.id.tvHeaderMobile)
        val tvDrawerSubscription =
            headerView.findViewById<TextView>(R.id.tvDrawerSubscription)

        // Session prefs: name, mobile, subscription type
        val session = getSharedPreferences("session", MODE_PRIVATE)
        val name = session.getString("reg_name", "Your Name")
        val mobile = session.getString("reg_mobile", "9999999999")
        val subType = session.getString("subscription_type", "Free")

        tvHeaderName.text = name
        tvHeaderMobile.text = mobile
        tvDrawerSubscription.text =
            if (subType == "Premium") "Premium Version" else "Free Version"

        // Profile image from profile_prefs (MyPage se)
        val profileSp = getSharedPreferences(PROFILE_PREF, MODE_PRIVATE)
        val encoded = profileSp.getString(KEY_PROFILE_IMAGE, null)
        if (encoded != null) {
            try {
                val bytes = Base64.decode(encoded, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                imgHeaderProfile.setImageBitmap(bmp)
            } catch (e: Exception) {
                e.printStackTrace()
                imgHeaderProfile.setImageResource(R.drawable.ic_person)
            }
        } else {
            imgHeaderProfile.setImageResource(R.drawable.ic_person)
        }

        // Header click -> My Page
        headerView.setOnClickListener {
            loadFragment(MyPageFragment())
            drawerLayout.close()
        }
        // ----------------------------------------

        // Drawer item clicks
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_subscription -> {
                    loadFragment(SubscriptionFragment())
                }
                R.id.nav_notice -> {
                    loadFragment(NoticeFragment())
                }
                R.id.nav_social -> {
                    // TODO: yahan social links daalna hai
                    val url = "https://youtube.com" // apna actual link
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
                R.id.nav_share -> {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "Try Nursing Studio app for nursing exam preparation!"
                        )
                    }
                    startActivity(Intent.createChooser(shareIntent, "Share app via"))
                }
                R.id.nav_settings -> {
                    Toast.makeText(this, "Settings coming soon", Toast.LENGTH_SHORT).show()
                }
            }
            drawerLayout.close()
            true
        }

        // Bottom navigation clicks
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_quiz -> {
                    loadFragment(QuizFragment())
                    true
                }
                R.id.nav_pdf -> {
                    loadFragment(PdfFragment())
                    true
                }
                R.id.nav_video -> {
                    loadFragment(VideoFragment())
                    true
                }
                R.id.nav_mypage -> {
                    loadFragment(MyPageFragment())
                    true
                }
                else -> false
            }
        }

        // Default screen
        if (savedInstanceState == null) {
            loadFragment(QuizFragment())
            bottomNavigation.selectedItemId = R.id.nav_quiz
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
