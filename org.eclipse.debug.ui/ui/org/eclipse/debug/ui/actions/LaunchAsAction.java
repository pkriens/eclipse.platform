/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.ui.actions;


import java.util.Iterator;
import java.util.List;

import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.actions.ActionMessages;
import org.eclipse.debug.internal.ui.actions.LaunchShortcutAction;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationManager;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchGroupExtension;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchShortcutExtension;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowPulldownDelegate2;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.activities.IActivityManager;
import org.eclipse.ui.activities.IIdentifier;
import org.eclipse.ui.activities.IWorkbenchActivitySupport;
import org.eclipse.ui.help.WorkbenchHelp;

/**
 * A cascading sub-menu that shows all launch shortcuts pertinent to a
 * launch group.
 * 
 * @since 2.1
 */
public class LaunchAsAction extends Action implements IMenuCreator, IWorkbenchWindowPulldownDelegate2 {
	
	/**
	 * Cascading menu 
	 */
	private Menu fCreatedMenu;
	
	/**
	 * Launch group identifier 
	 */
	private String fLaunchGroupIdentifier;
	
	/**
	 * Presentation wrapper for this action
	 */
	private IAction fAction;
	
	/**
	 * Creates a cascading menu action to populate with shortcuts in the given
	 * launch group.
	 *  
	 * @param launchGroupIdentifier launch group identifier
	 */
	public LaunchAsAction(String launchGroupIdentifier) {
		super();
		fLaunchGroupIdentifier = launchGroupIdentifier;
		if (getMode().equals(ILaunchManager.DEBUG_MODE)) {
			setText(ActionMessages.getString("LaunchAsAction.Debug_As_1")); //$NON-NLS-1$
		} else {
			setText(ActionMessages.getString("LaunchAsAction.Run_As_2")); //$NON-NLS-1$
		}
		setMenuCreator(this);
	}
	
	/**
	 * Returns the launch group associatd with this action.
	 * 
	 * @return the launch group associatd with this action
	 */
	private LaunchGroupExtension getLaunchGroup() {
		return getLaunchConfigurationManager().getLaunchGroup(fLaunchGroupIdentifier);
	}

	/**
	 * @see IAction#run()
	 */
	public void run() {
		//do nothing, this action just creates a cascading menu.
	}
		
	private void createAction(Menu parent, IAction action, int count) {
		StringBuffer label= new StringBuffer();
		//add the numerical accelerator
		if (count < 10) {
			label.append('&');
			label.append(count);
			label.append(' ');
		}
		label.append(action.getText());
		action.setText(label.toString());
		ActionContributionItem item= new ActionContributionItem(action);
		item.fill(parent, -1);
	}
	
	/**
	 * @see IMenuCreator#dispose()
	 */
	public void dispose() {
		if (getCreatedMenu() != null) {
			getCreatedMenu().dispose();
		}
	}
	
	/**
	 * @see IMenuCreator#getMenu(Control)
	 */
	public Menu getMenu(Control parent) {
		return null;
	}
	
	/**
	 * @see IMenuCreator#getMenu(Menu)
	 */
	public Menu getMenu(Menu parent) {
		if (getCreatedMenu() != null) {
			 getCreatedMenu().dispose();
		 }
		setCreatedMenu(new Menu(parent));
		fillMenu();
		initMenu();
		return getCreatedMenu();
	}
	
	private void fillMenu() {
		//Retrieve the current perspective and the registered shortcuts
		 List shortcuts = null;
		 String activePerspID = getActivePerspectiveID();
		 if (activePerspID != null) {
			 shortcuts = getLaunchConfigurationManager().getLaunchShortcuts(activePerspID, getCategory());
		 }
	
		 // If NO shortcuts are listed in the current perspective, add ALL shortcuts
		 // to avoid an empty cascading menu
		 if (shortcuts == null || shortcuts.isEmpty()) {
			 shortcuts = getLaunchConfigurationManager().getLaunchShortcuts(getCategory());
		 }

		 int menuCount = 1;
		 IWorkbenchActivitySupport activitySupport = (IWorkbenchActivitySupport) PlatformUI.getWorkbench().getAdapter(IWorkbenchActivitySupport.class);
		 IActivityManager activityManager = null;
		 if (activitySupport != null) {
		 	activityManager = activitySupport.getActivityManager();
		 }
		 
		 Iterator iter = shortcuts.iterator();
		 while (iter.hasNext()) {
			 LaunchShortcutExtension ext = (LaunchShortcutExtension) iter.next();
			 if (ext.getModes().contains(getMode())) {
			 	if (activityManager != null) {
			 		IIdentifier identifier = activityManager.getIdentifier(ext.getId());
			 		if (!identifier.isEnabled()) {
			 			continue;
			 		}
			 	}
				populateMenu(ext, getCreatedMenu(), menuCount);
				menuCount++;
			 }
		 }
	}
	
