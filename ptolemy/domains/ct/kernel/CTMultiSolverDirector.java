/* A CT director that utilizes multiple ODE solvers.

Copyright (c) 1998-2004 The Regents of the University of California.
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

package ptolemy.domains.ct.kernel;

import java.util.Iterator;

import ptolemy.actor.Actor;
import ptolemy.actor.AtomicActor;
import ptolemy.actor.sched.ScheduleElement;
import ptolemy.actor.util.Time;
import ptolemy.data.BooleanToken;
import ptolemy.data.StringToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.domains.ct.kernel.util.TotallyOrderedSet;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.InternalErrorException;
import ptolemy.kernel.util.InvalidStateException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Nameable;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.Workspace;

//////////////////////////////////////////////////////////////////////////
//// CTMultiSolverDirector
/**
   A CTDirector that uses multiple ODE solvers. The reason for using different
   solvers is that we need to handle both normal integration over ODEs and 
   abrupt changes in signals or actors' functions where the state of the system 
   has to be treated specially.
   <p> 
   At the time points where abrupt changes happen, a special ODE solver, called 
   "breakpointODESolver", is used. Typically, breakpointODESolvers do not 
   advance time. The job for a breakpointODESolver is to find the final 
   states of the system at breakpoints. 
   <P>
   This director handles both predictable breakpoints, whose appearance can be
   told before the simulation, and unpredictable breakpoints, whose appearance 
   is not known beforehand.
   <P>
   This director can only be a top-level director. For a CT model as an opaque 
   composite actor inside another model, use CTMixedSignalDirector (if the outer
   model is a discrete-event model) or CTEmbeddedDirector (if the outer model is
   a CT or FSM one.)
   <P>
   This director recognizes actors that implement the CTStepSizeControlActor
   interface and adjusts the step size by polling such actors. If all actors 
   are content with the current step size, then it attempts to raise the
   step size. If any actor is not satisfied with the current step size, then 
   this director reduces the step size. A special case is that if there are 
   no such actors, then this director uses 5 times of the current step size 
   or the maximum step size, whichever is smaller.
   <P>
   This director has two more parameters than the CTDirector base class.<BR>
   <UL>
   <LI><I>ODESolver</I>: This is the name of the normal ODE solver
   used for iterations of normal integration over ODEs.
   <LI><I>breakpointODESolver</I>: This is the name of the ODE solver used 
   in iterations at breakpoint. The breakpoint ODE solvers does not need 
   history information (this property is called self-start). The default solver 
   is "ptolemy.domains.ct.kernel.solver.DerivativeResolver".
   </UL>

   @see ptolemy.domains.ct.kernel.CTDirector
   @author  Jie Liu, Haiyang Zheng
   @version $Id$
   @since Ptolemy II 0.2
   @Pt.ProposedRating Yellow (hyzheng)
   @Pt.AcceptedRating Red (hyzheng)
*/
public class CTMultiSolverDirector extends CTDirector {
    /** Construct a director in the default workspace with an empty string
     *  as its name. The director is added to the list of objects in
     *  the workspace. Increment the version number of the workspace.
     *  All the parameters take their default values.
     */
    public CTMultiSolverDirector() {
        super();
    }

    /** Construct a director in the given container with the given name.
     *  The container argument must not be null, or a
     *  NullPointerException will be thrown.
     *  If the name argument is null, then the name is set to the
     *  empty string. Increment the version number of the workspace.
     *  All the parameters take their default values.
     *  @param container The container.
     *  @param name Name of this director.
     *  @exception IllegalActionException If the director is not compatible
     *   with the specified container.  May be thrown in derived classes.
     *  @exception NameDuplicationException If the container is not a
     *   CompositeActor and the name collides with an entity in the container.
     */
    public CTMultiSolverDirector(CompositeEntity container, String name)
            throws IllegalActionException, NameDuplicationException {
        super(container, name);
    }

    /** Construct a director in the  workspace with an empty name.
     *  The director is added to the list of objects in the workspace.
     *  Increment the version number of the workspace.
     *  All the parameters take their default values.
     *  @param workspace The workspace of this object.
     */
    public CTMultiSolverDirector(Workspace workspace) {
        super(workspace);
    }

    ///////////////////////////////////////////////////////////////////
    ////                         parameters                        ////

    /** The name of the ODE solver that is used in the breakpoint iterations. 
     *  The default solver is 
     *  "ptolemy.domains.ct.kernel.solver.DerivativeResolver".
     */
    public Parameter breakpointODESolver;

    /** The name of the normal ODE solver used in iterations for 
     *  normal integration. The default solver is 
     *  "ptolemy.domains.ct.kernel.solver.ExplicitRK23Solver".
     */
    public Parameter ODESolver;

    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

    /** React to a change in an attribute. If the changed attribute matches
     *  a parameter of the director, then the corresponding private copy of the
     *  parameter value will be updated.
     *  In particular, if either the <i>ODESolver</i> or the
     *  <i>breakpointODESolver</i> parameter is changed, then
     *  the corresponding solver will be instantiated. If the ODEsolver
     *  instantiated is an instance of BreakpointODESolver, then
     *  an IllegalActionException will be thrown, and the original
     *  ODESolver will be unchanged.
     *  @param attribute The changed attribute.
     *  @exception IllegalActionException If the new solver can not be
     *  instantiated, or the change to other attributes is invalid.
     */
    public void attributeChanged(Attribute attribute)
            throws IllegalActionException {
        if (attribute == ODESolver) {
            if (_debugging) {
                _debug(getFullName() + " updating  ODE solver...");
            }
            _solverClassName =
                ((StringToken)ODESolver.getToken()).stringValue();
            ODESolver defaultSolver = _instantiateODESolver(_solverClassName);
            if (defaultSolver instanceof BreakpointODESolver) {
                throw new IllegalActionException(this, _solverClassName +
                        " can only be used as a breakpoint ODE solver.");
            }
            _normalSolver = defaultSolver;
            // FIXME: the following statement is unnecessary. Solver is reset
            // in the fire method (both continuous and discrete phase of 
            // executions)
            // _setCurrentODESolver(_normalSolver);
        } else if (attribute == breakpointODESolver) {
            if (_debugging) {
                _debug(getName() +" updating breakpoint solver...");
            }
            _breakpointSolverClassName =
                ((StringToken)breakpointODESolver.getToken()).stringValue();
            _breakpointSolver =
                _instantiateODESolver(_breakpointSolverClassName);
        } else {
            super.attributeChanged(attribute);
        }
    }

    /** Return false always, since this director cannot be an inside director.
     *  @return false.
     */
    public boolean canBeInsideDirector() {
        return false;
    }

    /** Return true since this director can be a top-level director.
     *  @return true.
     */
    public boolean canBeTopLevelDirector() {
        return true;
    }

