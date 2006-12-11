/* Base class for VHDL code generator helper.

 Copyright (c) 2005-2006 The Regents of the University of California.
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

 */
package ptolemy.codegen.vhdl.kernel;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import ptolemy.actor.Actor;
import ptolemy.actor.IOPort;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.lib.vhdl.FixTransformer;
import ptolemy.codegen.kernel.CodeGeneratorHelper;
import ptolemy.data.BooleanToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.Entity;
import ptolemy.kernel.Port;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.math.Precision;

//////////////////////////////////////////////////////////////////////////
//// VHDLCodeGeneratorHelper

/**
 Base class for VHDL code generator helper. 

 <p>Actor helpers extend this class and optionally define
 generateFireCode(), generateInitializeCode(), generatePreinitializeCode(),
 and generateWrapupCode() methods.  In derived classes, these methods,
 if present, make actor specific changes to the corresponding code.
 If these methods are not present, then the parent class will automatically
 read the corresponding .vhdl file and subsitute in the corresponding code
 block.  For example, generateInitializeCode() reads the
 <code>initBlock</code>, processes the macros and adds the resulting
 code block to the output.

 <p>For a complete list of methods to define, see 
 {@link ptolemy.codegen.kernel.CodeGeneratorHelper}.

 <p>For further details, see <code>$PTII/ptolemy/codegen/README.html</code>

 @author Christopher Brooks, Edward Lee, Jackie Leung, Gang Zhou, Ye Zhou
 @version $Id$
 @since Ptolemy II 6.0
 @Pt.ProposedRating Red (mankit)
 @Pt.AcceptedRating Red (mankit)
 */
public class VHDLCodeGeneratorHelper extends CodeGeneratorHelper {
    /**
     * Create a new instance of the C code generator helper.
     * @param component The actor object for this helper.
     */
    public VHDLCodeGeneratorHelper(NamedObj component) {
        super(component, component.getName() + " VHDL");
        _parseTreeCodeGenerator = new VHDLParseTreeCodeGenerator();
    }

    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

    /**
     * Generate the fire code. In this base class, add the name of the
     * associated component in the comment. Subclasses may extend this
     * method to generate the fire code of the associated component.
     * @return The generated code.
     * @exception IllegalActionException Not thrown in this base class.
     */
    public String generateFireCode() throws IllegalActionException {
        String code = super.generateFireCode();

        // check the latency of the actor and register the output.
        //_codeStream.append("??? Latency code ???\n");
        return processCode(code);
    }

    /** Return the reference to the specified parameter or port of the
     *  associated actor. For a parameter, the returned string is in
     *  the form "fullName_parameterName". For a port, the returned string
     *  is in the form "fullName_portName[channelNumber][offset]", if
     *  any channel number or offset is given.
     *  @param name The name of the parameter or port
     *  @exception IllegalActionException If the parameter or port does not
     *   exist or does not have a value.
     */
    public String getReference(String name) throws IllegalActionException {
        name = processCode(name);

        StringBuffer result = new StringBuffer();
        StringTokenizer tokenizer = new StringTokenizer(name, "#,", true);

        if ((tokenizer.countTokens() != 1) && (tokenizer.countTokens() != 3)
                && (tokenizer.countTokens() != 5)) {
            throw new IllegalActionException(getComponent(),
                    "Reference not found: " + name);
        }

        // Get the referenced name.
        String refName = tokenizer.nextToken().trim();

        boolean forComposite = false;

        // Usually given the name of an input port, getReference(String name) 
        // returns variable name representing the input port. Given the name 
        // of an output port, getReference(String name) returns variable names
        // representing the input ports connected to the output port. 
        // However, if the name of an input port starts with '@', 
        // getReference(String name) returns variable names representing the 
        // input ports connected to the given input port on the inside. 
        // If the name of an output port starts with '@', 
        // getReference(String name) returns variable name representing the 
        // the given output port which has inside receivers.
        // The special use of '@' is for composite actor when
        // tokens are transferred into or out of the composite actor.
        if (refName.charAt(0) == '@') {
            forComposite = true;
            refName = refName.substring(1);
        }

        TypedIOPort port = getPort(refName);

        String[] channelAndOffset = _getChannelAndOffset(name);

        if (port != null) {

            int channelNumber = 0;
            
            if (!channelAndOffset[0].equals("")) {
                channelNumber = (new Integer(channelAndOffset[0])).intValue();
            }

            // To support modal model, we need to check the following condition
            // first because an output port of a modal controller should be
            // mainly treated as an output port. However, during choice action,
            // an output port of a modal controller will receive the tokens sent
            // from the same port.  During commit action, an output port of a modal
            // controller will NOT receive the tokens sent from the same port.
            if ((port.isOutput() && !forComposite)
                    || (port.isInput() && forComposite)) {

                result.append(generateVariableName(port));
                result.append("_" + channelNumber);
                return result.toString();
            }

            // Note that if the width is 0, then we have no connection to
            // the port but the port might be a PortParameter, in which
            // case we want the Parameter.
            // codegen/c/actor/lib/string/test/auto/StringCompare3.xml
            // tests this.

            if ((port.isInput() && !forComposite && port.getWidth() > 0)
                    || (port.isOutput() && forComposite)) {

                Channel sourceChannel = getSourceChannel(port, channelNumber);

                result.append(generateVariableName(sourceChannel.port));
                result.append("_" + sourceChannel.channelNumber);
                return result.toString();                
            }
        }

        throw new IllegalActionException(getComponent(), 
                "Reference not found: " + name);
    }    

