# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

# Set code endianness to little endian; most (all?) Android platforms are little endian
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -D__NBISLE__")

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
             wsq

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             src/main/cpp/nbis/imgtools/src/lib/wsq/cropcoeff.c
             src/main/cpp/nbis/imgtools/src/lib/wsq/decoder.c
             src/main/cpp/nbis/imgtools/src/lib/wsq/encoder.c
             src/main/cpp/nbis/imgtools/src/lib/wsq/globals.c
             src/main/cpp/nbis/imgtools/src/lib/wsq/huff.c
             src/main/cpp/nbis/imgtools/src/lib/wsq/ppi.c
             src/main/cpp/nbis/imgtools/src/lib/wsq/sd14util.c
             src/main/cpp/nbis/imgtools/src/lib/wsq/tableio.c
             src/main/cpp/nbis/imgtools/src/lib/wsq/tree.c
             src/main/cpp/nbis/imgtools/src/lib/wsq/util.c
             src/main/cpp/nbis/imgtools/src/lib/jpegl/decoder.c
             src/main/cpp/nbis/imgtools/src/lib/jpegl/encoder.c
             src/main/cpp/nbis/imgtools/src/lib/jpegl/huff.c
             src/main/cpp/nbis/imgtools/src/lib/jpegl/huftable.c
             src/main/cpp/nbis/imgtools/src/lib/jpegl/imgdat.c
             src/main/cpp/nbis/imgtools/src/lib/jpegl/ppi.c
             src/main/cpp/nbis/imgtools/src/lib/jpegl/sd4util.c
             src/main/cpp/nbis/imgtools/src/lib/jpegl/tableio.c
             src/main/cpp/nbis/imgtools/src/lib/jpegl/util.c
             src/main/cpp/nbis/imgtools/src/lib/image/img_io.c
             src/main/cpp/nbis/imgtools/src/lib/image/readihdr.c
             src/main/cpp/nbis/imgtools/src/lib/image/imgutil.c
             src/main/cpp/nbis/imgtools/src/lib/image/rl.c
             src/main/cpp/nbis/imgtools/src/lib/image/grp4deco.c
             src/main/cpp/nbis/imgtools/src/lib/image/imageops.c
             src/main/cpp/nbis/imgtools/src/lib/ihead/getcomp.c
             src/main/cpp/nbis/imgtools/src/lib/ihead/getnset.c
             src/main/cpp/nbis/imgtools/src/lib/ihead/nullihdr.c
             src/main/cpp/nbis/imgtools/src/lib/ihead/parsihdr.c
             src/main/cpp/nbis/imgtools/src/lib/ihead/prntihdr.c
             src/main/cpp/nbis/imgtools/src/lib/ihead/readihdr.c
             src/main/cpp/nbis/imgtools/src/lib/ihead/valdcomp.c
             src/main/cpp/nbis/imgtools/src/lib/ihead/writihdr.c
             src/main/cpp/nbis/commonnbis/src/lib/ioutil/dataio.c
             src/main/cpp/nbis/commonnbis/src/lib/ioutil/fileexst.c
             src/main/cpp/nbis/commonnbis/src/lib/ioutil/filehead.c
             src/main/cpp/nbis/commonnbis/src/lib/ioutil/fileroot.c
             src/main/cpp/nbis/commonnbis/src/lib/ioutil/filesize.c
             src/main/cpp/nbis/commonnbis/src/lib/ioutil/filetail.c
             src/main/cpp/nbis/commonnbis/src/lib/ioutil/findfile.c
             src/main/cpp/nbis/commonnbis/src/lib/ioutil/newext.c
             src/main/cpp/nbis/commonnbis/src/lib/ioutil/readutil.c
             src/main/cpp/nbis/commonnbis/src/lib/util/bres.c
             src/main/cpp/nbis/commonnbis/src/lib/util/bubble.c
             src/main/cpp/nbis/commonnbis/src/lib/util/computil.c
             src/main/cpp/nbis/commonnbis/src/lib/util/fatalerr.c
             src/main/cpp/nbis/commonnbis/src/lib/util/invbyte.c
             src/main/cpp/nbis/commonnbis/src/lib/util/invbytes.c
             src/main/cpp/nbis/commonnbis/src/lib/util/memalloc.c
             src/main/cpp/nbis/commonnbis/src/lib/util/ssxstats.c
             src/main/cpp/nbis/commonnbis/src/lib/util/syserr.c
             src/main/cpp/nbis/commonnbis/src/lib/util/ticks.c
             src/main/cpp/nbis/commonnbis/src/lib/util/time.c
             src/main/cpp/nbis/commonnbis/src/lib/fet/allocfet.c
             src/main/cpp/nbis/commonnbis/src/lib/fet/delfet.c
             src/main/cpp/nbis/commonnbis/src/lib/fet/extrfet.c
             src/main/cpp/nbis/commonnbis/src/lib/fet/freefet.c
             src/main/cpp/nbis/commonnbis/src/lib/fet/lkupfet.c
             src/main/cpp/nbis/commonnbis/src/lib/fet/nistcom.c
             src/main/cpp/nbis/commonnbis/src/lib/fet/printfet.c
             src/main/cpp/nbis/commonnbis/src/lib/fet/readfet.c
             src/main/cpp/nbis/commonnbis/src/lib/fet/strfet.c
             src/main/cpp/nbis/commonnbis/src/lib/fet/updatfet.c
             src/main/cpp/nbis/commonnbis/src/lib/fet/writefet.c
             src/main/cpp/wsq.cpp
             )

target_include_directories(wsq PRIVATE src/main/cpp/nbis/imgtools/include src/main/cpp/nbis/commonnbis/include src/main/cpp/nbis/commonnbis/include/mlp )

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
                       wsq

                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib} )