    /** Fire the system for one iteration. One iteration is defined as
     *  solving the behavior of a system over a closed time interval [t_0, t_1].
     *  A complete iteration includes resolving final states at t_0, 
     *  resolving the initial states at t_1, and producing outputs. 
     *  FIXME: define initial and final states. Need to introduce tagged-signal
     *  model. This should go to the class comment.
     *  <P>
     *  To resolve the final states at a time point, say t_0, a discrete phase 
     *  of execution is performed. A discrete phase of execution at a time point 
     *  is a fixed-point iteration, where the fixed point is reached when no 
     *  more events exist nor will be generated at the time point. 
     *  To be concrete, at the beginning of a discrete phase execution, 
     *  event generators generate some events, which will be consumed by
     *  waveform generators. The outputs of the waveform generators may trigger 
     *  event generators to generate more events, which may in turn change the
     *  outputs of waveform generators. The whole execution stops only when no 
     *  event generators generate any more events. At this ending of this 
     *  execution, the system states are resolved, which are called the final 
     *  states at t_0.
     *  <P>
     *  A continuous phase of execution immidiately follows a discrete one, 
     *  which solves the initial states at t_1. The initial states at t_1 are 
     *  resolved by the resolveStates() method of a normal ODE solver. This 
     *  process is a normal integration. 
     *  <p> 
     *  The ending time point t_1 of an iteration is determined by the step 
     *  sizes suggested by the step size control actors and the earliest entry
     *  in the breakpoint table. A correct step size is no greater than the 
     *  smallest suggested step size and the current time plus the step size 
     *  should not be later than the first entry in the breakpoint table.
     *  <P>
     *  Because of the existence of unpredictable events, a step size may need 
     *  refined. Another reason to adjust step size is to achieve a reasonably 
     *  accurate approximation of states. The mechanism to control step size is
     *  described below. After the states are resolved, step size control actors 
     *  in the dynamic actor schedule and the state transition schedule are 
     *  queried for the accuracy of the current step size. If any one of them 
     *  is not satisfied with the current step size, then the states will be 
     *  resolved with a refined step size, which is the minimum of the 
     *  refinedStepSize() from all step size control actors in the dynamic 
     *  actor schedule and the state transition schedule. On the other hand, 
     *  if all actors are satisfied with the current step, then the actors in 
     *  the output path will be fired according to the output schedule. Then, 
     *  the step size control actors in the output path will be checked for 
     *  accuracy. If any actor is not satisfied with the current step size, 
     *  the current step size is refined. Note that states have to resolved 
     *  again with this new step size. States are completely resolved only 
     *  when all actors agree that the step is accurate.
     *  <P>
     *  All the continuous actors are prefired before an iteration begins. 
     *  If any one of them returns false, then the iteration is
     *  cancelled, and the function returns.
     *
     *  @exception IllegalActionException If thrown a in discrete or continuous
     *  phase of execution.
     */
    public void fire() throws IllegalActionException {
        // A complete iteration consists of a discrete phase of execution 
        // and a continuous phase of execution. 
        if (_debugging && _verbose) {
            _debug(getName(), " fire: <<< ");
        }

        // FIXME: 
        // Why not setting the iteration begin time in the prefire method?
        // One possible source of bugs is that fire method changes model time?
        
        // set the start time of the current iteration
        _setIterationBeginTime(getModelTime());
        
        // discrete phase of execution to resolve the final states at the 
        // current time by processing discrete events.
        _discretePhaseExecution();

        // If the current time is the stop time, then the fire method 
        // should return because no further execution is necessary.
        if (getModelTime().equals(getModelStopTime())) {
            return;
        }
        
        // continuous phase execution to resolve the initial (starting) states 
        // in some future time by integration.
        _continuousPhaseExecution();

        if (_debugging && _verbose) {
            _debug(getName(), " end of fire. >>>");
        }
    }

    /** Fire event generators. This method is only called in a continuous
     *  phase of execution. Note that event generators are only fired but not
     *  postfired in this method. 
     *  @throws IllegalActionException If the schedule does not exist, 
     *  or any actor can not be prefired, or any actor throws it during firing. 
     */
    public void fireEventGenerators() throws IllegalActionException {
            CTSchedule schedule = (CTSchedule)getScheduler().getSchedule();
            Iterator eventGenerators =
                schedule.get(CTSchedule.EVENT_GENERATORS).actorIterator();
            _setExecutionPhase(CTExecutionPhase.FIRING_EVENT_GENERATORS_PHASE);
            while (eventGenerators.hasNext() && !_stopRequested) {
                Actor actor = (Actor)eventGenerators.next();
                if (!isPrefireComplete(actor)) {
                    setPrefireComplete(actor);
                    if (_debugging && _verbose) {
                        _debug("Prefire event generator: "
                                + ((Nameable)actor).getName()
                                + " at time "
                                + getModelTime());
                    }
                    if (!actor.prefire()) {
                        _setExecutionPhase(CTExecutionPhase.UNKNOWN_PHASE);
                        throw new IllegalActionException((Nameable)actor,
                                "Actor is not ready to fire. In the CT domain, "
                                + "all event generators should be ready to fire"
                                + " at all times.\n"
                                + "Does the actor only operate on sequence "
                                + "of tokens?");
                    }
                }
                if (_debugging) {
                    _debug("Fire event generator : "
                            + ((Nameable)actor).getName()
                            + " at time "
                            + getModelTime());
                }
                actor.fire();
            }
            _setExecutionPhase(CTExecutionPhase.UNKNOWN_PHASE);
        }

    /** Return the breakpoint ODE solver.
     *  @return The breakpoint ODE solver.
     */
    public ODESolver getBreakpointSolver() {
        return _breakpointSolver;
    }

    /** Return the normal ODE solver.
     *  @return The normal ODE solver
     */
    public ODESolver getODESolver() {
        return _normalSolver;
    }

    /** Return true if there is an event at current time.
     *  @return True if there is an event at current time.
     */
    public boolean hasCurrentEvent() {
        // NOTE: this can simplified by using the breakpoint table
        // Such as: getBreakPoints().contains(getCurrentTime())
        // As long as we implement the event generators in such a way
        // that events are generated only in discrete phase execution
        // and when an event is detected, a breakpoint is inserted.
        // NOTE: this causes a problem that multiple events may be 
        // generated during one continuous phase execution, while only
        // the first one is guaranteed to happen but the following ones may not 
        // occur in reality. So, we still need a mechanism to tell whether
        // events are detected. 
        // In summary, breakpoints and unpredicted events have to 
        // be dinstinguished.
        try {
            CTSchedule schedule = (CTSchedule)getScheduler().getSchedule();
            Iterator eventGenerators =
                schedule.get(CTSchedule.EVENT_GENERATORS).actorIterator();
            boolean hasDiscreteEvents = false;
            while (eventGenerators.hasNext()) {
                CTEventGenerator eventGenerator =
                    (CTEventGenerator) eventGenerators.next();
                // NOTE: We need to call hasCurrentEvent on all event 
                // generators, since some event generator may rely on it.
                // NOTE: This is probably not right... actors should not be
                // relying on this. 
                // FIXME: check the designs of CTEventGenerators, for example, 
                // {@link ptolemy.domains.ct.lib.LevelCrossingDetector} 
                // HasCurrentEvent method should be very simple, which returns
                // an internal state. The value of the state should not depend
                // on whether the hasCurrentEvent method has been called or not. 
                if (eventGenerator.hasCurrentEvent()) {
                    hasDiscreteEvents = true;
                }
            }
            // Check the breakpoint table for see if the the earliest breakpoint 
            // happens at the current time. 
            hasDiscreteEvents |= _processBreakpoints();
            return hasDiscreteEvents;
        } catch (IllegalActionException ex) {
            throw new InternalErrorException (
                    "Can not get a valid schedule: " +
                    ex.getMessage());
        }
    }

