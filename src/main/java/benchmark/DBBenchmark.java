package benchmark;

import dao.PatientDAO;
import util.AppCache;
import util.DBConnection;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Standalone benchmark tool for the Hospital Health Management System.
 *
 * <p>Measures and compares query execution times under four conditions:</p>
 * <ol>
 *   <li>Full table scan — no index, no cache (cold baseline)</li>
 *   <li>Index scan — index present, no cache (DB-only optimisation)</li>
 *   <li>Cache miss — index present, first call populates cache</li>
 *   <li>Cache hit — index present, second call served entirely from RAM</li>
 * </ol>
 *
 * <p>Results are printed to the console and saved to {@code benchmark_results.csv}
 * in the project root directory.</p>
 *
 * <p><b>How to run:</b> right-click this file in IntelliJ → Run 'DBBenchmark.main()'.
 * Make sure MySQL is running and the {@code hhm} database is populated before
 * executing.</p>
 */
public class DBBenchmark {

    /** Number of times each timed operation is repeated to get a stable average. */
    private static final int RUNS = 5;

    /** Search keyword used consistently across all benchmark tests. */
    private static final String SEARCH_TERM = "Uwi";

    /** Output CSV file path — written to the project root. */
    private static final String CSV_PATH = "benchmark_results.csv";

    /** Index name and DDL used for drop/restore operations. */
    private static final String INDEX_NAME  = "idx_patient_name";
    private static final String DROP_SQL    = "DROP INDEX idx_patient_name ON Patient";
    private static final String CREATE_SQL  = "CREATE INDEX idx_patient_name ON Patient (Name)";

    // -------------------------------------------------------------------------

