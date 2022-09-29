/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.webkit.internal;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.webkit.ProxyConfig;
import androidx.webkit.ProxyController;
import androidx.webkit.SafeBrowsingResponseCompat;
import androidx.webkit.ServiceWorkerClientCompat;
import androidx.webkit.TracingConfig;
import androidx.webkit.TracingController;
import androidx.webkit.WebMessageCompat;
import androidx.webkit.WebMessagePortCompat;
import androidx.webkit.WebResourceErrorCompat;
import androidx.webkit.WebResourceRequestCompat;
import androidx.webkit.WebViewClientCompat;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import org.chromium.support_lib_boundary.util.Features;

import java.io.OutputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Enum representing a WebView feature, this provides functionality for determining whether a
 * feature is supported by the current framework and/or WebView APK.
 */
public class WebViewFeatureInternal {

    /**
     * WebView APK feature that is used in cases where the standard feature detection
     * mechanism of querying for the flag in the WebView APK is not used.
     *
     * The non-standard feature detection mechanism can be provided by overriding the
     * {@link ApiFeature#isSupportedByWebView()} method of the framework dependant subclass. If
     * it is not overridden, {@link ApiFeature#isSupportedByWebView()} would return {@code false}.
     *
     * The value should not coincide with any other actual feature names in the WebView APK. The
     * extra '_'s prefixing the value is added to reduce the chance of collision.
     *
     * One of the main reason for using a non-standard feature detection is to check whether a
     * feature is present in the WebView APK without loading WebView into the calling process.
     */
    private static final String NONSTANDARD_FEATURE_DETECTION = "__NONSTANDARD_FEATURE_DETECTION";

    /**
     * This feature covers
     * {@link androidx.webkit.WebViewCompat#postVisualStateCallback(android.webkit.WebView, long,
     * androidx.webkit.WebViewCompat.VisualStateCallback)}, and
     * {@link WebViewClientCompat#onPageCommitVisible(android.webkit.WebView, String)}.
     */
    public static final ApiFeature.M VISUAL_STATE_CALLBACK = new ApiFeature.M(
            WebViewFeature.VISUAL_STATE_CALLBACK, Features.VISUAL_STATE_CALLBACK);

    /**
     * This feature covers
     * {@link androidx.webkit.WebSettingsCompat#getOffscreenPreRaster(WebSettings)}, and
     * {@link androidx.webkit.WebSettingsCompat#setOffscreenPreRaster(WebSettings, boolean)}.
     */
    public static final ApiFeature.M OFF_SCREEN_PRERASTER = new ApiFeature.M(
            WebViewFeature.OFF_SCREEN_PRERASTER, Features.OFF_SCREEN_PRERASTER);

    /**
     * This feature covers
     * {@link androidx.webkit.WebSettingsCompat#getSafeBrowsingEnabled(WebSettings)}, and
     * {@link androidx.webkit.WebSettingsCompat#setSafeBrowsingEnabled(WebSettings, boolean)}.
     */
    public static final ApiFeature.O SAFE_BROWSING_ENABLE = new ApiFeature.O(
            WebViewFeature.SAFE_BROWSING_ENABLE, Features.SAFE_BROWSING_ENABLE);

    /**
     * This feature covers
     * {@link androidx.webkit.WebSettingsCompat#getDisabledActionModeMenuItems(WebSettings)}, and
     * {@link androidx.webkit.WebSettingsCompat#setDisabledActionModeMenuItems(WebSettings, int)}.
     */
    public static final ApiFeature.N DISABLED_ACTION_MODE_MENU_ITEMS = new ApiFeature.N(
            WebViewFeature.DISABLED_ACTION_MODE_MENU_ITEMS,
            Features.DISABLED_ACTION_MODE_MENU_ITEMS);

    /**
     * This feature covers
     * {@link androidx.webkit.WebViewCompat#startSafeBrowsing(Context, ValueCallback)}.
     */
    public static final ApiFeature.O_MR1 START_SAFE_BROWSING = new ApiFeature.O_MR1(
            WebViewFeature.START_SAFE_BROWSING,
            Features.START_SAFE_BROWSING);

