/* Code generator adapter class associated with the StaticSchedulingDirector class.

 Copyright (c) 2005-2013 The Regents of the University of California.
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
package ptolemy.cg.adapter.generic.program.procedural.adapters.ptolemy.actor.sched;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import ptolemy.actor.Actor;
import ptolemy.actor.CompositeActor;
import ptolemy.actor.IOPort;
import ptolemy.actor.Receiver;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.parameters.ParameterPort;
import ptolemy.actor.sched.Firing;
import ptolemy.actor.sched.Schedule;
import ptolemy.actor.util.DFUtilities;
import ptolemy.cg.adapter.generic.adapters.ptolemy.actor.Director;
import ptolemy.cg.adapter.generic.adapters.ptolemy.actor.PortDirector;
import ptolemy.cg.adapter.generic.adapters.ptolemy.actor.PortInfo;
import ptolemy.cg.adapter.generic.adapters.ptolemy.actor.Ports;
import ptolemy.cg.kernel.generic.CodeGeneratorAdapter;
import ptolemy.cg.kernel.generic.GenericCodeGenerator;
import ptolemy.cg.kernel.generic.program.CodeStream;
import ptolemy.cg.kernel.generic.program.NamedProgramCodeGeneratorAdapter;
import ptolemy.cg.kernel.generic.program.ProgramCodeGenerator;
import ptolemy.cg.kernel.generic.program.ProgramCodeGeneratorAdapter;
import ptolemy.cg.kernel.generic.program.TemplateParser;
import ptolemy.cg.kernel.generic.program.procedural.c.CCodeGenerator;
import ptolemy.data.BooleanToken;
import ptolemy.data.DoubleToken;
import ptolemy.data.IntToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.Variable;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.DecoratorAttributes;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.InternalErrorException;
import ptolemy.kernel.util.KernelException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.util.StringUtilities;

///////////////////////////////////////////////////////////////////
//// StaticSchedulingDirector

/**
 Code generator adapter associated with the StaticSchedulingDirector class.
 This class is also associated with a code generator.

 @author Gang Zhou, Contributor: Bert Rodiers, Christopher Brooks
 @version $Id$
 @since Ptolemy II 10.0
 @Pt.ProposedRating Yellow (zgang)
 @Pt.AcceptedRating Red (eal)
 */
public class StaticSchedulingDirector extends PortDirector {

    /** Construct the code generator adapter associated with the given
     *  StaticSchedulingDirector.
     *  @param staticSchedulingDirector The associated
     *  ptolemy.actor.sched.StaticSchedulingDirector
     */
    public StaticSchedulingDirector(
            ptolemy.actor.sched.StaticSchedulingDirector staticSchedulingDirector) {
        super(staticSchedulingDirector);
        ports = new Ports(getComponent(), getCodeGenerator(), this);

    /** The meta information about the ports in the container. */
        //public Ports ports = new Ports();


    }

    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

    /** Return whether the channels in multiports can be dynamically
     *  referenced using the $ref macro.
     *  @return True when the channels in multiports can be dynamically
     *  referenced using the $ref macro.
     *  @exception IllegalActionException If the expression cannot
     *   be parsed or cannot be evaluated, or if the result of evaluation
     *   violates type constraints, or if the result of evaluation is null
     *   and there are variables that depend on this one.
     */
    final public Boolean allowDynamicMultiportReference()
            throws IllegalActionException {
        NamedObj component = getComponent();
        if (component == null) {
            return false;
        }
        if (getCodeGenerator() == null) {
            return false;
        }
        Parameter allowDynamicMultiportReference = (Parameter) component
                .getDecoratorAttribute(getCodeGenerator(),
                        "allowDynamicMultiportReference");
        if (allowDynamicMultiportReference == null) {
            // $PTII/bin/vergil ptolemy/cg/lib/test/auto/ScaleC.xml was failing
            // here with a NPE because the there were no decorators.
            return false;
        }
        return ((BooleanToken) allowDynamicMultiportReference.getToken())
                .booleanValue();
    }

