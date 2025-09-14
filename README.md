# Apportionment (HW1 – CS 3140)

This project implements **Hamilton’s Algorithm** and **Huntington–Hill** for congressional apportionment.

## How to build

```bash
./gradlew shadowJar
```

This produces `build/libs/Apportionment.jar`.

## How to run

Default method is **Huntington–Hill**. Use `--hamilton` to switch.

```bash
# Default seats = 435
java -jar build/libs/Apportionment.jar path/to/population.csv

# Specify total seats
java -jar build/libs/Apportionment.jar path/to/population.xlsx 1000

# Hamilton method
java -jar build/libs/Apportionment.jar path/to/population.csv --hamilton
java -jar build/libs/Apportionment.jar path/to/population.xlsx 600 --hamilton
```

Input files may be **CSV** or **XLSX**, with case‑insensitive headers containing `State` and `Population` in any column order. Bad rows are **skipped**; if no valid rows remain, the program errors with a clear message.

## Package

All code is under `edu.virginia.sde.hw1`.

## Team Contributions (example)
- Member 2: Implemented Hamilton and Huntington–Hill algorithms, default HH with `--hamilton` flag, helper utilities (total pop, divisor/priority, sorting).
