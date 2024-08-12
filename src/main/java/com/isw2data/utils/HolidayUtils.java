package com.isw2data.utils;

import com.google.gson.Gson;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

public class HolidayUtils {
    private static List<LocalDate> holidays = new ArrayList<>();

    // Recupera le festività per un intervallo di anni fino all'anno corrente
    public static void initializeHolidays(int startYear, String countryCode) {
        int currentYear = Year.now().getValue();
        //System.out.println(holidays.size());
        //System.out.println("Fetching holidays from " + startYear + " to " + currentYear + " in " + countryCode);

        for (int year = startYear; year <= currentYear; year++) {
            String url = String.format("https://date.nager.at/api/v3/publicholidays/%d/%s", year, countryCode);
            String json;
            try {
                json = new RestTemplate().getForObject(url, String.class);
            } catch (RestClientException e) {
                throw new RuntimeException("Error fetching holidays: " + e.toString());
            }

            Gson gson = new Gson();
            PublicHoliday[] holidaysArray = gson.fromJson(json, PublicHoliday[].class);

            for (PublicHoliday holiday : holidaysArray) {
                holidays.add(LocalDate.parse(holiday.date));
            }
        }
    }

    public static long countHolidaysBetween(LocalDateTime startDateTime, LocalDateTime endDateTime, int startYear, String countryCode) {
        if (holidays.size()==0) {
            initializeHolidays(startYear, countryCode);
        }

        LocalDate startDate = startDateTime.toLocalDate();
        LocalDate endDate = endDateTime.toLocalDate();

        return holidays.stream()
                .filter(date -> !date.isBefore(startDate) && !date.isAfter(endDate))
                .count();
    }

    static class PublicHoliday {
        public String date;
        public String localName;
        public String name;
        public String countryCode;
        public Boolean fixed;
        public Boolean global;
        public String[] counties;
        public String[] types;
    }
}