//
// Created by 姜瑜 on 2020/5/11.
//

#include "shader_utils.h"
#include <stdlib.h>

GLuint LoadShader(GLenum type, const char *shaderSource) {
    // 1. create shader
    GLuint shader = glCreateShader(type);
    if (shader == GL_NONE) {
        return GL_NONE;
    }
    // 2. load shader source
    glShaderSource(shader, 1, &shaderSource, NULL);
    // 3. compile shared source
    glCompileShader(shader);
    // 4. check compile status
    GLint compiled;
    glGetShaderiv(shader, GL_COMPILE_STATUS, &compiled);
    if (compiled == GL_FALSE) { // compile failed
        GLint len = 0;
        glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &len);
        if (len > 1) {
            char *log = static_cast<char *>(malloc(sizeof(char) * len));
            glGetShaderInfoLog(shader, len, NULL, log);
            free(log);
        }
        glDeleteShader(shader); // delete shader
        return GL_NONE;
    }
    return shader;
}

GLuint CreateProgram(const char *vertexSource, const char *fragmentSource) {
    // 1. load shader
    GLuint vertexShader = LoadShader(GL_VERTEX_SHADER, vertexSource);
    if (vertexShader == GL_NONE) {
        return GL_NONE;
    }
    GLuint fragmentShader = LoadShader(GL_FRAGMENT_SHADER, fragmentSource);
    if (fragmentShader == GL_NONE) {
        return GL_NONE;
    }
    // 2. create gl program
    GLuint program = glCreateProgram();
    if (program == GL_NONE) {
        return GL_NONE;
    }
    // 3. attach shader
    glAttachShader(program, vertexShader);
    glAttachShader(program, fragmentShader);
    // we can delete shader after attach
    glDeleteShader(vertexShader);
    glDeleteShader(fragmentShader);
    // 4. link program
    glLinkProgram(program);
    // 5. check link status
    GLint linked;
    glGetProgramiv(program, GL_LINK_STATUS, &linked);
    if (linked == GL_FALSE) { // link failed
        GLint len = 0;
        glGetProgramiv(program, GL_INFO_LOG_LENGTH, &len);
        if (len > 1) {
            char *log = static_cast<char *>(malloc(sizeof(char) * len));
            glGetProgramInfoLog(program, len, NULL, log);
            free(log);
        }
        glDeleteProgram(program); // delete program
        return GL_NONE;
    }
    return program;
}

char *readAssetFile(const char *filename, AAssetManager *mgr) {
    if (mgr == NULL) {
        return NULL;
    }
    AAsset *pAsset = AAssetManager_open(mgr, filename, AASSET_MODE_UNKNOWN);
    off_t len = AAsset_getLength(pAsset);
    char *pBuffer = (char *) malloc(len + 1);
    pBuffer[len] = '\0';
    int numByte = AAsset_read(pAsset, pBuffer, len);
    AAsset_close(pAsset);
    return pBuffer;
}