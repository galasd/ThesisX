package cz.xgald01.dp.service;

import java.util.*;

/**
 * Poskytovatel hlavnich mest statu Evropy
 */
public class CapitalsService {

    private Map<String, String> capitals = new TreeMap<String, String>();

    public CapitalsService() {
        // Pridat jednotliva hlavni mesta a jejich anglicke ekvivalenty
        capitals.put("Amsterdam", "Amsterdam");
        capitals.put("Andorra la Vella", "Andorra%20la%20Vella");
        capitals.put("Athény", "Athens");
        capitals.put("Berlín", "Berlin");
        capitals.put("Bern", "Bern");
        capitals.put("Bělehrad", "Belgrade");
        capitals.put("Bratislava", "Bratislava");
        capitals.put("Bukurešť", "Bucharest");
        capitals.put("Budapešť", "Budapest");
        capitals.put("Dublin", "Dublin");
        capitals.put("Helsinky", "Helsinki");
        capitals.put("Kišiněv", "Chisinau");
        capitals.put("Kodaň", "Copenhagen");
        capitals.put("Kyjev", "Kiev");
        capitals.put("Lisabon", "Lisbon");
        capitals.put("Londýn", "London");
        capitals.put("Lublaň", "Ljubljana");
        capitals.put("Lucemburk", "Luxembourg");
        capitals.put("Madrid", "Madrid");
        capitals.put("Minsk", "Minsk");
        capitals.put("Monako", "Monaco%20Ville");
        capitals.put("Moskva", "Moscow");
        capitals.put("Nikósie", "Nicosia");
        capitals.put("Nuuk", "Nuuk");
        capitals.put("Oslo", "Oslo");
        capitals.put("Paříž", "Paris");
        capitals.put("Podgorica", "Podgorica");
        capitals.put("Priština", "Pristina");
        capitals.put("Reykjavík", "Reykjavik");
        capitals.put("Riga", "Riga");
        capitals.put("Řím", "Rome");
        capitals.put("San Marino", "San%20Marino");
        capitals.put("Sarajevo", "Sarajevo");
        capitals.put("Skopje", "Skopje");
        capitals.put("Sofie", "Sofia");
        capitals.put("Stockholm", "Stockholm");
        capitals.put("Tallinn", "Tallinn");
        capitals.put("Vaduz", "Vaduz");
        capitals.put("Valletta", "Valletta");
        capitals.put("Vatikán", "Vatican%20City");
        capitals.put("Vídeň", "Vienna");
        capitals.put("Vilnius", "Vilnius");
        capitals.put("Varšava", "Warsaw");
        capitals.put("Záhřeb", "Zagreb");
    }

    // Prelozit vybrany nazev mesta do anglictiny
    public String translate(String city) {
        String capitalEN = "";
        for (Map.Entry<String, String> entry : capitals.entrySet()) {
            if (Objects.equals(city, entry.getKey())) {
                capitalEN = entry.getValue();
            }
        }
        return capitalEN;
    }

    // Vratit kolekci ceskych nazvu hlavnich mest
    public Collection<String> getCities() {

        Collection<String> citiesCZ = new ArrayList<>();
        for (Map.Entry<String, String> entry : capitals.entrySet()) {
            citiesCZ.add(entry.getKey());
        }
        return citiesCZ;
    }
}
