/*
 * Copyright 2026 Lars Wilhelmsen <lars@lars-backwards.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.larsw.webdav.postgresql;

/**
 * Configures a single virtual directory tree backed by a PostgreSQL table (or arbitrary SQL query).
 *
 * <h2>Virtual file-system layout</h2>
 * <pre>
 * /&lt;name&gt;/                                  ← mapping root collection
 *   &lt;pathColumn-value&gt;/                      ← zero or more nested folder segments
 *     &lt;nameColumn-value&gt;.csv (.json)         ← one file per row
 * </pre>
 *
 * <h2>Column conventions</h2>
 * <ul>
 *   <li>{@link #pathColumn} — virtual folder path for this row, e.g. {@code "europe/2024/Q1"}.
 *       May be {@code null} or empty to place the file at the mapping root.</li>
 *   <li>{@link #nameColumn} — virtual filename <em>without</em> extension, e.g. {@code "revenue"}.
 *       Must be unique within the same folder.</li>
 * </ul>
 *
 * <h2>Formats</h2>
 * <ul>
 *   <li>{@code CSV} — all non-metadata columns serialised as RFC 4180 CSV (header + one data row).</li>
 *   <li>{@code JSON} — all non-metadata columns serialised as a JSON object, or a single
 *       {@link #jsonColumn} emitted as-is when the column holds a {@code json}/{@code jsonb} value.</li>
 * </ul>
 */
public class TableMapping {

    /** Logical name; becomes the root collection segment directly under {@code /}. */
    private String name;

    /**
     * Table or view to {@code SELECT * FROM}.
     * Ignored when {@link #query} is also set.
     */
    private String table;

    /**
     * Custom SQL query that returns the rows to expose.
     * Overrides {@link #table} when set.
     * <p><strong>Important:</strong> the query must always return all columns
     * including {@link #pathColumn} and {@link #nameColumn}.</p>
     */
    private String query;

    /**
     * Name of the column whose value is the virtual folder path.
     * Forward slashes ({@code /}) create nested collections.
     * {@code null} or blank means all rows live at the mapping root.
     */
    private String pathColumn;

    /**
     * Name of the column whose value is the virtual filename (no extension).
     * Must not be {@code null}.
     */
    private String nameColumn;

    /** Serialisation format — determines the file extension and {@code Content-Type}. */
    private Format format = Format.CSV;

    /**
     * For {@link Format#JSON} only: name of the {@code json}/{@code jsonb} column
     * whose raw value is emitted as the file content.
     * When {@code null} (the default) all non-metadata columns are serialised as a JSON object.
     */
    private String jsonColumn;

    // ---- Enums ---------------------------------------------------------------

    public enum Format { CSV, JSON }

    // ---- Derived helpers -----------------------------------------------------

    /**
     * Returns the SQL used to fetch all rows for this mapping.
     * Callers are responsible for not using this with untrusted input.
     */
    public String buildBaseQuery() {
        if (query != null && !query.isBlank()) {
            return query;
        }
        if (table == null || table.isBlank()) {
            throw new IllegalStateException("TableMapping '" + name + "': either 'table' or 'query' must be set");
        }
        return "SELECT * FROM " + table;
    }

    /** File extension (with leading dot) for this mapping's format. */
    public String fileExtension() {
        return "." + format.name().toLowerCase();
    }

    /** HTTP {@code Content-Type} header value for this mapping's format. */
    public String contentType() {
        return switch (format) {
            case CSV  -> "text/csv; charset=UTF-8";
            case JSON -> "application/json; charset=UTF-8";
        };
    }

    // ---- Getters / setters ---------------------------------------------------

    public String getName()                  { return name; }
    public void setName(String name)         { this.name = name; }

    public String getTable()                 { return table; }
    public void setTable(String table)       { this.table = table; }

    public String getQuery()                 { return query; }
    public void setQuery(String query)       { this.query = query; }

    public String getPathColumn()            { return pathColumn; }
    public void setPathColumn(String pc)     { this.pathColumn = pc; }

    public String getNameColumn()            { return nameColumn; }
    public void setNameColumn(String nc)     { this.nameColumn = nc; }

    public Format getFormat()                { return format; }
    public void setFormat(Format format)     { this.format = format; }

    public String getJsonColumn()            { return jsonColumn; }
    public void setJsonColumn(String jc)     { this.jsonColumn = jc; }
}

