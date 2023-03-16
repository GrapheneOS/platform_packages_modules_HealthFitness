package com.android.healthconnect.controller.utils

import android.content.Context
import android.provider.DeviceConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

interface FeatureUtils {
    fun isSessionTypesEnabled(): Boolean
    fun isExerciseRouteEnabled(): Boolean
}

class FeatureUtilsImpl(context: Context) : FeatureUtils, DeviceConfig.OnPropertiesChangedListener {

    companion object {
        private const val HEALTH_FITNESS_FLAGS_NAMESPACE = DeviceConfig.NAMESPACE_HEALTH_FITNESS
        private const val PROPERTY_EXERCISE_ROUTE_ENABLE = "exercise_routes_enable"
        private const val PROPERTY_SESSIONS_TYPE_ENABLE = "session_types_enable"
    }

    private val lock = Any()

    init {
        DeviceConfig.addOnPropertiesChangedListener(
            HEALTH_FITNESS_FLAGS_NAMESPACE, context.mainExecutor, this)
    }

    private var isSessionTypesEnabled =
        DeviceConfig.getBoolean(HEALTH_FITNESS_FLAGS_NAMESPACE, PROPERTY_SESSIONS_TYPE_ENABLE, true)

    private var isExerciseRouteEnabled =
        DeviceConfig.getBoolean(
            HEALTH_FITNESS_FLAGS_NAMESPACE, PROPERTY_EXERCISE_ROUTE_ENABLE, true)

    override fun isSessionTypesEnabled(): Boolean {
        synchronized(lock) {
            return isSessionTypesEnabled
        }
    }

    override fun isExerciseRouteEnabled(): Boolean {
        synchronized(lock) {
            return isExerciseRouteEnabled
        }
    }

    override fun onPropertiesChanged(properties: DeviceConfig.Properties) {
        synchronized(lock) {
            if (!properties.namespace.equals(HEALTH_FITNESS_FLAGS_NAMESPACE)) {
                return
            }
            for (name in properties.keyset) {
                when (name) {
                    PROPERTY_EXERCISE_ROUTE_ENABLE ->
                        isExerciseRouteEnabled =
                            properties.getBoolean(PROPERTY_EXERCISE_ROUTE_ENABLE, true)
                    PROPERTY_SESSIONS_TYPE_ENABLE ->
                        isSessionTypesEnabled =
                            properties.getBoolean(PROPERTY_SESSIONS_TYPE_ENABLE, true)
                }
            }
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
class FeaturesModule {
    @Provides
    fun providesFeatureUtils(@ApplicationContext context: Context): FeatureUtils {
        return FeatureUtilsImpl(context)
    }
}
