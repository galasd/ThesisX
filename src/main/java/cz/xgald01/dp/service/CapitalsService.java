package cz.xgald01.dp.service;

import java.util.*;

/**
 * Capital cities provider, Europe only
 */
public class CapitalsService {

    private List<String> capitals = new ArrayList<>();

    public CapitalsService() {
        capitals.add("Amsterdam");
        capitals.add("Andorra la Vella");
        capitals.add("Athens");
        capitals.add("Berlin");
        capitals.add("Bern");
        capitals.add("Belgrade");
        capitals.add("Bratislava");
        capitals.add("Bucharest");
        capitals.add("Budapest");
        capitals.add("Dublin");
        capitals.add("Helsinki");
        capitals.add("Chisinau");
        capitals.add("Copenhagen");
        capitals.add( "Kiev");
        capitals.add("Lisbon");
        capitals.add("London");
        capitals.add("Ljubljana");
        capitals.add("Luxembourg");
        capitals.add("Madrid");
        capitals.add("Minsk");
        capitals.add("Monaco Ville");
        capitals.add("Moscow");
        capitals.add("Nicosia");
        capitals.add("Nuuk");
        capitals.add("Oslo");
        capitals.add("Paris");
        capitals.add("Podgorica");
        capitals.add("Pristina");
        capitals.add("Reykjavik");
        capitals.add("Riga");
        capitals.add("Rome");
        capitals.add("San Marino");
        capitals.add("Sarajevo");
        capitals.add("Skopje");
        capitals.add("Sofia");
        capitals.add("Stockholm");
        capitals.add("Tallinn");
        capitals.add("Vaduz");
        capitals.add("Valletta");
        capitals.add("Vatican City");
        capitals.add("Vienna");
        capitals.add("Vilnius");
        capitals.add("Warsaw");
        capitals.add("Zagreb");
    }

    public Collection<String> getCities() {
        return capitals;
    }
}
