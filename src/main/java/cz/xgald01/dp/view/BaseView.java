package cz.xgald01.dp.view;

import com.vaadin.navigator.View;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 * Base root view
 */
public class BaseView extends VerticalLayout implements View {

    private Label welcomeLabel;

    public BaseView() {
        // Root layout
        setSizeFull();
        VerticalLayout verticalLayout = new VerticalLayout();
        verticalLayout.setSizeFull();
        welcomeUser();
        // Add components to root layout
        verticalLayout.addComponent(welcomeLabel);
        verticalLayout.setComponentAlignment(welcomeLabel, Alignment.MIDDLE_CENTER);
        welcomeLabel.addStyleName("welcome-style");
        addComponent(verticalLayout);
    }

    // Create a welcome label
    private void welcomeUser(){
        welcomeLabel = new Label();
        welcomeLabel.setValue("Select a REST API");
        welcomeLabel.setSizeUndefined();
    }
}
