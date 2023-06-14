/*
 * Copyright 2021 The Android Open Source Project
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

@file:RequiresApi(31) // TODO(b/200306659): Remove and replace with annotation on package-info.java

package androidx.camera.camera2.pipe.compat

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraExtensionSession
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.UnsafeWrapper
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.internal.CameraErrorListener
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.Executor
import kotlin.reflect.KClass
import kotlinx.atomicfu.AtomicLong
import kotlinx.atomicfu.atomic

/**
 * Interface shim for [CameraExtensionSession] with minor modifications.
 *
 * This interface has been modified to correct nullness, adjust exceptions, and to return or produce
 * wrapper interfaces instead of the native Camera2 types.
 */
internal interface CameraExtensionSessionWrapper : CameraCaptureSessionWrapper, UnsafeWrapper,
    AutoCloseable {

    /**
     * @return The [CameraDeviceWrapper] that created this CameraExtensionSession
     * @see [CameraExtensionSession.getDevice]
     */
    override val device: CameraDeviceWrapper

    /** @see [CameraExtensionSession.stopRepeating]. */
    override fun stopRepeating(): Boolean

    /** @see CameraExtensionSession.StateCallback */
    interface StateCallback : OnSessionFinalized {
        /** @see CameraExtensionSession.StateCallback.onClosed */
        fun onClosed(session: CameraExtensionSessionWrapper)

        /** @see CameraExtensionSession.StateCallback.onConfigureFailed */
        fun onConfigureFailed(session: CameraExtensionSessionWrapper)

        /** @see CameraExtensionSession.StateCallback.onConfigured */
        fun onConfigured(session: CameraExtensionSessionWrapper)
    }
}

@RequiresApi(31) // TODO(b/200306659): Remove and replace with annotation on package-info.java
internal class AndroidExtensionSessionStateCallback(
    private val device: CameraDeviceWrapper,
    private val stateCallback: CameraExtensionSessionWrapper.StateCallback,
    lastStateCallback: OnSessionFinalized?,
    private val cameraErrorListener: CameraErrorListener,
    private val interopSessionStateCallback: CameraExtensionSession.StateCallback? = null,
    private val callbackExecutor: Executor
) : CameraExtensionSession.StateCallback() {
    private val _lastStateCallback = atomic(lastStateCallback)
    private val extensionSession = atomic<CameraExtensionSessionWrapper?>(null)

    override fun onConfigured(session: CameraExtensionSession) {
        stateCallback.onConfigured(getWrapped(session, cameraErrorListener))

        // b/249258992 - This is a workaround to ensure previous
        // CameraExtensionSession.StateCallback instances receive some kind of "finalization"
        // signal if onClosed is not fired by the framework after a subsequent session
        // has been configured.
        finalizeLastSession()
        interopSessionStateCallback?.onConfigured(session)
    }

    override fun onConfigureFailed(session: CameraExtensionSession) {
        stateCallback.onConfigureFailed(getWrapped(session, cameraErrorListener))
        finalizeSession()
        interopSessionStateCallback?.onConfigureFailed(session)
    }

    override fun onClosed(session: CameraExtensionSession) {
        stateCallback.onClosed(getWrapped(session, cameraErrorListener))
        finalizeSession()
        interopSessionStateCallback?.onClosed(session)
    }

    private fun getWrapped(
        session: CameraExtensionSession,
        cameraErrorListener: CameraErrorListener,
    ): CameraExtensionSessionWrapper {
        var local = extensionSession.value
        if (local != null) {
            return local
        }

        local = wrapSession(session, cameraErrorListener)
        if (extensionSession.compareAndSet(null, local)) {
            return local
        }
        return extensionSession.value!!
    }

    private fun wrapSession(
        session: CameraExtensionSession,
        cameraErrorListener: CameraErrorListener,
    ): CameraExtensionSessionWrapper {
        return AndroidCameraExtensionSession(device, session, cameraErrorListener, callbackExecutor)
    }

    private fun finalizeSession() {
        finalizeLastSession()
        stateCallback.onSessionFinalized()
    }

    private fun finalizeLastSession() {
        // Clear out the reference to the previous session, if one was set.
        val previousSession = _lastStateCallback.getAndSet(null)
        previousSession?.let { previousSession.onSessionFinalized() }
    }
}