    /** Initialize the model for simulation. Construct a valid schedule. 
     *  Invoke the initialize() method of the super class to set current time 
     *  and initialize all the actors directed by this director. 
     *  Set the step size and the suggested next step size for the first firing. 
     *  Register both the current time and the stop time as breakpoints.
     *  This method is called after data types are resolved.
     *
     *  @exception IllegalActionException If the initialize method of the
     *  super class throws it, or neither the start time nor stop time can be
     *  registered as breakpoints.
     */
    public void initialize() throws IllegalActionException {
        if (_debugging) {
            _debug("----- Initializing: " + getFullName());
        }
        // NOTE: The initialization order of the model is that the container 
        // actor initializes first to solve signal types of ports partialy 
        // (including the signal types of input ports of subsystems); then the
        // contained actors are initialized, which should solve the signal 
        // types of the output ports the contained actors; 
        // lastly, the signal types of the rest ports of the container actor 
        // will be resolved.
        // NOTE: It is possible that the signal type of an input port of a
        // subsystem depends on one of its output ports, e.g., a feedback loop.
        // It may take several iterations (by going inside and out of the 
        // subsystem) to completely solve the signal type of all input and 
        // output ports. A fixed point can be defined as the signal type of all
        // ports are resolved and never changed.  
        // NOTE: A lattice is UNKNOWN -> DISCRETE | CONTINUOUS -> GENERAL, 
        // where if any port has either UNKNOWN or GENERAL signal type, there 
        // is something wrong with the model.     
        
        CTSchedule schedule = (CTSchedule)getScheduler().getSchedule();
        if (_debugging) {
            // Display schedule
            _debug(getFullName(), " A schedule is computed: ");
            _debug(schedule.toString());
        }

        // Initialize protected variables and the contained actors.
        super.initialize();

        // Set the current step size.
        setCurrentStepSize(getInitialStepSize());
        if (_debugging) {
            _debug("Set current step size to " + getCurrentStepSize());
        }
        
        // Set suggested next step size.
        setSuggestedNextStepSize(getInitialStepSize());
        if (_debugging) {
            _debug("Set suggested next step size to "
                    + getSuggestedNextStepSize());
        }

        // FIXME: the following is unnecessary.
        
        // register two breakpoints, the start time and stop time.
        // NOTE: the null argumenet for fireAt method indicates the director
        // requests the refiring.
        
        if (_debugging) {
            _debug("Set the current time as a breakpoint: "
                    + getModelTime());
        }
        fireAt(null, getModelTime());
        
        if (_debugging) {
            _debug("Set the stop time as a breakpoint: "
                    + getModelStopTime());
        }
        fireAt(null, getModelStopTime());
        
        if (_debugging) {
            _debug("----- End of Initialization of: " + getFullName());
        }
        
//        // establish the initial states for continuous states
//        _createIterationStartingStates();
//        _iterateEventGenerators(schedule);
    }

    /** Return false if the stop time is reached and no more actors need to be
     *  fired at that time, or if any actor returned false from its postfire() 
     *  method. Otherwise, returns true.
     *  @return false If the simulation is finished.
     *  @exception IllegalActionException If thrown by the super class, or the 
     *  current model time is bigger than the stop time. 
     */
    public boolean postfire() throws IllegalActionException {

        if (getModelTime().compareTo(getModelStopTime()) > 0) {
            // This should never happen. Otherwise, there is a bug. 
            // Any place an InternalErrorException happens, there
            // is (at least) one bug. 
            throw new IllegalActionException(
                "Execution can not exceed the stop time.");
        }
        
        // Now, the current time is equal to the stop time. 
        // If the breakpoints table does not contain the current model time,
        // which means no events are and will be generated,
        // the execution stops by returning false in this postfire method.
        if (getModelTime().equals(getModelStopTime()) &&
            !getBreakPoints().contains(getModelTime()))  {
            if (_debugging) {
                _debug("Postfire returns false at: " + getModelTime());
            }
            return false;
        }
        
        return super.postfire();
    }

    /** After performing the preinitialize() method of the super class,
     *  instantiate all the solvers.
     *  @exception IllegalActionException If thrown by the super class,
     *  or not all solvers can be instantiated, or the current solver 
     *  can not be set.
     */
    public void preinitialize() throws IllegalActionException {
        super.preinitialize();
        // Instantiate a efault solver, an ODE solver.
        if (_debugging) {
            _debug(getFullName(), 
                    " instantiating the ODE solver ", _solverClassName);
        }
        _normalSolver = _instantiateODESolver(_solverClassName);
        // Instantiate a breakpoint solver.
        if (_debugging) {
            _debug(getFullName(), "instantiating the breakpoint solver ", 
                    _breakpointSolverClassName);
        }
        _breakpointSolver = _instantiateODESolver(_breakpointSolverClassName);
        // FIXME: the following statement is unnecessary. Solver is reset
        // in the fire method (both continuous and discrete phase of 
        // executions)
        // Set the current ODE solver.
        _setCurrentODESolver(_normalSolver);
    }

    /** Establish the initial states for the first iteration.
     *  @return True if this director is ready to fire.
     *  @throws IllegalActionException If the initial states can not be
     *  created or the super class throws it.
     */
    public boolean prefire() throws IllegalActionException {
        if (_firstIteration) {
            // establish the initial states for continuous states
            _resolveFinalStates();
            _firstIteration = false;
        }
        return super.prefire();
    }

    /** Fire all the actors in the output schedule.  If they have not
     *  had prefire() called in the current iteration, then first
     *  call prefire().  The abstract semantics of Ptolemy II require that
     *  prefire() be called exactly once in an iteration.  This is important
     *  because, for example, time can only be tested reliably in prefire().
     *  It indicates the starting point of an integration step.
     *  During the following possibly multiple firings, time may progress,
     *  depending on the ODE solver used. 
     *  Hierarchies in CT and hybrid systems cases actually rely on 
     *  this fact to control internal step sizes.
     * 
     *  // FIXME: If we treat time as a state, it should not change until the
     *  postfire method! What is more, only the diretor is allowed to change it.
     * 
     *  @exception IllegalActionException If there is no schedule, or any 
     *  output actor can not be prefired or fired, or any output actor 
     *  returns false from its prefire method.
     */
    public void produceOutput() throws IllegalActionException {
        _setExecutionPhase(CTExecutionPhase.PRODUCING_OUTPUTS_PHASE);
        CTSchedule schedule = (CTSchedule)getScheduler().getSchedule();
        Iterator outputActors =
            schedule.get(CTSchedule.OUTPUT_ACTORS).actorIterator();
        while (outputActors.hasNext() && !_stopRequested) {
            Actor actor = (Actor)outputActors.next();
            if (!isPrefireComplete(actor)) {
                setPrefireComplete(actor);
                if (_debugging && _verbose) {
                    _debug("Prefire output actor: "
                            + ((Nameable)actor).getName()
                            + " at time "
                            + getModelTime());
                }
                if (!actor.prefire()) {
                    throw new IllegalActionException((Nameable)actor,
                            "Actor is not ready to fire. In the CT domain, "
                            + "all continuous actors should be ready to fire "
                            + "at all times.\n"
                            + "Does the actor only operate on sequence "
                            + "of tokens?");
                }
            }
            if (_debugging) {
                _debug("Fire output actor: "
                        + ((Nameable)actor).getName()
                        + " at time "
                        + getModelTime());
            }
            actor.fire();
        }
        _setExecutionPhase(CTExecutionPhase.UNKNOWN_PHASE);
    }

