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

#ifndef OBOETESTER_FULL_DUPLEX_STREAM_H
#define OBOETESTER_FULL_DUPLEX_STREAM_H

#include <unistd.h>
#include <sys/types.h>

#include "oboe/Oboe.h"

#include "FormatConverterBox.h"

class FullDuplexStream : public oboe::AudioStreamCallback {
public:
    FullDuplexStream() {}
    virtual ~FullDuplexStream() = default;

    void setInputStream(oboe::AudioStream *stream) {
        mInputStream = stream;
    }

    oboe::AudioStream *getInputStream() {
        return mInputStream;
    }

    void setOutputStream(oboe::AudioStream *stream) {
        mOutputStream = stream;
    }
    oboe::AudioStream *getOutputStream() {
        return mOutputStream;
    }

    virtual oboe::Result start();

    virtual oboe::Result stop();

    oboe::ResultWithValue<int32_t>  readInput(int32_t numFrames);

    /**
     * Called when data is available on both streams.
     * Caller should override this method.
     */
    virtual oboe::DataCallbackResult onBothStreamsReady(
            const float *inputData,
            int   numInputFrames,
            float *outputData,
            int   numOutputFrames
            ) = 0;

    /**
     * Called by Oboe when the stream is ready to process audio.
     */
    oboe::DataCallbackResult onAudioReady(
            oboe::AudioStream *audioStream,
            void *audioData,
            int numFrames) override;

    int32_t getMNumInputBurstsCushion() const;

    /**
     * Number of bursts to leave in the input buffer as a cushion.
     * Typically 0 for latency measurements
     * or 1 for glitch tests.
     *
     * @param mNumInputBurstsCushion
     */
    void setMNumInputBurstsCushion(int32_t mNumInputBurstsCushion);

    void setMinimumFramesBeforeRead(int32_t numFrames) {
        mMinimumFramesBeforeRead = numFrames;
    }

    int32_t getMinimumFramesBeforeRead() const {
        return mMinimumFramesBeforeRead;
    }

private:

    // TODO add getters and setters
    static constexpr int32_t kNumCallbacksToDrain   = 20;
    static constexpr int32_t kNumCallbacksToDiscard = 30;

    // let input fill back up, usually 0 or 1
    int32_t mNumInputBurstsCushion =  0;
    int32_t mMinimumFramesBeforeRead = 0;

    // We want to reach a state where the input buffer is empty and
    // the output buffer is full.
    // These are used in order.
    // Drain several callback so that input is empty.
    int32_t              mCountCallbacksToDrain = kNumCallbacksToDrain;
    // Let the input fill back up slightly so we don't run dry.
    int32_t              mCountInputBurstsCushion = mNumInputBurstsCushion;
    // Discard some callbacks so the input and output reach equilibrium.
    int32_t              mCountCallbacksToDiscard = kNumCallbacksToDiscard;

    oboe::AudioStream   *mInputStream = nullptr;
    oboe::AudioStream   *mOutputStream = nullptr;

    std::unique_ptr<FormatConverterBox> mInputConverter;
    std::unique_ptr<FormatConverterBox> mOutputConverter;
};


#endif //OBOETESTER_FULL_DUPLEX_STREAM_H
