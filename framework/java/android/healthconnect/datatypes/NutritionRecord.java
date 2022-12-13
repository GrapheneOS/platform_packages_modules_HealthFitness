/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.healthconnect.datatypes;

import static android.healthconnect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_BIOTIN_TOTAL;
import static android.healthconnect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_CAFFEINE_TOTAL;
import static android.healthconnect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_CALCIUM_TOTAL;
import static android.healthconnect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_CHLORIDE_TOTAL;
import static android.healthconnect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_CHOLESTEROL_TOTAL;
import static android.healthconnect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_CHROMIUM_TOTAL;
import static android.healthconnect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_COPPER_TOTAL;
import static android.healthconnect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_DIETARY_FIBER_TOTAL;
import static android.healthconnect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_ENERGY_FROM_FAT_TOTAL;
import static android.healthconnect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_ENERGY_TOTAL;
import static android.healthconnect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_FOLATE_TOTAL;
import static android.healthconnect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_FOLIC_ACID_TOTAL;
import static android.healthconnect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_IODINE_TOTAL;
import static android.healthconnect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_IRON_TOTAL;
import static android.healthconnect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_MAGNESIUM_TOTAL;
import static android.healthconnect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_MANGANESE_TOTAL;
import static android.healthconnect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_MOLYBDENUM_TOTAL;
import static android.healthconnect.datatypes.AggregationType.AggregationTypeIdentifier.NUTRITION_RECORD_MONOUNSATURATED_FAT_TOTAL;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_NUTRITION;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.healthconnect.HealthConnectManager;
import android.healthconnect.datatypes.units.Energy;
import android.healthconnect.datatypes.units.Mass;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;

/** Captures what nutrients were consumed as part of a meal or a food item. */
@Identifier(recordIdentifier = RECORD_TYPE_NUTRITION)
public final class NutritionRecord extends IntervalRecord {
    /** Builder class for {@link NutritionRecord} */
    public static final class Builder {
        private final Metadata mMetadata;
        private final Instant mStartTime;
        private final Instant mEndTime;
        private ZoneOffset mStartZoneOffset;
        private ZoneOffset mEndZoneOffset;
        private Mass mUnsaturatedFat;
        private Mass mPotassium;
        private Mass mThiamin;
        private int mMealType;
        private Mass mTransFat;
        private Mass mManganese;
        private Energy mEnergyFromFat;
        private Mass mCaffeine;
        private Mass mDietaryFiber;
        private Mass mSelenium;
        private Mass mVitaminB6;
        private Mass mProtein;
        private Mass mChloride;
        private Mass mCholesterol;
        private Mass mCopper;
        private Mass mIodine;
        private Mass mVitaminB12;
        private Mass mZinc;
        private Mass mRiboflavin;
        private Energy mEnergy;
        private Mass mMolybdenum;
        private Mass mPhosphorus;
        private Mass mChromium;
        private Mass mTotalFat;
        private Mass mCalcium;
        private Mass mVitaminC;
        private Mass mVitaminE;
        private Mass mBiotin;
        private Mass mVitaminD;
        private Mass mNiacin;
        private Mass mMagnesium;
        private Mass mTotalCarbohydrate;
        private Mass mVitaminK;
        private Mass mPolyunsaturatedFat;
        private Mass mSaturatedFat;
        private Mass mSodium;
        private Mass mFolate;
        private Mass mMonounsaturatedFat;
        private Mass mPantothenicAcid;
        private String mMealName;
        private Mass mIron;
        private Mass mVitaminA;
        private Mass mFolicAcid;
        private Mass mSugar;

        /**
         * @param metadata Metadata to be associated with the record. See {@link Metadata}.
         * @param startTime Start time of this activity
         * @param endTime End time of this activity
         */
        public Builder(
                @NonNull Metadata metadata, @NonNull Instant startTime, @NonNull Instant endTime) {
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(startTime);
            Objects.requireNonNull(endTime);
            mMetadata = metadata;
            mStartTime = startTime;
            mEndTime = endTime;
            mStartZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
            mEndZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
        }

        /** Sets the zone offset of the user when the activity started */
        @NonNull
        public Builder setStartZoneOffset(@NonNull ZoneOffset startZoneOffset) {
            Objects.requireNonNull(startZoneOffset);

            mStartZoneOffset = startZoneOffset;
            return this;
        }

        /** Sets the zone offset of the user when the activity ended */
        @NonNull
        public Builder setEndZoneOffset(@NonNull ZoneOffset endZoneOffset) {
            Objects.requireNonNull(endZoneOffset);

            mEndZoneOffset = endZoneOffset;
            return this;
        }

        /**
         * Sets the unsaturatedFat of this activity
         *
         * @param unsaturatedFat UnsaturatedFat of this activity
         */
        @NonNull
        public Builder setUnsaturatedFat(@Nullable Mass unsaturatedFat) {
            Objects.requireNonNull(unsaturatedFat);
            mUnsaturatedFat = unsaturatedFat;
            return this;
        }

        /**
         * Sets the potassium of this activity
         *
         * @param potassium Potassium of this activity
         */
        @NonNull
        public Builder setPotassium(@Nullable Mass potassium) {
            Objects.requireNonNull(potassium);
            mPotassium = potassium;
            return this;
        }

        /**
         * Sets the thiamin of this activity
         *
         * @param thiamin Thiamin of this activity
         */
        @NonNull
        public Builder setThiamin(@Nullable Mass thiamin) {
            Objects.requireNonNull(thiamin);
            mThiamin = thiamin;
            return this;
        }

