# Tests for deep codegen
#
# @Author: Steve Neuendorffer
#
# @Version: $Id$
#
# @Copyright (c) 2000-2001 The Regents of the University of California.
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
#######################################################################

# Ptolemy II bed, see /users/cxh/ptII/doc/coding/testing.html for more information.

# Load up the test definitions.
if {[string compare test [info procs test]] == 1} then {
    source testDefs.tcl
} {}

if {[string compare test [info procs autoShallowCG]] == 1} then {
    source sootDeepCodeGeneration.tcl
} {}


if {[info procs jdkClassPathSeparator] == "" } then { 
    source [file join $PTII util testsuite jdktools.tcl]
}

# Uncomment this to get a full report, or set in your Tcl shell window.
#set VERBOSE 1

if {[info procs sootCodeGeneration] == "" } then { 
    source [file join $PTII util testsuite codegen.tcl]
}

# Uncomment this to get a full report, or set in your Tcl shell window.
# set VERBOSE 1

# Generate code for all the xml files in a directory.
proc autoDeepCG {autoDirectory} {
    foreach file [glob $autoDirectory/*.xml] {
	puts "---- testing $file"
	#set time [java::new Long [java::call System currentTimeMillis]]
	test "Auto" "Automatic test in file $file" {
	    set elapsedTime [time {sootCodeGeneration $file "Deep"}]
	    puts "soot took [expr {[lindex $elapsedTime 0] / 1000000.0}] seconds"
	    list {}
	} {{}}
	java::call System gc
	#puts "[java::call ptolemy.actor.Manager timeAndMemory [$time longValue]]"
    }
}

######################################################################
####
#

#  test Deep-1.1 {Compile and run the Orthocomm test} {
#      set result [sootDeepCodeGeneration \
#  	    ptolemy.domains.sdf.demo.OrthogonalCom.OrthogonalCom]
#      lrange $result 0 9
#  } {2 4 6 8 10 12 14 16 18 20}

# First, do an SDF test just to be sure things are working
test Deep-1.2 {Compile and run the SDF IIR test} {
    set result [sootDeepCodeGeneration \
	    [file join $relativePathToPTII ptolemy actor lib test auto \
	    IIR.xml]]
    list {}
} {{}}

#  test Deep-1.3 {Compile and run the SDF ArrayAppend test} {
#      set result [sootDeepCodeGeneration \
#  	    [file join $relativePathToPTII ptolemy actor lib test auto \
#  	    ArrayAppend.xml]]
#      list {}
#  } {{}}

# Now try to generate code for all the tests in the auto directories.
#  autoDeepCG [file join $relativePathToPTII ptolemy actor lib test auto]
#  autoDeepCG [file join $relativePathToPTII ptolemy actor lib conversions test auto]
#  #autoDeepCG [file join $relativePathToPTII ptolemy actor lib javasound test auto]
#  autoDeepCG [file join $relativePathToPTII ptolemy domains ct lib test auto]
#  autoDeepCG [file join $relativePathToPTII ptolemy domains de lib test auto]
#  autoDeepCG [file join $relativePathToPTII ptolemy domains dt kernel test auto]
#  autoDeepCG [file join $relativePathToPTII ptolemy domains fsm kernel test auto]
#  autoDeepCG [file join $relativePathToPTII ptolemy domains fsm test auto]
#  autoDeepCG [file join $relativePathToPTII ptolemy domains hdf kernel test auto]
#autoDeepCG [file join $relativePathToPTII ptolemy domains sdf kernel test auto]
#autoDeepCG [file join $relativePathToPTII ptolemy domains sdf lib test auto]
#autoDeepCG [file join $relativePathToPTII ptolemy domains sdf lib vq test auto]
#  autoDeepCG [file join $relativePathToPTII ptolemy domains sr kernel test auto]
#  autoDeepCG [file join $relativePathToPTII ptolemy domains sr lib test auto]

# Print out stats
#doneTests

