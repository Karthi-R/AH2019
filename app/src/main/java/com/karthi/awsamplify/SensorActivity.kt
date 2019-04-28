package com.karthi.awsamplify

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_sensor.*
import timber.log.Timber
import java.util.*


class SensorActivity : AppCompatActivity() {

    private val soilSensorTopic = "topic/soil"
    private val temperatureSensorTopic = "topic/temperature"
    private val humiditySensorTopic = "topic/humidity"

    //val data = LineData()
    val dataSet = ArrayList<Entry>()
    var count = 0f

    //val dataTmp = LineData()
    val dataSetTmp = ArrayList<Entry>()
    var countTmp = 0f

    //val dataHum = LineData()
    val dataSetHum = ArrayList<Entry>()
    var countHum = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sensor)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initDefaultStyle(soil_sensor_graph, 2000f)
        initDefaultStyle(temp_sensor_graph, 100f)
        initDefaultStyle(humidity_sensor_graph, 100f)

        App.instance.subscribe(soilSensorTopic, AWSIotMqttNewMessageCallback { topic, data ->
            val json = String(data)

            val gson = Gson()
            val payload = gson.fromJson(json, Payload::class.java)

            Timber.d("topic: %s, data: %s", topic, payload.state.reported.value)
            val soil = payload.state.reported.value as Double
            updateSoilSensor(soil.toFloat())
        })

        App.instance.subscribe(temperatureSensorTopic, AWSIotMqttNewMessageCallback { topic, data ->
            val json = String(data)

            val gson = Gson()
            val payload = gson.fromJson(json, Payload::class.java)

            Timber.d("topic: %s, data: %s", topic, payload.state.reported.value)
            val temp = payload.state.reported.value as Double
            updateTempSensor(temp.toFloat())
        })

        App.instance.subscribe(humiditySensorTopic, AWSIotMqttNewMessageCallback { topic, data ->
            val json = String(data)

            val gson = Gson()
            val payload = gson.fromJson(json, Payload::class.java)

            Timber.d("topic: %s, data: %s", topic, payload.state.reported.value)
            val humidity = payload.state.reported.value as Double
            updateHumiditySensor(humidity.toFloat())
        })


    }

    private fun updateTempSensor(value: Float) {
        dataSetTmp.add(Entry(countTmp, value))
        val set = LineDataSet(dataSetTmp, "")
        set.color = ContextCompat.getColor(this, R.color.yellow_800)
        set.lineWidth = 2f
        set.mode = LineDataSet.Mode.CUBIC_BEZIER
        set.setDrawValues(false)
        set.valueTextSize = 8f
        set.setCircleColor(ContextCompat.getColor(this, R.color.yellow_800))
        set.isHighlightEnabled = false

        val dataTmp = LineData()
        dataTmp.addDataSet(set)

        temp_sensor_graph.data = dataTmp

        temp_sensor_graph.invalidate() // refresh
        countTmp++
    }


    private fun updateHumiditySensor(value: Float) {
        dataSetHum.add(Entry(countHum, value))
        val set = LineDataSet(dataSetHum, "")
        set.color = ContextCompat.getColor(this, R.color.deep_orange_800)
        set.lineWidth = 2f
        set.mode = LineDataSet.Mode.CUBIC_BEZIER
        set.setDrawValues(false)
        set.valueTextSize = 8f
        set.setCircleColor(ContextCompat.getColor(this, R.color.deep_orange_800))
        set.isHighlightEnabled = false

        val data = LineData()
        data.addDataSet(set)

        humidity_sensor_graph.data = data

        humidity_sensor_graph.invalidate() // refresh
        countHum++
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

    private fun updateSoilSensor(value: Float) {

        dataSet.add(Entry(count, value))
        val set = LineDataSet(dataSet, "")
        set.color = ContextCompat.getColor(this, R.color.colorAccent)
        set.lineWidth = 2f
        set.mode = LineDataSet.Mode.CUBIC_BEZIER
        set.setDrawValues(false)
        set.valueTextSize = 8f
        set.setCircleColor(ContextCompat.getColor(this, R.color.colorAccent))
        set.isHighlightEnabled = false

        val data = LineData()
        data.addDataSet(set)

        soil_sensor_graph.data = data

        soil_sensor_graph.invalidate() // refresh
        count++
    }

    private fun initDefaultStyle(chart: LineChart, maxLimit: Float) {

        chart.setDrawGridBackground(false)
        chart.enableScroll()
        chart.setScaleEnabled(false)

        val desc = Description()
        desc.text = ""
        chart.description = desc

        /*val formatterX = IAxisValueFormatter { value, _ ->
            // need to check array index
            try {
                return@IAxisValueFormatter if (labels.size > 0 && value < labels.size) {
                    labels[value.toInt()]
                } else {
                    ""
                }
            } catch (e: ArrayIndexOutOfBoundsException) {
                return@IAxisValueFormatter ""
            }
        }*/

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.setAvoidFirstLastClipping(true)
        xAxis.axisMinimum = 0f
        xAxis.granularity = 1f
        //xAxis.setDrawAxisLine(false);
        //xAxis.valueFormatter = formatterX
        xAxis.labelRotationAngle = 0f


        val leftAxis = chart.axisLeft
        leftAxis.setDrawGridLines(true)
        //leftAxis.axisMaximum = maxLimit
        leftAxis.axisMinimum = 0f
        leftAxis.setDrawZeroLine(false)
        leftAxis.granularity = 5f
        leftAxis.gridLineWidth = 0.5f
        leftAxis.setDrawAxisLine(false)
        leftAxis.setDrawZeroLine(false)
        leftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)

        val rightAxis = chart.axisRight
        rightAxis.isEnabled = false

        // hide legend
        val legend = chart.legend
        legend.isEnabled = false


        // temp
        dataSet.add(Entry(count, 0f))
        val set = LineDataSet(dataSet, "")
        set.color = ContextCompat.getColor(this, R.color.colorAccent)
        set.lineWidth = 2f
        set.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
        set.setDrawValues(false)
        set.valueTextSize = 8f
        set.setCircleColor(ContextCompat.getColor(this, R.color.colorAccent))
        set.isHighlightEnabled = false

        val data = LineData()
        data.addDataSet(set)

        chart.data = data
        chart.invalidate()
    }
}