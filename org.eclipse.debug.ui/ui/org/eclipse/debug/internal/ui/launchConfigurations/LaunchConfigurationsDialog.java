package org.eclipse.debug.internal.ui.launchConfigurations;

/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp.  All rights reserved.
This file is made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html
**********************************************************************/

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.PixelConverter;
import org.eclipse.debug.internal.ui.SWTUtil;
import org.eclipse.debug.internal.ui.preferences.IDebugPreferenceConstants;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.IDebugView;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.ILaunchConfigurationTabGroup;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.operation.ModalContext;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.ProgressMonitorPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.help.WorkbenchHelp;
 

/**
 * The dialog used to edit and launch launch configurations.
 */
public class LaunchConfigurationsDialog extends TitleAreaDialog implements ILaunchConfigurationDialog {

	/**
	 * Keep track of the currently visible dialog instance	 */
	private static ILaunchConfigurationDialog fgCurrentlyVisibleLaunchConfigurationDialog;
		
	/**
	 * The label appearing above tree of configs & config types.
	 */
	private Label fTreeLabel;
	
	/**
	 * The workbench context present when this dialog is opened.
	 */
	private Object fContext;
	
	/**
	 * The Composite used to insert an adjustable 'sash' between the tree and the tabs.
	 */
	private SashForm fSashForm;
	
	/**
	 * Default weights for the SashForm that specify how wide the selection and
	 * edit areas aree relative to each other.
	 */
	private static final int[] DEFAULT_SASH_WEIGHTS = new int[] {11, 30};
	
	/**
	 * The launch configuration selection area.
	 */
	private Composite fSelectionArea;
	
	/**
	 * Tree view of launch configurations
	 */
	private LaunchConfigurationView fLaunchConfigurationView;	
	
	/**
	 * Tab edit area
	 */
	private LaunchConfigurationTabGroupViewer fTabViewer;
	
	/**
	 * True while setting the input to the tab viewer
	 */
	private boolean fInitializingTabs;
	
	/**
	 * The launch configuration edit area.
	 */
	private Composite fEditArea;
	
	/**
	 * The 'New configuration' action.
	 */
	private ButtonAction fButtonActionNew;
		
	/**
	 * The 'Delete configuration' action.
	 */
	private ButtonAction fButtonActionDelete;
		
	/**
	 * The 'cancel' button that appears when the in-dialog progress monitor is shown.
	 */
	private Button fProgressMonitorCancelButton;
	
	/**
	 * Flag indicating if the progress monitor part's Cancel button has been pressed.
	 */
	private boolean fCancelButtonPressed;
			
	/**
	 * Clients of this dialog may set an 'initial configuration type', which means that when
	 * the dialog is opened, a configuration of that type will be created, initialized, and
	 * saved.  Note that the initial config type is ignored if single-click launching is enabled.
	 */
	private ILaunchConfigurationType fInitialConfigType;
	
	/**
	 * When this dialog is opened in <code>LAUNCH_CONFIGURATION_DIALOG_OPEN_ON_SELECTION</code>
	 * mode, this specifies the selection that is initially shown in the dialog.
	 */
	private IStructuredSelection fInitialSelection;
		
	private ProgressMonitorPart fProgressMonitorPart;
	private Cursor waitCursor;
	private Cursor arrowCursor;
	private MessageDialog fWindowClosingDialog;
	
	/**
	 * The number of 'long-running' operations currently taking place in this dialog
	 */	
	private long fActiveRunningOperations = 0;
	
	/**
	 * The launch groupd being displayed
	 */
	private LaunchGroupExtension fGroup;
	
	/**
	 * Banner image
	 */
	private Image fBannerImage;
	
	/**
	 * Double-click action
	 */
	private IAction fDoubleClickAction;
			
	/**
	 * Id for 'Launch' button.
	 */
	protected static final int ID_LAUNCH_BUTTON = IDialogConstants.CLIENT_ID + 1;
	
	/**
	 * Id for 'Close' button.
	 */
	protected static final int ID_CLOSE_BUTTON = IDialogConstants.CLIENT_ID + 2;
	
	/**
	 * Id for 'Cancel' button.
	 */
	protected static final int ID_CANCEL_BUTTON = IDialogConstants.CLIENT_ID + 3;
	
	/**
	 * Constrant String used as key for setting and retrieving current Control with focus
	 */
	private static final String FOCUS_CONTROL = "focusControl";//$NON-NLS-1$

	/**
	 * The height in pixels of this dialog's progress indicator
	 */
	private static int PROGRESS_INDICATOR_HEIGHT = 18;

	/**
	 * Constant specifying how wide this dialog is allowed to get (as a percentage of
	 * total available screen width) as a result of tab labels in the edit area.
	 */
	protected static final float MAX_DIALOG_WIDTH_PERCENT = 0.70f;
	
	/**
	 * Constant specifying how tall this dialog is allowed to get (as a percentage of
	 * total available screen height) as a result of preferred tab size.
	 */
	protected static final float MAX_DIALOG_HEIGHT_PERCENT = 0.60f;	

	/**
	 * Empty array
	 */
	protected static final Object[] EMPTY_ARRAY = new Object[0];	
	
	/**
	 * Size of this dialog if there is no preference specifying a size.
	 */
	protected static final Point DEFAULT_INITIAL_DIALOG_SIZE = new Point(620, 560);

	/**
	 * Status area messages
	 */
	protected static final String LAUNCH_STATUS_OK_MESSAGE = LaunchConfigurationsMessages.getString("LaunchConfigurationDialog.Ready_to_launch_2"); //$NON-NLS-1$
	protected static final String LAUNCH_STATUS_STARTING_FROM_SCRATCH_MESSAGE 
										= LaunchConfigurationsMessages.getString("LaunchConfigurationDialog.Select_a_configuration_to_launch_or_a_config_type_to_create_a_new_configuration_3"); //$NON-NLS-1$

	/**
	 * Constant specifying that the launch configuration dialog should not actually open,
	 * but instead should attempt to re-launch the last configuration that was sucessfully
	 * launched in the workspace.  If there is no last launched configuration, just open the dialog.
	 */
	public static final int LAUNCH_CONFIGURATION_DIALOG_LAUNCH_LAST = 0;
		
	/**
	 * Constant specifying that this dialog should be opened with the last configuration launched
	 * in the workspace selected.
	 */
	public static final int LAUNCH_CONFIGURATION_DIALOG_OPEN_ON_LAST_LAUNCHED = 2;

	/**
	 * Constant specifying that this dialog should be opened with the value specified via 
	 * <code>setInitialSelection()</code> selected.
	 */
	public static final int LAUNCH_CONFIGURATION_DIALOG_OPEN_ON_SELECTION = 3;
	
	/**
	 * Constant specifying that a new launch configuration dialog was not opened.  Instead
	 * an existing launch configuration dialog was used.	 */
	public static final int LAUNCH_CONFIGURATION_DIALOG_REUSE_OPEN = 4;
	
	/**
	 * Specifies how this dialog behaves when opened.  Value is one of the 
	 * 'LAUNCH_CONFIGURATION_DIALOG' constants defined in this class.
	 */
	private int fOpenMode = LAUNCH_CONFIGURATION_DIALOG_LAUNCH_LAST;
	
