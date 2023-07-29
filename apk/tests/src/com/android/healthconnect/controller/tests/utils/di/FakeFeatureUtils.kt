package com.android.healthconnect.controller.tests.utils.di

import com.android.healthconnect.controller.utils.FeatureUtils
import com.android.healthconnect.controller.utils.FeaturesModule
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

class FakeFeatureUtils : FeatureUtils {

    private var isSessionTypesEnabled = true
    private var isExerciseRoutesEnabled = true
    private var isEntryPointsEnabled = true
    private var isNewAppPriorityEnabled = false
    private var isNewInformationArchitectureEnabled = false

    fun setIsSessionTypesEnabled(boolean: Boolean) {
        isSessionTypesEnabled = boolean
    }

    fun setIsExerciseRoutesEnabled(boolean: Boolean) {
        isExerciseRoutesEnabled = boolean
    }

    fun setIsEntryPointsEnabled(boolean: Boolean) {
        isEntryPointsEnabled = boolean
    }

    fun setIsNewAppPriorityEnabled(boolean: Boolean) {
        isNewAppPriorityEnabled = boolean
    }

    fun setIsNewInformationArchitectureEnabled(boolean: Boolean) {
        isNewInformationArchitectureEnabled = boolean
    }

    override fun isNewAppPriorityEnabled(): Boolean {
        return isNewAppPriorityEnabled
    }

    override fun isNewInformationArchitectureEnabled(): Boolean {
        return isNewInformationArchitectureEnabled
    }

    override fun isSessionTypesEnabled(): Boolean {
        return isSessionTypesEnabled
    }

    override fun isExerciseRouteEnabled(): Boolean {
        return isExerciseRoutesEnabled
    }

    override fun isEntryPointsEnabled(): Boolean {
        return isEntryPointsEnabled
    }

}

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [FeaturesModule::class])
object FakeFeaturesUtilsModule {
    @Provides
    @Singleton
    fun providesFeaturesUtils() : FeatureUtils =  FakeFeatureUtils()
}
