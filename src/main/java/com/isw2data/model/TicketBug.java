package com.isw2data.model;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import org.eclipse.jgit.revwalk.RevCommit;


public class TicketBug {

    private String key;
    private Release injectedVersion;
    private Release openingVersion;
    private Release fixVersion;
    private LocalDateTime creationDate;      //utilizzata per determinare l'opening version
    private LocalDateTime resolutionDate;    //utilizzata per determinare la fix version
    private List<Release> affectedVersions;
    private List<RevCommit> fixCommits = new ArrayList<>();

    public TicketBug(String key, Release openingVersion, Release fixVersion, List<Release> affectedVersions) {
        this.key = key;
        this.injectedVersion = null;
        this.openingVersion = openingVersion;
        this.fixVersion = fixVersion;
        this.affectedVersions = affectedVersions;
    }

    public TicketBug(String key, Release openingVersion, Release fixVersion) {
        this.key = key;
        this.injectedVersion = null;
        this.openingVersion = openingVersion;
        this.fixVersion = fixVersion;
        this.affectedVersions = new ArrayList<>();
    }

    public TicketBug(String key, LocalDateTime creationDate, LocalDateTime resolutionDate, Release openingVersion, Release fixVersion, List<Release> affectedVersions, Release injectedVersion) {
        this.key = key;
        this.creationDate = creationDate;
        this.resolutionDate = resolutionDate;
        this.openingVersion = openingVersion;
        this.fixVersion = fixVersion;
        this.affectedVersions = affectedVersions;
        this.injectedVersion = injectedVersion;
    }


    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Release getInjectedVersion() {
        return injectedVersion;
    }

    public void setInjectedVersion(Release injectedVersion) {
        this.injectedVersion = injectedVersion;
    }

    public Release getOpeningVersion() {
        return openingVersion;
    }

    public void setOpeningVersion(Release openingVersion) {
        this.openingVersion = openingVersion;
    }

    public Release getFixVersion() {
        return fixVersion;
    }

    public void setFixVersion(Release fixVersion) {
        this.fixVersion = fixVersion;
    }

    public List<Release> getAffectedVersions() {
        return affectedVersions;
    }

    public void setAffectedVersions(List<Release> affectedVersions) {
        this.affectedVersions = affectedVersions;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public LocalDateTime getResolutionDate() {
        return resolutionDate;
    }

    public void setResolutionDate(LocalDateTime resolutionDate) {
        this.resolutionDate = resolutionDate;
    }

    public List<RevCommit> getCommits() {
        return fixCommits;
    }

    public void add_fix_commit(RevCommit fix_commit) { fixCommits.add(fix_commit); }

    public void setCommits(List<RevCommit> commits) {
        this.fixCommits = commits;
    }
}