    /** Postfire event generators. This method is only called in a 
     *  continuous phase execution, after the current step size is accepted  
     *  by the output step size controllers. Note that discrete events must 
     *  not be generated at this phase. Instead, events can only be generated
     *  during a discrete phase of execution. 
     * 
     *  FIXME: This does not make much sense... Ideally, the event generators
     *  only generate events during discrete phase of execution.
     * 
     *  @throws IllegalActionException If the schedule does not exist, or 
     *  any actor throws it during postfiring.
     */
    public void postfireEventGenerators() throws IllegalActionException {
        CTSchedule schedule = (CTSchedule)getScheduler().getSchedule();
        Iterator eventGenerators =
            schedule.get(CTSchedule.EVENT_GENERATORS).actorIterator();
        _setExecutionPhase(CTExecutionPhase.POSTFIRING_EVENT_GENERATORS_PHASE);
        while (eventGenerators.hasNext() && !_stopRequested) {
            Actor actor = (Actor)eventGenerators.next();
            if (_debugging) {
                _debug("Postfire event generator : "
                        + ((Nameable)actor).getName()
                        + " at time "
                        + getModelTime());
            }
            _postfireReturns = _postfireReturns && actor.postfire();
        }
        _setExecutionPhase(CTExecutionPhase.UNKNOWN_PHASE);
    }

    /** Set a new value to the current time of the model, where the new
     *  time can be earlier than the current time to support rollback.
     *  This overrides the setCurrentTime() in the Director base class.
     *  This is a critical parameter in an execution, and the
     *  actors are not supposed to call it.
     *  @param newTime The new current simulation time.
     */
    public final void setModelTime(Time newTime) {
        // NOTE: this feature is necessary for the CTMixedSignalDirector
        // and CTEmbeddedDirector to roll back.
        // FIXME: why final?
        if (_debugging) {
            _debug("----- Setting current time to " + newTime);
        }
        _currentTime = newTime;
    }

    /** Call the postfire() method on all continuous actors in the schedule.
     *  For a correct CT simulation, the state of an actor can only change 
     *  at this stage of an iteration. Meanwhile, mark the current states 
     *  as a known good checkpoint.
     *  <p>
     *  If the <i>synchronizeToRealTime</i> parameter is <i>true</i>, 
     *  then this method will block execution until the real time catches 
     *  up with current model time. The units for time are seconds.
     * 
     *  @exception IllegalActionException If the synchronizeToRealTime 
     *  parameter does not have a valid token, or the sleep is interrupted,
     *  or there is not a schedule, or any of the actors in the schedule can
     *  not be postfired. 
     */
    public void updateContinuousStates() throws IllegalActionException {
        // Synchronize to real time if necessary.
        if (((BooleanToken)synchronizeToRealTime.getToken()).booleanValue()) {
            long realTime = System.currentTimeMillis() - _timeBase;
            long simulationTime = 
                (long)((getModelTime().subtract(getModelStartTime())
                        .getDoubleValue())*1000);
            if (_debugging) _debug("real time " + realTime,
                    "simulation time " + simulationTime);
            long timeDifference = simulationTime-realTime;
            if (timeDifference > 20) {
                try {
                    if (_debugging) {
                        _debug("Sleep for " + timeDifference + "ms");
                    }
                    Thread.sleep(timeDifference - 20);
                }  catch (Exception e) {
                    throw new IllegalActionException(this,
                            "Sleep Interrupted" + e.getMessage());
                }
            } else {
                if (_debugging) {
                    _debug("Warning: " + getFullName(), 
                            " cannot achieve real-time performance" +
                            " at simulation time " + getModelTime());
                }
            }
        }
        
        _setExecutionPhase(CTExecutionPhase.UPDATING_CONTINUOUS_STATES_PHASE);
        
        // Call the postfire method of the continuous actors.
        CTSchedule schedule = (CTSchedule)getScheduler().getSchedule();
        Iterator actors = schedule.get(
                CTSchedule.CONTINUOUS_ACTORS).actorIterator();
        while (actors.hasNext()) {
            Actor actor = (Actor)actors.next();
            if (_debugging) {
                _debug("Postfire " + (Nameable)actor);
            }
            _postfireReturns = _postfireReturns && actor.postfire();
        }
    
        // Mark the current state of the stateful actors.
        actors = schedule.get(
                CTSchedule.STATEFUL_ACTORS).actorIterator();
        while (actors.hasNext()) {
            CTStatefulActor actor = (CTStatefulActor)actors.next();
            if (_debugging) _debug("Postfire " + (Nameable)actor);
            actor.markState();
        }
    
        // FIXME: what about DE actors that have states??
        
        _setExecutionPhase(CTExecutionPhase.UNKNOWN_PHASE);
    }

    ///////////////////////////////////////////////////////////////////
    ////                         protected methods                 ////

    /** This method performs a continuous phase of execution. At this phase,
     *  a normal ODE solver tries to solve the initial states at a futhre time 
     *  t_1, based on the inputs and final states at t_0, where t_0 < t_1. 
     *  At the end of this method, outputs are generated. 
     *  <p>
     *  Note that t_1 can be adjusted by the normal ODE solver wirh respect to
     *  the accuracy requirements from step size control actors. Therefore,
     *  this method starts with a guess of t_1, or a integration step size 
     *  (t_1 - t_0). If all step size control actors are satisfied with the 
     *  guess, this method returns. Otherwise, the normal ODE solver chooses 
     *  a smaller step size and tries to solve the states. Therefore, this 
     *  method ends up with either resolved states or an exception complaining 
     *  that states can not be resolved even with the preconfigured minimum 
     *  step size. 
     *  
     *  @exception IllegalActionException If ODESolver can not be set or
     *  one of the continuous actors throws it.
     */
    protected void _continuousPhaseExecution() throws IllegalActionException {
        if (_debugging) {
            _debug("!!! continuous phase execution at " + getModelTime());
        }
        
        // Choose a suggested step size, which is a guess.        
        setCurrentStepSize(getSuggestedNextStepSize());
        
        // Refine the correct step size for the continuous phase execution
        // with respect to the breakpoint table.
        setCurrentStepSize(_refinedStepWRTBreakpoints());
        
        // Set the ODE solver to a normal ODE solver to do integration.
        _setCurrentODESolver(getODESolver());
        
        if (_debugging) {
            _debug("execute the system from "
                + getModelTime() + " step size " + getCurrentStepSize()
                + " using solver " + getCurrentODESolver().getName());
        }
        
        // Resolve the initial states at a future time 
        // (the current time plus the current step size).
        _resolveInitialStates();
    }
    
