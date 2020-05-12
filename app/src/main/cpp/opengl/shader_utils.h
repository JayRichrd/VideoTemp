//
// Created by 姜瑜 on 2020/5/11.
//
// openGL
#include <GLES3/gl3.h>
#include <android/asset_manager_jni.h>

#ifndef VIDEOTEMP_SHADER_UTILS_H
#define VIDEOTEMP_SHADER_UTILS_H

GLuint load_shader(GLenum type, const char *shader_source);
GLuint create_program(const char* vertex_source, const char* fragment_source);
char *read_asset_file(const char *file_name, AAssetManager *am);

#endif //VIDEOTEMP_SHADER_UTILS_H
