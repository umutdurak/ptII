/* A SchematicPort represents a PtII design

 Copyright (c) 1998-1999 The Regents of the University of California.
 All rights reserved.
 Permission is hereby granted, without written agreement and without
 license or royalty fees, to use, copy, modify, and distribute this
 software and its documentation for any purpose, provided that the above
 copyright notice and the following two paragraphs appear in all copies
 of this software.

 IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
 FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
 THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
 SUCH DAMAGE.

 THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
 PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
 CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
 ENHANCEMENTS, OR MODIFICATIONS.

                                        PT_COPYRIGHT_VERSION_2
                                        COPYRIGHTENDKEY

@ProposedRating Red (eal@eecs.berkeley.edu)
@AcceptedRating Red (johnr@eecs.berkeley.edu)
*/

package ptolemy.schematic.util;

import java.util.Enumeration;

////////////////////////////////////////////////////////////////////////////// SchematicPort
/**

A schematic port represents a port of an entity in a PtolemyII
schematic. Currently, it is not clear exactly how much
information is in this object...
<!-- port elements will be parsed into class SchematicPort -->
<!ELEMENT port EMPTY>
<!ATTLIST port
name ID #REQUIRED
input (true|false) "false"
output (true|false) "false"
multiport (true|false) "false"
type (string|double|doubleArray) #REQUIRED>

@author Steve Neuendorffer, John Reekie
@version $Id$
*/
public class SchematicPort extends PTMLObject {

    /**
     * Create a new SchematicPort object, with no attributes.
     */
    public SchematicPort () {
        this("port");
     }

    /**
     * Create a new SchematicPort object, with the specified attributes.
     * @param attributes a HashedMap from a String specifying the name of
     * an attribute to a String specifying the attribute's value.
     */
    public SchematicPort (String name) {
        super(name);
	setInput(false);
        setOutput(false);
        setMultiport(false);
        setType("undeclared");
    }

    /**
     * Return a string that represents the type of this port.  If the type
     * is not specified and should be determined at runtime, then
     * the type returned will be "undeclared".
     */
    public String getType() {
        return _type;
    }

    /**
     * Return true if and only if the port is an input port.
     */
    public boolean isInput() {
        return _input;
    }

    /**
     * Return true if and only if the port is a multiport.
     */
    public boolean isMultiport() {
        return _multi;
    }

    /**
     * Return true if and only if the port is an output port.
     */
    public boolean isOutput() {
        return _output;
    }

    /**
     * Set whether or not this port is an input port.
     */
    public void setInput(boolean flag) {
	_input = flag;
    }

    /**
     * Set whether or not this port is a multiport.
     */
    public void setMultiport(boolean flag) {
	_multi = flag;
    }

    /**
     * Set whether or not this port is an output port.
     */
    public void setOutput(boolean flag) {
	_output = flag;
    }

    /**
     * Set the type of this port.   If the type is to be dynamically
     * determined, then set the type to "undeclared".
     *
     * @param a String representing the type of this port.
     */
    public void setType(String type) {
	_type = type;
    }

    private boolean _input;
    private boolean _multi;
    private boolean _output;
    private String _type;
}

