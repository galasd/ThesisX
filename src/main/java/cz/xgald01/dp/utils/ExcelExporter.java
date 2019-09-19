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
 * Excel exporter pro grid a data bean
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
            // Ziskani pouziteho data provideru
            DataProvider currentDataProvider = grid.getDataProvider();
            currentDataProvider.refreshAll();
            // Cesta k objektu obsahujicimu json data
            Field queryResult;
            queryResult = currentDataProvider.getClass().getDeclaredField("requestResult");
            // Cesta k objektu obsahujicimu typ pouzivaneho API
            Field apiType;
            apiType = currentDataProvider.getClass().getDeclaredField("apiType");
            String api = (String) apiType.get(currentDataProvider);
            // Delka json array s daty k exportu
            int dataSize = 0;
            // Pokud je vracen prazdny zaznam, vratit prazdny sobor s jednim sheetem a upozornenim
            if (queryResult.get(currentDataProvider) == null) {
                Row row = exportSheet.createRow(0);
                Cell cell = row.createCell(0);
                cell.setCellValue("Nebyl odeslán dotaz na API.");
            } else {
                jsonData = (JSONObject) queryResult.get(currentDataProvider);
                // Vytvorit headery
                createHeaderRow();
                // Pokud je vracen neprazdny zaznam, zapsat data do sheetu
                // Pokud se jedna o data od NASA API
                if (Objects.equals(api, "Nasa")) {
                    jArray = jsonData.getJSONObject("near_earth_objects").getJSONArray(String.valueOf(beanData.nasaDateFrom));
                    viewKeys = beanData.jsonKeys;
                    dataSize = jArray.length();
                }
                // Pokud se jedna o data od Mapbox API
                if (Objects.equals(api, "Mapbox")) {
                    jArray = jsonData.getJSONArray("features");
                    viewKeys = beanData.jsonKeys;
                    dataSize = 1;
                }
                // Ziskani a naplneni jednotlivych radku a bunek pod headerem
                int firstRowIndex = 2;
                int normalColumnCount = 0;
                // Ziskani klicu z jsonu
                List<String> keys = viewKeys;
                for (int b = 0; b < dataSize; b++) {
                    Row row = exportSheet.createRow(firstRowIndex);
                    for (String key : keys) {
                        String textField = String.valueOf(getJSONObjectAttribute(jArray.getJSONObject(b), key));
                        int maxContentLength = 32766;
                        if (textField.length() > maxContentLength) {
                            textField.substring(0, 1500);
                        }
                        // Naplneni bunek klici
                        Cell cell = row.createCell(normalColumnCount);
                        cell.setCellValue(textField);
                        normalColumnCount++;
                    }
                    normalColumnCount = 0;
                    firstRowIndex++;
                }
                // Vytvorit titulni radku a zformatovat bunky
                createTitleRow();
                formatCells();
            }
            // Zapsat vytvoreny workbook do baos
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            workbook.close();
            baos.close();
            log.info("Export " + apiType.get(currentDataProvider) + " dat od formatu MS EXCEL.");
            return new ByteArrayInputStream(baos.toByteArray());
        } catch (IllegalAccessException | IOException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    // Zalozit workbook a jednotlive styly, ktere se pouzivaji dale
    private void init() {
        workbook = new XSSFWorkbook();
        exportSheet = workbook.createSheet("Export");
        titleStyle = workbook.createCellStyle();
        headerStyle = workbook.createCellStyle();
        titleFont = workbook.createFont();
    }

    // Zalozit titulni radku a vytvorit jednotlive styly
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

    // Vytvorit radku s headery jendotlivych sloupcu
    private void createHeaderRow() {
        int headerCellNum = 0;
        int headerColumnNum = 0;
        // Pokud jeste neni vytvoren list "Export", vytvorit
        if (workbook.getSheet("Export") == null) {
            exportSheet = workbook.createSheet("Export");
        }
        int screenWidth = (int) Toolkit.getDefaultToolkit().getScreenSize().getWidth();
        XSSFFont headerFont = workbook.createFont();
        // Naplneni header radku a jeho jednotlivych bunek
        int rowNum = 1;
        Row exportHeaderRow = exportSheet.createRow(rowNum);
        String cellValue;
        for (int a = 0; a < grid.getColumns().size(); a++) {
            cellValue = ((Grid.Column) grid.getColumns().get(headerColumnNum)).getCaption();
            // Bunky pro headery
            Cell headerCell = exportHeaderRow.createCell(headerCellNum);
            headerCell.setCellValue(cellValue);
            // Nazvy tucne
            headerFont.setFontHeight(12);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerCell.setCellStyle(headerStyle);
            // Nastaveni sirky sloupcu
            int cellValueLegth = cellValue.length();
            int screenWidthAdj = screenWidth / 3;
            // Pokud je dana bunka sirsi nez tretina obrazovky, zuzit z estetickych duvodu
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

    // Vypocitat prumernou hodnotu delky hodnot, ulozenych v Listu
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

    // Ziskat konkretni atribut z JSONObjectu
    private Object getJSONObjectAttribute(JSONObject jObject, String path) {
        return JsonPath.read(jObject.toString(), path);
    }

    // Nastavit title pro export
    public void setReportTitle(String reportTitle) {
        this.title = reportTitle;
    }

    // Zformatovat bunky v sheetu
    private void formatCells() {
        // Ziskat pocet vyuzitych sloupcu
        int columnsUsed = exportSheet.getRow(1).getPhysicalNumberOfCells();
        // Ziskat pocet vyuzitych radek
        int rowsUsed = exportSheet.getPhysicalNumberOfRows();
        //Ziskat sirku kazde bunky z radku headeru
        List<Integer> headerCellLengths = new ArrayList<>();
        for (int a = 0; a < exportSheet.getRow(1).getPhysicalNumberOfCells(); a++) {
            headerCellLengths.add(exportSheet.getRow(1).getCell(a).getStringCellValue().length());
        }
        // Nastavit vysku header bunek
        exportSheet.getRow(1).setHeight((short) 800);
        // Nastavit sirku bunek
        int customCellWidth = 0;
        // Iterace pro kazdy vyuzity sloupec tabulky
        for (int b = 0; b < headerCellLengths.size(); b++) {
            int headerWidth = headerCellLengths.get(b);
            List<Integer> stringCellLengths = new ArrayList<>();
            // Iterace pro kazdy vyuzity radek tabulky
            for (int c = 1; c < rowsUsed; c++) {
                Cell selectedCell = exportSheet.getRow(c).getCell(b);
                stringCellLengths.add(selectedCell.toString().length());
                //Pokud je v bunce cislo, zarovnat doprava
                if (NumberUtils.isNumber(selectedCell.toString())) {
                    CellStyle rightAligned = workbook.createCellStyle();
                    rightAligned.setAlignment(HorizontalAlignment.RIGHT);
                    selectedCell.setCellStyle(rightAligned);
                }
            }
            // Prumerna sirka bunky v danem sloupci
            int averageStringCellWidth = (int) calculateAverage(stringCellLengths);
            // Porovnani prumerne sirky bunek ve sloupci a sirky nadpisu sloupce
            if (averageStringCellWidth > headerWidth) {
                customCellWidth = averageStringCellWidth * 2;
            } else {
                customCellWidth = headerWidth * 2;
            }
            // Maximalni pocet znaku, ktery lze zapsat do bunky
            int MAX_COLUMN_WIDTH = 255;
            if (customCellWidth > MAX_COLUMN_WIDTH) {
                customCellWidth = MAX_COLUMN_WIDTH;
            }
            // Nastavit zvolenemu sloupci sirku
            // Metoda setColumnWidth defaultne nastavuje width/256, proto je nutne parametr znasobit*256
            exportSheet.setColumnWidth(b, customCellWidth * 256);
            // Vymazat pole kvuli pouziti pro dalsi sloupec
            stringCellLengths.clear();
        }
    }
}
