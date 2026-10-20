package com.webizera.schemadrift.controller;

import com.webizera.schemadrift.service.SchemaInspectorService;
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

    @GetMapping("/inspect")
    public ResponseEntity<List<SchemaInspectorService.TableMetaData>> inspectSchema() {
        List<SchemaInspectorService.TableMetaData> metaData = inspectorService.inspectDatabase();
        return ResponseEntity.ok(metaData);
    }

}
