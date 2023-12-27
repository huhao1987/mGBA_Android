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

#include <mgba/core/mem-search.h>
#include "mgba/core/interface.h"
#define PORT "sdl"
#include <android/log.h>
#include "android/sdl/android_sdl_events.h"
#include <jni.h>

#define TAG "mgba_android_Test:::"

#define LOG_E(...) __android_log_print(ANDROID_LOG_ERROR,    TAG, __VA_ARGS__)
#define LOG_W(...) __android_log_print(ANDROID_LOG_WARN,     TAG, __VA_ARGS__)
#define LOG_I(...) __android_log_print(ANDROID_LOG_INFO,     TAG, __VA_ARGS__)
#define LOG_D(...) __android_log_print(ANDROID_LOG_DEBUG,    TAG, __VA_ARGS__)

extern char* _fragmentShader;
extern char* _vertexShader;
static void mSDLDeinit(struct mSDLRenderer* renderer);

static int mSDLRun(struct mSDLRenderer* renderer, struct mArguments* args);

static struct mStandardLogger _logger;

static struct VFile* _state = NULL;
static void _loadState(struct mCoreThread* thread) {
    mCoreLoadStateNamed(thread->core, _state, SAVESTATE_RTC);
}
struct mSDLRenderer androidrenderer;
struct mCoreThread thread;

