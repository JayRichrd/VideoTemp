//
// Created by cainjiang on 2020/4/20.
//

#include "mp3_encode.h"

extern "C"

Mp3Encoder::Mp3Encoder() {

}

Mp3Encoder::~Mp3Encoder() {

}

int Mp3Encoder::init_encoder(const char *pcm_file_path, const char *mp3_file_path, int sample_rate, int channels, int bit_rate) {
    int ret = 0;
    pcm_file = fopen(pcm_file_path, "rb");
    if (pcm_file) {
        mp3_file = fopen(mp3_file_path, "wb");
        if (mp3_file) {
            //初始化lame相关参数，输入/输出采样率、音频声道数、码率
            lame_client = lame_init();
            lame_set_in_samplerate(lame_client, sample_rate);
            lame_set_out_samplerate(lame_client, sample_rate);
            lame_set_num_channels(lame_client, channels);
            lame_set_brate(lame_client, bit_rate);
            lame_init_params(lame_client);
            ret = 1;
        }
    }
    return ret;
}

void Mp3Encoder::encode_data() {
    int bufferSize = 1024 * 256;
    short *buffer = new short[bufferSize / 2];
    short *leftChannelBuffer = new short[bufferSize / 4];//左声道
    short *rightChannelBuffer = new short[bufferSize / 4];//右声道
    unsigned char *mp3_buffer = new unsigned char[bufferSize];
    size_t readBufferSize = 0;
    while ((readBufferSize = fread(buffer, 2, bufferSize / 2, pcm_file)) > 0) {
        for (int i = 0; i < readBufferSize; i++) {
            if (i % 2 == 0) {
                leftChannelBuffer[i / 2] = buffer[i];
            } else {
                rightChannelBuffer[i / 2] = buffer[i];
            }
        }
        size_t writeSize = static_cast<size_t>(lame_encode_buffer(
                lame_client,
                leftChannelBuffer,
                rightChannelBuffer,
                (int) (readBufferSize / 2),
                mp3_buffer,
                bufferSize));
        fwrite(mp3_buffer, 1, writeSize, mp3_file);
    }
    delete[] buffer;
    delete[] leftChannelBuffer;
    delete[] rightChannelBuffer;
    delete[] mp3_buffer;
}

void Mp3Encoder::destory() {
    if (pcm_file) {
        fclose(pcm_file);
    }
    if (mp3_file) {
        fclose(mp3_file);
        lame_close(lame_client);
    }
}


