package saf.core.ui;

import saf.core.ui.actions.ActionFactory;
import saf.core.ui.actions.ExitAction;
import saf.core.ui.dock.DockingFactory;
import saf.core.ui.dock.DockingManager;
import saf.core.ui.dock.Perspective;
import saf.core.ui.help.Help;
import saf.core.ui.util.FileChooserUtilities;
import saf.core.ui.MenuOrder;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * @author Nick Collier
 * @version $Revision: 1.11 $ $Date: 2006/06/01 18:05:01 $
 */
public class GUICreatorDelegate {

  private static GUICreatorDelegate instance = new GUICreatorDelegate();
  private java.util.List<BarItemDescriptor> descriptors = new ArrayList<BarItemDescriptor>();
  private MenuTreeDescriptor mtDescriptor;
  private List<Perspective> perspectives;
  private Properties props;
  private AppPreferences prefs;
  private Help help;
  private StatusBarDescriptor statusBarDescriptor;
  private DockingFactory dockingFactory;

  private GUICreatorDelegate() {

    // load properties
    InputStream strm = null;
    try {
      props = new Properties();
      File file = new File(System.getProperty("applicationRoot"), "ui.properties");
      if (file.exists()) {
        strm = new FileInputStream(file);
      } else {
        strm = getClass().getClassLoader().getResourceAsStream("ui.properties");
      }
      props.load(strm);

      String dockingFacClass = props.getProperty(GUIConstants.DOCKING_FACTORY_CLASS, "");
      if (dockingFacClass.equals("")) {
        dockingFactory = new DefaultDockingFactory();
      } else {
        Class clazz = Class.forName(dockingFacClass);
        dockingFactory = (DockingFactory) clazz.newInstance();
      }

      registerMacOSX();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    } catch (InstantiationException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    } catch (NoSuchMethodException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    } catch (InvocationTargetException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    } finally {
      if (strm != null) {
        try {
          strm.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  private void registerMacOSX() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException,
          InvocationTargetException {
    if (System.getProperty("os.name").toLowerCase().startsWith("mac os x")) {

      // we do this with reflection so the mac only code doesn't
      // need to be linked here
      Class osxAdapter = getClass().getClassLoader().loadClass("saf.core.ui.osx.OSXAdapter");
      Method registerMethod = osxAdapter.getDeclaredMethod("registerMacOSXApplication", new Class[0]);
      if (registerMethod != null) {
        Object[] args = {};
        registerMethod.invoke(osxAdapter, args);
      }

      Class[] defArgs = {Boolean.class};
      defArgs[0] = boolean.class;
      Method prefsEnableMethod = osxAdapter.getDeclaredMethod("enablePrefs", defArgs);
      if (prefsEnableMethod != null) {
        Object args[] = {Boolean.FALSE};
        prefsEnableMethod.invoke(osxAdapter, args);
      }

    }
  }


  static GUICreatorDelegate getInstance() {
    return instance;
  }

  public void addBarItemDescriptor(BarItemDescriptor descriptor) {
    descriptors.add(descriptor);
  }


  public DockingFactory getDockingFactory() {
    return dockingFactory;
  }

  public ISAFDisplay createDisplay(IAppConfigurator configurator, Workspace workspace) {

    final ISAFDisplay display = dockingFactory.getDisplay();
    IWindowCustomizer wCustomizer = new WindowCustomizer(prefs);
    boolean result = configurator.preWindowOpen(wCustomizer);
    if (!result) return null;

    JToolBar toolBar = new JToolBar();
    JMenuBar menuBar = new JMenuBar();
    GUIBarManager barManager = new GUIBarManager(toolBar, menuBar);

    display.init(wCustomizer, barManager);
    FileChooserUtilities.init(display.getFrame());
    ActionFactory.getInstance().registerAction(GUIConstants.EXIT_ACTION,
            new ExitAction(configurator, display.getFrame(), prefs));

    display.getFrame().addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        ActionFactory.getInstance().getAction(GUIConstants.EXIT_ACTION).
                actionPerformed(new ActionEvent(display, ActionEvent.ACTION_PERFORMED, "exit"));
      }
    });

    DockingManager vManager = dockingFactory.getViewManager(barManager, perspectives);
    workspace.setViewManager(vManager);
    configurator.createLayout(vManager);
    if (mtDescriptor != null) mtDescriptor.createMenus(barManager);
    for (BarItemDescriptor descriptor : descriptors) {
      descriptor.fillBars(barManager, workspace);
    }

    if (statusBarDescriptor != null) {
      statusBarDescriptor.fillBar(barManager);
    }

    configurator.fillBars(barManager);

    if (help != null) {
      JMenu menu = barManager.getMenu(GUIConstants.HELP_MENU_ID);
      if (menu == null) {
        menu = barManager.addMenu(GUIConstants.HELP_MENU_ID, "Help");
        menu.setMnemonic('h');
      }
      barManager.addMenuItem(GUIConstants.HELP_TOPICS_ID, menu, help.createAction());
    }

    barManager.createPerspectiveMenu(vManager);

    String windowMenuLabel = wCustomizer.getWindowsMenuLabel();

    MenuOrder menuOrder = new MenuOrder(props);
    menuOrder.orderItems(barManager, windowMenuLabel);

    ToolBarOrder toolBarOrder = new ToolBarOrder(props);
    barManager.setToolbarOrder(toolBarOrder);

    vManager.init();
    return display;
  }


  public void runDisplay(IAppConfigurator configurator, ISAFDisplay display) {
    display.display();
    configurator.postWindowOpen(display);
  }

  public void setMenuTreeDescriptor(MenuTreeDescriptor mtDescriptor) {
    this.mtDescriptor = mtDescriptor;
    for (BarItemDescriptor descriptor : descriptors) {
      String menuID = descriptor.getMenuID();
      if (menuID != null) {
        if (menuID.equals(GUIConstants.HELP_MENU_ID)) {
          mtDescriptor.addMenu(GUIConstants.HELP_MENU_ID, "&Help", null);
          break;
        }
      }
    }
  }

  public void setPerspectives(List<Perspective> perspectives) {
    this.perspectives = perspectives;
  }

  public void setHelp(Help help) {
    this.help = help;
  }

  public void setApplicationPrefs(AppPreferences prefs) {
    this.prefs = prefs;
  }

  public void setStatusBarDescriptor(StatusBarDescriptor descriptor) {
    this.statusBarDescriptor = descriptor;
  }
}
