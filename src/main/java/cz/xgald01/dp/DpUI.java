package cz.xgald01.dp;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.navigator.Navigator;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.*;
import cz.xgald01.dp.utils.NavigableMenuBar;
import cz.xgald01.dp.view.BaseView;
import cz.xgald01.dp.view.MapboxView;
import cz.xgald01.dp.view.NasaView;

import javax.servlet.annotation.WebServlet;

/**
 * DpUI is a main node of this app. UI is represented either by browser window
 * or by part of HTML page included in a Vaadin application
 * <p>
 * UI is initiated via {@link #init(VaadinRequest)}. This method is supposed to be overrided
 */
@Theme("mytheme")
public class DpUI extends UI {

    @Override
    protected void init(VaadinRequest vaadinRequest) {
        // Root layout
        VerticalLayout rootLayout = new VerticalLayout();
        rootLayout.setSizeFull();
        setContent(rootLayout);
        // View display controlled by navigator
        Panel viewDisplay = new Panel();
        viewDisplay.setSizeFull();
        // Navigator for views
        Navigator navigator = new Navigator(this, viewDisplay);
        // Add views into navigator
        navigator.addView("BaseView", new BaseView());
        navigator.addView("NASA_API", new NasaView("Nasa NEO - Near Earth Objects"));
        navigator.addView("Mapbox_API", new MapboxView("Mapbox - Geocoding"));
        navigator.navigateTo("BaseView");
        // Add navigator to main menu
        NavigableMenuBar menuBar = new NavigableMenuBar(navigator);
        rootLayout.addComponent(menuBar);
        // Add a view display to main menu
        rootLayout.addComponent(viewDisplay);
        rootLayout.setExpandRatio(viewDisplay, 1.0f);
        // Update screen after view selection
        navigator.addViewChangeListener(menuBar);
        // Add menu items
        menuBar.addView("NASA_API", "Nasa API");
        menuBar.addView("Mapbox_API", "Mapbox API");
        // Add the copyright line
        HorizontalLayout footLine = new HorizontalLayout();
        footLine.setWidth("100%");
        Label author = new Label("© David Galaš 2019");
        author.addStyleName("author-style");
        footLine.addComponent(author);
        author.setSizeUndefined();
        footLine.setComponentAlignment(author, Alignment.MIDDLE_RIGHT);
        rootLayout.addComponent(footLine);
    }

    @WebServlet(urlPatterns = "/*", name = "MyUIServlet", asyncSupported = true)
    @VaadinServletConfiguration(ui = DpUI.class, productionMode = false)
    public static class MyUIServlet extends VaadinServlet {
    }
}
