/* An actor that writes input tokens as expressiong to the specified file.

@Copyright (c) 1998-2003 The Regents of the University of California.
All rights reserved.

Permission is hereby granted, without written agreement and without
license or royalty fees, to use, copy, modify, and distribute this
software and its documentation for any purpose, provided that the
above copyright notice and the following two paragraphs appear in all
copies of this software.

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

						PT_COPYRIGHT_VERSION 2
						COPYRIGHTENDKEY
@ProposedRating Green (eal@eecs.berkeley.edu)
@AcceptedRating Yellow (cxh@eecs.berkeley.edu)
*/

package ptolemy.actor.lib.io;

import ptolemy.data.Token;
import ptolemy.kernel.attributes.FileAttribute; // For javadoc
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.*;

import java.io.IOException;

//////////////////////////////////////////////////////////////////////////
//// ExpressionWriter
/**
This actor reads input tokens and writes them, one line at a time,
to a specified file.  The format it uses is that generated by the
toString() method of the token, which for most tokens is an expression
that can be read back in using ExpressionReader.  String tokens, for
example, include the enclosing quotation marks in the output.
If you do not want the enclosing quotation marks, use LineWriter.
<p>
The file is specified  by the <i>fileName</i> attribute
using any form acceptable to FileAttribute.
<p>
If the <i>append</i> attribute has value <i>true</i>,
then the file will be appended to. If it has value <i>false</i>,
then if the file exists, the user will be queried for permission
to overwrite, and if granted, the file will be overwritten.
<p>
If the <i>confirmOverwrite</i> parameter has value <i>false</i>,
then this actor will overwrite the specified file if it exists
without asking.  If <i>true</i> (the default), then if the file
exists, then this actor will ask for confirmation before overwriting.

@see ExpressionReader
@see ptolemy.kernel.attribute.FileAttribute
@see LineWriter

@author Edward A. Lee
@version $Id$
@since Ptolemy II 0.4
*/
public class ExpressionWriter extends LineWriter {

    /** Construct an actor with the given container and name.
     *  @param container The container.
     *  @param name The name of this actor.
     *  @exception IllegalActionException If the actor cannot be contained
     *   by the proposed container.
     *  @exception NameDuplicationException If the container already has an
     *   actor with this name.
     */
    public ExpressionWriter(CompositeEntity container, String name)
            throws IllegalActionException, NameDuplicationException {
        super(container, name);
    }

    ///////////////////////////////////////////////////////////////////
    ////                         protected methods                 ////

    /** Write the specified token to the current writer using the toString()
     *  method of the token, which usually results in an expression that
     *  can be read back in using the expression parser.
     *  @param token The token to write.
     */
    protected void _writeToken(Token token) {
        _writer.println(token.toString());
    }
}
