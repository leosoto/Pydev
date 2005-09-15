package org.python.pydev.debug.model;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchListener;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.tasklist.ITaskListResourceAdapter;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.core.PydevDebugPrefs;
import org.python.pydev.debug.model.remote.AbstractDebuggerCommand;
import org.python.pydev.debug.model.remote.AbstractRemoteDebugger;
import org.python.pydev.debug.model.remote.RemoveBreakpointCommand;
import org.python.pydev.debug.model.remote.RunCommand;
import org.python.pydev.debug.model.remote.SetBreakpointCommand;
import org.python.pydev.debug.model.remote.ThreadListCommand;
import org.python.pydev.debug.model.remote.VersionCommand;
import org.python.pydev.plugin.PydevPlugin;

public abstract class AbstractDebugTarget extends PlatformObject implements IDebugTarget, ILaunchListener {
	
	protected IPath file;
	protected IThread[] threads;
	protected boolean disconnected = false;
	protected IStackFrame[] oldStackFrame;
	protected AbstractRemoteDebugger debugger;	
	protected ILaunch launch;
	
	public abstract boolean canTerminate();
	public abstract boolean isTerminated();
	public abstract void terminate() throws DebugException;
	
	public AbstractRemoteDebugger getDebugger() {
		return debugger;
	}
	
	public void debuggerDisconnected() {
		disconnected = true;
		fireEvent(new DebugEvent(this, DebugEvent.CHANGE));
	}
	
	public void launchAdded(ILaunch launch) {
		// noop
	}

	public void launchChanged(ILaunch launch) {
		// noop		
	}
	
	// From IDebugElement
	public String getModelIdentifier() {
		return PyDebugModelPresentation.PY_DEBUG_MODEL_ID;
	}
	// From IDebugElement
	public IDebugTarget getDebugTarget() {
		return this;
	}	
	
	public String getName() throws DebugException {
		if (file != null)
			return file.lastSegment();
		else
			return "unknown";
	}
	
	public boolean canResume() {
		for (int i=0; i< threads.length; i++)
			if (threads[i].canResume())
				return true;
		return false;
	}

	public boolean canSuspend() {
		for (int i=0; i< threads.length; i++)
			if (threads[i].canSuspend())
				return true;
		return false;
	}

	public boolean isSuspended() {
		return false;
	}

	public void resume() throws DebugException {
		for (int i=0; i< threads.length; i++)
			threads[i].resume();
	}

	public void suspend() throws DebugException {
		for (int i=0; i< threads.length; i++)
			threads[i].suspend();
	}
	
	public IThread[] getThreads() throws DebugException {
		if (debugger == null)
			return null;
		if (threads == null) {
			ThreadListCommand cmd = new ThreadListCommand(debugger, this);
			debugger.postCommand(cmd);
			try {
				cmd.waitUntilDone(1000);
				threads = cmd.getThreads();
			} catch (InterruptedException e) {
				threads = new IThread[0];
			}
		}
		return threads;
	}

	public boolean hasThreads() throws DebugException {
		return true;
	}

	public boolean supportsBreakpoint(IBreakpoint breakpoint) {
		return breakpoint instanceof PyBreakpoint;
	}
	
