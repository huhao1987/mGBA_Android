package com.mobileer.audio_device;
/*
 * Copyright 2022 The Android Open Source Project
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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources.Theme;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.util.AttributeSet;
import android.widget.Spinner;

import com.mobileer.oboetester.R;

import java.util.List;

public class CommunicationDeviceSpinner extends Spinner {
    private static final int CLEAR_DEVICE_ID = 0;
    private static final String TAG = CommunicationDeviceSpinner.class.getName();
    private AudioDeviceAdapter mDeviceAdapter;
    private AudioManager mAudioManager;
    private Context mContext;
    AudioDeviceInfo[] mCommDeviceArray = null;

    public CommunicationDeviceSpinner(Context context){
        super(context);
        setup(context);
    }

    public CommunicationDeviceSpinner(Context context, int mode){
        super(context, mode);
        setup(context);
    }

    public CommunicationDeviceSpinner(Context context, AttributeSet attrs){
        super(context, attrs);
        setup(context);
    }

    public CommunicationDeviceSpinner(Context context, AttributeSet attrs, int defStyleAttr){
        super(context, attrs, defStyleAttr);
        setup(context);
    }

    public CommunicationDeviceSpinner(Context context, AttributeSet attrs, int defStyleAttr, int mode){
        super(context, attrs, defStyleAttr, mode);
        setup(context);
    }

    public CommunicationDeviceSpinner(Context context, AttributeSet attrs, int defStyleAttr,
                                      int defStyleRes, int mode){
        super(context, attrs, defStyleAttr, defStyleRes, mode);
        setup(context);
    }

    public CommunicationDeviceSpinner(Context context, AttributeSet attrs, int defStyleAttr,
                                      int defStyleRes, int mode, Theme popupTheme){
        super(context, attrs, defStyleAttr, defStyleRes, mode, popupTheme);
        setup(context);
    }

    public AudioDeviceInfo[] getCommunicationsDevices() {
        return mCommDeviceArray;
    }

    private void setup(Context context){
        mContext = context;

        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        mDeviceAdapter = new AudioDeviceAdapter(context);
        setAdapter(mDeviceAdapter);

        // Add a default entry to the list and select it
        mDeviceAdapter.add(new AudioDeviceListEntry(CLEAR_DEVICE_ID,
                mContext.getString(R.string.auto_select)));
        setSelection(0);
        setupCommunicationDeviceListener();
    }

    @TargetApi(31)
    private void setupCommunicationDeviceListener(){
        // Note that we will immediately receive a call to onDevicesAdded with the list of
        // devices which are currently connected.
        mAudioManager.registerAudioDeviceCallback(new AudioDeviceCallback() {
            @Override
            public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
                updateDeviceList();
            }

            public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
                updateDeviceList();
            }

            private void updateDeviceList() {
                mDeviceAdapter.clear();
                mDeviceAdapter.add(new AudioDeviceListEntry(CLEAR_DEVICE_ID,
                        mContext.getString(R.string.clear)));
                setSelection(0);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    List<AudioDeviceInfo> commDeviceList = mAudioManager.getAvailableCommunicationDevices();
                    mCommDeviceArray = commDeviceList.toArray(new AudioDeviceInfo[0]);
                    // Communications Devices are always OUTPUTS.
                    List<AudioDeviceListEntry> deviceList =
                            AudioDeviceListEntry.createListFrom(
                                    mCommDeviceArray, AudioManager.GET_DEVICES_OUTPUTS);
                    mDeviceAdapter.addAll(deviceList);
                }
            }
        }, null);
    }
}
