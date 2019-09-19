package cz.xgald01.dp.utils;

import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.ui.MenuBar;

import java.util.HashMap;

/**
 * Menu bar pro navigator, ktery obsluhuje prepinani views
 */
public class NavigableMenuBar extends MenuBar implements ViewChangeListener {

    // Predchozi zvolena polozka menu
    private MenuBar.MenuItem previous = null;
    // Soucasna polozka menu
    private MenuBar.MenuItem current = null;
    // Namapovat nazvy jednotlivych view k polozkam menu
    private HashMap<String, MenuBar.MenuItem> menuItems = new HashMap<String, MenuBar.MenuItem>();
    private Navigator navigator = null;

    public NavigableMenuBar(Navigator navigator) {
        this.navigator = navigator;
    }

    // Navigovat k prislusnemu view na zaklade vyberu polozky z menu
    private MenuBar.Command mycommand = new MenuBar.Command() {
        @Override
        public void menuSelected(MenuBar.MenuItem menuItem) {
            String viewName = selectItem(menuItem);
            navigator.navigateTo(viewName);
        }
    };

    // Pridat view do menu
    public void addView(String viewName, String caption) {
        menuItems.put(viewName, addItem(caption, mycommand));
    }

    // Vybrat polozku menu na zaklade nazvu view
    private boolean selectView(String viewName) {
        // Overit, ze dana polozka menu skutecne existuje
        if (!menuItems.containsKey(viewName))
            return false;
        // Vyber polozky menu
        if (previous != null)
            previous.setStyleName(null);
        if (current == null)
            current = menuItems.get(viewName);
        current.setStyleName("highlight");
        previous = current;
        return true;
    }

    // Zvolit novou polozku menu
    private String selectItem(MenuBar.MenuItem selectedItem) {
        current = selectedItem;
        // Vyhledat nazev prislusne polozky menu
        for (String key : menuItems.keySet())
            if (menuItems.get(key) == selectedItem)
                return key;
        return null;
    }

    // Vybrat prislune view
    @Override
    public boolean beforeViewChange(ViewChangeListener.ViewChangeEvent event) {
        return selectView(event.getViewName());
    }

    // Event po zmene view
    @Override
    public void afterViewChange(ViewChangeListener.ViewChangeEvent event) {
    }
}







