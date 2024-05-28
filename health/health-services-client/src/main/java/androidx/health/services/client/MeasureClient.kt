/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.health.services.client

import androidx.health.services.client.data.DeltaDataType
import androidx.health.services.client.data.MeasureCapabilities
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor

/**
 * Client which provides a way to make measurements of health data on a device.
 *
 * This is optimized for apps to register live callbacks on data which may be sampled at a faster
 * rate; this is not meant to be used for long-lived subscriptions to data (for this, consider using
 * [ExerciseClient] or [PassiveMonitoringClient] depending on your use case).
 *
 * Existing subscriptions made with the [PassiveMonitoringClient] are also expected to get the data
 * generated by this client.
 */
public interface MeasureClient {
    /**
     * Registers the app for live measurement of the specified [DeltaDataType].
     *
     * The callback will be called on the main application thread. To move calls to an alternative
     * thread use [registerMeasureCallback].
     *
     * Even if data is registered for live capture, it can still be sent out in batches depending on
     * the application processor state.
     *
     * Registering a [DeltaDataType] for live measurement capture is expected to increase the sample
     * rate on the associated sensor(s); this is typically used for one-off measurements. Do not use
     * this method for background capture or workout tracking. The client is responsible for
     * ensuring that their requested [DeltaDataType] is supported on this device by checking the
     * [MeasureCapabilities]. The returned future will fail if the request is not supported on a
     * given device.
     *
     * The callback will continue to be called until the app is killed or
     * [unregisterMeasureCallbackAsync] is called.
     *
     * If the same [callback] is already registered for the given [DeltaDataType], this operation is
     * a no-op.
     *
     * @param dataType the [DeltaDataType] that needs to be measured
     * @param callback the [MeasureCallback] to receive updates from Health Services
     */
    public fun registerMeasureCallback(dataType: DeltaDataType<*, *>, callback: MeasureCallback)

    /**
     * Same as [registerMeasureCallback], except the [callback] is called on the given [Executor].
     *
     * @param dataType the [DeltaDataType] that needs to be measured
     * @param executor the [Executor] on which [callback] will be invoked
     * @param callback the [MeasureCallback] to receive updates from Health Services
     */
    public fun registerMeasureCallback(
        dataType: DeltaDataType<*, *>,
        executor: Executor,
        callback: MeasureCallback
    )

    /**
     * Unregisters the given [MeasureCallback] for updates of the given [DeltaDataType].
     *
     * @param dataType the [DeltaDataType] that needs to be unregistered
     * @param callback the [MeasureCallback] which was used in registration
     * @return a [ListenableFuture] that completes when the un-registration succeeds in Health
     *   Services. This is a no-op if the callback has already been unregistered.
     */
    public fun unregisterMeasureCallbackAsync(
        dataType: DeltaDataType<*, *>,
        callback: MeasureCallback
    ): ListenableFuture<Void>

    /**
     * Returns the [MeasureCapabilities] of this client for the device.
     *
     * This can be used to determine what [DeltaDataType]s this device supports for live
     * measurement. Clients should use the capabilities to inform their requests since Health
     * Services will typically reject requests made for [DeltaDataType]s which are not enabled for
     * measurement.
     *
     * @return a [ListenableFuture] containing the [MeasureCapabilities] for this device
     */
    public fun getCapabilitiesAsync(): ListenableFuture<MeasureCapabilities>
}
