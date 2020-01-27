package cz.xgald01.dp.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Parent bean for Nasa and Mapbox data
 */
public class ApiData {

    // Parametry pro predavani
    public LocalDate nasaDateFrom;
    public List<String> jsonKeys = new ArrayList<>();
}