    /** Create and return the decorated attributes for the
     *  corresponding Ptolemy component.
     *  @param target The corresponding Ptolemy Component.
     *  @param genericCodeGenerator The code generator that is the
     *  decorator for the corresponding Ptolemy Component.
     *  @return The decorated attributes.
     */
    public DecoratorAttributes createDecoratorAttributes(NamedObj target,
            GenericCodeGenerator genericCodeGenerator) {
        // FIXME: Which types of targets should get decorated?
        try {
            return new StaticSchedulingDirectorAttributes(target,
                    genericCodeGenerator);
        } catch (KernelException ex) {
            throw new InternalErrorException(ex);
        }
    }

    /** Generate the code for the firing of actors according to the SDF
     *  schedule.
     *  @return The generated fire code.
     *  @exception IllegalActionException If the SDF director does not have an
     *   attribute called "iterations" or a valid schedule, or the actor to be
     *   fired cannot find its associated adapter.
     */
    public String generateFireCode() throws IllegalActionException {

        StringBuffer code = new StringBuffer();
        code.append(CodeStream.indent(getCodeGenerator().comment(
                "The firing of the StaticSchedulingDirector")));

        // Generate code for one iteration.
        ptolemy.actor.sched.StaticSchedulingDirector director = (ptolemy.actor.sched.StaticSchedulingDirector) getComponent();
        Schedule schedule = director.getScheduler().getSchedule();

        ProgramCodeGenerator codeGenerator = getCodeGenerator();

        Iterator<?> actorsToFire = schedule.firingIterator();
        while (actorsToFire.hasNext()) {
            Firing firing = (Firing) actorsToFire.next();
            Actor actor = firing.getActor();

            // FIXME: Before looking for an adapter class, we should check to
            // see whether the actor contains a code generator attribute.
            // If it does, we should use that as the adapter.
            NamedProgramCodeGeneratorAdapter adapter = (NamedProgramCodeGeneratorAdapter) codeGenerator
                    .getAdapter(actor);

            boolean inline = ((BooleanToken) getCodeGenerator().inline
                    .getToken()).booleanValue();

            if (inline) {
                for (int i = 0; i < firing.getIterationCount(); i++) {
                    // Generate fire code for the actor.
                    if (codeGenerator instanceof CCodeGenerator) {
                        getCodeGenerator().setModifiedVariables(
                                adapter.getModifiedVariables());
                    }
                    code.append(adapter.generateFireCode());
                    if (!(codeGenerator instanceof CCodeGenerator)) {
                        _generateUpdatePortOffsetCode(code, actor);
                    }
                }
            } else {
                //variableDeclarations.add(codeGenerator.generateFireFunctionVariableDeclaration((NamedObj)actor));
                int count = firing.getIterationCount();
                if (count > 1) {
                    // for loops should have the loop initial declaration outside the for block.  Test case:
                    // $PTII/bin/ptcg -language c -generateInSubdirectory false -inline false -maximumLinesPerBlock 2500 -variablesAsArrays false $PTII/ptolemy/cg/adapter/generic/program/procedural/c/adapters/ptolemy/actor/lib/test/auto/DistributorMultipleTypes.xml
                    code.append("{" + _eol + "int i = 0;" + _eol
                            + "for (; i < " + count + " ; i++) {" + _eol);
                }

                code.append(codeGenerator
                        .generateFireFunctionMethodInvocation((NamedObj) actor)
                        + "();" + _eol);

                if (!(codeGenerator instanceof CCodeGenerator)) {
                    _generateUpdatePortOffsetCode(code, actor);
                }

                if (count > 1) {
                    code.append("}" + _eol + "}" + _eol);
                }
            }
        }

        return code.toString();
    }

    /** Generate the initialize code for this director.
     *  The initialize code for the director is generated by appending the
     *  initialize code for each actor.
     *  @return The generated initialize code.
     *  @exception IllegalActionException If the adapter associated with
     *   an actor throws it while generating initialize code for the actor.
     */
    public String generateInitializeCode() throws IllegalActionException {
        StringBuffer code = new StringBuffer(super.generateInitializeCode());

        // We have to generate offsets for ports, see
        // ptolemy/cg/adapter/generic/program/procedural/java/adapters/ptolemy/domains/sdf/lib/test/auto/SampleDelay1.xml

        Iterator<?> actors = ((CompositeActor) _director.getContainer())
                .deepEntityList().iterator();
        while (actors.hasNext()) {
            Actor actor = (Actor) actors.next();

            // Update write offset due to initial tokens produced.
            Iterator outputPorts = actor.outputPortList().iterator();
            while (outputPorts.hasNext()) {
                IOPort port = (IOPort) outputPorts.next();
                int rate = DFUtilities.getTokenInitProduction(port);
                _updateConnectedPortsOffset(port, code, rate);
            }

            //             for (IOPort port : (List<IOPort>) ((Entity) actor).portList()) {
            //                 if (port.isOutsideConnected()) {
            //                     CodeGeneratorHelper portHelper = (CodeGeneratorHelper) _getHelper(port);
            //                     code.append(portHelper.generateInitializeCode());
            //                 }
            //             }
        }
        return code.toString();
    }