	public void breakpointAdded(IBreakpoint breakpoint) {
		try {
			if (breakpoint instanceof PyBreakpoint && ((PyBreakpoint)breakpoint).isEnabled()) {
				PyBreakpoint b = (PyBreakpoint)breakpoint;
				SetBreakpointCommand cmd = new SetBreakpointCommand(debugger, b.getFile(), b.getLine());
				debugger.postCommand(cmd);
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
		if (breakpoint instanceof PyBreakpoint) {
			PyBreakpoint b = (PyBreakpoint)breakpoint;
			RemoveBreakpointCommand cmd = new RemoveBreakpointCommand(debugger, b.getFile(), b.getLine());
			debugger.postCommand(cmd);
		}
	}

	public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
		if (breakpoint instanceof PyBreakpoint) {
			breakpointRemoved(breakpoint, null);
			breakpointAdded(breakpoint);
		}
	}

	public boolean supportsStorageRetrieval() {
		return false;
	}

	public IMemoryBlock getMemoryBlock(long startAddress, long length)
		throws DebugException {
		return null;
	}	

	/**
	 * When a command that originates from daemon is received,
	 * this routine processes it.
	 * The responses to commands originating from here
	 * are processed by commands themselves
	 */
	public void processCommand(String sCmdCode, String sSeqCode, String payload) {
		try {
            int cmdCode = Integer.parseInt(sCmdCode);
//            int seqCode = Integer.parseInt(sSeqCode);
            if (cmdCode == AbstractDebuggerCommand.CMD_THREAD_CREATED)
                processThreadCreated(payload);
            else if (cmdCode == AbstractDebuggerCommand.CMD_THREAD_KILL)
                processThreadKilled(payload);
            else if (cmdCode == AbstractDebuggerCommand.CMD_THREAD_SUSPEND)
                processThreadSuspended(payload);
            else if (cmdCode == AbstractDebuggerCommand.CMD_THREAD_RUN)
                processThreadRun(payload);
            else
                PydevDebugPlugin.log(IStatus.WARNING, "Unexpected debugger command" + sCmdCode, null); 
        } catch (Exception e) {
            PydevDebugPlugin.log(IStatus.ERROR, "Error processing: " + sCmdCode+"\npayload: "+payload, e); 
        }	
	}

	protected void fireEvent(DebugEvent event) {
		DebugPlugin manager= DebugPlugin.getDefault();
		if (manager != null) {
			manager.fireDebugEventSet(new DebugEvent[]{event});
		}
	}

	/**
	 * @return an existing thread with a given id (null if none)
	 */
	protected PyThread findThreadByID(String thread_id)  {		
		for (int i = 0; i < threads.length; i++)
			if (thread_id.equals(((PyThread)threads[i]).getId()))
				return (PyThread)threads[i];
		return null;
	}
	
	/**
	 * Add it to the list of threads
	 */
	private void processThreadCreated(String payload) {
		
		IThread[] newThreads;
		try {
			newThreads = XMLUtils.ThreadsFromXML(this, payload);
		} catch (CoreException e) {
			PydevDebugPlugin.errorDialog("Error in processThreadCreated", e);
			return;
		}

		// Hide Pydevd threads if requested
		if (PydevDebugPrefs.getPreferences().getBoolean(PydevDebugPrefs.HIDE_PYDEVD_THREADS)) {
			int removeThisMany = 0;
			for (int i=0; i< newThreads.length; i++)
				if (((PyThread)newThreads[i]).isPydevThread())
					removeThisMany++;
			if (removeThisMany > 0) {
				int newSize = newThreads.length - removeThisMany;
				if (newSize == 0)	// no threads to add
					return;
				else {
					IThread[] newnewThreads = new IThread[newSize];
					int ii = 0;
					for (int i =0; i< newThreads.length; i++)
						if (!((PyThread)newThreads[i]).isPydevThread())
							newnewThreads[ii++] = newThreads[i];
					newThreads = newnewThreads;
				}
			}
		}

		// add threads to the thread list, and fire event
		if (threads == null)
			threads = newThreads;
		else {
			IThread[] combined = new IThread[threads.length + newThreads.length];
			int i = 0;
			for (i = 0; i < threads.length; i++)
				combined[i] = threads[i];
			for (int j = 0; j < newThreads.length; i++, j++)
				combined[i] = newThreads[j];
			threads = combined;
		}
		// Now notify debugger that new threads were added
		for (int i =0; i< newThreads.length; i++) 
			fireEvent(new DebugEvent(newThreads[i], DebugEvent.CREATE));
	}
	
	// Remote this from our thread list
	private void processThreadKilled(String thread_id) {
		IThread threadToDelete = findThreadByID(thread_id);
		if (threadToDelete != null) {
			int j = 0;
			IThread[] newThreads = new IThread[threads.length - 1];
			for (int i=0; i < threads.length; i++)
				if (threads[i] != threadToDelete) 
					newThreads[j++] = threads[i];
			threads = newThreads;
			fireEvent(new DebugEvent(threadToDelete, DebugEvent.TERMINATE));
		}
	}

	private void processThreadSuspended(String payload) {
		Object[] threadNstack;
		try {
			threadNstack = XMLUtils.XMLToStack(this, payload);
		} catch (CoreException e) {
			PydevDebugPlugin.errorDialog("Error reading ThreadSuspended", e);
			return;
		}
		PyThread t = (PyThread)threadNstack[0];
		int reason = DebugEvent.UNSPECIFIED;
		String stopReason = (String) threadNstack[1];
		if (stopReason != null) {
			int stopReason_i = Integer.parseInt(stopReason);
			if (stopReason_i == AbstractDebuggerCommand.CMD_STEP_OVER ||
				stopReason_i == AbstractDebuggerCommand.CMD_STEP_INTO ||
				stopReason_i == AbstractDebuggerCommand.CMD_STEP_RETURN)
				reason = DebugEvent.STEP_END;
			else if (stopReason_i == AbstractDebuggerCommand.CMD_THREAD_SUSPEND)
				reason = DebugEvent.CLIENT_REQUEST;
			else if (stopReason_i == AbstractDebuggerCommand.CMD_SET_BREAK)
				reason = DebugEvent.BREAKPOINT;
			else {
				PydevDebugPlugin.log(IStatus.ERROR, "Unexpected reason for suspension", null);
				reason = DebugEvent.UNSPECIFIED;
			}
		}
		if (t != null) {
			IStackFrame stackFrame[] = (IStackFrame[])threadNstack[2]; 

            //verify if there are modifications...
            verifyModified( stackFrame );
            
            //set it as being old already...
			oldStackFrame = stackFrame;
			
			t.setSuspended(true, stackFrame );
			fireEvent(new DebugEvent(t, DebugEvent.SUSPEND, reason));		
		}
	}
	
    
    /**
     * @param stackFrames the stack frames that should be gotten as a map
     * @return a map with the id of the stack pointing to the stack itself
     */
    private Map<String, IStackFrame> getStackFrameArrayAsMap(IStackFrame[] stackFrames){
        HashMap<String, IStackFrame> map = new HashMap<String, IStackFrame>();
        for (int i = 0; i < stackFrames.length; i++) {
            PyStackFrame s = (PyStackFrame) stackFrames[i];
            map.put(s.getId(), s);
        }
        return map;
    }
    
    /**
     * verifies (and marks as modified) variables in the stack frame
     * @param stackFrame the array of stack-frames we are analizing
     */
	private void verifyModified(IStackFrame[] stackFrame) {
	    if( oldStackFrame!=null ) {
    		
            Map<String, IStackFrame> oldStackFrameArrayAsMap = getStackFrameArrayAsMap(oldStackFrame);
            for( int i=0; i<stackFrame.length; i++ ) {
    			PyStackFrame newFrame = (PyStackFrame)stackFrame[i];
    		
                IStackFrame oldFrame = oldStackFrameArrayAsMap.get(newFrame.getId());
                if(oldFrame != null){
					verifyVariablesModified( newFrame, (PyStackFrame) oldFrame );
    			}
    		}		
        }
	}

    /**
     * compares stack frames to check for modified variables (and mark them as modified in the new stack)
     * 
     * @param newFrame the new frame
     * @param oldFrame the old frame
     */
	private void verifyVariablesModified(PyStackFrame newFrame, PyStackFrame oldFrame ) {
		PyVariable newVariable = null;
        
		try {
            Map<String, IVariable> variablesAsMap = oldFrame.getVariablesAsMap();
			IVariable[] newFrameVariables = newFrame.getVariables();
            
            //we have to check for each new variable
            for( int i=0; i<newFrameVariables.length; i++ ) {
				newVariable = (PyVariable)newFrameVariables[i];
			
                PyVariable oldVariable = (PyVariable)variablesAsMap.get(newVariable.getName());
			
                if( oldVariable != null) {
					boolean equals = newVariable.getValueString().equals( oldVariable.getValueString() );
                    
                    //if it is not equal, it was modified
					newVariable.setModified( !equals );
                    
				}else{ //it didn't exist before...
				    newVariable.setModified( true );
                }
			}
            
		} catch (DebugException e) {		
			PydevPlugin.log(e);
		}
	}

	// thread_id\tresume_reason
	static Pattern threadRunPattern = Pattern.compile("(\\d+)\\t(\\w*)");
	/**
	 * ThreadRun event processing
	 */
	private void processThreadRun(String payload) {
		String threadID = "";
		int resumeReason = DebugEvent.UNSPECIFIED;
		Matcher m = threadRunPattern.matcher(payload);
		if (m.matches()) {
			threadID = m.group(1);
			try {
				int raw_reason = Integer.parseInt(m.group(2));
				if (raw_reason == AbstractDebuggerCommand.CMD_STEP_OVER)
					resumeReason = DebugEvent.STEP_OVER;
				else if (raw_reason == AbstractDebuggerCommand.CMD_STEP_RETURN)
					resumeReason = DebugEvent.STEP_RETURN;
				else if (raw_reason == AbstractDebuggerCommand.CMD_STEP_INTO)
					resumeReason = DebugEvent.STEP_INTO;
				else if (raw_reason == AbstractDebuggerCommand.CMD_THREAD_RUN)
					resumeReason = DebugEvent.CLIENT_REQUEST;
				else {
					PydevDebugPlugin.log(IStatus.ERROR, "Unexpected resume reason code", null);
					resumeReason = DebugEvent.UNSPECIFIED;
				}				
			}
			catch (NumberFormatException e) {
				// expected, when pydevd reports "None"
				resumeReason = DebugEvent.UNSPECIFIED;
			}
		}
		else
			PydevDebugPlugin.log(IStatus.ERROR, "Unexpected treadRun payload " + payload, null);
		
		PyThread t = (PyThread)findThreadByID(threadID);
		if (t != null) {
			t.setSuspended(false, null);
			fireEvent(new DebugEvent(t, DebugEvent.RESUME, resumeReason));
		}
	}
	
	/**
	 * Called after debugger has been connected. 
	 * 
	 * Here we send all the initialization commands
	 */
	public void initialize() {
		// we post version command just for fun
		// it establishes the connection
		debugger.postCommand(new VersionCommand(debugger));
		
		// now, register all the breakpoints in our project
		IFile launched[];
		if( file!=null ) {
			IFile temp = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(file);
			if(temp != null) {
				launched = new IFile[1];
				launched[0] = temp;
			}
			else {
				launched = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocation(file);
			}
			for(int ii = 0; ii != launched.length; ++ii) {
				IProject project = launched[ii].getProject();
				try {
					IMarker[] markers = project.findMarkers(PyBreakpoint.PY_BREAK_MARKER, true, IResource.DEPTH_INFINITE);
					IBreakpointManager breakpointManager= DebugPlugin.getDefault().getBreakpointManager();
					for (int i= 0; i < markers.length; i++) {
						PyBreakpoint brk = (PyBreakpoint)breakpointManager.getBreakpoint(markers[i]);
						if (brk.isEnabled()) {
							SetBreakpointCommand cmd = new SetBreakpointCommand(debugger, brk.getFile(), brk.getLine());
							debugger.postCommand(cmd);
						}
					}
				} catch (Throwable t) {
					PydevDebugPlugin.errorDialog("Error setting breakpoints", t);
				}
			}
		}
			
		// Send the run command, and we are off
		RunCommand run = new RunCommand(debugger);
		debugger.postCommand(run);
	}
	
	public boolean canDisconnect() {
		return !disconnected;
	}

	public void disconnect() throws DebugException {
		if (debugger != null) {
			debugger.disconnect();
		}
	}

	public boolean isDisconnected() {
		return disconnected;
	}
	
	public Object getAdapter(Class adapter) {		
		// Not really sure what to do here, but I am trying
		if (adapter.equals(ILaunch.class))
			return launch;
		else if (adapter.equals(IResource.class)) {
			// used by Variable ContextManager, and Project:Properties menu item
			if( file!=null ) {
				IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocation(file);
				if (files != null && files.length > 0)
					return files[0];
				else
					return null;
			}
		} else if (adapter.equals(IPropertySource.class))
			return launch.getAdapter(adapter);
		else if (adapter.equals(ITaskListResourceAdapter.class) 
				|| adapter.equals(org.eclipse.debug.ui.actions.IRunToLineTarget.class) 
				|| adapter.equals(org.eclipse.debug.ui.actions.IToggleBreakpointsTarget.class) 
				)
			return  super.getAdapter(adapter);
		// System.err.println("Need adapter " + adapter.toString());
		return super.getAdapter(adapter);
	}
}