@RequiresApi(31) // TODO(b/200306659): Remove and replace with annotation on package-info.java
internal open class AndroidCameraExtensionSession(
    override val device: CameraDeviceWrapper,
    private val cameraExtensionSession: CameraExtensionSession,
    private val cameraErrorListener: CameraErrorListener,
    private val callbackExecutor: Executor
) : CameraExtensionSessionWrapper {

    private val frameNumbers: AtomicLong = atomic(0L)
    private val frameNumbersMap: MutableMap<CameraExtensionSession, Long> = HashMap()

    override fun capture(
        request: CaptureRequest,
        listener: CameraCaptureSession.CaptureCallback
    ): Int? = catchAndReportCameraExceptions(device.cameraId, cameraErrorListener) {
        val frameQueue = LinkedList<Long>()
        cameraExtensionSession.capture(
            request,
            callbackExecutor,
            Camera2CaptureSessionCallbackToExtensionCaptureCallback(
                listener as Camera2CaptureCallback,
                frameQueue
            )
        )
    }

    override fun setRepeatingRequest(
        request: CaptureRequest,
        listener: CameraCaptureSession.CaptureCallback,
    ): Int? = catchAndReportCameraExceptions(device.cameraId, cameraErrorListener) {
        val frameQueue = LinkedList<Long>()
        cameraExtensionSession.setRepeatingRequest(
            request,
            callbackExecutor,
            Camera2CaptureSessionCallbackToExtensionCaptureCallback(
                listener as Camera2CaptureCallback,
                frameQueue
            )
        )
    }

    override fun stopRepeating(): Boolean =
        catchAndReportCameraExceptions(device.cameraId, cameraErrorListener) {
            cameraExtensionSession.stopRepeating()
        } != null

    override val isReprocessable: Boolean
        get() = false

    override val inputSurface: Surface?
        get() = null

    override fun abortCaptures(): Boolean = false

    override fun captureBurst(
        requests: List<CaptureRequest>,
        listener: CameraCaptureSession.CaptureCallback
    ): Int? {
        requests.forEach { captureRequest -> capture(captureRequest, listener) }
        return null
    }

    override fun setRepeatingBurst(
        requests: List<CaptureRequest>,
        listener: CameraCaptureSession.CaptureCallback
    ): Int? {
        check(requests.size == 1) {
            "CameraExtensionSession does not support setRepeatingBurst for more than one" +
                "CaptureRequest"
        }
        return setRepeatingRequest(requests.single(), listener)
    }

    override fun finalizeOutputConfigurations(
        outputConfigs: List<OutputConfigurationWrapper>
    ): Boolean {
        Log.warn { "CameraExtensionSession does not support finalizeOutputConfigurations()" }
        return false
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: KClass<T>): T? =
        when (type) {
            CameraExtensionSession::class -> cameraExtensionSession as T?
            else -> null
        }

    override fun close() {
        return cameraExtensionSession.close()
    }

    inner class Camera2CaptureSessionCallbackToExtensionCaptureCallback(
        private val captureCallback: Camera2CaptureCallback,
        private val frameQueue: Queue<Long>
    ) : CameraExtensionSession.ExtensionCaptureCallback() {

        override fun onCaptureStarted(
            session: CameraExtensionSession,
            request: CaptureRequest,
            timestamp: Long
        ) {
            val frameNumber = frameNumbers.incrementAndGet()
            frameNumbersMap[session] = frameNumber
            frameQueue.add(frameNumber)
            captureCallback.onCaptureStarted(
                request,
                frameNumber,
                timestamp
            )
        }

        override fun onCaptureProcessStarted(
            session: CameraExtensionSession,
            request: CaptureRequest
        ) {
        }

        override fun onCaptureFailed(session: CameraExtensionSession, request: CaptureRequest) {
            val frameNumber = frameQueue.remove()
            captureCallback.onCaptureFailed(
                request,
                FrameNumber(frameNumber)
            )
        }

        override fun onCaptureSequenceCompleted(session: CameraExtensionSession, sequenceId: Int) {
            val frameNumber = frameNumbersMap[session]
            captureCallback.onCaptureSequenceCompleted(sequenceId, frameNumber!!)
        }

        override fun onCaptureSequenceAborted(session: CameraExtensionSession, sequenceId: Int) {
            captureCallback.onCaptureSequenceAborted(sequenceId)
        }

        override fun onCaptureResultAvailable(
            session: CameraExtensionSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            val frameNumber = frameQueue.remove()
            captureCallback.onCaptureCompleted(request, result, FrameNumber(frameNumber))
        }
    }
}
