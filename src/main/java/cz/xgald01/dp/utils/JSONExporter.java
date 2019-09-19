package cz.xgald01.dp.utils;

import com.vaadin.data.provider.DataProvider;
import com.vaadin.server.StreamResource;
import com.vaadin.ui.Grid;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * JSON exporter pro grid
 */
public class JSONExporter implements StreamResource.StreamSource {

    private static final Logger log = LoggerFactory.getLogger(JSONExporter.class);
    private final Grid grid;
    JSONObject apiJSONData = null;

    public JSONExporter(Grid grid) {

        this.grid = grid;
    }

    @Override
    public InputStream getStream() {
        try {
            // Vytvoreni souboru pro zapis dat
            File file = new File(File.createTempFile("ApiData", ".json").toURI());
            // Ziskani pouziteho data provideru
            DataProvider jsonDataProvider = grid.getDataProvider();
            // Cesta k objektu obsahujicimu typ pouzivaneho API
            Field apiType;
            apiType = jsonDataProvider.getClass().getDeclaredField("apiType");
            // Cesta k objektu obsahujicimu json data
            Field queryResult;
            queryResult = jsonDataProvider.getClass().getDeclaredField("requestResult");
            // String obsahujici typ pouzivaneho API
            String api = (String) apiType.get(jsonDataProvider);
            // Ziskani objektu obsahujiciho JSON data od pouziteho data provideru
            apiJSONData = (JSONObject) queryResult.get(jsonDataProvider);
            // Pokud byl vracena prazdna odpoved, vytvorit prazdne mock pole
            if (apiJSONData == null) {
                if (Objects.equals(api, "Nasa")) {
                    apiJSONData = new JSONObject("{near_earth_objects:{Nebyl odeslan dotaz na API.}}");
                }
                if (Objects.equals(api, "Mapbox")) {
                    apiJSONData = new JSONObject("{features:[Nebyl odeslan dotaz na API.]}");
                }
            }
            // Pokud byla vracena odpoved obsahujici data, ziskat z odpovedi potrebnou cast dat
            Object jObject = null;
            if (Objects.equals(api, "Nasa")) {
                jObject = apiJSONData.getJSONObject("near_earth_objects");
            }
            if (Objects.equals(api, "Mapbox")) {
                jObject = apiJSONData.getJSONArray("features");
            }
            // Zapsat data do souboru
            FileWriter fw = new FileWriter(file);
            fw.write(String.valueOf(jObject));
            fw.close();
            // Ulozit obsah souboru do byteArray
            byte[] bF = Files.readAllBytes(Paths.get(file.getPath()));
            // Zapsat byteArray do baos
            log.info("Stazeni kompletni odpovedi od " + apiType.get(jsonDataProvider) + " API ve formatu JSON.");
            return new ByteArrayInputStream(bF);

        } catch (IOException | NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}




