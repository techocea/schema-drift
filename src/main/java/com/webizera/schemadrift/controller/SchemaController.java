package com.webizera.schemadrift.controller;

import com.webizera.schemadrift.service.SchemaInspectorService;
import com.webizera.schemadrift.service.SchemaLinterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/schema")
@RequiredArgsConstructor
public class SchemaController {

    private final SchemaInspectorService inspectorService;
    private final SchemaLinterService linterService;

    @GetMapping("/inspect")
    public ResponseEntity<List<SchemaInspectorService.TableMetaData>> inspectSchema() {
        List<SchemaInspectorService.TableMetaData> metaData = inspectorService.inspectDatabase();
        return ResponseEntity.ok(metaData);
    }

    @GetMapping("/lint")
    public ResponseEntity<List<SchemaLinterService.LintViolation>> lintSchema() {
        List<SchemaInspectorService.TableMetaData> metadata = inspectorService.inspectDatabase();

        List<SchemaLinterService.LintViolation> violations = linterService.lint(metadata);

        return ResponseEntity.ok(violations);
    }
}
