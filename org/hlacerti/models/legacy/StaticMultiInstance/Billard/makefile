# Makefile for Ptolemy II - HLA/CERTI demos f14
#
# @Authors: Gilles Lasnier, based on a file by Christopher H. Brooks
#
# @Version: $Id: makefile 66384 2013-05-21 02:06:28Z glasnier $
#
# @Copyright (c) 2013-2018 The Regents of the University of California.
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
#                       PT_COPYRIGHT_VERSION_2
#                       COPYRIGHTENDKEY

ME =        ptolemy/apps/hlacerti/demo/legacy/StaticMultiInstance/2Billes2Fed

# Root of the Java directory
ROOT =      ../../../../../..

CLASSPATH = $(ROOT)

# Get configuration info
CONFIG =    $(ROOT)/mk/ptII.mk
include $(CONFIG)

# Used to build jar files
PTPACKAGE =     Billard
PTCLASSJAR =    $(PTPACKAGE).jar

JSRCS = 

OTHER_FILES_TO_BE_JARED = \
	BillardTable.xml \
	Billard.xml \
	README.txt \
	SingleBillardBall.xml \
	Test.fed \
	TwoBilliardBalls.xml

EXTRA_SRCS = \
    $(OTHER_FILES_TO_BE_JARED) \

# Sources that may or may not be present, but if they are present, we don't
# want make checkjunk to barf on them.
MISC_FILES =

# make checkjunk will not report OPTIONAL_FILES as trash
# make realclean removes OPTIONAL_FILES
OPTIONAL_FILES =

JCLASS = $(JSRCS:%.java=%.class)

# Don't include all or install rules here, we want the user
# to run 'make demos' to run the tests.
all: jclass $(PTCLASSJAR)
	@echo "To run all the demos, run 'make demo'"


install: all

demo: demo_ptolemy demo_vergil

demo_ptolemy:
	"$(PTII)/bin/ptolemy" CoSimulationFunctional.xml
	"$(PTII)/bin/ptolemy" consumer.xml
	"$(PTII)/bin/ptolemy" producerFunct.xml
	"$(PTII)/bin/ptolemy" producerSkel.xml

demo_vergil:
	"$(PTII)/bin/vergil" CoSimulationFunctional.xml
	"$(PTII)/bin/vergil" consumer.xml
	"$(PTII)/bin/vergil" producerFunct.xml
	"$(PTII)/bin/vergil" producerSkel.xml

# Get the rest of the rules
include $(ROOT)/mk/ptcommon.mk
