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
import android.healthconnect.datatypes.InstantRecord
import android.healthconnect.datatypes.IntervalRecord
import android.healthconnect.datatypes.Record
import android.healthconnect.datatypes.units.Energy
import android.healthconnect.datatypes.units.Length
import android.healthconnect.datatypes.units.Power
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.android.healthconnect.testapps.toolbox.Constants.HealthPermissionType
import com.android.healthconnect.testapps.toolbox.Constants.INPUT_TYPE_DOUBLE
import com.android.healthconnect.testapps.toolbox.Constants.INPUT_TYPE_LONG
import com.android.healthconnect.testapps.toolbox.R
import com.android.healthconnect.testapps.toolbox.fieldviews.DateTimePicker
import com.android.healthconnect.testapps.toolbox.fieldviews.EditableTextView
import com.android.healthconnect.testapps.toolbox.fieldviews.EnumDropDown
import com.android.healthconnect.testapps.toolbox.fieldviews.InputFieldView
import com.android.healthconnect.testapps.toolbox.fieldviews.ListInputField
import com.android.healthconnect.testapps.toolbox.utils.EnumFieldsWithValues
import com.android.healthconnect.testapps.toolbox.utils.GeneralUtils.Companion.getStaticFieldNamesAndValues
import com.android.healthconnect.testapps.toolbox.utils.InsertOrUpdateRecords.Companion.insertOrUpdateRecord
import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType
import kotlin.reflect.KClass

class InsertRecordFragment : Fragment() {

    private lateinit var mRecordFields: Array<Field>
    private lateinit var mRecordClass: KClass<out Record>
    private lateinit var mNavigationController: NavController
    private lateinit var mFieldNameToFieldInput: HashMap<String, InputFieldView>
    private lateinit var mLinearLayout: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return inflater.inflate(R.layout.fragment_insert_record, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mNavigationController = findNavController()
        val permissionType =
            arguments?.getSerializable("permissionType", HealthPermissionType::class.java)
                ?: throw java.lang.IllegalArgumentException("Please pass the permissionType.")

        mFieldNameToFieldInput = HashMap()
        mRecordFields = permissionType.recordClass?.java?.declaredFields as Array<Field>
        mRecordClass = permissionType.recordClass
        view.findViewById<TextView>(R.id.title).setText(permissionType.title)
        mLinearLayout = view.findViewById<LinearLayout>(R.id.record_input_linear_layout)

        when (mRecordClass.java.superclass) {
            IntervalRecord::class.java -> {
                setupStartAndEndTimeFields()
            }
            InstantRecord::class.java -> {
                setupTimeField("Time", "time")
            }
            else -> {
                Toast.makeText(context, R.string.not_implemented, Toast.LENGTH_SHORT).show()
                mNavigationController.popBackStack()
            }
        }
        setupRecordFields()
        setupSubmitButton(view)
    }

    private fun setupTimeField(title: String, key: String) {
        val timeField = DateTimePicker(this.requireContext(), title)
        mLinearLayout.addView(timeField)

        mFieldNameToFieldInput[key] = timeField
    }

    private fun setupStartAndEndTimeFields() {
        setupTimeField("Start Time", "startTime")
        setupTimeField("End Time", "endTime")
    }

    private fun setupRecordFields() {
        var field: InputFieldView
        for (mRecordsField in mRecordFields) {
            when (mRecordsField.type) {
                Long::class.java -> {
                    field =
                        EditableTextView(this.requireContext(), mRecordsField.name, INPUT_TYPE_LONG)
                }
                Length::class.java,
                Energy::class.java,
                Power::class.java, -> {
                    field =
                        EditableTextView(
                            this.requireContext(), mRecordsField.name, INPUT_TYPE_DOUBLE)
                }
                List::class.java -> {
                    field =
                        ListInputField(
                            this.requireContext(),
                            mRecordsField.name,
                            mRecordsField.genericType as ParameterizedType)
                }
                else -> {
                    break
                }
            }
            mLinearLayout.addView(field)
            mFieldNameToFieldInput[mRecordsField.name] = field
        }

        when (mRecordClass) {
            ExerciseEventRecord::class -> {
                val enumFieldsWithValues: EnumFieldsWithValues =
                    getStaticFieldNamesAndValues(ExerciseEventRecord.ExerciseEventType::class)
                field = EnumDropDown(this.requireContext(), "mEventType", enumFieldsWithValues)
                mLinearLayout.addView(field)
                mFieldNameToFieldInput["mEventType"] = field
            }
        }
    }

    private fun setupSubmitButton(view: View) {
        val buttonView = view.findViewById<Button>(R.id.insert_or_update_record)

        buttonView.setText(R.string.insert_data)
        buttonView.setOnClickListener {
            activity?.let { activity ->
                insertOrUpdateRecord(
                    mRecordClass, mFieldNameToFieldInput, activity, requireContext())
            }
        }
    }
}