	/**
	 * Creates the menu for the action
	 */
	private void initMenu() {
		// Add listener to repopulate the menu each time
		// it is shown to reflect changes in selection or active perspective
		fCreatedMenu.addMenuListener(new MenuAdapter() {
			public void menuShown(MenuEvent e) {
				Menu m = (Menu)e.widget;
				MenuItem[] items = m.getItems();
				for (int i=0; i < items.length; i++) {
					items[i].dispose();
				}
				fillMenu();
			}
		});
	}
		
	/**
	 * Add the shortcut to the menu.
	 */
	private void populateMenu(LaunchShortcutExtension ext, Menu menu, int menuCount) {
		LaunchShortcutAction action = new LaunchShortcutAction(getMode(), ext);
		action.setActionDefinitionId(ext.getId());
		String helpContextId = ext.getHelpContextId();
		if (helpContextId != null) {
			WorkbenchHelp.setHelp(action, helpContextId);
		}
		/*if (fKeyBindingService != null) {
			fKeyBindingService.registerGlobalAction(action);	
		}*/
		createAction(menu, action, menuCount);
	}
	
	/**
	 * Return the ID of the currently active perspective, or <code>null</code>
	 * if there is none.
	 */
	private String getActivePerspectiveID() {
		IWorkbenchWindow window = DebugUIPlugin.getActiveWorkbenchWindow();
		if (window != null) {
			IWorkbenchPage page = window.getActivePage();
			if (page != null) {
				IPerspectiveDescriptor persp = page.getPerspective();
				if (persp != null) {
					return persp.getId();
				}
			}
		}
		return null;
	}
		
	/**
	 * Returns the mode of this action - run or debug 
	 * 
	 * @return the mode of this action - run or debug
	 */
	private String getMode() {
		return getLaunchGroup().getMode();
	}
	
	/**
	 * Returns the category of this action - possibly <code>null</code>
	 *
	 * @return the category of this action - possibly <code>null</code>
	 */
	private String getCategory() {
		return getLaunchGroup().getCategory();
	}
	
	private Menu getCreatedMenu() {
		return fCreatedMenu;
	}
	
	private void setCreatedMenu(Menu createdMenu) {
		fCreatedMenu = createdMenu;
	}
	
	/**
	 * Returns the launch configuration manager.
	 *
	 * @return launch configuration manager
	 */
	private LaunchConfigurationManager getLaunchConfigurationManager() {
		return DebugUIPlugin.getDefault().getLaunchConfigurationManager();
	}	
	
	/**
	 * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#init(org.eclipse.ui.IWorkbenchWindow)
	 */
	public void init(IWorkbenchWindow window) {
//		if (window instanceof WorkbenchWindow) {
//			fKeyBindingService= ((WorkbenchWindow)window).getKeyBindingService();
//		}
	}

	/**
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
		// do nothing - this is just a menu
	}

	/**
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		if (fAction == null) {
			initialize(action);
		}
	}
	
	/**
	 * Set the enabled state of the underlying action based on whether there are any
	 * registered launch shortcuts for this launch mode.
	 */
	private void initialize(IAction action) {
		fAction = action;
		action.setEnabled(existsShortcutsForMode());	
	}	

	/**
	 * Return whether there are any registered launch shortcuts for
	 * the mode of this action.
	 * 
	 * @return whether there are any registered launch shortcuts for
	 * the mode of this action
	 */
	private boolean existsShortcutsForMode() {
		List shortcuts = getLaunchConfigurationManager().getLaunchShortcuts(getCategory());
		Iterator iter = shortcuts.iterator();
		while (iter.hasNext()) {
			LaunchShortcutExtension ext = (LaunchShortcutExtension) iter.next();
			if (ext.getModes().contains(getMode())) {
				return true;
			}
		}		
		return false;
	}	
}

