#include <jni.h>
#include <string>

using namespace std;

extern "C"
JNIEXPORT jstring JNICALL
Java_nl_tudelft_trustchain_hello_HelloCpp_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    char hello[60] = "Hello from C++";
    jstring message = env->NewStringUTF(hello);
    return message;
}