    /** Generate a main loop for an execution under the control of
     *  this director. If the associated director has a parameter
     *  named <i>iterations</i> with a value greater than zero,
     *  then wrap code generated by generateFireCode() in a
     *  loop that executes the specified number of iterations.
     *  Otherwise, wrap it in a loop that executes forever.
     *  In the loop, first get the code returned by generateFireCode(),
     *  and follow that with the code produced by the container
     *  help for generateModeTransitionCode(). That code will
     *  make state transitions in modal models at the conclusion
     *  of each iteration. Next, this code calls postfire(), and
     *  that returns false, breaks out of the main loop.
     *  Finally, if the director has a parameter named <i>period</i>,
     *  then increment the variable _currentTime after each
     *  pass through the loop.
     *  @return Code for the main loop of an execution.
     *  @exception IllegalActionException If something goes wrong.
     */
    public String generateMainLoop() throws IllegalActionException {
        // Need a leading _eol here or else the execute decl. gets stripped out.
        StringBuffer code = new StringBuffer(_eol
                + getCodeGenerator().getMethodVisibiliyString()
                + " void execute() "
                + getCodeGenerator().getMethodExceptionString() + " {" + _eol);

        Attribute iterations = _director.getAttribute("iterations");
        if (iterations == null) {
            code.append(_eol + "while (true) {" + _eol);
        } else {
            int iterationCount = ((IntToken) ((Variable) iterations).getToken())
                    .intValue();
            if (iterationCount <= 0) {
                code.append(_eol + "while (true) {" + _eol);
            } else {
                // Declare iteration outside of the loop to avoid
                // mode" with gcc-3.3.3
                code.append(_eol + "int iteration;" + _eol);
                code.append("for (iteration = 0; iteration < " + iterationCount
                        + "; iteration ++) {" + _eol);
            }
        }

        String[] splitFireCode = getCodeGenerator()._splitBody(
                "_" + CodeGeneratorAdapter.generateName(getComponent())
                        + "_run_", generateFireCode());

        code.append("if (!run()) {" + _eol + "break;" + _eol + "}" + _eol + "}"
                + _eol + "}" + _eol + _eol + splitFireCode[0] + _eol
                + getCodeGenerator().getMethodVisibiliyString()
                + " boolean run() "
                + getCodeGenerator().getMethodExceptionString() + " {" + _eol
                + splitFireCode[1]);

        // The code generated in generateModeTransitionCode() is executed
        // after one global iteration, e.g., in HDF model.
        NamedProgramCodeGeneratorAdapter modelAdapter = (NamedProgramCodeGeneratorAdapter) getCodeGenerator()
                .getAdapter(_director.getContainer());
        modelAdapter.generateModeTransitionCode(code);

        /*if (callPostfire) {
            code.append(_INDENT2 + "if (!postfire()) {" + _eol + _INDENT3
                    + "break;" + _eol + _INDENT2 + "}" + _eol);
        }
         */
        _generateUpdatePortOffsetCode(code, (Actor) _director.getContainer());

        code.append(generatePostfireCode());

        // Needed by the CurrentTime actor.
        code.append("++_iteration;" + _eol);

        Attribute period = _director.getAttribute("period");
        if (period != null) {
            Double periodValue = ((DoubleToken) ((Variable) period).getToken())
                    .doubleValue();
            if (periodValue != 0.0) {
                code.append("_currentTime += " + periodValue + ";" + _eol);
            }
        }

        code.append("return true;" + _eol + "}" + _eol);
        return code.toString();
    }

