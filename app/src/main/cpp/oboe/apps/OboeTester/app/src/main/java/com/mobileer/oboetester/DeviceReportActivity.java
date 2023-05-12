/*
 * Copyright 2019 The Android Open Source Project
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

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MicrophoneInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.mobileer.audio_device.AudioDeviceInfoConverter;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * Guide the user through a series of tests plugging in and unplugging a headset.
 * Print a summary at the end of any failures.
 */
public class DeviceReportActivity extends Activity {

    class MyAudioDeviceCallback extends AudioDeviceCallback {
        private HashMap<Integer, AudioDeviceInfo> mDevices
                = new HashMap<Integer, AudioDeviceInfo>();

        @Override
        public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
            for (AudioDeviceInfo info : addedDevices) {
                mDevices.put(info.getId(), info);
            }
            reportDeviceInfo(mDevices.values());
        }

        public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
            for (AudioDeviceInfo info : removedDevices) {
                mDevices.remove(info.getId());
            }
            reportDeviceInfo(mDevices.values());
        }
    }

    MyAudioDeviceCallback mDeviceCallback = new MyAudioDeviceCallback();
    private TextView      mAutoTextView;
    private AudioManager  mAudioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_report);
        mAutoTextView = (TextView) findViewById(R.id.text_log_device_report);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuItem settings = menu.findItem(R.id.action_share);
        settings.setOnMenuItemClickListener(item -> {
            if(mAutoTextView !=null) {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, mAutoTextView.getText().toString());
                sendIntent.setType("text/plain");
                Intent shareIntent = Intent.createChooser(sendIntent, null);
                startActivity(shareIntent);
            }
            return false;
        });
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        addAudioDeviceCallback();
    }

    @Override
    protected void onStop() {
        removeAudioDeviceCallback();
        super.onStop();
    }

    @TargetApi(23)
    private void addAudioDeviceCallback(){
        // Note that we will immediately receive a call to onDevicesAdded with the list of
        // devices which are currently connected.
        mAudioManager.registerAudioDeviceCallback(mDeviceCallback, null);
    }

    @TargetApi(23)
    private void removeAudioDeviceCallback(){
        mAudioManager.unregisterAudioDeviceCallback(mDeviceCallback);
    }

    private void reportDeviceInfo(Collection<AudioDeviceInfo> devices) {
        logClear();
        StringBuffer report = new StringBuffer();
        report.append("Device Report:\n");
        report.append("App: ").append(MainActivity.getVersionText()).append("\n");
        report.append("Device: ").append(Build.MANUFACTURER).append(", ").append(Build.MODEL)
                .append(", ").append(Build.PRODUCT).append("\n");

        report.append(reportExtraDeviceInfo());

        for (AudioDeviceInfo deviceInfo : devices) {
            report.append("\n==== Device =================== " + deviceInfo.getId() + "\n");
            String item = AudioDeviceInfoConverter.toString(deviceInfo);
            report.append(item);
        }
        report.append(reportAllMicrophones());
        log(report.toString());
    }

    public String reportAllMicrophones() {
        StringBuffer report = new StringBuffer();
        report.append("\n############################");
        report.append("\nMicrophone Report:\n");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            try {
                List<MicrophoneInfo> micList = mAudioManager.getMicrophones();
                for (MicrophoneInfo micInfo : micList) {
                    String micItem = MicrophoneInfoConverter.reportMicrophoneInfo(micInfo);
                    report.append(micItem);
                }
            } catch (IOException e) {
                Log.e(TestAudioActivity.TAG, "Caught ", e);
                return e.getMessage();
            } catch (Exception e) {
                Log.e(TestAudioActivity.TAG, "Caught ", e);
                showErrorToast(e.getMessage());
                report.append("\nERROR: " + e.getMessage() + "\n");
            }
        } else {
            report.append("\nMicrophoneInfo not available on V" + android.os.Build.VERSION.SDK_INT);
        }
        return report.toString();
    }

    private String reportExtraDeviceInfo() {
        StringBuffer report = new StringBuffer();
        report.append("\n\n############################");
        report.append("\nAudioManager:");
        report.append(AudioQueryTools.getAudioManagerReport(mAudioManager));
        report.append("\n\nFeatures:");
        report.append(AudioQueryTools.getAudioFeatureReport(getPackageManager()));
        report.append(AudioQueryTools.getMediaPerformanceClass());
        report.append("\n\nProperties:");
        report.append(AudioQueryTools.getAudioPropertyReport());
        return report.toString();
    }

    // Write to scrollable TextView
    private void log(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAutoTextView.append(text);
                mAutoTextView.append("\n");
            }
        });
    }

    private void logClear() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAutoTextView.setText("");
            }
        });
    }

    protected void showErrorToast(String message) {
        String text = "Error: " + message;
        Log.e(TestAudioActivity.TAG, text);
        showToast(text);
    }

    protected void showToast(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(DeviceReportActivity.this,
                        message,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
