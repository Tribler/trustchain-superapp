package nl.tudelft.ipv8.android.demo.ui.peers

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import kotlinx.android.synthetic.main.activity_main.*
import mu.KotlinLogging
import nl.tudelft.ipv8.android.demo.R

val logger = KotlinLogging.logger {}

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val navController = findNavController(R.id.navHostFragment)
        bottomNavigation.setupWithNavController(navController)
    }
}