    /**
     * This feature covers {@link androidx.webkit.WebViewCompat#setSafeBrowsingWhitelist(Set,
     * ValueCallback)}, plumbing through the deprecated boundary interface.
     *
     * <p>Don't use this value directly. This exists only so {@link WebViewFeature#isSupported}
     * supports the <b>deprecated</b> public feature when running against <b>old</b> WebView
     * versions.
     *
     * @deprecated use {@link #SAFE_BROWSING_ALLOWLIST_PREFERRED_TO_DEPRECATED} to test for the
     * <b>old</b> boundary interface
     */
    @Deprecated
    public static final ApiFeature.O_MR1 SAFE_BROWSING_ALLOWLIST_DEPRECATED_TO_DEPRECATED =
            new ApiFeature.O_MR1(WebViewFeature.SAFE_BROWSING_WHITELIST,
                    Features.SAFE_BROWSING_WHITELIST);

    /**
     * This feature covers {@link androidx.webkit.WebViewCompat#setSafeBrowsingWhitelist(Set,
     * ValueCallback)}, plumbing through the new boundary interface.
     *
     * <p>Don't use this value directly. This exists only so {@link WebViewFeature#isSupported}
     * supports the <b>deprecated</b> public feature when running against <b>new</b> WebView
     * versions.
     *
     * @deprecated use {@link #SAFE_BROWSING_ALLOWLIST_PREFERRED_TO_PREFERRED} to test for the
     * <b>new</b> boundary interface.
     */
    @Deprecated
    public static final ApiFeature.O_MR1 SAFE_BROWSING_ALLOWLIST_DEPRECATED_TO_PREFERRED =
            new ApiFeature.O_MR1(WebViewFeature.SAFE_BROWSING_WHITELIST,
                    Features.SAFE_BROWSING_ALLOWLIST);

    /**
     * This feature covers {@link androidx.webkit.WebViewCompat#setSafeBrowsingAllowlist(Set,
     * ValueCallback)}, plumbing through the deprecated boundary interface.
     */
    public static final ApiFeature.O_MR1 SAFE_BROWSING_ALLOWLIST_PREFERRED_TO_DEPRECATED =
            new ApiFeature.O_MR1(WebViewFeature.SAFE_BROWSING_ALLOWLIST,
                    Features.SAFE_BROWSING_WHITELIST);

    /**
     * This feature covers {@link androidx.webkit.WebViewCompat#setSafeBrowsingAllowlist(Set,
     * ValueCallback)}, plumbing through the new boundary interface.
     */
    public static final ApiFeature.O_MR1 SAFE_BROWSING_ALLOWLIST_PREFERRED_TO_PREFERRED =
            new ApiFeature.O_MR1(WebViewFeature.SAFE_BROWSING_ALLOWLIST,
                    Features.SAFE_BROWSING_ALLOWLIST);

    /**
     * This feature covers
     * {@link WebViewCompat#getSafeBrowsingPrivacyPolicyUrl()}.
     */
    public static final ApiFeature.O_MR1 SAFE_BROWSING_PRIVACY_POLICY_URL =
            new ApiFeature.O_MR1(WebViewFeature.SAFE_BROWSING_PRIVACY_POLICY_URL,
                    Features.SAFE_BROWSING_PRIVACY_POLICY_URL);

    /**
     * This feature covers
     * {@link androidx.webkit.ServiceWorkerControllerCompat#getInstance()}.
     */
    public static final ApiFeature.N SERVICE_WORKER_BASIC_USAGE =
            new ApiFeature.N(WebViewFeature.SERVICE_WORKER_BASIC_USAGE,
                    Features.SERVICE_WORKER_BASIC_USAGE);

    /**
     * This feature covers
     * {@link androidx.webkit.ServiceWorkerWebSettingsCompat#getCacheMode()}, and
     * {@link androidx.webkit.ServiceWorkerWebSettingsCompat#setCacheMode(int)}.
     */
    public static final ApiFeature.N SERVICE_WORKER_CACHE_MODE =
            new ApiFeature.N(WebViewFeature.SERVICE_WORKER_CACHE_MODE,
                    Features.SERVICE_WORKER_CACHE_MODE);

