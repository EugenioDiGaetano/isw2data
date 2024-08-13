package com.isw2data.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;

/**
 * Utility per la gestione di operazioni JSON.
 */
public class JsonUtils {

    // Costruttore privato per impedire l'instanziazione della classe utility
    private JsonUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Legge un array JSON da un URL.
     *
     * @param url L'URL da cui leggere l'array JSON.
     * @return Un array JSON letto dall'URL.
     * @throws IOException Se si verifica un errore durante la lettura dei dati.
     * @throws JSONException Se i dati letti non sono un array JSON valido.
     */
    public static JSONArray readJsonArrayFromUrl(String url) throws IOException, JSONException {
        try (InputStream is = new URL(url).openStream();
             BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")))) {
            String jsonText = readAll(rd);
            return new JSONArray(jsonText);
        }
    }

    /**
     * Legge un oggetto JSON da un URL.
     *
     * @param url L'URL da cui leggere l'oggetto JSON.
     * @return Un oggetto JSON letto dall'URL.
     * @throws IOException Se si verifica un errore durante la lettura dei dati.
     * @throws JSONException Se i dati letti non sono un oggetto JSON valido.
     */
    public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        try (InputStream is = new URL(url).openStream();
             BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")))) {
            String jsonText = readAll(rd);
            return new JSONObject(jsonText);
        }
    }

    /**
     * Legge tutto il contenuto di un Reader in una stringa.
     *
     * @param rd Il Reader da cui leggere i dati.
     * @return Il contenuto del Reader come stringa.
     * @throws IOException Se si verifica un errore durante la lettura dei dati.
     */
    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }
}