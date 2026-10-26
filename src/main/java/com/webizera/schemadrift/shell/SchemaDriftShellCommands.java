package com.webizera.schemadrift.shell;

import com.webizera.schemadrift.service.DocumentationGeneratorService;
import com.webizera.schemadrift.service.SchemaFixerService;
import com.webizera.schemadrift.service.SchemaInspectorService;
import com.webizera.schemadrift.service.SchemaLinterService;

import lombok.RequiredArgsConstructor;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.shell.table.ArrayTableModel;
import org.springframework.shell.table.BorderStyle;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.TableModel;

@ShellComponent
@RequiredArgsConstructor
public class SchemaDriftShellCommands {
    private final SchemaInspectorService inspectorService;
    private final SchemaLinterService linterService;
    private final DocumentationGeneratorService docService;
    private final SchemaFixerService fixerService;

    @ShellMethod(key = "inspect", value = "Inspect live database schema and output tables.")
    public AttributedStringOrTable inspect(@ShellOption(value = {"-u", "--url"}, defaultValue = "", help = "JDBC database URL to inspect") String url) {
        var metadata = inspectorService.inspectDatabase();

        if (metadata.isEmpty()) {
            return new AttributedStringOrTable("⚠️ No tables found in database.");
        }

        // Build a clean ASCII table for tables and columns
        String[][] data = new String[metadata.size() + 1][3];
        data[0] = new String[]{"TABLE NAME", "COLUMNS COUNT", "PRIMARY KEYS"};

        for (int i = 0; i < metadata.size(); i++) {
            var t = metadata.get(i);
            data[i + 1] = new String[]{
                    t.getTableName(),
                    String.valueOf(t.getColumns().size()),
                    t.getPrimaryKeys().toString()
            };
        }

        TableModel model = new ArrayTableModel(data);
        TableBuilder tableBuilder = new TableBuilder(model);
        tableBuilder.addFullBorder(BorderStyle.fancy_light);
        return new AttributedStringOrTable(tableBuilder.build().render(100));
    }

    @ShellMethod(key = "lint", value = "Run database linter rule engine to catch anti-patterns.")
    public String lint(@ShellOption(value = {"-u", "--url"}, defaultValue = "", help = "JDBC database URL to lint") String url) {
        var metadata = inspectorService.inspectDatabase();
        var violations = linterService.lint(metadata);

        if (violations.isEmpty()) {
            return "✅ SUCCESS: Zero linter violations found! Your database schema is clean.";
        }

        StringBuilder sb = new StringBuilder("\n❌ LINTER VIOLATIONS DETECTED:\n");
        sb.append("--------------------------------------------------------------------------------\n");
        for (var v : violations) {
            sb.append(String.format(" [%s] %s -> %s\n   Reason: %s\n\n",
                    v.getSeverity(), v.getTableName(), v.getRuleName(), v.getMessage()));
        }
        sb.append("--------------------------------------------------------------------------------\n");
        return sb.toString();
    }

    @ShellMethod(key = "diagram", value = "Generate Markdown ERD and Data Dictionary.")
    public String diagram(
            @ShellOption(defaultValue = "") String file
    ) {
        var metadata = inspectorService.inspectDatabase();
        String markdown = docService.generateMarkdown(metadata);

        // If a file name is provided, write directly to disk
        if (!file.isEmpty()) {
            try {
                java.nio.file.Files.writeString(java.nio.file.Path.of(file), markdown);
                return "✅ Successfully exported documentation and ERD to: " + file;
            } catch (java.io.IOException e) {
                return "❌ Error: Failed to write file - " + e.getMessage();
            }
        }

        // Otherwise, print to console
        return markdown;
    }

    @ShellMethod(key = "fixes", value = "Generate structured SQL fix plans for violations.")
    public String fixes() {
        var metadata = inspectorService.inspectDatabase();
        var violations = linterService.lint(metadata);
        var report = fixerService.generateFixes(violations);

        if (report.plans().isEmpty()) {
            return "✅ No fixes needed.";
        }

        StringBuilder sb = new StringBuilder("\n🛠️ SCHEMAFIX REMEDIATION PLANS:\n");
        for (var plan : report.plans()) {
            sb.append("--------------------------------------------------------------------------------\n");
            sb.append(String.format("ID: %s | Rule: %s | Severity: %s | Safety: %s\n",
                    plan.violationId(), plan.rule(), plan.severity(), plan.safety()));
            sb.append("Title: ").append(plan.title()).append("\n\n");
            sb.append("Execution Steps:\n");
            for (var step : plan.steps()) {
                sb.append(String.format("  Step %d: %s\n  SQL: %s\n", step.order(), step.description(), step.sql()));
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    // Helper wrapper to let Spring Shell return rendered tables
    public static class AttributedStringOrTable {
        private final String renderedTable;

        public AttributedStringOrTable(String renderedTable) {
            this.renderedTable = renderedTable;
        }

        @Override
        public String toString() {
            return "\n" + renderedTable + "\n";
        }
    }
}