    /**
     * This feature covers
     * {@link androidx.webkit.ServiceWorkerWebSettingsCompat#getAllowContentAccess()}, and
     * {@link androidx.webkit.ServiceWorkerWebSettingsCompat#setAllowContentAccess(boolean)}.
     */
    public static final ApiFeature.N SERVICE_WORKER_CONTENT_ACCESS =
            new ApiFeature.N(WebViewFeature.SERVICE_WORKER_CONTENT_ACCESS,
                    Features.SERVICE_WORKER_CONTENT_ACCESS);

    /**
     * This feature covers
     * {@link androidx.webkit.ServiceWorkerWebSettingsCompat#getAllowFileAccess()}, and
     * {@link androidx.webkit.ServiceWorkerWebSettingsCompat#setAllowFileAccess(boolean)}.
     */
    public static final ApiFeature.N SERVICE_WORKER_FILE_ACCESS =
            new ApiFeature.N(WebViewFeature.SERVICE_WORKER_FILE_ACCESS,
                    Features.SERVICE_WORKER_FILE_ACCESS);

    /**
     * This feature covers
     * {@link androidx.webkit.ServiceWorkerWebSettingsCompat#getBlockNetworkLoads()}, and
     * {@link androidx.webkit.ServiceWorkerWebSettingsCompat#setBlockNetworkLoads(boolean)}.
     */
    public static final ApiFeature.N SERVICE_WORKER_BLOCK_NETWORK_LOADS =
            new ApiFeature.N(WebViewFeature.SERVICE_WORKER_BLOCK_NETWORK_LOADS,
                    Features.SERVICE_WORKER_BLOCK_NETWORK_LOADS);

    /**
     * This feature covers
     * {@link ServiceWorkerClientCompat#shouldInterceptRequest(WebResourceRequest)}.
     */
    public static final ApiFeature.N SERVICE_WORKER_SHOULD_INTERCEPT_REQUEST =
            new ApiFeature.N(WebViewFeature.SERVICE_WORKER_SHOULD_INTERCEPT_REQUEST,
                    Features.SERVICE_WORKER_SHOULD_INTERCEPT_REQUEST);

    /**
     * This feature covers
     * {@link WebViewClientCompat#onReceivedError(android.webkit.WebView, WebResourceRequest,
     * WebResourceErrorCompat)}.
     */
    public static final ApiFeature.M RECEIVE_WEB_RESOURCE_ERROR =
            new ApiFeature.M(WebViewFeature.RECEIVE_WEB_RESOURCE_ERROR,
                    Features.RECEIVE_WEB_RESOURCE_ERROR);

    /**
     * This feature covers
     * {@link WebViewClientCompat#onReceivedHttpError(android.webkit.WebView, WebResourceRequest,
     * WebResourceResponse)}.
     */
    public static final ApiFeature.M RECEIVE_HTTP_ERROR = new ApiFeature.M(
            WebViewFeature.RECEIVE_HTTP_ERROR, Features.RECEIVE_HTTP_ERROR);

    /**
     * This feature covers
     * {@link WebViewClientCompat#shouldOverrideUrlLoading(android.webkit.WebView,
     * WebResourceRequest)}.
     */
    public static final ApiFeature.N SHOULD_OVERRIDE_WITH_REDIRECTS =
            new ApiFeature.N(WebViewFeature.SHOULD_OVERRIDE_WITH_REDIRECTS,
                    Features.SHOULD_OVERRIDE_WITH_REDIRECTS);

    /**
     * This feature covers
     * {@link WebViewClientCompat#onSafeBrowsingHit(android.webkit.WebView,
     * WebResourceRequest, int, SafeBrowsingResponseCompat)}.
     */
    public static final ApiFeature.O_MR1 SAFE_BROWSING_HIT = new ApiFeature.O_MR1(
            WebViewFeature.SAFE_BROWSING_HIT, Features.SAFE_BROWSING_HIT);

