#!/bin/sh
#
# SoX Regression Test script: Lossless file conversion

#NDK路径，这里需要替换成你自己的NDK路径
NDK_BASE=/Users/cainjiang/SDK/android-ndk-r21b
API=28
NDK_SYSROOT=$NDK_BASE/platforms/android-28/arch-arm
NDK_TOOLCHAIN_BASE=$NDK_BASE/toolchains/aarch64-linux-android-4.9/prebuilt/darwin-x86_64
CC=$NDK_TOOLCHAIN_BASE/bin/armv7a-linux-android$API-clang
LD=$NDK_TOOLCHAIN_BASE/bin/armv7a-linux-androideabi-ld
CWD=`pwd`
PROJECT_ROOT=$CWD
./configure \
#编译产物输出路径
--prefix=$(pwd)/android_lib \
--target=armv7a \
--host=arm-linux-androideabi \
--with-sysroot=$NDK_SYSROOT \
--enable-static \
--disable-shared \
--disable-openmp \
--without-libltdl
CFLAGS='-O2' \
CC=$CC \
LD=$LD \
