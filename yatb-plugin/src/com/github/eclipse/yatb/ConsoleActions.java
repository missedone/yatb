package com.github.eclipse.yatb;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.RuntimeProcess;
import org.eclipse.debug.internal.ui.views.console.ProcessConsole;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.ui.part.IPageSite;

public class ConsoleActions implements IConsolePageParticipant {

	private IPageBookViewPage page;
	private Action terminateAction;
	private Action terminateAllAction;
	private IActionBars bars;
	private IConsole console;

	@Override
	public void init(final IPageBookViewPage page, final IConsole console) {
		this.console = console;
		this.page = page;
		IPageSite site = page.getSite();
		this.bars = site.getActionBars();

		terminateAction = createTerminateButton();
		terminateAllAction = createTerminateAllButton();

		bars.getMenuManager().add(new Separator());

		IToolBarManager toolbarManager = bars.getToolBarManager();

		toolbarManager.appendToGroup(IConsoleConstants.LAUNCH_GROUP, terminateAction);
		toolbarManager.appendToGroup(IConsoleConstants.LAUNCH_GROUP, terminateAllAction);

		bars.updateActionBars();
	}

	private Action createTerminateButton() {
		ImageDescriptor imageDescriptor = ImageDescriptor.createFromFile(getClass(), "/icons/terminate_rem_co.gif");
		return new Action("Kill Process", imageDescriptor) {
			@Override
			public void run() {
				if (console instanceof ProcessConsole) {
					RuntimeProcess runtimeProcess = (RuntimeProcess) ((ProcessConsole) console)
							.getAttribute(IDebugUIConstants.ATTR_CONSOLE_PROCESS);
					ILaunch launch = runtimeProcess.getLaunch();
					stopProcess(launch);
				}
			}
		};
	}

	private Action createTerminateAllButton() {
		ImageDescriptor imageDescriptor = ImageDescriptor.createFromFile(getClass(), "/icons/terminate_rem_co.gif");
		return new Action("Kill All Processes", imageDescriptor) {
			@Override
			public void run() {
				ILaunch[] launches = DebugPlugin.getDefault().getLaunchManager().getLaunches();
				for (ILaunch launch : launches) {
					stopProcess(launch);
				}
			}
		};
	}

	private void stopProcess(ILaunch launch) {
		if (launch != null && !launch.isTerminated()) {
			try {
				if (Platform.OS_WIN32.equals(Platform.getOS())) {
					launch.terminate();
				} else {
					for (IProcess p : launch.getProcesses()) {
						try {
							Method m = p.getClass().getDeclaredMethod("getSystemProcess");
							m.setAccessible(true);
							Process proc = (Process) m.invoke(p);

							Field f = proc.getClass().getDeclaredField("pid");
							f.setAccessible(true);
							int pid = (int) f.get(proc);

							// force kill the process on OSX and Linux-like
							// platform
							// since on Linux the default behaviour of
							// Process.destroy() is to
							// gracefully shutdown
							// which rarely can stop the busy process
							Runtime rt = Runtime.getRuntime();
							rt.exec("kill -9 " + pid);
						} catch (Exception ex) {
							Activator.log(ex);
						}
					}
				}
			} catch (DebugException e) {
				Activator.log(e);
			}
		}
	}

	@Override
	public void dispose() {
		terminateAction = null;
		terminateAllAction = null;
		bars = null;
		page = null;
	}

	@Override
	public Object getAdapter(Class adapter) {
		return null;
	}

	@Override
	public void activated() {
		updateVis();
	}

	@Override
	public void deactivated() {
		updateVis();
	}

	private void updateVis() {
		if (page == null)
			return;
		terminateAction.setEnabled(true);
		terminateAllAction.setEnabled(true);
		bars.updateActionBars();
	}

}