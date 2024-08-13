package com.isw2data.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Classe che rappresenta una singola classe di codice sorgente all'interno di una release.
 */
public class Class {

    // Percorso del file della classe
    private String path;
    // Contenuto del file della classe
    private String content;
    // Release alla quale appartiene questa classe
    private Release release;

    // Linee di codice
    private int loc;
    // Linee di codice toccate (modificate) in questa release
    private int locTouched;
    // Numero di revisioni della classe
    private short nr;
    // Numero di correzioni (fix) applicate alla classe
    private short nFix;
    // Numero di autori che hanno contribuito alla classe
    private int nAuth;
    // Linee di codice aggiunte in questa release
    private int locAdded;
    // Massimo numero di linee di codice aggiunte in una release
    private int maxLocAdded;
    // Numero totale di linee di codice cambiate (churn)
    private int churn;
    // Massimo churn registrato per questa classe
    private int maxChurn;
    // Churn medio per questa classe
    private float averageChurn;
    // Media dei giorni di ferie degli autori
    private float averageHolydays;
    // Indica se la classe è buggy (presenta difetti)
    private boolean isBuggy = false;

    // Array delle modifiche di churn nel tempo
    private List<Integer> churnArray = new ArrayList<>();
    // Lista degli autori che hanno modificato questa classe
    private List<String> authors = new ArrayList<>();

    /**
     * Costruttore della classe.
     *
     * @param path    Percorso del file della classe.
     * @param content Contenuto del file della classe.
     * @param release Release alla quale appartiene questa classe.
     */
    public Class(String path, String content, Release release) {
        this.path = path;
        this.content = content;
        this.release = release;
    }

    // Imposta il valore della media dei giorni di ferie
    public void setHoliday(float holiday) {
        this.averageHolydays = holiday;
    }

    // Restituisce la media dei giorni di ferie
    public double getAverageHolydays() {
        return averageHolydays;
    }

    // Restituisce il percorso del file della classe
    public String getPath() {
        return path;
    }

    // Restituisce la versione della release della classe
    public Integer getVersion() {
        return release.getIndex();
    }

    // Restituisce il contenuto del file della classe
    public String getContent() {
        return content;
    }

    // Restituisce il numero di linee di codice
    public int getLoc() {
        return loc;
    }

    // Imposta il numero di linee di codice
    public void setLoc(int size) {
        this.loc = size;
    }

    // Restituisce il numero di linee di codice toccate
    public int getLocTouched() {
        return locTouched;
    }

    // Restituisce il numero di revisioni
    public short getNr() {
        return nr;
    }

    // Imposta il numero di revisioni
    public void setNr(short nr) {
        this.nr = nr;
    }

    // Restituisce il numero di correzioni (fix) applicate
    public short getNumFix() {
        return nFix;
    }

    // Incrementa il numero di correzioni (fix)
    public void addFix() {
        this.nFix++;
    }

    // Restituisce il numero di autori
    public int getNumAuth() {
        return nAuth;
    }

    // Imposta il numero di autori
    public void setNumAuth(int nAuth) {
        this.nAuth = nAuth;
    }

    // Restituisce il numero di linee di codice aggiunte
    public int getLocAdded() {
        return locAdded;
    }

    // Restituisce il massimo numero di linee di codice aggiunte
    public int getMaxLocAdded() {
        return maxLocAdded;
    }

    // Imposta il massimo numero di linee di codice aggiunte
    public void setMaxLocAdded(int maxLocAdded) {
        this.maxLocAdded = maxLocAdded;
    }

    // Restituisce il churn totale
    public int getChurn() {
        return churn;
    }

    // Imposta il churn totale
    public void setChurn(int churn) {
        this.churn = churn;
    }

    // Restituisce il massimo churn registrato
    public int getMaxChurn() {
        return maxChurn;
    }

    // Imposta il massimo churn registrato
    public void setMaxChurn(int maxChurn) {
        this.maxChurn = maxChurn;
    }

    // Restituisce il churn medio
    public float getAverageChurn() {
        return averageChurn;
    }

    // Imposta il churn medio
    public void setAverageChurn(float averageChurn) {
        this.averageChurn = averageChurn;
    }

    // Incrementa il numero di linee di codice toccate
    public void incrementLocTouched(int lines) {
        locTouched += lines;
    }

    // Incrementa il numero di linee di codice aggiunte
    public void incrementLocAdded(int lines) {
        locAdded += lines;
    }

    // Incrementa il churn
    public void incrementLocChurn(int lines) {
        churn += lines;
    }

    // Restituisce l'array delle modifiche di churn
    public List<Integer> getChurnArray() {
        return churnArray;
    }

    // Aggiunge un valore di churn all'array
    public void addChurnArray(Integer churn) {
        churnArray.add(churn);
    }

    // Incrementa il numero di revisioni
    public void incrementNumRevisions() {
        nr++;
    }

    // Restituisce la lista degli autori
    public List<String> getAuthors() {
        return authors;
    }

    // Aggiunge un autore alla lista
    public void addAuthors(String auth) {
        authors.add(auth);
    }

    // Verifica se la classe è considerata buggy
    public boolean isBuggy() {
        return isBuggy;
    }

    // Imposta se la classe è considerata buggy
    public void setBuggy(boolean buggy) {
        isBuggy = buggy;
    }
}