        /**
         * Sets the mealType of this activity
         *
         * @param mealType MealType of this activity
         */
        @NonNull
        public Builder setMealType(@MealType.MealTypes int mealType) {
            mMealType = mealType;
            return this;
        }

        /**
         * Sets the transFat of this activity
         *
         * @param transFat TransFat of this activity
         */
        @NonNull
        public Builder setTransFat(@Nullable Mass transFat) {
            mTransFat = transFat;
            return this;
        }

        /**
         * Sets the manganese of this activity
         *
         * @param manganese Manganese of this activity
         */
        @NonNull
        public Builder setManganese(@Nullable Mass manganese) {
            mManganese = manganese;
            return this;
        }

        /**
         * Sets the energyFromFat of this activity
         *
         * @param energyFromFat EnergyFromFat of this activity
         */
        @NonNull
        public Builder setEnergyFromFat(@Nullable Energy energyFromFat) {
            mEnergyFromFat = energyFromFat;
            return this;
        }

        /**
         * Sets the caffeine of this activity
         *
         * @param caffeine Caffeine of this activity
         */
        @NonNull
        public Builder setCaffeine(@Nullable Mass caffeine) {
            mCaffeine = caffeine;
            return this;
        }

        /**
         * Sets the dietaryFiber of this activity
         *
         * @param dietaryFiber DietaryFiber of this activity
         */
        @NonNull
        public Builder setDietaryFiber(@Nullable Mass dietaryFiber) {
            mDietaryFiber = dietaryFiber;
            return this;
        }

        /**
         * Sets the selenium of this activity
         *
         * @param selenium Selenium of this activity
         */
        @NonNull
        public Builder setSelenium(@Nullable Mass selenium) {
            mSelenium = selenium;
            return this;
        }

        /**
         * Sets the vitaminB6 of this activity
         *
         * @param vitaminB6 VitaminB6 of this activity
         */
        @NonNull
        public Builder setVitaminB6(@Nullable Mass vitaminB6) {
            mVitaminB6 = vitaminB6;
            return this;
        }

        /**
         * Sets the protein of this activity
         *
         * @param protein Protein of this activity
         */
        @NonNull
        public Builder setProtein(@Nullable Mass protein) {
            mProtein = protein;
            return this;
        }

        /**
         * Sets the chloride of this activity
         *
         * @param chloride Chloride of this activity
         */
        @NonNull
        public Builder setChloride(@Nullable Mass chloride) {
            mChloride = chloride;
            return this;
        }

        /**
         * Sets the cholesterol of this activity
         *
         * @param cholesterol Cholesterol of this activity
         */
        @NonNull
        public Builder setCholesterol(@Nullable Mass cholesterol) {
            mCholesterol = cholesterol;
            return this;
        }

        /**
         * Sets the copper of this activity
         *
         * @param copper Copper of this activity
         */
        @NonNull
        public Builder setCopper(@Nullable Mass copper) {
            mCopper = copper;
            return this;
        }

        /**
         * Sets the iodine of this activity
         *
         * @param iodine Iodine of this activity
         */
        @NonNull
        public Builder setIodine(@Nullable Mass iodine) {
            mIodine = iodine;
            return this;
        }

        /**
         * Sets the vitaminB12 of this activity
         *
         * @param vitaminB12 VitaminB12 of this activity
         */
        @NonNull
        public Builder setVitaminB12(@Nullable Mass vitaminB12) {
            mVitaminB12 = vitaminB12;
            return this;
        }

        /**
         * Sets the zinc of this activity
         *
         * @param zinc Zinc of this activity
         */
        @NonNull
        public Builder setZinc(@Nullable Mass zinc) {
            mZinc = zinc;
            return this;
        }

        /**
         * Sets the riboflavin of this activity
         *
         * @param riboflavin Riboflavin of this activity
         */
        @NonNull
        public Builder setRiboflavin(@Nullable Mass riboflavin) {
            mRiboflavin = riboflavin;
            return this;
        }

        /**
         * Sets the energy of this activity
         *
         * @param energy Energy of this activity
         */
        @NonNull
        public Builder setEnergy(@Nullable Energy energy) {
            mEnergy = energy;
            return this;
        }

        /**
         * Sets the molybdenum of this activity
         *
         * @param molybdenum Molybdenum of this activity
         */
        @NonNull
        public Builder setMolybdenum(@Nullable Mass molybdenum) {
            mMolybdenum = molybdenum;
            return this;
        }

        /**
         * Sets the phosphorus of this activity
         *
         * @param phosphorus Phosphorus of this activity
         */
        @NonNull
        public Builder setPhosphorus(@Nullable Mass phosphorus) {
            mPhosphorus = phosphorus;
            return this;
        }

        /**
         * Sets the chromium of this activity
         *
         * @param chromium Chromium of this activity
         */
        @NonNull
        public Builder setChromium(@Nullable Mass chromium) {
            mChromium = chromium;
            return this;
        }

        /**
         * Sets the totalFat of this activity
         *
         * @param totalFat TotalFat of this activity
         */
        @NonNull
        public Builder setTotalFat(@Nullable Mass totalFat) {
            mTotalFat = totalFat;
            return this;
        }

        /**
         * Sets the calcium of this activity
         *
         * @param calcium Calcium of this activity
         */
        @NonNull
        public Builder setCalcium(@Nullable Mass calcium) {
            mCalcium = calcium;
            return this;
        }

        /**
         * Sets the vitaminC of this activity
         *
         * @param vitaminC VitaminC of this activity
         */
        @NonNull
        public Builder setVitaminC(@Nullable Mass vitaminC) {
            mVitaminC = vitaminC;
            return this;
        }

