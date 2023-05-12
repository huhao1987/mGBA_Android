/*
 * Copyright 2017 The Android Open Source Project
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

package com.mobileer.oboetester;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.IOException;

/**
 * Test basic output.
 */
public final class TestOutputActivity extends TestOutputActivityBase {

    public static final int MAX_CHANNEL_BOXES = 16;
    private CheckBox[] mChannelBoxes;
    private Spinner mOutputSignalSpinner;
    protected CommunicationDeviceView mCommunicationDeviceView;

    private class OutputSignalSpinnerListener implements android.widget.AdapterView.OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            mAudioOutTester.setSignalType(pos);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            mAudioOutTester.setSignalType(0);
        }
    }

    @Override
    protected void inflateActivity() {
        setContentView(R.layout.activity_test_output);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        updateEnabledWidgets();

        mAudioOutTester = addAudioOutputTester();

        mChannelBoxes = new CheckBox[MAX_CHANNEL_BOXES];
        int ic = 0;
        mChannelBoxes[ic++] = (CheckBox) findViewById(R.id.channelBox0);
        mChannelBoxes[ic++] = (CheckBox) findViewById(R.id.channelBox1);
        mChannelBoxes[ic++] = (CheckBox) findViewById(R.id.channelBox2);
        mChannelBoxes[ic++] = (CheckBox) findViewById(R.id.channelBox3);
        mChannelBoxes[ic++] = (CheckBox) findViewById(R.id.channelBox4);
        mChannelBoxes[ic++] = (CheckBox) findViewById(R.id.channelBox5);
        mChannelBoxes[ic++] = (CheckBox) findViewById(R.id.channelBox6);
        mChannelBoxes[ic++] = (CheckBox) findViewById(R.id.channelBox7);
        mChannelBoxes[ic++] = (CheckBox) findViewById(R.id.channelBox8);
        mChannelBoxes[ic++] = (CheckBox) findViewById(R.id.channelBox9);
        mChannelBoxes[ic++] = (CheckBox) findViewById(R.id.channelBox10);
        mChannelBoxes[ic++] = (CheckBox) findViewById(R.id.channelBox11);
        mChannelBoxes[ic++] = (CheckBox) findViewById(R.id.channelBox12);
        mChannelBoxes[ic++] = (CheckBox) findViewById(R.id.channelBox13);
        mChannelBoxes[ic++] = (CheckBox) findViewById(R.id.channelBox14);
        mChannelBoxes[ic++] = (CheckBox) findViewById(R.id.channelBox15);
        configureChannelBoxes(0);

        mOutputSignalSpinner = (Spinner) findViewById(R.id.spinnerOutputSignal);
        mOutputSignalSpinner.setOnItemSelectedListener(new OutputSignalSpinnerListener());
        mOutputSignalSpinner.setSelection(StreamConfiguration.NATIVE_API_UNSPECIFIED);

        mCommunicationDeviceView = (CommunicationDeviceView) findViewById(R.id.comm_device_view);
    }

    @Override
    int getActivityType() {
        return ACTIVITY_TEST_OUTPUT;
    }

    @Override
    protected void onStop() {
        if (mCommunicationDeviceView != null) {
            mCommunicationDeviceView.cleanup();
        }
        super.onStop();
    }

    public void openAudio() throws IOException {
        super.openAudio();
    }

    private void configureChannelBoxes(int channelCount) {
        for (int i = 0; i < mChannelBoxes.length; i++) {
            mChannelBoxes[i].setChecked(i < channelCount);
            mChannelBoxes[i].setEnabled(i < channelCount);
        }
    }

    public void stopAudio() {
        configureChannelBoxes(0);
        mOutputSignalSpinner.setEnabled(true);
        super.stopAudio();
    }

    public void pauseAudio() {
        configureChannelBoxes(0);
        mOutputSignalSpinner.setEnabled(true);
        super.pauseAudio();
    }

    public void releaseAudio() {
        configureChannelBoxes(0);
        mOutputSignalSpinner.setEnabled(true);
        super.releaseAudio();
    }

    public void closeAudio() {
        configureChannelBoxes(0);
        mOutputSignalSpinner.setEnabled(true);
        super.closeAudio();
    }

    public void startAudio() throws IOException {
        super.startAudio();
        int channelCount = mAudioOutTester.getCurrentAudioStream().getChannelCount();
        configureChannelBoxes(channelCount);
        mOutputSignalSpinner.setEnabled(false);
    }

    public void onChannelBoxClicked(View view) {
        CheckBox checkBox = (CheckBox) view;
        String text = (String) checkBox.getText();
        int channelIndex = Integer.parseInt(text);
        mAudioOutTester.setChannelEnabled(channelIndex, checkBox.isChecked());
    }

    @Override
    public void startTestUsingBundle() {
        try {
            StreamConfiguration requestedOutConfig = mAudioOutTester.requestedConfiguration;
            IntentBasedTestSupport.configureOutputStreamFromBundle(mBundleFromIntent, requestedOutConfig);

            int signalType = IntentBasedTestSupport.getSignalTypeFromBundle(mBundleFromIntent);
            mAudioOutTester.setSignalType(signalType);

            openAudio();
            startAudio();

            int durationSeconds = IntentBasedTestSupport.getDurationSeconds(mBundleFromIntent);
            if (durationSeconds > 0) {
                // Schedule the end of the test.
                Handler handler = new Handler(Looper.getMainLooper()); // UI thread
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        stopAutomaticTest();
                    }
                }, durationSeconds * 1000);
            }
        } catch (Exception e) {
            showErrorToast(e.getMessage());
        } finally {
            mBundleFromIntent = null;
        }
    }

    void stopAutomaticTest() {
        String report = getCommonTestReport();
        stopAudio();
        maybeWriteTestResult(report);
        mTestRunningByIntent = false;
    }
}
