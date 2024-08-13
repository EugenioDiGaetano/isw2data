package com.isw2data.controller;

import com.isw2data.model.TicketBug;
import com.isw2data.utils.JsonUtils;
import com.isw2data.enumeration.ProjName;
import com.isw2data.model.Release;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.format.DateTimeFormatter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import static java.lang.System.*;

public class JiraController {
    private static final HashMap<LocalDateTime, String> releaseNames = new HashMap<>();
    private static final HashMap<LocalDateTime, String> releaseID = new HashMap<>();
    private static final ArrayList<LocalDateTime> releases = new ArrayList<>();
    private double coldStartProportion;

    // Recupera informazioni sulle release di un progetto
    public List<Release> getReleaseInfo(String projectName) throws IOException {
        String url = "https://issues.apache.org/jira/rest/api/2/project/" + projectName;
        JSONObject json = JsonUtils.readJsonFromUrl(url);
        JSONArray versions = json.getJSONArray("versions");

        for (int i = 0; i < versions.length(); i++) {
            String releaseDate = "";
            String name = "";
            String id = "";
            if (versions.getJSONObject(i).has("releaseDate")) {
                releaseDate = versions.getJSONObject(i).get("releaseDate").toString();
                if (versions.getJSONObject(i).has("name"))
                    name = versions.getJSONObject(i).get("name").toString();
                if (versions.getJSONObject(i).has("id"))
                    id = versions.getJSONObject(i).get("id").toString();
                addRelease(releaseDate, name, id);
            }
        }
        releases.sort(LocalDateTime::compareTo);
        List<Release> allReleases = new ArrayList<>();
        int numVersions = releases.size();

        // Salva i dati delle release e crea le release ordinandole
        for (int i = 0; i < numVersions; i++) {
            int index = i + 1;
            int releaseId = Integer.parseInt(releaseID.get(releases.get(i)));
            String name = releaseNames.get(releases.get(i));
            LocalDateTime releaseDate = releases.get(i);
            Release releaseTemp = new Release(index, releaseId, name, releaseDate);
            allReleases.add(releaseTemp);
        }

        return allReleases;
    }

    // Aggiunge una release alle strutture dati di supporto
    private void addRelease(String strDate, String name, String id) {
        LocalDate date = LocalDate.parse(strDate);
        LocalDateTime dateTime = date.atStartOfDay();
        if (!releases.contains(dateTime))
            releases.add(dateTime);
        releaseNames.put(dateTime, name);
        releaseID.put(dateTime, id);
    }

    // Recupera i bug fix ticket di un progetto
    public List<TicketBug> getFixTicket(String projectName, List<Release> allReleases) throws IOException {
        int i = 0;
        int total;
        List<TicketBug> fixTickets = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");

        do {
            int j = i + 1000;
            String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"
                    + projectName + "%22AND%22issueType%22=%22Bug%22AND(%22status%22=%22closed%22OR"
                    + "%22status%22=%22resolved%22)AND%22resolution%22=%22fixed%22&fields=key,resolutiondate,versions,created&startAt="
                    + i + "&maxResults=" + j;
            JSONObject json = JsonUtils.readJsonFromUrl(url);
            JSONArray issues = json.getJSONArray("issues");
            total = json.getInt("total");
            int deletedTicket = 0;

            for (; i < total && i < j; i++) {
                String key = issues.getJSONObject(i % 1000).get("key").toString();
                JSONObject issuesObject = issues.getJSONObject(i % 1000).getJSONObject("fields");
                JSONArray versions = issuesObject.getJSONArray("versions");
                List<Release> affectedVersions = new ArrayList<>();
                LocalDateTime openingDate = LocalDateTime.parse(issuesObject.get("created").toString(), formatter);
                LocalDateTime fixDate = LocalDateTime.parse(issuesObject.get("resolutiondate").toString(), formatter);
                getAffectedVersions(versions, affectedVersions, allReleases);
                Release openingVersion = getVersionFromDate(openingDate, allReleases);
                Release fixVersion = getVersionFromDate(fixDate, allReleases);
                Release injectedVersion = (!affectedVersions.isEmpty()) ? affectedVersions.get(0) : null;

                TicketBug ticket = createTicket(key, openingDate, fixDate, injectedVersion, fixVersion, openingVersion, affectedVersions);

                // Scarto i ticket con IV = FV e se IV > OV non considero IV
                if (ticket == null) deletedTicket++;
                else {
                    fixTickets.add(ticket);
                    fixVersion.getFixTickets().add(ticket);
                }
            }
            out.println("Ticket eliminati: " + deletedTicket);
            out.println("Ticket non eliminati: " + fixTickets.size());

        } while (i < total);
        return fixTickets;
    }

    // Crea un ticket bug escludendo alcuni casi specifici
    private TicketBug createTicket(String key, LocalDateTime creation, LocalDateTime resolution,
                                   Release injectedVersion, Release fixVersion, Release openingVersion,
                                   List<Release> affectedVersions) {
        // Escludo IV = FV
        if ((injectedVersion != null) && (fixVersion.getIndex() == injectedVersion.getIndex())) {
            return null;
        }

        // Se IV > OV, non considero IV
        if ((injectedVersion != null) && (injectedVersion.getIndex() > openingVersion.getIndex())) {
            injectedVersion = null;
        }

        return new TicketBug(key, creation, resolution, openingVersion, fixVersion, affectedVersions, injectedVersion);
    }

