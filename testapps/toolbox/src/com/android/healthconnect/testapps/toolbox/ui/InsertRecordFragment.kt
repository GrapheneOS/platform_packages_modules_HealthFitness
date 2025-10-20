/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.testapps.toolbox.ui

import android.health.connect.HealthConnectManager
import android.health.connect.datatypes.ActivityIntensityRecord
import android.health.connect.datatypes.AlcoholConsumptionRecord
import android.health.connect.datatypes.BasalBodyTemperatureRecord
import android.health.connect.datatypes.BloodGlucoseRecord
import android.health.connect.datatypes.BloodPressureRecord
import android.health.connect.datatypes.BodyTemperatureMeasurementLocation
import android.health.connect.datatypes.BodyTemperatureRecord
import android.health.connect.datatypes.CervicalMucusRecord
import android.health.connect.datatypes.ExerciseRoute
import android.health.connect.datatypes.ExerciseSessionRecord
import android.health.connect.datatypes.ExerciseSessionType
import android.health.connect.datatypes.FloorsClimbedRecord
import android.health.connect.datatypes.InstantRecord
import android.health.connect.datatypes.IntervalRecord
import android.health.connect.datatypes.MealType
import android.health.connect.datatypes.MenstruationFlowRecord
import android.health.connect.datatypes.MindfulnessSessionRecord
import android.health.connect.datatypes.NicotineIntakeRecord
import android.health.connect.datatypes.OvulationTestRecord
import android.health.connect.datatypes.PlannedExerciseSessionRecord
import android.health.connect.datatypes.Record
import android.health.connect.datatypes.SexualActivityRecord
import android.health.connect.datatypes.SkinTemperatureRecord
import android.health.connect.datatypes.SymptomRecord
import android.health.connect.datatypes.Vo2MaxRecord
import android.health.connect.datatypes.units.BloodGlucose
import android.health.connect.datatypes.units.Energy
import android.health.connect.datatypes.units.Length
import android.health.connect.datatypes.units.Mass
import android.health.connect.datatypes.units.Percentage
import android.health.connect.datatypes.units.Power
import android.health.connect.datatypes.units.Pressure
import android.health.connect.datatypes.units.Temperature
import android.health.connect.datatypes.units.TemperatureDelta
import android.health.connect.datatypes.units.Volume
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.android.healthconnect.testapps.toolbox.Constants.HealthPermissionType
import com.android.healthconnect.testapps.toolbox.Constants.INPUT_TYPE_DOUBLE
import com.android.healthconnect.testapps.toolbox.Constants.INPUT_TYPE_INT
import com.android.healthconnect.testapps.toolbox.Constants.INPUT_TYPE_LONG
import com.android.healthconnect.testapps.toolbox.Constants.INPUT_TYPE_TEXT
import com.android.healthconnect.testapps.toolbox.R
import com.android.healthconnect.testapps.toolbox.data.ExerciseRoutesTestData.Companion.routeDataMap
import com.android.healthconnect.testapps.toolbox.fieldviews.DateTimePicker
import com.android.healthconnect.testapps.toolbox.fieldviews.EditableTextView
import com.android.healthconnect.testapps.toolbox.fieldviews.EnumDropDown
import com.android.healthconnect.testapps.toolbox.fieldviews.InputFieldView
import com.android.healthconnect.testapps.toolbox.fieldviews.ListInputField
import com.android.healthconnect.testapps.toolbox.utils.EnumFieldsWithValues
import com.android.healthconnect.testapps.toolbox.utils.GeneralUtils
import com.android.healthconnect.testapps.toolbox.utils.InsertOrUpdateRecords.Companion.createRecordObject
import com.android.healthconnect.testapps.toolbox.viewmodels.InsertOrUpdateRecordsViewModel
import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType
import kotlin.reflect.KClass

class InsertRecordFragment : Fragment() {

