package com.isw2data.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Utility per operazioni generali.
 */
public class Utils {

    // Costruttore privato per impedire l'instanziazione della classe utility
    private Utils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Converte un timestamp Unix (in secondi) in un oggetto {@link LocalDateTime}.
     *
     * @param unixSeconds Il timestamp Unix da convertire, espresso in secondi.
     * @return Un oggetto {@link LocalDateTime} rappresentante il timestamp Unix.
     */
    public static LocalDateTime convertTime(long unixSeconds) {
        return LocalDateTime.ofInstant(
                Instant.ofEpochSecond(unixSeconds),
                ZoneId.systemDefault() // Utilizza il fuso orario di sistema
        );
    }
}