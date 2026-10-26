package com.webizera.schemadrift.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SchemaInspectorService {

    private final DataSource dataSource;

    @Data
    public static class TableMetaData{
        private String tableName;
        private List<ColumnMetaData> columns = new ArrayList<>();
        private List<String> primaryKeys = new ArrayList<>();
    }

    @Data
    public static class ColumnMetaData{
        private String columnName;
        private String dataType;
        private boolean isNullable;
    }

    public List<TableMetaData> inspectDatabase() {
        Map<String, TableMetaData> tablesMap = new LinkedHashMap<>();

        try (Connection connection = dataSource.getConnection()){
            DatabaseMetaData metadata = connection.getMetaData();

            // Get Tables
            try (ResultSet rs = metadata.getTables(null, "public", "%", new String[]{"TABLE"})) {
                while(rs.next()){
                    String tableName = rs.getString("TABLE_NAME");
                    TableMetaData table = new TableMetaData();
                    table.setTableName(tableName);
                    tablesMap.put(tableName, table);
                }
            }

            // Get Columns for each table
            for (String tableName : tablesMap.keySet()) {
                TableMetaData table = tablesMap.get(tableName);

                try(ResultSet rs = metadata.getColumns(null,"public", tableName,"%")){
                    while(rs.next()){
                        ColumnMetaData column = new ColumnMetaData();
                        column.setColumnName(rs.getString("COLUMN_NAME"));
                        column.setDataType(rs.getString("TYPE_NAME"));
                        column.setNullable("YES".equalsIgnoreCase(rs.getString("IS_NULLABLE")));
                        table.getColumns().add(column);
                    }
                }

                try(ResultSet rs = metadata.getPrimaryKeys(null,"public", tableName)){
                    while(rs.next()){
                        table.getPrimaryKeys().add(rs.getString("COLUMN_NAME"));
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to inspect database schema", e);
        }

        return new ArrayList<>(tablesMap.values());
    }
}
