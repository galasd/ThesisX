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
 * JSON exporter for grid
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
            // Create a file for json data
            File file = new File(File.createTempFile("ApiData", ".json").toURI());
            // Get an appplied data provider
            DataProvider jsonDataProvider = grid.getDataProvider();
            // Get an object containing the API type
            Field apiType;
            apiType = jsonDataProvider.getClass().getDeclaredField("apiType");
            // Get an object containing the json data
            Field queryResult;
            queryResult = jsonDataProvider.getClass().getDeclaredField("requestResult");
            String api = (String) apiType.get(jsonDataProvider);
            // Get a JSON data object from a given data provider
            apiJSONData = (JSONObject) queryResult.get(jsonDataProvider);
            // In case of an empty API response, create an empty mock array
            if (apiJSONData == null) {
                if (Objects.equals(api, "Nasa")) {
                    apiJSONData = new JSONObject("{near_earth_objects:{No API query was sent.}}");
                }
                if (Objects.equals(api, "Mapbox")) {
                    apiJSONData = new JSONObject("{features:[No API query was sent.]}");
                }
            }
            // In case of non empty response, get the result data
            Object jObject = null;
            if (Objects.equals(api, "Nasa")) {
                jObject = apiJSONData.getJSONObject("near_earth_objects");
            }
            if (Objects.equals(api, "Mapbox")) {
                jObject = apiJSONData.getJSONArray("features");
            }
            // Write data into file
            FileWriter fw = new FileWriter(file);
            fw.write(String.valueOf(jObject));
            fw.close();
            // Save the file into byteArray
            byte[] bF = Files.readAllBytes(Paths.get(file.getPath()));
            // Write the byteArray into baos
            log.info("A response from " + apiType.get(jsonDataProvider) + " API in JSON format was downloaded.");
            return new ByteArrayInputStream(bF);

        } catch (IOException | NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}




