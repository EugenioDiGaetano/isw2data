package com.isw2data.controller;

import com.isw2data.enumeration.CostSensitive;
import com.isw2data.enumeration.FeatureSelection;
import com.isw2data.model.ClassifierEvaluation;
import com.isw2data.model.Acume;
import com.isw2data.enumeration.Sampling;
import com.isw2data.enumeration.ClassifierType;

import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.BestFirst;
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
import weka.core.SelectedTag;
import weka.core.converters.ConverterUtils;
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
    private static List<Acume> acumeClasses = new ArrayList<>();
    private static final String FILE_PATH = "%s//%s//%s_%d_%s%s";
    private static final String TRAINING_NAME = "training";
    private static final String TESTING_NAME = "testing";
    private static final String ARRF_EXT = ".arff";
    private static final String ACUME_NAME = "acume";
    private static final String PRINT = "NO";

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
            String trainingSet = String.format(FILE_PATH, projectName, TRAINING_NAME, TRAINING_NAME, i, projectName, ARRF_EXT);
            String testingSet = String.format(FILE_PATH, projectName, TESTING_NAME, TESTING_NAME, i, projectName, ARRF_EXT);
            String acumeName = String.format(FILE_PATH, projectName, ACUME_NAME, classifierName, i, projectName, "");

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
            printProbabilities(classifier, training, testing, testing, acumeName, "NONE");

            // FEATURE SELECTION BEAST FIRST
            AttributeSelection featureSelectionBest = new AttributeSelection();
            CfsSubsetEval cfsEvaluatorBest = new CfsSubsetEval(); // Usando CfsSubsetEval
            BestFirst searchBest = new BestFirst();
            searchBest.setDirection(new SelectedTag(0, searchBest.getDirection().getTags()));

            featureSelectionBest.setEvaluator(cfsEvaluatorBest);
            featureSelectionBest.setSearch(searchBest);

            featureSelectionBest.SelectAttributes(training);
            Instances fsBestTraining = featureSelectionBest.reduceDimensionality(training);
            Instances fsBestTesting = featureSelectionBest.reduceDimensionality(testing);

            int numAttributesFsBest = fsBestTraining.numAttributes();
            fsBestTraining.setClassIndex(numAttributesFsBest-1);
            fsBestTesting.setClassIndex(numAttributesFsBest-1);

            StringBuilder attributeNameFsBestBld = new StringBuilder();
            attributeNameFsBestBld.append(fsBestTraining.attribute(0).name());
            for (int k = 1; k < numAttributesFsBest; k++) {
                attributeNameFsBestBld.append(", ").append(fsBestTraining.attribute(k).name());
            }
            String attributeNameFsBest = attributeNameFsBestBld.toString();
            out.println("Attributi scelti da forwards: "+ attributeNameFsBest);

            classifier.buildClassifier(fsBestTraining);

            Evaluation evalWithFsBest = new Evaluation(fsBestTraining);
            evalWithFsBest.evaluateModel(classifier, fsBestTesting);

            ClassifierEvaluation evaluationWithFsBest = new ClassifierEvaluation(projectName, i, evalWithFsBest, classifierName,
                    FeatureSelection.BESTFIRST, Sampling.NONE, CostSensitive.NONE);
            evaluations.add(evaluationWithFsBest);
            printProbabilities(classifier, fsBestTraining, fsBestTesting, testing, acumeName, FeatureSelection.BESTFIRST.name());

            // FEATURE SELECTION GREEDY STEPWISE
            AttributeSelection featureSelectionGreedyStepwise = new AttributeSelection();
            CfsSubsetEval cfsEvaluatorGreedyStepwise = new CfsSubsetEval(); // Usando CfsSubsetEval
            GreedyStepwise searchGreedyStepwise = new GreedyStepwise();
            searchGreedyStepwise.setSearchBackwards(false);

            featureSelectionGreedyStepwise.setEvaluator(cfsEvaluatorGreedyStepwise);
            featureSelectionGreedyStepwise.setSearch(searchGreedyStepwise);

            featureSelectionGreedyStepwise.SelectAttributes(training);
            Instances fsGreedyStepwiseTraining = featureSelectionGreedyStepwise.reduceDimensionality(training);
            Instances fsGreedyStepwiseTesting = featureSelectionGreedyStepwise.reduceDimensionality(testing);

            int numAttributesFsGreedyStepwise = fsGreedyStepwiseTraining.numAttributes();
            fsGreedyStepwiseTraining.setClassIndex(numAttributesFsGreedyStepwise-1);
            fsGreedyStepwiseTesting.setClassIndex(numAttributesFsGreedyStepwise-1);

            StringBuilder attributeNameFsGreedyStepwiseBld = new StringBuilder();
            attributeNameFsGreedyStepwiseBld.append(fsGreedyStepwiseTraining.attribute(0).name());
            for (int k = 1; k < numAttributesFsGreedyStepwise; k++) {
                attributeNameFsGreedyStepwiseBld.append(", ").append(fsGreedyStepwiseTraining.attribute(k).name());
            }
            String attributeNameFsGreedyStepwise = attributeNameFsGreedyStepwiseBld.toString();
            out.println("Attributi scelti da GreedyStepwise: "+ attributeNameFsGreedyStepwise);

            classifier.buildClassifier(fsGreedyStepwiseTraining);
            Evaluation evalWithFsGreedyStepwise = new Evaluation(fsGreedyStepwiseTraining);
            evalWithFsGreedyStepwise.evaluateModel(classifier, fsGreedyStepwiseTesting);

            ClassifierEvaluation evaluationWithFsGreedyStepwise = new ClassifierEvaluation(projectName, i, evalWithFsGreedyStepwise, classifierName,
                    FeatureSelection.GREEDYSTEPWISE, Sampling.NONE, CostSensitive.NONE);
            evaluations.add(evaluationWithFsGreedyStepwise);
            printProbabilities(classifier, fsGreedyStepwiseTraining, fsGreedyStepwiseTesting, testing, acumeName, FeatureSelection.GREEDYSTEPWISE.name());

            // OVERSAMPLING
            double positiveCount = 0;
            double negativeCount = 0;
            for (int k = 0; k<training.numInstances(); k++) {
                if (training.instance(k).classValue() == 0.0) positiveCount++;
                else negativeCount++;
            }
            if (negativeCount > positiveCount) out.println("OK: " + i);

            Resample resample = new Resample();
            resample.setInputFormat(training);

            if ((positiveCount + negativeCount) != 0)
                resample.setSampleSizePercent(2.0*100.0*negativeCount/(positiveCount + negativeCount));
            resample.setSampleSizePercent(200);

            FilteredClassifier fcOver = new FilteredClassifier();
            fcOver.setClassifier(classifier);
            fcOver.setFilter(resample);
            fcOver.buildClassifier(training);

            Evaluation overSamplingEval = new Evaluation(testing);
            overSamplingEval.evaluateModel(fcOver, testing);
            ClassifierEvaluation evaluationWithOverSampling = new ClassifierEvaluation(projectName, i, overSamplingEval, classifierName, FeatureSelection.NONE, Sampling.OVERSAMPLING, CostSensitive.NONE);
            evaluations.add(evaluationWithOverSampling);
            printProbabilities(fcOver, training, testing, testing, acumeName, Sampling.OVERSAMPLING.name());

            // UNDERSAMPLING
            SpreadSubsample spreadSubsample = new SpreadSubsample();
            spreadSubsample.setOptions(new String[]{"-M", "1.0"});

            FilteredClassifier fcUnder = new FilteredClassifier();
            fcUnder.setClassifier(classifier);
            fcUnder.setFilter(spreadSubsample);
            fcUnder.buildClassifier(training);

            Evaluation underSamplingEval = new Evaluation(testing);
            underSamplingEval.evaluateModel(fcUnder, testing);

            ClassifierEvaluation evaluationWithUnderSampling = new ClassifierEvaluation(projectName, i, underSamplingEval, classifierName, FeatureSelection.NONE, Sampling.UNDERSAMPLING, CostSensitive.NONE);
            evaluations.add(evaluationWithUnderSampling);
            printProbabilities(fcUnder, training, testing, testing, acumeName, Sampling.UNDERSAMPLING.name());

            // SMOTE
            SMOTE smote = new SMOTE();
            smote.setInputFormat(training);
            if (negativeCount != 0)
                smote.setPercentage(((negativeCount / positiveCount) * 100.0) - 100.0);

            FilteredClassifier fcSmote = new FilteredClassifier();
            fcSmote.setClassifier(classifier);
            fcSmote.setFilter(smote);
            fcSmote.buildClassifier(training);

            Evaluation evalWithSMOTE = new Evaluation(testing);
            evalWithSMOTE.evaluateModel(fcSmote, testing);
            ClassifierEvaluation evaluationWithSMOTE = new ClassifierEvaluation(projectName, i, evalWithSMOTE, classifierName,
                    FeatureSelection.NONE, Sampling.SMOTE, CostSensitive.NONE);
            evaluations.add(evaluationWithSMOTE);
            printProbabilities(fcSmote, training, testing, testing, acumeName, Sampling.SMOTE.name());

            // COST SENSITIVE
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
            printProbabilities(c1, training, testing, testing, acumeName, CostSensitive.COST_SENSITIVE_CLASSIFIER.name());
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

    public static void createAcumeFiles(List<Acume> classes, String fileName) throws IOException {
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(fileName+".csv");
            fileWriter.append("ID,Size,Predicted %,Actual value").append("\n");
            for (Acume c: classes){
                fileWriter.append(String.valueOf(c.getId())).append(",")
                        .append(c.getLoc()).append(",")
                        .append(c.getProbability()).append(",")
                        .append(c.getValue()).append("\n");
            }

        } finally {
            if (fileWriter != null) fileWriter.close();
        }
    }

    public static void printProbabilities(Classifier classifier, Instances training, Instances testing, Instances testingLoc, String path, String outName) throws Exception {
        String name = path + "_" + outName;
        int index = 0;
        int numtesting = testing.numInstances();
        if (PRINT.equals("ALL")) {
        out.println("There are " + numtesting + " test instances");
}
        if (!acumeClasses.isEmpty()) {
            acumeClasses.clear();
        }
        classifier.buildClassifier(training);

        // Loop over each test instance.
        for (int i = 0; i < numtesting; i++) {
            // Get the true class label from the instance's own classIndex.
            //ritorna il valore che l'istanza i-esima ha nel test set
            String trueClassLabel =
                    testing.instance(i).toString(testing.classIndex());

            int locIndex = testingLoc.attribute("LOC").index();
            int locValue = (int) testingLoc.instance(i).value(locIndex);

            // Make the prediction here.
            double predictionIndex =
                    classifier.classifyInstance(testing.instance(i));

            // Get the predicted class label from the predictionIndex.
            String predictedClassLabel =
                    testing.classAttribute().value((int) predictionIndex);

            // Get the prediction probability distribution.
            //ritorna la probabilità che l'instanza i-esima appartenga a ciascuna delle classi possibili (in questo caso 2)
            double[] predictionDistribution =
                    classifier.distributionForInstance(testing.instance(i));

            // Print out the true label, predicted label, and the distribution.
            if (PRINT.equals("ALL")) {
                out.printf("%5d: true=%-10s, predicted=%-10s, distribution=",
                        i, trueClassLabel, predictedClassLabel);
            }
            // Loop over all the prediction labels in the distribution.
            //in questo caso abbiamo due possibili classi quindi due probabilità
            for (int predictionDistributionIndex = 0;
                 predictionDistributionIndex < predictionDistribution.length;
                 predictionDistributionIndex++) {
                // Get this distribution index's class label.
                String predictionDistributionIndexAsClassLabel =
                        testing.classAttribute().value(
                                predictionDistributionIndex);

                // Get the probability.
                double predictionProbability =
                        predictionDistribution[predictionDistributionIndex];
                if (PRINT.equals("ALL")) {
                    out.printf("[%10s : %6.3f]",
                            predictionDistributionIndexAsClassLabel,
                            predictionProbability);
                }
            }
            if (PRINT.equals("ALL")) {
                out.println();
            }
            Acume acumeClass = new Acume(index, locValue, classifier.distributionForInstance(testing.instance(i))[0], trueClassLabel);
            index++;
            acumeClasses.add(acumeClass);
        }
        createAcumeFiles(acumeClasses, name);
    }

}