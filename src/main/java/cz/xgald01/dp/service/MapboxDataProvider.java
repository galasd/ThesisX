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
 * Data provider pro Mapbox API
 */
public class MapboxDataProvider<T> extends AbstractBackEndDataProvider<JSONObject, T> implements ConfigurableFilterDataProvider<JSONObject, T, T> {

    private static final Logger log = LoggerFactory.getLogger(MapboxDataProvider.class);
    private HttpClient client = new HttpClient();
    public JSONObject requestResult = null;
    public String apiType = "Mapbox";
    MapboxView.MapboxData mapboxData;

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
        if (mapboxData == null) {
            return 0;
        } else {
            try {
                // V tomto pripade prijde prazdny query, jen pocet vysledku
                sendQuery(query);
                // Vratit pocet vysledku z query.
                // Mapbox geocoding API vraci vzdy jen jeden vysledek pro jedno mesto.
                return 1;
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Odeslani dotazu na api
     *
     * @param query dotaz na api
     * @return odpoved od api
     */
    private synchronized Stream<JSONObject> sendQuery(Query<JSONObject, T> query) {
        try {

            // Pokud nejsou vybrany parametry, vratit prazdny vysledek
            if (mapboxData == null) {
                JSONObject jsonObject = new JSONObject("{ \"mockData\":[]}");
                JSONArray mockArray = jsonObject.getJSONArray("mockData");
                return jsonArrayStream(mockArray);
            } else {
                // Defince url adresy k API
                String url = "https://api.mapbox.com/geocoding/v5/mapbox.places/";
                GetMethod getMethod = new GetMethod(url + mapboxData.getSearchText() + ".json");
                // Definice povinnych parametru
                getMethod.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
                        new DefaultHttpMethodRetryHandler(3, false));
                getMethod.getParams().setContentCharset(StandardCharsets.UTF_8.toString());
                getMethod.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
                getMethod.setRequestHeader("Accept", "application/json");
                // Nastavit volitelne parametry dotazu
                getMethod.setQueryString(new NameValuePair[]{
                        new NameValuePair("access_token", "pk.eyJ1Ijoic2hhZG93MTUiLCJhIjoiY2pzYTk4YnVuMTd6djQ0b2tldWxjNWY4eSJ9.ggoq5tLa1gx-U14rndkDFA"),
                });
                // Odeslat dotaz
                int apiResponse = client.executeMethod(getMethod);
                log.info("Odeslani dotazu na " + apiType + " API. Parametr dotazu: search_text: " + mapboxData.getSearchText());
                byte[] primaryResponseBody = getMethod.getResponseBody();
                requestResult = new JSONObject(new String(primaryResponseBody, "UTF-8"));
                log.info("Ziskani odpovedi od API. Kod odpovedi: " + apiResponse);
            }
            // V pripade Athen je primarne zobrazovano mesto ve state Georgia v USA. Recke Atheny jsou umisteny
            // az na druhem miste ve vracenem json poli
            if(mapboxData.getSearchText().equals("Athens")){
                return jsonObjectStream(requestResult.getJSONArray("features").getJSONObject(1));
            }
            else {
                return jsonObjectStream(requestResult.getJSONArray("features").getJSONObject(0));
            }

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
     * Vytvorit stream z JSON objectu
     *
     * @param jObject JSON object ze ktereho je vytvoren stream
     * @return stream JSON values
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

    /**
     * Nastavi filter pro data provider
     *
     * @param filter MapboxData
     */
    @Override
    public void setFilter(T filter) {

        this.mapboxData = (MapboxView.MapboxData) filter;
    }
}

