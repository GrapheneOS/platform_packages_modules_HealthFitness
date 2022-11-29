/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * ```
 *    http://www.apache.org/licenses/LICENSE-2.0
 * ```
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.testapps.toolbox.fieldviews

import android.annotation.SuppressLint
import android.content.Context
import android.healthconnect.datatypes.CyclingPedalingCadenceRecord.CyclingPedalingCadenceRecordSample
import android.healthconnect.datatypes.HeartRateRecord.HeartRateSample
import android.healthconnect.datatypes.PowerRecord.PowerRecordSample
import android.healthconnect.datatypes.SpeedRecord.SpeedRecordSample
import android.healthconnect.datatypes.units.Power
import android.healthconnect.datatypes.units.Velocity
import android.widget.LinearLayout
import android.widget.TextView
import com.android.healthconnect.testapps.toolbox.Constants.INPUT_TYPE_DOUBLE
import com.android.healthconnect.testapps.toolbox.Constants.INPUT_TYPE_LONG
import com.android.healthconnect.testapps.toolbox.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.time.Instant

@SuppressLint("ViewConstructor")
class ListInputField(context: Context, fieldName: String, inputFieldType: ParameterizedType) :
    InputFieldView(context) {

    private var mLinearLayout: LinearLayout
    private var mDataTypeClass: Type
    private var mInstantToDataPoint: HashMap<InputFieldView, InputFieldView>

    init {
        inflate(context, R.layout.fragment_list_input_view, this)
        findViewById<TextView>(R.id.field_name).text = fieldName
        mLinearLayout = findViewById(R.id.list_input_linear_layout)
        mDataTypeClass = inputFieldType.actualTypeArguments[0]
        mInstantToDataPoint = HashMap()
        setupView()
        setupAddRowButtonListener()
    }

    private fun setupAddRowButtonListener() {
        val buttonView = findViewById<FloatingActionButton>(R.id.add_row)

        buttonView.setOnClickListener { addRow() }
    }

    private fun setupView() {
        addRow()
    }

    private fun addRow() {
        val rowLayout = LinearLayout(context)
        rowLayout.orientation = VERTICAL

        val instantField = DateTimePicker(context, "Time")
        rowLayout.addView(instantField)
        val dataPointField: InputFieldView =
            when (mDataTypeClass) {
                SpeedRecordSample::class.java -> {
                    EditableTextView(context, "Velocity", INPUT_TYPE_DOUBLE)
                }
                HeartRateSample::class.java -> {
                    EditableTextView(context, "Beats per minute", INPUT_TYPE_LONG)
                }
                PowerRecordSample::class.java -> {
                    EditableTextView(context, "Power", INPUT_TYPE_DOUBLE)
                }
                CyclingPedalingCadenceRecordSample::class.java -> {
                    EditableTextView(context, "Revolutions Per Minute", INPUT_TYPE_DOUBLE)
                }
                else -> {
                    return
                }
            }
        rowLayout.addView(dataPointField)
        mInstantToDataPoint[instantField] = dataPointField
        mLinearLayout.addView(rowLayout, 0)
    }

    override fun getFieldValue(): Any {
        val samples: ArrayList<Any> = ArrayList()
        for (instant in mInstantToDataPoint.keys) {
            val dataPoint = mInstantToDataPoint[instant]
            if (dataPoint != null) {
                val dataPointString = dataPoint.getFieldValue().toString()
                when (mDataTypeClass) {
                    SpeedRecordSample::class.java -> {
                        samples.add(
                            SpeedRecordSample(
                                Velocity.fromMetersPerSecond(dataPointString.toDouble()),
                                instant.getFieldValue() as Instant))
                    }
                    HeartRateSample::class.java -> {
                        samples.add(
                            HeartRateSample(
                                dataPointString.toLong(), instant.getFieldValue() as Instant))
                    }
                    PowerRecordSample::class.java -> {
                        samples.add(
                            PowerRecordSample(
                                Power.fromWatts(dataPointString.toDouble()),
                                instant.getFieldValue() as Instant))
                    }
                    CyclingPedalingCadenceRecordSample::class.java -> {
                        samples.add(
                            CyclingPedalingCadenceRecordSample(
                                dataPointString.toDouble(), instant.getFieldValue() as Instant))
                    }
                }
            }
        }
        return samples
    }
}