        /**
         * Sets the vitaminE of this activity
         *
         * @param vitaminE VitaminE of this activity
         */
        @NonNull
        public Builder setVitaminE(@Nullable Mass vitaminE) {
            mVitaminE = vitaminE;
            return this;
        }

        /**
         * Sets the biotin of this activity
         *
         * @param biotin Biotin of this activity
         */
        @NonNull
        public Builder setBiotin(@Nullable Mass biotin) {
            mBiotin = biotin;
            return this;
        }

        /**
         * Sets the vitaminD of this activity
         *
         * @param vitaminD VitaminD of this activity
         */
        @NonNull
        public Builder setVitaminD(@Nullable Mass vitaminD) {
            mVitaminD = vitaminD;
            return this;
        }

        /**
         * Sets the niacin of this activity
         *
         * @param niacin Niacin of this activity
         */
        @NonNull
        public Builder setNiacin(@Nullable Mass niacin) {
            mNiacin = niacin;
            return this;
        }

        /**
         * Sets the magnesium of this activity
         *
         * @param magnesium Magnesium of this activity
         */
        @NonNull
        public Builder setMagnesium(@Nullable Mass magnesium) {
            mMagnesium = magnesium;
            return this;
        }

        /**
         * Sets the totalCarbohydrate of this activity
         *
         * @param totalCarbohydrate TotalCarbohydrate of this activity
         */
        @NonNull
        public Builder setTotalCarbohydrate(@Nullable Mass totalCarbohydrate) {
            mTotalCarbohydrate = totalCarbohydrate;
            return this;
        }

        /**
         * Sets the vitaminK of this activity
         *
         * @param vitaminK VitaminK of this activity
         */
        @NonNull
        public Builder setVitaminK(@Nullable Mass vitaminK) {
            mVitaminK = vitaminK;
            return this;
        }

        /**
         * Sets the polyunsaturatedFat of this activity
         *
         * @param polyunsaturatedFat PolyunsaturatedFat of this activity
         */
        @NonNull
        public Builder setPolyunsaturatedFat(@Nullable Mass polyunsaturatedFat) {
            mPolyunsaturatedFat = polyunsaturatedFat;
            return this;
        }

        /**
         * Sets the saturatedFat of this activity
         *
         * @param saturatedFat SaturatedFat of this activity
         */
        @NonNull
        public Builder setSaturatedFat(@Nullable Mass saturatedFat) {
            mSaturatedFat = saturatedFat;
            return this;
        }

        /**
         * Sets the sodium of this activity
         *
         * @param sodium Sodium of this activity
         */
        @NonNull
        public Builder setSodium(@Nullable Mass sodium) {
            mSodium = sodium;
            return this;
        }

        /**
         * Sets the folate of this activity
         *
         * @param folate Folate of this activity
         */
        @NonNull
        public Builder setFolate(@Nullable Mass folate) {
            mFolate = folate;
            return this;
        }

        /**
         * Sets the monounsaturatedFat of this activity
         *
         * @param monounsaturatedFat MonounsaturatedFat of this activity
         */
        @NonNull
        public Builder setMonounsaturatedFat(@Nullable Mass monounsaturatedFat) {
            mMonounsaturatedFat = monounsaturatedFat;
            return this;
        }

        /**
         * Sets the pantothenicAcid of this activity
         *
         * @param pantothenicAcid PantothenicAcid of this activity
         */
        @NonNull
        public Builder setPantothenicAcid(@Nullable Mass pantothenicAcid) {
            mPantothenicAcid = pantothenicAcid;
            return this;
        }

        /**
         * Sets the name of this activity
         *
         * @param mealName Name of this activity
         */
        @NonNull
        public Builder setMealName(@NonNull String mealName) {
            mMealName = mealName;
            return this;
        }

        /**
         * Sets the iron of this activity
         *
         * @param iron Iron of this activity
         */
        @NonNull
        public Builder setIron(@Nullable Mass iron) {
            mIron = iron;
            return this;
        }

        /**
         * Sets the vitaminA of this activity
         *
         * @param vitaminA VitaminA of this activity
         */
        @NonNull
        public Builder setVitaminA(@Nullable Mass vitaminA) {
            mVitaminA = vitaminA;
            return this;
        }

        /**
         * Sets the folicAcid of this activity
         *
         * @param folicAcid FolicAcid of this activity
         */
        @NonNull
        public Builder setFolicAcid(@Nullable Mass folicAcid) {
            mFolicAcid = folicAcid;
            return this;
        }

        /**
         * Sets the sugar of this activity
         *
         * @param sugar Sugar of this activity
         */
        @NonNull
        public Builder setSugar(@Nullable Mass sugar) {
            mSugar = sugar;
            return this;
        }

        /**
         * @return Object of {@link NutritionRecord}
         */
        @NonNull
        public NutritionRecord build() {
            return new NutritionRecord(
                    mMetadata,
                    mStartTime,
                    mStartZoneOffset,
                    mEndTime,
                    mEndZoneOffset,
                    mUnsaturatedFat,
                    mPotassium,
                    mThiamin,
                    mMealType,
                    mTransFat,
                    mManganese,
                    mEnergyFromFat,
                    mCaffeine,
                    mDietaryFiber,
                    mSelenium,
                    mVitaminB6,
                    mProtein,
                    mChloride,
                    mCholesterol,
                    mCopper,
                    mIodine,
                    mVitaminB12,
                    mZinc,
                    mRiboflavin,
                    mEnergy,
                    mMolybdenum,
                    mPhosphorus,
                    mChromium,
                    mTotalFat,
                    mCalcium,
                    mVitaminC,
                    mVitaminE,
                    mBiotin,
                    mVitaminD,
                    mNiacin,
                    mMagnesium,
                    mTotalCarbohydrate,
                    mVitaminK,
                    mPolyunsaturatedFat,
                    mSaturatedFat,
                    mSodium,
                    mFolate,
                    mMonounsaturatedFat,
                    mPantothenicAcid,
                    mMealName,
                    mIron,
                    mVitaminA,
                    mFolicAcid,
                    mSugar);
        }
    }

