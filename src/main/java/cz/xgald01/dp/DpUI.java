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
 * DpUI predstavuje zakladni vstupni bod cele aplikace. UI je reprezentovano bud oknem prohlizece
 * nebo casti HTML stranky obsazenou ve Vaadinovske aplikaci.
 * <p>
 * UI je inicializovano prostrednictvim {@link #init(VaadinRequest)}. U teto metody se predpoklada,
 * jeji prekryti za ucelem pridani komponent do ui.
 */
@Theme("mytheme")
public class DpUI extends UI {

    @Override
    protected void init(VaadinRequest vaadinRequest) {
        // Zakladni layout aplikace
        VerticalLayout rootLayout = new VerticalLayout();
        rootLayout.setSizeFull();
        setContent(rootLayout);
        // View display ovladany navigatorem.
        Panel viewDisplay = new Panel();
        viewDisplay.setSizeFull();
        // Navigator pro ovladani jednotlivych view
        Navigator navigator = new Navigator(this, viewDisplay);
        // Pridat jednotliva view
        navigator.addView("BaseView", new BaseView());
        navigator.addView("NASA_API", new NasaView("Nasa NEO - Zemi blízké asteroidy"));
        navigator.addView("Mapbox_API", new MapboxView("Mapbox - Geokódování"));
        navigator.navigateTo("BaseView");
        // Pridat navigator k hlavnimu menu
        NavigableMenuBar menuBar = new NavigableMenuBar(navigator);
        rootLayout.addComponent(menuBar);
        // Pridat view display pod hlavni menu
        rootLayout.addComponent(viewDisplay);
        rootLayout.setExpandRatio(viewDisplay, 1.0f);
        // Updatovat obrazovku po vyberu view
        navigator.addViewChangeListener(menuBar);
        // Pridat polozky do menu a asociovat je s prislusnym view ID
        menuBar.addView("NASA_API", "Nasa API");
        menuBar.addView("Mapbox_API", "Mapbox API");
        // Pridat copyright line
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