    /**
     * This feature covers
     * {@link WebResourceRequestCompat#isRedirect(WebResourceRequest)}.
     */
    public static final ApiFeature.N WEB_RESOURCE_REQUEST_IS_REDIRECT =
            new ApiFeature.N(WebViewFeature.WEB_RESOURCE_REQUEST_IS_REDIRECT,
                    Features.WEB_RESOURCE_REQUEST_IS_REDIRECT);

    /**
     * This feature covers
     * {@link WebResourceErrorCompat#getDescription()}.
     */
    public static final ApiFeature.M WEB_RESOURCE_ERROR_GET_DESCRIPTION =
            new ApiFeature.M(WebViewFeature.WEB_RESOURCE_ERROR_GET_DESCRIPTION,
                    Features.WEB_RESOURCE_ERROR_GET_DESCRIPTION);

    /**
     * This feature covers
     * {@link WebResourceErrorCompat#getErrorCode()}.
     */
    public static final ApiFeature.M WEB_RESOURCE_ERROR_GET_CODE =
            new ApiFeature.M(WebViewFeature.WEB_RESOURCE_ERROR_GET_CODE,
                    Features.WEB_RESOURCE_ERROR_GET_CODE);

    /**
     * This feature covers
     * {@link SafeBrowsingResponseCompat#backToSafety(boolean)}.
     */
    public static final ApiFeature.O_MR1 SAFE_BROWSING_RESPONSE_BACK_TO_SAFETY =
            new ApiFeature.O_MR1(WebViewFeature.SAFE_BROWSING_RESPONSE_BACK_TO_SAFETY,
                    Features.SAFE_BROWSING_RESPONSE_BACK_TO_SAFETY);

    /**
     * This feature covers
     * {@link SafeBrowsingResponseCompat#proceed(boolean)}.
     */
    public static final ApiFeature.O_MR1 SAFE_BROWSING_RESPONSE_PROCEED =
            new ApiFeature.O_MR1(WebViewFeature.SAFE_BROWSING_RESPONSE_PROCEED,
                    Features.SAFE_BROWSING_RESPONSE_PROCEED);

    /**
     * This feature covers
     * {@link SafeBrowsingResponseCompat#showInterstitial(boolean)}.
     */
    public static final ApiFeature.O_MR1 SAFE_BROWSING_RESPONSE_SHOW_INTERSTITIAL =
            new ApiFeature.O_MR1(WebViewFeature.SAFE_BROWSING_RESPONSE_SHOW_INTERSTITIAL,
                    Features.SAFE_BROWSING_RESPONSE_SHOW_INTERSTITIAL);

    /**
     * This feature covers
     * {@link WebMessagePortCompat#postMessage(WebMessageCompat)}.
     */
    public static final ApiFeature.M WEB_MESSAGE_PORT_POST_MESSAGE =
            new ApiFeature.M(WebViewFeature.WEB_MESSAGE_PORT_POST_MESSAGE,
                    Features.WEB_MESSAGE_PORT_POST_MESSAGE);

    /**
     * * This feature covers
     * {@link androidx.webkit.WebMessagePortCompat#close()}.
     */
    public static final ApiFeature.M WEB_MESSAGE_PORT_CLOSE =
            new ApiFeature.M(WebViewFeature.WEB_MESSAGE_PORT_CLOSE,
                    Features.WEB_MESSAGE_PORT_CLOSE);

    /**
     * This feature covers
     * {@link WebMessagePortCompat#postMessage(WebMessageCompat)} with ArrayBuffer type, and
     * {@link WebViewCompat#postWebMessage(WebView, WebMessageCompat, Uri)} with ArrayBuffer type.
     */
    public static final ApiFeature.NoFramework WEB_MESSAGE_GET_MESSAGE_PAYLOAD =
            new ApiFeature.NoFramework(WebViewFeature.WEB_MESSAGE_GET_MESSAGE_PAYLOAD,
                    Features.WEB_MESSAGE_GET_MESSAGE_PAYLOAD);