    /**
     * Metric identifier to get total biotin using aggregate APIs in {@link HealthConnectManager}
     */
    @NonNull
    public static final AggregationType<Mass> BIOTIN_TOTAL =
            new AggregationType<>(
                    NUTRITION_RECORD_BIOTIN_TOTAL,
                    AggregationType.SUM,
                    RECORD_TYPE_NUTRITION,
                    Mass.class);

    /**
     * Metric identifier to get total caffeine using aggregate APIs in {@link HealthConnectManager}
     */
    @NonNull
    public static final AggregationType<Mass> CAFFEINE_TOTAL =
            new AggregationType<>(
                    NUTRITION_RECORD_CAFFEINE_TOTAL,
                    AggregationType.SUM,
                    RECORD_TYPE_NUTRITION,
                    Mass.class);

    /**
     * Metric identifier to get total calcium using aggregate APIs in {@link HealthConnectManager}
     */
    @NonNull
    public static final AggregationType<Mass> CALCIUM_TOTAL =
            new AggregationType<>(
                    NUTRITION_RECORD_CALCIUM_TOTAL,
                    AggregationType.SUM,
                    RECORD_TYPE_NUTRITION,
                    Mass.class);

    /**
     * Metric identifier to get total chloride using aggregate APIs in {@link HealthConnectManager}
     */
    @NonNull
    public static final AggregationType<Mass> CHLORIDE_TOTAL =
            new AggregationType<>(
                    NUTRITION_RECORD_CHLORIDE_TOTAL,
                    AggregationType.SUM,
                    RECORD_TYPE_NUTRITION,
                    Mass.class);

    /**
     * Metric identifier to get total cholesterol using aggregate APIs in {@link
     * HealthConnectManager}
     */
    @NonNull
    public static final AggregationType<Mass> CHOLESTEROL_TOTAL =
            new AggregationType<>(
                    NUTRITION_RECORD_CHOLESTEROL_TOTAL,
                    AggregationType.SUM,
                    RECORD_TYPE_NUTRITION,
                    Mass.class);

    /**
     * Metric identifier to get total chromium using aggregate APIs in {@link HealthConnectManager}
     */
    @NonNull
    public static final AggregationType<Mass> CHROMIUM_TOTAL =
            new AggregationType<>(
                    NUTRITION_RECORD_CHROMIUM_TOTAL,
                    AggregationType.SUM,
                    RECORD_TYPE_NUTRITION,
                    Mass.class);

    /**
     * Metric identifier to get total copper using aggregate APIs in {@link HealthConnectManager}
     */
    @NonNull
    public static final AggregationType<Mass> COPPER_TOTAL =
            new AggregationType<>(
                    NUTRITION_RECORD_COPPER_TOTAL,
                    AggregationType.SUM,
                    RECORD_TYPE_NUTRITION,
                    Mass.class);

    /**
     * Metric identifier to get total dietary fibre using aggregate APIs in {@link
     * HealthConnectManager}
     */
    @NonNull
    public static final AggregationType<Mass> DIETARY_FIBER_TOTAL =
            new AggregationType<>(
                    NUTRITION_RECORD_DIETARY_FIBER_TOTAL,
                    AggregationType.SUM,
                    RECORD_TYPE_NUTRITION,
                    Mass.class);

    /**
     * Metric identifier to get total energy using aggregate APIs in {@link HealthConnectManager}
     */
    @NonNull
    public static final AggregationType<Energy> ENERGY_TOTAL =
            new AggregationType<>(
                    NUTRITION_RECORD_ENERGY_TOTAL,
                    AggregationType.SUM,
                    RECORD_TYPE_NUTRITION,
                    Energy.class);

    /**
     * Metric identifier to get total energy from fat using aggregate APIs in {@link
     * HealthConnectManager}
     */
    @NonNull
    public static final AggregationType<Energy> ENERGY_FROM_FAT_TOTAL =
            new AggregationType<>(
                    NUTRITION_RECORD_ENERGY_FROM_FAT_TOTAL,
                    AggregationType.SUM,
                    RECORD_TYPE_NUTRITION,
                    Energy.class);

    /**
     * Metric identifier to get total folate using aggregate APIs in {@link HealthConnectManager}
     */
    @NonNull
    public static final AggregationType<Mass> FOLATE_TOTAL =
            new AggregationType<>(
                    NUTRITION_RECORD_FOLATE_TOTAL,
                    AggregationType.SUM,
                    RECORD_TYPE_NUTRITION,
                    Mass.class);

    /**
     * Metric identifier to get total folic acid using aggregate APIs in {@link
     * HealthConnectManager}
     */
    @NonNull
    public static final AggregationType<Mass> FOLIC_ACID_TOTAL =
            new AggregationType<>(
                    NUTRITION_RECORD_FOLIC_ACID_TOTAL,
                    AggregationType.SUM,
                    RECORD_TYPE_NUTRITION,
                    Mass.class);

    /**
     * Metric identifier to get total iodine using aggregate APIs in {@link HealthConnectManager}
     */
    @NonNull
    public static final AggregationType<Mass> IODINE_TOTAL =
            new AggregationType<>(
                    NUTRITION_RECORD_IODINE_TOTAL,
                    AggregationType.SUM,
                    RECORD_TYPE_NUTRITION,
                    Mass.class);

