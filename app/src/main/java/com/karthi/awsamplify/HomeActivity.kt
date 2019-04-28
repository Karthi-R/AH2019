package com.karthi.awsamplify

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_home.*

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        controlSwitch.setOnClickListener {
            startActivity(Intent(this, SwitchActivity::class.java))
        }

        sensorData.setOnClickListener {
            startActivity(Intent(this, SensorActivity::class.java))
        }

        forecastData.setOnClickListener {
            startActivity(Intent(this, ForecastActivity::class.java))
        }
    }

}