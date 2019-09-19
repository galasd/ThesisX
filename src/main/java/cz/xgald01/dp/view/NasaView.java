package cz.xgald01.dp.view;

import com.jayway.jsonpath.JsonPath;
import com.vaadin.data.Binder;
import com.vaadin.navigator.View;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.Sizeable;
import com.vaadin.server.StreamResource;
import com.vaadin.server.ThemeResource;
import com.vaadin.shared.ui.window.WindowMode;
import com.vaadin.ui.*;
import cz.xgald01.dp.DpUI;
import cz.xgald01.dp.service.ApiData;
import cz.xgald01.dp.service.NasaDataProvider;
import cz.xgald01.dp.utils.ExcelExporter;
import cz.xgald01.dp.utils.JSONExporter;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;

/**
 * View pro NASA API
 */
public class NasaView extends VerticalLayout implements View {

    NasaData nasaData = new NasaData();
    private NasaDataProvider<NasaData> nasaDataProvider = new NasaDataProvider();
    private Window modalWindow;
    private FormLayout formLayout;
    private HorizontalLayout buttonsLayout;
    private DateField dateFrom;
    private Button request;
    private Button saveAs;
    private Button export;
    private Grid<JSONObject> grid;
    private String title;
    private DpUI ui;

    public NasaView(String pageTitle) {
        this.title = pageTitle;
        // Ziskani korenoveho UI
        ui = ((DpUI) UI.getCurrent());
        // Korenovy layout
        VerticalLayout rootLayout = new VerticalLayout();
        // Layout pro formular a tlacitka
        HorizontalLayout formAreaLayout = new HorizontalLayout();
        formAreaLayout.setSizeFull();
        formAreaLayout.setStyleName("form-style");
        // Layout pro grid
        VerticalLayout gridAreaLayout = new VerticalLayout();
        gridAreaLayout.setSizeFull();
        // Formularovy layout
        formLayout = new FormLayout();
        // Title layout
        HorizontalLayout titleLayout = new HorizontalLayout();
        Label apiTitle = new Label();
        apiTitle.setValue("NASA NEO - Zemi blízké asteoridy");
        titleLayout.addComponent(apiTitle);
        apiTitle.addStyleName("api-title-style");
        // Button layout
        buttonsLayout = new HorizontalLayout();
        buttonsLayout.setSizeUndefined();
        // Vytvorit formular
        createForm();
        // Rozmisteni komponent na strance
        formAreaLayout.addComponents(formLayout, titleLayout, buttonsLayout);
        formAreaLayout.setComponentAlignment(buttonsLayout, Alignment.MIDDLE_RIGHT);
        formAreaLayout.setComponentAlignment(titleLayout, Alignment.TOP_CENTER);
        rootLayout.addComponents(formAreaLayout, gridAreaLayout);
        // Binder pro formular
        final Binder<NasaData> dataBinder = new Binder<>(NasaData.class);
        dataBinder.bind(dateFrom, NasaData::getDateFrom, NasaData::setDateFrom);
        dataBinder.bind(dateFrom, NasaData::getDateTo, NasaData::setDateTo);
        dateFrom.addValueChangeListener(valueChangeEvent -> {
            nasaData.setDateFrom(dateFrom.getValue());
            nasaData.setDateTo(dateFrom.getValue());
            dataBinder.setBean(nasaData);
        });
        // Grid
        createGrid();
        // JSON export
        StreamResource jsonSource = new StreamResource(new JSONExporter(grid), "Json-Export.json");
        jsonSource.setCacheTime(0);
        FileDownloader fdJson = new FileDownloader(jsonSource);
        fdJson.extend(saveAs);
        // Excel export
        ExcelExporter excelExporter = new ExcelExporter(grid, nasaData);
        excelExporter.setReportTitle(getTitle());
        StreamResource excelSource = new StreamResource(excelExporter, "Grid-Export.xlsx");
        excelSource.setCacheTime(0);
        FileDownloader fdExcel = new FileDownloader(excelSource);
        fdExcel.extend(export);
        // pridat rootLayout
        rootLayout.addComponent(grid);
        addComponent(rootLayout);
    }

    // Vytvorit formular pro odesilani dotazu
    private void createForm(){
        // Prvky formulare
        dateFrom = new DateField();
        dateFrom.setCaption("Vyberte datum");
        dateFrom.setDateFormat("dd-MM-yyyy");
        dateFrom.setTextFieldEnabled(false);
        formLayout.addComponents(dateFrom);
        // Tlacitka
        request = new Button("Odeslat dotaz", new ThemeResource("icons/request.png"));
        saveAs = new Button("Uložit jako", new ThemeResource("icons/save-as.png"));
        export = new Button("Export", new ThemeResource("icons/excel.png"));
        request.setSizeUndefined();
        saveAs.setSizeUndefined();
        export.setSizeUndefined();
        buttonsLayout.addComponents(request, saveAs, export);
    }

