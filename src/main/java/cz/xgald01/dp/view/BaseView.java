package cz.xgald01.dp.view;

import com.vaadin.navigator.View;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 * Zakladni korenove view
 */
public class BaseView extends VerticalLayout implements View {

    private Label welcomeLabel;

    public BaseView() {
        // Korenovy layout
        setSizeFull();
        VerticalLayout verticalLayout = new VerticalLayout();
        verticalLayout.setSizeFull();
        welcomeUser();
        // Pridani komponent
        verticalLayout.addComponent(welcomeLabel);
        verticalLayout.setComponentAlignment(welcomeLabel, Alignment.MIDDLE_CENTER);
        welcomeLabel.addStyleName("welcome-style");
        addComponent(verticalLayout);
    }

    // Vytvorit uvodni uvitaci napis
    private void welcomeUser(){
        // Uvitaci napis
        welcomeLabel = new Label();
        welcomeLabel.setValue("Vítejte na hlavní stránce aplikace. Po práci s REST API pokračujte výběrem z menu.");
        welcomeLabel.setSizeUndefined();
    }
}
