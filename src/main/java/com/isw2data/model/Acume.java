package com.isw2data.model;

public class Acume {
        private int id;
        private int loc;
        private double probability;
        private String value;

        public Acume(int id, int size, double predictedProbability, String actualValue) {
            this.id = id;
            this.loc = size;
            this.probability = predictedProbability;
            this.value = actualValue;
        }


        public String getLoc() {
            return String.valueOf(loc);
        }

        public String getProbability() {
            return String.valueOf(probability);
        }

        public String getValue() {
            return value;
        }

        public int getId() {
            return id;
        }
}