    /**
     * Generate sanitized name for the given named object. Remove all
     * underscores to avoid conflicts with systems functions.
     * @param port The port for which the name is generated.
     * @return The sanitized name.
     * @exception IllegalActionException If the variablesAsArrays parameter
     * cannot be read or if the buffer size of the port cannot be read.
     */
    public String generatePortName(TypedIOPort port)
            throws IllegalActionException {

        // FIXME: note that if we have a port that has a character that
        // is santized away, then we will run into problems if we try to
        // refer to the port by the sanitized name.
        String portName = StringUtilities.sanitizeName(port.getFullName());

        // FIXME: Assume that all objects share the same top level. In this case,
        // having the top level in the generated name does not help to
        // expand the name space but merely lengthen the name string.
        //        NamedObj parent = namedObj.toplevel();
        //        if (namedObj.toplevel() == namedObj) {
        //            return "_toplevel_";
        //        }
        //        String name = StringUtilities.sanitizeName(namedObj.getName(parent));
        if (portName.startsWith("_")) {
            portName = portName.substring(1, portName.length());
        }
        portName = TemplateParser.escapePortName(portName);

        if (!((BooleanToken) getCodeGenerator().variablesAsArrays.getToken())
                .booleanValue()) {
            return portName;
        }

        // Get the name of the port that refers to the array of all ports.
        return getCodeGenerator().generatePortName(port, portName,
                ports.getBufferSize(port));
    }

    /** Generate the preinitialize code for this director.
     *  The preinitialize code for the director is generated by appending
     *  the preinitialize code for each actor.
     *  @return The generated preinitialize code.
     *  @exception IllegalActionException If getting the adapter fails,
     *   or if generating the preinitialize code for a adapter fails,
     *   or if there is a problem getting the buffer size of a port.
     */
    public String generatePreinitializeCode() throws IllegalActionException {
        StringBuffer code = new StringBuffer();
        code.append(super.generatePreinitializeCode());

        ptolemy.actor.sched.StaticSchedulingDirector director = (ptolemy.actor.sched.StaticSchedulingDirector) getComponent();

        // Force schedule (re)calculation before generating code
        // because we need to know buffer capacity. (otherwise
        // sometimes new receivers are created but the schedule
        // is not re-calculated.)
        director.invalidateSchedule();
        director.getScheduler().getSchedule();

        return code.toString();
    }

    /** Return the reference to the specified parameter or port of the
     *  associated actor. For a parameter, the returned string is in
     *  the form "fullName_parameterName". For a port, the returned string
     *  is in the form "fullName_portName[channelNumber][offset]", if
     *  any channel number or offset is given.
     *
     * <p>Usually given the name of an input port, getReference(String
     * name) returns a target language variable name representing the
     * input port. Given the name of an output port,
     * getReference(String name) returns variable names representing
     * the input ports connected to the output port.  However, if the
     * name of an input port starts with '@', getReference(String
     * name) returns variable names representing the input ports
     * connected to the given input port on the inside.  If the name
     * of an output port starts with '@', getReference(String name)
     * returns variable name representing the the given output port
     * which has inside receivers.  The special use of '@' is for
     * composite actor when tokens are transferred into or out of the
     * composite actor.</p>
     *
     *  @param name The name of the parameter or port
     *  @param isWrite Whether to generate the write or read offset.
     *  @param target The ProgramCodeGeneratorAdapter for which code needs to be generated.
     *  @return The reference to that parameter or port (a variable name,
     *   for example).
     *  @exception IllegalActionException If the parameter or port does not
     *   exist or does not have a value.
     */
    public String getReference(String name, boolean isWrite,
            NamedProgramCodeGeneratorAdapter target)
            throws IllegalActionException {
        name = processCode(name);
        String castType = _getCastType(name);
        String refName = _getRefName(name);
        String[] channelAndOffset = _getChannelAndOffset(name);

        boolean forComposite = false;
        if (refName.charAt(0) == '@') {
            forComposite = true;
            refName = refName.substring(1);
        }

        TypedIOPort port = target.getTemplateParser().getPort(refName);
        if (port != null) {

            if (port instanceof ParameterPort && port.numLinks() <= 0) {

                // Then use the parameter (attribute) instead.
            } else {
                String result = getReference(port, channelAndOffset,
                        forComposite, isWrite, target);

                String refType = getCodeGenerator().codeGenType(port.getType());

                String returnValue = _templateParser.generateTypeConvertMethod(
                        result, castType, refType);
                return returnValue;
            }
        }

        // Try if the name is a parameter.
        Attribute attribute = target.getComponent().getAttribute(refName);
        if (attribute != null) {
            String refType = _getRefType(attribute);

            String result = _getParameter(target, attribute, channelAndOffset);

            result = _templateParser.generateTypeConvertMethod(result,
                    castType, refType);

            return result;
        }

        throw new IllegalActionException(target.getComponent(),
                "Reference not found: " + name);
    }

