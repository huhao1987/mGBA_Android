#include "android/sdl/main.h"
#include <mgba/internal/debugger/cli-debugger.h>
#ifdef ENABLE_SCRIPTING
#include <mgba/core/scripting.h>

#endif

#include <mgba/core/cheats.h>
#include <mgba/core/core.h>
#include <mgba/core/config.h>
#include <mgba/core/input.h>
#include <mgba/core/serialize.h>
#include <mgba/core/thread.h>
#include <mgba/internal/gba/input.h>

#include <mgba/feature/commandline.h>
#include <mgba-util/vfs.h>

#include <SDL.h>

#include <errno.h>
#include <signal.h>

#define PORT "sdl"
#include <android/log.h>
#include "android/sdl/android_sdl_events.h"
#include <jni.h>

#define TAG "mgba_android_Test:::"

#define LOG_E(...) __android_log_print(ANDROID_LOG_ERROR,    TAG, __VA_ARGS__)
#define LOG_W(...) __android_log_print(ANDROID_LOG_WARN,     TAG, __VA_ARGS__)
#define LOG_I(...) __android_log_print(ANDROID_LOG_INFO,     TAG, __VA_ARGS__)
#define LOG_D(...) __android_log_print(ANDROID_LOG_DEBUG,    TAG, __VA_ARGS__)

static void mSDLDeinit(struct mSDLRenderer* renderer);

static int mSDLRun(struct mSDLRenderer* renderer, struct mArguments* args);

static struct mStandardLogger _logger;

