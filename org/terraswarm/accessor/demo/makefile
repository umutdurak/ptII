# Makefile for TerraSwarm demos
#
# @Version: $Id$
# @Author: Christopher Brooks (makefile only)
#
# @Copyright (c) 2015-2019 The Regents of the University of California.
# All rights reserved.
#
# Permission is hereby granted, without written agreement and without
# license or royalty fees, to use, copy, modify, and distribute this
# software and its documentation for any purpose, provided that the
# above copyright notice and the following two paragraphs appear in all
# copies of this software.
#
# IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
# FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
# ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
# THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
# SUCH DAMAGE.
#
# THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
# INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
# MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
# PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
# CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
# ENHANCEMENTS, OR MODIFICATIONS.
#
# 						PT_COPYRIGHT_VERSION_2
# 						COPYRIGHTENDKEY
##########################################################################

# Root of Ptolemy II directory
ROOT = ../../../..

DIRS = \
	AugmentedReality \
	AugmentedRealityVideoSOHO \
	AugmentedRealityVideoTerra \
	ConnectedCar \
	DynamicTopologyUpdate \
	Hue \
	IndoorLocationAwareDiscovery \
	SemanticRepository \
	Lifx \
	NaturalLanguage \
	Scarab \
	WatchHandRaisingLight \

# $(ME) is used in ptdir.mk
ME =  org/terraswarm/accessor/demo

CLASSPATH =	$(ROOT)

# True source directory
VPATH =	$(ROOT)/$(ME)

# Get configuration info
CONFIG = $(ROOT)/mk/ptII.mk
include $(CONFIG)

# Used to build jar files
PTPACKAGE = demo
#PTCLASSJAR = $(PTPACKAGE).jar
PTCLASSALLJAR = $(PTPACKAGE).jar

# Include the .class files from these jars in PTCLASSALLJAR
PTCLASSALLJARS = \
	AugmentedReality/AugmentedReality.jar \
	AugmentedRealityVideoSOHO/AugmentedRealityVideoSOHO.jar \
	AugmentedRealityVideoTerra/AugmentedRealityVideoTerra.jar \
	ConnectedCar/ConnectedCar.jar \
	DynamicTopologyUpdate/DynamicTopologyUpdate.jar \
	Hue/Hue.jar \
	IndoorLocationAwareDiscovery/IndoorLocationAwareDiscovery.jar \
	SemanticRepository/SemanticRepository.jar \
	Lifx/Lifx.jar \
	NaturalLanguage/NaturalLanguage.jar \
	Scarab/Scarab.jar \
	WatchHandRaisingLight/WatchHandRaisingLight.jar

JSRCS = 

OTHER_FILES_TO_BE_JARED = 

EXTRA_SRCS = $(JSRCS) $(OTHER_FILES_TO_BE_JARED)

# Sources that may or may not be present, but if they are present, we don't
# want make checkjunk to barf on them.
# Don't include demo or DIRS here, or else 'make sources' will run 'make demo'
MISC_FILES =

# make checkjunk will not report OPTIONAL_FILES as trash
# make distclean removes OPTIONAL_FILES
OPTIONAL_FILES = $(DIRS)

JCLASS = $(JSRCS:%.java=%.class)

all: jclass
install: jclass jars

# Get the rest of the rules
include $(ROOT)/mk/ptcommon.mk
