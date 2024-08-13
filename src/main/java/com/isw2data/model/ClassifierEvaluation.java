package com.isw2data.model;

import com.isw2data.enumeration.CostSensitive;
import com.isw2data.enumeration.FeatureSelection;
import com.isw2data.enumeration.Sampling;
import weka.classifiers.Evaluation;

/**
 * Classe per la valutazione di un classificatore.
 */
public class ClassifierEvaluation {

    // Nome del progetto
    private String projName;
    // Numero di release di addestramento utilizzate
    private int numTrainingReleases;
    // Nome del classificatore utilizzato
    private String classifier;
    // Numero di veri positivi
    private int tp;
    // Numero di falsi positivi
    private int fp;
    // Numero di veri negativi
    private int tn;
    // Numero di falsi negativi
    private int fn;
    // Precisione del classificatore
    private double precision;
    // Richiamo (recall) del classificatore
    private double recall;
    // Area sotto la curva ROC (AUC)
    private double auc;
    // Statistica kappa
    private double kappa;
    // F1-score del classificatore
    private double f1;
    // Costo totale calcolato
    private double cost;
    // Metodo di selezione delle caratteristiche
    private FeatureSelection featureSelection;
    // Metodo di campionamento utilizzato
    private Sampling sampling;
    // Metodo di sensibilità ai costi utilizzato
    private CostSensitive costSensitive;

    /**
     * Costruttore della classe.
     *
     * @param project          Nome del progetto.
     * @param numRelease       Numero di release di addestramento.
     * @param evaluation       Oggetto Evaluation di Weka contenente le metriche di valutazione.
     * @param classifier       Nome del classificatore utilizzato.
     * @param featureSelection Metodo di selezione delle caratteristiche.
     * @param sampling         Metodo di campionamento.
     * @param costSensitive    Metodo di sensibilità ai costi.
     */
    public ClassifierEvaluation(String project, int numRelease, Evaluation evaluation, String classifier, FeatureSelection featureSelection, Sampling sampling, CostSensitive costSensitive) {
        this.projName = project;
        this.numTrainingReleases = numRelease;
        this.classifier = classifier;
        this.featureSelection = featureSelection;
        this.sampling = sampling;
        this.costSensitive = costSensitive;
        this.precision = evaluation.precision(0);
        this.recall = evaluation.recall(0);
        this.auc = evaluation.areaUnderROC(0);
        this.kappa = evaluation.kappa();
        this.tp = (int) evaluation.numTruePositives(0);
        this.fp = (int) evaluation.numFalsePositives(0);
        this.tn = (int) evaluation.numTrueNegatives(0);
        this.fn = (int) evaluation.numFalseNegatives(0);
        this.f1 = evaluation.fMeasure(0);
        this.cost = 0.0;
    }

    /**
     * Calcola e imposta il costo totale basato sui costi di falsi positivi e falsi negativi.
     *
     * @param costFP Costo per ciascun falso positivo.
     * @param costFN Costo per ciascun falso negativo.
     */
    public void setCost(double costFP, double costFN) {
        this.cost = fp * costFP + fn * costFN;
    }

    // Restituisce il nome del progetto
    public String getProjName() {
        return projName;
    }

    // Restituisce il numero di release di addestramento
    public int getNumTrainingReleases() {
        return numTrainingReleases;
    }

    // Restituisce il nome del classificatore
    public String getClassifier() {
        return classifier;
    }

    // Restituisce il metodo di selezione delle caratteristiche
    public FeatureSelection getFeatureSelection() {
        return featureSelection;
    }

    // Restituisce il metodo di campionamento
    public Sampling getSampling() {
        return sampling;
    }

    // Restituisce il metodo di sensibilità ai costi
    public CostSensitive getCostSensitive() {
        return costSensitive;
    }

    // Restituisce la precisione del classificatore
    public double getPrecision() {
        return precision;
    }

    // Restituisce il richiamo (recall) del classificatore
    public double getRecall() {
        return recall;
    }

    // Restituisce l'area sotto la curva ROC (AUC)
    public double getAuc() {
        return auc;
    }

    // Restituisce la statistica kappa
    public double getKappa() {
        return kappa;
    }

    // Restituisce il numero di veri positivi
    public int getTp() {
        return tp;
    }

    // Restituisce il numero di falsi positivi
    public int getFp() {
        return fp;
    }

    // Restituisce il numero di veri negativi
    public int getTn() {
        return tn;
    }

    // Restituisce il numero di falsi negativi
    public int getFn() {
        return fn;
    }

    // Restituisce il costo totale calcolato
    public double getCost() {
        return cost;
    }

    // Restituisce il F1-score del classificatore
    public double getF1() {
        return f1;
    }
}