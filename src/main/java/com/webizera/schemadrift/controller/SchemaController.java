package com.webizera.schemadrift.controller;

import com.webizera.schemadrift.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/schema")
@RequiredArgsConstructor
public class SchemaController {

    private final SchemaInspectorService inspectorService;
    private final SchemaLinterService linterService;
    private final DocumentationGeneratorService docService;
    private final SchemaDiffService diffService;
    private final SchemaFixerService fixerService;

    @GetMapping("/inspect")
    public ResponseEntity<List<SchemaInspectorService.TableMetaData>> inspectSchema() {
        List<SchemaInspectorService.TableMetaData> metadata = inspectorService.inspectDatabase();
        return ResponseEntity.ok(metadata);
    }

    @GetMapping("/lint")
    public ResponseEntity<List<SchemaLinterService.LintViolation>> lintSchema() {
        List<SchemaInspectorService.TableMetaData> metadata = inspectorService.inspectDatabase();
        List<SchemaLinterService.LintViolation> violations = linterService.lint(metadata);
        return ResponseEntity.ok(violations);
    }

    @GetMapping(value = "/docs", produces = MediaType.TEXT_MARKDOWN_VALUE)
    public ResponseEntity<String> generateDocs() {
        var metadata = inspectorService.inspectDatabase();
        String markdown = docService.generateMarkdown(metadata);
        return ResponseEntity.ok(markdown);
    }

    @PostMapping("/snapshot")
    public ResponseEntity<String> createSnapshot(){
        try {
            String message = diffService.createSnapshot();
            return ResponseEntity.ok(message);
        }catch(IOException e){
            return ResponseEntity.internalServerError().body("Failed to create snapshot: " + e.getMessage());
        }
    }

    @GetMapping("/diff")
    public ResponseEntity<List<String>> checkDrift() {
        List<String> driftReport = diffService.compareWithBaseline();
        return ResponseEntity.ok(driftReport);
    }

    @GetMapping("/fix-suggestions")
    public ResponseEntity<SchemaFixerService.SchemaFixReport> fixSuggestions() {
        var metadata = inspectorService.inspectDatabase();
        var violations = linterService.lint(metadata);
        SchemaFixerService.SchemaFixReport report = fixerService.generateFixes(violations);
        return ResponseEntity.ok(report);
    }
}
