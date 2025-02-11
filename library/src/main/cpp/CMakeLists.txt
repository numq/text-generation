cmake_minimum_required(VERSION 3.24)
project(text-generation VERSION 1.0.0)

set(CMAKE_CXX_STANDARD 23)

set(CMAKE_SHARED_LIBRARY_PREFIX "")

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

target_link_directories(text-generation PRIVATE bin)

target_link_libraries(text-generation PRIVATE ggml-base ggml-cpu ggml-cuda ggml-rpc ggml llama)