package com.isw2data.utils;

import com.google.gson.Gson;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility per gestire le festività pubbliche.
 */
public class HolidayUtils {
    private static List<LocalDate> holidays = new ArrayList<>();

    // Costruttore privato per impedire l'instanziazione della classe utility
    private HolidayUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Recupera le festività per un intervallo di anni fino all'anno corrente.
     *
     * @param startYear Anno di inizio dell'intervallo.
     * @param countryCode Codice del paese per cui recuperare le festività.
     */
    public static void initializeHolidays(int startYear, String countryCode) {
        int currentYear = Year.now().getValue(); // Anno corrente

        for (int year = startYear; year <= currentYear; year++) {
            String url = String.format("https://date.nager.at/api/v3/publicholidays/%d/%s", year, countryCode);
            String json;

            try {
                // Effettua la richiesta HTTP per ottenere le festività
                json = new RestTemplate().getForObject(url, String.class);
            } catch (RestClientException e) {
                throw new RestClientException("Errore durante il recupero delle festività: " + e.toString());
            }

            // Parse del JSON per ottenere le festività
            Gson gson = new Gson();
            PublicHoliday[] holidaysArray = gson.fromJson(json, PublicHoliday[].class);

            // Aggiunge le festività alla lista
            for (PublicHoliday holiday : holidaysArray) {
                holidays.add(LocalDate.parse(holiday.date));
            }
        }
    }

    /**
     * Conta il numero di festività comprese tra due date.
     *
     * @param startDateTime Data e ora di inizio dell'intervallo.
     * @param endDateTime Data e ora di fine dell'intervallo.
     * @param startYear Anno di inizio dell'intervallo per il recupero delle festività.
     * @param countryCode Codice del paese per cui recuperare le festività.
     * @return Numero di festività nel range specificato.
     */
    public static long countHolidaysBetween(LocalDateTime startDateTime, LocalDateTime endDateTime, int startYear, String countryCode) {
        // Inizializza la lista delle festività se è vuota
        if (holidays.isEmpty()) {
            initializeHolidays(startYear, countryCode);
        }

        LocalDate startDate = startDateTime.toLocalDate();
        LocalDate endDate = endDateTime.toLocalDate();

        // Conta le festività comprese tra le due date
        return holidays.stream()
                .filter(date -> !date.isBefore(startDate) && !date.isAfter(endDate))
                .count();
    }

    /**
     * Classe interna per rappresentare una festività pubblica.
     */
    static class PublicHoliday {
        private String date; // Data della festività in formato ISO 8601
    }
}