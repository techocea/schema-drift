package com.webizera.schemadrift.service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentationGeneratorService {

    public String generateMarkdown(List<SchemaInspectorService.TableMetaData> tables) {
        StringBuilder sb = new StringBuilder();

        sb.append("# 📊 Auto-Generated Database Architecture\n\n");
        sb.append("> *This document is automatically generated and updated by **SchemaDrift**.*\n\n");

        // Generate Mermaid ER Diagram
        sb.append("## Entity Relationship Diagram\n\n");
        sb.append("```mermaid\nerDiagram\n");

        for (SchemaInspectorService.TableMetaData table : tables) {
            sb.append("    ").append(table.getTableName()).append(" {\n");
            for (SchemaInspectorService.ColumnMetaData col : table.getColumns()) {
                sb.append("        ").append(col.getDataType()).append(" ").append(col.getColumnName()).append("\n");
            }
            sb.append("    }\n");
        }
        sb.append("```\n\n");

        // Generate Data Dictionary Tables
        sb.append("## Data Dictionary\n\n");

        for (SchemaInspectorService.TableMetaData table : tables) {
            sb.append("### Table: `").append(table.getTableName()).append("`\n\n");
            sb.append("| Column | Type | Nullable | Primary Key |\n");
            sb.append("| :--- | :--- | :---: | :---: |\n");

            for (SchemaInspectorService.ColumnMetaData col : table.getColumns()) {
                boolean isPk = table.getPrimaryKeys().contains(col.getColumnName());
                sb.append("| `").append(col.getColumnName())
                        .append("` | ").append(col.getDataType())
                        .append(" | ").append(col.isNullable() ? "YES" : "NO")
                        .append(" | ").append(isPk ? "🔑" : "-")
                        .append(" |\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}