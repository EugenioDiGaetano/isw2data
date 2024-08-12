package com.isw2data;

import com.isw2data.controller.GitController;
import com.isw2data.controller.JiraController;
import com.isw2data.controller.WekaController;
import com.isw2data.model.TicketBug;
import com.isw2data.utils.Configuration;
import com.isw2data.model.Release;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.springframework.web.client.RestClientException;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Main {

    public static void main(String[] args) throws Exception {
        try{
        Configuration config = new Configuration("/config%s.properties");
        config.loadConfiguration(args);

        String projectName = config.getProjectName();
        String repoPath = config.getRepoPath();

        Repository repo = new FileRepositoryBuilder().setGitDir(new File(repoPath)).readEnvironment().findGitDir().build();
        JiraController jiraInfo = new JiraController();

        List<Release> releases = jiraInfo.getReleaseInfo(projectName);
        List<TicketBug> tickets = jiraInfo.getFixTicket(projectName, releases);

        jiraInfo.calculateProportionColdStart();
        jiraInfo.assignInjectedInversion(releases);

        GitController gitInfo= new GitController(repo, releases,tickets);
        gitInfo.createDataset(projectName);

        WekaController wekaController = new WekaController();
        wekaController.evaluateProject(projectName, releases.size());

        System.exit(0);
    }   catch (IOException e) {
            throw new IOException("Error IO: " + e.toString());
        }
    }
}