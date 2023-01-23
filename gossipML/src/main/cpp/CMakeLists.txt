cmake_minimum_required(VERSION 3.22.1)

project("superappessentia")

add_library(superappessentia
            SHARED
            essentia_jni.cpp)

add_library(essentia STATIC IMPORTED)
add_library(avformat STATIC IMPORTED)
add_library(avcodec STATIC IMPORTED)
add_library(avutil STATIC IMPORTED)
add_library(avresample STATIC IMPORTED)
add_library(samplerate STATIC IMPORTED)
add_library(mp3lame STATIC IMPORTED)
add_library(chromaprint STATIC IMPORTED)
add_library(tag STATIC IMPORTED)
add_library(yaml STATIC IMPORTED)

set_target_properties(  essentia
                        PROPERTIES IMPORTED_LOCATION
                        ${CMAKE_SOURCE_DIR}/../../../superapp-essentia/libs/${ANDROID_ABI}/libessentia.a)

set_target_properties(  avformat
                        PROPERTIES IMPORTED_LOCATION
                        ${CMAKE_SOURCE_DIR}/../../../superapp-essentia/libs/${ANDROID_ABI}/libavformat.a)

set_target_properties(  avcodec
                        PROPERTIES IMPORTED_LOCATION
                        ${CMAKE_SOURCE_DIR}/../../../superapp-essentia/libs/${ANDROID_ABI}/libavcodec.a)

set_target_properties(  avutil
                        PROPERTIES IMPORTED_LOCATION
                        ${CMAKE_SOURCE_DIR}/../../../superapp-essentia/libs/${ANDROID_ABI}/libavutil.a)

set_target_properties(  avresample
                        PROPERTIES IMPORTED_LOCATION
                        ${CMAKE_SOURCE_DIR}/../../../superapp-essentia/libs/${ANDROID_ABI}/libavresample.a)

set_target_properties(  samplerate
                        PROPERTIES IMPORTED_LOCATION
                        ${CMAKE_SOURCE_DIR}/../../../superapp-essentia/libs/${ANDROID_ABI}/libsamplerate.a)

set_target_properties(  mp3lame
                        PROPERTIES IMPORTED_LOCATION
                        ${CMAKE_SOURCE_DIR}/../../../superapp-essentia/libs/${ANDROID_ABI}/libmp3lame.a)

set_target_properties(  chromaprint
                        PROPERTIES IMPORTED_LOCATION
                        ${CMAKE_SOURCE_DIR}/../../../superapp-essentia/libs/${ANDROID_ABI}/libchromaprint.a)

set_target_properties(  tag
                        PROPERTIES IMPORTED_LOCATION
                        ${CMAKE_SOURCE_DIR}/../../../superapp-essentia/libs/${ANDROID_ABI}/libtag.a)

set_target_properties(  yaml
                        PROPERTIES IMPORTED_LOCATION
                        ${CMAKE_SOURCE_DIR}/../../../superapp-essentia/libs/${ANDROID_ABI}/libyaml.a)

include_directories(${CMAKE_SOURCE_DIR}/../../../superapp-essentia/include)
include_directories(${CMAKE_SOURCE_DIR}/../../../superapp-essentia/include/eigen3)

find_library(log-lib log)
find_library(math-lib m)
find_library(zlib z)

target_link_libraries(superappessentia essentia yaml avresample avformat avcodec avutil mp3lame
                      chromaprint tag samplerate ${log-lib} ${math-lib} ${zlib})