    /** Metric identifier to get total iron using aggregate APIs in {@link HealthConnectManager} */
    @NonNull
    public static final AggregationType<Mass> IRON_TOTAL =
            new AggregationType<>(
                    NUTRITION_RECORD_IRON_TOTAL,
                    AggregationType.SUM,
                    RECORD_TYPE_NUTRITION,
                    Mass.class);

    /**
     * Metric identifier to get total magnesium using aggregate APIs in {@link HealthConnectManager}
     */
    @NonNull
    public static final AggregationType<Mass> MAGNESIUM_TOTAL =
            new AggregationType<>(
                    NUTRITION_RECORD_MAGNESIUM_TOTAL,
                    AggregationType.SUM,
                    RECORD_TYPE_NUTRITION,
                    Mass.class);

    /**
     * Metric identifier to get total manganese using aggregate APIs in {@link HealthConnectManager}
     */
    @NonNull
    public static final AggregationType<Mass> MANGANESE_TOTAL =
            new AggregationType<>(
                    NUTRITION_RECORD_MANGANESE_TOTAL,
                    AggregationType.SUM,
                    RECORD_TYPE_NUTRITION,
                    Mass.class);

    /**
     * Metric identifier to get total molybdenum using aggregate APIs in {@link
     * HealthConnectManager}
     */
    @NonNull
    public static final AggregationType<Mass> MOLYBDENUM_TOTAL =
            new AggregationType<>(
                    NUTRITION_RECORD_MOLYBDENUM_TOTAL,
                    AggregationType.SUM,
                    RECORD_TYPE_NUTRITION,
                    Mass.class);

    /**
     * Metric identifier to get total monounsaturated fat using aggregate APIs in {@link
     * HealthConnectManager}
     */
    @NonNull
    public static final AggregationType<Mass> MONOUNSATURATED_FAT_TOTAL =
            new AggregationType<>(
                    NUTRITION_RECORD_MONOUNSATURATED_FAT_TOTAL,
                    AggregationType.SUM,
                    RECORD_TYPE_NUTRITION,
                    Mass.class);

    private final int mMealType;
    private final Mass mUnsaturatedFat;
    private final Mass mPotassium;
    private final Mass mThiamin;
    private final Mass mTransFat;
    private final Mass mManganese;
    private final Energy mEnergyFromFat;
    private final Mass mCaffeine;
    private final Mass mDietaryFiber;
    private final Mass mSelenium;
    private final Mass mVitaminB6;
    private final Mass mProtein;
    private final Mass mChloride;
    private final Mass mCholesterol;
    private final Mass mCopper;
    private final Mass mIodine;
    private final Mass mVitaminB12;
    private final Mass mZinc;
    private final Mass mRiboflavin;
    private final Energy mEnergy;
    private final Mass mMolybdenum;
    private final Mass mPhosphorus;
    private final Mass mChromium;
    private final Mass mTotalFat;
    private final Mass mCalcium;
    private final Mass mVitaminC;
    private final Mass mVitaminE;
    private final Mass mBiotin;
    private final Mass mVitaminD;
    private final Mass mNiacin;
    private final Mass mMagnesium;
    private final Mass mTotalCarbohydrate;
    private final Mass mVitaminK;
    private final Mass mPolyunsaturatedFat;
    private final Mass mSaturatedFat;
    private final Mass mSodium;
    private final Mass mFolate;
    private final Mass mMonounsaturatedFat;
    private final Mass mPantothenicAcid;
    private final String mMealName;
    private final Mass mIron;
    private final Mass mVitaminA;
    private final Mass mFolicAcid;
    private final Mass mSugar;

