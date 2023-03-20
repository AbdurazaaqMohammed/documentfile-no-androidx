/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appactions.interaction.capabilities.productivity

import androidx.appactions.interaction.capabilities.core.impl.converters.ParamValueConverter
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters
import androidx.appactions.interaction.capabilities.core.impl.converters.UnionTypeSpec
import androidx.appactions.interaction.capabilities.core.values.SearchAction
import androidx.appactions.interaction.capabilities.core.values.Timer

class TimerValue private constructor(
    val asTimer: Timer?,
    val asTimerFilter: SearchAction<Timer>?,
) {
    constructor(timer: Timer) : this(timer, null)

    // TODO(b/268071906) add TimerFilter type to SearchAction
    constructor(timerFilter: SearchAction<Timer>) : this(null, timerFilter)

    companion object {
        private val TYPE_SPEC = UnionTypeSpec.Builder<TimerValue>()
            .bindMemberType(
                memberGetter = TimerValue::asTimer,
                ctor = { TimerValue(it) },
                typeSpec = TypeConverters.TIMER_TYPE_SPEC,
            )
            .bindMemberType(
                memberGetter = TimerValue::asTimerFilter,
                ctor = { TimerValue(it) },
                typeSpec = TypeConverters.createSearchActionTypeSpec(
                    TypeConverters.TIMER_TYPE_SPEC,
                ),
            )
            .build()

        internal val FROM_PARAM_VALUE = ParamValueConverter {
            TYPE_SPEC.fromStruct(it.getStructValue())
        }
    }
}
