package com.isw2data.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Rappresenta un ticket di bug con informazioni su versioni e commit associati.
 */
public class TicketBug {

    // Chiave identificativa del ticket
    private String key;
    // Versione in cui è stato introdotto il bug
    private Release injectedVersion;
    // Versione in cui è stato aperto il ticket
    private Release openingVersion;
    // Versione in cui il bug è stato risolto
    private Release fixVersion;
    // Data di creazione del ticket (utilizzata per determinare l'apertura del ticket)
    private LocalDateTime creationDate;
    // Data di risoluzione del ticket (utilizzata per determinare la versione di fix)
    private LocalDateTime resolutionDate;
    // Liste delle versioni affette dal bug
    private List<Release> affectedVersions;
    // Lista dei commit che risolvono il bug
    private List<RevCommit> fixCommits = new ArrayList<>();

    /**
     * Costruttore per creare un TicketBug con versioni specifiche e senza date.
     *
     * @param key            Chiave del ticket.
     * @param openingVersion Versione in cui è stato aperto il ticket.
     * @param fixVersion     Versione in cui il bug è stato risolto.
     * @param affectedVersions Liste delle versioni affette.
     */
    public TicketBug(String key, Release openingVersion, Release fixVersion, List<Release> affectedVersions) {
        this.key = key;
        this.injectedVersion = null;
        this.openingVersion = openingVersion;
        this.fixVersion = fixVersion;
        this.affectedVersions = affectedVersions;
    }

    /**
     * Costruttore per creare un TicketBug con versioni specifiche e senza date e injectedVersion.
     *
     * @param key            Chiave del ticket.
     * @param openingVersion Versione in cui è stato aperto il ticket.
     * @param fixVersion     Versione in cui il bug è stato risolto.
     */
    public TicketBug(String key, Release openingVersion, Release fixVersion) {
        this.key = key;
        this.injectedVersion = null;
        this.openingVersion = openingVersion;
        this.fixVersion = fixVersion;
        this.affectedVersions = new ArrayList<>();
    }

    /**
     * Costruttore completo per creare un TicketBug con tutte le informazioni.
     *
     * @param key            Chiave del ticket.
     * @param creationDate   Data di creazione del ticket.
     * @param resolutionDate Data di risoluzione del ticket.
     * @param openingVersion Versione in cui è stato aperto il ticket.
     * @param fixVersion     Versione in cui il bug è stato risolto.
     * @param affectedVersions Liste delle versioni affette.
     * @param injectedVersion Versione in cui è stato introdotto il bug.
     */
    public TicketBug(String key, LocalDateTime creationDate, LocalDateTime resolutionDate, Release openingVersion, Release fixVersion, List<Release> affectedVersions, Release injectedVersion) {
        this.key = key;
        this.creationDate = creationDate;
        this.resolutionDate = resolutionDate;
        this.openingVersion = openingVersion;
        this.fixVersion = fixVersion;
        this.affectedVersions = affectedVersions;
        this.injectedVersion = injectedVersion;
    }

    // Restituisce la chiave del ticket
    public String getKey() {
        return key;
    }

    // Imposta la chiave del ticket
    public void setKey(String key) {
        this.key = key;
    }

    // Restituisce la versione in cui è stato introdotto il bug
    public Release getInjectedVersion() {
        return injectedVersion;
    }

    // Imposta la versione in cui è stato introdotto il bug
    public void setInjectedVersion(Release injectedVersion) {
        this.injectedVersion = injectedVersion;
    }

    // Restituisce la versione in cui è stato aperto il ticket
    public Release getOpeningVersion() {
        return openingVersion;
    }

    // Imposta la versione in cui è stato aperto il ticket
    public void setOpeningVersion(Release openingVersion) {
        this.openingVersion = openingVersion;
    }

    // Restituisce la versione in cui il bug è stato risolto
    public Release getFixVersion() {
        return fixVersion;
    }

    // Imposta la versione in cui il bug è stato risolto
    public void setFixVersion(Release fixVersion) {
        this.fixVersion = fixVersion;
    }

    // Restituisce la lista delle versioni affette
    public List<Release> getAffectedVersions() {
        return affectedVersions;
    }

    // Imposta la lista delle versioni affette
    public void setAffectedVersions(List<Release> affectedVersions) {
        this.affectedVersions = affectedVersions;
    }

    // Restituisce la data di creazione del ticket
    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    // Imposta la data di creazione del ticket
    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    // Restituisce la data di risoluzione del ticket
    public LocalDateTime getResolutionDate() {
        return resolutionDate;
    }

    // Imposta la data di risoluzione del ticket
    public void setResolutionDate(LocalDateTime resolutionDate) {
        this.resolutionDate = resolutionDate;
    }

    // Restituisce la lista dei commit che risolvono il bug
    public List<RevCommit> getCommits() {
        return fixCommits;
    }

    /**
     * Aggiunge un commit alla lista dei commit che risolvono il bug.
     *
     * @param fixCommit Il commit da aggiungere.
     */
    public void addFixCommit(RevCommit fixCommit) {
        fixCommits.add(fixCommit);
    }

    // Imposta la lista dei commit che risolvono il bug
    public void setCommits(List<RevCommit> commits) {
        this.fixCommits = commits;
    }
}