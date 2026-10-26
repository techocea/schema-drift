package com.webizera.schemadrift.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class SchemaDiffService {
    private final SchemaInspectorService inspectorService;
    private final ObjectMapper objectMapper;
    private static final String SNAPSHOT_FILE = "schemadrift-baseline.json";

    // Save live schema as a baseline JSON snapshot file
    public String createSnapshot() throws IOException {
        List<SchemaInspectorService.TableMetaData> liveSchema = inspectorService.inspectDatabase();
        File file = new File(SNAPSHOT_FILE);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, liveSchema);
        return "Snapshot successfully created and saved to " + file.getAbsolutePath();
    }

    //Compare live schema against the baseline snapshot to detect drift
    public List<String> compareWithBaseline() {
        List<String> driftReport = new ArrayList<>();
        File file = new File(SNAPSHOT_FILE);

        if (!file.exists()) {
            return List.of("ERROR: No baseline snapshot found. Please create a snapshot first using POST /api/schema/snapshot.");
        }

        // load baseline
        SchemaInspectorService.TableMetaData[] baselineArray = objectMapper.readValue(file, SchemaInspectorService.TableMetaData[].class);
        Map<String, SchemaInspectorService.TableMetaData> baselineMap = Stream.of(baselineArray)
                .collect(Collectors.toMap(SchemaInspectorService.TableMetaData::getTableName, t -> t));

        // Get live schema
        List<SchemaInspectorService.TableMetaData> liveSchema = inspectorService.inspectDatabase();
        Map<String, SchemaInspectorService.TableMetaData> liveMap = liveSchema.stream()
                .collect(Collectors.toMap(SchemaInspectorService.TableMetaData::getTableName, t -> t));

        // Check for deleted or altered tables
        for(String baseTable : baselineMap.keySet()){
            if(!liveMap.containsKey(baseTable)){
                driftReport.add("DRIFT DETECTED: Table '" + baseTable + "' was DROPPED or missing in live database!\n");
            }
        }

        // Check for added tables or column shifts
        for (String liveTable : liveMap.keySet()) {
            if (!baselineMap.containsKey(liveTable)) {
                driftReport.add("DRIFT DETECTED: New table '" + liveTable + "' was added in live database.\n");
            } else {
                // Compare column counts
                int baseCols = baselineMap.get(liveTable).getColumns().size();
                int liveCols = liveMap.get(liveTable).getColumns().size();
                if (baseCols != liveCols) {
                    driftReport.add("DRIFT DETECTED: Table '" + liveTable + "' column count changed from " + baseCols + " to " + liveCols + ".\n");
                }
            }
        }

        if (driftReport.isEmpty()) {
            driftReport.add("SUCCESS: Zero schema drift detected. Live database matches baseline snapshot.\n");
        }
        return driftReport;
    }
}

