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
import cz.xgald01.dp.service.NasaDataProvider;
import cz.xgald01.dp.service.NasaEntity;
import cz.xgald01.dp.utils.ExcelExporter;
import cz.xgald01.dp.utils.JSONExporter;
import org.apache.commons.lang3.math.NumberUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;

/**
 * View for NASA API
 */
public class NasaView extends VerticalLayout implements View {

    NasaData nasaData = new NasaData();
    private NasaDataProvider<NasaData> nasaDataProvider = new NasaDataProvider();
    private static SessionFactory factory;
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
        apiTitle.setValue("NASA NEO - Near Earth Objects");
        titleLayout.addComponent(apiTitle);
        apiTitle.addStyleName("api-title-style");
        // Button layout
        buttonsLayout = new HorizontalLayout();
        buttonsLayout.setSizeUndefined();
        // Create form
        createForm();
        formAreaLayout.addComponents(formLayout, titleLayout, buttonsLayout);
        formAreaLayout.setComponentAlignment(buttonsLayout, Alignment.MIDDLE_RIGHT);
        formAreaLayout.setComponentAlignment(titleLayout, Alignment.TOP_CENTER);
        rootLayout.addComponents(formAreaLayout, gridAreaLayout);
        // Binder for form
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
        /// add rootLayout
        rootLayout.addComponent(grid);
        addComponent(rootLayout);
    }

    // Create a form to send queries
    private void createForm() {
        // Form elements
        dateFrom = new DateField();
        dateFrom.setCaption("Select a date");
        dateFrom.setDateFormat("dd-MM-yyyy");
        dateFrom.setTextFieldEnabled(false);
        formLayout.addComponents(dateFrom);
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
        grid.setDataProvider(nasaDataProvider);
        // Add a column to grid
        request.addClickListener((Button.ClickEvent e) -> {
            try {
                if (nasaData.getDateFrom() == null) {
                    showErrorWindow();
                    ui.addWindow(modalWindow);
                } else {
                    // Show query result
                    nasaDataProvider.setFilter(nasaData);
                    nasaDataProvider.refreshAll();
                    grid.removeAllColumns();
                    nasaDataProvider.sendQuery(new Query<>());
                    JSONArray result = nasaDataProvider.getResultData();
                    // Grid rows according to a result
                    grid.addColumn(json -> {
                        try {
                            return getStringAttribute(json, nasaData.jsonKeys.get(0));
                        } catch (JSONException e1) {
                            throw new RuntimeException();
                        }
                    }).setCaption("Asteroid name");
                    grid.addColumn(json -> {
                        try {
                            return getStringAttribute(json, nasaData.jsonKeys.get(1));
                        } catch (JSONException e1) {
                            throw new RuntimeException();
                        }
                    }).setCaption("Relative velocity (km/s)");
                    grid.addColumn(json -> {
                        try {
                            return getNumAttribute(json, nasaData.jsonKeys.get(2));
                        } catch (JSONException e1) {
                            throw new RuntimeException();
                        }
                    }).setCaption("Minimal diameter (m)");
                    grid.addColumn(json -> {
                        try {
                            return getNumAttribute(json, nasaData.jsonKeys.get(3));
                        } catch (JSONException e1) {
                            throw new RuntimeException();
                        }
                    }).setCaption("Maximal diameter (m)");
                    grid.addColumn(json -> {
                        try {
                            return getStringAttribute(json, nasaData.jsonKeys.get(4));
                        } catch (JSONException e1) {
                            throw new RuntimeException();
                        }
                    }).setCaption("Distance from Earth (km)");
                    // Insert data into database
                    for (int i = 0; i < result.length(); i++) {
                        dbInsert(getStringAttribute(result.getJSONObject(i), nasaData.jsonKeys.get(0)),
                                getStringAttribute(result.getJSONObject(i), nasaData.jsonKeys.get(4)));
                    }
                }
            } catch (Exception a) {
                throw new RuntimeException();
            }
        });
    }

    // Insert given data into database
    private void dbInsert(String name, String distance) {
        Session session = factory.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            NasaEntity nasaEntity = new NasaEntity();
            nasaEntity.setName(name);
            nasaEntity.setEarthDistance(Math.round(NumberUtils.createFloat(distance)));
            session.save(nasaEntity);
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
    private Double getNumAttribute(JSONObject json, String path) {
        return JsonPath.read(json.toString(), path);
    }

    // Show modal window
    private void showErrorWindow() {
        VerticalLayout vertLayout = new VerticalLayout();
        Label info = new Label("No date selected.");
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
     * Bean for NASA data
     */
    public class NasaData extends ApiData {

        // Filter parameter
        LocalDate dateTo;

        // Getters and setters
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
            // Keys for json data selection
            jsonKeys.add(0, "name");
            jsonKeys.add(1, "close_approach_data[0].relative_velocity.kilometers_per_second");
            jsonKeys.add(2, "estimated_diameter.meters.estimated_diameter_min");
            jsonKeys.add(3, "estimated_diameter.meters.estimated_diameter_max");
            jsonKeys.add(4, "close_approach_data[0].miss_distance.kilometers");
        }
    }
}
