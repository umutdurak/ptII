#!/bin/bash
# Build OpenCV
# From: https://github.com/nebgnahz/cv-rs/blob/master/.ci/travis_build_opencv.sh
#
# .travis.yml should look like:
#
# before_install:
#  - sudo -E ./.ci/travis_build_opencv.sh
#
# cache:
#  timeout: 1000
#  directories:
#    - $HOME/usr/installed-version
#    - $HOME/usr/include
#   - $HOME/usr/lib

set -eux -o pipefail

OPENCV_VERSION=${OPENCV_VERSION:-3.4.0}
URL=https://github.com/opencv/opencv/archive/$OPENCV_VERSION.zip
URL_CONTRIB=https://github.com/opencv/opencv_contrib/archive/$OPENCV_VERSION.zip
OPENCV_BUILD=$(pwd)/opencv-$OPENCV_VERSION/build
OPENCV_CONTRIB=$(pwd)/opencv_contrib-$OPENCV_VERSION/modules
INSTALL_FLAG=$HOME/usr/installed-version/$OPENCV_VERSION
INSTALL_PREFIX=$HOME/usr

if [[ ! -e $INSTALL_FLAG ]]; then
     mkdir $HOME/logs && mkdir $HOME/cv && pushd $HOME/cv
     sudo apt-get install -y cmake pkg-config ninja-build zlib1g-dev libjpeg8-dev libtiff5-dev libopenexr-dev libavcodec-dev libavformat-dev libswscale-dev libv4l-dev libdc1394-22-dev libxine2-dev libgphoto2-dev libgtk2.0-dev libtbb-dev libeigen3-dev libblas-dev liblapack-dev liblapacke-dev libatlas-base-dev libhdf5-dev libprotobuf-dev libgflags-dev libgoogle-glog-dev

     # sudo apt-get install -y libjasper-dev libpng12-dev libgstreamer0.10-dev libgstreamer-plugins-base0.10-dev
    TMP=$(mktemp -d)
    if [[ ! -d $OPENCV_BUILD ]]; then
	OPENCV_TAR=/tmp/opencv-3.4.1.tar.gz
	if [ ! -f $OPENCV_TAR ]; then
	    wget --quiet -O $OPENCV_TAR https://github.com/opencv/opencv/archive/3.4.1.tar.gz
	fi
	OPENCV_CONTRIB_TAR=/tmp/opencv_contrib-3.4.1.tar.gz
	if [ ! -f $OPENCV_CONTRIB_TAR ]; then
	    wget --quiet -O /tmp/opencv_contrib-3.4.1.tar.gz https://github.com/opencv/opencv_contrib/archive/3.4.1.tar.gz
	fi

	tar -zxf $OPENCV_TAR
	tar -zxf $OPENCV_CONTRIB_TAR

        # curl -sL ${URL}  > ${TMP}/opencv.zip
        # unzip -q ${TMP}/opencv.zip
        # rm ${TMP}/opencv.zip

        # curl -sL ${URL_CONTRIB}  > ${TMP}/opencv_contrib.zip
        # unzip -q ${TMP}/opencv_contrib.zip
        # rm ${TMP}/opencv_contrib.zip

        mkdir $OPENCV_BUILD
    fi

    pushd $OPENCV_BUILD

    # CUDA configuration
    # cmake \
    #     -D WITH_CUDA=ON \
    #     -D BUILD_EXAMPLES=OFF \
    #     -D BUILD_TESTS=OFF \
    #     -D BUILD_PERF_TESTS=OFF  \
    #     -D BUILD_opencv_java=OFF \
    #     -D BUILD_opencv_python=OFF \
    #     -D BUILD_opencv_python2=OFF \
    #     -D BUILD_opencv_python3=OFF \
    #     -D OPENCV_EXTRA_MODULES_PATH=$OPENCV_CONTRIB \
    #     -D CMAKE_INSTALL_PREFIX=$INSTALL_PREFIX \
    #     -D CMAKE_BUILD_TYPE=Release \
    #     -D CUDA_ARCH_BIN=5.2 \
    #     -D CUDA_ARCH_PTX="" \
    #     ..


    # OpenCV Java configuration.
    cmake -Wno-dev \
      -DBUILD_DOCS:BOOL=OFF \
      -DBUILD_EXAMPLES:BOOL=OFF \
      -DBUILD_PACKAGE:BOOL=OFF \
      -DBUILD_PERF_TESTS:BOOL=OFF \
      -DBUILD_TESTS:BOOL=OFF \
      -DBUILD_WITH_DEBUG_INFO:BOOL=OFF \
      -DBUILD_ITT:BOOL=OFF \
      -DCV_TRACE:BOOL=OFF \
      -DENABLE_PYLINT:BOOL=OFF \
      -DWITH_CUDA:BOOL=OFF \
      -DWITH_CUBLAS:BOOL=OFF \
      -DWITH_CUFFT:BOOL=OFF \
      -DWITH_NVCUVID:BOOL=OFF \
      -DWITH_ITT:BOOL=OFF \
      -DWITH_MATLAB:BOOL=OFF \
      -DWITH_OPENCL:BOOL=OFF \
      -DWITH_VTK:BOOL=OFF \
      -DBUILD_opencv_apps:BOOL=OFF \
      -DBUILD_opencv_cudaarithm:BOOL=OFF \
      -DBUILD_opencv_cudabgsegm:BOOL=OFF \
      -DBUILD_opencv_cudacodec:BOOL=OFF \
      -DBUILD_opencv_cudafeatures2d:BOOL=OFF \
      -DBUILD_opencv_cudafilters:BOOL=OFF \
      -DBUILD_opencv_cudaimgproc:BOOL=OFF \
      -DBUILD_opencv_cudalegacy:BOOL=OFF \
      -DBUILD_opencv_cudaobjdetect:BOOL=OFF \
      -DBUILD_opencv_cudaoptflow:BOOL=OFF \
      -DBUILD_opencv_cudastereo:BOOL=OFF \
      -DBUILD_opencv_cudawarping:BOOL=OFF \
      -DBUILD_opencv_cudev:BOOL=OFF \
      -DBUILD_opencv_java:BOOL=ON \
      -DBUILD_opencv_js:BOOL=OFF \
      -DBUILD_opencv_python2:BOOL=OFF \
      -DBUILD_opencv_python3:BOOL=OFF \
      -DBUILD_opencv_ts:BOOL=OFF \
      -DBUILD_opencv_viz:BOOL=OFF \
      -DBUILD_opencv_world:BOOL=OFF \
      -DBUILD_opencv_contrib_world:BOOL=OFF \
      -DBUILD_opencv_matlab:BOOL=OFF \
      -DBUILD_opencv_ccalib:BOOL=OFF \
      -DBUILD_opencv_cvv:BOOL=OFF \
      -DBUILD_opencv_hdf:BOOL=OFF \
      -DBUILD_opencv_sfm:BOOL=OFF \
      -DBUILD_opencv_structured_light:BOOL=OFF \
      -DBUILD_opencv_surface_matching:BOOL=OFF \
      -DCMAKE_BUILD_TYPE:STRING=Release \
      -DCMAKE_INSTALL_PREFIX:PATH=$INSTALL_PREFIX \
      -DOPENCV_ENABLE_NONFREE:BOOL=ON \
      -DOPENCV_EXTRA_MODULES_PATH:PATH=$OPENCV_CONTRIB ..
              
    make install && sudo mkdir -p "$(dirname "$INSTALL_FLAG")" && sudo touch "$INSTALL_FLAG";
    popd
    touch $HOME/fresh-cache

fi

sudo cp -r $HOME/usr/include/* /usr/local/include/
sudo cp -r $HOME/usr/lib/* /usr/local/lib/
sudo sh -c 'echo "$INSTALL_PREFIX/lib" > /etc/ld.so.conf.d/opencv.conf'
sudo ldconfig