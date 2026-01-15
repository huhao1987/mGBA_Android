cmake_minimum_required(VERSION 3.22.1)
project("mgba_android")

set(ANDROID ON)

add_definitions(-DHAVE_STRTOF_L=1)
add_definitions(-DHAVE_LOCALE=1)
add_definitions(-DBUILD_GLES2=1)
add_definitions(-DUSE_PTHREADS=1)
add_definitions(-DENABLE_VFS -DENABLE_VFS_FD -DENABLE_DIRECTORIES)

add_subdirectory(SDL2)
add_subdirectory(mgba)

INCLUDE_DIRECTORIES("SDL2/include" "mgba/include" "mgba/src" "mgba/src/platform/sdl" "android/opengl" "android/sdl")
file(GLOB PLATFORM_SRC mgba/src/platform/sdl/sdl-*.c)
file(GLOB GBA_UTIL_SRC android/utils/*.cpp)
add_library(${CMAKE_PROJECT_NAME} SHARED
		runGame.cpp
		mgba/src/platform/sdl/sw-sdl2.c
		android/sdl/gl-common.c
    mgba/src/platform/android/oboe-audio.cpp
		android/sdl/gles2-sdl.c
		android/sdl/android_sdl_events.cpp
		${PLATFORM_SRC}
		${GBA_UTIL_SRC}
		)


find_package(games-frame-pacing REQUIRED CONFIG)
find_package(oboe REQUIRED CONFIG)

target_link_libraries(${CMAKE_PROJECT_NAME}
		mgba
		SDL2
		android
		GLESv2
        EGL
		OpenSLES
		log
        games-frame-pacing::swappy_static
        oboe::oboe)