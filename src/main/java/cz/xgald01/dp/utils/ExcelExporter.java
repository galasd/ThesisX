package cz.xgald01.dp.utils;

import com.jayway.jsonpath.JsonPath;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.server.StreamResource;
import com.vaadin.ui.Grid;
import cz.xgald01.dp.service.ApiData;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Excel exporter for grid a data bean
 */
public class ExcelExporter implements StreamResource.StreamSource {

    private static final Logger log = LoggerFactory.getLogger(ExcelExporter.class);
    private final Grid grid;
    private XSSFWorkbook workbook;
    private XSSFSheet exportSheet;
    private XSSFCellStyle titleStyle;
    private XSSFCellStyle headerStyle;
    private XSSFFont titleFont;
    private String title;
    private List<String> viewKeys = null;
    private ApiData beanData;
    JSONObject jsonData = null;
    JSONArray jArray = null;

    public ExcelExporter(Grid grid, ApiData apiData) {

        this.grid = grid;
        this.beanData = apiData;
    }

    @Override
    public InputStream getStream() {
        try {
            init();
            // Get an applied  data provider
            DataProvider currentDataProvider = grid.getDataProvider();
            currentDataProvider.refreshAll();
            // Get an object containing the json data
            Field queryResult;
            queryResult = currentDataProvider.getClass().getDeclaredField("requestResult");
            // Get an object containing the API type
            Field apiType;
            apiType = currentDataProvider.getClass().getDeclaredField("apiType");
            String api = (String) apiType.get(currentDataProvider);
            // JSON array data length
            int dataSize = 0;
            // In case of no data in a response, return an empty file with a warning
            if (queryResult.get(currentDataProvider) == null) {
                Row row = exportSheet.createRow(0);
                Cell cell = row.createCell(0);
                cell.setCellValue("No API query was sent.");
            } else {
                jsonData = (JSONObject) queryResult.get(currentDataProvider);
                // Create headers
                createHeaderRow();
                // In case of non empty API response
                // NASA API
                if (Objects.equals(api, "Nasa")) {
                    jArray = jsonData.getJSONObject("near_earth_objects").getJSONArray(String.valueOf(beanData.nasaDateFrom));
                    viewKeys = beanData.jsonKeys;
                    dataSize = jArray.length();
                }
                // Mapbox API
                if (Objects.equals(api, "Mapbox")) {
                    jArray = jsonData.getJSONArray("features");
                    viewKeys = beanData.jsonKeys;
                    dataSize = 1;
                }
                // Get and fill rows and cells under given header
                int firstRowIndex = 2;
                int normalColumnCount = 0;
                // Get json keys
                List<String> keys = viewKeys;
                for (int b = 0; b < dataSize; b++) {
                    Row row = exportSheet.createRow(firstRowIndex);
                    for (String key : keys) {
                        String textField = String.valueOf(getJSONObjectAttribute(jArray.getJSONObject(b), key));
                        int maxContentLength = 32766;
                        if (textField.length() > maxContentLength) {
                            textField.substring(0, 1500);
                        }
                        // Fill excel cells with keys
                        Cell cell = row.createCell(normalColumnCount);
                        cell.setCellValue(textField);
                        normalColumnCount++;
                    }
                    normalColumnCount = 0;
                    firstRowIndex++;
                }
                // Create title row and format cells
                createTitleRow();
                formatCells();
            }
            // Write the workbook into baos
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            workbook.close();
            baos.close();
            log.info("Export " + apiType.get(currentDataProvider) + " into MS EXCEL format.");
            return new ByteArrayInputStream(baos.toByteArray());
        } catch (IllegalAccessException | IOException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    // Creates a workbook and styles
    private void init() {
        workbook = new XSSFWorkbook();
        exportSheet = workbook.createSheet("Export");
        titleStyle = workbook.createCellStyle();
        headerStyle = workbook.createCellStyle();
        titleFont = workbook.createFont();
    }

    // Creates a title row
    private void createTitleRow() {
        XSSFRow titleRow = exportSheet.createRow(0);
        titleRow.createCell(0).setCellValue(title);
        titleRow.getCell(0).setCellStyle(titleStyle);
        titleFont.setBold(true);
        titleFont.setFontHeight(24);
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        CellRangeAddress titleRange = new CellRangeAddress(0, 0, 0, (exportSheet.getRow(1).getLastCellNum() - 1));
        exportSheet.addMergedRegion(titleRange);
        exportSheet.validateMergedRegions();
    }

    // Creates a header row
    private void createHeaderRow() {
        int headerCellNum = 0;
        int headerColumnNum = 0;
        if (workbook.getSheet("Export") == null) {
            exportSheet = workbook.createSheet("Export");
        }
        int screenWidth = (int) Toolkit.getDefaultToolkit().getScreenSize().getWidth();
        XSSFFont headerFont = workbook.createFont();
        // Fill header row with data
        int rowNum = 1;
        Row exportHeaderRow = exportSheet.createRow(rowNum);
        String cellValue;
        for (int a = 0; a < grid.getColumns().size(); a++) {
            cellValue = ((Grid.Column) grid.getColumns().get(headerColumnNum)).getCaption();
            // Cells for headers
            Cell headerCell = exportHeaderRow.createCell(headerCellNum);
            headerCell.setCellValue(cellValue);
            headerFont.setFontHeight(12);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerCell.setCellStyle(headerStyle);
            // Set width for columns
            int cellValueLegth = cellValue.length();
            int screenWidthAdj = screenWidth / 3;
            // If a given cell is wider than 1/3 of screen size, make it smaller
            if (cellValueLegth >= screenWidthAdj) {
                exportSheet.setColumnWidth(headerColumnNum, screenWidthAdj * 256);
            } else {
                if (cellValue.length() < 6) {
                    exportSheet.setColumnWidth(headerColumnNum, (cellValue.length()) * 2000);
                } else {
                    exportSheet.setColumnWidth(headerColumnNum, (cellValue.length()) * 500);
                }
            }
            headerCellNum++;
            headerColumnNum++;
        }
    }

    // Average length of legths in a list
    private double calculateAverage(List<Integer> lengths) {
        List<Integer> values = new ArrayList<>();
        Integer sum = 0;
        if (!lengths.isEmpty()) {
            for (Integer length : lengths) {
                if (length > 0) {
                    values.add(length);
                    sum += length;
                }
            }
            return sum.doubleValue() / values.size();
        }
        return sum;
    }

    // Geta  given attribute from JSONObject
    private Object getJSONObjectAttribute(JSONObject jObject, String path) {
        return JsonPath.read(jObject.toString(), path);
    }

    // Sets a title for export
    public void setReportTitle(String reportTitle) {
        this.title = reportTitle;
    }

    // Format cells in a sheet
    private void formatCells() {
        int columnsUsed = exportSheet.getRow(1).getPhysicalNumberOfCells();
        int rowsUsed = exportSheet.getPhysicalNumberOfRows();
        // Get a width of each cell in a sheet
        List<Integer> headerCellLengths = new ArrayList<>();
        for (int a = 0; a < exportSheet.getRow(1).getPhysicalNumberOfCells(); a++) {
            headerCellLengths.add(exportSheet.getRow(1).getCell(a).getStringCellValue().length());
        }
        // Set cells height
        exportSheet.getRow(1).setHeight((short) 800);
        // Set cells width
        int customCellWidth = 0;
        for (int b = 0; b < headerCellLengths.size(); b++) {
            int headerWidth = headerCellLengths.get(b);
            List<Integer> stringCellLengths = new ArrayList<>();
            for (int c = 1; c < rowsUsed; c++) {
                Cell selectedCell = exportSheet.getRow(c).getCell(b);
                stringCellLengths.add(selectedCell.toString().length());
                if (NumberUtils.isNumber(selectedCell.toString())) {
                    CellStyle rightAligned = workbook.createCellStyle();
                    rightAligned.setAlignment(HorizontalAlignment.RIGHT);
                    selectedCell.setCellStyle(rightAligned);
                }
            }
            // Average cell width in given column
            int averageStringCellWidth = (int) calculateAverage(stringCellLengths);
            if (averageStringCellWidth > headerWidth) {
                customCellWidth = averageStringCellWidth * 2;
            } else {
                customCellWidth = headerWidth * 2;
            }
            // Max number of characters in an excel cell
            int MAX_COLUMN_WIDTH = 255;
            if (customCellWidth > MAX_COLUMN_WIDTH) {
                customCellWidth = MAX_COLUMN_WIDTH;
            }
            //setColumnWidth sets width/256 by default, thus multiply by 256
            exportSheet.setColumnWidth(b, customCellWidth * 256);
            // Erase all data in an array to use it for another column
            stringCellLengths.clear();
        }
    }
}
