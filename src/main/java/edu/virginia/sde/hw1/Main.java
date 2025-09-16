//package edu.virginia.sde.hw1;
//import java.util.*;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Paths;
//import org.apache.poi.ss.usermodel.*;
//import org.apache.poi.xssf.usermodel.XSSFWorkbook;
package edu.virginia.sde.hw1;

import java.util.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

//import org.apache.poi.ss.usermodel.*;
//import org.apache.poi.xssf.usermodel.XSSFWorkbook;
//import java.io.FileInputStream;


public class Main {


    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Error: No Input File Specified");
            System.err.println("Usage: java -jar Apportionment.jar <filename>");
            System.exit(1);
        }


        String filename = args[0];


        Map<String, Integer> statePopulations = null;
        try {
            if (filename.toLowerCase().endsWith(".csv")) {
                statePopulations = parseCSV(filename);
            } else if (filename.toLowerCase().endsWith(".xlsx")) {
               // statePopulations = parseXLSX(filename);
            } else {
                System.err.println("Unsupported file type. Must be .csv or .xlsx");
                System.exit(1);
            }


            //Next line should print the parsed data
            statePopulations.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> System.out.println(entry.getKey() + ": " + entry.getValue()));
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            System.exit(1);
        }
    }

    //CSV file parser
    private static Map<String, Integer> parseCSV(String filename) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filename));
        if (lines.isEmpty()) {
            throw new IOException("CSV file is empty: " + filename);
        }

        //parse header row in order to find state and population columns
        String header = lines.get(0);
        String[] headerColumns = header.split(",");
        int stateCol = -1;
        int popCol = -1;

        for (int i = 0; i < headerColumns.length; i++) {
            String colName = headerColumns[i].trim().toLowerCase();
            if (colName.equals("state")) {
                stateCol = i;
            } else if (colName.equals("population")) {
                popCol = i;
            }
        }

        if (stateCol == -1 || popCol == -1) {

            throw new IOException("CSV file missing required 'State' and/or 'Population' columns.");
        }

        Map<String, Integer> statePopulations = new HashMap<>();

        //parse every row
        for (int i = 1; i < lines.size(); i++) {
            String row = lines.get(i).trim();
            if (row.isEmpty()) continue;

            String[] cols = row.split(",");
            if (cols.length <= Math.max(stateCol, popCol)) {
                System.err.println("Warning: Skipping bad row (not enough columns): " + row);
                continue;
            }

            String state = cols[stateCol].trim();
            String popStr = cols[popCol].trim();

            try {
                int population = Integer.parseInt(popStr);
                if (population < 0) {
                    System.err.println("Warning: Negative population for state " + state + " - skipped.");
                    continue;
                }
                statePopulations.put(state, population);
            } catch (NumberFormatException e) {
                System.err.println("Warning: Invalid population for state " + state + " - skipped.");
            }
        }

        if (statePopulations.isEmpty()) {
            throw new IOException("No valid state population data found in CSV file.");
        }

        return statePopulations;
    }
}