    /** Process all discrete events happening at the same model time.  
     *  The execution repeats executing event generators, purely discrete 
     *  actors, wavefore generators, and continuous actors, until the execution 
     *  reaches a fixed point, where no more events happen at the current time. 
     *  @exception IllegalActionException If one of the actors throws it or
     *  the schedule does not exist.
     */
    protected void _discretePhaseExecution() throws IllegalActionException {
        _setDiscretePhase(true);

        if (_debugging) {
            _debug("!!! discrete phase execution at " + getModelTime());
        }

        // Choose a suggested step size
        setCurrentStepSize(getSuggestedNextStepSize());

        CTSchedule schedule = (CTSchedule)getScheduler().getSchedule();

        while (hasCurrentEvent() && _postfireReturns) {
            
            if (_debugging) {
                _debug("     Iterate the discrete phase execution once ...");
            }
            
            // NOTE: a schedule where all the actors are topologically sorted
            // may be necessary to accelerate the process to reach a fixed point.

            if (_debugging) {
                _debug("  ---> " + getName(), 
                ": generating more events (if any) (continuous -> discrete)");
            }
            _iterateEventGenerators(schedule);
            
            if (_debugging) {
                _debug("  ---> " + getName(), 
                ": iterating pure discrete actors (discrete -> discrete)");
            }
            _iteratePurelyDiscreteActors(schedule);
        
            if (_debugging) {
                _debug("  ---> " + getName(), 
                ": iterating waveform generators (discrete -> continuous)");
            }
            _iterateWaveformGenerators(schedule);
            
            if (_debugging) {
                _debug("  ---> " + getName(), 
                ": creating starting states for continuous phase execution");
            }
            // NOTE: Output schedule does not contain event generators
            // because they are iterated separately.
            _resolveFinalStates();

            // NOTE: 
            // Events generated by event generators may not actually happen. 
            // The reason is that several event generators can generate events
            // in a continuous phase of execution, and these events happen at 
            // different times. However, only the first event is guaranteed to
            // happen and the rest may never occur in reality. 
            // The solution we take is to hold all events and use the 
            // hasCurrentEvent to queuy the existence of events. But we only
            // advance time to when the first event happens.
            
            // NOTE: If more events are generated by event generators, 
            // a new iteration at the current time is necessary. 
            // Otherwise, a fixed point is reached.
            
            // At the end of the _createIterationStartingStates() method,
            // the final states at the current model time, or 
            // the starting states for the immediately following continuous 
            // phase of execution are created.
            
            // FIXME: make all event generators call fireAt when events are
            // to be generated. The events are only generated at discrete phase.
            // FIXME: the above statement is wrong, for example, the 
            // CTPeriodicSampler generates discrete events at the end of 
            // a continuous phase of execution. FIXIT!!
            // FIXME: the difference between predictable and unpredictable event?
        }
        
        if (_debugging) {
            _debug(" >>> The next breakpoint is at " 
                + getBreakPoints().first() + ", which is in the future.");
            _debug(" >>> The fix point of the current discrete " +
                "phase execution is reached.");
        }
        // We are leaving discrete phase execution...
        _setDiscretePhase(false);
    }

    /** Initialize parameters to their default values.
     */
    protected void _initParameters() {
        super._initParameters();
        try {
            _solverClassName =
                "ptolemy.domains.ct.kernel.solver.ExplicitRK23Solver";
            ODESolver = new Parameter(
                    this, "ODESolver", new StringToken(_solverClassName));
            ODESolver.setTypeEquals(BaseType.STRING);
            _breakpointSolverClassName =
                "ptolemy.domains.ct.kernel.solver.DerivativeResolver";
            breakpointODESolver = new Parameter(this, "breakpointODESolver",
                    new StringToken(_breakpointSolverClassName));
            breakpointODESolver.setTypeEquals(BaseType.STRING);
        } catch (IllegalActionException e) {
            //Should never happens. The parameters are always compatible.
            throw new InternalErrorException("Parameter creation error.");
        } catch (NameDuplicationException ex) {
            throw new InvalidStateException(this, 
                    "Parameter name duplication.");
        }
    }

    /** Return true if all step size control actors in the output
     *  schedule agree that the current step is accurate.
     *  @return True if all step size control actors agree with the current
     *  step size.
     */
    protected boolean _isOutputAccurate() {
        if (_debugging) {
            _debug("Check accuracy of outputs for step size control actors:");
        }
        boolean accurate = true;
        
        // Get all the output step size control actors.
        Iterator actors;
        try {
            CTSchedule schedule = (CTSchedule)getScheduler().getSchedule();
            actors = schedule.get(
                    CTSchedule.OUTPUT_STEP_SIZE_CONTROL_ACTORS).actorIterator();
        } catch (IllegalActionException e) {
            throw new InternalErrorException("Can not get schedule.");
        }
        
        // Ask all the actors whether the current step size is accurate.
        // NOTE: all actors are guranteed to be asked once even if some 
        // actors already set the "accurate" variable to false.
        // The reason is that event generators do not check the step size 
        // accuracy in their fire emthods and they need to check existence
        // of events in the special isOutputAccurate() method.
        // FIXME: this is not a good way to imiplement event generators!!!
        while (actors.hasNext()) {
            CTStepSizeControlActor actor =
                (CTStepSizeControlActor) actors.next();
            boolean thisAccurate = actor.isOutputAccurate();
            if (_debugging) {
                _debug("  Checking output step size control actor: "
                        + ((NamedObj)actor).getName()
                        + ", which returns " + thisAccurate);
            }
            accurate = accurate && thisAccurate;
        }
        if (_debugging) {
            _debug("Overall output accuracy result: " + accurate);
        }
        return accurate;
    }

    /** Return true if all step size control actors in the dynamic actor
     *  schedule and the state transition schedule agree that
     *  the current step size is accurate.
     *  @return True if all state step size control actors agree with the 
     *  current step size.
     */
    protected boolean _isStateAccurate() {
        if (_debugging) {
            _debug("Checking accuracy for state step size control actors:");
        }
        boolean accurate = true;

        // Get all the output step size control actors.
        Iterator actors;
        try {
            CTSchedule schedule = (CTSchedule)getScheduler().getSchedule();
            actors = schedule.get(
                    CTSchedule.STATE_STEP_SIZE_CONTROL_ACTORS).actorIterator();
        } catch (IllegalActionException e) {
            throw new InternalErrorException("Can not get schedule.");
        }

        // Ask all the actors whether the current step size is accurate.
        // NOTE: all actors are guranteed to be asked once.
        while (actors.hasNext()) {
            CTStepSizeControlActor actor =
                (CTStepSizeControlActor) actors.next();
            boolean thisAccurate = actor.isStateAccurate();
            if (_debugging) {
                _debug("  Checking state step size control actor: "
                        + ((NamedObj)actor).getName()
                        + ", which returns " + thisAccurate);
            }
            accurate = accurate && thisAccurate;
        }
        if (_debugging) {
            _debug("Overall state accuracy result: " + accurate);
        }
        return accurate;
    }

    // NOTE: The following methods are protected because they are
    // also used by CTEmbeddedDirector.

    /** Iterate all purely discrete-event actors. Purely discrete-event actors
     *  are those take discrete signals as inputs and generate discrete
     *  signals as outputs. 
     *  @param schedule The schedule that contains purely discrete-event actors.
     *  @throws IllegalActionException If any actor can not be iterated.
     */
    protected void _iteratePurelyDiscreteActors(CTSchedule schedule) 
        throws IllegalActionException {
        _setExecutionPhase(CTExecutionPhase.ITERATING_PURELY_DISCRETE_PHASE);
        _iterateSchedule(schedule.get(CTSchedule.DISCRETE_ACTORS));
        _setExecutionPhase(CTExecutionPhase.UNKNOWN_PHASE);
    }

