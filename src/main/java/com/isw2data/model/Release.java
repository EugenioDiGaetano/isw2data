package com.isw2data.model;

import org.eclipse.jgit.revwalk.RevCommit;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Rappresenta una release di un progetto software.
 */
public class Release {

    // Indice della release, utilizzato nel dataset per la previsione
    private int index;
    // ID utilizzato in JIRA per identificare questa versione
    private long id;
    // Nome della release
    private String name;
    // Data di rilascio della versione
    private LocalDateTime releaseDate;
    // Mappa delle classi presenti alla data di rilascio di questa versione
    private Map<String, Class> allClasses;
    // Lista dei commit effettuati entro la data di rilascio della versione
    private List<RevCommit> allCommits = new ArrayList<>();
    // Ultimo commit effettuato
    private RevCommit lastCommit;
    // Lista dei ticket con fixVersion uguale a questa versione
    private List<TicketBug> fixTickets = new ArrayList<>();
    // Proporzione di fix tickets rispetto alla versione
    private double proportion;

    /**
     * Costruttore della classe Release.
     *
     * @param index        Indice della release.
     * @param id           ID della release in JIRA.
     * @param name         Nome della release.
     * @param releaseDate  Data di rilascio della versione.
     */
    public Release(int index, long id, String name, LocalDateTime releaseDate) {
        this.index = index;
        this.id = id;
        this.name = name;
        this.releaseDate = releaseDate;
    }

    // Restituisce l'indice della release
    public int getIndex() {
        return index;
    }

    // Imposta l'indice della release
    public void setIndex(int index) {
        this.index = index;
    }

    // Restituisce l'ID della release in JIRA
    public long getId() {
        return id;
    }

    // Imposta l'ID della release in JIRA
    public void setId(long id) {
        this.id = id;
    }

    // Restituisce il nome della release
    public String getName() {
        return name;
    }

    // Imposta il nome della release
    public void setName(String name) {
        this.name = name;
    }

    // Restituisce la data di rilascio della versione
    public LocalDateTime getReleaseDate() {
        return releaseDate;
    }

    // Imposta la data di rilascio della versione
    public void setReleaseDate(LocalDateTime releaseDate) {
        this.releaseDate = releaseDate;
    }

    // Restituisce la mappa delle classi presenti alla data di rilascio
    public Map<String, Class> getAllClasses() {
        return allClasses;
    }

    // Imposta la mappa delle classi presenti alla data di rilascio
    public void setAllClasses(Map<String, Class> allClasses) {
        this.allClasses = allClasses;
    }

    // Restituisce la lista dei commit effettuati entro la data di rilascio
    public List<RevCommit> getAllCommits() {
        return allCommits;
    }

    // Imposta la lista dei commit effettuati entro la data di rilascio
    public void setAllCommits(List<RevCommit> allCommits) {
        this.allCommits = allCommits;
    }

    // Restituisce l'ultimo commit effettuato
    public RevCommit getLastCommit() {
        return lastCommit;
    }

    // Imposta l'ultimo commit effettuato
    public void setLastCommit(RevCommit lastCommit) {
        this.lastCommit = lastCommit;
    }

    /**
     * Aggiunge un commit alla lista dei commit della release.
     *
     * @param commit Il commit da aggiungere.
     */
    public void addCommit(RevCommit commit) {
        if (allCommits == null) {
            allCommits = new ArrayList<>();
        }
        allCommits.add(commit);
    }

    // Restituisce la lista dei ticket con fixVersion uguale a questa versione
    public List<TicketBug> getFixTickets() {
        return fixTickets;
    }

    // Imposta la lista dei ticket con fixVersion uguale a questa versione
    public void setFixTickets(List<TicketBug> fixTickets) {
        this.fixTickets = fixTickets;
    }

    // Restituisce la proporzione di fix tickets rispetto alla versione
    public double getProportion() {
        return proportion;
    }

    // Imposta la proporzione di fix tickets rispetto alla versione
    public void setProportion(double proportion) {
        this.proportion = proportion;
    }
}