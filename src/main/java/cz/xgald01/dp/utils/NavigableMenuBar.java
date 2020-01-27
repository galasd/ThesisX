package cz.xgald01.dp.utils;

import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.ui.MenuBar;

import java.util.HashMap;

/**
 * Menu bar for navigator, which includes views selection
 */
public class NavigableMenuBar extends MenuBar implements ViewChangeListener {

    // Previous menu item
    private MenuBar.MenuItem previous = null;
    // Current menu item
    private MenuBar.MenuItem current = null;
    // Map all views names to menu items
    private HashMap<String, MenuBar.MenuItem> menuItems = new HashMap<String, MenuBar.MenuItem>();
    private Navigator navigator = null;

    public NavigableMenuBar(Navigator navigator) {
        this.navigator = navigator;
    }

    // Navigate to a given view
    private MenuBar.Command mycommand = new MenuBar.Command() {
        @Override
        public void menuSelected(MenuBar.MenuItem menuItem) {
            String viewName = selectItem(menuItem);
            navigator.navigateTo(viewName);
        }
    };

    // Add a view
    public void addView(String viewName, String caption) {
        menuItems.put(viewName, addItem(caption, mycommand));
    }

    // Select a menu item mapped to a given view
    private boolean selectView(String viewName) {
        if (!menuItems.containsKey(viewName))
            return false;
        // Select a menu item
        if (previous != null)
            previous.setStyleName(null);
        if (current == null)
            current = menuItems.get(viewName);
        current.setStyleName("highlight");
        previous = current;
        return true;
    }

    // Choose new menu item
    private String selectItem(MenuBar.MenuItem selectedItem) {
        current = selectedItem;
        for (String key : menuItems.keySet())
            if (menuItems.get(key) == selectedItem)
                return key;
        return null;
    }

    // Select given view
    @Override
    public boolean beforeViewChange(ViewChangeListener.ViewChangeEvent event) {
        return selectView(event.getViewName());
    }

    // After viewChange event
    @Override
    public void afterViewChange(ViewChangeListener.ViewChangeEvent event) {
    }
}







