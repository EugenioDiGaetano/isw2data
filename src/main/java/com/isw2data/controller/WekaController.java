package com.isw2data.controller;

import com.isw2data.enumeration.CostSensitive;
import com.isw2data.enumeration.FeatureSelection;
import com.isw2data.model.ClassifierEvaluation;
import com.isw2data.enumeration.Sampling;
import com.isw2data.enumeration.ClassifierType;

import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.CfsSubsetEval;
import weka.attributeSelection.GreedyStepwise;
import weka.classifiers.Classifier;
import weka.classifiers.CostMatrix;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.meta.CostSensitiveClassifier;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;
import weka.filters.Filter;
import weka.filters.supervised.instance.SMOTE;
import weka.filters.supervised.instance.SpreadSubsample;
import weka.filters.unsupervised.instance.Resample;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.System.*;

public class WekaController {
    // Lista per memorizzare i risultati delle valutazioni
    private List<ClassifierEvaluation> evaluations = new ArrayList<>();
    private static final String FILE_PATH = "%s//%s//%s_%d_%s.arff";
    private static final String TRAINING_NAME = "training";
    private static final String TESTING_NAME = "testing";

    // Valuta un progetto con più classificatori su diverse release
    public void evaluateProject(String projectName, int numReleases) throws Exception {
        for (ClassifierType classifierType : ClassifierType.values()) {
            crossForwardEvaluations(projectName, classifierType.name(), numReleases);
        }
        printEvaluationsToCsv(projectName);
    }