    /**
     * Return an unique label for the given port channel referenced
     * by the given adapter. By default, this delegates to the adapter to
     * generate the reference. Subclass may override this method
     * to generate the desire label according to the given parameters.
     * @param port The given port.
     * @param channelAndOffset The given channel and offset.
     * @param forComposite Whether the given adapter is associated with
     *  a CompositeActor
     * @param isWrite The type of the reference. True if this is
     *  a write reference; otherwise, this is a read reference.
     * @param target The ProgramCodeGeneratorAdapter for which code
     * needs to be generated.
     * @return an unique reference label for the given port channel.
     * @exception IllegalActionException If the adapter throws it while
     *  generating the label.
     */
    public String getReference(TypedIOPort port, String[] channelAndOffset,
            boolean forComposite, boolean isWrite,
            NamedProgramCodeGeneratorAdapter target)
            throws IllegalActionException {
        boolean dynamicReferencesAllowed = allowDynamicMultiportReference();

        int channelNumber = 0;
        boolean isChannelNumberInt = true;
        if (!channelAndOffset[0].equals("")) {
            // If dynamic multiport references are allowed, catch errors
            // when the channel specification is not an integer.
            if (dynamicReferencesAllowed) {
                try {
                    channelNumber = Integer.valueOf(channelAndOffset[0])
                            .intValue();
                } catch (NumberFormatException ex) {
                    isChannelNumberInt = false;
                }
            } else {
                channelNumber = Integer.valueOf(channelAndOffset[0]).intValue();
            }
        }
        if (!isChannelNumberInt) { // variable channel reference.
            if (port.isOutput()) {
                throw new IllegalActionException(
                        "Variable channel reference not supported"
                                + " for output ports");
            } else {
                String returnValue = _generatePortReference(port,
                        channelAndOffset, isWrite);
                return returnValue;
            }
        }

        StringBuffer result = new StringBuffer();

        // To support modal model, we need to check the following condition
        // first because an output port of a modal controller should be
        // mainly treated as an output port. However, during choice action,
        // an output port of a modal controller will receive the tokens sent
        // from the same port.  During commit action, an output port of a modal
        // controller will NOT receive the tokens sent from the same port.
        if (_checkRemote(forComposite, port)) {
            Receiver[][] remoteReceivers;

            // For the same reason as above, we cannot do: if (port.isInput())...
            if (port.isOutput()) {
                remoteReceivers = port.getRemoteReceivers();
            } else {
                remoteReceivers = port.deepGetReceivers();
            }

            if (remoteReceivers.length == 0) {
                // The channel of this output port doesn't have any
                // sink or is a PortParameter.
                // Needed by $PTII/bin/ptcg -language java -variablesAsArrays true $PTII/ptolemy/cg/kernel/generic/program/procedural/java/test/auto/PortParameterOpaque.xml
                String returnValue = generatePortName(port);
                return returnValue;
            }

            ProgramCodeGeneratorAdapter.Channel sourceChannel = new ProgramCodeGeneratorAdapter.Channel(
                    port, channelNumber);

            List<ProgramCodeGeneratorAdapter.Channel> typeConvertSinks = target
                    .getTypeConvertSinkChannels(sourceChannel);

            List<ProgramCodeGeneratorAdapter.Channel> sinkChannels = getSinkChannels(
                    port, channelNumber);

            boolean hasTypeConvertReference = false;
            for (int i = 0; i < sinkChannels.size(); i++) {
                ProgramCodeGeneratorAdapter.Channel channel = sinkChannels
                        .get(i);
                TypedIOPort sinkPort = (TypedIOPort) channel.port;
                int sinkChannelNumber = channel.channelNumber;

                // Type convert.
                if (typeConvertSinks.contains(channel)
                        && getCodeGenerator().isPrimitive(
                                ((TypedIOPort) sourceChannel.port).getType())) {

                    if (!hasTypeConvertReference) {
                        if (i != 0) {
                            result.append(" = ");
                        }
                        result.append(getTypeConvertReference(sourceChannel));

                        if (dynamicReferencesAllowed && port.isInput()) {
                            if (channelAndOffset[1].trim().length() > 0) {
                                result.append("[" + channelAndOffset[1].trim()
                                        + "]");
                            } else {
                                result.append("["
                                        + _generateChannelOffset(port, isWrite,
                                                channelAndOffset[0]) + "]");
                            }
                        } else {
                            int rate = Math
                                    .max(DFUtilities
                                            .getTokenProductionRate(sourceChannel.port),
                                            DFUtilities
                                                    .getTokenConsumptionRate(sourceChannel.port));
                            if (rate > 1
                                    && channelAndOffset[1].trim().length() > 0) {
                                result.append("[" + channelAndOffset[1].trim()
                                        + "]");
                            }
                        }
                        hasTypeConvertReference = true;
                    } else {
                        // We already generated reference for this sink.
                        continue;
                    }
                } else {
                    if (i != 0) {
                        result.append(" = ");
                    }
                    result.append(generatePortName(sinkPort));

                    if (sinkPort.isMultiport()) {
                        result.append("[" + sinkChannelNumber + "]");
                    }
                    if (channelAndOffset[1].equals("")) {
                        channelAndOffset[1] = "0";
                    }
                    result.append(ports.generateOffset(sinkPort,
                            channelAndOffset[1], sinkChannelNumber, true));
                }
            }
            return result.toString();
        }

        // Note that if the width is 0, then we have no connection to
        // the port but the port might be a PortParameter, in which
        // case we want the Parameter.
        // codegen/c/actor/lib/string/test/auto/StringCompare3.xml
        // tests this.

        if (_checkLocal(forComposite, port)) {

            result.append(/*NamedProgramCodeGeneratorAdapter.*/generatePortName(port));

            //if (!channelAndOffset[0].equals("")) {
            if (port.isMultiport()) {
                // Channel number specified. This must be a multiport.
                result.append("[" + channelAndOffset[0] + "]");
            }

            result.append(ports.generateOffset(port, channelAndOffset[1],
                    channelNumber, isWrite));
            return result.toString();
        }

        // FIXME: when does this happen?
        return "";
    }

