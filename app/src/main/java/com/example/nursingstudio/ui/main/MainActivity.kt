package com.example.nursingstudio.ui.main

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.nursingstudio.R
import com.example.nursingstudio.data.local.DataStoreManager
import com.example.nursingstudio.data.model.SocialItem
import com.example.nursingstudio.ui.auth.AuthActivity
import com.example.nursingstudio.ui.features.social.SocialAdapter
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var topAppBar: MaterialToolbar
    private lateinit var navController: NavController

    // Drawer header views
    private lateinit var imgHeaderProfile: ImageView
    private lateinit var tvHeaderName: TextView
    private lateinit var tvHeaderMobile: TextView
    private lateinit var tvDrawerSubscription: TextView

    // In variables ko add karein
    private lateinit var viewModel: MainViewModel
    private lateinit var dataStoreManager: DataStoreManager

    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val URL_YOUTUBE  = "https://youtube.com/@NursingStudio2026"
        private const val URL_WHATSAPP = "https://whatsapp.com/channel/0029Vb6Sjdq6BIEapKtNUE2L"
        private const val URL_TELEGRAM = "https://telegram.me/NursingStudio"
        private const val URL_ARATTAI  = "https://aratt.ai/@nursingstudio"
        private const val URL_INSTA    = "https://instagram.com/NursingStudio2026"
        private const val URL_TWITTER  = "https://twitter.com/"
        private const val URL_FACEBOOK = "https://facebook.com/"
        private const val URL_PLAYSTORE = "https://play.google.com/store/apps/details?id=com.example.nursingstudio"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Auth Check (Priority 1)
        val currentUser = auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }
        // 2. Initialize Core Components
        dataStoreManager = DataStoreManager(this)
        viewModel = MainViewModel(FirebaseFirestore.getInstance(), dataStoreManager)
        // 3. Setup UI & Navigation
        setupViews()
        setupNavigation()
        // 4. Data Sync & Observation (Modern Approach)
        viewModel.syncUserData(currentUser.uid)
        observeUserData()
    }
    private fun setupViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        topAppBar = findViewById(R.id.topAppBar)

        setSupportActionBar(topAppBar)

        val headerView = navigationView.getHeaderView(0)
        imgHeaderProfile = headerView.findViewById(R.id.imgHeaderProfile)
        tvHeaderName = headerView.findViewById(R.id.tvHeaderName)
        tvHeaderMobile = headerView.findViewById(R.id.tvHeaderMobile)
        tvDrawerSubscription = headerView.findViewById(R.id.tvDrawerSubscription)

        headerView.setOnClickListener {
            navController.navigate(R.id.nav_profile)
            drawerLayout.closeDrawer(GravityCompat.START)
        }
    }
    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Navigation UI ko setup karein
        bottomNavigation.setupWithNavController(navController)
        navigationView.setupWithNavController(navController)

        // Hamburger icon aur Title management
        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_home, R.id.nav_quiz, R.id.nav_pdf, R.id.nav_video, R.id.nav_profile),
            drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

// ⭐ FIX: Hamburger menu behavior ko force karne ke liye
        topAppBar.setNavigationOnClickListener {
            // Agar Drawer layout hai, toh check karo ki back jana hai ya drawer kholna hai
            val isTopLevelDestination = appBarConfiguration.topLevelDestinations.contains(navController.currentDestination?.id)

            if (isTopLevelDestination) {
                drawerLayout.openDrawer(GravityCompat.START)
            } else {
                // Agar aap chahte hain ki har screen se Hamburger Drawer hi khole (Industry Standard for Drawers)
                // Toh niche wali line use karein:
                drawerLayout.openDrawer(GravityCompat.START)

                // Agar aap chahte hain ki sub-screens par 'Back' arrow dikhe, toh 'navController.navigateUp()' use karein
            }
        }
        navController.addOnDestinationChangedListener { _, _, _ ->
            supportActionBar?.title = getString(R.string.nursing_studio)
        }

        // ⭐ SPECIAL HANDLING: Un items ke liye jo Fragment nahi hain
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_share -> shareApp()
                R.id.nav_social_dialog -> showSocialDialog()
                else -> {
                    // Baki sare items (Settings, Notice etc.) automatic handle honge agar ID match hai
                    val handled = NavigationUI.onNavDestinationSelected(menuItem, navController)
                    if (handled) drawerLayout.closeDrawer(GravityCompat.START)
                    return@setNavigationItemSelectedListener handled
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    // Hamburger Menu ke click ko handle karne ke liye mandatory override
    override fun onSupportNavigateUp(): Boolean {
        val appBarConfiguration = AppBarConfiguration(navController.graph, drawerLayout)
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp()
    }
    private fun openUrl(url: String, packageName: String? = null) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            packageName?.let { intent.setPackage(it) }
            startActivity(intent)
        } catch (_: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        }
    }

    private fun showSocialDialog() {
        val socialItems = listOf(
            SocialItem("YouTube Channel", R.drawable.ic_youtube),
            SocialItem("WhatsApp Channel", R.drawable.ic_whatsapp),
            SocialItem("Telegram Channel", R.drawable.ic_telegram),
            SocialItem("Arattai Channel", R.drawable.ic_arattai),
            SocialItem("Instagram", R.drawable.ic_instagram),
            SocialItem("Twitter", R.drawable.ic_twitter),
            SocialItem("Facebook", R.drawable.ic_facebook)
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
        sp.edit { putInt(key, sp.getInt(key, 0) + 1) }
    }
    private fun observeUserData() {
        // 2026 Gold Standard: Collecting Flows from DataStore safely
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                dataStoreManager.userName.collect { name ->
                    tvHeaderName.text = name ?: "Scholar"
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                dataStoreManager.userMobile.collect { mobile ->
                    tvHeaderMobile.text = mobile ?: ""
                }
            }
        }
        // NAYA: Subscription type ko observe karein
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                dataStoreManager.subscriptionType.collect { type ->
                    // tvDrawerSubscription aapka Drawer Header ka TextView hai
                    tvDrawerSubscription.text = "Plan: $type"

                    // Professional Touch: Agar Premium hai toh color badal dein
                    if (type == "Premium") {
                        tvDrawerSubscription.setTextColor(getColor(R.color.saffron))
                    } else {
                        tvDrawerSubscription.setTextColor(getColor(android.R.color.white))
                    }
                }
            }
        }
    }
    private fun shareApp() {
        val shareText = "Start smart preparation for nursing competitive exams with Nursing Studio.\n\nDownload: $URL_PLAYSTORE"
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(shareIntent, "Share App"))
    }
}