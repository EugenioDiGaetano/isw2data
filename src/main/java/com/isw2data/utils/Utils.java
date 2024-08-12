package com.isw2data.utils;
import static java.lang.System.*;

import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;

import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedWriter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.TimeZone;

public class Utils {
    //converte unix time in localDateTime
    public static LocalDateTime convertTime(long unixSeconds) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(unixSeconds),
                TimeZone.getDefault().toZoneId());
    }
    /*
    public static void convertCsvToArff(String fileName) throws IOException {
        CSVLoader loader = new CSVLoader();
        loader.setSource(new File(fileName));
        Instances data = loader.getDataSet();

        ArffSaver saver = new ArffSaver();
        saver.setInstances(data);
        String[] parts = fileName.split("\\.");
        saver.setFile(new File(parts[0] + ".arff"));
        saver.writeBatch();
        replaceLine(parts[0] + ".arff");
    }

     */


    public static void replaceLine(String filePath) throws IOException {
        BufferedReader reader = null;
        BufferedWriter writer = null;
        StringBuilder content = null;
        try {
            reader = new BufferedReader(new FileReader(filePath));
            content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) content.append(line).append(System.lineSeparator());
        } finally {
            if (reader != null) reader.close();
        }
        try {
            writer = new BufferedWriter(new FileWriter(filePath));
            writer.write(content.toString());
            writer.close();
        } catch (IOException e) {
            out.println("Si è verificato un errore durante la sostituzione della linea: " + e.getMessage());
        } finally {
            if (writer != null) writer.close();
        }
    }
}