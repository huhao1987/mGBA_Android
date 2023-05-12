/*
 * Copyright (C) 2020 The Android Open Source Project
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

#ifndef ANALYZER_BASE_SINE_ANALYZER_H
#define ANALYZER_BASE_SINE_ANALYZER_H

#include <algorithm>
#include <cctype>
#include <iomanip>
#include <iostream>

#include "InfiniteRecording.h"
#include "LatencyAnalyzer.h"

/**
 * Output a steady sine wave and analyze the return signal.
 *
 * Use a cosine transform to measure the predicted magnitude and relative phase of the
 * looped back sine wave. Then generate a predicted signal and compare with the actual signal.
 */
class BaseSineAnalyzer : public LoopbackProcessor {
public:

    BaseSineAnalyzer()
            : LoopbackProcessor()
            , mInfiniteRecording(64 * 1024) {}


    virtual bool isOutputEnabled() { return true; }

    void setMagnitude(double magnitude) {
        mMagnitude = magnitude;
        mScaledTolerance = mMagnitude * mTolerance;
    }

    double getPhaseOffset() {
        return mPhaseOffset;
    }

    double getMagnitude() const {
        return mMagnitude;
    }

    void setInputChannel(int inputChannel) {
        mInputChannel = inputChannel;
    }

    int getInputChannel() const {
        return mInputChannel;
    }

    void setOutputChannel(int outputChannel) {
        mOutputChannel = outputChannel;
    }

    int getOutputChannel() const {
        return mOutputChannel;
    }

    void setNoiseAmplitude(double noiseAmplitude) {
        mNoiseAmplitude = noiseAmplitude;
    }

    double getNoiseAmplitude() const {
        return mNoiseAmplitude;
    }

    double getTolerance() {
        return mTolerance;
    }

    void setTolerance(double tolerance) {
        mTolerance = tolerance;
    }

    // advance and wrap phase
    void incrementOutputPhase() {
        mOutputPhase += mPhaseIncrement;
        if (mOutputPhase > M_PI) {
            mOutputPhase -= (2.0 * M_PI);
        }
    }

    /**
     * @param frameData upon return, contains the reference sine wave
     * @param channelCount
     */
    result_code processOutputFrame(float *frameData, int channelCount) override {
        float output = 0.0f;
        // Output sine wave so we can measure it.
        if (isOutputEnabled()) {
            float sinOut = sinf(mOutputPhase);
            incrementOutputPhase();
            output = (sinOut * mOutputAmplitude)
                     + (mWhiteNoise.nextRandomDouble() * getNoiseAmplitude());
            // ALOGD("sin(%f) = %f, %f\n", mOutputPhase, sinOut,  kPhaseIncrement);
        }
        for (int i = 0; i < channelCount; i++) {
            frameData[i] = (i == mOutputChannel) ? output : 0.0f;
        }
        return RESULT_OK;
    }

    /**
     * Calculate the magnitude of the component of the input signal
     * that matches the analysis frequency.
     * Also calculate the phase that we can use to create a
     * signal that matches that component.
     * The phase will be between -PI and +PI.
     */
    double calculateMagnitudePhase(double *phasePtr = nullptr) {
        if (mFramesAccumulated == 0) {
            return 0.0;
        }
        double sinMean = mSinAccumulator / mFramesAccumulated;
        double cosMean = mCosAccumulator / mFramesAccumulated;
        double magnitude = 2.0 * sqrt((sinMean * sinMean) + (cosMean * cosMean));
        if (phasePtr != nullptr) {
            double phase = atan2(cosMean, sinMean);

            *phasePtr = phase;
        }
        return magnitude;
    }

    /**
     * Perform sin/cos analysis on each sample.
     * Measure magnitude and phase on every period.
     * Updates mPhaseOffset
     * @param sample
     * @param referencePhase
     * @return true if magnitude and phase updated
     */
    bool transformSample(float sample, float referencePhase) {
        // Track incoming signal and slowly adjust magnitude to account
        // for drift in the DRC or AGC.
        mSinAccumulator += static_cast<double>(sample) * sinf(referencePhase);
        mCosAccumulator += static_cast<double>(sample) * cosf(referencePhase);
        mFramesAccumulated++;
        // Must be a multiple of the period or the calculation will not be accurate.
        if (mFramesAccumulated == mSinePeriod) {
            const double coefficient = 0.1;
            double magnitude = calculateMagnitudePhase(&mPhaseOffset);
            // One pole averaging filter.
            setMagnitude((mMagnitude * (1.0 - coefficient)) + (magnitude * coefficient));
            resetAccumulator();
            return true;
        } else {
            return false;
        }
    }

    // reset the sine wave detector
    virtual void resetAccumulator() {
        mFramesAccumulated = 0;
        mSinAccumulator = 0.0;
        mCosAccumulator = 0.0;
    }

    void reset() override {
        LoopbackProcessor::reset();
        resetAccumulator();
        mMagnitude = 0.0;
    }

    void prepareToTest() override {
        LoopbackProcessor::prepareToTest();
        mSinePeriod = getSampleRate() / kTargetGlitchFrequency;
        mOutputPhase = 0.0f;
        mInverseSinePeriod = 1.0 / mSinePeriod;
        mPhaseIncrement = 2.0 * M_PI * mInverseSinePeriod;
    }

protected:
    static constexpr int32_t kTargetGlitchFrequency = 1000;

    int32_t mSinePeriod = 1; // this will be set before use
    double  mInverseSinePeriod = 1.0;
    double  mPhaseIncrement = 0.0;
    double  mOutputPhase = 0.0;
    double  mOutputAmplitude = 0.75;
    // If this jumps around then we are probably just hearing noise.
    double  mPhaseOffset = 0.0;
    double  mMagnitude = 0.0;
    int32_t mFramesAccumulated = 0;
    double  mSinAccumulator = 0.0;
    double  mCosAccumulator = 0.0;
    double  mScaledTolerance = 0.0;

    InfiniteRecording<float> mInfiniteRecording;

private:
    int32_t mInputChannel = 0;
    int32_t mOutputChannel = 0;
    float   mTolerance = 0.10; // scaled from 0.0 to 1.0

    float mNoiseAmplitude = 0.00; // Used to experiment with warbling caused by DRC.
    PseudoRandom  mWhiteNoise;
};

#endif //ANALYZER_BASE_SINE_ANALYZER_H
