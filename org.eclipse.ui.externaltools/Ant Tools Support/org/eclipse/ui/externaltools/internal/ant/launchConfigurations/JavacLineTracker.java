package org.eclipse.ui.externaltools.internal.ant.launchConfigurations;

/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp.  All rights reserved.
This file is made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html
**********************************************************************/

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.ui.console.FileLink;
import org.eclipse.debug.ui.console.IConsole;
import org.eclipse.debug.ui.console.IConsoleLineTracker;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.externaltools.model.StringMatcher;

/**
 * Generates hyperlinks for javac output
 */
public class JavacLineTracker implements IConsoleLineTracker {
	
	private IConsole fConsole;
	private StringMatcher fEclipseCompilerMatcher;
	private StringMatcher fJavacMatcher;

	/**
	 * Constructor for JavacLineTracker.
	 */
	public JavacLineTracker() {
		super();
	}

	/**
	 * @see org.eclipse.debug.ui.console.IConsoleLineTracker#init(org.eclipse.debug.ui.console.IConsole)
	 */
	public void init(IConsole console) {
		fConsole = console;
		fEclipseCompilerMatcher = new StringMatcher("*[javac]*ERROR in*.java*(at line*)*",false, false); //$NON-NLS-1$
		fJavacMatcher = new StringMatcher("*[javac] *.java:*:*",false, false); //$NON-NLS-1$
	}

	/**
	 * @see org.eclipse.debug.ui.console.IConsoleLineTracker#lineAppended(org.eclipse.jface.text.IRegion)
	 */
	public void lineAppended(IRegion line) {
		try {
			int lineOffset = line.getOffset();
			int lineLength = line.getLength();
			String text = fConsole.getDocument().get(lineOffset, lineLength);
			String fileName = null;
			String lineNumber = ""; //$NON-NLS-1$
			int fileStart = -1;
			if (fEclipseCompilerMatcher.match(text)) {
				int index = text.indexOf("ERROR in"); //$NON-NLS-1$
				if (index > 0) {
					fileStart = index + 9;
					index = text.lastIndexOf("(at line "); //$NON-NLS-1$
					if (index > 0) {
						int fileEnd = index - 1;
						int numberStart = index + 9;
						index = text.lastIndexOf(')');
						if (index > 0) {
							int numberEnd = index;
							fileName = text.substring(fileStart, fileEnd).trim();
							lineNumber = text.substring(numberStart, numberEnd).trim();
						}
					}
				}
			} else if (fJavacMatcher.match(text)) {
				fileStart = text.indexOf("[javac] "); //$NON-NLS-1$
				fileStart += 8;
				int index = text.indexOf(".java:", fileStart); //$NON-NLS-1$
				if (index > 0) {
					int numberStart = index + 6;
					fileName = text.substring(fileStart, numberStart - 1).trim();
					index = text.indexOf(":", numberStart); //$NON-NLS-1$
					if (index > numberStart) {
						lineNumber = text.substring(numberStart, index);
					}
				}
			}
			if (fileName != null) {
				int num = -1;
				try {
					num = Integer.parseInt(lineNumber);
				} catch (NumberFormatException e) {
				}
				IFile file = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(new Path(fileName));
				if (file != null && file.exists()) {
					FileLink link = new FileLink(file, null, -1, -1, num);
					fConsole.addLink(link, lineOffset + fileStart, lineLength - fileStart);
				}
			}
		} catch (BadLocationException e) {
		}
	}

	/**
	 * @see org.eclipse.debug.ui.console.IConsoleLineTracker#dispose()
	 */
	public void dispose() {
		fConsole = null;
	}

}
