package io.github.larsw.webdav.postgresql;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.larsw.webdav.core.spi.WebDavException;
import io.github.larsw.webdav.core.spi.WebDavResource;
import io.github.larsw.webdav.core.spi.WebDavResourceStore;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Read-only {@link WebDavResourceStore} that exposes PostgreSQL table rows as virtual files.
 *
 * <h2>Virtual layout</h2>
 * <pre>
 * /                                    ← root collection (lists all mapping names)
 * /&lt;mapping.name&gt;/                    ← one collection per {@link TableMapping}
 *   [&lt;pathColumn&gt;/]*                  ← zero or more nested collection segments
 *     &lt;nameColumn&gt;.csv (.json)        ← one file per row
 * </pre>
 *
 * <h2>Content generation</h2>
 * <ul>
 *   <li>CSV format: RFC 4180 — one header row + one data row per file.  All columns
 *       except {@code pathColumn} and {@code nameColumn} are included.</li>
 *   <li>JSON format: all non-metadata columns as a JSON object, <em>or</em> the raw
 *       value of the configured {@code jsonColumn} (useful for {@code jsonb} columns).</li>
 * </ul>
 *
 * <h2>Write operations</h2>
 * All mutating operations ({@code PUT}, {@code DELETE}, {@code MKCOL}, {@code COPY},
 * {@code MOVE}) throw {@link WebDavException} with HTTP 405 — this store is read-only.
 *
 * <h2>Performance note</h2>
 * Every request executes a full {@code SELECT *} over the configured table/query.
 * This is intentional (simplicity, always-fresh data) and suitable for moderate-sized
 * tables.  Add a caching layer ({@code @Cacheable}) or materialised views at the
 * database level when serving large datasets.
 */
public class PostgresqlResourceStore implements WebDavResourceStore {

    private final JdbcTemplate jdbc;
    private final List<TableMapping> mappings;
    private final ObjectMapper objectMapper;

