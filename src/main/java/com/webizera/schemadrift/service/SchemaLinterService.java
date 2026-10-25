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
            private String severity; //ERROR, WARNING
    }

    public List<LintViolation> lint(List<SchemaInspectorService.TableMetaData> tables){
        List<LintViolation> violations = new ArrayList<>();

        //Every table must have a Primary Key
        for(SchemaInspectorService.TableMetaData table : tables){
            if(table.getPrimaryKeys().isEmpty()){
                violations.add(new LintViolation(
                        table.getTableName(),
                        "MISSING_PRIMARY_KEY",
                        "Table '" + table.getTableName() + "' has no primary key defined. High risk of data duplication and query inefficiency.",
                        "ERROR"
                ));
            }

            //check for potential unindexed foreign key columns (naming convention heuristic)
            for(SchemaInspectorService.ColumnMetaData col : table.getColumns()){
                String colName = col.getColumnName().toLowerCase();
                if(colName.endsWith("_id") && !colName.equals("id") && !table.getPrimaryKeys().contains(col.getColumnName())){
                    violations.add(new LintViolation(
                            table.getTableName(),
                            "POTENTIAL_UNINDEXED_FK",
                            "Column '" + col.getColumnName() + "' looks like a foreign key but lacks explicit indexing verification.",
                            "WARNING"
                    ));
                }
            }
        }
        return violations;
    }
}
