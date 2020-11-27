# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

#IF (${CMAKE_HOST_SYSTEM_NAME} MATCHES "Windows")
#    ADD_DEFINITIONS(-DWindows)
#ELSE (${CMAKE_HOST_SYSTEM_NAME} MATCHES "Linux")
#    ADD_DEFINITIONS(-DLinux)
#ENDIF ()

# 支持C++11标准和GNU扩展特性
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11")

# 定义so库和头文件所在目录，方面后面使用
set(FFMPEG_LIB_DIR ${CMAKE_SOURCE_DIR}/ffmpeg/jniLibs/${ANDROID_ABI})
set(FFMPEG_HEAD_DIR ${CMAKE_SOURCE_DIR}/ffmpeg/include)
set(SOX_HEAD_DIR ${CMAKE_SOURCE_DIR}/sox/include)
set(SOX_LIB_DIR ${CMAKE_SOURCE_DIR}/sox/jniLibs/${ANDROID_ABI})

# 添加头文件目录
include_directories(${FFMPEG_HEAD_DIR})
#include_directories(${SOX_HEAD_DIR})

AUX_SOURCE_DIRECTORY(${CMAKE_SOURCE_DIR}/sox/ DIR_SRCS)

# 配置目标so库编译信息
add_library( # Sets the name of the library.
        native-lib
        # Sets the library as a shared library.
        SHARED
        # Provides a relative path to your source file(s).
        native-lib.cpp
        lame/bitstream.c
        lame/encoder.c
        lame/fft.c
        lame/gain_analysis.c
        lame/id3tag.c
        lame/lame.c
        lame/mpglib_interface.c
        lame/newmdct.c
        lame/presets.c
        lame/psymodel.c
        lame/quantize_pvt.c
        lame/quantize.c
        lame/reservoir.c
        lame/set_get.c
        lame/tables.c
        lame/takehiro.c
        lame/util.c
        lame/vbrquantize.c
        lame/VbrTag.c
        lame/version.c
        audio/mp3_encode.cpp
        opengl/shader_utils.cpp
        opengl/EglCore.cpp
        opengl/GLRender.cpp
        opengl/WindowSurface.cpp
        opengl/EglSurfaceBase.cpp
        opengl/Triangle.cpp
        opengl/GlUtils.cpp
        opengl/GlShaders.cpp
        opengl/MyLooper.cpp
        opengl/Looper.cpp
        ffmpeg/video/ffpmeg_filter_manager.cpp
        ${DIR_SRCS}
        )

###添加ffmpeg相关的so库######start###
add_library(avutil
            SHARED
            IMPORTED)
set_target_properties(avutil
                      PROPERTIES IMPORTED_LOCATION
                      ${FFMPEG_LIB_DIR}/libavutil.so)
add_library(swresample
            SHARED
            IMPORTED)
set_target_properties(swresample
                      PROPERTIES IMPORTED_LOCATION
                      ${FFMPEG_LIB_DIR}/libswresample.so)
add_library(avcodec
            SHARED
            IMPORTED)
set_target_properties(avcodec
                      PROPERTIES IMPORTED_LOCATION
                      ${FFMPEG_LIB_DIR}/libavcodec.so)
add_library(avfilter
            SHARED
            IMPORTED)
set_target_properties(avfilter
                      PROPERTIES IMPORTED_LOCATION
                      ${FFMPEG_LIB_DIR}/libavfilter.so)
add_library(swscale
            SHARED
            IMPORTED)
set_target_properties(swscale
                      PROPERTIES IMPORTED_LOCATION
                      ${FFMPEG_LIB_DIR}/libswscale.so)
add_library(avformat
            SHARED
            IMPORTED)
set_target_properties(avformat
                      PROPERTIES IMPORTED_LOCATION
                      ${FFMPEG_LIB_DIR}/libavformat.so)
###添加ffmpeg相关的so库######end###

###添加sox相关的静态库######start###
add_library(sox
            STATIC
            IMPORTED)
set_target_properties(sox
                      PROPERTIES IMPORTED_LOCATION
                      ${SOX_LIB_DIR}/libsox.a)
###添加sox相关的静态库######end###

# 查找代码中使用到的系统库
find_library(
        log-lib
        log)

find_library(EGL-lib EGL)
find_library(GLESv2-lib GLESv2)

# 指定编译目标库时，cmake要链接的库
target_link_libraries( # Specifies the target library.
        native-lib
        # Links the target library to the log library
        # included in the NDK.
        OpenSLES
        # for opengl es 3.0 library
        GLESv3
        avcodec
        avfilter
        avformat
        avutil
        swresample
        swscale
        sox
        -ljnigraphics
        -landroid
        ${log-lib}
        ${EGL-lib}
        ${GLESv2-lib})