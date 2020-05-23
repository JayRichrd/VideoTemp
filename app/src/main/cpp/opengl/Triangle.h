//
// Created by cain on 2018/4/7.
//

#ifndef EGLNATIVERENDER_TRIANGLE_H
#define EGLNATIVERENDER_TRIANGLE_H

#include "GlUtils.h"
#include "GlShaders.h"
#include "log_util.h"

class Triangle {
public:
    Triangle();

    virtual ~Triangle();

    int init(void);

    void onDraw(int width, int height);

    void destroy();

private:
    int programHandle;
};


#endif //EGLNATIVERENDER_TRIANGLE_H