int runGame(char** argv){
    androidrenderer = {0};

    struct mCoreOptions opts = {
            .useBios = true,
            .logLevel = mLOG_WARN | mLOG_ERROR | mLOG_FATAL,
            .rewindEnable = false,
            .audioBuffers = 8192,
            .volume = 0x100,
            .videoSync = true,
            .audioSync = true,
            .interframeBlending = true,
            .lockIntegerScaling = true,
            .lockAspectRatio = true,
            .resampleVideo = true
    };

    struct mArguments args;
    struct mGraphicsOpts graphicsOpts;

    struct mSubParser subparser;

    mSubParserGraphicsInit(&subparser, &graphicsOpts);
    args.fname =  argv[1];
    args.frameskip = 0;
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
    if(argv[2])
    args.cheatsFile = argv[2];
//    if(argv[3])
//        _fragmentShader = argv[3];
//    if(argv[4])
//        _vertexShader = argv[4];
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
    return runGame(argv);
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
JNIEXPORT jboolean JNICALL
Java_hh_game_mgba_1android_activity_GameActivity_QuickSaveState(JNIEnv *env, jobject thiz) {
    return static_cast<jboolean>(mCoreSaveState(androidrenderer.core, 0, SAVESTATE_SAVEDATA | SAVESTATE_SCREENSHOT | SAVESTATE_RTC));
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_hh_game_mgba_1android_activity_GameActivity_QuickLoadState(JNIEnv *env, jobject thiz) {
    return static_cast<jboolean> (mCoreLoadState(androidrenderer.core, 0, SAVESTATE_SCREENSHOT | SAVESTATE_RTC));
}
extern "C"
JNIEXPORT void JNICALL
Java_hh_game_mgba_1android_activity_GameActivity_PauseGame(JNIEnv *env, jobject thiz) {
    mCoreThreadInterrupt(&thread);
}
extern "C"
JNIEXPORT void JNICALL
Java_hh_game_mgba_1android_activity_GameActivity_ResumeGame(JNIEnv *env, jobject thiz) {
    mCoreThreadContinue(&thread);
}

extern "C"
JNIEXPORT void JNICALL
Java_hh_game_mgba_1android_activity_GameActivity_TakeScreenshot(JNIEnv *env, jobject thiz,
                                                                jstring path) {
    //Cannot interrupt when take the screenshot, or it will crash
    char* screenshotpath = convertJStringToChar(env,path);
    struct VFile* screenshotFile = VFileOpen(screenshotpath, O_RDWR);
    mCoreTakeScreenshotVF(androidrenderer.core,screenshotFile);
}

void muteVolume(bool mute){
    androidrenderer.core->opts.mute = mute;
    thread.core->reloadConfigOption(thread.core, NULL, NULL);
}

extern "C"
JNIEXPORT void JNICALL
Java_hh_game_mgba_1android_activity_GameActivity_Forward(JNIEnv *env, jobject thiz, jfloat speed) {
    thread.impl->sync.fpsTarget = speed;
    thread.core->reloadConfigOption(thread.core, NULL, NULL);
}

extern "C"
JNIEXPORT void JNICALL
Java_hh_game_mgba_1android_activity_GameActivity_Mute(JNIEnv *env, jobject thiz, jboolean mute) {
    muteVolume(mute);
}

extern "C"
JNIEXPORT void JNICALL
Java_hh_game_mgba_1android_utils_CheatUtils_00024Companion_memorySearch(JNIEnv *env, jobject thiz,jint search_value) {
    struct mCoreMemorySearchParams* params;
    params->memoryFlags = mCORE_MEMORY_RW;
    params->type = mCORE_MEMORY_SEARCH_INT;
    params->op = mCORE_MEMORY_SEARCH_EQUAL;
    params->valueInt =search_value;
    params->width = sizeof(params->valueInt);
    params->align = -1;
    struct mCoreMemorySearchResults* out;
    mCoreMemorySearch(thread.core,params,out,10000);
    out;
}

#include <jni.h>

jobject createIntArray(JNIEnv* env, uint32_t* memresult, uint32_t start, int length) {
    jclass arrayListClass = env->FindClass("java/util/ArrayList");
    if (arrayListClass == NULL) {
        return NULL;
    }
    jclass pairClass = env->FindClass("kotlin/Pair");
    if (pairClass == NULL) {
        return NULL;
    }
    jmethodID arrayListConstructor = env->GetMethodID(arrayListClass, "<init>", "()V");
    if (arrayListConstructor == NULL) {
        return NULL;
    }
    jobject arrayList = env->NewObject(arrayListClass, arrayListConstructor);
    if (arrayList == NULL) {
        return NULL;
    }
    jmethodID arrayListAdd = env->GetMethodID(arrayListClass, "add", "(Ljava/lang/Object;)Z");
    if (arrayListAdd == NULL) {
        return NULL;
    }
    jmethodID pairConstructor = env->GetMethodID(pairClass, "<init>", "(Ljava/lang/Object;Ljava/lang/Object;)V");
    if (pairConstructor == NULL) {
        return NULL;
    }
    for (int i = 0; i < length; i += 4) {
        uint32_t address = start + i;
        jobject pair = env->NewObject(pairClass, pairConstructor, env->NewObject(env->FindClass("java/lang/Integer"), env->GetMethodID(env->FindClass("java/lang/Integer"), "<init>", "(I)V"),  (jint)address), env->NewObject(env->FindClass("java/lang/Integer"), env->GetMethodID(env->FindClass("java/lang/Integer"), "<init>", "(I)V"), (jint)memresult[i >> 2]));
        env->CallBooleanMethod(arrayList, arrayListAdd, pair);
        env->DeleteLocalRef(pair);
    }
    return arrayList;
}


extern "C"
JNIEXPORT jobject JNICALL
Java_hh_game_mgba_1android_activity_GameActivity_getMemoryBlock(JNIEnv *env, jobject thiz) {
    mCoreThreadInterrupt(&thread);
    const struct mCoreMemoryBlock* blocks;
    size_t nBlocks = androidrenderer.core->listMemoryBlocks(androidrenderer.core, &blocks);
    jclass arrayListClass = env->FindClass("java/util/ArrayList");
    jmethodID arrayListConstructor = env->GetMethodID(arrayListClass, "<init>", "()V");
    jmethodID arrayListAddMethod = env->GetMethodID(arrayListClass, "add", "(Ljava/lang/Object;)Z");
    jobject arrayListObject = env->NewObject(arrayListClass, arrayListConstructor);
    jclass kotlinClass = env->FindClass("hh/game/mgba_android/memory/CoreMemoryBlock");
    jmethodID constructor = env->GetMethodID(kotlinClass, "<init>",
                                             "(JLjava/lang/String;Ljava/lang/String;Ljava/lang/String;IIIISILjava/util/ArrayList;)V");

    for (int i = 0; i < nBlocks; i++) {
        jclass arrayListClass = env->FindClass("java/util/ArrayList");
        jmethodID arrayListConstructor = env->GetMethodID(arrayListClass, "<init>", "()V");
        jobject arrayList = env->NewObject(arrayListClass, arrayListConstructor);
        const struct mCoreMemoryBlock* block = &blocks[i];
        if(block->id == 2|| block->id ==3){
            size_t size;
            void* mem = androidrenderer.core->getMemoryBlock(androidrenderer.core, block->id, &size);
            if(mem){
                uint32_t* memresult = getBlock(mem);
                uint32_t start = block->start;
                arrayList = createIntArray(env, memresult, start, block->size);
            }
        }

        jobject kotlinObject = env->NewObject(kotlinClass, constructor,
                                              (jlong)block->id,
                                              env->NewStringUTF(block->internalName),
                                              env->NewStringUTF(block->shortName),
                                              env->NewStringUTF(block->longName),
                                              (jint)block->start,
                                              (jint)block->end,
                                              (jint)block->size,
                                              (jint)block->flags,
                                              (jshort)block->maxSegment,
                                              (jint)block->segmentStart,
                                              arrayList
        );
        env->CallBooleanMethod(arrayListObject, arrayListAddMethod, kotlinObject);
    }
    mCoreThreadContinue(&thread);
    return arrayListObject;

}

extern "C"
JNIEXPORT void JNICALL
Java_hh_game_mgba_1android_activity_GameActivity_writeMem(JNIEnv *env, jobject thiz, jint address,
                                                          jint value) {
    androidrenderer.core->busWrite32(androidrenderer.core,(uint32_t)address,(int32_t)value);
}