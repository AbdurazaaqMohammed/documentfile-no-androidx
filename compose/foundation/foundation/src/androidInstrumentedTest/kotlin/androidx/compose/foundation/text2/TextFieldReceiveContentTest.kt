/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.foundation.text2

import android.content.ClipDescription
import android.net.Uri
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputContentInfo
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.content.MediaType
import androidx.compose.foundation.content.TransferableContent
import androidx.compose.foundation.content.receiveContent
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text2.input.rememberTextFieldState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.requestFocus
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests InputConnection#commitContent calls from BasicTextField2 to receiveContent modifier.
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalFoundationApi::class)
class TextFieldReceiveContentTest {

    @get:Rule
    val rule = createComposeRule()

    private val inputMethodInterceptor = InputMethodInterceptor(rule)

    private val tag = "BasicTextField2"

    @SdkSuppress(minSdkVersion = 25)
    @Test
    fun commitContentReturnsFalse_whenNoReceiveContentConfigured() {
        inputMethodInterceptor.setContent {
            BasicTextField2(state = rememberTextFieldState(), modifier = Modifier.testTag(tag))
        }
        rule.onNodeWithTag(tag).requestFocus()
        inputMethodInterceptor.withInputConnection {
            assertFalse(
                commitContent(
                    createInputContentInfo().unwrap() as InputContentInfo,
                    0,
                    null
                )
            )
        }
    }

    @SdkSuppress(maxSdkVersion = 24)
    @Test
    fun preformPrivateCommandReturnsFalse_whenNoReceiveContentConfigured() {
        inputMethodInterceptor.setContent {
            BasicTextField2(state = rememberTextFieldState(), modifier = Modifier.testTag(tag))
        }
        rule.onNodeWithTag(tag).requestFocus()
        inputMethodInterceptor.onIdle { editorInfo, inputConnection ->
            // Although we are testing `performPrivateCommand` that should return true by default
            // in the existence of no configuration, semantically the caller is still calling
            // commitContent which should return false by default.
            assertFalse(
                InputConnectionCompat.commitContent(
                    inputConnection,
                    editorInfo,
                    InputContentInfoCompat(DEFAULT_CONTENT_URI, DEFAULT_CLIP_DESCRIPTION, null),
                    0,
                    null
                )
            )
        }
    }

    @Test
    fun singleReceiveContent_configuresEditorInfo() {
        inputMethodInterceptor.setContent {
            BasicTextField2(
                state = rememberTextFieldState(),
                modifier = Modifier
                    .testTag(tag)
                    .receiveContent(MediaType.Image) { null }
            )
        }
        rule.onNodeWithTag(tag).requestFocus()
        inputMethodInterceptor.withEditorInfo {
            val contentMimeTypes = EditorInfoCompat.getContentMimeTypes(this)
            assertThat(contentMimeTypes).isEqualTo(arrayOf(MediaType.Image.representation))
        }
    }

    @Test
    fun singleReceiveContent_duplicateMediaTypes_appliedUniquely() {
        inputMethodInterceptor.setContent {
            BasicTextField2(
                state = rememberTextFieldState(),
                modifier = Modifier
                    .testTag(tag)
                    .receiveContent(
                        MediaType.Image,
                        MediaType.PlainText,
                        MediaType.Image,
                        MediaType.HtmlText
                    ) { null }
            )
        }
        rule.onNodeWithTag(tag).requestFocus()
        inputMethodInterceptor.withEditorInfo {
            val contentMimeTypes = EditorInfoCompat.getContentMimeTypes(this)
            assertThat(contentMimeTypes).isEqualTo(
                arrayOf(
                    MediaType.Image.representation,
                    MediaType.PlainText.representation,
                    MediaType.HtmlText.representation
                )
            )
        }
    }

    @Test
    fun multiReceiveContent_mergesMediaTypes() {
        inputMethodInterceptor.setContent {
            Box(modifier = Modifier.receiveContent(
                MediaType.Text
            ) { null }) {
                BasicTextField2(
                    state = rememberTextFieldState(),
                    modifier = Modifier
                        .testTag(tag)
                        .receiveContent(MediaType.Image) { null }
                )
            }
        }
        rule.onNodeWithTag(tag).requestFocus()
        inputMethodInterceptor.withEditorInfo {
            val contentMimeTypes = EditorInfoCompat.getContentMimeTypes(this)
            assertThat(contentMimeTypes).isEqualTo(
                arrayOf(
                    MediaType.Image.representation,
                    MediaType.Text.representation
                )
            )
        }
    }