    /** Generate a variable declaration for the <i>period</i> parameter,
     *  if there is one.
     *  @return code The generated code.
     *  @exception IllegalActionException If the adapter class for the model
     *   director cannot be found.
     */
    public String generateVariableDeclaration() throws IllegalActionException {
        StringBuffer variableDeclarations = new StringBuffer(
                super.generateVariableDeclaration());
        Attribute period = _director.getAttribute("period");
        if (period != null) {
            Double periodValue = ((DoubleToken) ((Variable) period).getToken())
                    .doubleValue();
            if (periodValue != 0.0) {
                if (variableDeclarations.toString().indexOf(
                        StaticSchedulingDirector.CURRENTTIME_DECLARATION) == -1) {
                    variableDeclarations.append(_eol
                            + getCodeGenerator().comment(
                                    "Director has a period attribute,"
                                            + " so we track current time."));
                    variableDeclarations.append(CURRENTTIME_DECLARATION);
                }
            }
        }

        return variableDeclarations.toString();
    }

    /** Return whether we need to pad buffers or not.
     *  @return True when we need to pad buffers.
     *  @exception IllegalActionException If the expression cannot
     *   be parsed or cannot be evaluated, or if the result of evaluation
     *   violates type constraints, or if the result of evaluation is null
     *   and there are variables that depend on this one.
     */
    final public Boolean padBuffers() throws IllegalActionException {
        DecoratorAttributes decorators = getComponent().getDecoratorAttributes(
                getCodeGenerator());
        if (!(decorators instanceof StaticSchedulingDirectorAttributes)) {
            // $PTII/bin/vergil ptolemy/cg/lib/test/auto/ScaleC.xml was failing here
            // it is okd to not have a decorator and just default to false.
            // throw new IllegalActionException(getComponent(), "Has no StaticSchedulingDirectorAttributes decorators!");
            return false;
        }
        return ((BooleanToken) ((StaticSchedulingDirectorAttributes) decorators).padBuffers
                .getToken()).booleanValue();
    }

