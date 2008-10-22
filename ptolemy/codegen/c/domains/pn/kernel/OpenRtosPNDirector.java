/* Code generator helper class associated with the PNDirector class.

 Copyright (c) 2008 The Regents of the University of California.
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
package ptolemy.codegen.c.domains.pn.kernel;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import ptolemy.actor.Actor;
import ptolemy.actor.CompositeActor;
import ptolemy.actor.IOPort;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.lib.LimitedFiringSource;
import ptolemy.codegen.kernel.CodeGeneratorHelper;
import ptolemy.codegen.kernel.Director;
import ptolemy.data.BooleanToken;
import ptolemy.data.IntToken;
import ptolemy.data.expr.Parameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.Entity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NamedObj;

//////////////////////////////////////////////////////////////////
////OpenRtosPNDirector

/**
 * Code generator helper associated with the PNDirector class. This
 * helper generates OpenRTOS specific code. This director starts a
 * task for each actor.  Each task executes a given function which
 * includes the actor initialization, fire and wrapup code. The
 * communication between tasks use the OpenRTOS queues. These queues
 * provide synchronized access methods. This director helper generates
 * a queue for each connection which is referenced by its referable
 * port channel. There is one referable port channel for each
 * connection.
 * 
 * @author Man-Kit Leung
 * @version $Id: OpenRtosPNDirector.java 50690 2008-09-21 06:27:03Z mankit $
 * @since Ptolemy II 7.0
 * @Pt.ProposedRating Red (mankit)
 * @Pt.AcceptedRating Red (mankit)
 */
public class OpenRtosPNDirector extends Director {

	/**
	 * Construct the code generator helper associated with the
	 * given PNDirector.
	 * 
	 * @param pnDirector
	 *            The associated ptolemy.domains.pn.kernel.PNDirector
	 */
	public OpenRtosPNDirector(ptolemy.domains.pn.kernel.PNDirector pnDirector) {
		super(pnDirector);
	}