    /** Iterate all event generators . 
     *  @param schedule The schedule that contains event generators.
     *  @throws IllegalActionException If any actor can not be iterated.
     */
    protected void _iterateEventGenerators(CTSchedule schedule) 
        throws IllegalActionException {
        _setExecutionPhase(CTExecutionPhase.GENERATING_EVENTS_PHASE);
        _iterateSchedule(schedule.get(CTSchedule.EVENT_GENERATORS));
        _setExecutionPhase(CTExecutionPhase.UNKNOWN_PHASE);
    }

    /** Iterate all wave generators. 
     *  @param schedule The schedule that contains wave generators.
     *  @throws IllegalActionException If any actor can not be iterated.
     */
    protected void _iterateWaveformGenerators(CTSchedule schedule) 
        throws IllegalActionException {
        _setExecutionPhase(CTExecutionPhase.GENERATING_WAVEFORMS_PHASE);
        _iterateSchedule(schedule.get(CTSchedule.WAVEFORM_GENERATORS));
        _setExecutionPhase(CTExecutionPhase.UNKNOWN_PHASE);
    }

    /** Predict the next step size. This method should be called if the
     *  current integration step is accurate to estimate the step size
     *  for the next step. The predicted step size
     *  is the minimum of all predictions from step size control actors,
     *  and it never exceeds 10 times this step size.
     *  If there are no step-size control actors at all, then return
     *  the current step size times 5 or the maximum step size.  
     *  @return the prediced next step size.
     *  @exception IllegalActionException If the scheduler throws it.
     */
    protected double _predictNextStepSize() throws IllegalActionException {
        // The assumption here is that if the current phase of execution is 
        // not discrete, it must be continuous. 
        if (!isDiscretePhase()) {
            double predictedStep = 10.0*getCurrentStepSize();
            boolean foundOne = false;
            CTSchedule schedule = (CTSchedule)getScheduler().getSchedule();
            Iterator actors = schedule.get(
                    CTSchedule.STATE_STEP_SIZE_CONTROL_ACTORS).actorIterator();
            while (actors.hasNext()) {
                CTStepSizeControlActor actor =
                    (CTStepSizeControlActor) actors.next();
                predictedStep = Math.min(predictedStep,
                        actor.predictedStepSize());
                foundOne = true;
            }
            actors = schedule.get(
                    CTSchedule.OUTPUT_STEP_SIZE_CONTROL_ACTORS).actorIterator();
            while (actors.hasNext()) {
                CTStepSizeControlActor actor =
                    (CTStepSizeControlActor) actors.next();
                predictedStep = Math.min(predictedStep,
                        actor.predictedStepSize());
                foundOne = true;
            }
            if (foundOne) {
                return predictedStep;
            } else {
                // We multiple the step size by 5.0.
                // This is an agressive guess.
                predictedStep = getCurrentStepSize() * 5.0; 
                if (predictedStep <= getMaxStepSize()) {
                    return Math.min(predictedStep, getMaxStepSize());
                }
                return getCurrentStepSize();
            }
        } else {
            // Use the initial step size for breakpoint solver.
            return getInitialStepSize();
        }
    }

    /** Invoke prefire() on all DYNAMIC_ACTORS, such as integrators,
     *  and emit their tentative outputs.
     *  Return true if all the prefire() methods return true and stop()
     *  is not called. Otherwise, return false.  Note that prefire()
     *  is called on all actors even if one returns false.
     *  @return True if all dynamic actors return true from their prefire 
     *  method and stop() is called.
     *  @exception IllegalActionException If scheduler throws it, or dynamic
     *  actors throw it in their prefire method, or they can not be prefired.
     */
    protected boolean _prefireDynamicActors()
            throws IllegalActionException {
        
        // FIXME: can we treat dynamic actors also as waveform generators?
        
        CTSchedule schedule = (CTSchedule)getScheduler().getSchedule();
        boolean result = true;
        Iterator actors = schedule.get(
                CTSchedule.DYNAMIC_ACTORS).actorIterator();
                
        _setExecutionPhase(CTExecutionPhase.PREFIRING_DYNAMIC_ACTORS_PHASE);
        while (actors.hasNext() && !_stopRequested) {
            Actor actor = (Actor)actors.next();
            if (_debugging) {
                _debug("Prefire dynamic actor: " + ((Nameable)actor).getName());
            }
            boolean ready = false;
            // Atomic actors call prefire method, and  
            // CT composite actors call both prefire and fire method. 
            // FIXME: this is not a good design. how does the lower level
            // inform the upper level that some actor can not be prefired?
            // This should go to the prefire() method of this director.
            if (actor instanceof AtomicActor) {
                ready = actor.prefire();
            } else {
                actor.prefire();
                actor.fire();
                ready = true;
            }
            if (!ready) {
                _setExecutionPhase(CTExecutionPhase.UNKNOWN_PHASE);
                throw new IllegalActionException((Nameable)actor,
                        "Actor is not ready to fire. In the CT domain, all "
                        + "dynamic actors should be ready to fire at "
                        + "all times.\n "
                        + "Does the actor only operate on sequence of tokens?");
            }
            if (_debugging) {
                _debug("Prefire of " + ((Nameable)actor).getName() 
                        + " returns " + ready);
            }
            result = result && ready;
        }

        // NOTE: Need for integrators to emit their current output so that
        // the state transition actors can operate on the most up-to
        // date inputs and generate derivatives for integrators.  
        // Also, without this, on the first round of firing, the state 
        // transition actors will complain that inputs are not ready.
        Iterator integrators =
            schedule.get(CTSchedule.DYNAMIC_ACTORS).actorIterator();
        while (integrators.hasNext() && !_stopRequested) {
            CTDynamicActor dynamic = (CTDynamicActor)integrators.next();
            if (_debugging) {
                _debug("Emit tentative state " + ((Nameable)dynamic).getName());
            }
            dynamic.emitTentativeOutputs();
        }

        _setExecutionPhase(CTExecutionPhase.UNKNOWN_PHASE);
        
        return result && !_stopRequested;
    }

    /** Return true if the current time is the first element in the 
     *  breakpoint table. Remove that element from the breakpoint table.
     *  @return true if the current time is a breakpoint.
     *  @exception IllegalActionException If a breakpoint is missed.
     */
    protected boolean _processBreakpoints() throws IllegalActionException  {
        // FIXME: rename it. or seperate it into two methods: 
        // process breakpoints and compare the breakpoint with the current time.
        boolean currentTimeIsABreakpoint = false;
        TotallyOrderedSet breakPoints = getBreakPoints();
        Time now = getModelTime();
        // If the current time is a breakpoint, remove it from table.
        if (breakPoints != null && !breakPoints.isEmpty()) {
            if (breakPoints.contains(now)) {
                // The current time is a break point.
                currentTimeIsABreakpoint = true;
                Time time = (Time) breakPoints.removeFirst();
                if (time.compareTo(now) < 0) {
                    // This should never happen, because it is a bug
                    // to have breakpoints earlier than the current time...
                    throw new InternalErrorException("The first break point " +
                        "is in the past.");
                }
                if (_debugging) _debug(
                    "\n >>> Remove " + now + " from the break-point list.");
            } 
        }
        return currentTimeIsABreakpoint;
    }

