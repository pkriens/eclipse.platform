package org.eclipse.debug.core;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

/**
 * A status handler registers to handle a specific status - error
 * or otherwise. Provides a mechanism for separating core (headless)
 * function from UI interaction. The debug plug-in provides a
 * status handlers extension point, against which handlers can
 * register for specific status codes - identified by plug-in
 * identifier and plug-in specific status code. The interaction between
 * an object requiring a status handler (source), and the status handler
 * is defined by the source and handler.
 * <p>
 * For example, a launch configuration delegate might encounter a timeout
 * while launching an application. In this case the delegate could abort
 * or, via the use of a status handler, prompt the user to continue. This
 * allows the launcher to be implemented in a plug-in that does not require
 * UI support, and allows another (UI) plug-in to register a handler.
 * </p>
 * <p>
 * A status handler extension is defined in <code>plugin.xml</code>.
 * Following is an example definition of a status handler extension.
 * <pre>
 * &lt;extension point="org.eclipse.debug.core.statusHandlers"&gt;
 *   &lt;statusHandler 
 *      id="com.example.ExampleIdentifier"
 *      class="com.example.ExampleStatusHandler"
 *      plugin="com.example.ExamplePluginId"
 *      code="123"
 *   &lt;/statusHandler&gt;
 * &lt;/extension&gt;
 * </pre>
 * The attributes are specified as follows:
 * <ul>
 * <li><code>id</code> specifies a unique identifier for this status handler.</li>
 * <li><code>class</code> specifies the fully qualified name of the Java class
 *   that implements <code>IStatusHandler</code>.</li>
 * <li><code>plugin</code> plug-in identifier that corresponds to the
 *   plug-in of the status this handler is registered for (i.e.
 *   <code>IStatus.getPlugin()</code>).</li>
 * <li><code>code</code> specifies the status code this handler
 *   is registered for.</li>
 * </ul>
 * </p>
 * <p>
 * Clients may implement this interface.
 * </p>
 * @see DebugPlugin#getStatusHandler(IStatus)
 * @since 2.0
 */

public interface IStatusHandler {

	/**
	 * Notifies this status handler that the given status has been
	 * generated by the specified source object and requires resolution.
	 * 
	 * @param status the status to handle
	 * @param source the object delegating to this status handler
	 *   the given status
	 * @return an object representing the resolution of the status
	 * @exception CoreException if unable to resolve the status
	 */
	public Object handleStatus(IStatus status, Object source) throws CoreException;
}