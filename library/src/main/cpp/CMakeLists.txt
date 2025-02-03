cmake_minimum_required(VERSION 3.24)
project(ttt VERSION 1.0.0)

set(CMAKE_CXX_STANDARD 23)

add_library(ttt SHARED src/Java_com_github_numq_ttt_llama_NativeLlamaTextToText.cpp)

find_package(JNI)

if (JNI_FOUND)
    message(STATUS "JNI_INCLUDE_DIRS=${JNI_INCLUDE_DIRS}")
    message(STATUS "JNI_LIBRARIES=${JNI_LIBRARIES}")
    target_include_directories(ttt PRIVATE ${JNI_INCLUDE_DIRS})
else ()
    message(FATAL_ERROR "JNI not found.")
endif ()

target_include_directories(ttt PRIVATE include include/ggml include/llama)

target_link_directories(ttt PRIVATE bin)

target_link_libraries(ttt PRIVATE ggml-base ggml-rpc ggml-cpu ggml-cuda ggml llama)