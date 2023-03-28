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

package androidx.credentials.provider

import android.os.Bundle
import android.service.credentials.CallingAppInfo
import androidx.annotation.OptIn
import androidx.core.os.BuildCompat
import androidx.credentials.provider.utils.BeginGetCredentialUtil

/**
 * Query stage request for getting user's credentials from a given credential provider.
 *
 * <p>This request contains a list of [BeginGetCredentialOption] that have parameters
 * to be used to query credentials, and return a list of [CredentialEntry] to be set
 * on the [BeginGetCredentialResponse]. This list is then shown to the user on a selector.
 *
 * @param beginGetCredentialOptions the list of type specific credential options to to be processed
 * in order to produce a [BeginGetCredentialResponse]
 * @param callingAppInfo info pertaining to the app requesting credentials
 */
class BeginGetCredentialRequest @JvmOverloads constructor(
    val beginGetCredentialOptions: List<BeginGetCredentialOption>,
    val callingAppInfo: CallingAppInfo? = null,
) {
    companion object {
        private const val REQUEST_KEY = "androidx.credentials.provider.BeginGetCredentialRequest"

        /**
         * Helper method to convert the class to a parcelable [Bundle], in case the class
         * instance needs to be sent across a process. Consumers of this method should use
         * [readFromBundle] to reconstruct the class instance back from the bundle returned here.
         */
        @JvmStatic
        @OptIn(markerClass = [BuildCompat.PrereleaseSdkCheck::class])
        fun writeToBundle(request: BeginGetCredentialRequest): Bundle {
            val bundle = Bundle()
            if (BuildCompat.isAtLeastU()) {
                bundle.putParcelable(REQUEST_KEY,
                    BeginGetCredentialUtil.convertToFrameworkRequest(request))
            }
            return bundle
        }

        /**
         * Helper method to convert a [Bundle] retrieved through [writeToBundle], back
         * to an instance of [BeginGetCredentialRequest].
         */
        @JvmStatic
        @OptIn(markerClass = [BuildCompat.PrereleaseSdkCheck::class])
        fun readFromBundle(bundle: Bundle): BeginGetCredentialRequest? {
            if (BuildCompat.isAtLeastU()) {
                val frameworkRequest = bundle.getParcelable(REQUEST_KEY,
                    android.service.credentials.BeginGetCredentialRequest::class.java)
                if (frameworkRequest != null) {
                    return BeginGetCredentialUtil.convertToJetpackRequest(frameworkRequest)
                }
                return null
            }
            return null
        }
    }
}