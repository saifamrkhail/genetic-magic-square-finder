package com.guisehn.main;

import com.guisehn.crossover.Crossover2;
import com.guisehn.crossover.CrossoverOperator;
import com.guisehn.crossover.CrossoverResult;

import java.util.*;

public class MagicSquareFinder {
    private final int size;
    private final int arraySize;
    private final int populationSize;
    private final int eliteSize;
    private final int eliteDeathPeriod;
    private final int minimumCrossoverPoint;
    private final int maximumCrossoverPoint;
    private final double mutationProbability;
    private final boolean allowDuplicates;
    private final MagicSquareFitnessCalculator fitnessCalculator;
    private final RandomMagicSquareGenerator randomGenerator;
    private final CrossoverOperator crossoverOperator;
    private final Random random = new Random();
    private final Set<Individual> magicSquaresFound;
    private final List<Individual> population;
    private int generationCount;
    private int amountOfGenerationsSinceLastNewMagicSquare;

    public MagicSquareFinder(int size, int populationSize, int eliteSize,
                             int eliteDeathPeriod, double mutationProbability,
                             boolean allowDuplicates, int minimumCrossoverPoint,
                             int maximumCrossoverPoint) {
        this.size = size;
        this.arraySize = (int) Math.pow(size, 2);
        this.populationSize = populationSize;
        this.eliteSize = eliteSize;
        this.eliteDeathPeriod = eliteDeathPeriod;
        this.mutationProbability = mutationProbability;
        this.allowDuplicates = allowDuplicates;
        this.minimumCrossoverPoint = minimumCrossoverPoint;
        this.maximumCrossoverPoint = maximumCrossoverPoint;
        this.fitnessCalculator = new MagicSquareFitnessCalculator(size);
        this.randomGenerator = new RandomMagicSquareGenerator(size);
        this.crossoverOperator = new Crossover2();
        this.magicSquaresFound = new HashSet<>();
        this.population = new ArrayList<>();
    }

    public void run() {
        generationCount = amountOfGenerationsSinceLastNewMagicSquare = 0;
        generateInitialPopulation();

        while (true) {
            population.sort(Comparator.comparingInt(Individual::getFitness));
            addAndPublishMagicSquares();

            //search is completed, break out and stop the program.
            if (magicSquaresFound.size() > 0) {
                break;
            }

            createNewGeneration();
        }
        //print magic squares
        Iterator<Individual> iterator = magicSquaresFound.iterator();
        while (iterator.hasNext()) {
            Individual individual = iterator.next();
            int[] numbers = individual.getSquare();
            for (int i = 0; i < numbers.length; i++) {
                if (i % this.size == 0) {
                    System.out.println();
                }
                System.out.print(numbers[i] + " ");
            }
            System.out.println();
        }
    }

    private void generateInitialPopulation() {
        population.clear();

        for (int i = 0; i < populationSize; i++) {
            population.add(new Individual(randomGenerator.generate(),
                    null,
                    null,
                    null,
                    "",
                    fitnessCalculator)
            );
        }
    }

    /**
     * Checks if there are new magic squares on the current generation.
     * If there are new ones, add them to the magic square list
     */
    private void addAndPublishMagicSquares() {
        Individual[] magicSquares = population.stream()
                .filter(i -> i.getFitness() == 0)
                .toArray(Individual[]::new);

        for (Individual magicSquare : magicSquares) {
            boolean added = magicSquaresFound.add(magicSquare);

            if (added) {
                amountOfGenerationsSinceLastNewMagicSquare = 0;
            }
        }
    }

    private List<Individual> createMatingPool() {
        List<Individual> matingPool = new ArrayList<>();

        int poolSize = populationSize / 2;
        while (matingPool.size() < poolSize) {
            Individual i1 = Utils.getRandom(population);
            Individual i2 = Utils.getRandom(population);

            if (i1 == i2) {
                continue;
            }

            matingPool.add(i1.getFitness() > i2.getFitness() ? i1 : i2);
        }

        return matingPool;
    }

    private void createNewGeneration() {
        generationCount++;

        // Applies the elite death period
        if (eliteDeathPeriod != 0 && amountOfGenerationsSinceLastNewMagicSquare > eliteDeathPeriod) {
            population.subList(0, eliteSize).clear();
            amountOfGenerationsSinceLastNewMagicSquare = 0;
        } else {
            amountOfGenerationsSinceLastNewMagicSquare++;
        }

        List<Individual> matingPool = createMatingPool();

        // Elitism. Transfers the N best individuals to the next generation.
        try {
            population.subList(eliteSize, populationSize).clear();
            population.forEach(individual -> individual.setBelongsToElite(true));
        } catch (java.lang.IndexOutOfBoundsException e) {
            e.printStackTrace();
        }

        while (population.size() < populationSize) {
            Individual i1 = Utils.getRandom(matingPool);
            Individual i2 = Utils.getRandom(matingPool);
            Individual[] children = crossoverAndMutate(i1, i2);

            if (allowDuplicates) {
                population.addAll(Arrays.asList(children));
            } else {
                for (Individual child : children) {
                    String representation = child.toString();
                    boolean duplicate = false;

                    for (Individual individual : population) {
                        if (representation.equals(individual.toString())) {
                            duplicate = true;
                            break;
                        }
                    }

                    if (!duplicate) {
                        population.add(child);
                    }
                }
            }
        }
    }

    /**
     * Performs the crossover of two individuals and (possibily) mutation
     *
     * @param parent1 1st parent
     * @param parent2 2nd parent
     * @return children
     */
    private Individual[] crossoverAndMutate(Individual parent1, Individual parent2) {
        CrossoverResult result = crossoverOperator.crossover(parent1.getSquare(),
                parent2.getSquare(), minimumCrossoverPoint, maximumCrossoverPoint);

        int[][] children = result.getChildren();
        int[][] mutationPoints = new int[children.length][];

        // Mutation
        for (int i = 0; i < children.length; i++) {
            int[] child = children[i];

            if (Math.random() <= mutationProbability) {
                int index1, index2;

                do {
                    index1 = random.nextInt(arraySize);
                    index2 = random.nextInt(arraySize);
                }
                while (index1 == index2);

                int aux = child[index1];
                child[index1] = child[index2];
                child[index2] = aux;

                mutationPoints[i] = new int[]{index1, index2};
            } else {
                mutationPoints[i] = null;
            }
        }

        // Transforms the int arrays to Individual objects
        Individual[] individuals = new Individual[children.length];

        for (int i = 0; i < individuals.length; i++) {
            individuals[i] = new Individual(children[i], parent1.getSquare(),
                    parent2.getSquare(), mutationPoints[i], result.getDetails(),
                    fitnessCalculator);
        }

        return individuals;
    }

}