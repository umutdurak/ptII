# Tests for the RcvrTimeTriple class
#
# @Author: John S. Davis II
#
# @Version: @(#)RcvrTimeTriple.tcl	1.1	11/17/98
#
# @Copyright (c) 1997-1999 The Regents of the University of California.
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

# Tycho test bed, see $TYCHO/doc/coding/testing.html for more information.

# Load up the test definitions.
if {[string compare test [info procs test]] == 1} then {
    source testDefs.tcl
} {}

# Uncomment this to get a full report, or set in your Tcl shell window.
# set VERBOSE 1

# If a file contains non-graphical tests, then it should be named .tcl
# If a file contains graphical tests, then it should be called .itcl
#
# It would be nice if the tests would work in a vanilla itkwish binary.
# Check for necessary classes and adjust the auto_path accordingly.
#

######################################################################
####
#
test RcvrTimeTriple-2.1 {Check for correct receiver} {
    set iop [java::new ptolemy.actor.IOPort]
    set tqr [java::new ptolemy.domains.od.kernel.TimedQueueReceiver $iop]
    set rtt [java::new ptolemy.domains.od.kernel.RcvrTimeTriple $tqr 0.0 0]
    list [expr { $tqr == [$rtt getReceiver] } ]
} {1}

######################################################################
####
#
test RcvrTimeTriple-2.2 {Check for correct time} {
    set iop [java::new ptolemy.actor.IOPort]
    set tqr [java::new ptolemy.domains.od.kernel.TimedQueueReceiver $iop]
    set rtt [java::new ptolemy.domains.od.kernel.RcvrTimeTriple $tqr 30.0 0]
    list [expr { 30.0 == [$rtt getTime] } ]
} {1}

######################################################################
####
#
test RcvrTimeTriple-2.3 {Check for correct priority} {
    set iop [java::new ptolemy.actor.IOPort]
    set tqr [java::new ptolemy.domains.od.kernel.TimedQueueReceiver $iop]
    set rtt [java::new ptolemy.domains.od.kernel.RcvrTimeTriple $tqr 30.0 8]
    list [expr { 8 == [$rtt getPriority] } ]
} {1}