    // Esegue la valutazione incrociata "forward" per un progetto
    public void crossForwardEvaluations(String projectName, String classifierName, int numReleases) throws Exception {

        Classifier classifier = switch (classifierName) {
            case "NAIVE_BAYES" -> new NaiveBayes();
            case "IBK" -> new IBk();
            default -> new RandomForest();
        };

        out.println("Evaluation for " + classifierName);

        // Nell'i-esima iterazione del walkForward, il training set contiene fino alla release i, il testing set è costituito dalla release i + 1
        for (int i = 2; i < numReleases; i++) {
            String trainingSet = String.format(FILE_PATH, projectName, TRAINING_NAME, TRAINING_NAME, i, projectName);
            String testingSet = String.format(FILE_PATH, projectName, TESTING_NAME, TESTING_NAME, i, projectName);
            Instances training = ConverterUtils.DataSource.read(trainingSet);
            Instances testing = ConverterUtils.DataSource.read(testingSet);

            int numAttr = training.numAttributes();
            training.setClassIndex(numAttr - 1);
            testing.setClassIndex(numAttr - 1);

            classifier.buildClassifier(training);
            Evaluation eval = new Evaluation(testing);
            eval.evaluateModel(classifier, testing);

            ClassifierEvaluation evaluation = new ClassifierEvaluation(projectName, i, eval, classifierName, FeatureSelection.NONE, Sampling.NONE, CostSensitive.NONE);
            evaluations.add(evaluation);

            // Esegui la selezione delle caratteristiche
            AttributeSelection featureSelection = new AttributeSelection();
            CfsSubsetEval cfsEvaluator = new CfsSubsetEval(); // Usando CfsSubsetEval
            GreedyStepwise search = new GreedyStepwise();
            search.setSearchBackwards(false);

            featureSelection.setEvaluator(cfsEvaluator);
            featureSelection.setSearch(search);

            // Applica la selezione delle caratteristiche al set di training
            featureSelection.SelectAttributes(training);
            Instances reducedTraining = featureSelection.reduceDimensionality(training);

            // Applica la selezione delle caratteristiche al set di testing
            Instances reducedTesting = featureSelection.reduceDimensionality(testing);

            // Costruisci e valuta il modello con selezione delle caratteristiche
            classifier.buildClassifier(reducedTraining);
            Evaluation evalWithFS = new Evaluation(reducedTraining);
            evalWithFS.evaluateModel(classifier, reducedTesting);

            // Salva i risultati della valutazione con feature selection specifica
            ClassifierEvaluation evaluationWithFS = new ClassifierEvaluation(projectName, i, evalWithFS, classifierName,
                    FeatureSelection.BESTFIRST, Sampling.NONE, CostSensitive.NONE);
            evaluations.add(evaluationWithFS);

            // Applicare oversampling senza selezione delle caratteristiche
            SMOTE smote = new SMOTE();
            smote.setInputFormat(training);
            Instances smoteTraining = Filter.useFilter(training, smote);

            // Costruisci e valuta il modello con oversampling senza selezione delle caratteristiche
            classifier.buildClassifier(smoteTraining);
            Evaluation evalWithSMOTE = new Evaluation(smoteTraining);
            evalWithSMOTE.evaluateModel(classifier, testing);
            ClassifierEvaluation evaluationWithSMOTE = new ClassifierEvaluation(projectName, i, evalWithSMOTE, classifierName,
                    FeatureSelection.NONE, Sampling.SMOTE, CostSensitive.NONE);
            evaluations.add(evaluationWithSMOTE);

            // Applicare oversampling senza selezione delle caratteristiche
            Resample resample = new Resample();
            resample.setInputFormat(training);
            resample.setNoReplacement(false);
            resample.setSampleSizePercent(200);

            FilteredClassifier fcOver = new FilteredClassifier();
            fcOver.setClassifier(classifier);
            fcOver.setFilter(resample);
            fcOver.buildClassifier(training);

            Evaluation overSamplingEval = new Evaluation(testing);
            overSamplingEval.evaluateModel(fcOver, testing);

            ClassifierEvaluation evaluationWithOverSampling = new ClassifierEvaluation(projectName, i, overSamplingEval, classifierName, FeatureSelection.NONE, Sampling.OVERSAMPLING, CostSensitive.NONE);
            evaluations.add(evaluationWithOverSampling);

            // Applicare undersampling senza selezione delle caratteristiche
            SpreadSubsample spreadSubsample = new SpreadSubsample();
            spreadSubsample.setOptions(new String[]{"-M", "1.0"}); // Bilancia le classi

            FilteredClassifier fcUnder = new FilteredClassifier();
            fcUnder.setClassifier(classifier);
            fcUnder.setFilter(spreadSubsample);
            fcUnder.buildClassifier(training);

            Evaluation underSamplingEval = new Evaluation(testing);
            underSamplingEval.evaluateModel(fcUnder, testing);

            ClassifierEvaluation evaluationWithUnderSampling = new ClassifierEvaluation(projectName, i, underSamplingEval, classifierName, FeatureSelection.NONE, Sampling.UNDERSAMPLING, CostSensitive.NONE);
            evaluations.add(evaluationWithUnderSampling);

            // Applicare il classificatore cost-sensitive
            CostSensitiveClassifier c1 = new CostSensitiveClassifier();
            c1.setClassifier(classifier);
            c1.setCostMatrix(createCostMatrix(1, 10)); // Crea una matrice dei costi
            c1.setMinimizeExpectedCost(true); // Minimizza il costo atteso
            c1.buildClassifier(training);

            Evaluation ec1 = new Evaluation(testing, c1.getCostMatrix());
            ec1.evaluateModel(c1, testing);

            ClassifierEvaluation evaluationWithCostSensitive = new ClassifierEvaluation(projectName, i, ec1, classifierName, FeatureSelection.NONE, Sampling.NONE, CostSensitive.COST_SENSITIVE_CLASSIFIER);
            evaluationWithCostSensitive.setCost(1, 10);
            evaluations.add(evaluationWithCostSensitive);
        }
    }

    // Crea una matrice dei costi con pesi specifici per FP e FN
    private CostMatrix createCostMatrix(double weightFalsePositive, double weightFalseNegative) {
        CostMatrix costMatrix = new CostMatrix(2);
        costMatrix.setCell(0, 0, 0.0);
        costMatrix.setCell(1, 0, weightFalsePositive);
        costMatrix.setCell(0, 1, weightFalseNegative);
        costMatrix.setCell(1, 1, 0.0);
        return costMatrix;
    }

