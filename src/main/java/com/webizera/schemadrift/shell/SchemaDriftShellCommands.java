package com.webizera.schemadrift.shell;

import com.webizera.schemadrift.service.DocumentationGeneratorService;
import com.webizera.schemadrift.service.SchemaFixerService;
import com.webizera.schemadrift.service.SchemaInspectorService;
import com.webizera.schemadrift.service.SchemaLinterService;

import lombok.RequiredArgsConstructor;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

@ShellComponent
@RequiredArgsConstructor
public class SchemaDriftShellCommands {
    private final SchemaInspectorService inspectorService;
    private final SchemaLinterService linterService;
    private final DocumentationGeneratorService docService;
    private final SchemaFixerService fixerService;

    @ShellMethod(key = "inspect", value = "Inspect live database schema and output tables/columns.")
    public String inspect() {
        var metadata = inspectorService.inspectDatabase();
        return ">>> Inspected " + metadata.size() + " tables successfully.\n" + metadata.toString();
    }

    @ShellMethod(key = "lint", value = "Run database linter rule engine to catch anti-patterns.")
    public String lint() {
        var metadata = inspectorService.inspectDatabase();
        var violations = linterService.lint(metadata);

        if (violations.isEmpty()) {
            return "SUCCESS: Zero linter violations found!";
        }

        StringBuilder sb = new StringBuilder("LINTER VIOLATIONS DETECTED:\n");
        for (var v : violations) {
            sb.append(" - [").append(v.getSeverity()).append("] ").append(v.getTableName()).append(": ").append(v.getMessage()).append("\n");
        }
        return sb.toString();
    }

    @ShellMethod(key = "diagram", value = "Generate Markdown ERD and Data Dictionary.")
    public String diagram() {
        var metadata = inspectorService.inspectDatabase();
        return docService.generateMarkdown(metadata);
    }

    @ShellMethod(key = "fixes", value = "Generate structured SQL fix plans for violations.")
    public String fixes() {
        var metadata = inspectorService.inspectDatabase();
        var violations = linterService.lint(metadata);
        var report = fixerService.generateFixes(violations);

        return ">>> Generated " + report.plans().size() + " remediation fix plans.\n" + report.toString();
    }
}
