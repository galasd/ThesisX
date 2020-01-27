package cz.xgald01.dp.service;

import com.vaadin.data.provider.AbstractBackEndDataProvider;
import com.vaadin.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.data.provider.Query;
import cz.xgald01.dp.view.NasaView;
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
 * Data provider for NASA API
 */
public class NasaDataProvider<T> extends AbstractBackEndDataProvider<JSONObject, T> implements ConfigurableFilterDataProvider<JSONObject, T, T> {

    private static final Logger log = LoggerFactory.getLogger(NasaDataProvider.class);
    private String url = "https://api.nasa.gov/neo/rest/v1/feed";
    private GetMethod getMethod = new GetMethod(url);
    private HttpClient client = new HttpClient();
    public String apiType = "Nasa";
    public JSONObject requestResult = null;
    public JSONArray requestData = null;
    NasaView.NasaData nasaData;

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
        if (nasaData == null) {
            return 0;
        } else {
            try {
                // Empty query is send to get a number of records
                sendQuery(query);
                // Return a number of record in a response
                return requestResult.getInt("element_count");
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
            if (nasaData == null) {
                JSONObject jsonObject = new JSONObject("{ \"mockData\":[]}");
                JSONArray mockArray = jsonObject.getJSONArray("mockData");
                return jsonArrayStream(mockArray);
            } else {
                // Mandatory parameters
                getMethod.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
                        new DefaultHttpMethodRetryHandler(3, false));
                getMethod.getParams().setContentCharset(StandardCharsets.UTF_8.toString());
                getMethod.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
                getMethod.setRequestHeader("Accept", "application/json");
                // Set optional parameters
                getMethod.setQueryString(new NameValuePair[]{
                        new NameValuePair("api_key", "G5RtgrVQak8TBnysXstyRbd1MmJiOal23aUurQh9"),
                        new NameValuePair("start_date", String.valueOf(nasaData.getDateFrom())),
                        new NameValuePair("end_date", String.valueOf(nasaData.getDateTo()))
                });
                log.info("Sending a query " + apiType + " to API. Query parameters: start_date: " +
                        String.valueOf(nasaData.getDateFrom()) + ", end_date: " + String.valueOf(nasaData.getDateTo()));
                // Send query
                int apiResponse = client.executeMethod(getMethod);
                byte[] primaryResponseBody = getMethod.getResponseBody();
                requestResult = new JSONObject(new String(primaryResponseBody, "UTF-8"));
                log.info("Getting a response from API. Response code: " + apiResponse);
                requestData = requestResult.getJSONObject("near_earth_objects").
                        getJSONArray(String.valueOf(nasaData.getDateFrom()));
            }
            return jsonArrayStream(requestData);

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
     * Sets a filter for data provider
     *
     * @param filter MapboxData
     */
    @Override
    public void setFilter(T filter) {

        this.nasaData = (NasaView.NasaData) filter;
    }

    public JSONArray getResultData() {
        return requestData;
    }
}

