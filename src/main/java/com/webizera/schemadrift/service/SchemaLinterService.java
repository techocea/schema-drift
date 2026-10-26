package com.webizera.schemadrift.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SchemaLinterService {

    @Data
    @AllArgsConstructor
    public static class LintViolation{
            private String tableName;
            private String ruleName;
            private String message;
            private String severity; // CRITICAL, HIGH, MEDIUM, INFO
    }

    public List<LintViolation> lint(List<SchemaInspectorService.TableMetaData> tables){
        List<LintViolation> violations = new ArrayList<>();

        //Every table must have a Primary Key
        for(SchemaInspectorService.TableMetaData table : tables){
            String tName = table.getTableName().toLowerCase();
            if(tName.startsWith("pg_") || tName.startsWith("sql_")) continue;

            //missing primary key
            if(table.getPrimaryKeys().isEmpty()){
                violations.add(new LintViolation(
                        table.getTableName(),
                        "MISSING_PRIMARY_KEY",
                        "Table '" + table.getTableName() + "' has no primary key defined. High risk of data duplication and query inefficiency.",
                        "CRITICAL"
                ));
            }

            boolean hasUpdatedAt = false;

            for(SchemaInspectorService.ColumnMetaData col : table.getColumns()){
                String cName = col.getColumnName().toLowerCase();

                // Track if table has an updated_at or modification timestamp
                if(cName.equals("updated_at") || cName.equals("modified_at")){
                    hasUpdatedAt = true;
                }

               // HIGH: Column ends with '_id' but is not part of primary key or explicit foreign keys
                if(cName.endsWith("_id") && !cName.equals("id") && !table.getPrimaryKeys().contains(col.getColumnName())){
                    violations.add(new LintViolation(
                            table.getTableName(),
                            "UNCONSTRAINED_FK",
                            "Column '" + col.getColumnName() + "' looks like a foreign key but lacks explicit indexing verification.",
                            "HIGH"
                    ));
                }

                // INFO: Suspicious naming convention checks (mixed casing or styles)
                if (cName.contains("__") || (cName.matches(".*[A-Z].*") && cName.matches(".*_.*"))) {
                    violations.add(new LintViolation(
                            table.getTableName(),
                            "SUSPICIOUS_NAMING",
                            "Column '" + col.getColumnName() + "' mixes snake_case and camelCase or contains double underscores.",
                            "INFO"
                    ));
                }

                // INFO: Mutable tables should usually track updated_at timestamps
                if (!tName.contains("log") && !tName.contains("audit") && !tName.contains("historic") && !hasUpdatedAt && table.getColumns().size() > 2) {
                    violations.add(new LintViolation(
                            table.getTableName(),
                            "MISSING_UPDATED_AT",
                            "Table '" + table.getTableName() + "' appears to be a mutable entity table but lacks an 'updated_at' timestamp column.",
                            "INFO"
                    ));
                }
            }
        }
        return violations;
    }
}
