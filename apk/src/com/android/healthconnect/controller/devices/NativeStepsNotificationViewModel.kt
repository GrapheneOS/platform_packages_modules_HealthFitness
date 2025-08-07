/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.healthconnect.controller.devices

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.android.healthconnect.controller.shared.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class NativeStepsNotificationViewModel
@Inject
constructor(@ApplicationContext private val context: Context) : ViewModel() {
    private val sharedPreferences =
        context.getSharedPreferences(Constants.USER_ACTIVITY_TRACKER, Context.MODE_PRIVATE)

    private val _wasSeen = MutableLiveData<Boolean>(false)
    val wasSeen: LiveData<Boolean>
        get() = _wasSeen

    fun loadWasSeen() {
        val wasSeen = sharedPreferences.getBoolean(Constants.NATIVE_STEPS_BANNER_SEEN, false)
        _wasSeen.postValue(wasSeen)
    }
}