    public PostgresqlResourceStore(JdbcTemplate jdbc,
                                   List<TableMapping> mappings,
                                   ObjectMapper objectMapper) {
        this.jdbc         = Objects.requireNonNull(jdbc, "jdbc");
        this.mappings     = List.copyOf(Objects.requireNonNull(mappings, "mappings"));
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    // =========================================================================
    // WebDavResourceStore — read operations
    // =========================================================================

    @Override
    public Optional<WebDavResource> getResource(String path) {
        String p = normalizePath(path);

        // Root collection
        if ("/".equals(p)) {
            return Optional.of(PostgresqlWebDavResource.collection("/"));
        }

        // Parse: /mappingName[/rest]
        String relative = p.substring(1);                       // strip leading /
        int slash = relative.indexOf('/');
        String mappingName = slash >= 0 ? relative.substring(0, slash) : relative;
        String rest        = slash >= 0 ? relative.substring(slash + 1) : "";

        TableMapping mapping = findMapping(mappingName);
        if (mapping == null) return Optional.empty();

        // Mapping root collection
        if (rest.isEmpty()) {
            return Optional.of(PostgresqlWebDavResource.collection(p));
        }

        // Try to resolve as a file (must end with the mapping's extension)
        String ext = mapping.fileExtension();
        if (rest.endsWith(ext)) {
            int lastSlash  = rest.lastIndexOf('/');
            String folder  = lastSlash >= 0 ? rest.substring(0, lastSlash) : "";
            String rawName = lastSlash >= 0
                    ? rest.substring(lastSlash + 1, rest.length() - ext.length())
                    : rest.substring(0, rest.length() - ext.length());

            Map<String, Object> row = findRow(mapping, folder, rawName);
            if (row == null) return Optional.empty();

            byte[] content     = generateContent(mapping, row);
            Instant lastMod    = extractTimestamp(row);
            return Optional.of(PostgresqlWebDavResource.file(
                    p, content.length, mapping.contentType(), computeETag(content), lastMod));
        }

        // Otherwise try as a collection (virtual folder driven by pathColumn values)
        if (folderExistsInMapping(mapping, rest)) {
            return Optional.of(PostgresqlWebDavResource.collection(p));
        }

        return Optional.empty();
    }

    @Override
    public List<WebDavResource> listChildren(String path) {
        String p = normalizePath(path);

        // Root — list one collection per mapping
        if ("/".equals(p)) {
            return mappings.stream()
                    .map(m -> (WebDavResource) PostgresqlWebDavResource.collection("/" + m.getName()))
                    .toList();
        }

        // Parse mapping name and sub-folder
        String relative    = p.substring(1);
        int slash          = relative.indexOf('/');
        String mappingName = slash >= 0 ? relative.substring(0, slash) : relative;
        String folderPath  = slash >= 0 ? relative.substring(slash + 1) : "";

        TableMapping mapping = findMapping(mappingName);
        if (mapping == null) return List.of();

        return listChildrenOfFolder(mapping, "/" + mappingName, folderPath);
    }

    @Override
    public InputStream getContent(String path) throws WebDavException {
        String p = normalizePath(path);
        if ("/".equals(p)) throw new WebDavException(409, "Path is a collection: " + path);

        String relative    = p.substring(1);
        int slash          = relative.indexOf('/');
        if (slash < 0) throw new WebDavException(409, "Path is a collection: " + path);

        String mappingName = relative.substring(0, slash);
        String rest        = relative.substring(slash + 1);

        TableMapping mapping = findMapping(mappingName);
        if (mapping == null) throw new WebDavException(404, "Not found: " + path);

        String ext = mapping.fileExtension();
        if (!rest.endsWith(ext)) throw new WebDavException(404, "Not found: " + path);

        int lastSlash  = rest.lastIndexOf('/');
        String folder  = lastSlash >= 0 ? rest.substring(0, lastSlash) : "";
        String rawName = lastSlash >= 0
                ? rest.substring(lastSlash + 1, rest.length() - ext.length())
                : rest.substring(0, rest.length() - ext.length());

        Map<String, Object> row = findRow(mapping, folder, rawName);
        if (row == null) throw new WebDavException(404, "Not found: " + path);

        return new ByteArrayInputStream(generateContent(mapping, row));
    }

    // =========================================================================
    // WebDavResourceStore — write operations (all rejected — read-only store)
    // =========================================================================

    @Override
    public void createCollection(String path) {
        throw new WebDavException(405, "Read-only store: MKCOL not supported");
    }

    @Override
    public void delete(String path) {
        throw new WebDavException(405, "Read-only store: DELETE not supported");
    }

    @Override
    public void copy(String src, String dest, boolean overwrite, boolean recursive) {
        throw new WebDavException(405, "Read-only store: COPY not supported");
    }

    @Override
    public void move(String src, String dest, boolean overwrite) {
        throw new WebDavException(405, "Read-only store: MOVE not supported");
    }

    @Override
    public void putContent(String path, InputStream content, String contentType, long contentLength) {
        throw new WebDavException(405, "Read-only store: PUT not supported");
    }

    // =========================================================================
    // Core helpers
    // =========================================================================

    /**
     * Lists the direct children (sub-collections + files) of the given virtual folder
     * within a mapping.
     *
     * @param mapping    the table mapping
     * @param davBase    the DAV path prefix up to and including the mapping name (e.g. {@code /sales})
     * @param folderPath the sub-path within the mapping (empty string = mapping root)
     */
    private List<WebDavResource> listChildrenOfFolder(TableMapping mapping,
                                                       String davBase,
                                                       String folderPath) {
        List<Map<String, Object>> rows = fetchAllRows(mapping);

        Set<String> subFolderNames = new LinkedHashSet<>();
        List<WebDavResource> files = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            String rowFolder = resolvePathColumn(mapping, row);
            String rowName   = resolveNameColumn(mapping, row);

            if (rowFolder.equals(folderPath)) {
                // Direct file in this folder
                byte[]  content  = generateContent(mapping, row);
                Instant lastMod  = extractTimestamp(row);
                String  filePath = childPath(davBase, folderPath, rowName + mapping.fileExtension());
                files.add(PostgresqlWebDavResource.file(
                        filePath, content.length, mapping.contentType(), computeETag(content), lastMod));

            } else if (isDirectSubFolder(folderPath, rowFolder)) {
                // Extract next path segment → synthetic sub-collection
                String remaining   = folderPath.isEmpty() ? rowFolder : rowFolder.substring(folderPath.length() + 1);
                String nextSegment = remaining.split("/", 2)[0];
                if (!nextSegment.isBlank()) {
                    subFolderNames.add(nextSegment);
                }
            }
        }

        // Build result: collections first, then files
        List<WebDavResource> result = new ArrayList<>(subFolderNames.size() + files.size());
        for (String seg : subFolderNames) {
            result.add(PostgresqlWebDavResource.collection(childPath(davBase, folderPath, seg)));
        }
        result.addAll(files);
        return result;
    }

    /**
     * Returns {@code true} if {@code rowFolder} is located <em>somewhere</em> below
     * {@code currentFolder} (i.e. is a proper descendant).
     */
    private boolean isDirectSubFolder(String currentFolder, String rowFolder) {
        if (rowFolder.equals(currentFolder)) return false;
        if (currentFolder.isEmpty()) return !rowFolder.isEmpty();
        return rowFolder.startsWith(currentFolder + "/");
    }

