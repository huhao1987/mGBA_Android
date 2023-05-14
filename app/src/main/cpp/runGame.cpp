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

int runGame(char* fname, char * cheats){
    struct mSDLRenderer renderer = {0};

    struct mCoreOptions opts = {
            .useBios = true,
            .logLevel = mLOG_WARN | mLOG_ERROR | mLOG_FATAL,
            .rewindEnable = false,
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
    LOG_D("thecheats %s",cheats);
//    args.patch = "sdcard/gba/hjty.gba";
    if (!args.fname && !args.showVersion) {
    }

    if (SDL_Init(SDL_INIT_VIDEO) < 0) {
        LOG_D("Could not initialize video: %s\n", SDL_GetError());
        mArgumentsDeinit(&args);
        return 1;
    }

    renderer.core = mCoreFind(args.fname);
    if (!renderer.core) {
        LOG_D("Could not run game. Are you sure the file exists and is a compatible game?\n");
        mArgumentsDeinit(&args);
        return 1;
    }

    if (!renderer.core->init(renderer.core)) {
        mArgumentsDeinit(&args);
        return 1;
    }

    renderer.core->desiredVideoDimensions(renderer.core, &renderer.width, &renderer.height);
    renderer.ratio = graphicsOpts.multiplier;
    if (renderer.ratio == 0) {
        renderer.ratio = 1;
    }
    opts.width = renderer.width * renderer.ratio;
    opts.height = renderer.height * renderer.ratio;

    if(cheats!= nullptr){

    }

    struct mCheatDevice* device = renderer.core->cheatDevice(renderer.core);
    if (args.cheatsFile) {
        struct VFile* vf = VFileOpen(args.cheatsFile, O_RDONLY);
        if (vf) {
            mCheatDeviceClear(device);
            mCheatParseFile(device, vf);
            vf->close(vf);
        }
    }

    mInputMapInit(&renderer.core->inputMap, &GBAInputInfo);
    mCoreInitConfig(renderer.core, PORT);
    mArgumentsApply(&args, &subparser, 1, &renderer.core->config);

    mCoreConfigSetDefaultIntValue(&renderer.core->config, "logToStdout", true);
    mCoreConfigLoadDefaults(&renderer.core->config, &opts);
    mCoreLoadConfig(renderer.core);

    renderer.viewportWidth = renderer.core->opts.width;
    renderer.viewportHeight = renderer.core->opts.height;
    renderer.player.fullscreen = renderer.core->opts.fullscreen;
    renderer.player.windowUpdated = 0;

    renderer.lockAspectRatio = renderer.core->opts.lockAspectRatio;
    renderer.lockIntegerScaling = renderer.core->opts.lockIntegerScaling;
    renderer.interframeBlending = renderer.core->opts.interframeBlending;
    renderer.filter = renderer.core->opts.resampleVideo;

#ifdef BUILD_GL
    if (mSDLGLCommonInit(&renderer)) {
		mSDLGLCreate(&renderer);
	} else
#elif defined(BUILD_GLES2) || defined(USE_EPOXY)
#ifdef BUILD_RASPI
    mRPIGLCommonInit(&renderer);
#else
    if (mSDLGLCommonInit(&renderer))
#endif
    {
        mSDLGLES2Create(&renderer);
    } else
#endif
    {
        mSDLSWCreate(&renderer);
    }

    if (!renderer.init(&renderer)) {
        mArgumentsDeinit(&args);
        mCoreConfigDeinit(&renderer.core->config);
        renderer.core->deinit(renderer.core);
        return 1;
    }

    renderer.player.bindings = &renderer.core->inputMap;
    mSDLInitBindingsGBAforAndroid(&renderer.core->inputMap);
    mSDLInitEvents(&renderer.events);
    mSDLEventsLoadConfig(&renderer.events, mCoreConfigGetInput(&renderer.core->config));
    mSDLAttachPlayer(&renderer.events, &renderer.player);
    mSDLPlayerLoadConfig(&renderer.player, mCoreConfigGetInput(&renderer.core->config));

#if SDL_VERSION_ATLEAST(2, 0, 0)
    renderer.core->setPeripheral(renderer.core, mPERIPH_RUMBLE, &renderer.player.rumble.d);
#endif

    int ret;

    // TODO: Use opts and config
    mStandardLoggerInit(&_logger);
    mStandardLoggerConfig(&_logger, &renderer.core->config);
    ret = mSDLRun(&renderer, &args);
    mSDLDetachPlayer(&renderer.events, &renderer.player);
    mInputMapDeinit(&renderer.core->inputMap);

    if (device) {
        mCheatDeviceDestroy(device);
    }

    mSDLDeinit(&renderer);
    mStandardLoggerDeinit(&_logger);

    mArgumentsDeinit(&args);
    mCoreConfigFreeOpts(&opts);
    mCoreConfigDeinit(&renderer.core->config);
    renderer.core->deinit(renderer.core);

    return ret;
}
int mSDLRun(struct mSDLRenderer* renderer, struct mArguments* args) {
    struct mCoreThread thread = {
            .core = renderer->core
    };
    if (!mCoreLoadFile(renderer->core, args->fname)) {
        return 1;
    }
    mCoreAutoloadSave(renderer->core);
    mCoreAutoloadCheats(renderer->core);
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