    /**
     * This feature covers
     * {@link WebMessagePortCompat#setWebMessageCallback(
     *WebMessagePortCompat.WebMessageCallbackCompat)}, and
     * {@link WebMessagePortCompat#setWebMessageCallback(Handler,
     * WebMessagePortCompat.WebMessageCallbackCompat)}.
     */
    public static final ApiFeature.M WEB_MESSAGE_PORT_SET_MESSAGE_CALLBACK =
            new ApiFeature.M(WebViewFeature.WEB_MESSAGE_PORT_SET_MESSAGE_CALLBACK,
                    Features.WEB_MESSAGE_PORT_SET_MESSAGE_CALLBACK);

    /**
     * This feature covers
     * {@link WebViewCompat#createWebMessageChannel(WebView)}.
     */
    public static final ApiFeature.M CREATE_WEB_MESSAGE_CHANNEL =
            new ApiFeature.M(WebViewFeature.CREATE_WEB_MESSAGE_CHANNEL,
                    Features.CREATE_WEB_MESSAGE_CHANNEL);

    /**
     * This feature covers
     * {@link WebViewCompat#postWebMessage(WebView, WebMessageCompat, Uri)}.
     */
    public static final ApiFeature.M POST_WEB_MESSAGE = new ApiFeature.M(
            WebViewFeature.POST_WEB_MESSAGE, Features.POST_WEB_MESSAGE);

    /**
     * This feature covers
     * {@link WebViewCompat#postWebMessage(WebView, WebMessageCompat, Uri)}.
     */
    public static final ApiFeature.M WEB_MESSAGE_CALLBACK_ON_MESSAGE =
            new ApiFeature.M(WebViewFeature.WEB_MESSAGE_CALLBACK_ON_MESSAGE,
                    Features.WEB_MESSAGE_CALLBACK_ON_MESSAGE);

    /**
     * This feature covers {@link WebViewCompat#getWebViewClient(WebView)}.
     */
    public static final ApiFeature.O GET_WEB_VIEW_CLIENT = new ApiFeature.O(
            WebViewFeature.GET_WEB_VIEW_CLIENT, Features.GET_WEB_VIEW_CLIENT);

    /**
     * This feature covers {@link WebViewCompat#getWebChromeClient(WebView)}.
     */
    public static final ApiFeature.O GET_WEB_CHROME_CLIENT =
            new ApiFeature.O(WebViewFeature.GET_WEB_CHROME_CLIENT, Features.GET_WEB_CHROME_CLIENT);

    public static final ApiFeature.Q GET_WEB_VIEW_RENDERER =
            new ApiFeature.Q(WebViewFeature.GET_WEB_VIEW_RENDERER, Features.GET_WEB_VIEW_RENDERER);
    public static final ApiFeature.Q WEB_VIEW_RENDERER_TERMINATE =
            new ApiFeature.Q(WebViewFeature.WEB_VIEW_RENDERER_TERMINATE,
                    Features.WEB_VIEW_RENDERER_TERMINATE);

    /**
     * This feature covers
     * {@link TracingController#getInstance()},
     * {@link TracingController#isTracing()},
     * {@link TracingController#start(TracingConfig)},
     * {@link TracingController#stop(OutputStream, Executor)}.
     */
    public static final ApiFeature.P TRACING_CONTROLLER_BASIC_USAGE =
            new ApiFeature.P(WebViewFeature.TRACING_CONTROLLER_BASIC_USAGE,
                    Features.TRACING_CONTROLLER_BASIC_USAGE);

    /**
     * This feature covers
     * {@link androidx.webkit.ProcessGlobalConfig#setDataDirectorySuffix(String)}.
     */
    public static final ApiFeature.P SET_DATA_DIRECTORY_SUFFIX =
            new ApiFeature.P(WebViewFeature.SET_DATA_DIRECTORY_SUFFIX,
                    NONSTANDARD_FEATURE_DETECTION) {
                @Override
                public boolean isSupportedByWebView() {
                    // TODO(crbug.com/1355297): Change it to version check once the support is
                    //  added to WebView.
                    return false;
                }
            };

