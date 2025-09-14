package edu.virginia.sde.hw1;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class InputLoader {

    private InputLoader() {}

    public static List<Apportionment.State> load(String path) throws IOException {
        String lower = path.toLowerCase();
        if (lower.endsWith(".csv")) {
            return loadCsv(path);
        } else if (lower.endsWith(".xlsx")) {
            return loadXlsx(path);
        } else {
            throw new IllegalArgumentException("Unsupported input file type: " + path);
        }
    }

    private static List<Apportionment.State> loadCsv(String path) throws IOException {
        try (Reader reader = Files.newBufferedReader(Path.of(path), StandardCharsets.UTF_8)) {
            CSVParser parser = CSVFormat.DEFAULT
                    .builder()
                    .setIgnoreSurroundingSpaces(true)
                    .setTrim(true)
                    .build()
                    .parse(reader);

            Iterator<CSVRecord> it = parser.iterator();
            if (!it.hasNext()) {
                throw new IllegalArgumentException("CSV file is empty.");
            }
            CSVRecord header = it.next();
            Map<String, Integer> headerMap = headerIndex(header);
            Integer stateIdx = headerMap.get("state");
            Integer popIdx   = headerMap.get("population");
            if (stateIdx == null || popIdx == null) {
                throw new IllegalArgumentException("CSV must contain headers 'State' and 'Population' (case-insensitive).");
            }

            List<Apportionment.State> out = new ArrayList<>();
            while (it.hasNext()) {
                CSVRecord rec = it.next();
                try {
                    String name = rec.size() > stateIdx ? rec.get(stateIdx).trim() : null;
                    String popStr = rec.size() > popIdx ? rec.get(popIdx).trim() : null;
                    if (name == null || name.isEmpty()) continue;
                    if (popStr == null || popStr.isEmpty()) continue;

                    long pop = Long.parseLong(popStr.replaceAll("[_,]", ""));
                    if (pop < 0) continue;

                    out.add(new Apportionment.State(name, pop));
                } catch (Exception e) {
                    // skip bad row; optionally could log warning
                }
            }
            return out;
        }
    }

    private static Map<String, Integer> headerIndex(CSVRecord header) {
        Map<String, Integer> idx = new HashMap<>();
        for (int i = 0; i < header.size(); i++) {
            String key = header.get(i);
            if (key == null) continue;
            String norm = key.trim().toLowerCase();
            if (norm.equals("state") || norm.equals("population")) {
                idx.put(norm, i);
            }
        }
        return idx;
    }

    private static List<Apportionment.State> loadXlsx(String path) throws IOException {
        try (InputStream in = Files.newInputStream(Path.of(path));
             Workbook wb = new XSSFWorkbook(in)) {

            Sheet sheet = wb.getNumberOfSheets() > 0 ? wb.getSheetAt(0) : null;
            if (sheet == null) throw new IllegalArgumentException("XLSX has no sheets.");

            Iterator<Row> it = sheet.iterator();
            if (!it.hasNext()) throw new IllegalArgumentException("XLSX is empty.");

            Row header = it.next();
            Map<String, Integer> idx = headerIndex(header);
            Integer stateIdx = idx.get("state");
            Integer popIdx   = idx.get("population");
            if (stateIdx == null || popIdx == null) {
                throw new IllegalArgumentException("XLSX must contain headers 'State' and 'Population' (case-insensitive).");
            }

            List<Apportionment.State> out = new ArrayList<>();
            while (it.hasNext()) {
                Row r = it.next();
                try {
                    Cell stateCell = r.getCell(stateIdx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    Cell popCell = r.getCell(popIdx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    if (stateCell == null || popCell == null) continue;

                    String name = getString(stateCell);
                    if (name == null || name.isBlank()) continue;

                    String popStr = getString(popCell);
                    if (popStr == null || popStr.isBlank()) continue;

                    long pop = Long.parseLong(popStr.replaceAll("[_,]", ""));
                    if (pop < 0) continue;

                    out.add(new Apportionment.State(name.trim(), pop));
                } catch (Exception e) {
                    // skip bad row
                }
            }
            return out;
        }
    }

    private static Map<String, Integer> headerIndex(Row header) {
        Map<String, Integer> idx = new HashMap<>();
        short last = header.getLastCellNum();
        for (int i = 0; i < last; i++) {
            Cell c = header.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (c == null) continue;
            String key = getString(c);
            if (key == null) continue;
            String norm = key.trim().toLowerCase();
            if (norm.equals("state") || norm.equals("population")) {
                idx.put(norm, i);
            }
        }
        return idx;
    }

    private static String getString(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING -> { return cell.getStringCellValue(); }
            case NUMERIC -> { 
                // Avoid scientific notation when reading as string
                return BigDecimal.valueOf(cell.getNumericCellValue()).toPlainString(); 
            }
            case BOOLEAN -> { return Boolean.toString(cell.getBooleanCellValue()); }
            case FORMULA -> {
                try {
                    return cell.getStringCellValue();
                } catch (IllegalStateException e) {
                    try {
                        return BigDecimal.valueOf(cell.getNumericCellValue()).toPlainString();
                    } catch (Exception ex) {
                        return null;
                    }
                }
            }
            default -> { return null; }
        }
    }
}