static struct VFile* _state = NULL;
static void _loadState(struct mCoreThread* thread) {
    mCoreLoadStateNamed(thread->core, _state, SAVESTATE_RTC);
}
struct mSDLRenderer androidrenderer;
struct mCoreThread thread;
int runGame(char* fname, char * internalcheatfile){
    androidrenderer = {0};

    struct mCoreOptions opts = {
            .useBios = true,
            .logLevel = mLOG_WARN | mLOG_ERROR | mLOG_FATAL,
            .rewindEnable = true,
            .rewindBufferCapacity = 600,
            .audioBuffers = 4096,
            .volume = 0x100,
            .videoSync = true,
            .audioSync = true
    };

    struct mArguments args;
    struct mGraphicsOpts graphicsOpts;

    struct mSubParser subparser;

    mSubParserGraphicsInit(&subparser, &graphicsOpts);
    args.fname = fname;
    args.frameskip = 0;
    LOG_D("thecheats %s",internalcheatfile);
    //    args.patch = "sdcard/gba/hjty.gba";
    if (!args.fname && !args.showVersion) {
    }

    if (SDL_Init(SDL_INIT_VIDEO) < 0) {
        LOG_D("Could not initialize video: %s\n", SDL_GetError());
        mArgumentsDeinit(&args);
        return 1;
    }

    androidrenderer.core = mCoreFind(args.fname);
    if (!androidrenderer.core) {
        LOG_D("Could not run game. Are you sure the file exists and is a compatible game?\n");
        mArgumentsDeinit(&args);
        return 1;
    }

    if (!androidrenderer.core->init(androidrenderer.core)) {
        mArgumentsDeinit(&args);
        return 1;
    }

    androidrenderer.core->desiredVideoDimensions(androidrenderer.core, &androidrenderer.width, &androidrenderer.height);
    androidrenderer.ratio = graphicsOpts.multiplier;
    if (androidrenderer.ratio == 0) {
        androidrenderer.ratio = 1;
    }
    opts.width = androidrenderer.width * androidrenderer.ratio;
    opts.height = androidrenderer.height * androidrenderer.ratio;



    struct mCheatDevice* device = androidrenderer.core->cheatDevice(androidrenderer.core);
    args.cheatsFile = internalcheatfile;

    mInputMapInit(&androidrenderer.core->inputMap, &GBAInputInfo);
    mCoreInitConfig(androidrenderer.core, PORT);
    mArgumentsApply(&args, &subparser, 1, &androidrenderer.core->config);

    mCoreConfigSetDefaultIntValue(&androidrenderer.core->config, "logToStdout", true);
    mCoreConfigLoadDefaults(&androidrenderer.core->config, &opts);
    mCoreLoadConfig(androidrenderer.core);

    androidrenderer.viewportWidth = androidrenderer.core->opts.width;
    androidrenderer.viewportHeight = androidrenderer.core->opts.height;
    androidrenderer.player.fullscreen = androidrenderer.core->opts.fullscreen;
    androidrenderer.player.windowUpdated = 0;

    androidrenderer.lockAspectRatio = androidrenderer.core->opts.lockAspectRatio;
    androidrenderer.lockIntegerScaling = androidrenderer.core->opts.lockIntegerScaling;
    androidrenderer.interframeBlending = androidrenderer.core->opts.interframeBlending;
    androidrenderer.filter = androidrenderer.core->opts.resampleVideo;

#ifdef BUILD_GL
    if (mSDLGLCommonInit(&renderer)) {
		mSDLGLCreate(&renderer);
	} else
#elif defined(BUILD_GLES2) || defined(USE_EPOXY)
#ifdef BUILD_RASPI
    mRPIGLCommonInit(&renderer);
#else
    if (mSDLGLCommonInit(&androidrenderer))
#endif
    {
        mSDLGLES2Create(&androidrenderer);
    } else
#endif
    {
        mSDLSWCreate(&androidrenderer);
    }

    if (!androidrenderer.init(&androidrenderer)) {
        mArgumentsDeinit(&args);
        mCoreConfigDeinit(&androidrenderer.core->config);
        androidrenderer.core->deinit(androidrenderer.core);
        return 1;
    }

    androidrenderer.player.bindings = &androidrenderer.core->inputMap;
    mSDLInitBindingsGBAforAndroid(&androidrenderer.core->inputMap);
    mSDLInitEvents(&androidrenderer.events);
    mSDLEventsLoadConfig(&androidrenderer.events, mCoreConfigGetInput(&androidrenderer.core->config));
    mSDLAttachPlayer(&androidrenderer.events, &androidrenderer.player);
    mSDLPlayerLoadConfig(&androidrenderer.player, mCoreConfigGetInput(&androidrenderer.core->config));

#if SDL_VERSION_ATLEAST(2, 0, 0)
    androidrenderer.core->setPeripheral(androidrenderer.core, mPERIPH_RUMBLE, &androidrenderer.player.rumble.d);
#endif

    int ret;

    // TODO: Use opts and config
    mStandardLoggerInit(&_logger);
    mStandardLoggerConfig(&_logger, &androidrenderer.core->config);
    ret = mSDLRun(&androidrenderer, &args);
    mSDLDetachPlayer(&androidrenderer.events, &androidrenderer.player);
    mInputMapDeinit(&androidrenderer.core->inputMap);

    if (device) {
        mCheatDeviceDestroy(device);
    }

    mSDLDeinit(&androidrenderer);
    mStandardLoggerDeinit(&_logger);

    mArgumentsDeinit(&args);
    mCoreConfigFreeOpts(&opts);
    mCoreConfigDeinit(&androidrenderer.core->config);
    androidrenderer.core->deinit(androidrenderer.core);

    return ret;
}
int mSDLRun(struct mSDLRenderer* renderer, struct mArguments* args) {
    thread = {
            .core = renderer->core
    };
    if (!mCoreLoadFile(renderer->core, args->fname)) {
        return 1;
    }
    mCoreAutoloadSave(renderer->core);
    if(args->cheatsFile)
        mCoreAutoloadCheatsFromFile(renderer->core, args->cheatsFile);
#ifdef ENABLE_SCRIPTING
    struct mScriptBridge* bridge = mScriptBridgeCreate();
#ifdef ENABLE_PYTHON
	mPythonSetup(bridge);
#endif
#ifdef USE_DEBUGGERS
	CLIDebuggerScriptEngineInstall(bridge);
#endif
#endif

#ifdef USE_DEBUGGERS
    struct mDebugger* debugger = mDebuggerCreate(args->debuggerType, renderer->core);
	if (debugger) {
#ifdef USE_EDITLINE
		if (args->debuggerType == DEBUGGER_CLI) {
			struct CLIDebugger* cliDebugger = (struct CLIDebugger*) debugger;
			CLIDebuggerAttachBackend(cliDebugger, CLIDebuggerEditLineBackendCreate());
		}
#endif
		mDebuggerAttach(debugger, renderer->core);
		mDebuggerEnter(debugger, DEBUGGER_ENTER_MANUAL, NULL);
#ifdef ENABLE_SCRIPTING
		mScriptBridgeSetDebugger(bridge, debugger);
#endif
	}
#endif

    if (args->patch) {
        struct VFile* patch = VFileOpen(args->patch, O_RDONLY);
        if (patch) {
            renderer->core->loadPatch(renderer->core, patch);
        }
    } else {
        mCoreAutoloadPatch(renderer->core);
    }

    renderer->audio.samples = renderer->core->opts.audioBuffers;
    renderer->audio.sampleRate = 44100;
    thread.logger.logger = &_logger.d;

    bool didFail = !mCoreThreadStart(&thread);
    if (!didFail) {
//#if SDL_VERSION_ATLEAST(2, 0, 0)
        renderer->core->desiredVideoDimensions(renderer->core, &renderer->width, &renderer->height);
        unsigned width = renderer->width * renderer->ratio;
        unsigned height = renderer->height * renderer->ratio;
        if (width != (unsigned) renderer->viewportWidth && height != (unsigned) renderer->viewportHeight) {
            SDL_SetWindowSize(renderer->window, width, height);
            renderer->player.windowUpdated = 1;
        }
        mSDLSetScreensaverSuspendable(&renderer->events, renderer->core->opts.suspendScreensaver);
        mSDLSuspendScreensaver(&renderer->events);
//#endif
        if (mSDLInitAudio(&renderer->audio, &thread)) {
            if (args->savestate) {
                struct VFile* state = VFileOpen(args->savestate, O_RDONLY);
                if (state) {
                    _state = state;
                    mCoreThreadRunFunction(&thread, _loadState);
                    _state = NULL;
                    state->close(state);
                }
            }
            renderer->runloop(renderer, &thread);
            mSDLPauseAudio(&renderer->audio);
            if (mCoreThreadHasCrashed(&thread)) {
                didFail = true;
                LOG_D("The game crashed!\n");
                mCoreThreadEnd(&thread);
            }
        } else {
            didFail = true;
            LOG_D("Could not initialize audio.\n");
        }
#if SDL_VERSION_ATLEAST(2, 0, 0)
        mSDLResumeScreensaver(&renderer->events);
        mSDLSetScreensaverSuspendable(&renderer->events, false);
#endif
        mCoreThreadJoin(&thread);
    } else {
        LOG_D("Could not run game. Are you sure the file exists and is a compatible game?\n");
    }
    renderer->core->unloadROM(renderer->core);

#ifdef ENABLE_SCRIPTING
    mScriptBridgeDestroy(bridge);
#endif

    return didFail;
}