    /**
     * Returns the first matching row whose path column equals {@code folder}
     * and whose name column equals {@code fileName}.
     */
    private Map<String, Object> findRow(TableMapping mapping, String folder, String fileName) {
        for (Map<String, Object> row : fetchAllRows(mapping)) {
            if (resolvePathColumn(mapping, row).equals(folder)
                    && resolveNameColumn(mapping, row).equals(fileName)) {
                return row;
            }
        }
        return null;
    }

    /**
     * Returns {@code true} if any row in the mapping has a path column value that
     * equals {@code folderPath} or starts with {@code folderPath/}.
     */
    private boolean folderExistsInMapping(TableMapping mapping, String folderPath) {
        for (Map<String, Object> row : fetchAllRows(mapping)) {
            String rowFolder = resolvePathColumn(mapping, row);
            if (rowFolder.equals(folderPath) || rowFolder.startsWith(folderPath + "/")) {
                return true;
            }
        }
        return false;
    }

    /** Executes the mapping's SQL query and returns all rows. */
    private List<Map<String, Object>> fetchAllRows(TableMapping mapping) {
        return jdbc.queryForList(mapping.buildBaseQuery());
    }

    // =========================================================================
    // Content generation
    // =========================================================================

    /**
     * Generates the file content bytes for a single row.
     * Metadata columns ({@code pathColumn}, {@code nameColumn}) are excluded from the output.
     * Column name comparison is case-insensitive so the store works with databases that
     * return uppercased column names (e.g. H2 in the default compatibility mode).
     */
    private byte[] generateContent(TableMapping mapping, Map<String, Object> row) {
        // Build a copy of the row without the metadata columns (case-insensitive removal)
        Map<String, Object> data = new LinkedHashMap<>(row);
        removeKeyIgnoreCase(data, mapping.getPathColumn());
        removeKeyIgnoreCase(data, mapping.getNameColumn());

        return switch (mapping.getFormat()) {
            case CSV  -> toCsv(data);
            case JSON -> toJson(mapping, row, data);
        };
    }

    /** Removes the entry whose key matches {@code name} (case-insensitively). */
    private void removeKeyIgnoreCase(Map<String, Object> map, String name) {
        if (name == null) return;
        // Fast path: exact match
        if (map.remove(name) != null) return;
        // Slow path: case-insensitive scan
        map.entrySet().removeIf(e -> e.getKey().equalsIgnoreCase(name));
    }

