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

package androidx.wear.protolayout.expression.pipeline;

import static com.google.common.truth.Truth.assertThat;

import static org.robolectric.Shadows.shadowOf;

import android.os.Looper;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.protolayout.expression.pipeline.ColorNodes.AnimatableFixedColorNode;
import androidx.wear.protolayout.expression.pipeline.ColorNodes.DynamicAnimatedColorNode;
import androidx.wear.protolayout.expression.pipeline.ColorNodes.FixedColorNode;
import androidx.wear.protolayout.expression.pipeline.ColorNodes.StateColorSourceNode;
import androidx.wear.protolayout.expression.proto.AnimationParameterProto.AnimationSpec;
import androidx.wear.protolayout.expression.proto.DynamicProto.AnimatableFixedColor;
import androidx.wear.protolayout.expression.proto.DynamicProto.StateColorSource;
import androidx.wear.protolayout.expression.proto.FixedProto.FixedColor;
import androidx.wear.protolayout.expression.proto.StateEntryProto.StateEntryValue;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class ColorNodesTest {

    private static final int FROM_COLOR = 0xFF00FF00;
    private static final int TO_COLOR = 0xFFFF00FF;

    @Test
    public void fixedColorNode() {
        List<Integer> results = new ArrayList<>();

        FixedColor protoNode = FixedColor.newBuilder().setArgb(FROM_COLOR).build();
        FixedColorNode node = new FixedColorNode(protoNode, new AddToListCallback<>(results));

        node.init();

        assertThat(results).containsExactly(FROM_COLOR);
    }

    @Test
    public void stateColorSourceNode_worksWithFixedColor() {
        List<Integer> results = new ArrayList<>();
        ObservableStateStore oss =
                new ObservableStateStore(
                        ImmutableMap.of(
                                "foo",
                                StateEntryValue.newBuilder()
                                        .setColorVal(FixedColor.newBuilder().setArgb(FROM_COLOR))
                                        .build()));

        StateColorSource protoNode = StateColorSource.newBuilder().setSourceKey("foo").build();
        StateColorSourceNode node =
                new StateColorSourceNode(oss, protoNode, new AddToListCallback<>(results));

        node.preInit();
        node.init();

        assertThat(results).containsExactly(FROM_COLOR);
    }

    @Test
    public void stateColorSourceNode_updatesWithStateChanges() {
        List<Integer> results = new ArrayList<>();
        ObservableStateStore oss =
                new ObservableStateStore(
                        ImmutableMap.of(
                                "foo",
                                StateEntryValue.newBuilder()
                                        .setColorVal(FixedColor.newBuilder().setArgb(FROM_COLOR))
                                        .build()));
        StateColorSource protoNode = StateColorSource.newBuilder().setSourceKey("foo").build();
        StateColorSourceNode node =
                new StateColorSourceNode(oss, protoNode, new AddToListCallback<>(results));

        node.preInit();
        node.init();
        oss.setStateEntryValues(
                ImmutableMap.of(
                        "foo",
                        StateEntryValue.newBuilder()
                                .setColorVal(FixedColor.newBuilder().setArgb(TO_COLOR))
                                .build()));

        assertThat(results).containsExactly(FROM_COLOR, TO_COLOR).inOrder();
    }

    @Test
    public void stateColorSourceNode_noUpdatesAfterDestroy() {
        List<Integer> results = new ArrayList<>();
        ObservableStateStore oss =
                new ObservableStateStore(
                        ImmutableMap.of(
                                "foo",
                                StateEntryValue.newBuilder()
                                        .setColorVal(FixedColor.newBuilder().setArgb(FROM_COLOR))
                                        .build()));
        StateColorSource protoNode = StateColorSource.newBuilder().setSourceKey("foo").build();
        StateColorSourceNode node =
                new StateColorSourceNode(oss, protoNode, new AddToListCallback<>(results));

        node.preInit();
        node.init();
        assertThat(results).containsExactly(FROM_COLOR);

        results.clear();
        node.destroy();
        oss.setStateEntryValues(
                ImmutableMap.of(
                        "foo",
                        StateEntryValue.newBuilder()
                                .setColorVal(FixedColor.newBuilder().setArgb(TO_COLOR))
                                .build()));
        assertThat(results).isEmpty();
    }

    @Test
    public void animatableFixedColor_animates() {
        List<Integer> results = new ArrayList<>();
        QuotaManager quotaManager = new UnlimitedQuotaManager();
        AnimatableFixedColor protoNode =
                AnimatableFixedColor.newBuilder()
                        .setFromArgb(FROM_COLOR)
                        .setToArgb(TO_COLOR)
                        .build();
        AnimatableFixedColorNode node =
                new AnimatableFixedColorNode(
                        protoNode, new AddToListCallback<>(results), quotaManager
                );
        node.setVisibility(true);

        node.init();
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(results.size()).isGreaterThan(2);
        assertThat(results.get(0)).isEqualTo(FROM_COLOR);
        assertThat(Iterables.getLast(results)).isEqualTo(TO_COLOR);
    }

    @Test
    public void animatableFixedColor_whenInvisible_skipsToEnd() {
        List<Integer> results = new ArrayList<>();
        QuotaManager quotaManager = new UnlimitedQuotaManager();
        AnimatableFixedColor protoNode =
                AnimatableFixedColor
                        .newBuilder()
                        .setFromArgb(FROM_COLOR)
                        .setToArgb(TO_COLOR)
                        .build();
        AnimatableFixedColorNode node =
                new AnimatableFixedColorNode(
                        protoNode, new AddToListCallback<>(results), quotaManager
                );
        node.setVisibility(false);

        node.init();
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(results).hasSize(1);
        assertThat(results).containsExactly(TO_COLOR);
    }

    @Test
    public void dynamicAnimatedColor_animatesWithStateChange() {
        List<Integer> results = new ArrayList<>();
        QuotaManager quotaManager = new UnlimitedQuotaManager();
        ObservableStateStore oss =
                new ObservableStateStore(
                        ImmutableMap.of(
                                "foo",
                                StateEntryValue.newBuilder()
                                        .setColorVal(
                                                FixedColor.newBuilder().setArgb(FROM_COLOR).build()
                                        )
                                        .build()));
        DynamicAnimatedColorNode colorNode =
                new DynamicAnimatedColorNode(
                        new AddToListCallback<>(results),
                        AnimationSpec.getDefaultInstance(),
                        quotaManager
                );
        colorNode.setVisibility(true);
        StateColorSourceNode stateNode =
                new StateColorSourceNode(
                        oss,
                        StateColorSource.newBuilder().setSourceKey("foo").build(),
                        colorNode.getInputCallback());

        stateNode.preInit();
        stateNode.init();

        oss.setStateEntryValues(
                ImmutableMap.of(
                        "foo",
                        StateEntryValue.newBuilder()
                                .setColorVal(FixedColor.newBuilder().setArgb(TO_COLOR))
                                .build()));
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(results.size()).isGreaterThan(2);
        assertThat(results.get(0)).isEqualTo(FROM_COLOR);
        assertThat(Iterables.getLast(results)).isEqualTo(TO_COLOR);
    }

    @Test
    public void dynamicAnimatedColor_onlyAnimateWhenVisible() {
        int color1 = FROM_COLOR;
        int color2 = TO_COLOR;
        int color3 = 0xFFFFFFFF;
        List<Integer> results = new ArrayList<>();
        QuotaManager quotaManager = new UnlimitedQuotaManager();
        ObservableStateStore oss =
                new ObservableStateStore(
                        ImmutableMap.of(
                                "foo",
                                StateEntryValue.newBuilder()
                                        .setColorVal(
                                                FixedColor.newBuilder().setArgb(color1).build()
                                        )
                                        .build()));
        DynamicAnimatedColorNode colorNode =
                new DynamicAnimatedColorNode(
                        new AddToListCallback<>(results),
                        AnimationSpec.getDefaultInstance(),
                        quotaManager
                );
        colorNode.setVisibility(false);
        StateColorSourceNode stateNode =
                new StateColorSourceNode(
                        oss,
                        StateColorSource.newBuilder().setSourceKey("foo").build(),
                        colorNode.getInputCallback());

        stateNode.preInit();
        stateNode.init();

        results.clear();
        oss.setStateEntryValues(
                ImmutableMap.of(
                        "foo",
                        StateEntryValue.newBuilder()
                                .setColorVal(FixedColor.newBuilder().setArgb(color2))
                                .build()));
        shadowOf(Looper.getMainLooper()).idle();

        // Only contains last value.
        assertThat(results).hasSize(1);
        assertThat(results).containsExactly(color2);

        colorNode.setVisibility(true);
        results.clear();
        oss.setStateEntryValues(
                ImmutableMap.of(
                        "foo",
                        StateEntryValue.newBuilder()
                                .setColorVal(FixedColor.newBuilder().setArgb(color3))
                                .build()));
        shadowOf(Looper.getMainLooper()).idle();

        // Contains intermediate values besides the initial and last.
        assertThat(results.size()).isGreaterThan(2);
        assertThat(results.get(0)).isEqualTo(color2);
        assertThat(Iterables.getLast(results)).isEqualTo(color3);
        assertThat(results).isInOrder();
    }
}
