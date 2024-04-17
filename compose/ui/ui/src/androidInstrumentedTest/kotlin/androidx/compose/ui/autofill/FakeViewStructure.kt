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

package androidx.compose.ui.autofill

import android.graphics.Matrix
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.os.Parcel
import android.view.View
import android.view.ViewStructure
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import androidx.annotation.GuardedBy
import androidx.annotation.RequiresApi

/**
 * A fake implementation of [ViewStructure] to use in tests.
 *
 * @param virtualId An ID that is unique for each viewStructure node in the viewStructure tree.
 * @param packageName The package name of the app (Used as an autofill heuristic).
 * @param typeName The type name of the view's identifier, or null if there is none.
 * @param entryName The entry name of the view's identifier, or null if there is none.
 * @param children A list of [ViewStructure]s that are children of the current [ViewStructure].
 * @param bounds The bounds (Dimensions) of the component represented by this [ViewStructure].
 * @param autofillId The [autofillId] for the parent component. The same autofillId is used for
 * other child components.
 * @param autofillType The data type. Can be one of the following:
 * [View.AUTOFILL_TYPE_DATE],
 * [View.AUTOFILL_TYPE_LIST],
 * [View.AUTOFILL_TYPE_TEXT],
 * [View.AUTOFILL_TYPE_TOGGLE] or
 * [View.AUTOFILL_TYPE_NONE].
 * @param autofillHints The autofill hint. If this value not specified, we use heuristics to
 * determine what data to use while performing autofill.
 *
 */