    /** Return the refined the step size with respect to the output actors.
     *  All the step size control actors in the state transition and dynamic 
     *  schedule are queried for a refined step size. The smallest one is 
     *  returned. 
     *  @return The refined step size.
     *  @exception IllegalActionException If the scheduler throws it or the
     *  refined step size is less than the time resolution.
     */
    protected double _refinedStepWRTOutput() throws IllegalActionException {
        if (_debugging) {
            _debug("Refining the current step size w.r.t. all output actors:");
        }

        double refinedStep = getCurrentStepSize();
        CTSchedule schedule = (CTSchedule)getScheduler().getSchedule();
        Iterator actors = schedule.get(
                CTSchedule.OUTPUT_STEP_SIZE_CONTROL_ACTORS).actorIterator();
        while (actors.hasNext()) {
            CTStepSizeControlActor actor =
                (CTStepSizeControlActor)actors.next();
            refinedStep = Math.min(refinedStep, actor.refinedStepSize());
        }
        if (refinedStep < 0.5*getTimeResolution()) {
            throw new IllegalActionException(this,
                    "The refined step size is less than the minimum time "
                    + "resolution, at time " + getModelTime());
        }

        if (_debugging && _verbose) {
            _debug(getFullName(), "refine step with respect to output to"
                    + refinedStep);
        }
        _setIterationEndTime(getModelTime().add(refinedStep));
        return refinedStep;
    }

    /** Return the refined step size with respect to resolving the
     *  new state.
     *  All the step size control actors in the state transition
     *  and dynamic actor schedule are queried for a refined step size. Then
     *  the smallest one is returned. 
     *  @return The refined step size.
     *  @exception IllegalActionException If the scheduler throws it, or the
     *  refined step size is less than the minimum step size parameter 
     *  of the director.
     */
    protected double _refinedStepWRTState() throws IllegalActionException {
        if (_debugging) {
            _debug("Refining the current step size w.r.t. all state actors:");
        }
        double refinedStep = getCurrentStepSize();
        CTSchedule schedule = (CTSchedule)getScheduler().getSchedule();
        Iterator actors = schedule.get(
                CTSchedule.STATE_STEP_SIZE_CONTROL_ACTORS).actorIterator();
        while (actors.hasNext()) {
            CTStepSizeControlActor actor =
                (CTStepSizeControlActor)actors.next();
            double size = actor.refinedStepSize();
            if (_debugging) {
                _debug(((NamedObj)actor).getName(),
                    "refines step size to "
                    + size);
            }
            refinedStep = Math.min(refinedStep, size);
        }
        if (refinedStep < 0.5*getMinStepSize()) {
            throw new IllegalActionException(this,
                    "Cannot resolve new states even using "+
                    "the minimum step size, at time "+ getModelTime());
        }
        _setIterationEndTime(getModelTime().add(refinedStep));
        return refinedStep;
    }

    /** Use breakpoint ODE solver to resolve the final states at the current 
     *  time. These final states are the starting states for the following 
     *  continuous phase of execution.
     *  @exception IllegalActionException If one of the actors throws it or
     *  the ODE solver can not be set.
     */
    protected void _resolveFinalStates() 
        throws IllegalActionException {
        if (_debugging) {
            _debug("       creating the starting states for "
                + "the following continuous execution...");
        }
        // set a breakpoint solver
        _setCurrentODESolver(_breakpointSolver);
        // start creating the states
        _setExecutionPhase(CTExecutionPhase.CREATING_STARTING_STATES_PHASE);
        // NOTE: The step size used in the breakpoint iteration will be 
        // set by the breakpoint ODE solver. There is no need to set the 
        // step size here. 
        // A simplified fireOneIteration is performed, where
        // no step size refinement is needed. Two important facts: 
        // states can always be resolved; outputs are always correct.
        CTSchedule schedule = (CTSchedule)getScheduler().getSchedule();
        _fireExactlyOneIteration();
        _setExecutionPhase(CTExecutionPhase.UNKNOWN_PHASE);
    }

    /** Resolve the initial states with a normal ODE solver at a futhre time. 
     *  The future time is the current time puls the step size used by the 
     *  solver. Return immediately if any actor returns false in their 
     *  prefire() method. After this method is called, time advances to the 
     *  future time.
     *  @exception IllegalActionException If one the actors throws it
     *  in its execution methods.
     */
    protected void _resolveInitialStates() throws IllegalActionException {
        if (_debugging && _verbose) {
            _debug("Resolving the initial states at " + getModelTime(),
                " (current time) plus step size " + getCurrentStepSize());
        }
        ODESolver solver = getCurrentODESolver();
        if (_debugging && _verbose) {
            _debug( "Using ODE solver: " + solver.getName());
        }
        // NOTE: This used to execute prefire() methods all at once before
        // anything else happened.  But this was too early.
        // The input data is not yet present. 
        // Instead, we execute it before the first fire() in the iteration
        // for all actors except DYNAMIC actors (like integrators).
        // FIXME: The previous note should does not make sense. Continuous 
        // signals should always have values. This should be guaranteed at 
        // the initialize() method.
        
        // FIXME: the call to resolveStates() below increments time prior
        // to calling prefire() for the actors, so current time when
        // prefire is not (as it was before) the start time of the
        // iteration.  It is (unfortunately) the first guess as to the
        // end time of the iteration.  If this needs to be changed,
        // then probably the easiest place to do this is where prefire()
        // is called (which includes in ODESolver), by setting time back
        // to the start of the iteration. EAL 1/13/03
        
        // The easiest solution is to only allow directors to advance time.
        // However, this requires the redesign of ODE solvers.
        
        prefireClear();
        // FIXME: the following statement may not be necessary since the
        // _creatingIterationStartingStates method already creates the
        // history information. 
        // prefire dynamic actors (intergrators actually) to produce temporary
        // inputs for state transition actors.
        _prefireDynamicActors();
        
        // If stop is not requested, keep trying to resolve states, where
        // the current step size may get refined if the resolved 
        // states and outputs are inaccurate.
        while (!_stopRequested) {
            while (!_stopRequested) {
               
                // FIXME: this can be moved to the end of this while loop.
                // The reset of model time to the iteration begin time
                // can also be done this way.
                // Restore the saved state of the stateful actors.
                CTSchedule schedule = (CTSchedule)getScheduler().getSchedule();
                Iterator actors = schedule.get(
                        CTSchedule.STATEFUL_ACTORS).actorIterator();
                while (actors.hasNext()) {
                    CTStatefulActor actor = (CTStatefulActor)actors.next();
                    if (_debugging) 
                        _debug("Restore states " + (Nameable)actor);
                    actor.goToMarkedState();
                }
    
                // Reset the round counts and the convergencies to false.
                // NOTE: some solvers have their convergencies depending on 
                // the round counts. For example, it takes 3 rounds for a
                // RK-23 solver to solve states.
                solver.resetRoundCount();
                solver._setConverged(false);
                // repeating resolving states until states converge.
                while (!solver.isConverged() && solver.resolveStates()) {
                    // fire dynamic actors
                    _setExecutionPhase(
                        CTExecutionPhase.FIRING_DYNAMIC_ACTORS_PHASE);
                    solver.fireDynamicActors();
                    _setExecutionPhase(CTExecutionPhase.UNKNOWN_PHASE);
                    // NOTE: at exactly this point, time is advanced.
                    // The amount of advance depends on the current ODE solver.
                    _setExecutionPhase(
                        CTExecutionPhase.FIRING_STATE_TRANSITION_ACTORS_PHASE);
                    // fire state transition actors to calculate derivatives
                    solver.fireStateTransitionActors();
                    _setExecutionPhase(CTExecutionPhase.UNKNOWN_PHASE);
                }
                // NOTE: solver.resolveStates() returns true if the states are
                // resolved and returns false if the maximum iterations have 
                // been reached but the states have not be resolved.
                if (solver.resolveStates()) {
                    if (_debugging && _verbose) 
                        _debug("State resolved by solver.");
                    // Check whether this step is acceptable
                    if (!_isStateAccurate()) {
                        setModelTime(getIterationBeginTime());
                        setCurrentStepSize(_refinedStepWRTState());
                        if (_debugging && _verbose) {
                            _debug("Execute the system from "
                                    + getModelTime()
                                    + " with a smaller step size" +
                                    getCurrentStepSize());
                        }
                    } else {
                        // states are resolved sucessfully.
                        break;
                    }
                } else {
                    // Resolve state failed within the maximum number
                    // of iterations. e.g. in implicit methods.
                    if (getCurrentStepSize() < 0.5*getMinStepSize()) {
                        throw new IllegalActionException(this,
                                "Cannot resolve new states even using "+
                                "the minimum step size, at time "+
                        getModelTime());
                    }
                    setModelTime(getIterationBeginTime());
                    setCurrentStepSize(0.5*getCurrentStepSize());
                    if (_debugging && _verbose) {
                        _debug("Execute the system from "
                                + getModelTime()
                                + " with a smaller step size" +
                                getCurrentStepSize());
                    }
                }
            }
    
            // States have be resolved. Note that the current
            // time has been increased by the amount of the
            // step size used in this iteration.
    
            // If the while loop was terminated by the stop request,
            // jump out of the loop in spite of whether the states 
            // have been resolved or not.
            if (_stopRequested) {
                break;
            }
            
            // Iterate output actors to produce outputs.
            produceOutput();
            
            // NOTE: Event generators are fired but not postfired 
            // because the current step size may not be accurate. 
            // The event generators will be postfired when the current step
            // size is accurate. 
            fireEventGenerators();
    
            // If event generators are not satisfied with the current step size,
            // refine it to a smaller one.
            if (!_isOutputAccurate()) {
                setModelTime(getIterationBeginTime());
                setCurrentStepSize(_refinedStepWRTOutput());
                if (_debugging && _verbose) {
                    _debug("Refine the current step size"
                            + " with a smaller one " +
                            getCurrentStepSize());
                }
            } else {
                // outputs are generated successfully.
                break;
            }
       }
        
        // postfire all event generators to update their states.
        // NOTE: no events should be produced. Instead, they will be produced
        // at discrete phase of executions.
        postfireEventGenerators();
        
        // postfire all continuous actors to commit their states.
        updateContinuousStates();
    
        // predict the next step size.
        setSuggestedNextStepSize(_predictNextStepSize());
    }