    private lateinit var mRecordFields: Array<Field>
    private lateinit var mRecordClass: KClass<out Record>
    private lateinit var mNavigationController: NavController
    private lateinit var mFieldNameToFieldInput: HashMap<String, InputFieldView>
    private lateinit var mLinearLayout: LinearLayout
    private lateinit var mHealthConnectManager: HealthConnectManager
    private lateinit var mUpdateRecordUuid: InputFieldView

    private val mInsertOrUpdateViewModel: InsertOrUpdateRecordsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        mInsertOrUpdateViewModel.insertedRecordsState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is InsertOrUpdateRecordsViewModel.InsertedRecordsState.WithData -> {
                    showInsertSuccessDialog(state.entries)
                }

                is InsertOrUpdateRecordsViewModel.InsertedRecordsState.Error -> {
                    Toast.makeText(
                            context,
                            "Unable to insert record(s)! ${state.errorMessage}",
                            Toast.LENGTH_SHORT,
                        )
                        .show()
                }
            }
        }

        mInsertOrUpdateViewModel.updatedRecordsState.observe(viewLifecycleOwner) { state ->
            if (state is InsertOrUpdateRecordsViewModel.UpdatedRecordsState.Error) {
                Toast.makeText(
                        context,
                        "Unable to update record(s)! ${state.errorMessage}",
                        Toast.LENGTH_SHORT,
                    )
                    .show()
            } else {
                Toast.makeText(context, "Successfully updated record(s)!", Toast.LENGTH_SHORT)
                    .show()
            }
        }
        return inflater.inflate(R.layout.fragment_insert_record, container, false)
    }

    private fun showInsertSuccessDialog(records: List<Record>) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Record UUID(s)")
        builder.setMessage(records.joinToString { it.metadata.id })
        builder.setPositiveButton(android.R.string.ok) { _, _ -> }
        val alertDialog: AlertDialog = builder.create()
        alertDialog.show()
        alertDialog.findViewById<TextView>(android.R.id.message)?.setTextIsSelectable(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mNavigationController = findNavController()
        mHealthConnectManager =
            requireContext().getSystemService(HealthConnectManager::class.java)!!

        val permissionType =
            arguments?.getSerializable("permissionType", HealthPermissionType::class.java)
                ?: throw java.lang.IllegalArgumentException("Please pass the permissionType.")

        mFieldNameToFieldInput = HashMap()
        mRecordFields = permissionType.recordClass?.java?.declaredFields as Array<Field>
        mRecordClass = permissionType.recordClass
        view.requireViewById<TextView>(R.id.title).setText(permissionType.title)
        mLinearLayout = view.requireViewById(R.id.record_input_linear_layout)

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
        setupEnumFields()
        handleSpecialCases()
        setupListFields()
        setupInsertDataButton(view)
        setupUpdateDataButton(view)
    }

    private fun setupTimeField(title: String, key: String, setPreviousHour: Boolean = false) {
        val timeField = DateTimePicker(this.requireContext(), title, setPreviousHour)
        mLinearLayout.addView(timeField)

        mFieldNameToFieldInput[key] = timeField
    }

    private fun setupStartAndEndTimeFields() {
        setupTimeField("Start Time", "startTime", true)
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

                ExerciseRoute::class.java, // Edge case
                Int::class.java, // Most of int fields are enums and are handled separately
                List::class
                    .java // Handled later so that list fields are always added towards the end
                -> {
                    continue
                }

                Double::class.java,
                Pressure::class.java,
                BloodGlucose::class.java,
                Temperature::class.java,
                Volume::class.java,
                Percentage::class.java,
                Mass::class.java,
                Length::class.java,
                Energy::class.java,
                Power::class.java -> {
                    field =
                        EditableTextView(
                            this.requireContext(),
                            mRecordsField.name,
                            INPUT_TYPE_DOUBLE,
                        )
                }

                TemperatureDelta::class.java -> {
                    field =
                        EditableTextView(
                            this.requireContext(),
                            mRecordsField.name,
                            INPUT_TYPE_DOUBLE,
                        )
                }

                CharSequence::class.java -> {
                    field =
                        EditableTextView(this.requireContext(), mRecordsField.name, INPUT_TYPE_TEXT)
                }

                else -> {
                    continue
                }
            }
            mLinearLayout.addView(field)
            mFieldNameToFieldInput[mRecordsField.name] = field
        }
    }

    private fun setupEnumFields() {
        val enumFieldNameToClass =
            when (mRecordClass) {
                MenstruationFlowRecord::class ->
                    mapOf("mFlow" to MenstruationFlowRecord.MenstruationFlowType::class)
                OvulationTestRecord::class ->
                    mapOf("mResult" to OvulationTestRecord.OvulationTestResult::class)
                SexualActivityRecord::class ->
                    mapOf(
                        "mProtectionUsed" to
                            SexualActivityRecord.SexualActivityProtectionUsed::class
                    )
                CervicalMucusRecord::class ->
                    mapOf(
                        "mSensation" to CervicalMucusRecord.CervicalMucusSensation::class,
                        "mAppearance" to CervicalMucusRecord.CervicalMucusAppearance::class,
                    )

                Vo2MaxRecord::class ->
                    mapOf("mMeasurementMethod" to Vo2MaxRecord.Vo2MaxMeasurementMethod::class)
                BasalBodyTemperatureRecord::class ->
                    mapOf(
                        "mBodyTemperatureMeasurementLocation" to
                            BodyTemperatureMeasurementLocation::class
                    )
                BloodGlucoseRecord::class ->
                    mapOf(
                        "mSpecimenSource" to BloodGlucoseRecord.SpecimenSource::class,
                        "mRelationToMeal" to BloodGlucoseRecord.RelationToMealType::class,
                        "mMealType" to MealType::class,
                    )

                BloodPressureRecord::class ->
                    mapOf(
                        "mMeasurementLocation" to BodyTemperatureMeasurementLocation::class,
                        "mBodyPosition" to BloodPressureRecord.BodyPosition::class,
                    )

                BodyTemperatureRecord::class ->
                    mapOf("mMeasurementLocation" to BodyTemperatureMeasurementLocation::class)

                SkinTemperatureRecord::class ->
                    mapOf("mMeasurementLocation" to SkinTemperatureRecord::class)
                ExerciseSessionRecord::class -> mapOf("mExerciseType" to ExerciseSessionType::class)

                PlannedExerciseSessionRecord::class ->
                    mapOf("mPlannedExerciseType" to ExerciseSessionType::class)

                MindfulnessSessionRecord::class ->
                    mapOf("mMindfulnessSessionType" to MindfulnessSessionRecord::class)
                ActivityIntensityRecord::class ->
                    mapOf("mActivityIntensityType" to ActivityIntensityRecord::class)

                NicotineIntakeRecord::class ->
                    mapOf("mNicotineIntakeType" to NicotineIntakeRecord::class)
                AlcoholConsumptionRecord::class ->
                    mapOf(
                        "mBeverageType" to AlcoholConsumptionRecord::class,
                        "mServingSize" to AlcoholConsumptionRecord::class,
                        "mTemporalType" to AlcoholConsumptionRecord::class,
                    )
                else -> mapOf()
            }
        enumFieldNameToClass.forEach { fieldName, enumClass ->
            val enumFieldsWithValues: EnumFieldsWithValues =
                GeneralUtils.getStaticFieldNamesAndValues(enumClass)
            val field = EnumDropDown(this.requireContext(), fieldName, enumFieldsWithValues)
            mLinearLayout.addView(field)
            mFieldNameToFieldInput[fieldName] = field
        }
    }

    private fun setupListFields() {
        var field: InputFieldView
        for (mRecordsField in mRecordFields) {
            when (mRecordsField.type) {
                List::class.java -> {
                    field =
                        ListInputField(
                            this.requireContext(),
                            mRecordsField.name,
                            mRecordsField.genericType as ParameterizedType,
                        )
                }

                else -> {
                    continue
                }
            }
            mLinearLayout.addView(field)
            mFieldNameToFieldInput[mRecordsField.name] = field
        }
    }

    private fun handleSpecialCases() {
        var field: InputFieldView? = null
        var fieldName: String? = null

        when (mRecordClass) {
            FloorsClimbedRecord::class -> {
                fieldName = "mFloors"
                field = EditableTextView(this.requireContext(), fieldName, INPUT_TYPE_INT)
            }

            ExerciseSessionRecord::class -> {
                fieldName = "mExerciseRoute"
                field =
                    EnumDropDown(
                        this.requireContext(),
                        fieldName,
                        EnumFieldsWithValues(routeDataMap as Map<String, Any>),
                    )
            }

            NicotineIntakeRecord::class -> {
                fieldName = "mQuantity"
                field = EditableTextView(this.requireContext(), fieldName, INPUT_TYPE_INT)
            }

            AlcoholConsumptionRecord::class -> {
                fieldName = "mServingCount"
                field = EditableTextView(this.requireContext(), fieldName, INPUT_TYPE_INT)
            }

            SymptomRecord::class -> {
                val temporalTypes =
                    mapOf(
                        "Interval" to SymptomRecord.RECORD_TEMPORAL_TYPE_INTERVAL,
                        "Instant" to SymptomRecord.RECORD_TEMPORAL_TYPE_INSTANT,
                        "LocalDate" to SymptomRecord.RECORD_TEMPORAL_TYPE_LOCAL_DATE,
                    )
                val temporalTypeField =
                    EnumDropDown(
                        this.requireContext(),
                        "mTemporalType",
                        EnumFieldsWithValues(temporalTypes as Map<String, Any>),
                    )
                mLinearLayout.addView(temporalTypeField)
                mFieldNameToFieldInput["mTemporalType"] = temporalTypeField

                val symptomTypes =
                    mapOf(
                        "Cough" to SymptomRecord.SYMPTOM_TYPE_COUGH,
                        "Snore" to SymptomRecord.SYMPTOM_TYPE_SNORE,
                        "Abdominal Pain" to SymptomRecord.SYMPTOM_TYPE_ABDOMINAL_PAIN,
                        "Acne" to SymptomRecord.SYMPTOM_TYPE_ACNE,
                        "Back Pain" to SymptomRecord.SYMPTOM_TYPE_BACK_PAIN,
                        "Bloating" to SymptomRecord.SYMPTOM_TYPE_BLOATING,
                        "Brain Fog" to SymptomRecord.SYMPTOM_TYPE_BRAIN_FOG,
                        "Breast Tenderness" to SymptomRecord.SYMPTOM_TYPE_BREAST_TENDERNESS,
                        "Brittle Nails" to SymptomRecord.SYMPTOM_TYPE_BRITTLE_NAILS,
                        "Burning Mouth" to SymptomRecord.SYMPTOM_TYPE_BURNING_MOUTH,
                        "Chest Pain" to SymptomRecord.SYMPTOM_TYPE_CHEST_PAIN,
                        "Chest Tightness" to SymptomRecord.SYMPTOM_TYPE_CHEST_TIGHTNESS,
                        "Chills" to SymptomRecord.SYMPTOM_TYPE_CHILLS,
                        "Constipation" to SymptomRecord.SYMPTOM_TYPE_CONSTIPATION,
                        "Cramps" to SymptomRecord.SYMPTOM_TYPE_CRAMPS,
                        "Cravings" to SymptomRecord.SYMPTOM_TYPE_CRAVINGS,
                        "Dehydration" to SymptomRecord.SYMPTOM_TYPE_DEHYDRATION,
                        "Diarrhea" to SymptomRecord.SYMPTOM_TYPE_DIARRHEA,
                        "Difficulty Swallowing" to SymptomRecord.SYMPTOM_TYPE_DIFFICULTY_SWALLOWING,
                        "Dizziness" to SymptomRecord.SYMPTOM_TYPE_DIZZINESS,
                        "Dry Skin" to SymptomRecord.SYMPTOM_TYPE_DRY_SKIN,
                        "Earaches" to SymptomRecord.SYMPTOM_TYPE_EARACHES,
                        "Fatigue" to SymptomRecord.SYMPTOM_TYPE_FATIGUE,
                        "Fever" to SymptomRecord.SYMPTOM_TYPE_FEVER,
                        "Generalized Body Ache" to SymptomRecord.SYMPTOM_TYPE_GENERALIZED_BODY_ACHE,
                        "Hair Loss" to SymptomRecord.SYMPTOM_TYPE_HAIR_LOSS,
                        "Headache" to SymptomRecord.SYMPTOM_TYPE_HEADACHE,
                        "Heartburn" to SymptomRecord.SYMPTOM_TYPE_HEARTBURN,
                        "Heart Palpitations" to SymptomRecord.SYMPTOM_TYPE_HEART_PALPITATIONS,
                        "Hot Flashes" to SymptomRecord.SYMPTOM_TYPE_HOT_FLASHES,
                        "Insomnia" to SymptomRecord.SYMPTOM_TYPE_INSOMNIA,
                        "Joint Pain" to SymptomRecord.SYMPTOM_TYPE_JOINT_PAIN,
                        "Joint Stiffness" to SymptomRecord.SYMPTOM_TYPE_JOINT_STIFFNESS,
                        "Loss Of Appetite" to SymptomRecord.SYMPTOM_TYPE_LOSS_OF_APPETITE,
                        "Loss Of Consciousness" to SymptomRecord.SYMPTOM_TYPE_LOSS_OF_CONSCIOUSNESS,
                        "Lower Back Pain" to SymptomRecord.SYMPTOM_TYPE_LOWER_BACK_PAIN,
                        "Memory Lapse" to SymptomRecord.SYMPTOM_TYPE_MEMORY_LAPSE,
                        "Mood Change" to SymptomRecord.SYMPTOM_TYPE_MOOD_CHANGE,
                        "Muscle Pain" to SymptomRecord.SYMPTOM_TYPE_MUSCLE_PAIN,
                        "Nausea" to SymptomRecord.SYMPTOM_TYPE_NAUSEA,
                        "Night Sweats" to SymptomRecord.SYMPTOM_TYPE_NIGHT_SWEATS,
                        "Pelvic Pain" to SymptomRecord.SYMPTOM_TYPE_PELVIC_PAIN,
                        "Rapid Pounding Or Fluttering Heartbeat" to
                            SymptomRecord.SYMPTOM_TYPE_RAPID_POUNDING_OR_FLUTTERING_HEARTBEAT,
                        "Reduced Capacity For Exercise" to
                            SymptomRecord.SYMPTOM_TYPE_REDUCED_CAPACITY_FOR_EXERCISE,
                        "Runny Nose" to SymptomRecord.SYMPTOM_TYPE_RUNNY_NOSE,
                        "Shortness Of Breath" to SymptomRecord.SYMPTOM_TYPE_SHORTNESS_OF_BREATH,
                        "Skipped Heartbeat" to SymptomRecord.SYMPTOM_TYPE_SKIPPED_HEARTBEAT,
                        "Sleepiness" to SymptomRecord.SYMPTOM_TYPE_SLEEPINESS,
                        "Sleep Changes" to SymptomRecord.SYMPTOM_TYPE_SLEEP_CHANGES,
                        "Sneezing" to SymptomRecord.SYMPTOM_TYPE_SNEEZING,
                        "Sore Throat" to SymptomRecord.SYMPTOM_TYPE_SORE_THROAT,
                        "Stomach Ache" to SymptomRecord.SYMPTOM_TYPE_STOMACH_ACHE,
                        "Stuffy Nose" to SymptomRecord.SYMPTOM_TYPE_STUFFY_NOSE,
                        "Unexplained Weight Changes" to
                            SymptomRecord.SYMPTOM_TYPE_UNEXPLAINED_WEIGHT_CHANGES,
                        "Vaginal Dryness" to SymptomRecord.SYMPTOM_TYPE_VAGINAL_DRYNESS,
                        "Vaginal Itchiness" to SymptomRecord.SYMPTOM_TYPE_VAGINAL_ITCHINESS,
                        "Vomiting" to SymptomRecord.SYMPTOM_TYPE_VOMITING,
                        "Water Retention" to SymptomRecord.SYMPTOM_TYPE_WATER_RETENTION,
                        "Wheezing" to SymptomRecord.SYMPTOM_TYPE_WHEEZING,
                    )
                val typeField =
                    EnumDropDown(
                        this.requireContext(),
                        "mType",
                        EnumFieldsWithValues(symptomTypes as Map<String, Any>),
                    )
                mLinearLayout.addView(typeField)
                mFieldNameToFieldInput["mType"] = typeField

                val severities =
                    mapOf(
                        "Unspecified" to SymptomRecord.SEVERITY_UNSPECIFIED,
                        "Mild" to SymptomRecord.SEVERITY_MILD,
                        "Moderate" to SymptomRecord.SEVERITY_MODERATE,
                        "Severe" to SymptomRecord.SEVERITY_SEVERE,
                    )
                val severityField =
                    EnumDropDown(
                        this.requireContext(),
                        "mSeverity",
                        EnumFieldsWithValues(severities as Map<String, Any>),
                    )
                mLinearLayout.addView(severityField)
                mFieldNameToFieldInput["mSeverity"] = severityField
            }
        }
        if (field != null && fieldName != null) {
            mLinearLayout.addView(field)
            mFieldNameToFieldInput[fieldName] = field
        }
    }

    private fun setupInsertDataButton(view: View) {
        val buttonView = view.requireViewById<Button>(R.id.insert_record)

        buttonView.setOnClickListener {
            try {
                val record =
                    createRecordObject(mRecordClass, mFieldNameToFieldInput, requireContext())
                mInsertOrUpdateViewModel.insertRecordsViaViewModel(
                    listOf(record),
                    mHealthConnectManager,
                )
            } catch (ex: Exception) {
                Log.d("InsertOrUpdateRecordsViewModel", ex.localizedMessage!!)
                Toast.makeText(
                        context,
                        "Unable to insert record: ${ex.localizedMessage}",
                        Toast.LENGTH_SHORT,
                    )
                    .show()
            }
        }
    }

    private fun setupUpdateRecordUuidInputDialog() {
        mUpdateRecordUuid = EditableTextView(requireContext(), null, INPUT_TYPE_TEXT)
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Enter UUID")
        builder.setView(mUpdateRecordUuid)
        builder.setPositiveButton(android.R.string.ok) { _, _ ->
            try {
                if (mUpdateRecordUuid.getFieldValue().toString().isEmpty()) {
                    throw IllegalArgumentException("Please enter UUID")
                }
                val record =
                    createRecordObject(
                        mRecordClass,
                        mFieldNameToFieldInput,
                        requireContext(),
                        mUpdateRecordUuid.getFieldValue().toString(),
                    )
                mInsertOrUpdateViewModel.updateRecordsViaViewModel(
                    listOf(record),
                    mHealthConnectManager,
                )
            } catch (ex: Exception) {
                Toast.makeText(
                        context,
                        "Unable to update: ${ex.localizedMessage}",
                        Toast.LENGTH_SHORT,
                    )
                    .show()
            }
        }
        val alertDialog: AlertDialog = builder.create()
        alertDialog.show()
    }

    private fun setupUpdateDataButton(view: View) {
        val buttonView = view.requireViewById<Button>(R.id.update_record)

        buttonView.setOnClickListener { setupUpdateRecordUuidInputDialog() }
    }
}
