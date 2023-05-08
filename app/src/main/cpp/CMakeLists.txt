cmake_minimum_required(VERSION 3.22.1)
project("mgba_android")

set(BUILD_QT OFF)
set(BUILD_SDL OFF)
set(ANDROID ON)


add_definitions(-DHAVE_STRTOF_L=1)
add_definitions(-DHAVE_LOCALE=1)
add_definitions(-DBUILD_GLES2=1)
add_definitions(-DUSE_PTHREADS=1)
add_subdirectory(SDL2)
INCLUDE_DIRECTORIES("SDL2/include")
add_subdirectory(mgba)
INCLUDE_DIRECTORIES("mgba/include")
INCLUDE_DIRECTORIES("mgba/src")
INCLUDE_DIRECTORIES("mgba/src/platform/sdl")
file(GLOB PLATFORM_SRC mgba/src/platform/sdl/sdl-*.c)
#list(REMOVE_ITEM PLATFORM_SRC
#		mgba/src/platform/sdl/sdl-events.c
#		)
add_library(${CMAKE_PROJECT_NAME} SHARED
		runGame.cpp
		mgba/src/platform/sdl/sw-sdl2.c
		android/sdl/gl-common.c
		android/sdl/gles2-sdl.c
#		android/sdl/sdl-events.c
		${PLATFORM_SRC}
		)

target_link_libraries(${CMAKE_PROJECT_NAME}
		mgba
		SDL2
		android
		GLESv2
		OpenSLES
		log)