# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)
# 定义变量
set(distribution_DIR ../../../../libs)


# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
             native-lib
             # Sets the library as a shared library.
             SHARED
             # Provides a relative path to your source file(s).
             src/main/cpp/native-lib.cpp
             src/main/cpp/lame/bitstream.c
             src/main/cpp/lame/encoder.c
             src/main/cpp/lame/fft.c
             src/main/cpp/lame/gain_analysis.c
             src/main/cpp/lame/id3tag.c
             src/main/cpp/lame/lame.c
             src/main/cpp/lame/mpglib_interface.c
             src/main/cpp/lame/newmdct.c
             src/main/cpp/lame/presets.c
             src/main/cpp/lame/psymodel.c
             src/main/cpp/lame/quantize_pvt.c
             src/main/cpp/lame/quantize.c
             src/main/cpp/lame/reservoir.c
             src/main/cpp/lame/set_get.c
             src/main/cpp/lame/tables.c
             src/main/cpp/lame/takehiro.c
             src/main/cpp/lame/util.c
             src/main/cpp/lame/vbrquantize.c
             src/main/cpp/lame/VbrTag.c
             src/main/cpp/lame/version.c
             src/main/cpp/audio/mp3_encode.cpp)

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

# 添加库——外部引入的库
# 库名称：avcodec（不需要包含前缀lib）
# 库类型：SHARED，表示动态库，后缀为.so（如果是STATIC，则表示静态库，后缀为.a）
# IMPORTED表明是外部引入的库
add_library(avcodec
        SHARED
        IMPORTED)
# 设置目标属性
# 设置avcodec目标库的IMPORTED_LOCATION属性，用于说明引入库的位置
# 还可以设置其他属性，格式：PROPERTIES key value
set_target_properties(avcodec
        PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/${ANDROID_ABI}/libavcodec.so)


find_library(
        log-lib
        log)
add_library(avfilter
        SHARED
        IMPORTED)
set_target_properties(avfilter
        PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/${ANDROID_ABI}/libavfilter.so)

add_library(avformat
        SHARED
        IMPORTED)
set_target_properties(avformat
        PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/${ANDROID_ABI}/libavformat.so)

add_library(avutil
        SHARED
        IMPORTED)
set_target_properties(avutil
        PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/${ANDROID_ABI}/libavutil.so)

add_library(swresample
        SHARED
        IMPORTED)
set_target_properties(swresample
        PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/${ANDROID_ABI}/libswresample.so)

add_library(swscale
        SHARED
        IMPORTED)
set_target_properties(swscale
        PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/${ANDROID_ABI}/libswscale.so)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.
# 引入头文件
include_directories(libs/include)
target_link_libraries( # Specifies the target library.
                       native-lib
                       # Links the target library to the log library
                       # included in the NDK.
                       OpenSLES
                       avcodec
                       avfilter
                       avformat
                       avutil
                       swresample
                       swscale
                       -landroid
                       ${log-lib})