    ///////////////////////////////////////////////////////////////////
    ////                         private methods                   ////

    // This method is used in the discrete phase of execution and initialize
    // method only. 
    // It is a simplified version of the _fireOneIteration method. 
    // In particular, no step size refinement is performed because states
    // can always be resolved and outputs are always accurate.
    private void _fireExactlyOneIteration() throws IllegalActionException {
        
        ODESolver solver = getCurrentODESolver();
        if (_debugging && _verbose) {
            _debug("Using breakpoint solver: " + solver.getName());
            _debug("Fire one iteration from " + getModelTime(),
                "using step size " + getCurrentStepSize());
        }
        
        prefireClear();
        
        _setExecutionPhase(CTExecutionPhase.PREFIRING_DYNAMIC_ACTORS_PHASE);
        _prefireDynamicActors();
        _setExecutionPhase(CTExecutionPhase.UNKNOWN_PHASE);

        // build history information. In particular, the derivative.
        _setExecutionPhase(CTExecutionPhase.FIRING_STATE_TRANSITION_ACTORS_PHASE);
        solver.fireStateTransitionActors();
        _setExecutionPhase(CTExecutionPhase.UNKNOWN_PHASE);

        _setExecutionPhase(CTExecutionPhase.FIRING_DYNAMIC_ACTORS_PHASE);
        solver.fireDynamicActors();
        _setExecutionPhase(CTExecutionPhase.UNKNOWN_PHASE);
        
        // Iterate output actors. 
        _setExecutionPhase(CTExecutionPhase.PRODUCING_OUTPUTS_PHASE);
        produceOutput();
        _setExecutionPhase(CTExecutionPhase.UNKNOWN_PHASE);

        // postfire all the continuous actors.
        _setExecutionPhase(CTExecutionPhase.UPDATING_CONTINUOUS_STATES_PHASE);
        updateContinuousStates();
        _setExecutionPhase(CTExecutionPhase.UNKNOWN_PHASE);

        // predict a possible next step size.
        setSuggestedNextStepSize(_predictNextStepSize());
    }

    // Iterate all the actors inside a given schedule, by prefiring, 
    // firing and postfiring them. 
    private void _iterateSchedule(ScheduleElement schedule) 
        throws IllegalActionException {
        Iterator actors = schedule.actorIterator();
        while (actors.hasNext()) {
            Actor actor = (Actor)actors.next();
            if (_debugging && _verbose) {
                _debug("Prefire actor: "
                        + ((Nameable)actor).getName()
                        + " at time "
                        + getModelTime());
            }
            if (actor.prefire()) {
                if (_debugging && _verbose) {
                    _debug("Fire actor: "
                            + ((Nameable)actor).getName()
                            + " at time "
                            + getModelTime());
                }
                actor.fire();
                if (_debugging && _verbose) {
                    _debug("Postfire actor: "
                            + ((Nameable)actor).getName()
                            + " at time "
                            + getModelTime());
                }
                _postfireReturns = actor.postfire() && _postfireReturns;
            }
        }
    }

    // Return the refined step size with respect to the breakpoints.
    // If the current time plus the current step size exceeds the
    // time of the next breakpoint, reduce the step size such that the next
    // breakpoint is the end time of the current iteration. 
    private double _refinedStepWRTBreakpoints() {

        double currentStepSize = getCurrentStepSize();
        Time iterationEndTime = getModelTime().add(currentStepSize);
        _setIterationEndTime(iterationEndTime);
            
        TotallyOrderedSet breakPoints = getBreakPoints();
        if (breakPoints != null && !breakPoints.isEmpty()) {
            if (_debugging && _verbose) {
                _debug(
                    "The first breakpoint in the breakpoint list is at "
                    + breakPoints.first());
            }
            // Adjust step size so that the first breakpoint is
            // not in the middle of this step.
            Time point = ((Time)breakPoints.first());
            if (iterationEndTime.compareTo(point) > 0) {
                currentStepSize 
                    = point.subtract(getModelTime()).getDoubleValue();
                if (_debugging && _verbose) {
                    _debug(
                    "Refining the current step size w.r.t. " 
                    + "the next breakpoint to " + currentStepSize);
                }
                _setIterationEndTime(point);
            }
        }
        return currentStepSize;
    }
    
    
    ///////////////////////////////////////////////////////////////////
    ////                         private variables                 ////

    // The breakpoint solver.
    private ODESolver _breakpointSolver;

    // The classname of the breakpoint ODE solver
    private String _breakpointSolverClassName;

    // The normal solver.
    private ODESolver _normalSolver;

    // The classname of the normal ODE solver.
    private String _solverClassName;
}
