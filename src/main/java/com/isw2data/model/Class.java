package com.isw2data.model;

import java.util.ArrayList;
import java.util.List;

public class Class {

    private String path;
    private String content;
    private Release release;

    private int loc;
    private int locTouched;
    private short nr;
    private short nFix;
    private int nAuth;
    private int locAdded;
    private int maxLocAdded;
    private int churn;
    private int maxChurn;
    private float averageChurn;
    private float averageHolydays;
    private boolean isBuggy = false;

    private List<Integer> churnArray = new ArrayList<>();
    private List<String> authors = new ArrayList<>();

    public Class(String path, String content, Release release)  {
        this.path = path;
        this.content = content;
        this.release = release;
    }

    public void setHoliday(float holiday) {
        this.averageHolydays = holiday;
    }

    public double getAverageHolydays() {
        return averageHolydays;
    }

    public String getPath() {
        return path;
    }

    public Integer getVersion() {
        return release.getIndex();
    }

    public String getContent() {
        return content;
    }

    public int getLoc() {
        return loc;
    }

    public void setLoc(int size) {
        this.loc = size;
    }

    public int getLocTouched() {
        return locTouched;
    }

    public short getNr() {
        return nr;
    }

    public void setNr(short nr) {
        this.nr = nr;
    }

    public short getNumFix() {
        return nFix;
    }

    public void addFix() {
        this.nFix++;
    }

    public int getNumAuth() {
        return nAuth;
    }

    public void setNumAuth(int nAuth) {
        this.nAuth = nAuth;
    }

    public int getLocAdded() {
        return locAdded;
    }

    public int getMaxLocAdded() {
        return maxLocAdded;
    }

    public void setMaxLocAdded(int maxLocAdded) {
        this.maxLocAdded = maxLocAdded;
    }

    public int getChurn() {
        return churn;
    }

    public void setChurn(int churn) {
        this.churn = churn;
    }

    public int getMaxChurn() {
        return maxChurn;
    }

    public void setMaxChurn(int maxChurn) {
        this.maxChurn = maxChurn;
    }

    public float getAverageChurn() {
        return averageChurn;
    }

    public void setAverageChurn(float averageChurn) {
        this.averageChurn = averageChurn;
    }

    public void incrementLocTouched(int lines) {
        locTouched += lines;
    }

    public void incrementLocAdded(int lines) {
        locAdded += lines;
    }

    public void incrementLocChurn(int lines) {
        churn += lines;
    }

    public List<Integer> getChurnArray() {
        return churnArray;
    }

    public void addChurnArray(Integer churn) {
        churnArray.add(churn);
    }

    public void incrementNumRevisions() {
        nr += 1;
    }

    public List<String> getAuthors() {
        return authors;
    }

    public void addAuthors(String auth) {
        authors.add(auth);
    }

    public boolean isBuggy() {
        return isBuggy;
    }

    public void setBuggy(boolean buggy) {
        isBuggy = buggy;
    }
}