	/**
	 * Constructs a new launch configuration dialog on the given
	 * parent shell.
	 * 
	 * @param shell the parent shell
	 * @param group the group of lanuch configuration to display
	 */
	public LaunchConfigurationsDialog(Shell shell, LaunchGroupExtension group) {
		super(shell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		setLaunchGroup(group);
	}
	
	/**
	 * Set the flag indicating how this dialog behaves when the <code>open()</code> method is called.
	 * Valid values are defined by the LAUNCH_CONFIGURATION_DIALOG... constants in this class.
	 */
	public void setOpenMode(int mode) {
		fOpenMode = mode;
	}
	
	protected int getOpenMode() {
		return fOpenMode;
	}
	
	/**
	 * A launch configuration dialog overrides this method
	 * to create a custom set of buttons in the button bar.
	 * This dialog has 'Launch' and 'Cancel'
	 * buttons.
	 * 
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(Composite)
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, ID_LAUNCH_BUTTON, getLaunchButtonText(), true);
		createButton(parent, ID_CLOSE_BUTTON, LaunchConfigurationsMessages.getString("LaunchConfigurationDialog.Close_1"), false);  //$NON-NLS-1$
	}
	
	/**
	 * Handle the 'save and launch' & 'launch' buttons here, all others are handled
	 * in <code>Dialog</code>
	 * 
	 * @see Dialog#buttonPressed(int)
	 */
	protected void buttonPressed(int buttonId) {
		if (buttonId == ID_LAUNCH_BUTTON) {
			handleLaunchPressed();
		} else if (buttonId == ID_CLOSE_BUTTON) {
			handleClosePressed();
		} else {
			super.buttonPressed(buttonId);
		}
	}

	/**
	 * Returns the appropriate text for the launch button - run or debug.
	 */
	private String getLaunchButtonText() {
		if (getMode().equals(ILaunchManager.DEBUG_MODE)) {
			return LaunchConfigurationsMessages.getString("LaunchConfigurationDialog.Deb&ug_4"); //$NON-NLS-1$
		} else {
			return LaunchConfigurationsMessages.getString("LaunchConfigurationDialog.R&un_5"); //$NON-NLS-1$
		}
	}

	/**
	 * @see Dialog#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		Control contents = super.createContents(parent);
		initializeContent();
		return contents;
	}

	protected void initializeContent() {
		initializeWorkingSet();
		doInitialTreeSelection();
	}
	

	/**
	 * Initialize the relative weights (widths) of the 2 sides of the sash.
	 */
	private void initializeSashForm() {
		if (getSashForm() != null) {
			IDialogSettings settings = getDialogSettings();
			int[] sashWeights;
			try {
				int w1, w2;
				w1 = settings.getInt(IDebugPreferenceConstants.DIALOG_SASH_WEIGHTS_1);
				w2 = settings.getInt(IDebugPreferenceConstants.DIALOG_SASH_WEIGHTS_2);
				sashWeights = new int[] {w1, w2};
			} catch (NumberFormatException e) {
				sashWeights = DEFAULT_SASH_WEIGHTS;
			}
			getSashForm().setWeights(sashWeights);
		}
	}
	
	/**
	 * Retrieve the last working set in use and apply it to the tree.
	 */
	private void initializeWorkingSet() {
		IDialogSettings settings = getDialogSettings();
		String workingSetName = settings.get(IDebugPreferenceConstants.DIALOG_WORKING_SET);
		if (workingSetName != null) {
			IWorkingSet workingSet = getWorkingSetManager().getWorkingSet(workingSetName);
			if (workingSet != null) {
				getWorkingSetActionManager().setWorkingSet(workingSet, true);
			}
		}
	}

	/**
	 * Check if the selection area is currently wide enough so that both the 'New' &
	 * 'Delete' buttons are shown without truncation.  If so, do nothing.  Otherwise,
	 * increase the width of this dialog's Shell just enough so that both buttons 
	 * are shown cleanly.
	 */
	private void ensureSelectionAreaWidth() {
		if (fLaunchConfigurationView != null) {
			Button newButton = getButtonActionNew().getButton();
			Button deleteButton = getButtonActionDelete().getButton();		
			int requiredWidth = newButton.getBounds().width + deleteButton.getBounds().width;
			int marginWidth = ((GridLayout)getSelectionArea().getLayout()).marginWidth;
			int horizontalSpacing = ((GridLayout)getSelectionArea().getLayout()).horizontalSpacing;
			requiredWidth += (2 * marginWidth) + horizontalSpacing;
			int currentWidth = getSelectionArea().getBounds().width;
	
			if (requiredWidth > currentWidth) {
				int[] newSashWeights = new int[2];
				newSashWeights[0] = requiredWidth;
				newSashWeights[1] = getEditArea().getBounds().width;
				Shell shell= getShell();
				Point shellSize= shell.getSize();
				setShellSize(shellSize.x + (requiredWidth - currentWidth), shellSize.y);
				getSashForm().setWeights(newSashWeights);			
			}
		}
	}
	
	/**
	 * Set the initial selection in the tree.
	 */
	public void doInitialTreeSelection() {
		fLaunchConfigurationView.getViewer().setSelection(getInitialSelection());
	}
	
	/**
	 * Write out this dialog's Shell size, location & sash weights to the preference store.
	 */
	protected void persistShellGeometry() {
		Point shellLocation = getShell().getLocation();
		Point shellSize = getShell().getSize();
		IDialogSettings settings = getDialogSettings();
		settings.put(IDebugPreferenceConstants.DIALOG_ORIGIN_X, shellLocation.x);
		settings.put(IDebugPreferenceConstants.DIALOG_ORIGIN_Y, shellLocation.y);
		settings.put(IDebugPreferenceConstants.DIALOG_WIDTH, shellSize.x);
		settings.put(IDebugPreferenceConstants.DIALOG_HEIGHT, shellSize.y);
	}
	
	protected void persistSashWeights() {
		IDialogSettings settings = getDialogSettings();
		SashForm sashForm = getSashForm();
		if (sashForm != null) {
			int[] sashWeights = getSashForm().getWeights();
			settings.put(IDebugPreferenceConstants.DIALOG_SASH_WEIGHTS_1, sashWeights[0]);
			settings.put(IDebugPreferenceConstants.DIALOG_SASH_WEIGHTS_2, sashWeights[1]);
		}
	}
	
	/**
	 * Store the current working set.
	 */
	private void persistWorkingSet() {
		if (getWorkingSetActionManager() != null) {
			IDialogSettings settings = getDialogSettings();
			IWorkingSet workingSet = getWorkingSetActionManager().getWorkingSet();
			String name = ""; //$NON-NLS-1$
			if (workingSet != null) {
				name = workingSet.getName();
			}
			settings.put(IDebugPreferenceConstants.DIALOG_WORKING_SET, name);
		}		
	}
	
	/**
	 * @see Window#close()
	 */
	public boolean close() {
		persistShellGeometry();
		persistSashWeights();
		persistWorkingSet();
		setCurrentlyVisibleLaunchConfigurationDialog(null);
		getBannerImage().dispose();
		getTabViewer().dispose();
		if (fLaunchConfigurationView != null) {
			fLaunchConfigurationView.dispose();
		}
		return super.close();
	}
	
	/**
	 * Determine the initial configuration for this dialog.  If the open mode is
	 * set to 'LAUNCH_LAST', relaunch the last config and return
	 * <code>ILaunchConfigurationDialog.LAUNCHED_BEFORE_OPENING</code>.
	 * Otherwise, open the dialog in the specified mode and return one of
	 * <code>Window. OK</code> or <code>Window.CANCEL</code>.
	 * 
	 * @see Window#open()
	 */
	public int open() {		
		int mode = getOpenMode();	
		if (mode == LAUNCH_CONFIGURATION_DIALOG_LAUNCH_LAST) {
			return doLastLaunchedConfig(true);
		} else if (mode == LAUNCH_CONFIGURATION_DIALOG_OPEN_ON_LAST_LAUNCHED) {
			return doLastLaunchedConfig(false);
		} else if (mode == LAUNCH_CONFIGURATION_DIALOG_OPEN_ON_SELECTION) {
			return openDialogOnSelection();
		}		
		return super.open();
	}
	
	/**
	 * Retrieve the last launched configuration in the workspace.  If <code>launch</code>
	 * is <code>true</code>, launch this configuration without showing the dialog, otherwise 
	 * just set the initial selection in the dialog to the last launched configuration.
	 */
	protected int doLastLaunchedConfig(boolean launch) {
		ILaunchConfiguration lastLaunchedConfig = getLastLaunchedWorkbenchConfiguration();
		if (launch) {
			try {
				if (lastLaunchedConfig != null) {
					if (lastLaunchedConfig.supportsMode(getMode())) {
						doLaunch(lastLaunchedConfig);
					} else {
						// If we're trying to launch, but the last launched config doesn't 
						// support the current mode of the dialog, show an error dialog
						String configName = lastLaunchedConfig.getName();
						String title = LaunchConfigurationsMessages.getString("LaunchConfigurationDialog.Cannot_relaunch_1"); //$NON-NLS-1$
						String message = MessageFormat.format(LaunchConfigurationsMessages.getString("LaunchConfigurationDialog.Cannot_relaunch_[{1}]_because_it_does_not_support_{2}_mode_2"), new String[] {configName, getMode()}); //$NON-NLS-1$
						MessageDialog.openError(getShell(), title, message);										
					}
					return ILaunchConfigurationDialog.LAUNCHED_BEFORE_OPENING;
				}
			} catch(CoreException e) {
				DebugUIPlugin.errorDialog(DebugUIPlugin.getShell(), LaunchConfigurationsMessages.getString("LaunchConfigurationDialog.Launch_Configuration_Error_6"), LaunchConfigurationsMessages.getString("LaunchConfigurationDialog.Exception_occurred_processing_launch_configuration._See_log_for_more_information_7"), e); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		setCurrentlyVisibleLaunchConfigurationDialog(this);
		if (lastLaunchedConfig != null) {
			setInitialSelection(new StructuredSelection(lastLaunchedConfig));
		}			
		return super.open();
	}

	/**
	 * Open this dialog with the selection set to the value specified by 
	 * <code>setInitialSelection()</code>.
	 */
	protected int openDialogOnSelection() {
		// Nothing special is required, the dialog will open and whatever was specified
		// via setInitialSelection() will be selected in the tree
		setCurrentlyVisibleLaunchConfigurationDialog(this);
		return super.open();
	}
	
	/**
	 * Return the last launched configuration in the workspace.
	 */
	protected ILaunchConfiguration getLastLaunchedWorkbenchConfiguration() {
		return DebugUIPlugin.getDefault().getLaunchConfigurationManager().getLastLaunch(getLaunchGroup().getIdentifier());
	}
	
	/**
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		Composite dialogComp = (Composite)super.createDialogArea(parent);
		addContent(dialogComp);
		return dialogComp;
	}

	/**
	 * Adds content to the dialog area
	 * 
	 * @param dialogComp
	 */
	protected void addContent(Composite dialogComp) {
		GridData gd;
		Composite topComp = new Composite(dialogComp, SWT.NONE);
		gd = new GridData(GridData.FILL_BOTH);
		topComp.setLayoutData(gd);
		GridLayout topLayout = new GridLayout();
		topLayout.numColumns = 2;
		topLayout.marginHeight = 5;
		topLayout.marginWidth = 0;
		topComp.setLayout(topLayout);
		
		// Set the things that TitleAreaDialog takes care of 
		setTitle(LaunchConfigurationsMessages.getString("LaunchConfigurationDialog.Create,_manage,_and_run_launch_configurations_8")); //$NON-NLS-1$
		setMessage(LaunchConfigurationsMessages.getString("LaunchConfigurationDialog.Ready_to_launch_2")); //$NON-NLS-1$
		setModeLabelState();
		
		// Create the SashForm that contains the selection area on the left,
		// and the edit area on the right
		setSashForm(new SashForm(topComp, SWT.NONE));
		getSashForm().setOrientation(SWT.HORIZONTAL);
		gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 2;
		getSashForm().setLayoutData(gd);
		
		// Build the launch configuration selection area and put it into the composite.
		Control launchConfigSelectionArea = createLaunchConfigurationSelectionArea(getSashForm());
		gd = new GridData(GridData.FILL_VERTICAL);
		launchConfigSelectionArea.setLayoutData(gd);
		
		// Build the launch configuration edit area and put it into the composite.
		Composite editAreaComp = createLaunchConfigurationEditArea(getSashForm());
		setEditArea(editAreaComp);
		gd = new GridData(GridData.FILL_BOTH);
		editAreaComp.setLayoutData(gd);
			
		// Build the separator line that demarcates the button bar
		Label separator = new Label(topComp, SWT.HORIZONTAL | SWT.SEPARATOR);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		separator.setLayoutData(gd);
		
		dialogComp.layout(true);
	}
	
	/**
	 * Set the title area image based on the mode this dialog was initialized with
	 */
	protected void setModeLabelState() {
		setTitleImage(getBannerImage());
	}
	
	/**
	 * Update buttons and message.
	 */
	protected void refreshStatus() {
		updateMessage();
		updateButtons();
	}
			
	protected Display getDisplay() {
		Shell shell = getShell();
		if (shell != null) {
			return shell.getDisplay();
		} else {
			return Display.getDefault();
		}
	}
		
	/**
	 * Creates the launch configuration selection area of the dialog.
	 * This area displays a tree of launch configurations that the user
	 * may select, and allows users to create new configurations, and
	 * delete and duplicate existing configurations.
	 * 
	 * @return the composite used for launch configuration selection area
	 */ 
	protected Control createLaunchConfigurationSelectionArea(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		setSelectionArea(comp);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.marginHeight = 0;
		layout.marginWidth = 5;
		comp.setLayout(layout);
		
		setTreeLabel(new Label(comp, SWT.NONE));
		GridData gd = new GridData();
		gd.horizontalSpan = 3;
		getTreeLabel().setLayoutData(gd);
		getTreeLabel().setText(LaunchConfigurationsMessages.getString("LaunchConfigurationDialog.Launch_Con&figurations__1")); //$NON-NLS-1$
		// TODO: tooltip		
		//updateTreeLabelTooltip();
		
		fLaunchConfigurationView = new LaunchConfigurationView(getLaunchGroup());
		fLaunchConfigurationView.createLaunchDialogControl(comp);
		Viewer viewer = fLaunchConfigurationView.getViewer();
		Control control = viewer.getControl();
		
		gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 3;
		// Set width hint to 0 to force tree to only be as wide as the combined
		// width of the 'New' & 'Delete' buttons.  Otherwise tree wants to be much wider.
		gd.widthHint = 0;
		control.setLayoutData(gd);
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			/**
			 * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
			 */
			public void selectionChanged(SelectionChangedEvent event) {
				handleLaunchConfigurationSelectionChanged(event);
			}
		});
		
		fDoubleClickAction = new DoubleClickAction();
		fLaunchConfigurationView.setAction(IDebugView.DOUBLE_CLICK_ACTION, fDoubleClickAction);
		
		Button newButton = SWTUtil.createPushButton(comp, LaunchConfigurationsMessages.getString("LaunchConfigurationDialog.Ne&w_13"), null); //$NON-NLS-1$
		setButtonActionNew(new ButtonActionNew(newButton.getText(), newButton));
		
		Button deleteButton = SWTUtil.createPushButton(comp, LaunchConfigurationsMessages.getString("LaunchConfigurationDialog.Dele&te_14"), null); //$NON-NLS-1$
		setButtonActionDelete(new ButtonActionDelete(deleteButton.getText(), deleteButton));
		
		AbstractLaunchConfigurationAction.IConfirmationRequestor requestor =
			new AbstractLaunchConfigurationAction.IConfirmationRequestor() {
					/**
					 * @see org.eclipse.debug.internal.ui.launchConfigurations.AbstractLaunchConfigurationAction.IConfirmationRequestor#getConfirmation()
					 */
					public boolean getConfirmation() {
						return canDiscardCurrentConfig();
					}
			};
			
		// confirmation requestors
		getDuplicateAction().setConfirmationRequestor(requestor);
		getNewAction().setConfirmationRequestor(requestor);
							
		return comp;
	}	
	
	/**
	 * Creates the launch configuration edit area of the dialog.
	 * This area displays the name of the launch configuration
	 * currently being edited, as well as a tab folder of tabs
	 * that are applicable to the launch configuration.
	 * 
	 * @return the composite used for launch configuration editing
	 */ 
	protected Composite createLaunchConfigurationEditArea(Composite parent) {
		setTabViewer(new LaunchConfigurationTabGroupViewer(parent, this));
		getTabViewer().addSelectionChangedListener(new ISelectionChangedListener() {
			/**
			 * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
			 */
			public void selectionChanged(SelectionChangedEvent event) {
				handleTabSelectionChanged(event);
			}
		});
		return (Composite)getTabViewer().getControl();
	}	
	
	/**
	 * @see Dialog#createButtonBar(Composite)
	 */
	protected Control createButtonBar(Composite parent) {
		Composite composite= new Composite(parent, SWT.NULL);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		GridLayout pmLayout = new GridLayout();
		pmLayout.numColumns = 3;
		setProgressMonitorPart(new ProgressMonitorPart(composite, pmLayout, PROGRESS_INDICATOR_HEIGHT));
		Button cancelButton = createButton(getProgressMonitorPart(), ID_CANCEL_BUTTON, LaunchConfigurationsMessages.getString("LaunchConfigurationDialog.Cancel_3"), true); //$NON-NLS-1$
		setProgressMonitorCancelButton(cancelButton);
		getProgressMonitorCancelButton().addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				setCancelButtonPressed(true);
			}
		});
		getProgressMonitorPart().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		getProgressMonitorPart().setVisible(false);

		return super.createButtonBar(composite);
	}
	
	/**
	 * Sets the title for the dialog, and establishes the help context.
	 * 
	 * @see org.eclipse.jface.window.Window#configureShell(Shell);
	 */
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText(getShellTitle());
		WorkbenchHelp.setHelp(
			shell,
			getHelpContextId());
	}
	
	protected String getHelpContextId() {
		return IDebugHelpContextIds.LAUNCH_CONFIGURATION_DIALOG;
	}
	
	protected String getShellTitle() {
		String title = getLaunchGroup().getLabel();
		if (title == null) {
			title = LaunchConfigurationsMessages.getString("LaunchConfigurationDialog.Launch_Configurations_18"); //$NON-NLS-1$
		}
		return title;		
	}
	
	/**
	 * @see Window#getInitialLocation(Point)
	 */
	protected Point getInitialLocation(Point initialSize) {
		IDialogSettings settings = getDialogSettings();
		try {
			int x, y;
			x = settings.getInt(IDebugPreferenceConstants.DIALOG_ORIGIN_X);
			y = settings.getInt(IDebugPreferenceConstants.DIALOG_ORIGIN_Y);
			return new Point(x,y);
		} catch (NumberFormatException e) {
		}
		return super.getInitialLocation(initialSize);
	}

	/**
	 * @see Window#getInitialSize()
	 */
	protected Point getInitialSize() {		
		IDialogSettings settings = getDialogSettings();
		try {
			int x, y;
			x = settings.getInt(IDebugPreferenceConstants.DIALOG_WIDTH);
			y = settings.getInt(IDebugPreferenceConstants.DIALOG_HEIGHT);
			return new Point(x, y);
		} catch (NumberFormatException e) {
		}
		return DEFAULT_INITIAL_DIALOG_SIZE;
	}
		
	private void setSashForm(SashForm sashForm) {
		fSashForm = sashForm;
	}
	
	private SashForm getSashForm() {
		return fSashForm;
	}

	/**
	 * Returns the launch manager.
	 * 
	 * @return the launch manager
	 */
	private ILaunchManager getLaunchManager() {
		return DebugPlugin.getDefault().getLaunchManager();
	}

	private IWorkingSetManager getWorkingSetManager() {
		return PlatformUI.getWorkbench().getWorkingSetManager();
	}

	/**
	 * Returns whether this dialog is currently open
	 */
	private boolean isVisible() {
		return getShell() != null && getShell().isVisible();
	}	
		
	/**
	 * Notification that selection has changed in the launch configuration tree.
	 * <p>
	 * If the currently displayed configuration is not saved,
	 * prompt for saving before moving on to the new selection.
	 * </p>
	 * 
	 * @param event selection changed event
	 */
 	protected void handleLaunchConfigurationSelectionChanged(SelectionChangedEvent event) {
 		
 		Object input = getTabViewer().getInput();
 		Object newInput = null;
 		ISelection selection = event.getSelection();
 		if (!selection.isEmpty()) {
 			if (selection instanceof IStructuredSelection) {
 				IStructuredSelection structuredSelection = (IStructuredSelection)selection;
 				if (structuredSelection.size() == 1) {
 					newInput = structuredSelection.getFirstElement();
 				}
 			}
 		}
 		ILaunchConfiguration original = getTabViewer().getOriginal();
 		if (original != null && newInput == null && getLaunchManager().getMovedTo(original) != null) {
			// the current config is about to be deleted ignore this change
			return;
		}
 		updateButtons();
 		
 		if (!isEqual(input, newInput)) {
 			ILaunchConfigurationTabGroup group = getTabGroup();
 			if (original != null) {
 				boolean deleted = !original.exists();
 				boolean renamed = false;
 				if (newInput instanceof ILaunchConfiguration) {
 					ILaunchConfiguration lc = (ILaunchConfiguration)newInput;
 					renamed = getLaunchManager().getMovedFrom(lc) != null;
 				}
	 			if (getTabViewer().isDirty() && !deleted && !renamed) {
	 				boolean canReplace = showSaveChangesDialog();
	 				if (!canReplace) {
	 					// restore the original selection
	 					IStructuredSelection sel = new StructuredSelection(getTabViewer().getOriginal());
	 					fLaunchConfigurationView.getViewer().setSelection(sel);
	 					return;
	 				}
	 			}
 			}
 			setInitializingTabs(true);
 			getTabViewer().setInput(newInput);
 			setInitializingTabs(false);
 			refreshStatus();
 			// bug 14758 - if the newly selected config is dirty, save its changes
 			if (getTabViewer().isDirty()) {
 				getTabViewer().handleApplyPressed();
 			} 
 			// bug 14758			
 			ILaunchConfigurationTabGroup newGroup = getTabGroup();
 			if (!isEqual(group, newGroup)) {
 				if (isVisible()) {
 					resize();
 				}
 			}
 		}
  	}
  	
  	protected boolean isEqual(Object o1, Object o2) {
  		if (o1 == o2) {
  			return true;
  		} else if (o1 == null) {
  			return false;
  		} else {
  			return o1.equals(o2);
  		}
  	}
  	
  	
  	protected void resize() {
		// determine the maximum tab dimensions
		PixelConverter pixelConverter = new PixelConverter(getEditArea());
		int runningTabWidth = 0;
		ILaunchConfigurationTabGroup group = getTabGroup();
		if (group == null) {
			return;
		}
		ILaunchConfigurationTab[] tabs = group.getTabs();
		Point contentSize = new Point(0, 0);
		for (int i = 0; i < tabs.length; i++) {
			String name = tabs[i].getName();
			Image image = tabs[i].getImage();
			runningTabWidth += pixelConverter.convertWidthInCharsToPixels(name.length() + 5);
			if (image != null) {
				runningTabWidth += image.getBounds().width;
			}
			Control control = tabs[i].getControl();
			if (control != null) {
				Point size = control.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
				if (size.x > contentSize.x) {
					contentSize.x = size.x;
				}
				if (size.y > contentSize.y) {
					contentSize.y = size.y;
				}
			}
		}
	
		// Determine if more space is needed to show all tab labels across the top of the
		// tab folder.  If so, only increase size of dialog to some percent of the available
		// screen real estate.
		if (runningTabWidth > contentSize.x) {
			int maxAllowedWidth = (int) (getDisplay().getBounds().width * MAX_DIALOG_WIDTH_PERCENT);
			int otherWidth = getSashForm().SASH_WIDTH + getSelectionArea().getBounds().width;
			int totalWidth = runningTabWidth + otherWidth;
			if (totalWidth > maxAllowedWidth) {
				contentSize.x = maxAllowedWidth - otherWidth;
			} else {
				contentSize.x = runningTabWidth;
			}
		}
		
		int maxAllowedHeight =(int) (getDisplay().getBounds().height * MAX_DIALOG_HEIGHT_PERCENT);
		contentSize.y = Math.min(contentSize.y, maxAllowedHeight);
	
		// Adjust the maximum tab dimensions to account for the extra space required for the tab labels
		Rectangle tabFolderBoundingBox = getEditArea().computeTrim(0, 0, contentSize.x, contentSize.y);
		contentSize.x = tabFolderBoundingBox.width;
		contentSize.y = tabFolderBoundingBox.height;
	
		// Force recalculation of sizes
		getEditArea().layout(true);
	
		// Calculate difference between required space for tab folder and current size,
		// then increase size of this dialog's Shell by that amount
		Rectangle rect = getEditArea().getClientArea();
		Point containerSize= new Point(rect.width, rect.height);
		int hdiff= contentSize.x - containerSize.x;
		int vdiff= contentSize.y - containerSize.y;
		// Only increase size of dialog, never shrink it
		if (hdiff > 0 || vdiff > 0) {
			int[] newSashWeights = null;
			if (hdiff > 0) {
				newSashWeights = calculateNewSashWeights(hdiff);
			}
			hdiff= Math.max(0, hdiff);
			vdiff= Math.max(0, vdiff);
			Shell shell= getShell();
			Point shellSize= shell.getSize();
			setShellSize(shellSize.x + hdiff, shellSize.y + vdiff);
			// Adjust the sash weights so that all of the increase in width
			// is given to the tab area
			if (newSashWeights != null) {
				getSashForm().setWeights(newSashWeights);
			}
		}  		
	}
  	
	/**
	 * Notification that tab selection has changed.
	 *
	 * @param event selection changed event
	 */
	protected void handleTabSelectionChanged(SelectionChangedEvent event) {
		refreshStatus();
	}
	 	
 	private void setInitializingTabs(boolean init) {
 		fInitializingTabs = init;
 	}
 	
 	protected boolean isInitializingTabs() {
 		return fInitializingTabs;
 	}
 	
 	private void setProgressMonitorPart(ProgressMonitorPart part) {
 		fProgressMonitorPart = part;
 	}
 	
 	private ProgressMonitorPart getProgressMonitorPart() {
 		return fProgressMonitorPart;
 	}
 	
 	private void setProgressMonitorCancelButton(Button button) {
 		fProgressMonitorCancelButton = button;
 	}
 	
 	private Button getProgressMonitorCancelButton() {
 		return fProgressMonitorCancelButton;
 	}
 	 	 	
 	/**
 	 * Calculate & return a 2 element integer array that specifies the relative 
 	 * weights of the selection area and the edit area, based on the specified
 	 * increase in width of the owning shell.  The point of this method is calculate 
 	 * sash weights such that when the shell gets wider, all of the increase in width
 	 * is given to the edit area (tab folder), and the selection area (tree) stays
 	 * the same width.
 	 */
	private int[] calculateNewSashWeights(int widthIncrease) {
		int[] newWeights = new int[2];
		newWeights[0] = getSelectionArea().getBounds().width;
		newWeights[1] = getEditArea().getBounds().width + widthIncrease;
		return newWeights;
	}

 	/**
 	 * Increase the size of this dialog's <code>Shell</code> by the specified amounts.
 	 * Do not increase the size of the Shell beyond the bounds of the Display.
 	 */
	protected void setShellSize(int width, int height) {
		Rectangle bounds = getShell().getDisplay().getBounds();
		getShell().setSize(Math.min(width, bounds.width), Math.min(height, bounds.height));
	}
 	 
 	/** 
 	 * @see ILaunchConfigurationDialog#getMode()
 	 */
 	public String getMode() {
 		return getLaunchGroup().getMode();
 	}
 	 	 	
 	/**
 	 * Returns the current tab group
 	 * 
 	 * @return the current tab group, or <code>null</code> if none
 	 */
 	public ILaunchConfigurationTabGroup getTabGroup() {
 		if (getTabViewer() != null) {
 			return getTabViewer().getTabGroup();
 		}
 		return null;
 	}
 	
 	/**
 	 * @see ILaunchConfigurationDialog#getTabs()
 	 */
 	public ILaunchConfigurationTab[] getTabs() {
 		if (getTabGroup() == null) {
 			return null;
 		} else {
 			return getTabGroup().getTabs();
 		}
 	} 	
 	
	/**
	 * Return whether the current configuration can be discarded.  This involves determining
	 * if it is dirty, and if it is, asking the user what to do.
	 */
	private boolean canDiscardCurrentConfig() {				
		if (getTabViewer().isDirty()) {
			return showUnsavedChangesDialog();
		} else {
			return true;
		}
	}
	
	/**
	 * Show the user a dialog appropriate to whether the unsaved changes in the current config
	 * can be saved or not.  Return <code>true</code> if the user indicated that they wish to replace
	 * the current config, either by saving changes or by discarding the, return <code>false</code>
	 * otherwise.
	 */
	private boolean showUnsavedChangesDialog() {
		if (getTabViewer().canSave()) {
			return showSaveChangesDialog();
		} else {
			return showDiscardChangesDialog();
		}
	}
	
	/**
	 * Create and return a dialog that asks the user whether they want to save
	 * unsaved changes.  Return <code>true </code> if they chose to save changes,
	 * <code>false</code> otherwise.
	 */
	private boolean showSaveChangesDialog() {
		StringBuffer buffer = new StringBuffer(LaunchConfigurationsMessages.getString("LaunchConfigurationDialog.The_configuration___29")); //$NON-NLS-1$
		buffer.append(getTabViewer().getWorkingCopy().getName());
		buffer.append(LaunchConfigurationsMessages.getString("LaunchConfigurationDialog.__has_unsaved_changes.__Do_you_wish_to_save_them__30")); //$NON-NLS-1$
		MessageDialog dialog = new MessageDialog(getShell(), 
												 LaunchConfigurationsMessages.getString("LaunchConfigurationDialog.Save_changes__31"), //$NON-NLS-1$
												 null,
												 buffer.toString(),
												 MessageDialog.QUESTION,
												 new String[] {LaunchConfigurationsMessages.getString("LaunchConfigurationDialog.Yes_32"), LaunchConfigurationsMessages.getString("LaunchConfigurationDialog.No_33"), LaunchConfigurationsMessages.getString("LaunchConfigurationDialog.Cancel_34")}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
												 0);
		// If user clicked 'Cancel' or closed dialog, return false
		int selectedButton = dialog.open();
		if ((selectedButton < 0) || (selectedButton == 2)) {
			return false;
		}
		
		// If they hit 'Yes', save the working copy 
		if (selectedButton == 0) {
			getTabViewer().handleApplyPressed();
		} else {
			// this will discard the changes
			getTabViewer().inputChanged(getTabViewer().getInput());
		}
		
		return true;
	}
	
	/**
	 * Create and return a dialog that asks the user whether they want to discard
	 * unsaved changes.  Return <code>true</code> if they chose to discard changes,
	 * <code>false</code> otherwise.
	 */
	private boolean showDiscardChangesDialog() {
		StringBuffer buffer = new StringBuffer(LaunchConfigurationsMessages.getString("LaunchConfigurationDialog.The_configuration___35")); //$NON-NLS-1$
		buffer.append(getTabViewer().getWorkingCopy().getName());
		buffer.append(LaunchConfigurationsMessages.getString("LaunchConfigurationDialog.__has_unsaved_changes_that_CANNOT_be_saved_because_of_the_following_error_36")); //$NON-NLS-1$
		buffer.append(getTabViewer().getErrorMesssage());
		buffer.append(LaunchConfigurationsMessages.getString("LaunchConfigurationDialog.Do_you_wish_to_discard_changes_37")); //$NON-NLS-1$
		MessageDialog dialog = new MessageDialog(getShell(), 
												 LaunchConfigurationsMessages.getString("LaunchConfigurationDialog.Discard_changes__38"), //$NON-NLS-1$
												 null,
												 buffer.toString(),
												 MessageDialog.QUESTION,
												 new String[] {LaunchConfigurationsMessages.getString("LaunchConfigurationDialog.Yes_32"), LaunchConfigurationsMessages.getString("LaunchConfigurationDialog.No_33")}, //$NON-NLS-1$ //$NON-NLS-2$
												 1);
		// If user clicked 'Yes', return true
		int selectedButton = dialog.open();
		if (selectedButton == 0) {
			return true;
		}
		return false;
	}
			
	/**
	 * Notification the 'Close' button has been pressed.
	 */
	protected void handleClosePressed() {
		if (canDiscardCurrentConfig()) {
			cancelPressed();
		}
	}
	
	/**
	 * Notification the 'launch' button has been pressed.
	 * Save and launch.
	 */
	protected void handleLaunchPressed() {
		int result = CANCEL;
		ILaunchConfiguration config = getTabViewer().getOriginal(); 
		try {
			if (getTabViewer().isDirty()) {
				getTabViewer().handleApplyPressed();
				config = getTabViewer().getOriginal();
			}
			result = doLaunch(config);
		} catch (CoreException e) {
			DebugUIPlugin.errorDialog(getShell(), LaunchConfigurationsMessages.getString("LaunchConfigurationDialog.Launch_Configuration_Error_6"), LaunchConfigurationsMessages.getString("LaunchConfigurationDialog.Exception_occurred_while_launching_configuration._See_log_for_more_information_49"), e); //$NON-NLS-1$ //$NON-NLS-2$
			return;
		}
		if (result == OK) {
			try {
				getPreferenceStore().setValue(IDebugPreferenceConstants.PREF_LAST_LAUNCH_CONFIGURATION_SELECTION, config.getMemento());
			} catch (CoreException e) {
				DebugUIPlugin.log(e);
			}
			close();
		} else {
			getShell().setFocus();
			updateButtons();
		}
	}
	
	/**
	 * Save the working copy if necessary, then launch the underlying configuration.
	 * 
	 * @return one of CANCEL or OK
	 */
	private int doLaunch(ILaunchConfiguration config) throws CoreException {
		
		if (!DebugUITools.saveAndBuildBeforeLaunch()) {
			return CANCEL;
		}
		
		// liftoff
		ILaunch launch = launchWithProgress(config);
		
		// If the launch was cancelled, get out.  Otherwise, notify the tabs of the successful launch.
		if (cancelButtonPressed()) {
			launch.terminate();
			return CANCEL;
		} else if (launch != null) {
			ILaunchConfigurationTabGroup group = getTabGroup();
			if (group != null) {
				group.launched(launch);
			}
		}
		
		return OK;
	}
	
	/**
	 * @return the resulting launch, or <code>null</code> if cancelled.
	 * @exception CoreException if an exception occurrs launching
	 */
	private ILaunch launchWithProgress(final ILaunchConfiguration config) throws CoreException {
		final ILaunch[] launchResult = new ILaunch[1];
		// Do the launch
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					launchResult[0] = config.launch(getMode(), monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				}
			}
		};
		try {
			run(true, true, runnable);
		} catch (InterruptedException e) {
			return null;
		} catch (InvocationTargetException e) {
			Throwable t = e.getTargetException();
			if (t instanceof CoreException) {
				throw (CoreException)t;
			} else {
				IStatus status = new Status(IStatus.ERROR, IDebugUIConstants.PLUGIN_ID, DebugException.INTERNAL_ERROR, LaunchConfigurationsMessages.getString("LaunchConfigurationDialog.Exception_occurred_while_launching_50"), t); //$NON-NLS-1$
				throw new CoreException(status);
			}
		} finally {
			//remove any "error" launch
			ILaunchManager manager= DebugPlugin.getDefault().getLaunchManager();
			ILaunch[] launches= manager.getLaunches();
			for (int i = 0; i < launches.length; i++) {
				ILaunch iLaunch = launches[i];
				if (!iLaunch.hasChildren()) {
					manager.removeLaunch(iLaunch);
				}
			}
		}
				
		return launchResult[0];		
	}
	
	private IPreferenceStore getPreferenceStore() {
		return DebugUIPlugin.getDefault().getPreferenceStore();
	}

	/***************************************************************************************
	 * 
	 * ProgressMonitor & IRunnableContext related methods
	 * 
	 ***************************************************************************************/

	/**
	 * @see IRunnableContext#run(boolean, boolean, IRunnableWithProgress)
	 */
	public void run(boolean fork, boolean cancelable, IRunnableWithProgress runnable) throws InvocationTargetException, InterruptedException {
		if (isVisible()) {
			// The operation can only be canceled if it is executed in a separate thread.
			// Otherwise the UI is blocked anyway.
			Object state = aboutToStart();
			fActiveRunningOperations++;
			try {
				ModalContext.run(runnable, fork, fProgressMonitorPart, getShell().getDisplay());
			} finally {
				fActiveRunningOperations--;
				stopped(state);
			}
		} else {
			ProgressMonitorDialog dialog = new ProgressMonitorDialog(DebugUIPlugin.getShell());
			dialog.run(fork, cancelable, runnable);
		}
	}
	
	/**
	 * About to start a long running operation triggered through
	 * the dialog. Shows the progress monitor and disables the dialog's
	 * buttons and controls.
	 *
	 * @return the saved UI state
	 */
	private Object aboutToStart() {
		Map savedState = null;
		if (getShell() != null) {
			// Save focus control
			Control focusControl = getShell().getDisplay().getFocusControl();
			if (focusControl != null && focusControl.getShell() != getShell()) {
				focusControl = null;
			}
			
			// Set the busy cursor to all shells.
			Display d = getShell().getDisplay();
			waitCursor = new Cursor(d, SWT.CURSOR_WAIT);
			setDisplayCursor(waitCursor);
					
			// Set the arrow cursor to the cancel component.
			arrowCursor= new Cursor(d, SWT.CURSOR_ARROW);
			getProgressMonitorCancelButton().setCursor(arrowCursor);
	
			// Deactivate shell
			savedState = saveUIState();
			if (focusControl != null) {
				savedState.put(FOCUS_CONTROL, focusControl);
			}
				
			// Attach the progress monitor part to the cancel button
			getProgressMonitorCancelButton().setEnabled(true);
			setCancelButtonPressed(false);
			getProgressMonitorPart().attachToCancelComponent(getProgressMonitorCancelButton());
			getProgressMonitorPart().setVisible(true);
			getProgressMonitorCancelButton().setFocus();
		}
		return savedState;
	}

	/**
	 * A long running operation triggered through the dialog
	 * was stopped either by user input or by normal end.
	 * Hides the progress monitor and restores the enable state
	 * of the dialog's buttons and controls.
	 *
	 * @param savedState the saved UI state as returned by <code>aboutToStart</code>
	 * @see #aboutToStart
	 */
	private void stopped(Object savedState) {
		if (getShell() != null) {
			getProgressMonitorPart().setVisible(false);
			getProgressMonitorPart().removeFromCancelComponent(getProgressMonitorCancelButton());
			Map state = (Map)savedState;
			restoreUIState(state);
	
			setDisplayCursor(null);	
			waitCursor.dispose();
			waitCursor = null;
			arrowCursor.dispose();
			arrowCursor = null;
			Control focusControl = (Control)state.get(FOCUS_CONTROL);
			if (focusControl != null) {
				focusControl.setFocus();
			}
		}
	}

	/**
	 * Captures and returns the enabled/disabled state of the wizard dialog's
	 * buttons and the tree of controls for the currently showing page. All
	 * these controls are disabled in the process, with the possible excepton of
	 * the Cancel button.
	 *
	 * @return a map containing the saved state suitable for restoring later
	 *   with <code>restoreUIState</code>
	 * @see #restoreUIState
	 */
	private Map saveUIState() {
		Map savedState= new HashMap(10);
		saveEnableStateAndSet(getButtonActionNew().getButton(), savedState, "new", false);//$NON-NLS-1$
		saveEnableStateAndSet(getButtonActionDelete().getButton(), savedState, "delete", false);//$NON-NLS-1$
		saveEnableStateAndSet(getButton(ID_LAUNCH_BUTTON), savedState, "launch", false);//$NON-NLS-1$
		saveEnableStateAndSet(getButton(ID_CLOSE_BUTTON), savedState, "close", false);//$NON-NLS-1$
		savedState.put("editarea", ControlEnableState.disable(getEditArea()));//$NON-NLS-1$
		return savedState;
	}

	/**
	 * Saves the enabled/disabled state of the given control in the
	 * given map, which must be modifiable.
	 *
	 * @param w the control, or <code>null</code> if none
	 * @param h the map (key type: <code>String</code>, element type:
	 *   <code>Boolean</code>)
	 * @param key the key
	 * @param enabled <code>true</code> to enable the control, 
	 *   and <code>false</code> to disable it
	 * @see #restoreEnableStateAndSet
	 */
	private void saveEnableStateAndSet(Control w, Map h, String key, boolean enabled) {
		if (w != null) {
			h.put(key, new Boolean(w.isEnabled()));
			w.setEnabled(enabled);
		}
	}

	/**
	 * Restores the enabled/disabled state of the wizard dialog's
	 * buttons and the tree of controls for the currently showing page.
	 *
	 * @param state a map containing the saved state as returned by 
	 *   <code>saveUIState</code>
	 * @see #saveUIState
	 */
	private void restoreUIState(Map state) {
		restoreEnableState(getButtonActionNew().getButton(), state, "new");//$NON-NLS-1$
		restoreEnableState(getButtonActionDelete().getButton(), state, "delete");//$NON-NLS-1$
		restoreEnableState(getButton(ID_LAUNCH_BUTTON), state, "launch");//$NON-NLS-1$
		restoreEnableState(getButton(ID_CLOSE_BUTTON), state, "close");//$NON-NLS-1$
		ControlEnableState tabState = (ControlEnableState) state.get("editarea");//$NON-NLS-1$
		tabState.restore();
	}

	/**
	 * Restores the enabled/disabled state of the given control.
	 *
	 * @param w the control
	 * @param h the map (key type: <code>String</code>, element type:
	 *   <code>Boolean</code>)
	 * @param key the key
	 * @see #saveEnableStateAndSet
	 */
	private void restoreEnableState(Control w, Map h, String key) {
		if (w != null) {
			Boolean b = (Boolean) h.get(key);
			if (b != null)
				w.setEnabled(b.booleanValue());
		}
	}
	
	private void setCancelButtonPressed(boolean pressed) {
		fCancelButtonPressed = pressed;
	}
	
	private boolean cancelButtonPressed() {
		return fCancelButtonPressed;
	}

	/**
	 * Sets the given cursor for all shells currently active
	 * for this window's display.
	 *
	 * @param cursor the cursor
	 */
	private void setDisplayCursor(Cursor cursor) {
		Shell[] shells = getShell().getDisplay().getShells();
		for (int i = 0; i < shells.length; i++)
			shells[i].setCursor(cursor);
	}

	/**
	 * @see ILaunchConfigurationDialog#updateButtons()
	 */
	public void updateButtons() {
		if (isInitializingTabs()) {
			return;
		}
				
		// New & Delete buttons
 		getButtonActionNew().setEnabled(getNewAction().isEnabled());
		getButtonActionDelete().setEnabled(getDeleteAction().isEnabled());

		// Launch button
		getTabViewer().refresh();
		getButton(ID_LAUNCH_BUTTON).setEnabled(getTabViewer().canLaunch());
		
	}
	
	/**
	 * @see ILaunchConfigurationDialog#getActiveTab()
	 */
	public ILaunchConfigurationTab getActiveTab() {
		return getTabViewer().getActiveTab();
	}

	/**
	 * @see ILaunchConfigurationDialog#updateMessage()
	 */
	public void updateMessage() {
		if (isInitializingTabs()) {
			return;
		}
		setErrorMessage(getTabViewer().getErrorMesssage());
		setMessage(getTabViewer().getMesssage());				
	}
	
	/**
	 * Show the default informational message that explains how to create a new configuration.
	 */
	private void setDefaultMessage() {
		setMessage(LaunchConfigurationsMessages.getString("LaunchConfigurationDialog.Select_a_type_of_configuration_to_create,_and_press___new__51")); //$NON-NLS-1$		
	}
	
	/**
	 * Set the tooltip of the config tree label based on the current working set in effect.
	 */
	private void updateTreeLabelTooltip() {
		LaunchConfigurationWorkingSetActionManager mgr = getWorkingSetActionManager();
		if (mgr != null) {
			IWorkingSet workingSet = mgr.getWorkingSet();
			if (workingSet != null) {
				String newTooltip = MessageFormat.format(LaunchConfigurationsMessages.getString("LaunchConfigurationDialog.Working_Set__{0}_1"), new String[] {workingSet.getName()} ); //$NON-NLS-1$
				getTreeLabel().setToolTipText(newTooltip);
				return;
			}
		}
		
		// No working set, so don't show a tooltip
		getTreeLabel().setToolTipText(null);
	}
	
	/**
	 * Returns the working set action manager
	 */
	private LaunchConfigurationWorkingSetActionManager getWorkingSetActionManager() {
		if (fLaunchConfigurationView != null) {
			return fLaunchConfigurationView.getWorkingSetActionManager();
		}
		return null;
	}
	
	/**
	 * Returns the launch configuration selection area control.
	 * 
	 * @return control
	 */
	private Composite getSelectionArea() {
		return fSelectionArea;
	}

	/**
	 * Sets the launch configuration selection area control.
	 * 
	 * @param editArea control
	 */
	private void setSelectionArea(Composite selectionArea) {
		fSelectionArea = selectionArea;
	}

	/**
	 * Returns the launch configuration edit area control.
	 * 
	 * @return control
	 */
	protected Composite getEditArea() {
		return fEditArea;
	}

	/**
	 * Sets the launch configuration edit area control.
	 * 
	 * @param editArea control
	 */
	protected void setEditArea(Composite editArea) {
		fEditArea = editArea;
	}
	
	/**
	 * @see ILaunchConfigurationDialog#setName(String)
	 */
	public void setName(String name) {
		getTabViewer().setName(name);
	}
	
	/**
	 * @see ILaunchConfigurationDialog#generateName(String)
	 */
	public String generateName(String name) {
		if (name == null) {
			name = ""; //$NON-NLS-1$
		}
		return getLaunchManager().generateUniqueLaunchConfigurationNameFrom(name);
	}
		
	/**
	 * Returns the initial launch configuration type, or <code>null</code> if none has been set.
	 */
	private ILaunchConfigurationType getInitialConfigType() {
		return fInitialConfigType;
	}
	
	/**
	 * Sets the initial launch configuration type to be used when this dialog is opened.
	 */
	public void setInitialConfigType(ILaunchConfigurationType configType) {
		fInitialConfigType = configType;
	}
	
	/**
	 * Returns the initial selection shown in this dialog when opened in
	 * <code>LAUNCH_CONFIGURATION_DIALOG_OPEN_ON_SELECTION</code> mode.
	 */
	private IStructuredSelection getInitialSelection() {
		return fInitialSelection;
	}
	
	/**
	 * Sets the initial selection for the dialog when opened in 
	 * <code>LAUNCH_CONFIGURATION_DIALOG_OPEN_ON_SELECTION</code> mode.
	 */
	public void setInitialSelection(IStructuredSelection selection) {
		fInitialSelection = selection;
	}
	
	/**
	 * Handles key events in the tree viewer. Specifically
	 * when the delete key is pressed.
	 */
	protected void handleTreeViewerKeyPressed(KeyEvent event) {
		if (event.character == SWT.DEL && event.stateMask == 0) {
			if (getButtonActionDelete().isEnabled()) {
				getButtonActionDelete().run();
			}
		} 
	}
	
	private void setButtonActionNew(ButtonAction action) {
		fButtonActionNew = action;
	}
	
	private ButtonAction getButtonActionNew() {
		return fButtonActionNew;
	}

	private void setButtonActionDelete(ButtonAction action) {
		fButtonActionDelete = action;
	}
	
	private ButtonAction getButtonActionDelete() {
		return fButtonActionDelete;
	}

	private void setTreeLabel(Label treeLabel) {
		fTreeLabel = treeLabel;
	}

	private Label getTreeLabel() {
		return fTreeLabel;
	}

	public static void setCurrentlyVisibleLaunchConfigurationDialog(ILaunchConfigurationDialog dialog) {
		fgCurrentlyVisibleLaunchConfigurationDialog = dialog;
	}

	public static ILaunchConfigurationDialog getCurrentlyVisibleLaunchConfigurationDialog() {
		return fgCurrentlyVisibleLaunchConfigurationDialog;
	}

	/**
	 * Extension of <code>Action</code> that manages a <code>Button</code>
	 * widget.  This allows common handling for actions that must appear in
	 * a pop-up menu and also as a (non-toolbar) button in the UI.
	 */
	private abstract class ButtonAction extends Action {
		
		protected Button fButton;
		
		/**
		 * Construct a ButtonAction handler.  All details of the specified
		 * <code>Button</code>'s layout and appearance should be handled 
		 * external to this class.
		 */
		public ButtonAction(String text, Button button) {
			super(text);
			fButton = button;
			if (fButton != null) {
				fButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						ButtonAction.this.run();
					}
				});
			}
		}
		
		public Button getButton() {
			return fButton;
		}
		
		/**
		 * @see IAction#setEnabled(boolean)
		 */
		public void setEnabled(boolean enabled) {
			super.setEnabled(enabled);
			if (fButton != null) {
				fButton.setEnabled(enabled);
			}
		}
	}
	
	/**
	 * Handler for creating a new configuration.
	 */
	private class ButtonActionNew extends ButtonAction {
		
		public ButtonActionNew(String text, Button button) {
			super(text, button);
		}
		
		public void run() {
			getNewAction().run();
		}
	}

	/**
	 * Handler for deleting a configuration.
	 */
	private class ButtonActionDelete extends ButtonAction {
		
		public ButtonActionDelete(String text, Button button) {
			super(text, button);
		}
		
		public void run() {
			getDeleteAction().run();
		}
	}
	
	private class DoubleClickAction extends Action {
		/**
		 * @see org.eclipse.jface.action.IAction#run()
		 */
		public void run() {
			IStructuredSelection selection = (IStructuredSelection)fLaunchConfigurationView.getViewer().getSelection();
			Object target = selection.getFirstElement();
			if (target instanceof ILaunchConfiguration) {
				handleLaunchPressed();
			} else {
				getNewAction().run();
			}
		}

	}
	
	/**
	 * Returns the banner image to display in the title area	 */
	protected Image getBannerImage() {
		if (fBannerImage == null) {
			ImageDescriptor descriptor = getLaunchGroup().getBannerImageDescriptor(); 
			if (descriptor != null) {
				fBannerImage = descriptor.createImage();
			} 		
		}
		return fBannerImage;
	}
	
	/**
	 * Sets the launch group to display.
	 * 
	 * @param group launch group
	 */
	protected void setLaunchGroup(LaunchGroupExtension group) {
		fGroup = group;
	}
	
	/**
	 * Returns the launch group being displayed.
	 * 
	 * @return launch group	 */
	public LaunchGroupExtension getLaunchGroup() {
		return fGroup;
	}
	
	protected AbstractLaunchConfigurationAction getNewAction() {
		return (AbstractLaunchConfigurationAction)fLaunchConfigurationView.getAction(CreateLaunchConfigurationAction.ID_CREATE_ACTION);
	}
	
	protected AbstractLaunchConfigurationAction getDeleteAction() {
		return (AbstractLaunchConfigurationAction)fLaunchConfigurationView.getAction(DeleteLaunchConfigurationAction.ID_DELETE_ACTION);
	}
	
	protected AbstractLaunchConfigurationAction getDuplicateAction() {
		return (AbstractLaunchConfigurationAction)fLaunchConfigurationView.getAction(DuplicateLaunchConfigurationAction.ID_DUPLICATE_ACTION);
	}		

	/**
	 * Returns the dialog settings for this dialog. Subclasses should override
	 * <code>getDialogSettingsKey()</code>.
	 * 
	 * @return IDialogSettings
	 */
	protected IDialogSettings getDialogSettings() {
		IDialogSettings settings = DebugUIPlugin.getDefault().getDialogSettings();
		IDialogSettings section = settings.getSection(getDialogSettingsSectionName());
		if (section == null) {
			section = settings.addNewSection(getDialogSettingsSectionName());
		} 
		return section;
	}
	
	/**
	 * Returns the name of the section that this dialog stores its settings in
	 * 
	 * @return String
	 */
	protected String getDialogSettingsSectionName() {
		return IDebugUIConstants.PLUGIN_ID + ".LAUNCH_CONFIGURATIONS_DIALOG_SECTION"; //$NON-NLS-1$
	}
	
	/**
	 * Sets the viewer used to display the tabs for a launch configuration.
	 * 
	 * @param viewer
	 */
	protected void setTabViewer(LaunchConfigurationTabGroupViewer viewer) {
		fTabViewer = viewer;
	}
	
	/**
	 * Returns the viewer used to display the tabs for a launch configuration.
	 * 
	 * @return LaunchConfigurationTabGroupViewer
	 */
	protected LaunchConfigurationTabGroupViewer getTabViewer() {
		return fTabViewer;
	}
	/**
	 * @see org.eclipse.jface.window.Window#initializeBounds()
	 */
	protected void initializeBounds() {
		super.initializeBounds();
		initializeSashForm();
		ensureSelectionAreaWidth();
		resize();			
	}

	/**
	 * @see org.eclipse.jface.window.Window#create()
	 */
	public void create() {
		super.create();
		// bug 27011
		if (getTabViewer().getInput() == null) {
			getTabViewer().inputChanged(null);
		}			
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationDialog#setActiveTab(org.eclipse.debug.ui.ILaunchConfigurationTab)
	 */
	public void setActiveTab(ILaunchConfigurationTab tab) {
		getTabViewer().setActiveTab(tab);
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationDialog#setActiveTab(int)
	 */
	public void setActiveTab(int index) {
		getTabViewer().setActiveTab(index);
	}

}