    /**
     * @param metadata Metadata to be associated with the record. See {@link Metadata}.
     * @param startTime Start time of this activity
     * @param startZoneOffset Zone offset of the user when the activity started
     * @param endTime End time of this activity
     * @param endZoneOffset Zone offset of the user when the activity finished
     * @param unsaturatedFat UnsaturatedFat of this activity in {@link Mass} unit. Optional field.
     * @param potassium Potassium of this activity in {@link Mass} unit. Optional field.
     * @param thiamin Thiamin of this activity in {@link Mass} unit. Optional field.
     * @param mealType Type of meal related to the nutrients consumed. Optional, enum field. Allowed
     *     values: {@link MealType.MealTypes}
     * @param transFat TransFat of this activity in {@link Mass} unit. Optional field.
     * @param manganese Manganese of this activity in {@link Mass} unit. Optional field.
     * @param energyFromFat EnergyFromFat of this activity in {@link Energy} unit. Optional field.
     * @param caffeine Caffeine of this activity in {@link Mass} unit. Optional field.
     * @param dietaryFiber DietaryFiber of this activity in {@link Mass} unit. Optional field.
     * @param selenium Selenium of this activity in {@link Mass} unit. Optional field.
     * @param vitaminB6 VitaminB6 of this activity in {@link Mass} unit. Optional field.
     * @param protein Protein of this activity in {@link Mass} unit. Optional field.
     * @param chloride Chloride of this activity in {@link Mass} unit. Optional field.
     * @param cholesterol Cholesterol of this activity in {@link Mass} unit. Optional field.
     * @param copper Copper of this activity in {@link Mass} unit. Optional field.
     * @param iodine Iodine of this activity in {@link Mass} unit. Optional field.
     * @param vitaminB12 VitaminB12 of this activity in {@link Mass} unit. Optional field.
     * @param zinc Zinc of this activity in {@link Mass} unit. Optional field.
     * @param riboflavin Riboflavin of this activity in {@link Mass} unit. Optional field.
     * @param energy Energy of this activity in {@link Energy} unit. Optional field.
     * @param molybdenum Molybdenum of this activity in {@link Mass} unit. Optional field.
     * @param phosphorus Phosphorus of this activity in {@link Mass} unit. Optional field.
     * @param chromium Chromium of this activity in {@link Mass} unit. Optional field.
     * @param totalFat TotalFat of this activity in {@link Mass} unit. Optional field.
     * @param calcium Calcium of this activity in {@link Mass} unit. Optional field.
     * @param vitaminC VitaminC of this activity in {@link Mass} unit. Optional field.
     * @param vitaminE VitaminE of this activity in {@link Mass} unit. Optional field.
     * @param biotin Biotin of this activity in {@link Mass} unit. Optional field.
     * @param vitaminD VitaminD of this activity in {@link Mass} unit. Optional field.
     * @param niacin Niacin of this activity in {@link Mass} unit. Optional field.
     * @param magnesium Magnesium of this activity in {@link Mass} unit. Optional field.
     * @param totalCarbohydrate TotalCarbohydrate of this activity in {@link Mass} unit. Optional
     *     field.
     * @param vitaminK VitaminK of this activity in {@link Mass} unit. Optional field.
     * @param polyunsaturatedFat PolyunsaturatedFat of this activity in {@link Mass} unit. Optional
     *     field.
     * @param saturatedFat SaturatedFat of this activity in {@link Mass} unit. Optional field.
     * @param sodium Sodium of this activity in {@link Mass} unit. Optional field.
     * @param folate Folate of this activity in {@link Mass} unit. Optional field.
     * @param monounsaturatedFat MonounsaturatedFat of this activity in {@link Mass} unit. Optional
     *     field.
     * @param pantothenicAcid PantothenicAcid of this activity in {@link Mass} unit. Optional field.
     * @param mealName Name of the meal. Optional field.
     * @param iron Iron of this activity in {@link Mass} unit. Optional field.
     * @param vitaminA VitaminA of this activity in {@link Mass} unit. Optional field.
     * @param folicAcid FolicAcid of this activity in {@link Mass} unit. Optional field.
     * @param sugar Sugar of this activity in {@link Mass} unit. Optional field.
     */
    private NutritionRecord(
            @NonNull Metadata metadata,
            @NonNull Instant startTime,
            @NonNull ZoneOffset startZoneOffset,
            @NonNull Instant endTime,
            @NonNull ZoneOffset endZoneOffset,
            @Nullable Mass unsaturatedFat,
            @Nullable Mass potassium,
            @Nullable Mass thiamin,
            @MealType.MealTypes int mealType,
            @Nullable Mass transFat,
            @Nullable Mass manganese,
            @Nullable Energy energyFromFat,
            @Nullable Mass caffeine,
            @Nullable Mass dietaryFiber,
            @Nullable Mass selenium,
            @Nullable Mass vitaminB6,
            @Nullable Mass protein,
            @Nullable Mass chloride,
            @Nullable Mass cholesterol,
            @Nullable Mass copper,
            @Nullable Mass iodine,
            @Nullable Mass vitaminB12,
            @Nullable Mass zinc,
            @Nullable Mass riboflavin,
            @Nullable Energy energy,
            @Nullable Mass molybdenum,
            @Nullable Mass phosphorus,
            @Nullable Mass chromium,
            @Nullable Mass totalFat,
            @Nullable Mass calcium,
            @Nullable Mass vitaminC,
            @Nullable Mass vitaminE,
            @Nullable Mass biotin,
            @Nullable Mass vitaminD,
            @Nullable Mass niacin,
            @Nullable Mass magnesium,
            @Nullable Mass totalCarbohydrate,
            @Nullable Mass vitaminK,
            @Nullable Mass polyunsaturatedFat,
            @Nullable Mass saturatedFat,
            @Nullable Mass sodium,
            @Nullable Mass folate,
            @Nullable Mass monounsaturatedFat,
            @Nullable Mass pantothenicAcid,
            @Nullable String mealName,
            @Nullable Mass iron,
            @Nullable Mass vitaminA,
            @Nullable Mass folicAcid,
            @Nullable Mass sugar) {
        super(metadata, startTime, startZoneOffset, endTime, endZoneOffset);
        mUnsaturatedFat = unsaturatedFat;
        mPotassium = potassium;
        mThiamin = thiamin;
        mMealType = mealType;
        mTransFat = transFat;
        mManganese = manganese;
        mEnergyFromFat = energyFromFat;
        mCaffeine = caffeine;
        mDietaryFiber = dietaryFiber;
        mSelenium = selenium;
        mVitaminB6 = vitaminB6;
        mProtein = protein;
        mChloride = chloride;
        mCholesterol = cholesterol;
        mCopper = copper;
        mIodine = iodine;
        mVitaminB12 = vitaminB12;
        mZinc = zinc;
        mRiboflavin = riboflavin;
        mEnergy = energy;
        mMolybdenum = molybdenum;
        mPhosphorus = phosphorus;
        mChromium = chromium;
        mTotalFat = totalFat;
        mCalcium = calcium;
        mVitaminC = vitaminC;
        mVitaminE = vitaminE;
        mBiotin = biotin;
        mVitaminD = vitaminD;
        mNiacin = niacin;
        mMagnesium = magnesium;
        mTotalCarbohydrate = totalCarbohydrate;
        mVitaminK = vitaminK;
        mPolyunsaturatedFat = polyunsaturatedFat;
        mSaturatedFat = saturatedFat;
        mSodium = sodium;
        mFolate = folate;
        mMonounsaturatedFat = monounsaturatedFat;
        mPantothenicAcid = pantothenicAcid;
        mMealName = mealName;
        mIron = iron;
        mVitaminA = vitaminA;
        mFolicAcid = folicAcid;
        mSugar = sugar;
    }