    /**
     * This feature covers
     * {@link WebViewCompat#getWebViewRenderProcessClient()},
     * {@link WebViewCompat#setWebViewRenderProcessClient(WebViewRenderProcessClient)},
     * {@link WebViewRenderProcessClient#onRenderProcessUnresponsive(WebView, WebViewRenderProcess)},
     * {@link WebViewRenderProcessClient#onRenderProcessResponsive(WebView, WebViewRenderProcess)}
     */
    public static final ApiFeature.Q WEB_VIEW_RENDERER_CLIENT_BASIC_USAGE =
            new ApiFeature.Q(WebViewFeature.WEB_VIEW_RENDERER_CLIENT_BASIC_USAGE,
                    Features.WEB_VIEW_RENDERER_CLIENT_BASIC_USAGE);

    /**
     * This feature covers
     * {@link ProxyController#setProxyOverride(ProxyConfig, Executor, Runnable)},
     * {@link ProxyController#setProxyOverride(ProxyConfig, Runnable)},
     * {@link ProxyController#clearProxyOverride(Executor, Runnable)}, and
     * {@link ProxyController#clearProxyOverride(Runnable)}.
     */
    public static final ApiFeature.NoFramework PROXY_OVERRIDE = new ApiFeature.NoFramework(
            WebViewFeature.PROXY_OVERRIDE, Features.PROXY_OVERRIDE);

    /**
     * This feature covers
     * {@link androidx.webkit.WebSettingsCompat#willSuppressErrorPage(WebSettings)} and
     * {@link androidx.webkit.WebSettingsCompat#setWillSuppressErrorPage(WebSettings, boolean)}.
     */
    public static final ApiFeature.NoFramework SUPPRESS_ERROR_PAGE =
            new ApiFeature.NoFramework(WebViewFeature.SUPPRESS_ERROR_PAGE,
                    Features.SUPPRESS_ERROR_PAGE);

    /**
     * This feature covers {@link WebViewCompat#isMultiProcessEnabled()}.
     */
    public static final ApiFeature.NoFramework MULTI_PROCESS = new ApiFeature.NoFramework(
            WebViewFeature.MULTI_PROCESS, Features.MULTI_PROCESS_QUERY);

    /**
     * This feature covers
     * {@link androidx.webkit.WebSettingsCompat#setForceDark(WebSettings, int)} and
     * {@link androidx.webkit.WebSettingsCompat#getForceDark(WebSettings)}.
     */
    public static final ApiFeature.Q FORCE_DARK = new ApiFeature.Q(
            WebViewFeature.FORCE_DARK, Features.FORCE_DARK);

    /**
     * This feature covers
     * {@link androidx.webkit.WebSettingsCompat#setForceDarkStrategy(WebSettings, int)} and
     * {@link androidx.webkit.WebSettingsCompat#getForceDarkStrategy(WebSettings)}.
     */
    public static final ApiFeature.NoFramework FORCE_DARK_STRATEGY =
            new ApiFeature.NoFramework(WebViewFeature.FORCE_DARK_STRATEGY,
                    Features.FORCE_DARK_BEHAVIOR);

    /**
     * This feature covers
     * {@link androidx.webkit.WebSettingsCompat#setAlgorithmicDarkeningAllowed(WebSettings, boolean)} and
     * {@link androidx.webkit.WebSettingsCompat#isAlgorithmicDarkeningAllowed(WebSettings)}.
     */
    public static final ApiFeature.NoFramework ALGORITHMIC_DARKENING =
            new ApiFeature.NoFramework(WebViewFeature.ALGORITHMIC_DARKENING,
                    Features.ALGORITHMIC_DARKENING);

    /**
     * This feature covers
     * {@link androidx.webkit.WebViewCompat#setWebMessageListener(android.webkit.WebView,
     * androidx.webkit.WebViewCompat.WebMessageListener, String, String[])} and
     * {@link androidx.webkit.WebViewCompat#removeWebMessageListener()}
     */
    public static final ApiFeature.NoFramework WEB_MESSAGE_LISTENER =
            new ApiFeature.NoFramework(WebViewFeature.WEB_MESSAGE_LISTENER,
                    Features.WEB_MESSAGE_LISTENER);

