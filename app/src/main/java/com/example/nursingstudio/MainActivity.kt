package com.example.nursingstudio

import android.os.Bundle
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Find Views
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        topAppBar = findViewById(R.id.topAppBar)

        // Top bar navigation button (hamburger menu)
        topAppBar.setNavigationOnClickListener {
            drawerLayout.open()
        }

        // Drawer item clicks
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_subscription -> {
                    Toast.makeText(this, "Subscription clicked", Toast.LENGTH_SHORT).show()
                }
                R.id.nav_notice -> {
                    Toast.makeText(this, "Notice Board clicked", Toast.LENGTH_SHORT).show()
                }
                R.id.nav_social -> {
                    Toast.makeText(this, "Social Links clicked", Toast.LENGTH_SHORT).show()
                }
                R.id.nav_share -> {
                    Toast.makeText(this, "Share App clicked", Toast.LENGTH_SHORT).show()
                }
                R.id.nav_settings -> {
                    Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show()
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

        // Default fragment
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