    /** The declaration for the _currentTime variable. */
    public static final String CURRENTTIME_DECLARATION = "double _currentTime = 0;"
            + _eol;

    ///////////////////////////////////////////////////////////////////
    ////                         protected methods                 ////

    /**
     * Generate the code that updates the input/output port offset.
     * @param code The given code buffer.
     * @param actor The given actor.
     * @exception IllegalActionException Thrown if
     *  _updatePortOffset(IOPort, StringBuffer, int) or getRate(IOPort)
     *  throw it.
     */
    protected void _generateUpdatePortOffsetCode(StringBuffer code, Actor actor)
            throws IllegalActionException {
        // update buffer offset after firing each actor once
        Iterator<?> inputPorts = actor.inputPortList().iterator();
        while (inputPorts.hasNext()) {
            IOPort port = (IOPort) inputPorts.next();
            int rate = DFUtilities.getRate(port);
            _updatePortOffset(port, code, rate);
        }

        Iterator<?> outputPorts = actor.outputPortList().iterator();
        while (outputPorts.hasNext()) {
            IOPort port = (IOPort) outputPorts.next();
            int rate = DFUtilities.getRate(port);
            _updateConnectedPortsOffset(port, code, rate);
        }
    }

    /**
     * Return an unique label for the given attribute referenced
     * by the given adapter. Subclass should override this method
     * to generate the desire label according to the given parameters.
     * @param target The ProgramCodeGeneratorAdapter for which code
     * needs to be generated.
     * @param attribute The given attribute.
     * @param channelAndOffset The given channel and offset.
     * @return an unique label for the given attribute.
     * @exception IllegalActionException If the adapter throws it while
     *  generating the label.
     */
    protected String _getParameter(NamedProgramCodeGeneratorAdapter target,
            Attribute attribute, String[] channelAndOffset)
            throws IllegalActionException {
        return "";
    }

    /** Update the offsets of the buffers associated with the ports connected
     *  with the given port in its downstream.
     *
     *  @param port The port whose directly connected downstream actors update
     *   their write offsets.
     *  @param code The string buffer that the generated code is appended to.
     *  @param rate The rate, which must be greater than or equal to 0.
     *  @exception IllegalActionException If thrown while reading or writing
     *   offsets, or getting the buffer size, or if the rate is less than 0.
     */
    final protected void _updateConnectedPortsOffset(IOPort port,
            StringBuffer code, int rate) throws IllegalActionException {
        if (rate == 0) {
            return;
        } else if (rate < 0) {
            throw new IllegalActionException(port, "the rate: " + rate
                    + " is negative.");
        }

        code.append(ports.updateConnectedPortsOffset(port, rate));
    }

    /**
     *  Update the read offsets of the buffer associated with the given port.
     *  @param port The port whose read offset is to be updated.
     *  @param code The string buffer that the generated code is appended to.
     *  @param rate The rate, which must be greater than or equal to 0.
     *  @exception IllegalActionException If thrown while reading or writing
     *   offsets, or getting the buffer size, or if the rate is less than 0.
     */
    protected void _updatePortOffset(IOPort port, StringBuffer code, int rate)
            throws IllegalActionException {
        if (rate == 0) {
            return;
        } else if (rate < 0) {
            throw new IllegalActionException(port, "the rate: " + rate
                    + " is negative.");
        }

        code.append(ports.updateOffset(port, rate));
    }

    ///////////////////////////////////////////////////////////////////
    ////                         private methods                   ////

    static private boolean _checkLocal(boolean forComposite, IOPort port)
            throws IllegalActionException {
        return port.isInput() && !forComposite && port.isOutsideConnected()
                || port.isOutput() && forComposite;
    }

    static private boolean _checkRemote(boolean forComposite, IOPort port) {
        return port.isOutput() && !forComposite || port.isInput()
                && forComposite;
    }