static void mSDLDeinit(struct mSDLRenderer* renderer) {
    mSDLDeinitEvents(&renderer->events);
    mSDLDeinitAudio(&renderer->audio);
#if SDL_VERSION_ATLEAST(2, 0, 0)
    SDL_DestroyWindow(renderer->window);
#endif

    renderer->deinit(renderer);

    SDL_Quit();
}
int main(int argc, char** argv) {
    return runGame(argv[1],argv[2]);
}

char* convertJStringToChar(JNIEnv* env, jstring jstr) {
    if (jstr == NULL) {
        return NULL;
    }
    const char* str = env->GetStringUTFChars(jstr, NULL);
    if (str == NULL) {
        return NULL;
    }
    jsize len = env->GetStringUTFLength(jstr);
    jbyteArray bytes = env->NewByteArray(len);
    if (bytes == NULL) {
        env->ReleaseStringUTFChars(jstr, str);
        return NULL;
    }
    env->SetByteArrayRegion(bytes, 0, len, reinterpret_cast<const jbyte*>(str));
    char* result = reinterpret_cast<char*>(env->GetByteArrayElements(bytes, NULL));
    env->ReleaseStringUTFChars(jstr, str);
    env->DeleteLocalRef(bytes);
    return result;
}


extern "C"
JNIEXPORT void JNICALL
Java_hh_game_mgba_1android_activity_GameActivity_reCallCheats(JNIEnv *env, jobject thiz,jstring cheatfile) {
    char* cheat = convertJStringToChar(env,cheatfile);
    mCoreAutoloadCheatsFromFile(androidrenderer.core, cheat);
}

extern "C"
JNIEXPORT void JNICALL
Java_hh_game_mgba_1android_activity_GameActivity_SaveState(JNIEnv *env, jobject thiz) {
    mCoreThreadInterrupt(&thread);
    mCoreSaveState(androidrenderer.core, 0, SAVESTATE_SAVEDATA | SAVESTATE_SCREENSHOT | SAVESTATE_RTC);
    mCoreThreadContinue(&thread);
}
extern "C"
JNIEXPORT void JNICALL
Java_hh_game_mgba_1android_activity_GameActivity_LoadState(JNIEnv *env, jobject thiz) {
    mCoreThreadInterrupt(&thread);
    mCoreLoadState(androidrenderer.core, 0, SAVESTATE_SCREENSHOT | SAVESTATE_RTC);
    mCoreThreadContinue(&thread);
}