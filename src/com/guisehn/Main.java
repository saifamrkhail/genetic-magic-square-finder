package com.guisehn;

import com.guisehn.main.MagicSquareFinder;

public class Main {
    public static void main(String[] args) {

        int size = 4;
        int populationSize = 10000000;
        int eliteSize = 1000;
        int eliteDeathPeriod = 100;
        double mutationProbability = 0.03;
        boolean allowDuplicates = false;
        int minimumCrossoverPoint = 0;
        int maximumCrossoverPoint = size - 1;

        MagicSquareFinder finder = new MagicSquareFinder(
                size,
                populationSize,
                eliteSize,
                eliteDeathPeriod,
                mutationProbability,
                allowDuplicates,
                minimumCrossoverPoint,
                maximumCrossoverPoint);

        finder.run();
    }
}
