package org.eclipse.debug.internal.ui;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.help.WorkbenchHelp;

/*
 * The page for setting the default debugger preferences.
 */
public class DebugPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage, IDebugPreferenceConstants {

	public DebugPreferencePage() {
		super(GRID);

		IPreferenceStore store= DebugUIPlugin.getDefault().getPreferenceStore();
		setPreferenceStore(store);
	}

	/**
	 * @see PreferencePage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		WorkbenchHelp.setHelp(
			parent,
			new Object[] { IDebugHelpContextIds.DEBUG_PREFERENCE_PAGE });
	}
	
	/**
	 * @see FieldEditorPreferencePage#createFieldEditors
	 */
	protected void createFieldEditors() {
		addField(new RadioGroupFieldEditor(IDebugPreferenceConstants.LAUNCHING_STYLE,
											"Launching style", 
											1,
											new String[][] {
												{"La&uncher-based", IDebugPreferenceConstants.LAUNCHING_STYLE_LAUNCHERS}, //$NON-NLS-1$
												{"C&onfiguration-based", IDebugPreferenceConstants.LAUNCHING_STYLE_CONFIGURATIONS} //$NON-NLS-1$
											},
											getFieldEditorParent()));
		addField(new BooleanFieldEditor(IDebugUIConstants.PREF_AUTO_BUILD_BEFORE_LAUNCH, DebugUIMessages.getString("DebugPreferencePage.auto_build_before_launch"), SWT.NONE, getFieldEditorParent())); //$NON-NLS-1$
		addField(new BooleanFieldEditor(IDebugUIConstants.PREF_SINGLE_CLICK_LAUNCHING, DebugUIMessages.getString("Enable_&single-click_launching_1"), SWT.NONE, getFieldEditorParent()));  //$NON-NLS-1$
		addField(new BooleanFieldEditor(IDebugUIConstants.PREF_AUTO_SHOW_DEBUG_VIEW, DebugUIMessages.getString("DebugPreferencePage.Show_Debug_Perspective_when_a_program_is_launched_in_de&bug_mode_1"), SWT.NONE, getFieldEditorParent())); //$NON-NLS-1$
		addField(new BooleanFieldEditor(IDebugUIConstants.PREF_AUTO_SHOW_PROCESS_VIEW, DebugUIMessages.getString("DebugPreferencePage.Show_Debug_Perspective_when_a_program_is_launched_in_&run_mode_2"), SWT.NONE, getFieldEditorParent())); //$NON-NLS-1$
		addField(new BooleanFieldEditor(CONSOLE_OPEN, DebugUIMessages.getString("DebugPreferencePage.Show_&Console_View_when_there_is_program_output_3"), SWT.NONE, getFieldEditorParent())); //$NON-NLS-1$
		addField(new BooleanFieldEditor(IDebugUIConstants.PREF_SHOW_VARIABLE_VALUE_CHANGES, DebugUIMessages.getString("DebugPreferencePage.Sho&w_variable_value_changes_in_Variables_and_Expressions_Views_1"), SWT.NONE, getFieldEditorParent())); //$NON-NLS-1$
		addField(new BooleanFieldEditor(IDebugUIConstants.PREF_AUTO_REMOVE_OLD_LAUNCHES, DebugUIMessages.getString("DebugPreferencePage.Remove_terminated_launches_when_a_new_launch_is_created_1"), SWT.NONE, getFieldEditorParent())); //$NON-NLS-1$
		addField(new RadioGroupFieldEditor(IDebugPreferenceConstants.VARIABLES_DETAIL_PANE_ORIENTATION,
											DebugUIMessages.getString("DebugPreferencePage.Orientation_of_detail_pane_in_variables_view_1"), //$NON-NLS-1$
											1,
											new String[][] {
												{DebugUIMessages.getString("DebugPreferencePage.To_the_right_of_variables_tree_pane_2"), IDebugPreferenceConstants.VARIABLES_DETAIL_PANE_RIGHT}, //$NON-NLS-1$
												{DebugUIMessages.getString("DebugPreferencePage.Underneath_the_variables_tree_pane_3"), IDebugPreferenceConstants.VARIABLES_DETAIL_PANE_UNDERNEATH} //$NON-NLS-1$
											},
											getFieldEditorParent()));
	}

	/**
	 * @see IWorkbenchPreferencePage#init(IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}
	
	protected static void initDefaults(IPreferenceStore store) {
		store.setDefault(IDebugPreferenceConstants.LAUNCHING_STYLE, IDebugPreferenceConstants.LAUNCHING_STYLE_LAUNCHERS);
		store.setDefault(IDebugUIConstants.PREF_AUTO_BUILD_BEFORE_LAUNCH, true);
		store.setDefault(IDebugUIConstants.PREF_SINGLE_CLICK_LAUNCHING, true);
		store.setDefault(IDebugUIConstants.PREF_AUTO_SHOW_DEBUG_VIEW, true);
		store.setDefault(IDebugUIConstants.PREF_AUTO_SHOW_PROCESS_VIEW, true);
		store.setDefault(IDebugPreferenceConstants.CONSOLE_OPEN, true);
		store.setDefault(IDebugUIConstants.PREF_AUTO_REMOVE_OLD_LAUNCHES, false);
		store.setDefault(IDebugUIConstants.PREF_SHOW_VARIABLE_VALUE_CHANGES, true);
		store.setDefault(IDebugPreferenceConstants.VARIABLES_DETAIL_PANE_ORIENTATION, IDebugPreferenceConstants.VARIABLES_DETAIL_PANE_UNDERNEATH);
	}
}