    // Recupera le versioni interessate da un ticket
    private void getAffectedVersions(JSONArray versions, List<Release> affectedVersions, List<Release> allReleases) {
        for (int k = 0; k < versions.length(); k++) {
            String affectedVersion = versions.getJSONObject(k).get("id").toString();
            int id = Integer.parseInt(affectedVersion);
            affectedVersions.add(getVersionFromId(id, allReleases));
        }
    }

    // Recupera la versione corrispondente a una certa data
    private Release getVersionFromDate(LocalDateTime date, List<Release> allReleases) {
        Release currentVersion = allReleases.get(0);
        for (Release release : allReleases) {
            if (release.getReleaseDate().isAfter(date)) {
                currentVersion = release;
                break;
            }
        }
        return currentVersion;
    }

    // Recupera la versione dato un certo ID (identificativo univoco per le versioni su JIRA)
    private Release getVersionFromId(long id, List<Release> allReleases) {
        Release currentVersion = allReleases.get(0);
        for (Release release : allReleases) {
            if (release.getId() == id) {
                currentVersion = release;
                break;
            }
        }
        return currentVersion;
    }

    // Recupera la versione dato un certo indice
    private Release getVersionFromIndex(double index, List<Release> allReleases) {
        Release currentVersion = allReleases.get(0);
        for (Release release : allReleases) {
            if (release.getIndex() == index) {
                currentVersion = release;
                break;
            }
        }
        return currentVersion;
    }

    // Assegna la versione di iniezione (IV) ai ticket che non la presentano
    public void assignInjectedInversion(List<Release> releases) {
        for (Release release : releases) {
            // Calcola la proportion per una certa versione
            release.setProportion(calculateProportion(release, releases));
            // Applica la proportion calcolata a tutti i ticket di questa versione
            for (TicketBug ticket : release.getFixTickets()) {
                if (ticket.getInjectedVersion() == null) {
                    int difference = ticket.getFixVersion().getIndex() - ticket.getOpeningVersion().getIndex();
                    if (difference == 0) difference = 1;
                    double injectedIndex = ticket.getFixVersion().getIndex() - (difference * ticket.getFixVersion().getProportion());
                    double index = Math.ceil(injectedIndex);
                    Release injectedVersion = getVersionFromIndex(index, releases);
                    ticket.setInjectedVersion(injectedVersion);
                }
            }
        }
    }

    // Calcolo proportion per i tickets che sono stati esclusi
    private double calculateProportion(Release currentVersion, List<Release> allReleases) {
        int count = 0;
        double proportion;
        for (Release release : allReleases) {
            if (release.getId() == currentVersion.getId() || count > 5) {
                break;
            }
            for (TicketBug ticket : release.getFixTickets()) {
                if (ticket.getInjectedVersion() != null) {
                    count++;
                    if (count > 5) break;
                }
            }
        }
        if (count < 5)
            proportion = coldStartProportion;
        else
            proportion = calculateProportionIncrement(currentVersion, allReleases);
        return proportion;
    }

    // Incrementa la proportion basata sui ticket precedenti
    private double calculateProportionIncrement(Release currentVersion, List<Release> allReleases) {
        double sum = 0.0;
        double numTickets = 0.0;

        // Raccoglie tutti i ticket fino alla versione corrente
        for (Release release : allReleases) {
            if (release.getId() == currentVersion.getId()) {
                break;
            }
            for (TicketBug ticket : release.getFixTickets()) {
                double fixIndex = ticket.getFixVersion().getIndex();
                double injectedIndex = ticket.getInjectedVersion().getIndex();
                double openingIndex = ticket.getOpeningVersion().getIndex();
                double difference = fixIndex - openingIndex;
                if (difference == 0) difference = 1;
                double proportion = (fixIndex - injectedIndex) / difference;
                if (proportion >= 1) {
                    sum += proportion;
                    numTickets += 1;
                }
            }
        }
        return (numTickets != 0) ? sum / numTickets : 1;
    }

    // Calcola il cold start proportion per i progetti in ProjName
    public void calculateProportionColdStart() throws IOException {
        List<Double> medium = new ArrayList<>();
        out.println("Sto calcolando coldStart");
        for (ProjName proj : ProjName.values()) {
            String project = proj.name();
            List<Release> allReleases = getReleaseInfo(project);
            List<TicketBug> tickets = getFixTicket(project, allReleases);

            double sum = 0.0;
            double numTickets = 0.0;
            for (TicketBug fixTicket : tickets) {
                if (fixTicket.getInjectedVersion() == null) continue;
                double fixIndex = fixTicket.getFixVersion().getIndex();
                double injectedIndex = fixTicket.getInjectedVersion().getIndex();
                double openingIndex = fixTicket.getOpeningVersion().getIndex();
                double difference = fixIndex - openingIndex;
                if (difference == 0) {
                    difference = 1;
                }
                double proportion = (fixIndex - injectedIndex) / difference;
                sum += proportion;
                numTickets += 1;
            }

            if (numTickets != 0) medium.add(sum / numTickets);
            out.println("Proportion del progetto " + proj + ": " + (sum / numTickets));
        }

        // Ritorna la mediana, cioè l'elemento in posizione centrale dopo aver ordinato
        medium.sort(Double::compareTo);
        coldStartProportion = medium.get((ProjName.values().length - 1) / 2);
        out.println("Cold start calcolato = " + coldStartProportion);
    }
}