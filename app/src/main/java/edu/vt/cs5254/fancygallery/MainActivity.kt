package edu.vt.cs5254.fancygallery

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavHost
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import edu.vt.cs5254.fancygallery.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Standard from ADWS
        val navHost = supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHost
        binding.bottomNav.setupWithNavController(navHost.navController)

        // require for navigation outside of bottom nav
        binding.bottomNav.setOnItemSelectedListener { item ->
            NavigationUI.onNavDestinationSelected(item, navHost.navController)
            true
        }
    }
}