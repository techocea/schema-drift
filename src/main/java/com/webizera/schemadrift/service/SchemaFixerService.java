package com.webizera.schemadrift.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class SchemaFixerService {
    public enum Severity {
        CRITICAL, HIGH, MEDIUM, INFO
    }

    public enum FixSafety {
        SAFE,
        REVIEW_REQUIRED,
        MANUAL_ONLY
    }

    public record FixStep(
            int order,
            String description,
            String sql,
            boolean optional,
            String rollbackHint
    ) {}

    public record FixPlan(
            String violationId,
            String rule,
            Severity severity,
            FixSafety safety,
            String title,
            String explanation,
            List<FixStep> steps,
            List<String> assumptions,
            List<String> manualChecks
    ) {}

    public record SchemaFixReport(
            List<FixPlan> plans
    ) {}

    public SchemaFixReport generateFixes(List<SchemaLinterService.LintViolation> violations){
        List<FixPlan> plans = violations.stream()
                .map(this::buildPlan)
                .toList();

        return new SchemaFixReport(plans);
    }

    private FixPlan buildPlan(SchemaLinterService.LintViolation v){
        Severity severity = parseSeverity(v.getSeverity());

        return switch (v.getRuleName()) {
            case "UNCONSTRAINED_FK" -> unconstrainedFk(v, severity);
            case "MISSING_INDEX_ON_FK" -> missingFkIndex(v, severity);
            case "MISSING_PRIMARY_KEY" -> missingPrimaryKey(v, severity);
            case "MISSING_UPDATED_AT" -> missingUpdatedAt(v, severity);
            default -> manualReview(v, severity);
        };
    }

    private Severity parseSeverity(String sevStr){
        try {
            return Severity.valueOf(sevStr);
        }catch (Exception e){
            return Severity.MEDIUM;
        }
    }

    // Rule specific plan builders
    private FixPlan unconstrainedFk(SchemaLinterService.LintViolation v, Severity severity){
        String violationId = "v-" + UUID.randomUUID().toString().substring(0,8);
        return new FixPlan(
                violationId,
                "UNCONSTRAINED_FK",
                severity,
                FixSafety.REVIEW_REQUIRED,
                v.getTableName() + " has unconstrained foreign key column",
                v.getMessage(),
                List.of(
                        new FixStep(
                                1,
                                "Check for orphan rows that violate integrity",
                                "SELECT o.id FROM " + v.getTableName() + " o LEFT JOIN customers c ON c.id = o." + extractColumnName(v.getMessage()) + " WHERE o." + extractColumnName(v.getMessage()) + " IS NOT NULL AND c.id IS NULL;",
                                false,
                                "None (read-only query)"
                        ),
                        new FixStep(
                                2,
                                "Add foreign-key constraint as NOT VALID to avoid table locks",
                                "ALTER TABLE " + v.getTableName() + " ADD CONSTRAINT fk_" + v.getTableName() + "_ref FOREIGN KEY (" + extractColumnName(v.getMessage()) + ") REFERENCES customers(id) NOT VALID;",
                                false,
                                "ALTER TABLE " + v.getTableName() + " DROP CONSTRAINT fk_" + v.getTableName() + "_ref;"
                        ),
                        new FixStep(
                                3,
                                "Validate existing rows safely without blocking writes",
                                "ALTER TABLE " + v.getTableName() + " VALIDATE CONSTRAINT fk_" + v.getTableName() + "_ref;",
                                false,
                                "None"
                        )
                ),
                List.of("Target reference table is 'customers(id)'", "Table has write traffic that requires non-blocking locks"),
                List.of("Verify no orphan rows exist before running step 3", "Confirm ON DELETE behavior requirements")
        );
    }

    private FixPlan missingFkIndex(SchemaLinterService.LintViolation v, Severity severity) {
        String violationId = "v-" + UUID.randomUUID().toString().substring(0, 8);
        String colName = extractColumnName(v.getMessage());
        return new FixPlan(
                violationId,
                "MISSING_INDEX_ON_FK",
                severity,
                FixSafety.SAFE,
                v.getTableName() + "." + colName + " lacks an index",
                v.getMessage(),
                List.of(
                        new FixStep(
                                1,
                                "Create index concurrently to prevent locking table writes",
                                "CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_" + v.getTableName() + "_" + colName + " ON " + v.getTableName() + " (" + colName + ");",
                                false,
                                "DROP INDEX CONCURRENTLY IF EXISTS idx_" + v.getTableName() + "_" + colName + ";"
                        )
                ),
                List.of("Database supports concurrent index creation"),
                List.of("Ensure sufficient disk space for index build")
        );
    }

    private FixPlan missingPrimaryKey(SchemaLinterService.LintViolation v, Severity severity) {
        String violationId = "v-" + UUID.randomUUID().toString().substring(0, 8);
        return new FixPlan(
                violationId,
                "MISSING_PRIMARY_KEY",
                severity,
                FixSafety.REVIEW_REQUIRED,
                v.getTableName() + " is missing a Primary Key",
                v.getMessage(),
                List.of(
                        new FixStep(
                                1,
                                "Add a standard auto-incrementing primary key ID",
                                "ALTER TABLE " + v.getTableName() + " ADD COLUMN id SERIAL PRIMARY KEY;",
                                false,
                                "ALTER TABLE " + v.getTableName() + " DROP COLUMN id;"
                        )
                ),
                List.of("Table does not have concurrent unique row identifiers"),
                List.of("Check if duplicate rows exist that would prevent adding a primary key constraint")
        );
    }

    private FixPlan missingUpdatedAt(SchemaLinterService.LintViolation v, Severity severity) {
        String violationId = "v-" + UUID.randomUUID().toString().substring(0, 8);
        return new FixPlan(
                violationId,
                "MISSING_UPDATED_AT",
                severity,
                FixSafety.SAFE,
                v.getTableName() + " lacks a modification timestamp",
                v.getMessage(),
                List.of(
                        new FixStep(
                                1,
                                "Add updated_at column with default timestamp",
                                "ALTER TABLE " + v.getTableName() + " ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;",
                                false,
                                "ALTER TABLE " + v.getTableName() + " DROP COLUMN updated_at;"
                        )
                ),
                List.of("Entity is mutable and benefits from change tracking"),
                List.of("Verify application triggers or ORMs handle updated_at mapping")
        );
    }

    private FixPlan manualReview(SchemaLinterService.LintViolation v, Severity severity) {
        String violationId = "v-" + UUID.randomUUID().toString().substring(0, 8);
        return new FixPlan(
                violationId,
                v.getRuleName(),
                severity,
                FixSafety.MANUAL_ONLY,
                "Manual review required for " + v.getTableName(),
                v.getMessage(),
                List.of(),
                List.of("Requires architect evaluation"),
                List.of("Inspect table definition manually and apply custom migration script")
        );
    }

    private String extractColumnName(String message) {
        // Simple heuristic extraction from message string quotes if present
        if (message.contains("'")) {
            int start = message.indexOf("'") + 1;
            int end = message.indexOf("'", start);
            if (end > start) {
                return message.substring(start, end);
            }
        }
        return "column_id";
    }
}
