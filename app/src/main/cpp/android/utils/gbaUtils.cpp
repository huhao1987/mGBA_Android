#include <jni.h>
#include <mgba/core/core.h>

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
    if (mCoreLoadFile(core, jstr)) {
        char title[32] = {0};
        core->getGameTitle(core, title);
        const char *s(title);
        jstring jstr = env->NewStringUTF(s);
        return jstr;
    }
}
extern "C"
JNIEXPORT jstring JNICALL
Java_hh_game_mgba_1android_utils_Gameutils_getGameCode(JNIEnv *env, jobject thiz) {
    if (mCoreLoadFile(core, jstr)) {
        char title[32] = {0};
        core->getGameCode(core, title);
        const char *code(title);
        jstring jstr = env->NewStringUTF(code);
        return jstr;
    }
}