    /**
     * This feature covers
     * {@link
     * androidx.webkit.WebViewCompat#addDocumentStartJavaScript(android.webkit.WebView, String,
     * Set)}
     */
    public static final ApiFeature.NoFramework DOCUMENT_START_SCRIPT =
            new ApiFeature.NoFramework(WebViewFeature.DOCUMENT_START_SCRIPT,
                    Features.DOCUMENT_START_SCRIPT);

    /**
     * This feature covers {@link androidx.webkit.ProxyConfig.Builder.setReverseBypass(boolean)}
     */
    public static final ApiFeature.NoFramework PROXY_OVERRIDE_REVERSE_BYPASS =
            new ApiFeature.NoFramework(WebViewFeature.PROXY_OVERRIDE_REVERSE_BYPASS,
                    Features.PROXY_OVERRIDE_REVERSE_BYPASS);

    /**
     * This feature covers
     * {@link androidx.webkit.WebViewCompat#getVariationsHeader()}
     */
    public static final ApiFeature.NoFramework GET_VARIATIONS_HEADER =
            new ApiFeature.NoFramework(WebViewFeature.GET_VARIATIONS_HEADER,
                    Features.GET_VARIATIONS_HEADER);

    /**
     * This feature covers
     * {@link androidx.webkit.WebSettingsCompat#setEnterpriseAuthenticationAppLinkPolicyEnabled(WebSettings, boolean)} and
     * {@link androidx.webkit.WebSettingsCompat#getEnterpriseAuthenticationAppLinkPolicyEnabled(WebSettings)}.
     */
    public static final ApiFeature.NoFramework ENTERPRISE_AUTHENTICATION_APP_LINK_POLICY =
            new ApiFeature.NoFramework(WebViewFeature.ENTERPRISE_AUTHENTICATION_APP_LINK_POLICY,
                    Features.ENTERPRISE_AUTHENTICATION_APP_LINK_POLICY);

    /**
     * This feature covers
     * {@link androidx.webkit.CookieManager#getCookieInfo(CookieManager, String)}.
     */
    public static final ApiFeature.NoFramework GET_COOKIE_INFO =
            new ApiFeature.NoFramework(WebViewFeature.GET_COOKIE_INFO, Features.GET_COOKIE_INFO);
    // --- Add new feature constants above this line ---

    private WebViewFeatureInternal() {
        // Class should not be instantiated
    }

    /**
     * Return whether a public feature is supported by any internal features defined in this class.
     */
    public static boolean isSupported(
            @NonNull @WebViewFeature.WebViewSupportFeature String publicFeatureValue) {
        return isSupported(publicFeatureValue, ApiFeature.values());
    }

    /**
     * Return whether a public feature is supported by any {@link ConditionallySupportedFeature}s
     * defined in {@code internalFeatures}.
     *
     * @throws RuntimeException if {@code publicFeatureValue} is not matched in
     *      {@code internalFeatures}
     */
    @VisibleForTesting
    public static <T extends ConditionallySupportedFeature> boolean isSupported(
            @NonNull @WebViewFeature.WebViewSupportFeature String publicFeatureValue,
            @NonNull Collection<T> internalFeatures) {
        Set<ConditionallySupportedFeature> matchingFeatures = new HashSet<>();
        for (ConditionallySupportedFeature feature : internalFeatures) {
            if (feature.getPublicFeatureName().equals(publicFeatureValue)) {
                matchingFeatures.add(feature);
            }
        }
        if (matchingFeatures.isEmpty()) {
            throw new RuntimeException("Unknown feature " + publicFeatureValue);
        }
        for (ConditionallySupportedFeature feature : matchingFeatures) {
            if (feature.isSupported()) return true;
        }
        return false;
    }

    /**
     * Utility method for throwing an exception explaining that the feature the app trying to use
     * isn't supported.
     */
    @NonNull
    public static UnsupportedOperationException getUnsupportedOperationException() {
        return new UnsupportedOperationException("This method is not supported by the current "
                + "version of the framework and the current WebView APK");
    }
}
