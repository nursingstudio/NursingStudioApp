package com.example.nursingstudio.ui.features.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.GravityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.example.nursingstudio.R
import com.example.nursingstudio.data.local.DataStoreManager
import com.example.nursingstudio.data.model.SocialItem
import com.example.nursingstudio.data.model.User
import com.example.nursingstudio.databinding.ActivityMainBinding
import com.example.nursingstudio.databinding.DrawerHeaderBinding
import com.example.nursingstudio.ui.features.auth.AuthActivity
import com.example.nursingstudio.ui.base.BaseActivity
import com.example.nursingstudio.ui.features.social.SocialAdapter
import com.example.nursingstudio.ui.features.profile.ProfileViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class MainActivity : BaseActivity() { // ✅ ⭐ 2026 GOLD STANDARD: Extends BaseActivity for unbreakable live runtime security checks

    private lateinit var binding: ActivityMainBinding
    private lateinit var headerBinding: DrawerHeaderBinding
    private lateinit var navController: NavController
    private lateinit var viewModel: MainViewModel
    private lateinit var dataStoreManager: DataStoreManager
    private val auth = FirebaseAuth.getInstance()

    private lateinit var profileViewModel: ProfileViewModel

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
        super.onCreate(savedInstanceState) // Dynamic BaseActivity hooks setup validation automatically

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        setupHeader()
        setupNavigation()
        // 4. Data Sync & Observation (Modern Approach)
        viewModel.syncUserData(currentUser.uid)
        observeUserData()

        // Initialize Shared Profile Engine inside Activity scope
        profileViewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
    }

    private fun setupHeader() {
        val headerView = binding.navigationView.getHeaderView(0)
        headerBinding = DrawerHeaderBinding.bind(headerView)

        headerView.setOnClickListener {
            navController.navigate(R.id.nav_profile)
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    private fun setupNavigation() {
        setSupportActionBar(binding.topAppBar)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)
        binding.navigationView.setupWithNavController(navController)

        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_home, R.id.nav_quiz, R.id.nav_pdf, R.id.nav_video, R.id.nav_profile),
            binding.drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.topAppBar.setNavigationOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        navController.addOnDestinationChangedListener { _, _, _ ->
            supportActionBar?.title = getString(R.string.nursing_studio)
        }

        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_share -> shareApp()
                R.id.nav_social_dialog -> showSocialDialog()
                else -> {
                    val handled = NavigationUI.onNavDestinationSelected(menuItem, navController)
                    if (handled) binding.drawerLayout.closeDrawer(GravityCompat.START)
                    return@setNavigationItemSelectedListener handled
                }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val appBarConfiguration = AppBarConfiguration(navController.graph, binding.drawerLayout)
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

        MaterialAlertDialogBuilder(this)
            .setTitle("Stay Connected")
            .setAdapter(SocialAdapter(this, socialItems)) { _, which ->
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

    // 🚀 2026 Enterprise Gold-Standard Stream Synchronizer Engine Pipeline
    private fun observeUserData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Channel 1: Real-time Profile Identity Ingestion Layer
                launch {
                    dataStoreManager.userName.collect { name ->
                        headerBinding.tvHeaderName.text = if (!name.isNullOrBlank()) name else "Scholar"
                    }
                }

                // Channel 2: Communications Payload Telemetry Sync
                launch {
                    dataStoreManager.userMobile.collect { mobile ->
                        headerBinding.tvHeaderMobile.text = if (!mobile.isNullOrBlank()) mobile else "No Mobile Linked"
                    }
                }

                // Channel 3: Modern Image Fallback Pipeline Engine (Real-time Mirroring Layer)
                launch {
                    profileViewModel.userData.observe(this@MainActivity) { user ->
                        user?.let { data ->
                            val profileUrl = data.profileImageUrl ?: ""
                            if (profileUrl.isNotBlank()) {
                                Glide.with(this@MainActivity)
                                    .load(profileUrl)
                                    .placeholder(R.drawable.ic_login_logo)
                                    .error(R.drawable.ic_login_logo)
                                    .circleCrop() // 2026 Premium Design UI feel
                                    .into(headerBinding.imgHeaderProfile)
                            } else {
                                headerBinding.imgHeaderProfile.setImageResource(R.drawable.ic_login_logo)
                            }
                        }
                    }
                }

                // Channel 4: Architectural Subscriptions Status Multi-State Evaluation
                launch {
                    dataStoreManager.subscriptionType.collect { type ->
                        val processType = type.ifBlank { "Free" }

                        // Direct explicit text assignments mapping
                        headerBinding.tvDrawerSubscription.text = processType.uppercase(Locale.ROOT)

                        if (processType.equals("Premium", ignoreCase = true)) {
                            headerBinding.tvDrawerSubscription.text = getString(R.string.premium)
                            headerBinding.tvDrawerSubscription.backgroundTintList = getColorStateList(R.color.brand_saffron_dark)
                            headerBinding.tvDrawerSubscription.setTextColor(getColor(R.color.brand_saffron_dark))
                        } else {
                            headerBinding.tvDrawerSubscription.text = getString(R.string.free)
                            headerBinding.tvDrawerSubscription.backgroundTintList = getColorStateList(R.color.brand_blue)
                            headerBinding.tvDrawerSubscription.setTextColor(getColor(R.color.brand_blue))
                        }
                    }
                }
// 🚀 NEW - Channel 5: Dynamic Identity Resolution Channel (NS-ID Link)
                launch {
                    dataStoreManager.uniqueNsId.collect { nsId ->
                        headerBinding.tvUniqueNsId.text = if (!nsId.isNullOrBlank()) {
                            nsId // Displays the direct global string synchronized value
                        } else {
                            "NS-2026-PENDING"
                        }
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