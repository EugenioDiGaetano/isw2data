package com.isw2data.utils;

import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

import static java.lang.System.*;

public class Configuration {

    private String projectName;
    private String repoPath;
    private static final String CONFIG_FILE_PATH_TEMPLATE = "/config%s.properties";

    public void loadConfiguration(String[] args) throws Exception {
        String configFilePath;

        if (args.length == 1) {
            String configOption = args[0];
            configFilePath = getConfigFilePath(configOption);
        } else {
            out.println("Avviando Bookkeeper di default");
            configFilePath = String.format(CONFIG_FILE_PATH_TEMPLATE, "BOOKKEEPER");
        }

        try (InputStream is = Objects.requireNonNull(Configuration.class.getResource(configFilePath)).openStream()) {
            Properties p = new Properties();
            p.load(is);
            this.repoPath = p.getProperty("repository");
            this.projectName = p.getProperty("projectName");
        }
    }

    private String getConfigFilePath(String configOption) {
        String configName;
        switch (configOption) {
            case "1":
                out.println("Avviando Syncope");
                configName = "SYNCOPE";
                break;
            default:
                out.println("Avviando default Bookkeeper");
                configName = "BOOKKEEPER";
                break;
        }
        return String.format(CONFIG_FILE_PATH_TEMPLATE, configName);
    }

    public String getProjectName() {
        return projectName;
    }

    public String getRepoPath() {
        return repoPath;
    }
}
