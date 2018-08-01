package ro.ubbcluj.ppd;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class TimetableGA {

    public static void main(String[] args) {
        long startTime = System.nanoTime();

        // Get a Timetable object with all the available information.
        Timetable timetable = initializeTimetable();

        // Initialize GA
        GeneticAlgorithm ga = new GeneticAlgorithm(100, 0.01, 0.9, 2, 5);

        // Initialize population
        Population population = ga.initPopulation(timetable);

        // Evaluate population
        ga.evalPopulation(population, timetable);

        // Keep track of current generation
        int generation = 1;

        // Start evolution loop
        while (ga.isTerminationConditionMet(generation, 1000) == false
                && ga.isTerminationConditionMet(population) == false) {
            // Print fitness
            System.out.println("G" + generation + " Best fitness: " + population.getFittest(0).getFitness());

            // Apply crossover
            population = ga.crossoverPopulation(population);

            // Apply mutation
            population = ga.mutatePopulation(population, timetable);

            // Evaluate population
            ga.evalPopulation(population, timetable);

            // Increment the current generation
            generation++;
        }
        long endTime = System.nanoTime();

        // Print fitness
        timetable.createClasses(population.getFittest(0));
        System.out.println();
        System.out.println("Solution found in " + generation + " generations");
        System.out.println("Final solution fitness: " + population.getFittest(0).getFitness());
        System.out.println("Clashes: " + timetable.calcClashes());

        // Print classes
        System.out.println();
        ro.ubbcluj.ppd.Class classes[] = timetable.getClasses();
        int classIndex = 1;
        for (ro.ubbcluj.ppd.Class bestClass : classes) {
            System.out.println("Class " + classIndex + ":");
            System.out.println("Module: " +
                    timetable.getModule(bestClass.getModuleId()).getModuleName());
            System.out.println("Group: " +
                    timetable.getGroup(bestClass.getGroupId()).getGroupId());
            System.out.println("Room: " +
                    timetable.getRoom(bestClass.getRoomId()).getRoomNumber());
            System.out.println("Professor: " +
                    timetable.getProfessor(bestClass.getProfessorId()).getProfessorName());
            System.out.println("Time: " +
                    timetable.getTimeslot(bestClass.getTimeslotId()).getTimeslot());
            System.out.println("-----");
            classIndex++;
        }

        NumberFormat formatter = new DecimalFormat("#0.00000");
        System.out.println("Elapsed time: " + formatter.format((double)(endTime - startTime) / 1E9 ) + " seconds.");
    }

    /**
     * Creates a Timetable with all the necessary course information.
     * <p>
     * Normally you'd get this info from a database.
     *
     * @return
     */
    private static Timetable initializeTimetable() {
        // Create timetable
        Timetable timetable = new Timetable();

        // Set up rooms
        timetable.addRoom(1, "100", 15);
        timetable.addRoom(2, "101", 30);
        timetable.addRoom(4, "102", 20);
        timetable.addRoom(5, "103", 25);

        // Set up timeslots
        timetable.addTimeslot(1, "Luni 9:00 - 11:00");
        timetable.addTimeslot(2, "Luni 11:00 - 13:00");
        timetable.addTimeslot(3, "Luni 13:00 - 15:00");
        timetable.addTimeslot(4, "Marti 9:00 - 11:00");
        timetable.addTimeslot(5, "Marti 11:00 - 13:00");
        timetable.addTimeslot(6, "Marti 13:00 - 15:00");
        timetable.addTimeslot(7, "Marti 9:00 - 11:00");
        timetable.addTimeslot(8, "Miercuri 11:00 - 13:00");
        timetable.addTimeslot(9, "Miercuri 13:00 - 15:00");
        timetable.addTimeslot(10, "Joi 9:00 - 11:00");
        timetable.addTimeslot(11, "Joi 11:00 - 13:00");
        timetable.addTimeslot(12, "Joi 13:00 - 15:00");
        timetable.addTimeslot(13, "Vineri 9:00 - 11:00");
        timetable.addTimeslot(14, "Vineri 11:00 - 13:00");
        timetable.addTimeslot(15, "Vineri 13:00 - 15:00");

        // Set up professors
        timetable.addProfessor(1, "Pop Ioan");
        timetable.addProfessor(2, "Avram Diana");
        timetable.addProfessor(3, "Mihalache Andrei");
        timetable.addProfessor(4, "Ivan Raluca");

        // Set up modules and define the professors that teach them
        timetable.addModule(1, "asc", "ASC", new int[] { 1, 2 });
        timetable.addModule(2, "lct", "Logica Computationala", new int[] { 1, 3 });
        timetable.addModule(3, "fp", "Fundamentele Programarii", new int[] { 1, 2 });
        timetable.addModule(4, "rdc", "Retele De Calculatoare", new int[] { 3, 4 });
        timetable.addModule(5, "map", "Metode Avansate De Programare", new int[] { 4 });
        timetable.addModule(6, "pw", "Programare Web", new int[] { 1, 4 });

        // Set up student groups and the modules they take.
        timetable.addGroup(1, 10, new int[] { 1, 3, 4 });
        timetable.addGroup(2, 30, new int[] { 2, 3, 5, 6 });
        timetable.addGroup(3, 18, new int[] { 3, 4, 5 });
        timetable.addGroup(4, 25, new int[] { 1, 4 });
        timetable.addGroup(5, 20, new int[] { 2, 3, 5 });
        timetable.addGroup(6, 22, new int[] { 1, 4, 5 });
        timetable.addGroup(7, 16, new int[] { 1, 3 });
        timetable.addGroup(8, 18, new int[] { 2, 6 });
        return timetable;
    }
}
