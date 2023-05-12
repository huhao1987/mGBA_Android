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
        char title[32] = {0};
        core->getGameTitle(core, title);
        const char *s(title);
         str = env->NewStringUTF(s);
    }
    return str;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_hh_game_mgba_1android_utils_Gameutils_getGameCode(JNIEnv *env, jobject thiz) {
    jstring str = nullptr;
    if (mCoreLoadFile(core, jstr)) {
        char title[32] = {0};
        core->getGameCode(core, title);
        const char *code(title);
        str = env->NewStringUTF(code);
    }
    return str;
}

extern "C"
JNIEXPORT jint JNICALL
Java_hh_game_mgba_1android_utils_Gameutils_00024Companion_getFPS(JNIEnv *env, jobject thiz) {
    static uint32_t lastTime = 0;
    uint32_t now = SDL_GetTicks();
    uint32_t delta = now - lastTime;
    lastTime = now;
    return delta;
}