    /**
     * @return mealType
     */
    @MealType.MealTypes
    public int getMealType() {
        return mMealType;
    }

    /**
     * @return unsaturatedFat
     */
    @Nullable
    public Mass getUnsaturatedFat() {
        return mUnsaturatedFat;
    }

    /**
     * @return potassium
     */
    @Nullable
    public Mass getPotassium() {
        return mPotassium;
    }

    /**
     * @return thiamin
     */
    @Nullable
    public Mass getThiamin() {
        return mThiamin;
    }

    /**
     * @return transFat in {@link Mass} unit.
     */
    @Nullable
    public Mass getTransFat() {
        return mTransFat;
    }

    /**
     * @return manganese in {@link Mass} unit.
     */
    @Nullable
    public Mass getManganese() {
        return mManganese;
    }

    /**
     * @return energyFromFat in {@link Energy} unit.
     */
    @Nullable
    public Energy getEnergyFromFat() {
        return mEnergyFromFat;
    }

    /**
     * @return caffeine in {@link Mass} unit.
     */
    @Nullable
    public Mass getCaffeine() {
        return mCaffeine;
    }

    /**
     * @return dietaryFiber in {@link Mass} unit.
     */
    @Nullable
    public Mass getDietaryFiber() {
        return mDietaryFiber;
    }

    /**
     * @return selenium in {@link Mass} unit.
     */
    @Nullable
    public Mass getSelenium() {
        return mSelenium;
    }

    /**
     * @return vitaminB6 in {@link Mass} unit.
     */
    @Nullable
    public Mass getVitaminB6() {
        return mVitaminB6;
    }

    /**
     * @return protein in {@link Mass} unit.
     */
    @Nullable
    public Mass getProtein() {
        return mProtein;
    }

    /**
     * @return chloride in {@link Mass} unit.
     */
    @Nullable
    public Mass getChloride() {
        return mChloride;
    }

    /**
     * @return cholesterol in {@link Mass} unit.
     */
    @Nullable
    public Mass getCholesterol() {
        return mCholesterol;
    }

    /**
     * @return copper in {@link Mass} unit.
     */
    @Nullable
    public Mass getCopper() {
        return mCopper;
    }

    /**
     * @return iodine in {@link Mass} unit.
     */
    @Nullable
    public Mass getIodine() {
        return mIodine;
    }

    /**
     * @return vitaminB12 in {@link Mass} unit.
     */
    @Nullable
    public Mass getVitaminB12() {
        return mVitaminB12;
    }

    /**
     * @return zinc in {@link Mass} unit.
     */
    @Nullable
    public Mass getZinc() {
        return mZinc;
    }

    /**
     * @return riboflavin in {@link Mass} unit.
     */
    @Nullable
    public Mass getRiboflavin() {
        return mRiboflavin;
    }

    /**
     * @return energy in {@link Energy} unit.
     */
    @Nullable
    public Energy getEnergy() {
        return mEnergy;
    }

    /**
     * @return molybdenum in {@link Mass} unit.
     */
    @Nullable
    public Mass getMolybdenum() {
        return mMolybdenum;
    }

    /**
     * @return phosphorus in {@link Mass} unit.
     */
    @Nullable
    public Mass getPhosphorus() {
        return mPhosphorus;
    }

    /**
     * @return chromium in {@link Mass} unit.
     */
    @Nullable
    public Mass getChromium() {
        return mChromium;
    }

    /**
     * @return totalFat in {@link Mass} unit.
     */
    @Nullable
    public Mass getTotalFat() {
        return mTotalFat;
    }

    /**
     * @return calcium in {@link Mass} unit.
     */
    @Nullable
    public Mass getCalcium() {
        return mCalcium;
    }

    /**
     * @return vitaminC in {@link Mass} unit.
     */
    @Nullable
    public Mass getVitaminC() {
        return mVitaminC;
    }

    /**
     * @return vitaminE in {@link Mass} unit.
     */
    @Nullable
    public Mass getVitaminE() {
        return mVitaminE;
    }

    /**
     * @return biotin in {@link Mass} unit.
     */
    @Nullable
    public Mass getBiotin() {
        return mBiotin;
    }

    /**
     * @return vitaminD in {@link Mass} unit.
     */
    @Nullable
    public Mass getVitaminD() {
        return mVitaminD;
    }

    /**
     * @return niacin in {@link Mass} unit.
     */
    @Nullable
    public Mass getNiacin() {
        return mNiacin;
    }

    /**
     * @return magnesium in {@link Mass} unit.
     */
    @Nullable
    public Mass getMagnesium() {
        return mMagnesium;
    }

    /**
     * @return totalCarbohydrate in {@link Mass} unit.
     */
    @Nullable
    public Mass getTotalCarbohydrate() {
        return mTotalCarbohydrate;
    }

    /**
     * @return vitaminK in {@link Mass} unit.
     */
    @Nullable
    public Mass getVitaminK() {
        return mVitaminK;
    }

    /**
     * @return polyunsaturatedFat in {@link Mass} unit.
     */
    @Nullable
    public Mass getPolyunsaturatedFat() {
        return mPolyunsaturatedFat;
    }

    /**
     * @return saturatedFat in {@link Mass} unit.
     */
    @Nullable
    public Mass getSaturatedFat() {
        return mSaturatedFat;
    }