@RequiresApi(Build.VERSION_CODES.O)
internal data class FakeViewStructure(
    var virtualId: Int = 0,
    var packageName: String? = null,
    var typeName: String? = null,
    var entryName: String? = null,
    var children: MutableList<FakeViewStructure> = mutableListOf(),
    var bounds: Rect? = null,
    private val autofillId: AutofillId? = generateAutofillId(),
    internal var autofillType: Int = View.AUTOFILL_TYPE_NONE,
    internal var autofillHints: Array<out String> = arrayOf()
) : ViewStructure() {

    private var activated: Boolean = false
    private var alpha: Float = 1f
    private var autofillOptions: Array<CharSequence>? = null
    private var autofillValue: AutofillValue? = null
    private var className: String? = null
    private var contentDescription: CharSequence? = null
    private var dataIsSensitive: Boolean = false
    private var elevation: Float = 0f
    private var extras: Bundle = Bundle()
    private var hint: CharSequence? = null
    private var htmlInfo: HtmlInfo? = null
    private var inputType: Int = 0
    private var isEnabled: Boolean = true
    private var isAccessibilityFocused: Boolean = false
    private var isCheckable: Boolean = false
    private var isChecked: Boolean = false
    private var isClickable: Boolean = true
    private var isContextClickable: Boolean = false
    private var isFocused: Boolean = false
    private var isFocusable: Boolean = false
    private var isLongClickable: Boolean = false
    private var isOpaque: Boolean = false
    private var selected: Boolean = false
    private var text: CharSequence = ""
    private var textLines: IntArray? = null
    private var transformation: Matrix? = null
    private var visibility: Int = View.VISIBLE
    private var webDomain: String? = null

    internal companion object {
        @GuardedBy("this")
        private var previousId = 0
        private val NO_SESSION = 0

        @Synchronized
        private fun generateAutofillId(): AutofillId {
            var autofillId: AutofillId? = null
            useParcel { parcel ->
                parcel.writeInt(++previousId) // View Id.
                parcel.writeInt(NO_SESSION) // Flag.
                parcel.setDataPosition(0)
                autofillId = AutofillId.CREATOR.createFromParcel(parcel)
            }
            return autofillId ?: error("Could not generate autofill id")
        }
    }

    override fun getChildCount() = children.count()

    override fun addChildCount(childCount: Int): Int {
        repeat(childCount) { children.add(FakeViewStructure(autofillId = autofillId)) }
        return children.count() - childCount
    }

    override fun newChild(index: Int): FakeViewStructure {
        if (index >= children.count()) error("Call addChildCount() before calling newChild()")
        return children[index]
    }

    override fun getAutofillId() = autofillId

    override fun setAutofillId(rootId: AutofillId, virtualId: Int) {
        this.virtualId = virtualId
    }

    override fun setId(
        virtualId: Int,
        packageName: String?,
        typeName: String?,
        entryName: String?
    ) {
        this.virtualId = virtualId
        this.packageName = packageName
        this.typeName = typeName
        this.entryName = entryName
    }

    override fun setAutofillType(autofillType: Int) {
        this.autofillType = autofillType
    }

    override fun setAutofillHints(autofillHints: Array<out String>?) {
        autofillHints?.let { this.autofillHints = it }
    }

    override fun setDimens(left: Int, top: Int, x: Int, y: Int, width: Int, height: Int) {
        this.bounds = Rect(left, top, width - left, height - top)
    }

    override fun equals(other: Any?) = other is FakeViewStructure &&
        other.virtualId == virtualId &&
        other.packageName == packageName &&
        other.typeName == typeName &&
        other.entryName == entryName &&
        other.autofillType == autofillType &&
        other.autofillHints.contentEquals(autofillHints) &&
        other.bounds.contentEquals(bounds) &&
        other.children == children

    override fun hashCode() = super.hashCode()

    override fun getExtras() = extras

    override fun getHint() = hint ?: ""

    override fun getText() = text

    override fun hasExtras() = !extras.isEmpty

    override fun setActivated(p0: Boolean) { activated = p0 }

    override fun setAccessibilityFocused(p0: Boolean) { isAccessibilityFocused = p0 }

    override fun setAlpha(p0: Float) { alpha = p0 }

    override fun setAutofillOptions(p0: Array<CharSequence>?) { autofillOptions = p0 }

    override fun setAutofillValue(p0: AutofillValue?) { autofillValue = p0 }

    override fun setCheckable(p0: Boolean) { isCheckable = p0 }

    override fun setChecked(p0: Boolean) { isChecked = p0 }

    override fun setClassName(p0: String?) { className = p0 }

    override fun setClickable(p0: Boolean) { isClickable = p0 }

    override fun setContentDescription(p0: CharSequence?) { contentDescription = p0 }

    override fun setContextClickable(p0: Boolean) { isContextClickable = p0 }

    override fun setDataIsSensitive(p0: Boolean) { dataIsSensitive = p0 }

    override fun setElevation(p0: Float) { elevation = p0 }

    override fun setEnabled(p0: Boolean) { isEnabled = p0 }

    override fun setFocusable(p0: Boolean) { isFocusable = p0 }

    override fun setFocused(p0: Boolean) { isFocused = p0 }

    override fun setHtmlInfo(p0: HtmlInfo) { htmlInfo = p0 }

    override fun setHint(p0: CharSequence?) { hint = p0 }

    override fun setInputType(p0: Int) { inputType = p0 }

    override fun setLongClickable(p0: Boolean) { isLongClickable = p0 }

    override fun setOpaque(p0: Boolean) { isOpaque = p0 }

    override fun setSelected(p0: Boolean) { selected = p0 }

    override fun setText(p0: CharSequence?) { p0?.let { text = it } }

    override fun setText(p0: CharSequence?, p1: Int, p2: Int) {
        p0?.let { text = it.subSequence(p1, p2) }
    }

    override fun setTextLines(p0: IntArray?, p1: IntArray?) { textLines = p0 }

    override fun setTransformation(p0: Matrix?) { transformation = p0 }

    override fun setVisibility(p0: Int) { visibility = p0 }

    override fun setWebDomain(p0: String?) { webDomain = p0 }

    // Unimplemented methods.
    override fun asyncCommit() { TODO("not implemented") }

    override fun asyncNewChild(p0: Int): ViewStructure { TODO("not implemented") }

    override fun getTextSelectionEnd(): Int { TODO("not implemented") }

    override fun getTextSelectionStart(): Int { TODO("not implemented") }

    override fun newHtmlInfoBuilder(p0: String): HtmlInfo.Builder { TODO("not implemented") }

    override fun setAutofillId(p0: AutofillId) { TODO("not implemented") }

    override fun setChildCount(p0: Int) { TODO("not implemented") }

    override fun setLocaleList(p0: LocaleList?) { TODO("not implemented") }

    override fun setTextStyle(p0: Float, p1: Int, p2: Int, p3: Int) { TODO("not implemented") }
}

private fun Rect?.contentEquals(other: Rect?) = when {
    (other == null && this == null) -> true
    (other == null || this == null) -> false
    else ->
        other.left == left &&
            other.right == right &&
            other.bottom == bottom &&
            other.top == top
}

/** Obtains a parcel and then recycles it correctly whether an exception is thrown or not. */
private fun useParcel(block: (Parcel) -> Unit) {
    var parcel: Parcel? = null
    try {
        parcel = Parcel.obtain()
        block(parcel)
    } finally {
        parcel?.recycle()
    }
}
