cmake_minimum_required(VERSION 3.24)
project(text-generation VERSION 1.0.0)

set(CMAKE_CXX_STANDARD 23)

set(CMAKE_SHARED_LIBRARY_PREFIX "")

option(BUILD_WITH_CUDA "Build with CUDA support" OFF)

add_library(text-generation SHARED src/Java_com_github_numq_textgeneration_llama_NativeLlamaTextGeneration.cpp)

find_package(JNI)

if (JNI_FOUND)
    message(STATUS "JNI_INCLUDE_DIRS=${JNI_INCLUDE_DIRS}")
    message(STATUS "JNI_LIBRARIES=${JNI_LIBRARIES}")
    target_include_directories(text-generation PRIVATE ${JNI_INCLUDE_DIRS})
else ()
    message(FATAL_ERROR "JNI not found.")
endif ()

target_include_directories(text-generation PRIVATE include include/ggml include/llama)

if (BUILD_WITH_CUDA)
    target_link_directories(text-generation PRIVATE bin/cuda)

    target_link_libraries(text-generation PRIVATE ggml-base ggml-cpu ggml-cuda ggml-rpc ggml llama)
else ()
    target_link_directories(text-generation PRIVATE bin/cpu)

    target_link_libraries(text-generation PRIVATE ggml-base ggml-cpu ggml-rpc ggml llama)
endif ()