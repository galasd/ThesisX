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
 * Data provider pro NASA API
 */
public class NasaDataProvider<T> extends AbstractBackEndDataProvider<JSONObject, T> implements ConfigurableFilterDataProvider<JSONObject, T, T> {

    private static final Logger log = LoggerFactory.getLogger(NasaDataProvider.class);
    private String url = "https://api.nasa.gov/neo/rest/v1/feed";
    private GetMethod getMethod = new GetMethod(url);
    private HttpClient client = new HttpClient();
    public String apiType = "Nasa";
    public JSONObject requestResult = null;
    NasaView.NasaData nasaData;

    /**
     * Pozadavek na data od gridu
     *
     * @param query dotaz na API
     * @return stream JSONObjectu odpovidajicich query
     */
    @Override
    protected Stream<JSONObject> fetchFromBackEnd(Query<JSONObject, T> query) {

        return sendQuery(query);
    }

    /**
     * Vratit pocet zaznamu od API
     *
     * @param query dotaz na API
     * @return pocet zaznamu v odpovedi
     */
    @Override
    protected int sizeInBackEnd(Query<JSONObject, T> query) {
        if (nasaData == null) {
            return 0;
        } else {
            try {
                // V tomto pripade prijde prazdny query, jen pocet vysledku
                sendQuery(query);
                // Vratit pocet vysledku z query
                return requestResult.getInt("element_count");
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Odeslani dotazu na API
     *
     * @param query dotaz na API
     * @return odpoved od API
     */
    private synchronized Stream<JSONObject> sendQuery(Query<JSONObject, T> query) {
        try {
            // Pokud nejsou vybrany parametry, vratit prazdny vysledek
            if (nasaData == null) {
                JSONObject jsonObject = new JSONObject("{ \"mockData\":[]}");
                JSONArray mockArray = jsonObject.getJSONArray("mockData");
                return jsonArrayStream(mockArray);
            } else {
                // Definice povinnych parametru
                getMethod.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
                        new DefaultHttpMethodRetryHandler(3, false));
                getMethod.getParams().setContentCharset(StandardCharsets.UTF_8.toString());
                getMethod.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
                getMethod.setRequestHeader("Accept", "application/json");
                // Nastavit volitelne parametry dotazu
                getMethod.setQueryString(new NameValuePair[]{
                        new NameValuePair("api_key", "G5RtgrVQak8TBnysXstyRbd1MmJiOal23aUurQh9"),
                        new NameValuePair("start_date", String.valueOf(nasaData.getDateFrom())),
                        new NameValuePair("end_date", String.valueOf(nasaData.getDateTo()))
                });
                log.info("Odeslani dotazu na " + apiType + " API. Parametry dotazu: start_date: " +
                        String.valueOf(nasaData.getDateFrom()) + ", end_date: " + String.valueOf(nasaData.getDateTo()));
                // Odeslat dotaz
                int apiResponse = client.executeMethod(getMethod);
                byte[] primaryResponseBody = getMethod.getResponseBody();
                requestResult = new JSONObject(new String(primaryResponseBody, "UTF-8"));
                log.info("Ziskani odpovedi od API. Kod odpovedi: " + apiResponse);
            }
            return jsonArrayStream(requestResult.getJSONObject("near_earth_objects").getJSONArray(String.valueOf(nasaData.getDateFrom())));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Vytvorit stream z JSON array
     *
     * @param array JSON array ze ktereho je vytvoren stream
     * @return stream JSON values
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
     * Nastavi filter pro data provider
     *
     * @param filter NasaData
     */
    @Override
    public void setFilter(T filter) {

        this.nasaData = (NasaView.NasaData) filter;
    }
}