    // Vytvorit grid pro zobrazeni vysledku dotazu
    private void createGrid(){
        grid = new Grid<>(JSONObject.class);
        grid.setSizeFull();
        grid.setDataProvider(nasaDataProvider);
        // Pridat sloupce do gridu
        request.addClickListener((Button.ClickEvent e) -> {
            try {
                if (nasaData.getDateFrom() == null) {
                    showErrorWindow();
                    ui.addWindow(modalWindow);
                } else {
                    // Zobrazeni vysledku dotazu v gridu
                    nasaDataProvider.setFilter(nasaData);
                    nasaDataProvider.refreshAll();
                    grid.removeAllColumns();
                    // Jednotlive radky gridu dle jsonObjectu
                    grid.addColumn(json -> {
                        try {
                            return getStringAttribute(json, nasaData.jsonKeys.get(0));
                        } catch (JSONException e1) {
                            throw new RuntimeException();
                        }
                    }).setCaption("Název asteroidu");
                    grid.addColumn(json -> {
                        try {
                            return getStringAttribute(json, nasaData.jsonKeys.get(1));
                        } catch (JSONException e1) {
                            throw new RuntimeException();
                        }
                    }).setCaption("Relativní rychlost (km/s)");
                    grid.addColumn(json -> {
                        try {
                            return getNumAttribute(json, nasaData.jsonKeys.get(2));
                        } catch (JSONException e1) {
                            throw new RuntimeException();
                        }
                    }).setCaption("Minimální průměr (m)");
                    grid.addColumn(json -> {
                        try {
                            return getNumAttribute(json, nasaData.jsonKeys.get(3));
                        } catch (JSONException e1) {
                            throw new RuntimeException();
                        }
                    }).setCaption("Maximální průměr (m)");
                    grid.addColumn(json -> {
                        try {
                            return getStringAttribute(json, nasaData.jsonKeys.get(4));
                        } catch (JSONException e1) {
                            throw new RuntimeException();
                        }
                    }).setCaption("Vzdálenost minutí Země (km)");
                }
            } catch (Exception a) {
                throw new RuntimeException();
            }
        });
    }

    // Ziskat nazev teto stranky
    private String getTitle() {
        return title;
    }

    // Ziskat konkretni stringovy atribut z JSONObjectu
    private String getStringAttribute(JSONObject json, String path) {
        return JsonPath.read(json.toString(), path);
    }

    // Ziskat konkretni numericky atribut z JSONObjectu
    private Double getNumAttribute(JSONObject json, String path) {
        return JsonPath.read(json.toString(), path);
    }

    // Zobrazit modalni okno
    private void showErrorWindow() {
        VerticalLayout vertLayout = new VerticalLayout();
        Label info = new Label("Není vyplněn formulář.");
        info.addStyleName("info-style");
        info.setSizeUndefined();
        vertLayout.addComponents(info);
        vertLayout.setComponentAlignment(info, Alignment.MIDDLE_CENTER);
        modalWindow = new Window("Upozornění", vertLayout);
        modalWindow.setModal(true);
        modalWindow.setWidth(300, Sizeable.Unit.PIXELS);
        modalWindow.setHeight(200, Sizeable.Unit.PIXELS);
        modalWindow.setWindowMode(WindowMode.NORMAL);
        modalWindow.addStyleName("popup-style");
        modalWindow.setResizable(false);
    }

    /**
     * Bean pro NASA data
     */
    public class NasaData extends ApiData {

        // Filtracni parametr
        LocalDate dateTo;

        // Gettery a settery
        public LocalDate getDateFrom() {
            return nasaDateFrom;
        }

        void setDateFrom(LocalDate dateFrom) {
            nasaDateFrom = dateFrom;
        }

        public LocalDate getDateTo() {
            return dateTo;
        }

        void setDateTo(LocalDate dateTo) {
            this.dateTo = dateTo;
        }

        NasaData() {
            super();
            // Pridat klice pro vyber potrebnych dat z jsonu
            jsonKeys.add(0, "name");
            jsonKeys.add(1, "close_approach_data[0].relative_velocity.kilometers_per_second");
            jsonKeys.add(2, "estimated_diameter.meters.estimated_diameter_min");
            jsonKeys.add(3, "estimated_diameter.meters.estimated_diameter_max");
            jsonKeys.add(4, "close_approach_data[0].miss_distance.kilometers");
        }
    }
}
