//
// Created by 姜瑜 on 2020/5/11.
//
// openGL
#include <GLES3/gl3.h>
#include <android/asset_manager_jni.h>

#ifndef VIDEOTEMP_SHADER_UTILS_H
#define VIDEOTEMP_SHADER_UTILS_H

GLuint LoadShader(GLenum type, const char *shaderSource);
GLuint CreateProgram(const char* vertexSource, const char* fragmentSource);
char *readAssetFile(const char *filename, AAssetManager *mgr);

#endif //VIDEOTEMP_SHADER_UTILS_H
