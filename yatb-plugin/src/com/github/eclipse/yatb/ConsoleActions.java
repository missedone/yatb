package com.github.eclipse.yatb;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.DebugException;
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
  private Action stop;
  private IActionBars bars;
  private IConsole console;

  @Override
  public void init(final IPageBookViewPage page, final IConsole console) {
    this.console = console;
    this.page = page;
    IPageSite site = page.getSite();
    this.bars = site.getActionBars();

    createTerminateAllButton();

    bars.getMenuManager().add(new Separator());

    IToolBarManager toolbarManager = bars.getToolBarManager();

    toolbarManager.appendToGroup(IConsoleConstants.LAUNCH_GROUP, stop);

    bars.updateActionBars();
  }

  private void createTerminateAllButton() {
    ImageDescriptor imageDescriptor = ImageDescriptor.createFromFile(getClass(), "/icons/terminate_rem_co.gif");
    this.stop = new Action("Kill Process", imageDescriptor) {
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

              // force kill the process on OSX and Linux-like platform
              // since on Linux the default behaviour of Process.destroy() is to
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
    stop = null;
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
    boolean isEnabled = true;
    stop.setEnabled(isEnabled);
    bars.updateActionBars();
  }

}
