package com.example.nursingstudio

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var topAppBar: MaterialToolbar

    // Drawer header views
    private lateinit var imgHeaderProfile: ImageView
    private lateinit var tvHeaderName: TextView
    private lateinit var tvHeaderMobile: TextView
    private lateinit var tvDrawerSubscription: TextView

    companion object {
        private const val PROFILE_PREF = "profile_prefs"
        private const val KEY_PROFILE_IMAGE = "profile_image_base64"

        // 👉 Tumhare social links
        private const val URL_YOUTUBE  = "https://youtube.com/@risingbharat2025"
        private const val URL_WHATSAPP = "https://whatsapp.com/channel/0029Vb6Sjdq6BIEapKtNUE2L"
        private const val URL_TELEGRAM = "https://telegram.me/NursingStudio"
        private const val URL_ARATTAI  = "https://aratt.ai/@nursingstudio"
        private const val URL_INSTA    = "https://instagram.com/risingbharat2025"
        private const val URL_TWITTER  = "https://twitter.com/"          // TODO: handle update
        private const val URL_FACEBOOK = "https://facebook.com/"         // TODO: handle update

        // Play Store link – baad me actual package se update kar lena
        private const val URL_PLAYSTORE =
            "https://play.google.com/store/apps/details?id=com.example.nursingstudio"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Views
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        topAppBar = findViewById(R.id.topAppBar)

        // Toolbar ko app bar bana + title set karo
        setSupportActionBar(topAppBar)

        // Hamburger click -> open drawer
        topAppBar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Drawer HEADER
        val headerView = navigationView.getHeaderView(0)
        imgHeaderProfile = headerView.findViewById(R.id.imgHeaderProfile)
        tvHeaderName = headerView.findViewById(R.id.tvHeaderName)
        tvHeaderMobile = headerView.findViewById(R.id.tvHeaderMobile)
        tvDrawerSubscription = headerView.findViewById(R.id.tvDrawerSubscription)

        // Pehli baar data set karo
        updateDrawerHeader()

        // Header click -> My Page
        headerView.setOnClickListener {
            loadFragment(MyPageFragment())
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        // Drawer menu clicks
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_subscription -> {
                    loadFragment(SubscriptionFragment())
                }
                R.id.nav_notice -> {
                    loadFragment(NoticeFragment())
                }
                R.id.nav_social -> {
                    loadFragment(SocialHandlesFragment())
                }
                R.id.nav_share -> {
                    shareApp()
                }
                R.id.nav_settings -> {
                    loadFragment(SettingsFragment())
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // Bottom navigation clicks
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.nav_quiz -> {          // text ko menu XML me "Test Series" likha hai
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
            loadFragment(HomeFragment())
            bottomNavigation.selectedItemId = R.id.nav_home
        }
    }

    // 🔹 Name, mobile, subscription & photo sab refresh karega
    fun updateDrawerHeader() {
        val session = getSharedPreferences("session", MODE_PRIVATE)
        val name = session.getString("reg_name", "Your Name")
        val mobile = session.getString("reg_mobile", "9999999999")
        val subType = session.getString("subscription_type", "Free")

        tvHeaderName.text = name
        tvHeaderMobile.text = mobile
        tvDrawerSubscription.text =
            if (subType == "Premium") "Premium Version" else "Free Version"

        // Profile image profile_prefs se
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
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    // 👉 Generic URL opener (app + fallback browser)
    private fun openUrl(url: String, packageName: String? = null) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            if (packageName != null) {
                intent.setPackage(packageName)
            }
            startActivity(intent)
        } catch (e: Exception) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (e2: Exception) {
                Toast.makeText(this, "Unable to open link", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 👉 Icon + text wala Social Media dialog (SocialAdapter use karega)
    private fun showSocialDialog() {
        val socialItems = listOf(
            SocialItem("YouTube Channel",   R.drawable.ic_youtube),
            SocialItem("WhatsApp Channel",  R.drawable.ic_whatsapp),
            SocialItem("Telegram Channel",  R.drawable.ic_telegram),
            SocialItem("Arattai Channel",   R.drawable.ic_arattai),
            SocialItem("Instagram",         R.drawable.ic_instagram),
            SocialItem("Twitter",           R.drawable.ic_twitter),
            SocialItem("Facebook",          R.drawable.ic_facebook)
        )

        val adapter = SocialAdapter(this, socialItems)

        MaterialAlertDialogBuilder(this)
            .setTitle("Stay Connected")
            .setAdapter(adapter) { _, which ->
                when (which) {
                    0 -> { trackSocialClick("YouTube");  openUrl(URL_YOUTUBE, "com.google.android.youtube") }
                    1 -> { trackSocialClick("WhatsApp"); openUrl(URL_WHATSAPP) }
                    2 -> { trackSocialClick("Telegram"); openUrl(URL_TELEGRAM, "org.telegram.messenger") }
                    3 -> { trackSocialClick("Arattai");  openUrl(URL_ARATTAI, "com.aratt.aratt") }
                    4 -> { trackSocialClick("Instagram");openUrl(URL_INSTA, "com.instagram.android") }
                    5 -> { trackSocialClick("Twitter");  openUrl(URL_TWITTER, "com.twitter.android") }
                    6 -> { trackSocialClick("Facebook"); openUrl(URL_FACEBOOK, "com.facebook.katana") }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun trackSocialClick(channel: String) {
        val sp = getSharedPreferences("analytics", MODE_PRIVATE)
        val key = "social_click_${channel.lowercase()}"
        val newCount = sp.getInt(key, 0) + 1
        sp.edit().putInt(key, newCount).apply()

        Log.d("SocialAnalytics", "Clicked $channel, total = $newCount")
    }

    // 👉 Share App – tumhara custom text
    private fun shareApp() {
        val shareText = """
 Start smart preparation for all nursing competitive exams. 👩‍⚕️👨‍⚕️
"Nursing Studio" app brings together Online Test Series, PDFs and videos.

Download link:
$URL_PLAYSTORE
        """.trimIndent()

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(shareIntent, "Share app via"))
    }
}
