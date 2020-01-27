package cz.xgald01.dp.view;

import com.jayway.jsonpath.JsonPath;
import com.vaadin.data.Binder;
import com.vaadin.data.provider.Query;
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
import cz.xgald01.dp.service.MapboxEntity;
import cz.xgald01.dp.utils.ExcelExporter;
import cz.xgald01.dp.utils.JSONExporter;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * View for Mapbox API
 */
public class MapboxView extends VerticalLayout implements View {

    private MapboxDataProvider<MapboxData> mapboxDataProvider = new MapboxDataProvider();
    private static SessionFactory factory;
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
    private DpUI ui;
    MapboxData mapboxData = new MapboxData();

    public MapboxView(String pageTitle) {
        this.title = pageTitle;
        factory = buildSessionFactory();
        // Get a root UI
        ui = ((DpUI) UI.getCurrent());
        // Root layout
        VerticalLayout rootLayout = new VerticalLayout();
        // Layout for form and buttons
        HorizontalLayout formAreaLayout = new HorizontalLayout();
        formAreaLayout.setSizeFull();
        formAreaLayout.setStyleName("form-style");
        // Layout for grid
        VerticalLayout gridAreaLayout = new VerticalLayout();
        gridAreaLayout.setSizeFull();
        // Layout for form
        formLayout = new FormLayout();
        // Title layout
        HorizontalLayout titleLayout = new HorizontalLayout();
        Label apiTitle = new Label();
        apiTitle.setValue("Mapbox - Geocoding");
        titleLayout.addComponent(apiTitle);
        apiTitle.addStyleName("api-title-style");
        // Button layout
        buttonsLayout = new HorizontalLayout();
        // Create form
        createForm();
        formAreaLayout.addComponents(formLayout, titleLayout, buttonsLayout);
        formAreaLayout.setComponentAlignment(buttonsLayout, Alignment.MIDDLE_RIGHT);
        formAreaLayout.setComponentAlignment(titleLayout, Alignment.TOP_CENTER);
        rootLayout.addComponents(formAreaLayout, gridAreaLayout);
        // Binder for form
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
        // add rootLayout
        rootLayout.addComponent(grid);
        addComponent(rootLayout);
    }

    // Create a form to send queries
    private void createForm() {
        buttonsLayout.setSizeUndefined();
        // Form elements
        cities = new ComboBox<>();
        cities.setCaption("Select a city");
        cities.setItems(capitalsService.getCities());
        cities.setEmptySelectionAllowed(false);
        cities.setTextInputAllowed(false);
        cities.setPageLength(9);
        formLayout.addComponents(cities);
        request = new Button("Send request", new ThemeResource("icons/request.png"));
        saveAs = new Button("Save as", new ThemeResource("icons/save-as.png"));
        export = new Button("Export", new ThemeResource("icons/excel.png"));
        request.setSizeUndefined();
        saveAs.setSizeUndefined();
        export.setSizeUndefined();
        buttonsLayout.addComponents(request, saveAs, export);
    }

    // Create a grid to show query results
    private void createGrid() {
        grid = new Grid<>(JSONObject.class);
        grid.setSizeFull();
        grid.setDataProvider(mapboxDataProvider);
        // Add a column to grid
        request.addClickListener((Button.ClickEvent e) -> {
            try {
                if (mapboxData.getSearchText() == null) {
                    showErrorWindow();
                    ui.addWindow(modalWindow);
                } else {
                    // Show query result
                    mapboxDataProvider.setFilter(mapboxData);
                    mapboxDataProvider.refreshAll();
                    grid.removeAllColumns();
                    mapboxDataProvider.sendQuery(new Query<>());
                    JSONObject result = mapboxDataProvider.getResultData();
                    // Grid rows according to a result
                    grid.addColumn(json -> {
                        try {
                            return getStringAttribute(json, mapboxData.jsonKeys.get(0));
                        } catch (JSONException e1) {
                            throw new RuntimeException();
                        }
                    }).setCaption("Name a country");
                    grid.addColumn(json -> {
                        try {
                            return getNumAttribute(json, mapboxData.jsonKeys.get(1));
                        } catch (JSONException e1) {
                            throw new RuntimeException();
                        }
                    }).setCaption("Latitude");
                    grid.addColumn(json -> {
                        try {
                            return getNumAttribute(json, mapboxData.jsonKeys.get(2));
                        } catch (JSONException e1) {
                            throw new RuntimeException();
                        }
                    }).setCaption("Longitude");
                    // Insert data into database
                    dbInsert(String.valueOf(result.get(mapboxData.jsonKeys.get(0))),
                            Double.valueOf(getNumAttribute(result, mapboxData.jsonKeys.get(1)).toString()),
                            Double.valueOf(getNumAttribute(result, mapboxData.jsonKeys.get(2)).toString()));
                }
            } catch (Exception a) {
                throw new RuntimeException();
            }
        });
    }

    // Insert given data into database
    private void dbInsert(String name, Double latitude, Double longitude) {
        Session session = factory.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            MapboxEntity mapboxEntity = new MapboxEntity();
            mapboxEntity.setName(name);
            mapboxEntity.setLatitude(latitude);
            mapboxEntity.setLongitude(longitude);
            session.save(mapboxEntity);
            tx.commit();
        } catch (HibernateException e) {
            if (tx != null)
                tx.rollback();
            throw new HibernateException(e);
        } finally {
            session.close();
        }
    }

    // Build a session factory for transactions
    private SessionFactory buildSessionFactory() {
        return new Configuration().configure("hibernate.cfg.xml").buildSessionFactory();
    }

    // Get a page title
    private String getTitle() {
        return title;
    }

    // Get a given String attribute from JSONObjectu
    private String getStringAttribute(JSONObject json, String path) {
        return JsonPath.read(json.toString(), path);
    }

    // Get a given Number attribute from JSONObjectu
    private Number getNumAttribute(JSONObject json, String path) {
        return JsonPath.read(json.toString(), path);
    }

    // Show modal window
    private void showErrorWindow() {
        VerticalLayout vertLayout = new VerticalLayout();
        Label info = new Label("No city selected.");
        info.addStyleName("info-style");
        info.setSizeUndefined();
        vertLayout.addComponents(info);
        vertLayout.setComponentAlignment(info, Alignment.MIDDLE_CENTER);
        modalWindow = new Window("Warning", vertLayout);
        modalWindow.setModal(true);
        modalWindow.setWidth(300, Sizeable.Unit.PIXELS);
        modalWindow.setHeight(200, Sizeable.Unit.PIXELS);
        modalWindow.setWindowMode(WindowMode.NORMAL);
        modalWindow.addStyleName("popup-style");
        modalWindow.setResizable(false);
    }

    /**
     * Bean for Mapbox data
     */
    public class MapboxData extends ApiData {
        // Filter parameter
        String searchText;

        // Getter and setter
        public String getSearchText() {
            return searchText;
        }

        void setSearchText(String searchText) {
            this.searchText = searchText;
        }

        MapboxData() {
            super();
            // Keys for json data selection
            jsonKeys.add(0, "place_name");
            jsonKeys.add(1, "center[0]");
            jsonKeys.add(2, "center[1]");
        }
    }
}