    /** Serialises a row's data columns as RFC 4180 CSV (header line + one data line). */
    private byte[] toCsv(Map<String, Object> data) {
        String header = data.keySet().stream()
                .map(this::csvQuote)
                .collect(Collectors.joining(","));
        String values = data.values().stream()
                .map(v -> csvQuote(v == null ? "" : String.valueOf(v)))
                .collect(Collectors.joining(","));
        return (header + "\r\n" + values + "\r\n").getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Quotes a CSV field per RFC 4180: wrap in double-quotes if the value contains
     * a comma, double-quote, newline, or carriage return; escape internal double-quotes
     * by doubling them.
     */
    private String csvQuote(String value) {
        if (value.contains(",") || value.contains("\"")
                || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /**
     * Serialises a row as JSON.
     * <ul>
     *   <li>If {@link TableMapping#getJsonColumn()} is set: emits the raw value of that column
     *       (handles PostgreSQL {@code PGobject} / {@code jsonb} transparently).</li>
     *   <li>Otherwise: serialises the {@code data} map as a JSON object.</li>
     * </ul>
     */
    private byte[] toJson(TableMapping mapping, Map<String, Object> row, Map<String, Object> data) {
        try {
            if (mapping.getJsonColumn() != null) {
                // Case-insensitive lookup — handles databases that uppercase column names
                Object jsonVal = getValueIgnoreCase(row, mapping.getJsonColumn());
                if (jsonVal == null) return "null".getBytes(StandardCharsets.UTF_8);

                // Handle PostgreSQL PGobject (jsonb) — its toString() returns the JSON string
                String jsonStr = tryExtractPgObjectValue(jsonVal);
                if (jsonStr == null) {
                    jsonStr = jsonVal instanceof String s ? s : objectMapper.writeValueAsString(jsonVal);
                }
                return jsonStr.getBytes(StandardCharsets.UTF_8);
            }
            return objectMapper.writeValueAsBytes(data);
        } catch (Exception e) {
            throw new WebDavException(500, "Failed to serialise row to JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Attempts to extract the JSON string from a PostgreSQL {@code PGobject} without a
     * compile-time dependency on the PostgreSQL driver.
     *
     * @return the JSON string, or {@code null} if {@code value} is not a {@code PGobject}
     */
    private String tryExtractPgObjectValue(Object value) {
        if (value == null) return null;
        if (!"org.postgresql.util.PGobject".equals(value.getClass().getName())) return null;
        try {
            return (String) value.getClass().getMethod("getValue").invoke(value);
        } catch (Exception ignored) {
            return value.toString(); // fallback: toString() also returns the JSON value
        }
    }

    // =========================================================================
    // Path utilities
    // =========================================================================

    /**
     * Normalises a DAV path: ensures leading {@code /}, strips trailing {@code /}
     * (except for the root {@code /} itself), and collapses empty input to {@code /}.
     */
    private String normalizePath(String path) {
        if (path == null || path.isBlank()) return "/";
        String p = path.startsWith("/") ? path : "/" + path;
        return (p.length() > 1 && p.endsWith("/")) ? p.substring(0, p.length() - 1) : p;
    }

    /**
     * Returns the normalised folder path from the row's path column:
     * leading/trailing slashes removed, {@code null} → empty string.
     * Lookup is case-insensitive to support databases that uppercase column names.
     */
    private String resolvePathColumn(TableMapping mapping, Map<String, Object> row) {
        if (mapping.getPathColumn() == null) return "";
        Object raw = getValueIgnoreCase(row, mapping.getPathColumn());
        if (raw == null) return "";
        String val = String.valueOf(raw).trim();
        // Strip surrounding slashes
        while (val.startsWith("/")) val = val.substring(1);
        while (val.endsWith("/"))   val = val.substring(0, val.length() - 1);
        return val;
    }

    /**
     * Returns the raw string value of the name column for the given row.
     * Lookup is case-insensitive.
     */
    private String resolveNameColumn(TableMapping mapping, Map<String, Object> row) {
        Object raw = getValueIgnoreCase(row, mapping.getNameColumn());
        return raw == null ? "" : String.valueOf(raw).trim();
    }

    /**
     * Case-insensitive map lookup: tries exact key first, then iterates entries.
     */
    private Object getValueIgnoreCase(Map<String, Object> row, String columnName) {
        if (columnName == null) return null;
        // Fast path: exact match
        if (row.containsKey(columnName)) return row.get(columnName);
        // Slow path: case-insensitive scan
        for (Map.Entry<String, Object> e : row.entrySet()) {
            if (e.getKey().equalsIgnoreCase(columnName)) return e.getValue();
        }
        return null;
    }

    /**
     * Builds a child DAV path by appending {@code childName} to {@code davBase}
     * under {@code folderPath}.
     *
     * <p>Examples:
     * <ul>
     *   <li>base={@code /sales}, folder={@code ""}, child={@code "summary.csv"}
     *       → {@code /sales/summary.csv}</li>
     *   <li>base={@code /sales}, folder={@code "europe"}, child={@code "revenue.csv"}
     *       → {@code /sales/europe/revenue.csv}</li>
     *   <li>base={@code /sales}, folder={@code "europe"}, child={@code "2024"}  (sub-collection)
     *       → {@code /sales/europe/2024}</li>
     * </ul>
     */
    private String childPath(String davBase, String folderPath, String childName) {
        String base = davBase.endsWith("/") ? davBase : davBase + "/";
        String mid  = folderPath.isEmpty() ? "" : folderPath + "/";
        return base + mid + childName;
    }

    // =========================================================================
    // ETag / timestamp helpers
    // =========================================================================

    /** Computes a hex-encoded MD5 digest of {@code content}. */
    private String computeETag(byte[] content) {
        try {
            byte[] hash = MessageDigest.getInstance("MD5").digest(content);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(Arrays.hashCode(content));
        }
    }

    /**
     * Attempts to extract a {@link java.sql.Timestamp} or {@link java.util.Date} value
     * from a row column named {@code updated_at}, {@code modified_at}, {@code created_at},
     * or {@code timestamp} (first found wins, case-insensitive).
     * Falls back to {@link Instant#EPOCH} when no such column exists.
     */
    private Instant extractTimestamp(Map<String, Object> row) {
        for (String candidate : List.of("updated_at", "modified_at", "created_at", "timestamp")) {
            Object v = getValueIgnoreCase(row, candidate);
            if (v instanceof java.sql.Timestamp ts) return ts.toInstant();
            if (v instanceof java.util.Date d)     return d.toInstant();
        }
        return Instant.EPOCH;
    }

    // =========================================================================
    // Misc
    // =========================================================================

    private TableMapping findMapping(String name) {
        return mappings.stream()
                .filter(m -> m.getName().equals(name))
                .findFirst()
                .orElse(null);
    }
}