    @Test
    fun multiReceiveContent_mergesMediaTypes_uniquely() {
        inputMethodInterceptor.setContent {
            Box(modifier = Modifier.receiveContent(
                MediaType.Text, MediaType.Image
            ) { null }) {
                BasicTextField2(
                    state = rememberTextFieldState(),
                    modifier = Modifier
                        .testTag(tag)
                        .receiveContent(MediaType.Image) { null }
                )
            }
        }
        rule.onNodeWithTag(tag).requestFocus()
        inputMethodInterceptor.withEditorInfo {
            val contentMimeTypes = EditorInfoCompat.getContentMimeTypes(this)
            assertThat(contentMimeTypes).isEqualTo(
                arrayOf(
                    MediaType.Image.representation,
                    MediaType.Text.representation
                )
            )
        }
    }

    @Test
    fun multiReceiveContent_mergesMediaTypes_includingAnotherTraversableNode() {
        inputMethodInterceptor.setContent {
            Box(modifier = Modifier
                .receiveContent(
                    MediaType.Text
                ) { null }
                .dragAndDropTarget({ true }, object : DragAndDropTarget {
                    override fun onDrop(event: DragAndDropEvent): Boolean {
                        return false
                    }
                })
            ) {
                BasicTextField2(
                    state = rememberTextFieldState(),
                    modifier = Modifier
                        .testTag(tag)
                        .receiveContent(MediaType.Image) { null }
                )
            }
        }
        rule.onNodeWithTag(tag).requestFocus()
        inputMethodInterceptor.withEditorInfo {
            val contentMimeTypes = EditorInfoCompat.getContentMimeTypes(this)
            assertThat(contentMimeTypes).isEqualTo(
                arrayOf(
                    MediaType.Image.representation,
                    MediaType.Text.representation
                )
            )
        }
    }

    @Test
    fun singleReceiveContent_isCalledAfterCommitContent() {
        var transferableContent: TransferableContent? = null
        inputMethodInterceptor.setContent {
            BasicTextField2(
                state = rememberTextFieldState(),
                modifier = Modifier
                    .testTag(tag)
                    .receiveContent(MediaType.All) {
                        transferableContent = it
                        null
                    }
            )
        }
        rule.onNodeWithTag(tag).requestFocus()

        val linkUri = Uri.parse("https://example.com")
        val bundle = Bundle().apply { putString("key", "value") }
        inputMethodInterceptor.onIdle { editorInfo, inputConnection ->
            InputConnectionCompat.commitContent(
                inputConnection,
                editorInfo,
                createInputContentInfo(linkUri = linkUri),
                0,
                bundle
            )
        }

        rule.runOnIdle {
            assertThat(transferableContent).isNotNull()
            assertThat(transferableContent?.source).isEqualTo(TransferableContent.Source.Keyboard)
            assertThat(transferableContent?.clipMetadata?.clipDescription)
                .isEqualTo(DEFAULT_CLIP_DESCRIPTION)

            assertThat(transferableContent?.clipEntry?.clipData?.itemCount).isEqualTo(1)
            assertThat(transferableContent?.clipEntry?.clipData?.getItemAt(0)?.uri)
                .isEqualTo(DEFAULT_CONTENT_URI)

            assertThat(transferableContent?.platformTransferableContent?.linkUri)
                .isEqualTo(linkUri)
            assertThat(transferableContent?.platformTransferableContent?.extras)
                .isEqualTo(bundle)
        }
    }

