package com.karthi.awsamplify

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_switches.*
import org.json.JSONObject

class SwitchActivity : AppCompatActivity() {

    val topic = "topic/switch"

    fun Boolean.toInt() = if (this) 1 else 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_switches)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        switch1.setOnCheckedChangeListener { _, isChecked ->
            App.instance.publish(topic, makePayload(isChecked))
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun makePayload(value: Boolean): String {
        val root = JSONObject()
        val state = JSONObject()
        val reported = JSONObject()
        try {
            reported.put("value", value.toInt())
            state.put("reported", reported)
            root.put("state", state)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return root.toString()
    }
}