	// ////////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Generate code for getting data from the specified
	 * channel. This uses the built-in queues provided by OpenRTOS
	 * to transport data between tasks. It generates an invocation
	 * of the xQueueReceive() function which takes as arguments a
	 * queue reference, variable pointer for holding the data, and
	 * a maximum wait time. To fulfill the blocking communication
	 * semantics of PN, a while loop is generated to ensure the
	 * receipt of data. A queue is declared and created for each
	 * connection (input-output port pair).
	 * $ref(port#channalNumber) is used as the data holder, and
	 * the wait time is determined by _getMaxDelay(Channel).
	 * 
	 * @param port
	 *            The specified port.
	 * @param channelNumber
	 *            The specified channel.
	 * @return The code for getting data from specified channel.
	 * @throws IllegalActionException
	 *             If the specified port channel has more than one
	 *              referable queues.
	 * @exception IllegalActionException
	 *                If the {@link #getReferenceChannels(IOPort, int),
	 *                #processCode(String)} method throws the exceptions.
	 */
	public String generateCodeForGet(IOPort port, int channelNumber)
			throws IllegalActionException {

		List<Channel> channels = getReferenceChannels(port,
                        channelNumber);

		if (channels.size() == 0) {
			return "";
		}
		if (channels.size() != 1) {
			throw new IllegalActionException(this,
					"There are more than one channel to get data from. "
							+ "This is ambiguous.");
		}

		Channel referenceChannel = channels.get(0);
		IOPort referencePort = referenceChannel.port;

		if (referencePort.getWidth() <= 0) {
			return "";
		}

		CodeGeneratorHelper actorHelper = (CodeGeneratorHelper) _getHelper(port
				.getContainer());

		String dataVariable = "$ref(" + referencePort.getName() + "#"
				+ referenceChannel.channelNumber + ")";
		String queue = _generateQueueReference(referencePort,
				referenceChannel.channelNumber);
		String waitTime = _getMaxDelay(referenceChannel);

		return actorHelper.processCode("while( pdTRUE != xQueueReceive("
				+ queue + ", &" + dataVariable + ", "
                        + waitTime + ") );");
	}

	/**
	 * Generate code for sending data to the specified channel.
	 * 
	 * @param port
	 *            The specified port.
	 * @param channelNumber
	 *            The specified channel.
	 * @return The code for sending data to the specified channel.
	 * @exception IllegalActionException
	 *                If the {@link #getReferenceChannels(IOPort, int),
	 *                #processCode(String)} method throws the exceptions.
	 */
	public String generateCodeForSend(IOPort port, int channelNumber,
			String dataToken) throws IllegalActionException {

		StringBuffer result = new StringBuffer();
		List<Channel> channels = getReferenceChannels(port, channelNumber);

		for (Channel referenceChannel : channels) {
			IOPort referencePort = referenceChannel.port;

			CodeGeneratorHelper actorHelper = (CodeGeneratorHelper) _getHelper(referencePort
					.getContainer());

			String dataVariable = "$ref(" + referencePort.getName() + "#"
					+ referenceChannel.channelNumber + ")";

			String queue = _generateQueueReference(referencePort,
					referenceChannel.channelNumber);
			String waitTime = _getMaxDelay(referenceChannel);

			result.append(actorHelper
					.processCode("while( pdTRUE != xQueueSend(" + queue + ", &"
							+ dataVariable + ", " + waitTime + ") );" + _eol));
		}
		return result.toString();
	}

	/**
	 * Generate the fire code. This generates a xTaskCreate()
	 * function call for each actor. The xTaskCreate() function
	 * takes as parameters a task function pointer, the task name,
	 * a stack size value, a pointer to the task parameters, a
	 * value for task priority, and the task handle which would be
	 * assigned upon the return of the function. The
	 * vTaskStartScheduler() is generated after the xTaskCreate()
	 * calls. It starts the task scheduler once every task is
	 * created.
	 * 
	 * @return The generated fire code.
	 * @exception IllegalActionException
	 *                Not thrown in this class.
	 */
	public String generateFireCode() throws IllegalActionException {
		StringBuffer code = new StringBuffer();
		CompositeActor compositeActor = (CompositeActor) _director
				.getContainer();

		code.append(_codeGenerator.comment("Create a task for each actor."));

		List<Actor> actorList = compositeActor.deepEntityList();

		for (Actor actor : actorList) {
			code.append("xTaskCreate(");
			code.append(_getActorTaskLabel(actor)); // task function.
			code.append(", \"" + actor.getDisplayName()); // task string label.
			code.append("\", " + _getStackSize(actor)); // stack depth.
			code.append(", NULL"); // pointer to task parameters
			code.append(", " + _getPriority(actor)); // priority.
			code.append(", task_" + // task handle.
					_getActorTaskLabel(actor) + ");" + _eol);
		}

		/* Start the scheduler. */
		code.append("vTaskStartScheduler();" + _eol);

		return code.toString();
	}

	/**
	 * Generate the fire function code. This generates the
	 * function code for each task. It is a collection of global
	 * functions, one for each actor that is visible to this
	 * director helper. Creating each new task requires one of
	 * these function as parameter. It is the code that the task
	 * executes. When the inline parameter is checked, the task
	 * function code is generated in 
         * {@link #generatePreinitializeCode()} which is outside the main
	 * function.
	 * 
	 * @return The fire function code.
	 * @exception IllegalActionException
	 *                If there is an exception in generating the
	 *                task function code.
	 */
	public String generateFireFunctionCode()
                throws IllegalActionException {
		StringBuffer code = new StringBuffer();
		_generateTaskFunctionCode(code);
		return code.toString();
	}

	/**
	 * Return the buffer size to generate the variable for the
	 * specified port channel. This number dictates the size of
	 * the array generated for variable associated with the port
	 * channel. This returns 1, since queuing is done using a
	 * separate structure.
	 * 
	 * @return The buffer size to generate the variable for the specified
         *         port channel. In this case, it's 1.
	 * @exception IllegalActionException
	 *                Not thrown in this class.
	 */
	public int getBufferSize(IOPort port, int channelNumber)
			throws IllegalActionException {
		return 1;
	}

	/**
	 * Get the files needed by the code generated from this helper
	 * class. The header files required are "FreeRTOS.h",
	 * "task.h", "queue.h", "lcd_message.h", and
	 * "semphr.h". Because of dependencies between these header
	 * files, they are included in the order specified.
	 * 
	 * @return A set of strings that are header files needed by the code
	 *         generated from this helper class.
	 * @exception IllegalActionException
	 *                Not thrown in this class.
	 */
	public Set<String> getHeaderFiles() throws IllegalActionException {
		Set<String> files = new LinkedHashSet<String>();
		files.add("\"FreeRTOS.h\"");
		files.add("\"task.h\"");
		files.add("\"queue.h\"");
		files.add("\"lcd_message.h\"");
		files.add("\"semphr.h\"");
		return files;
	}

	/**
	 * Generate the initialize code. This generates the hardware
	 * initialization code and creates the queues for all
	 * referable port channels.
	 * 
	 * @return The generated initialize code.
	 * @exception IllegalActionException
	 *                If the helper associated with an actor throws it while
	 *                generating initialize code for the actor.
	 */
	public String generateInitializeCode() throws IllegalActionException {
		StringBuffer code = new StringBuffer();

		code.append(_codeGenerator
				.comment("Initialization code of the PNDirector."));

		// Don't call super.generateInitializeCode() which
		// would generate the initialize code of the actors.

		code.append(_codeStream.getCodeBlock("initHWBlock"));

		List args = new LinkedList();
		args.add("");
		args.add("");
		args.add("");

		// Initialize each queue variable.
		for (Channel buffer : _queues) {
			args.set(0, _generateQueueReference(buffer.port,
					buffer.channelNumber));
			args.set(1, _getQueueSize(buffer.port, buffer.channelNumber));
			args.set(2, CodeGeneratorHelper
					.targetType(((TypedIOPort) buffer.port).getType()));
			code.append(_codeStream.getCodeBlock("initBuffer", args));
		}
		return code.toString();
	}

	/**
	 * Generate the main loop code. If the inline parameter of the
	 * code generator is checked, it generates the fire code;
	 * otherwise, it generates the a call to the fire function
	 * generated by generateFireFunction().
	 * 
	 * @return The main loop code.
	 * @exception IllegalActionException
	 *                If looking up the inline parameter or generating
         *                the fire code throws it.
	 */
	public String generateMainLoop() throws IllegalActionException {
		StringBuffer code = new StringBuffer();

		boolean inline = ((BooleanToken) _codeGenerator.inline.getToken())
				.booleanValue();

		if (inline) {
			code.append(generateFireCode());
		} else {
			code.append(CodeGeneratorHelper.generateName(_director
					.getContainer())
					+ "();" + _eol);
		}
		return code.toString();
	}

	/**
	 * Generate the preinitialize code for the associated PN director.
	 * 
	 * @return The generated preinitialize code.
	 * @exception IllegalActionException
	 *                If the helper associated with an actor throws it
	 *                while generating preinitialize code for the actor.
	 */
	public String generatePreinitializeCode()
                throws IllegalActionException {
		StringBuffer bufferCode = new StringBuffer();
		StringBuffer code = new StringBuffer();

		_queues.clear();

		List<Entity> actorList = ((CompositeEntity) _director.getContainer())
				.deepEntityList();

		for (Entity actor : actorList) {

			code.append("xTaskHandle *task_");
			code.append(_getActorTaskLabel((Actor) actor));
			code.append(";" + _eol);

			for (TypedIOPort port : (List<TypedIOPort>) actor.portList()) {
				bufferCode.append(_createDynamicOffsetVariables(port));
			}
		}
		code.append(super.generatePreinitializeCode());

		code.append(bufferCode);

		if (_codeGenerator.inline.getToken() == BooleanToken.TRUE) {
			_generateTaskFunctionCode(code);
		}

		return code.toString();
	}

	/**
	 * Generate code for transferring tokens into a composite.
	 * 
	 * FIXME: We haven't considered the case where this PNDirector
	 * is embedded within another director. The only possible
	 * combination is PN inside PN.  In this case, we should
	 * generate one cross-level queue rather than two.  In any
	 * case, this method override the base class method and
	 * generate no extra code.
	 * 
	 * @param inputPort
	 *            The port to transfer tokens.
	 * @param code
	 *            The string buffer that the generated code is appended to.
	 * @exception IllegalActionException
	 *                If thrown while transferring tokens.
	 */
	public void generateTransferInputsCode(IOPort inputPort, StringBuffer code)
			throws IllegalActionException {
		return;
	}

	/**
	 * Generate code for transferring tokens outside of a composite.
	 * 
	 * FIXME: We haven't considered the case where this PNDirector
	 * is embedded within another director. The only possible
	 * combination is PN inside PN.  In this case, we should
	 * generate one cross-level queue rather than two.  In any
	 * case, this method override the base class method and
	 * generate no extra code.
	 * 
	 * @param port
	 *            The specified port.
	 * @param code
	 *            The given code buffer.
	 * @exception IllegalActionException
	 *                Not thrown in this class.
	 */
	public void generateTransferOutputsCode(IOPort port, StringBuffer code)
			throws IllegalActionException {
		return;
	}

	/**
	 * Generate variable initialization for the referenced
	 * parameters. This overrides the super class method and
	 * returns an empty string. It avoids generating any offset
	 * variables.
	 * 
	 * @return code The empty string.
	 * @exception IllegalActionException
	 *                Not thrown in this class.
	 */
	public String generateVariableInitialization()
			throws IllegalActionException {
		return "";
	}

	// /////////////////////////////////////////////////////////////////
	// // protected methods ////

	/**
	 * Create the offset variables for the given port. Since
	 * built-in queue structure is used, it eliminates the need
	 * for offset variables. Instead, this method generates the
	 * declaration of the queues if the specified port has any
	 * referable port channels. There is one referable port
	 * channel for each connection. All input port channels are
	 * considered referable.
	 * 
	 * @param port
	 *            The specified port.
	 * @return Code that declares the read and write offset variables.
	 * @exception IllegalActionException
	 *                If getting the code block throws it.
	 */
	protected String _createDynamicOffsetVariables(TypedIOPort port)
			throws IllegalActionException {
		StringBuffer code = new StringBuffer();
		code.append(_eol
				+ _codeGenerator
						.comment("OpenRtosPNDirector's queue declarations."));

		int width;
		if (port.isInput()) {
			width = port.getWidth();
		} else {
			width = 0;
		}

		// Declare a queue variable for each channel.
		if (width != 0) {

			List args = new LinkedList();
			args.add("");

			for (int i = 0; i < width; i++) {
				args.set(0, _generateQueueReference(port, i));
				code.append(_codeStream.getCodeBlock("declareBufferHeader",
						args));

				// Keep track of the queues.
				_queues.add(new Channel(port, i));
			}
		}
		return code.toString();
	}

	// /////////////////////////////////////////////////////////////////
	// // private methods ////

	/**
	 * Generate the reference of the queue generated for the
	 * specified port and channel number. The specified port
	 * channel is assumed to be referable.
	 * 
	 * @param port
	 *            The specified port.
	 * @param channelNumber
	 *            The specified channel number.
	 * @return The reference for the queue.
	 */
	private static String _generateQueueReference(IOPort port,
                int channelNumber) {
		return CodeGeneratorHelper.generateName(port) + "_"
                    + channelNumber + "_queue";
	}

	/**
	 * Generate the task functions. A task function is generated
	 * for each actor that is visible to this director helper. A
	 * task function consists of the actor's initialize, fire and
	 * wrapup code. In particular, a loop is generated to iterate
	 * the actor's fire code. If the actor has a firing count
	 * limit, a finite for loop is generated. Otherwise, the fire
	 * code is wrapped inside an infinite loop.
	 * 
	 * @param code
	 *            The given code buffer.
	 * @throws IllegalActionException
	 *             If getting the helper or generating the actor
	 *             initialize, fire, or wrapup code throws it.
	 */
	private void _generateTaskFunctionCode(StringBuffer code)
			throws IllegalActionException {

		List<Actor> actorList = ((CompositeActor) _director.getContainer())
				.deepEntityList();
		boolean inline = ((BooleanToken) _codeGenerator.inline.getToken())
				.booleanValue();

		// Generate the task function for each actor.
		for (Actor actor : actorList) {
			StringBuffer functionCode = new StringBuffer();

			CodeGeneratorHelper helper = (CodeGeneratorHelper) _getHelper((NamedObj) actor);

			if (!inline) {
				code.append(helper.generateFireFunctionCode());
			}

			code.append(_eol + "static void " + _getActorTaskLabel(actor)
					+ "(void* arg) {" + _eol);

			// Check if the actor is an opague CompositeActor.
			// The actor is guaranteed to be opague from calling
			// deepEntityList(),
			// so all we need to check whether or not it is a CompositeActor.
			if (actor instanceof CompositeActor) {
				Director directorHelper = (Director) _getHelper(actor
						.getDirector());

				// If so, it should contain a different Director.
				assert (directorHelper != this);

				functionCode.append(directorHelper.generateMainLoop());

			} else {

				// if firingCountLimit exists, generate for loop.
				if (actor instanceof LimitedFiringSource) {
					int firingCount = ((IntToken) ((LimitedFiringSource) actor).firingCountLimit
							.getToken()).intValue();

					functionCode.append("int i = 0;" + _eol);
					functionCode.append("for (; i < " + firingCount
							+ "; i++) {" + _eol);

				} else {
					functionCode.append("while (true) {" + _eol);
				}

				functionCode.append(helper.generateFireCode());
				functionCode.append(helper.generatePostfireCode());

				functionCode.append(_eol + "}" + _eol);
			}

			// wrapup
			functionCode.append(helper.generateWrapupCode());

			// Make sure the task is running forever.
			functionCode.append("while(true);" + _eol);

			functionCode.append("}" + _eol);

			// init
			// This needs to be called last because all references
			// need to be collected before generating their initialization.
			String initializeCode = helper.generateInitializeCode();
			String variableInitializeCode = helper
					.generateVariableInitialization();

			code.append(variableInitializeCode);
			code.append(initializeCode);
			code.append(functionCode);
		}
	}

	/**
	 * Generate the label of the task generated for the specified actor.
	 * 
	 * @param actor
	 *            The specified actor.
	 * @return The task label for the specified actor.
	 */
	private String _getActorTaskLabel(Actor actor) {
		return CodeGeneratorHelper.generateName((NamedObj) actor)
				+ "_TaskFunction";
	}

	/**
	 * Return the maximum wait time for getting and sending data
	 * on the specified port channel. The wait time specifies the
	 * duration the queue access functions block before
	 * returning. In this case, it is set to the maximum,
	 * portMAX_DELAY, which is a symbolic constant defined in the
	 * OpenRTOS header file.
	 * 
	 * @param channel
	 *            The specified port channel.
	 * @return The maximum wait time for getting and sending data.
	 */
	private String _getMaxDelay(Channel channel) {
		return "portMAX_DELAY";
	}

	/**
	 * Return the task priority associated with the specified
	 * actor. If the actor has a parameter named "_priority", its
	 * expression is returned.  Otherwise, zero is the default
	 * priority value.
	 * 
	 * @param actor
	 *            The specified actor.
	 * @return The task priority associated with the specified actor.
	 */
	private String _getPriority(Actor actor) {
		// Getting this info from static analysis.
		Parameter parameter = (Parameter) ((Entity) actor)
				.getAttribute("_priority");
		if (parameter != null) {
			return parameter.getExpression();
		}
		return "0";
	}

	/**
	 * Return the size of the queue to be generated for the specified port
	 * channel.
	 * 
	 * @param port
	 *            The specified port.
	 * @param channelNumber
	 *            The specified channel number.
	 * @return The size of the queue to be generated for the given port
                      channel.
	 * @throws IllegalActionException
	 */
	private int _getQueueSize(IOPort port, int channelNumber)
			throws IllegalActionException {
		// FIXME: we can get this info from static analysis.
		IntToken sizeToken = (IntToken) ((ptolemy.domains.pn.kernel.PNDirector) _director).initialQueueCapacity
				.getToken();

		return sizeToken.intValue();
	}

	/**
	 * Return the expression of the stack size value for the
	 * specified actor task. If the specified actor has a
	 * parameter named "_stackSize", its expression is
	 * return. Otherwise, 80 is default return value.
	 * 
	 * @param actor
	 *            The specified actor.
	 * @return The expression of the stack size value.
	 */
	private String _getStackSize(Actor actor) {
		// FIXME: we can get this info from static analysis.
		Parameter parameter = (Parameter) ((Entity) actor)
				.getAttribute("_stackSize");
		if (parameter != null) {
			return parameter.getExpression();
		}
		return "80";
	}

	// /////////////////////////////////////////////////////////////////
	// // private variables ////

	/**
	 * The set of referable channels that are associated with a
	 * generated queue.
	 */
	private HashSet<Channel> _queues = new HashSet<Channel>();
}