    @SdkSuppress(minSdkVersion = 25) // Permissions are acquired only on SDK levels 25 or higher.
    @Test
    fun singleReceiveContent_permissionIsRequested() {
        var transferableContent: TransferableContent? = null
        inputMethodInterceptor.setContent {
            BasicTextField2(
                state = rememberTextFieldState(),
                modifier = Modifier
                    .testTag(tag)
                    .receiveContent(MediaType.All) {
                        transferableContent = it
                        null
                    }
            )
        }
        rule.onNodeWithTag(tag).requestFocus()

        val inputContentInfo: InputContentInfoCompat = createInputContentInfo()

        inputMethodInterceptor.onIdle { editorInfo, inputConnection ->
            InputConnectionCompat.commitContent(
                inputConnection,
                editorInfo,
                inputContentInfo,
                InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION,
                null
            )
        }

        rule.runOnIdle {
            assertThat(transferableContent).isNotNull()
            assertTrue(
                transferableContent?.platformTransferableContent
                    ?.extras
                    ?.containsKey("EXTRA_INPUT_CONTENT_INFO") ?: false
            )
        }
    }

    @Test
    fun multiReceiveContent_delegatesRemainingItems_toParent() {
        var childTransferableContent: TransferableContent? = null
        var parentTransferableContent: TransferableContent? = null
        inputMethodInterceptor.setContent {
            BasicTextField2(
                state = rememberTextFieldState(),
                modifier = Modifier
                    .testTag(tag)
                    .receiveContent(MediaType.All) {
                        parentTransferableContent = it
                        null
                    }
                    .receiveContent(MediaType.All) {
                        childTransferableContent = it
                        it
                    }
            )
        }
        rule.onNodeWithTag(tag).requestFocus()
        inputMethodInterceptor.onIdle { editorInfo, inputConnection ->
            InputConnectionCompat.commitContent(
                inputConnection,
                editorInfo,
                createInputContentInfo(),
                0,
                null
            )
        }

        rule.runOnIdle {
            assertThat(childTransferableContent).isNotNull()
            assertThat(childTransferableContent).isSameInstanceAs(parentTransferableContent)

            assertThat(parentTransferableContent?.source)
                .isEqualTo(TransferableContent.Source.Keyboard)
            assertThat(parentTransferableContent?.clipMetadata?.clipDescription)
                .isEqualTo(DEFAULT_CLIP_DESCRIPTION)

            assertThat(parentTransferableContent?.clipEntry?.clipData?.itemCount).isEqualTo(1)
            assertThat(parentTransferableContent?.clipEntry?.clipData?.getItemAt(0)?.uri)
                .isEqualTo(DEFAULT_CONTENT_URI)
        }
    }

    @Test
    fun multiReceiveContent_doesNotCallParent_ifAllItemsAreProcessed() {
        var childTransferableContent: TransferableContent? = null
        var parentTransferableContent: TransferableContent? = null
        inputMethodInterceptor.setContent {
            BasicTextField2(
                state = rememberTextFieldState(),
                modifier = Modifier
                    .testTag(tag)
                    .receiveContent(MediaType.All) {
                        parentTransferableContent = it
                        null
                    }
                    .receiveContent(MediaType.All) {
                        childTransferableContent = it
                        null
                    }
            )
        }
        rule.onNodeWithTag(tag).requestFocus()
        inputMethodInterceptor.onIdle { editorInfo, inputConnection ->
            InputConnectionCompat.commitContent(
                inputConnection,
                editorInfo,
                createInputContentInfo(),
                0,
                null
            )
        }

        rule.runOnIdle {
            assertThat(childTransferableContent).isNotNull()
            assertThat(parentTransferableContent).isNull()
        }
    }

    companion object {
        private val DEFAULT_CONTENT_URI = Uri.parse("content://com.example.app/content")
        private val DEFAULT_CLIP_DESCRIPTION = ClipDescription("image", arrayOf("image/jpeg"))

        private fun createInputContentInfo(
            contentUri: Uri = DEFAULT_CONTENT_URI,
            clipDescription: ClipDescription = DEFAULT_CLIP_DESCRIPTION,
            linkUri: Uri? = null
        ) = InputContentInfoCompat(contentUri, clipDescription, linkUri)

        private fun InputMethodInterceptor.onIdle(block: (EditorInfo, InputConnection) -> Unit) {
            withInputConnection {
                withEditorInfo {
                    block(this@withEditorInfo, this@withInputConnection)
                }
            }
        }
    }
}