    /**
     * 
     * @return
     * @throws IllegalActionException 
     */
    public boolean isSynthesizeable() throws IllegalActionException {
        return isSynthesizeable;
    }
    
    ///////////////////////////////////////////////////////////////////
    ////                     protected methods.                    ////
    
    /**
     * 
     * @param port
     * @return
     */
    protected String _generateVHDLType(TypedIOPort port) {
        StringBuffer code = new StringBuffer();
        if (port.getType() == BaseType.FIX) {
            int bits = new Precision(
                    _getPortPrecision(port)).getNumberOfBits() - 1;
            
            if (bits == 0) {
                code.append("std_logic");                
            } else {
                code.append("std_logic_vector (" + bits + " DOWNTO 0)");
            }
            
        } else if (port.getType() == BaseType.BOOLEAN) {
            
            code.append("std_logic");
            
        } else {
            
            code.append("UNKNOWN TYPE");
            
        }
        return code.toString();
    }

    /**
     * 
     * @param port
     * @return
     */
    protected String _getPortPrecision(Port port) {
        Parameter precision = (Parameter) 
        ((Entity) port.getContainer())
        .getAttribute(port.getName() + "Precision");  
        
        return precision.getExpression();
    }

    /**
     * 
     * @return
     */
    protected String _getPortDeclarations() {
        StringBuffer code = new StringBuffer();
        Actor actor = (Actor) getComponent();
        Iterator inputPorts = actor.inputPortList().iterator();
        while (inputPorts.hasNext()) {
            TypedIOPort port = (TypedIOPort) inputPorts.next();
            code.append(port.getName() + " : IN ");
            code.append(_generateVHDLType(port));
            if (inputPorts.hasNext()) {
                code.append(";\n");
            }
        }
        
        Iterator outputPorts = actor.outputPortList().iterator();
        while (outputPorts.hasNext()) {
            code.append(";\n");
            TypedIOPort port = (TypedIOPort) outputPorts.next();
            code.append(port.getName() + " : OUT ");
            code.append(_generateVHDLType(port));
        }
        // FIXME: we are not handling inout ports.
        
        return code.toString();
    }

    /**
     * @param port
     * @return
     * @throws IllegalActionException
     */
    protected Precision _getSourcePortPrecision(IOPort port) throws IllegalActionException {
        IOPort sourcePortA = getSourceChannel(port, 0).port;

        FixTransformer sourceHelper = 
        ((FixTransformer) sourcePortA.getContainer());
        
        Precision precision = 
            new Precision(sourceHelper.getPortPrecision(sourcePortA));

        return precision;
    }        
    
    ///////////////////////////////////////////////////////////////////
    ////                         protected variables               ////

    /**
     * Indicate whether this actor can be synthesized to hardware.
     * If false, its VHDL code goes into the testbench file (tb.vhdl).
     * Otherwise, it goes to the synthesizeable code file (model.vhdl).
     * The default value is true.
     */
    protected boolean isSynthesizeable = true;
}
