package edu.virginia.sde.hw1;

import java.io.*;
import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class Apportionment {

    public static void main(String[] args) {
        try {
            CliOptions options = CliOptions.parse(args);
            if (options == null) {
                printUsageAndExit();
            }

            // Load states
            List<State> states = InputLoader.load(options.inputPath);
            if (states.isEmpty()) {
                throw new IllegalArgumentException("No valid states with valid populations were found in the input file.");
            }

            // Seats sanity (Huntington-Hill requires seats >= number of states)
            if (!options.useHamilton && options.seats < states.size()) {
                throw new IllegalArgumentException(
                        "Huntington-Hill requires at least as many representatives as states. " +
                        "Requested seats=" + options.seats + ", states=" + states.size() + "."
                );
            }

            Map<String, Integer> apportionment;
            if (options.useHamilton) {
                apportionment = Hamilton.apportion(states, options.seats);
            } else {
                apportionment = HuntingtonHill.apportion(states, options.seats);
            }

            // Print alphabetically by state name
            states.stream()
                    .map(State::name)
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .forEach(name -> {
                        int reps = apportionment.getOrDefault(name, 0);
                        System.out.println(name + " - " + reps);
                    });

        } catch (CliOptions.CliException e) {
            System.err.println("Error: " + e.getMessage());
            printUsageAndExit(2);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(2);
        }
    }

    private static void printUsageAndExit() {
        printUsageAndExit(1);
    }

    private static void printUsageAndExit(int code) {
        System.err.println("Usage:");
        System.err.println("  java -jar Apportionment.jar <input.csv|input.xlsx> [totalSeats] [--hamilton]");
        System.err.println("Examples:");
        System.err.println("  java -jar Apportionment.jar 1990census.csv 1000");
        System.err.println("  java -jar Apportionment.jar census2020.xlsx --hamilton");
        System.err.println("  java -jar Apportionment.jar 2020census.xlsx 600 --hamilton");
        System.err.println("  java -jar Apportionment.jar 2020census.csv 20");
        System.exit(code);
    }

    // --- CLI parsing ---
    static final class CliOptions {
        final String inputPath;
        final int seats;
        final boolean useHamilton;

        private CliOptions(String inputPath, int seats, boolean useHamilton) {
            this.inputPath = inputPath;
            this.seats = seats;
            this.useHamilton = useHamilton;
        }

        static class CliException extends RuntimeException {
            CliException(String msg) { super(msg); }
        }

        static CliOptions parse(String[] args) {
            if (args == null || args.length < 1) {
                return null;
            }
            String input = null;
            Integer seats = null;
            boolean hamilton = false;

            for (String a : args) {
                if ("--hamilton".equalsIgnoreCase(a.trim())) {
                    hamilton = true;
                }
            }
            // Find first non-flag as input
            List<String> positionals = Arrays.stream(args)
                    .filter(a -> !a.equalsIgnoreCase("--hamilton"))
                    .collect(Collectors.toList());
            if (positionals.isEmpty()) return null;

            input = positionals.get(0);
            if (positionals.size() >= 2) {
                try {
                    seats = Integer.parseInt(positionals.get(1));
                    if (seats <= 0) throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    throw new CliException("Invalid totalSeats: '" + positionals.get(1) + "'. Must be a positive integer.");
                }
            } else {
                seats = 435; // default
            }

            if (!Files.exists(Path.of(input))) {
                throw new CliException("Input file does not exist: " + input);
            }
            if (!(input.toLowerCase().endsWith(".csv") || input.toLowerCase().endsWith(".xlsx"))) {
                throw new CliException("Unsupported file type. Use .csv or .xlsx");
            }
            return new CliOptions(input, seats, hamilton);
        }
    }

    // --- State record ---
    public record State(String name, long population) {}

    // --- Helpers shared by both methods ---
    static final class Helpers {
        static long totalPopulation(List<State> states) {
            long sum = 0L;
            for (State s : states) sum += s.population();
            return sum;
        }
    }

    // --- Hamilton ---
    static final class Hamilton {
        public static Map<String, Integer> apportion(List<State> states, int seats) {
            if (seats <= 0) throw new IllegalArgumentException("Seats must be positive.");

            long totalPop = Helpers.totalPopulation(states);
            // average population per seat as BigDecimal for precision
            BigDecimal divisor = new BigDecimal(totalPop).divide(new BigDecimal(seats), MathContext.DECIMAL128);

            class Row {
                final State s;
                final int floor;
                final BigDecimal remainder;
                Row(State s, int floor, BigDecimal remainder) {
                    this.s = s; this.floor = floor; this.remainder = remainder;
                }
            }

            List<Row> rows = new ArrayList<>();
            int allocated = 0;
            for (State s : states) {
                BigDecimal quota = new BigDecimal(s.population()).divide(divisor, MathContext.DECIMAL128);
                int floor = quota.setScale(0, java.math.RoundingMode.DOWN).intValueExact();
                BigDecimal remainder = quota.subtract(new BigDecimal(floor));
                rows.add(new Row(s, floor, remainder));
                allocated += floor;
            }

            // base assignment
            Map<String, Integer> reps = new HashMap<>();
            for (Row r : rows) reps.put(r.s.name(), r.floor);

            int remaining = seats - allocated;
            if (remaining < 0) {
                // In rare cases due to rounding extremes; clamp by taking seats from smallest remainders
                // but practically with standard integers it shouldn't happen. We'll handle anyway.
                rows.sort(Comparator.comparing((Row r) -> r.remainder).thenComparing(r -> r.s.name()));
                int take = -remaining;
                for (int i = 0; i < take && i < rows.size(); i++) {
                    String n = rows.get(i).s.name();
                    reps.put(n, Math.max(0, reps.get(n) - 1));
                }
                remaining = 0;
            }
            if (remaining > 0) {
                // allocate by largest remainders
                rows.sort(Comparator.<Row>comparing(r -> r.remainder).reversed()
                        .thenComparing(r -> r.s.name()));
                for (int i = 0; i < remaining && i < rows.size(); i++) {
                    String n = rows.get(i).s.name();
                    reps.put(n, reps.get(n) + 1);
                }
            }
            return reps;
        }
    }

    // --- Huntington–Hill ---
    static final class HuntingtonHill {

        private static double priority(long population, int currentSeats) {
            // population / sqrt(n * (n + 1))
            double denom = Math.sqrt((double) currentSeats * (currentSeats + 1.0));
            return population / denom;
        }

        public static Map<String, Integer> apportion(List<State> states, int seats) {
            if (seats <= 0) throw new IllegalArgumentException("Seats must be positive.");
            int nStates = states.size();
            if (seats < nStates) throw new IllegalArgumentException(
                    "Seats must be at least number of states for Huntington-Hill.");

            // Start with 1 representative each
            Map<String, Integer> reps = new HashMap<>();
            for (State s : states) reps.put(s.name(), 1);

            int remaining = seats - nStates;

            // Priority queue by highest priority
            PriorityQueue<StateAlloc> pq = new PriorityQueue<>(Comparator
                    .comparingDouble(StateAlloc::priority).reversed()
                    .thenComparing(StateAlloc::name));

            // Initialize priorities for next seat
            for (State s : states) {
                pq.add(new StateAlloc(s.name(), s.population(), 1, priority(s.population(), 1)));
            }

            while (remaining-- > 0) {
                StateAlloc top = pq.poll();
                if (top == null) break;
                // give one seat to this state
                reps.put(top.name, reps.get(top.name) + 1);
                int newSeats = top.currentSeats + 1;
                pq.add(new StateAlloc(top.name, top.population, newSeats, priority(top.population, newSeats)));
            }

            return reps;
        }

        private static final class StateAlloc {
            final String name;
            final long population;
            final int currentSeats; // after last assignment
            final double priority;

            StateAlloc(String name, long population, int currentSeats, double priority) {
                this.name = name;
                this.population = population;
                this.currentSeats = currentSeats;
                this.priority = priority;
            }
            String name() { return name; }
            double priority() { return priority; }
        }
    }
}
