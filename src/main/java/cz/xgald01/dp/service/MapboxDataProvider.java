package cz.xgald01.dp.service;

import com.vaadin.data.provider.AbstractBackEndDataProvider;
import com.vaadin.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.data.provider.Query;
import cz.xgald01.dp.view.MapboxView;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.AbstractList;
import java.util.stream.Stream;

/**
 * Data provider for Mapbox API
 */
public class MapboxDataProvider<T> extends AbstractBackEndDataProvider<JSONObject, T> implements ConfigurableFilterDataProvider<JSONObject, T, T> {

    private static final Logger log = LoggerFactory.getLogger(MapboxDataProvider.class);
    private HttpClient client = new HttpClient();
    public JSONObject requestResult = null;
    public JSONObject requestData = null;
    public String apiType = "Mapbox";
    MapboxView.MapboxData mapboxData;

    /**
     * Data query made by grid
     *
     * @param query API query
     * @return JSONObject stream according to given query
     */
    @Override
    protected Stream<JSONObject> fetchFromBackEnd(Query<JSONObject, T> query) {

        return sendQuery(query);
    }

    /**
     * Returns a number of records
     *
     * @param query API query
     * @return number of record in a response
     */
    @Override
    protected int sizeInBackEnd(Query<JSONObject, T> query) {
        if (mapboxData == null) {
            return 0;
        } else {
            try {
                // Empty query is send to get a number of records
                sendQuery(query);
                // Return a number of record in a response
                // Mapbox geocoding API always returns only 1 record (1 place)
                return 1;
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Send and API query
     *
     * @param query API query
     * @return API query response
     */
    public synchronized Stream<JSONObject> sendQuery(Query<JSONObject, T> query) {
        try {

            // When no query parameters are selected, return empty data set
            if (mapboxData == null) {
                JSONObject jsonObject = new JSONObject("{ \"mockData\":[]}");
                JSONArray mockArray = jsonObject.getJSONArray("mockData");
                return jsonArrayStream(mockArray);
            } else {
                // URL address definition
                String url = "https://api.mapbox.com/geocoding/v5/mapbox.places/";
                // Check for spaces in search text
                String comboText = mapboxData.getSearchText();
                String seachText = "";
                if (comboText.contains(" ")) {
                    seachText = comboText.replace(" ", "%20");
                } else {
                    seachText = comboText;
                }
                GetMethod getMethod = new GetMethod(url + seachText + ".json");
                // Mandatory parameters
                getMethod.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
                        new DefaultHttpMethodRetryHandler(3, false));
                getMethod.getParams().setContentCharset(StandardCharsets.UTF_8.toString());
                getMethod.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
                getMethod.setRequestHeader("Accept", "application/json");
                // Set optional parameters
                getMethod.setQueryString(new NameValuePair[]{
                        new NameValuePair("access_token", "pk.eyJ1Ijoic2hhZG93MTUiLCJhIjoiY2pzYTk4YnVuMTd6djQ0b2tldWxjNWY4eSJ9.ggoq5tLa1gx-U14rndkDFA"),
                });
                // Send query
                int apiResponse = client.executeMethod(getMethod);
                log.info("Sending a query " + apiType + " to API. Query parameter: search_text: " + mapboxData.getSearchText());
                byte[] primaryResponseBody = getMethod.getResponseBody();
                requestResult = new JSONObject(new String(primaryResponseBody, "UTF-8"));
                log.info("Getting a response from API. Response code: " + apiResponse);
            }
            // In case of Athens, there's Athens, GA in the first place.
            // In order to show the city of Athens in Greece, second element of the result array needs to be called
            if (mapboxData.getSearchText().equals("Athens")) {
                requestData = requestResult.getJSONArray("features").getJSONObject(1);
                return jsonObjectStream(requestData);
            } else {
                requestData = requestResult.getJSONArray("features").getJSONObject(0);
                return jsonObjectStream(requestData);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a JSONArray stream
     *
     * @param array JSON array to create a stream from
     * @return stream of JSON values
     */
    private Stream<JSONObject> jsonArrayStream(JSONArray array) {
        assert array != null;
        return new AbstractList<JSONObject>() {
            @Override
            public JSONObject get(int index) {
                try {
                    return (JSONObject) array.get(index);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public int size() {
                return array.length();
            }
        }.stream();
    }

    /**
     * Creates a JSONObject stream
     *
     * @param jObject JSON object to create a stream from
     * @return stream of JSON values
     */
    private Stream<JSONObject> jsonObjectStream(JSONObject jObject) {
        assert jObject != null;
        return new AbstractList<JSONObject>() {
            @Override
            public JSONObject get(int index) {
                try {
                    return jObject;
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public int size() {
                return jObject.length();
            }
        }.stream();
    }

    public JSONObject getResultData() {
        return requestData;
    }

    /**
     * Sets a filter for data provider
     *
     * @param filter MapboxData
     */
    @Override
    public void setFilter(T filter) {
        this.mapboxData = (MapboxView.MapboxData) filter;
    }
}