    // Stampa le probabilità di classificazione
    public static void printProbabilities(String projectName, int numReleases) throws Exception {
        Classifier classifier = new RandomForest();
        String trainingSet = String.format(FILE_PATH, projectName, TRAINING_NAME, TRAINING_NAME, (numReleases - 1), projectName);
        String testingSet = String.format(FILE_PATH, projectName, TESTING_NAME, TESTING_NAME, (numReleases - 1), projectName);
        Instances training = ConverterUtils.DataSource.read(trainingSet);
        Instances testing = ConverterUtils.DataSource.read(testingSet);

        int numAttr = training.numAttributes();
        training.setClassIndex(numAttr - 1);
        testing.setClassIndex(numAttr - 1);

        int numtesting = testing.numInstances();
        out.println("There are " + numtesting + " test instances");

        classifier.buildClassifier(training);

        // Loop su ogni istanza del set di test
        for (int i = 0; i < numtesting; i++) {
            // Ottieni l'etichetta di classe reale dall'indice di classe dell'istanza
            String trueClassLabel = testing.instance(i).toString(testing.classIndex());

            // Effettua la previsione
            double predictionIndex = classifier.classifyInstance(testing.instance(i));

            // Ottieni l'etichetta di classe prevista dall'indice di previsione
            String predictedClassLabel = testing.classAttribute().value((int) predictionIndex);

            // Ottieni la distribuzione delle probabilità di previsione
            double[] predictionDistribution = classifier.distributionForInstance(testing.instance(i));

            // Stampa l'etichetta reale, l'etichetta prevista e la distribuzione
            out.printf("%5d: true=%-10s, predicted=%-10s, distribution=",
                    i, trueClassLabel, predictedClassLabel);

            // Loop su tutte le etichette di previsione nella distribuzione
            for (int predictionDistributionIndex = 0; predictionDistributionIndex < predictionDistribution.length; predictionDistributionIndex++) {
                String predictionDistributionIndexAsClassLabel = testing.classAttribute().value(predictionDistributionIndex);
                double predictionProbability = predictionDistribution[predictionDistributionIndex];

                out.printf(" [%10s : %6.3f] ", predictionDistributionIndexAsClassLabel, predictionProbability);
            }
        }
    }

    // Stampa le valutazioni su CSV
    public void printEvaluationsToCsv(String projName) throws IOException {
        FileWriter fileWriter = null;
        String outname = projName + "//evaluations" + projName + ".csv";
        try {
            fileWriter = new FileWriter(outname);
            fileWriter.append("Dataset, #TrainingReleases, Classifier, FeatureSelection, Balancing, CostSensitive, TruePositive, FalsePositive, TrueNegative, FalseNegative, Cost, Precision, Recall, AUC, Kappa, FMeasure");
            fileWriter.append("\n");
            for (ClassifierEvaluation evaluation : evaluations) {
                fileWriter.append(evaluation.getProjName());
                fileWriter.append(",");
                fileWriter.append(String.valueOf(evaluation.getNumTrainingReleases()));
                fileWriter.append(",");
                fileWriter.append(evaluation.getClassifier());
                fileWriter.append(",");
                fileWriter.append(String.valueOf(evaluation.getFeatureSelection()));
                fileWriter.append(",");
                fileWriter.append(String.valueOf(evaluation.getSampling()));
                fileWriter.append(",");
                fileWriter.append(String.valueOf(evaluation.getCostSensitive()));
                fileWriter.append(",");
                fileWriter.append(String.valueOf(evaluation.getTp()));
                fileWriter.append(",");
                fileWriter.append(String.valueOf(evaluation.getFp()));
                fileWriter.append(",");
                fileWriter.append(String.valueOf(evaluation.getTn()));
                fileWriter.append(",");
                fileWriter.append(String.valueOf(evaluation.getFn()));
                fileWriter.append(",");
                fileWriter.append(String.valueOf(evaluation.getCost()));
                fileWriter.append(",");
                fileWriter.append(String.valueOf(evaluation.getPrecision()));
                fileWriter.append(",");
                fileWriter.append(String.valueOf(evaluation.getRecall()));
                fileWriter.append(",");
                fileWriter.append(String.valueOf(evaluation.getAuc()));
                fileWriter.append(",");
                fileWriter.append(String.valueOf(evaluation.getKappa()));
                fileWriter.append(",");
                fileWriter.append(String.valueOf(evaluation.getF1()));
                fileWriter.append("\n");
            }
        } finally {
            if (fileWriter != null) fileWriter.close();
        }
    }
}