    /**
     * @return sodium in {@link Mass} unit.
     */
    @Nullable
    public Mass getSodium() {
        return mSodium;
    }

    /**
     * @return folate in {@link Mass} unit.
     */
    @Nullable
    public Mass getFolate() {
        return mFolate;
    }

    /**
     * @return monounsaturatedFat in {@link Mass} unit.
     */
    @Nullable
    public Mass getMonounsaturatedFat() {
        return mMonounsaturatedFat;
    }

    /**
     * @return pantothenicAcid in {@link Mass} unit.
     */
    @Nullable
    public Mass getPantothenicAcid() {
        return mPantothenicAcid;
    }

    /**
     * @return the meal name.
     */
    @Nullable
    public String getMealName() {
        return mMealName;
    }

    /**
     * @return iron in {@link Mass} unit.
     */
    @Nullable
    public Mass getIron() {
        return mIron;
    }

    /**
     * @return vitaminA in {@link Mass} unit.
     */
    @Nullable
    public Mass getVitaminA() {
        return mVitaminA;
    }

    /**
     * @return folicAcid in {@link Mass} unit.
     */
    @Nullable
    public Mass getFolicAcid() {
        return mFolicAcid;
    }

    /**
     * @return sugar in {@link Mass} unit.
     */
    @Nullable
    public Mass getSugar() {
        return mSugar;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param o the reference object with which to compare.
     * @return {@code true} if this object is the same as the obj
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!super.equals(o)) return false;
        NutritionRecord that = (NutritionRecord) o;
        return getMealType() == that.getMealType()
                && Objects.equals(getUnsaturatedFat(), that.getUnsaturatedFat())
                && Objects.equals(getPotassium(), that.getPotassium())
                && Objects.equals(getThiamin(), that.getThiamin())
                && Objects.equals(getTransFat(), that.getTransFat())
                && Objects.equals(getManganese(), that.getManganese())
                && Objects.equals(getEnergyFromFat(), that.getEnergyFromFat())
                && Objects.equals(getCaffeine(), that.getCaffeine())
                && Objects.equals(getDietaryFiber(), that.getDietaryFiber())
                && Objects.equals(getSelenium(), that.getSelenium())
                && Objects.equals(getVitaminB6(), that.getVitaminB6())
                && Objects.equals(getProtein(), that.getProtein())
                && Objects.equals(getChloride(), that.getChloride())
                && Objects.equals(getCholesterol(), that.getCholesterol())
                && Objects.equals(getCopper(), that.getCopper())
                && Objects.equals(getIodine(), that.getIodine())
                && Objects.equals(getVitaminB12(), that.getVitaminB12())
                && Objects.equals(getZinc(), that.getZinc())
                && Objects.equals(getRiboflavin(), that.getRiboflavin())
                && Objects.equals(getEnergy(), that.getEnergy())
                && Objects.equals(getMolybdenum(), that.getMolybdenum())
                && Objects.equals(getPhosphorus(), that.getPhosphorus())
                && Objects.equals(getChromium(), that.getChromium())
                && Objects.equals(getTotalFat(), that.getTotalFat())
                && Objects.equals(getCalcium(), that.getCalcium())
                && Objects.equals(getVitaminC(), that.getVitaminC())
                && Objects.equals(getVitaminE(), that.getVitaminE())
                && Objects.equals(getBiotin(), that.getBiotin())
                && Objects.equals(getVitaminD(), that.getVitaminD())
                && Objects.equals(getNiacin(), that.getNiacin())
                && Objects.equals(getMagnesium(), that.getMagnesium())
                && Objects.equals(getTotalCarbohydrate(), that.getTotalCarbohydrate())
                && Objects.equals(getVitaminK(), that.getVitaminK())
                && Objects.equals(getPolyunsaturatedFat(), that.getPolyunsaturatedFat())
                && Objects.equals(getSaturatedFat(), that.getSaturatedFat())
                && Objects.equals(getSodium(), that.getSodium())
                && Objects.equals(getFolate(), that.getFolate())
                && Objects.equals(getMonounsaturatedFat(), that.getMonounsaturatedFat())
                && Objects.equals(getPantothenicAcid(), that.getPantothenicAcid())
                && Objects.equals(getMealName(), that.getMealName())
                && Objects.equals(getIron(), that.getIron())
                && Objects.equals(getVitaminA(), that.getVitaminA())
                && Objects.equals(getFolicAcid(), that.getFolicAcid())
                && Objects.equals(getSugar(), that.getSugar());
    }

    /**
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                getMealType(),
                getUnsaturatedFat(),
                getPotassium(),
                getThiamin(),
                getTransFat(),
                getManganese(),
                getEnergyFromFat(),
                getCaffeine(),
                getDietaryFiber(),
                getSelenium(),
                getVitaminB6(),
                getProtein(),
                getChloride(),
                getCholesterol(),
                getCopper(),
                getIodine(),
                getVitaminB12(),
                getZinc(),
                getRiboflavin(),
                getEnergy(),
                getMolybdenum(),
                getPhosphorus(),
                getChromium(),
                getTotalFat(),
                getCalcium(),
                getVitaminC(),
                getVitaminE(),
                getBiotin(),
                getVitaminD(),
                getNiacin(),
                getMagnesium(),
                getTotalCarbohydrate(),
                getVitaminK(),
                getPolyunsaturatedFat(),
                getSaturatedFat(),
                getSodium(),
                getFolate(),
                getMonounsaturatedFat(),
                getPantothenicAcid(),
                getMealName(),
                getIron(),
                getVitaminA(),
                getFolicAcid(),
                getSugar());
    }
}