    /**
     * Entry point. Runs all benchmark suites in sequence and writes a CSV report.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        System.out.println("==========================================================");
        System.out.println("  HHM Database Performance Benchmark");
        System.out.println("  " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.println("==========================================================\n");

        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Scenario", "Operation", "Avg Time (ms)", "Rows Returned", "Notes"});

        try {
            // ------------------------------------------------------------------
            // 1. Baseline: full table scan with NO index
            // ------------------------------------------------------------------
            System.out.println("[ 1 / 4 ]  Full table scan — dropping index...");
            runDDL(DROP_SQL);

            long baselineFull  = timeQuery("SELECT * FROM Patient", 0, RUNS);
            long baselineSearch = timeSearchLike(SEARCH_TERM, RUNS);
            int  baselineRows  = countRows("SELECT * FROM Patient");
            int  searchRows    = countRows("SELECT * FROM Patient WHERE Name LIKE '%" + SEARCH_TERM + "%'");

            rows.add(new String[]{"No Index", "SELECT * (all rows)", ms(baselineFull),  String.valueOf(baselineRows),  "Full table scan"});
            rows.add(new String[]{"No Index", "Search LIKE '" + SEARCH_TERM + "'", ms(baselineSearch), String.valueOf(searchRows), "Full table scan — no index"});

            printRow("No Index | SELECT all",          baselineFull,  baselineRows);
            printRow("No Index | Search '" + SEARCH_TERM + "'", baselineSearch, searchRows);

            // ------------------------------------------------------------------
            // 2. Index present: same queries with idx_patient_name
            // ------------------------------------------------------------------
            System.out.println("\n[ 2 / 4 ]  Restoring index...");
            runDDL(CREATE_SQL);

            long indexedFull   = timeQuery("SELECT * FROM Patient", 0, RUNS);
            long indexedSearch = timeSearchLike(SEARCH_TERM, RUNS);

            rows.add(new String[]{"With Index", "SELECT * (all rows)", ms(indexedFull),   String.valueOf(baselineRows),  "Index on Name column"});
            rows.add(new String[]{"With Index", "Search LIKE '" + SEARCH_TERM + "'", ms(indexedSearch), String.valueOf(searchRows), "Index on Name column"});

            printRow("With Index | SELECT all",          indexedFull,  baselineRows);
            printRow("With Index | Search '" + SEARCH_TERM + "'", indexedSearch, searchRows);

            // ------------------------------------------------------------------
            // 3. Cache miss: first call to PatientDAO.findAll() hits the DB
            // ------------------------------------------------------------------
            System.out.println("\n[ 3 / 4 ]  Cache miss (cold call)...");
            AppCache.invalidate(model.Patient.class);

            PatientDAO dao = new PatientDAO();
            long cacheMissTime = timeDAOCall(dao, RUNS, true); // clears cache before each run

            rows.add(new String[]{"Cache Miss", "PatientDAO.findAll()", ms(cacheMissTime), String.valueOf(baselineRows), "Cache cleared before each call — hits DB"});
            printRow("Cache Miss | findAll()", cacheMissTime, baselineRows);

            // ------------------------------------------------------------------
            // 4. Cache hit: second call served entirely from RAM
            // ------------------------------------------------------------------
            System.out.println("\n[ 4 / 4 ]  Cache hit (warm call)...");
            dao.findAll(); // warm the cache
            long cacheHitTime = timeDAOCall(dao, RUNS, false); // cache stays warm

            rows.add(new String[]{"Cache Hit", "PatientDAO.findAll()", ms(cacheHitTime), String.valueOf(baselineRows), "Served from RAM — no DB round-trip"});
            printRow("Cache Hit  | findAll()", cacheHitTime, baselineRows);

            // ------------------------------------------------------------------
            // Summary
            // ------------------------------------------------------------------
            System.out.println("\n----------------------------------------------------------");
            System.out.printf("  Index speedup on search  : %.2fx%n",
                (double) baselineSearch / Math.max(1, indexedSearch));
            System.out.printf("  Cache speedup (miss→hit) : %.2fx%n",
                (double) cacheMissTime / Math.max(1, cacheHitTime));
            System.out.println("----------------------------------------------------------\n");

            // Append speedup summary to CSV
            rows.add(new String[]{"", "", "", "", ""});
            rows.add(new String[]{"Summary", "Index speedup (search)",
                String.format("%.2fx", (double) baselineSearch / Math.max(1, indexedSearch)), "", ""});
            rows.add(new String[]{"Summary", "Cache speedup (miss→hit)",
                String.format("%.2fx", (double) cacheMissTime  / Math.max(1, cacheHitTime)),  "", ""});

            // Write CSV
            writeCsv(rows);
            System.out.println("Results saved to: " + CSV_PATH);

        } catch (SQLException e) {
            System.err.println("SQL error during benchmark: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Could not write CSV: " + e.getMessage());
        }
    }

    // =========================================================================
    //  Timing helpers
    // =========================================================================

    /**
     * Executes a plain SQL SELECT repeatedly and returns the average elapsed
     * time in milliseconds.
     *
     * @param sql      the SELECT statement to execute
     * @param paramLong a {@code BIGINT} parameter to bind (pass {@code 0} when
     *                  the query has no parameters)
     * @param runs     number of repetitions to average over
     * @return average execution time in milliseconds
     * @throws SQLException if the query fails
     */
    private static long timeQuery(String sql, long paramLong, int runs) throws SQLException {
        long total = 0;
        for (int i = 0; i < runs; i++) {
            long start = System.nanoTime();
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                if (paramLong != 0) stmt.setLong(1, paramLong);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) { /* consume all rows */ }
            }
            total += System.nanoTime() - start;
        }
        return (total / runs) / 1_000_000L;
    }

    /**
     * Executes a {@code LIKE} search on the Patient Name column repeatedly and
     * returns the average elapsed time in milliseconds.
     *
     * @param keyword the search keyword (wrapped automatically with {@code %})
     * @param runs    number of repetitions to average over
     * @return average execution time in milliseconds
     * @throws SQLException if the query fails
     */
    private static long timeSearchLike(String keyword, int runs) throws SQLException {
        String sql  = "SELECT * FROM Patient WHERE Name LIKE ?";
        String term = "%" + keyword + "%";
        long total  = 0;
        for (int i = 0; i < runs; i++) {
            long start = System.nanoTime();
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, term);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) { /* consume all rows */ }
            }
            total += System.nanoTime() - start;
        }
        return (total / runs) / 1_000_000L;
    }

    /**
     * Times repeated calls to {@link dao.PatientDAO#findAll()} to measure
     * the cache's effect on response time.
     *
     * @param dao         the {@code PatientDAO} instance to use
     * @param runs        number of repetitions to average over
     * @param clearCache  if {@code true}, the cache entry is invalidated before
     *                    each run (simulates a cold miss); if {@code false}, the
     *                    cache is left warm (simulates a hit)
     * @return average execution time in milliseconds
     * @throws SQLException if the DAO's database call fails
     */
    private static long timeDAOCall(PatientDAO dao, int runs, boolean clearCache)
            throws SQLException {
        long total = 0;
        for (int i = 0; i < runs; i++) {
            if (clearCache) AppCache.invalidate(model.Patient.class);
            long start = System.nanoTime();
            dao.findAll();
            total += System.nanoTime() - start;
        }
        return (total / runs) / 1_000_000L;
    }

    // =========================================================================
    //  DDL helpers
    // =========================================================================

    /**
     * Executes a DDL statement (e.g. {@code DROP INDEX}, {@code CREATE INDEX})
     * directly against the database.
     *
     * @param ddl the DDL statement to execute
     * @throws SQLException if the statement fails
     */
    private static void runDDL(String ddl) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(ddl)) {
            stmt.execute();
            System.out.println("  DDL OK: " + ddl);
        }
    }

    /**
     * Counts the rows returned by a SELECT query.
     *
     * @param sql the query to count rows for
     * @return number of rows in the result set
     * @throws SQLException if the query fails
     */
    private static int countRows(String sql) throws SQLException {
        int count = 0;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) count++;
        }
        return count;
    }

    // =========================================================================
    //  Output helpers
    // =========================================================================

    /**
     * Prints a single benchmark result row to standard output in a fixed-width
     * format.
     *
     * @param label label describing the scenario
     * @param ms    elapsed time in milliseconds
     * @param rows  number of rows returned
     */
    private static void printRow(String label, long ms, int rows) {
        System.out.printf("  %-45s %4d ms   (%d rows)%n", label, ms, rows);
    }

    /**
     * Converts a {@code long} millisecond value to a {@code String} for CSV output.
     *
     * @param ms milliseconds
     * @return string representation
     */
    private static String ms(long ms) { return String.valueOf(ms); }

    /**
     * Writes all benchmark result rows to a CSV file.
     *
     * <p>The file is created (or overwritten) at {@link #CSV_PATH} in the
     * current working directory. Each row in the list represents one CSV line;
     * each element in the array represents one cell.</p>
     *
     * @param rows list of string arrays, each representing one CSV row
     * @throws IOException if the file cannot be written
     */
    private static void writeCsv(List<String[]> rows) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(CSV_PATH))) {
            for (String[] row : rows) {
                pw.println(String.join(",", row));
            }
        }
    }
}
