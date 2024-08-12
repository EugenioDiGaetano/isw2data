package com.isw2data.model;

import org.eclipse.jgit.revwalk.RevCommit;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class Release {

    private int index; //used in the dataset for prediction
    private long id;   //id used in jira to identify this version
    private String name;
    private LocalDateTime releaseDate;
    private Map<String, Class> allClasses; //tiene traccia di tutte le classi presenti alla releaseDate di quella versione
    private List<RevCommit> allCommits; //tiene traccia di tutti i commit afferenti alla versione (effettuati entro la releaseDate della versione)
    private RevCommit lastCommit;
    private List<TicketBug> fixTickets = new ArrayList<>();    //ticket con fixVersion uguale a versione
    private double proportion;

    public Release(int index, long id, String name, LocalDateTime releaseDate) {
        this.index = index;
        this.id = id;
        this.name = name;
        this.releaseDate = releaseDate;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(LocalDateTime releaseDate) {
        this.releaseDate = releaseDate;
    }

    public Map<String, Class> getAllClasses() {
        return allClasses;
    }

    public void setAllClasses(Map<String, Class> allClasses) {
        this.allClasses = allClasses;
    }

    public List<RevCommit> getAllCommits() {
        return allCommits;
    }

    public void setAllCommits(List<RevCommit> allCommits) {
        this.allCommits = allCommits;
    }

    public RevCommit getLastCommit() {
        return lastCommit;
    }

    public void setLastCommit(RevCommit lastCommit) {
        this.lastCommit = lastCommit;
    }

    public void addCommit(RevCommit commit) {
        if (allCommits == null) {
            allCommits = new ArrayList<>();
        }
        allCommits.add(commit);
    }

    public List<TicketBug> getFixTickets() {
        return fixTickets;
    }

    public void setFixTickets(List<TicketBug> fixTickets) {
        this.fixTickets = fixTickets;
    }

    public double getProportion() {
        return proportion;
    }

    public void setProportion(double proportion) {
        this.proportion = proportion;
    }
}