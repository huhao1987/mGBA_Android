#include "android_sdl_events.h"

#include "sdl-events.h"
#include "android_sdl_events.h"
#include <mgba/core/core.h>
#include <mgba/core/input.h>
#include <mgba/core/serialize.h>
#include <mgba/core/thread.h>
#include <mgba/debugger/debugger.h>
#include <mgba/internal/gba/input.h>
#include <mgba-util/configuration.h>
#include <mgba-util/formatting.h>
#include <mgba-util/vfs.h>

void mSDLInitBindingsGBAforAndroid(struct mInputMap *inputMap) {
    mInputBindKey(inputMap, SDL_BINDING_KEY, SDLK_x, GBA_KEY_A);
    mInputBindKey(inputMap, SDL_BINDING_KEY, SDLK_z, GBA_KEY_B);
    mInputBindKey(inputMap, SDL_BINDING_KEY, SDLK_q, GBA_KEY_L);
    mInputBindKey(inputMap, SDL_BINDING_KEY, SDLK_u, GBA_KEY_R);
    mInputBindKey(inputMap, SDL_BINDING_KEY, SDLK_y, GBA_KEY_START);
    mInputBindKey(inputMap, SDL_BINDING_KEY, SDLK_n, GBA_KEY_SELECT);
    mInputBindKey(inputMap, SDL_BINDING_KEY, SDLK_UP, GBA_KEY_UP);
    mInputBindKey(inputMap, SDL_BINDING_KEY, SDLK_DOWN, GBA_KEY_DOWN);
    mInputBindKey(inputMap, SDL_BINDING_KEY, SDLK_LEFT, GBA_KEY_LEFT);
    mInputBindKey(inputMap, SDL_BINDING_KEY, SDLK_RIGHT, GBA_KEY_RIGHT);
//	struct mInputAxis description = { GBA_KEY_RIGHT, GBA_KEY_LEFT, 0x4000, -0x4000 };
//	mInputBindAxis(inputMap, SDL_BINDING_BUTTON, 0, &description);
//	description = (struct mInputAxis) { GBA_KEY_DOWN, GBA_KEY_UP, 0x4000, -0x4000 };
//	mInputBindAxis(inputMap, SDL_BINDING_BUTTON, 1, &description);
//
//	mInputBindHat(inputMap, SDL_BINDING_BUTTON, 0, &GBAInputInfo.hat);
}
