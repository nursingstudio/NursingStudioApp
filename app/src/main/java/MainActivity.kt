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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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

    // Firebase instance
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    companion object {
        private const val PROFILE_PREF = "profile_prefs"
        private const val KEY_PROFILE_IMAGE = "profile_image_base64"
        private const val URL_YOUTUBE  = "https://youtube.com/@risingbharat2025"
        private const val URL_WHATSAPP = "https://whatsapp.com/channel/0029Vb6Sjdq6BIEapKtNUE2L"
        private const val URL_TELEGRAM = "https://telegram.me/NursingStudio"
        private const val URL_ARATTAI  = "https://aratt.ai/@nursingstudio"
        private const val URL_INSTA    = "https://instagram.com/risingbharat2025"
        private const val URL_TWITTER  = "https://twitter.com/"
        private const val URL_FACEBOOK = "https://facebook.com/"
        private const val URL_PLAYSTORE = "https://play.google.com/store/apps/details?id=com.example.nursingstudio"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Session Check: Agar user logged out hai toh AuthActivity pe bhej do
        if (auth.currentUser == null) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        // Views Mapping
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        topAppBar = findViewById(R.id.topAppBar)

        setSupportActionBar(topAppBar)
        topAppBar.setNavigationOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }

        // Drawer HEADER Mapping
        val headerView = navigationView.getHeaderView(0)
        imgHeaderProfile = headerView.findViewById(R.id.imgHeaderProfile)
        tvHeaderName = headerView.findViewById(R.id.tvHeaderName)
        tvHeaderMobile = headerView.findViewById(R.id.tvHeaderMobile)
        tvDrawerSubscription = headerView.findViewById(R.id.tvDrawerSubscription)

        // 🔹 Naya Kaam: Firestore se data fetch karke SP mein save karna
        fetchUserDataFromFirestore()

        // Initial UI Update (From Local SP)
        updateDrawerHeader()

        headerView.setOnClickListener {
            loadFragment(MyPageFragment())
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        // Drawer menu clicks (Same as yours)
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_subscription -> loadFragment(SubscriptionFragment())
                R.id.nav_notice -> loadFragment(NoticeFragment())
                R.id.nav_social_dialog -> showSocialDialog() // Chhota pop-up
                R.id.nav_social_fragment -> loadFragment(SocialHandlesFragment()) // Pura sundar page
                R.id.nav_share -> shareApp()
                R.id.nav_settings -> loadFragment(SettingsFragment())
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // Bottom navigation clicks (Same as yours)
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> loadFragment(HomeFragment())
                R.id.nav_quiz -> loadFragment(QuizFragment())
                R.id.nav_pdf -> loadFragment(PdfFragment())
                R.id.nav_video -> loadFragment(VideoFragment())
                R.id.nav_mypage -> loadFragment(MyPageFragment())
                else -> false
            }
            true
        }

        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
            bottomNavigation.selectedItemId = R.id.nav_home
        }
    }

    // 🔹 Firestore se live data lekar SharedPreferences update karega
    private fun fetchUserDataFromFirestore() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("Users").document(uid).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                val name = doc.getString("name") ?: ""
                val mobile = doc.getString("mobile") ?: ""

                // Saving in SharedPreferences for offline speed
                getSharedPreferences("session", MODE_PRIVATE).edit().apply {
                    putString("reg_name", name)
                    putString("reg_mobile", mobile)
                    apply()
                }
                // Refreshing UI
                updateDrawerHeader()
            }
        }
    }

    fun updateDrawerHeader() {
        val session = getSharedPreferences("session", MODE_PRIVATE)
        val name = session.getString("reg_name", "Your Name")
        val mobile = session.getString("reg_mobile", "9999999999")
        val subType = session.getString("subscription_type", "Free")

        tvHeaderName.text = name
        tvHeaderMobile.text = mobile
        tvDrawerSubscription.text = if (subType == "Premium") "Premium Version" else "Free Version"

        // Profile image logic (Mypage se synchronized)
        val profileSp = getSharedPreferences(PROFILE_PREF, MODE_PRIVATE)
        val encoded = profileSp.getString(KEY_PROFILE_IMAGE, null)

        if (encoded != null) {
            try {
                val bytes = Base64.decode(encoded, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                imgHeaderProfile.setImageBitmap(bmp)
            } catch (e: Exception) {
                imgHeaderProfile.setImageResource(R.drawable.ic_person)
            }
        } else {
            // Default Logo if no image uploaded
            imgHeaderProfile.setImageResource(R.mipmap.ic_launcher_round)
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun openUrl(url: String, packageName: String? = null) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            packageName?.let { intent.setPackage(it) }
            startActivity(intent)
        } catch (e: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

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
                    0 -> openUrl(URL_YOUTUBE, "com.google.android.youtube")
                    1 -> openUrl(URL_WHATSAPP)
                    2 -> openUrl(URL_TELEGRAM, "org.telegram.messenger")
                    3 -> openUrl(URL_ARATTAI, "com.aratt.aratt")
                    4 -> openUrl(URL_INSTA, "com.instagram.android")
                    5 -> openUrl(URL_TWITTER)
                    6 -> openUrl(URL_FACEBOOK)
                }
                trackSocialClick(socialItems[which].title)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun trackSocialClick(channel: String) {
        val sp = getSharedPreferences("analytics", MODE_PRIVATE)
        val key = "social_click_${channel.lowercase().replace(" ", "_")}"
        sp.edit().putInt(key, sp.getInt(key, 0) + 1).apply()
    }

    private fun shareApp() {
        val shareText = "Start smart preparation for nursing competitive exams with Nursing Studio.\n\nDownload: $URL_PLAYSTORE"
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(shareIntent, "Share app via"))
    }
}