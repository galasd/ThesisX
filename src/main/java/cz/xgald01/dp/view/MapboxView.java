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
import cz.xgald01.dp.service.CapitalsService;
import cz.xgald01.dp.service.MapboxDataProvider;
import cz.xgald01.dp.utils.ExcelExporter;
import cz.xgald01.dp.utils.JSONExporter;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * View pro Mapbox API
 */
public class MapboxView extends VerticalLayout implements View {

    private MapboxDataProvider<MapboxData> mapboxDataProvider = new MapboxDataProvider();
    private CapitalsService capitalsService = new CapitalsService();
    private Window modalWindow;
    private HorizontalLayout buttonsLayout;
    private FormLayout formLayout;
    private ComboBox<String> cities;
    private Button request;
    private Button saveAs;
    private Button export;
    private Grid<JSONObject> grid;
    private String title;
    private String selectedCity;
    private String cityEN;
    private DpUI ui;
    MapboxData mapboxData = new MapboxData();

    public MapboxView(String pageTitle) {
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
        apiTitle.setValue("Mapbox - Geokódování");
        titleLayout.addComponent(apiTitle);
        apiTitle.addStyleName("api-title-style");
        // Button layout
        buttonsLayout = new HorizontalLayout();
        // Vytvorit formular
        createForm();
        // Rozmisteni komponent na strance
        formAreaLayout.addComponents(formLayout, titleLayout, buttonsLayout);
        formAreaLayout.setComponentAlignment(buttonsLayout, Alignment.MIDDLE_RIGHT);
        formAreaLayout.setComponentAlignment(titleLayout, Alignment.TOP_CENTER);
        rootLayout.addComponents(formAreaLayout, gridAreaLayout);
        // Binder pro formular
        final Binder<MapboxData> dataBinder = new Binder<>(MapboxData.class);
        dataBinder.bind(cities, MapboxData::getSearchText, MapboxData::setSearchText);
        cities.addValueChangeListener(valueChangeEvent -> {
            mapboxData.setSearchText(cities.getValue());
            dataBinder.setBean(mapboxData);
        });
        // Grid
        createGrid();
        // JSON export
        StreamResource jsonSource = new StreamResource(new JSONExporter(grid), "Json-Export.json");
        jsonSource.setCacheTime(0);
        FileDownloader fdJson = new FileDownloader(jsonSource);
        fdJson.extend(saveAs);
        // Excel export
        ExcelExporter excelExporter = new ExcelExporter(grid, mapboxData);
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
    private void createForm() {
        buttonsLayout.setSizeUndefined();
        // Prvky formulare
        cities = new ComboBox<>();
        cities.setCaption("Vyberte město");
        cities.setItems(capitalsService.getCities());
        cities.setEmptySelectionAllowed(false);
        cities.setTextInputAllowed(false);
        cities.setPageLength(9);
        formLayout.addComponents(cities);
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
    private void createGrid() {
        grid = new Grid<>(JSONObject.class);
        grid.setSizeFull();
        grid.setDataProvider(mapboxDataProvider);
        // Pridat sloupce do gridu
        request.addClickListener((Button.ClickEvent e) -> {
            try {
                if (mapboxData.getSearchText() == null) {
                    showErrorWindow();
                    ui.addWindow(modalWindow);
                } else {
                    // Prelozit vybrane hlavni mesto
                    selectedCity = mapboxData.getSearchText();
                    // Pokud je odeslan znovu ten samy dotaz a mesto uz bylo prelozeno do EN, neprekladat
                    if(!selectedCity.equals(cityEN)) {
                        cityEN = capitalsService.translate(selectedCity);
                    }
                    // Odeslat vybrane hlavni mesto v EN
                    mapboxData.setSearchText(cityEN);
                    // Zobrazeni vysledku dotazu v gridu
                    mapboxDataProvider.setFilter(mapboxData);
                    mapboxDataProvider.refreshAll();
                    grid.removeAllColumns();
                    // Jednotlive radky gridu dle jsonObjectu
                    grid.addColumn(json -> {
                        try {
                            return getStringAttribute(json, mapboxData.jsonKeys.get(0));
                        } catch (JSONException e1) {
                            throw new RuntimeException();
                        }
                    }).setCaption("Název a stát");
                    grid.addColumn(json -> {
                        try {
                            return getNumAttribute(json, mapboxData.jsonKeys.get(1));
                        } catch (JSONException e1) {
                            throw new RuntimeException();
                        }
                    }).setCaption("Zeměpisná šířka");
                    grid.addColumn(json -> {
                        try {
                            return getNumAttribute(json, mapboxData.jsonKeys.get(2));
                        } catch (JSONException e1) {
                            throw new RuntimeException();
                        }
                    }).setCaption("Zeměpisná délka");

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
    private Number getNumAttribute(JSONObject json, String path) {

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
     * Bean pro Mapbox data
     */
    public class MapboxData extends ApiData {

        // Filtracni parametr
        String searchText;

        // Getter a setter
        public String getSearchText() {
            return searchText;
        }

        void setSearchText(String searchText) {
            this.searchText = searchText;
        }

        MapboxData() {
            super();
            // Pridat klice pro vyber potrebnych dat z jsonu
            jsonKeys.add(0, "place_name");
            jsonKeys.add(1, "center[0]");
            jsonKeys.add(2, "center[1]");
        }
    }
}
