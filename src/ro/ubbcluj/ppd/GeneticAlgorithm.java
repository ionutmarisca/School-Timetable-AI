package ro.ubbcluj.ppd;


import mpi.MPI;

import java.util.stream.IntStream;

public class GeneticAlgorithm {

    protected int tournamentSize;
    private int populationSize;
    private double mutationRate;
    private double crossoverRate;
    private int elitismCount;
    private String[] args;

    public GeneticAlgorithm(int populationSize, double mutationRate, double crossoverRate, int elitismCount,
                            int tournamentSize, String[] args) {

        this.populationSize = populationSize;
        this.mutationRate = mutationRate;
        this.crossoverRate = crossoverRate;
        this.elitismCount = elitismCount;
        this.tournamentSize = tournamentSize;
        this.args = args;
    }

    /**
     * Initialize population
     *
     * @param timetable The timetable used for Population initialize
     * @return population The initial population generated
     */
    public Population initPopulation(Timetable timetable) {
        // Initialize population
        Population population = new Population(this.populationSize, timetable);
        return population;
    }

    /**
     * Check if population has met termination condition
     *
     * @param generationsCount Number of generations passed
     * @param maxGenerations   Number of generations to terminate after
     * @return boolean True if termination condition met, otherwise, false
     */
    public boolean isTerminationConditionMet(int generationsCount, int maxGenerations) {
        return (generationsCount > maxGenerations);
    }

    /**
     * Check if population has met termination condition
     *
     * @param population
     * @return boolean True if termination condition met, otherwise, false
     */
    public boolean isTerminationConditionMet(Population population) {
        return population.getFittest(0).getFitness() == 1.0;
    }

    /**
     * Calculate individual's fitness value
     *
     * @param individual
     * @param timetable
     * @return fitness
     */
    public double calcFitness(Individual individual, Timetable timetable) {

        // Create new timetable object to use -- cloned from an existing timetable
        Timetable threadTimetable = new Timetable(timetable);
        threadTimetable.createClasses(individual);

        // Calculate fitness
        int clashes = threadTimetable.calcClashes();
        double fitness = 1 / (double) (clashes + 1);

        individual.setFitness(fitness);

        return fitness;
    }

    /**
     * Evaluate population
     *
     * @param population
     * @param timetable
     */
    public void evalPopulation(Population population, Timetable timetable) {
        MPI.Init(args);
        int me = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();

         if (me == 0) {
                double populationFitness =0;

            // At this point, we should have all fitness values calculated for each Individual
            // Loop over population evaluating individuals and summing population
            // fitness
            for (Individual individual : population.getIndividuals()) {
                MPI.COMM_WORLD.Ssend(individual, 1, 1, MPI.OBJECT, MPI.ANY_SOURCE, 1);
                MPI.COMM_WORLD.Ssend(timetable, 1, 1, MPI.OBJECT, MPI.ANY_SOURCE, 2);
                double receivedValue = 0;
                MPI.COMM_WORLD.Recv(receivedValue, 1, 1, MPI.DOUBLE, MPI.ANY_SOURCE, MPI.ANY_TAG);
                populationFitness += receivedValue;
            }

            population.setPopulationFitness(populationFitness);
        } else {
            Timetable recvTimetable = new Timetable();
            MPI.COMM_WORLD.Recv(recvTimetable, 1, 1, MPI.OBJECT, 0, 2);
            Individual individual = new Individual(recvTimetable);
            MPI.COMM_WORLD.Recv(individual, 1, 1, MPI.OBJECT, 0, 1);
            double fitness = this.calcFitness(individual, recvTimetable);
            MPI.COMM_WORLD.Ssend(fitness, 1, 1, MPI.DOUBLE, 0, MPI.ANY_TAG);
        }
        MPI.Finalize();
    }

    /**
     * Evaluate population, optimized for Java 8
     *
     * @param population
     * @param timetable
     */
    public void evalPopulationOptimized(Population population, Timetable timetable) {
        IntStream.range(0, population.size()).parallel()
                .forEach(i -> this.calcFitness(population.getIndividual(i), timetable));

        double populationFitness = 0;

        // At this point, we should have all fitness values calculated for each Individual
        // Loop over population evaluating individuals and summing population
        // fitness
        for (Individual individual : population.getIndividuals()) {
            populationFitness += individual.getFitness();
        }

        population.setPopulationFitness(populationFitness);
    }

    /**
     * Selects parent for crossover using tournament selection
     * <p>
     * Tournament selection works by choosing N random individuals, and then
     * choosing the best of those.
     *
     * @param population
     * @return The individual selected as a parent
     */
    public Individual selectParent(Population population) {
        // Create tournament
        Population tournament = new Population(this.tournamentSize);

        // Add random individuals to the tournament
        population.shuffle();
        for (int i = 0; i < this.tournamentSize; i++) {
            Individual tournamentIndividual = population.getIndividual(i);
            tournament.setIndividual(i, tournamentIndividual);
        }

        // Return the best
        return tournament.getFittest(0);
    }


    /**
     * Apply mutation to population
     *
     * @param population
     * @param timetable
     * @return The mutated population
     */
    public Population mutatePopulation(Population population, Timetable timetable) {
        // Initialize new population
        Population newPopulation = new Population(this.populationSize);

        // Loop over current population by fitness
        for (int populationIndex = 0; populationIndex < population.size(); populationIndex++) {
            Individual individual = population.getFittest(populationIndex);

            // Create random individual to swap genes with
            Individual randomIndividual = new Individual(timetable);

            // Loop over individual's genes
            for (int geneIndex = 0; geneIndex < individual.getChromosomeLength(); geneIndex++) {
                // Skip mutation if this is an elite individual
                if (populationIndex > this.elitismCount) {
                    // Does this gene need mutation?
                    if (this.mutationRate > Math.random()) {
                        // Swap for new gene
                        individual.setGene(geneIndex, randomIndividual.getGene(geneIndex));
                    }
                }
            }

            // Add individual to population
            newPopulation.setIndividual(populationIndex, individual);
        }

        // Return mutated population
        return newPopulation;
    }

    /**
     * Apply crossover to population
     *
     * @param population The population to apply crossover to
     * @return The new population
     */
    public Population crossoverPopulation(Population population) {
        // Create new population
        Population newPopulation = new Population(population.size());

        // Loop over current population by fitness
        for (int populationIndex = 0; populationIndex < population.size(); populationIndex++) {
            Individual parent1 = population.getFittest(populationIndex);

            // Apply crossover to this individual?
            if (this.crossoverRate > Math.random() && populationIndex >= this.elitismCount) {
                // Initialize offspring
                Individual offspring = new Individual(parent1.getChromosomeLength());

                // Find second parent
                Individual parent2 = selectParent(population);

                // Loop over genome
                for (int geneIndex = 0; geneIndex < parent1.getChromosomeLength(); geneIndex++) {
                    // Use half of parent1's genes and half of parent2's genes
                    if (0.5 > Math.random()) {
                        offspring.setGene(geneIndex, parent1.getGene(geneIndex));
                    } else {
                        offspring.setGene(geneIndex, parent2.getGene(geneIndex));
                    }
                }

                // Add offspring to new population
                newPopulation.setIndividual(populationIndex, offspring);
            } else {
                // Add individual to new population without applying crossover
                newPopulation.setIndividual(populationIndex, parent1);
            }
        }

        return newPopulation;
    }


}
