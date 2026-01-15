#include <jni.h>
#include <mgba/core/core.h>
#include "SDL_timer.h"

static struct mCore *core;
const char *jstr;
extern "C"
JNIEXPORT void JNICALL
Java_hh_game_mgba_1android_utils_Gameutils_initCore(JNIEnv *env, jobject thiz, jstring path) {
    jstr = env->GetStringUTFChars(path, nullptr);
    core = mCoreFind(jstr);
    core->init(core);
}
extern "C"
JNIEXPORT jstring JNICALL
Java_hh_game_mgba_1android_utils_Gameutils_getGameTitle(JNIEnv *env, jobject thiz) {
    jstring str = nullptr;
    if (mCoreLoadFile(core, jstr)) {
        struct mGameInfo info;
        core->getGameInfo(core, &info);
        str = env->NewStringUTF(info.title);
    }
    return str;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_hh_game_mgba_1android_utils_Gameutils_getGameCode(JNIEnv *env, jobject thiz) {
    jstring str = nullptr;
    if (mCoreLoadFile(core, jstr)) {
        struct mGameInfo info;
        core->getGameInfo(core, &info);
        str = env->NewStringUTF(info.code);
    }
    return str;
}
Uint32 last_tick;
float fps;
extern "C"
JNIEXPORT jfloat JNICALL
Java_hh_game_mgba_1android_utils_Gameutils_00024Companion_getFPS(JNIEnv *env, jobject thiz) {
    return 0;
}