/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.testapps.toolbox.ui

import android.healthconnect.datatypes.ExerciseEventRecord
import android.healthconnect.datatypes.IntervalRecord
import android.healthconnect.datatypes.units.Energy
import android.healthconnect.datatypes.units.Length
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.android.healthconnect.testapps.toolbox.Constants.HealthPermissionType
import com.android.healthconnect.testapps.toolbox.R
import com.android.healthconnect.testapps.toolbox.fieldviews.DateTimePicker
import com.android.healthconnect.testapps.toolbox.fieldviews.EditableTextView
import com.android.healthconnect.testapps.toolbox.fieldviews.EnumDropDown
import com.android.healthconnect.testapps.toolbox.fieldviews.InputFieldInterface
import com.android.healthconnect.testapps.toolbox.utils.EnumFieldsWithValues
import com.android.healthconnect.testapps.toolbox.utils.GeneralUtils.Companion.getStaticFieldNamesAndValues
import com.android.healthconnect.testapps.toolbox.utils.InsertOrUpdateIntervalRecords.Companion.insertOrUpdateRecord
import java.lang.reflect.Field
import kotlin.reflect.KClass

class IntervalRecordEntry : Fragment() {

    private lateinit var mRecordFields: Array<Field>
    private lateinit var mRecordClass: KClass<out IntervalRecord>
    private lateinit var mNavigationController: NavController
    private lateinit var mFieldNameToFieldInput: HashMap<String, InputFieldInterface>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return inflater.inflate(R.layout.fragment_interval_entry, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mNavigationController = findNavController()
        val permissionType =
            arguments?.getSerializable("permissionType", HealthPermissionType::class.java)
                ?: throw java.lang.IllegalArgumentException("Please pass the permissionType.")

        mFieldNameToFieldInput = HashMap()
        mRecordFields = permissionType.recordClass?.java?.declaredFields as Array<Field>
        mRecordClass = permissionType.recordClass as KClass<out IntervalRecord>
        view.findViewById<TextView>(R.id.title).setText(permissionType.title)

        setupStartAndEndTimeFields(view)
        setupRecordFields(view)
        setupSubmitButton(view)
    }

    private fun setupStartAndEndTimeFields(view: View) {
        val startTimeField = DateTimePicker(this.requireContext(), "Start Time")
        val endTimeField = DateTimePicker(this.requireContext(), "End Time")

        view.findViewById<LinearLayout>(R.id.interval_linear_layout).addView(startTimeField)

        view.findViewById<LinearLayout>(R.id.interval_linear_layout).addView(endTimeField)

        mFieldNameToFieldInput["startTime"] = startTimeField
        mFieldNameToFieldInput["endTime"] = endTimeField
    }

    private fun setupRecordFields(view: View) {
        val layout = view.findViewById<LinearLayout>(R.id.interval_linear_layout)
        var field: InputFieldInterface
        for (mRecordsField in mRecordFields) {
            when (mRecordsField.type) {
                Long::class.java -> {
                    field =
                        EditableTextView(
                            this.requireContext(), mRecordsField.name, InputType.TYPE_CLASS_NUMBER)
                }
                Length::class.java,
                Energy::class.java, -> {
                    field =
                        EditableTextView(
                            this.requireContext(),
                            mRecordsField.name,
                            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
                }
                else -> {
                    break
                }
            }
            layout.addView(field)
            mFieldNameToFieldInput[mRecordsField.name] = field
        }

        when (mRecordClass) {
            ExerciseEventRecord::class -> {
                val enumFieldsWithValues: EnumFieldsWithValues =
                    getStaticFieldNamesAndValues(ExerciseEventRecord.ExerciseEventType::class)
                field = EnumDropDown(this.requireContext(), "mEventType", enumFieldsWithValues)
                layout.addView(field)
                mFieldNameToFieldInput["mEventType"] = field
            }
        }
    }

    private fun setupSubmitButton(view: View) {
        val buttonView = view.findViewById<Button>(R.id.insert_or_update_interval_record)

        buttonView.setText(R.string.insert_data)
        buttonView.setOnClickListener {
            activity?.let { activity ->
                insertOrUpdateRecord(
                    mRecordClass, mFieldNameToFieldInput, activity, requireContext())
            }
        }
    }
}
