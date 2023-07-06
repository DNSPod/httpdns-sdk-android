cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++14")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fdata-sections")

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
        httpdns

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        src/main/cpp/stack/NetworkStack.cpp
        src/main/cpp/Jni.cpp
        src/main/cpp/JniHelper.cpp
        src/main/cpp/adapter/HttpDnsBridge.cpp
        )

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# buildInetRequest script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
        httpdns
        # Links the target library to the log library
        # included in the NDK.
        ${log-lib})