    /**
     * Generate a string that represents the offset for a dynamically determined
     *  channel of a multiport.
     * @param port The referenced port.
     * @param isWrite Whether to generate the write or read offset.
     * @param channelString The string that will determine the channel.
     * @return The expression that represents the offset for a channel determined
     *  dynamically in the generated code.
     */
    private/*static*/String _generateChannelOffset(TypedIOPort port,
            boolean isWrite, String channelString)
            throws IllegalActionException {
        // By default, return the channel offset for the first channel.
        if (channelString.equals("")) {
            channelString = "0";
        }

        String channelOffset = generatePortName(port)
                + (isWrite ? "_writeOffset" : "_readOffset") + "["
                + channelString + "]";

        return channelOffset;
    }

    private/*static*/String _generatePortReference(TypedIOPort port,
            String[] channelAndOffset, boolean isWrite)
            throws IllegalActionException {

        StringBuffer result = new StringBuffer();
        String channelOffset;
        if (channelAndOffset[1].equals("")) {
            channelOffset = _generateChannelOffset(port, isWrite,
                    channelAndOffset[0]);
        } else {
            channelOffset = channelAndOffset[1];
        }

        result.append(generatePortName(port));

        if (port.isMultiport()) {
            result.append("[" + channelAndOffset[0] + "]");
        }
        result.append("[" + channelOffset + "]");

        return result.toString();
    }

    private String _getCastType(String name) throws IllegalActionException {
        StringTokenizer tokenizer = new StringTokenizer(name, "#,", true);

        // Get the referenced name.
        String refName = tokenizer.nextToken().trim();

        // Get the cast type (if any), so we can add the proper convert method.
        StringTokenizer tokenizer2 = new StringTokenizer(refName, "()", false);
        if (tokenizer2.countTokens() != 1 && tokenizer2.countTokens() != 2) {
            throw new IllegalActionException(getComponent(),
                    "Invalid cast type: " + refName);
        }

        if (tokenizer2.countTokens() == 2) {
            String type = tokenizer2.nextToken().trim();
            return type.length() > 0 ? type : null;
        }
        return null;
    }

    /** Return the channel and offset given in a string.
     *  The result is an string array of length 2. The first element
     *  indicates the channel index, and the second the offset. If either
     *  element is an empty string, it means that channel/offset is not
     *  specified.
     * @param name The given string.
     * @return An string array of length 2, containing expressions of the
     *  channel index and offset.
     * @exception IllegalActionException If the channel index or offset
     *  specified in the given string is illegal.
     */
    private String[] _getChannelAndOffset(String name)
            throws IllegalActionException {

        String[] result = { "", "" };

        // Given expression of forms:
        //     "port"
        //     "port, offset", or
        //     "port#channel, offset".

        int poundIndex = TemplateParser.indexOf("#", name, 0);
        int commaIndex = TemplateParser.indexOf(",", name, 0);

        if (commaIndex < 0) {
            commaIndex = name.length();
        }
        if (poundIndex < 0) {
            poundIndex = commaIndex;
        }

        if (poundIndex < commaIndex) {
            result[0] = name.substring(poundIndex + 1, commaIndex);
        }

        if (commaIndex < name.length()) {
            result[1] = name.substring(commaIndex + 1);
        }
        return result;
    }

    private String _getRefName(String name) throws IllegalActionException {
        StringTokenizer tokenizer = new StringTokenizer(name, "#,", true);

        if (tokenizer.countTokens() != 1 && tokenizer.countTokens() != 3
                && tokenizer.countTokens() != 5) {
            throw new IllegalActionException(getComponent(),
                    "Reference not found: " + name);
        }

        // Get the referenced name.
        String refName = tokenizer.nextToken().trim();

        // Get the cast type (if any), so we can add the proper convert method.
        StringTokenizer tokenizer2 = new StringTokenizer(refName, "()", false);
        if (tokenizer2.countTokens() != 1 && tokenizer2.countTokens() != 2) {
            throw new IllegalActionException(getComponent(),
                    "Invalid cast type: " + refName);
        }

        if (tokenizer2.countTokens() == 2) {
            // castType
            tokenizer2.nextToken();
        }

        return tokenizer2.nextToken().trim();
    }

    private String _getRefType(Attribute attribute) {
        if (attribute instanceof Parameter) {
            return getCodeGenerator().codeGenType(
                    ((Parameter) attribute).getType());
        }
        return null;
    }
}
