//
// Created by cainjiang on 2020/4/20.
//
#include <stdio.h>
#include "../lame/lame.h"
#include "../../../../../../../../SDK/ndk-bundle/toolchains/llvm/prebuilt/darwin-x86_64/sysroot/usr/include/c++/v1/cstdio"

#ifndef VIDEOTEMP_MP3_ENCODE_H
#define VIDEOTEMP_MP3_ENCODE_H
#ifdef __cplusplus
extern "C" {
#endif

class Mp3Encoder {
private:
    FILE *pcm_file;
    FILE *mp3_file;
    lame_t lame_client;

public:
    Mp3Encoder();

    ~Mp3Encoder();

    int init_encoder(const char *pcm_file_path, const char *mp3_file_path, int sample_rate, int channels, int bit_rate);

    void encode_data();

    void destory();
};

#ifdef __cplusplus
}
#endif
#endif

