package com.webizera.schemadrift;

import com.webizera.schemadrift.service.SchemaFixerService;
import com.webizera.schemadrift.service.SchemaInspectorService;
import com.webizera.schemadrift.service.SchemaLinterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
public class SchemaDrfitV1IntegrationTest {
    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SchemaInspectorService inspectorService;

    @Autowired
    private SchemaLinterService linterService;

    @Autowired
    private SchemaFixerService fixerService;

    @BeforeEach
    void setUp() {
        // Clean the test tables before each test
        jdbcTemplate.execute("DROP TABLE IF EXISTS test_table CASCADE;");
    }

    @Test
    void testInspectorDetectsCreatedTable() {
        //ARRANGE - create a table without primary key
        jdbcTemplate.execute("CREATE TABLE test_table (name VARCHAR(50));");

        //ACT - inspect the database
        List<SchemaInspectorService.TableMetaData> metadata = inspectorService.inspectDatabase();

        //ASSERT - verify table and column were picked up
        boolean found = metadata.stream().anyMatch(t -> t.getTableName().equals("test_table"));
        assertTrue(found, "Inspector should detect 'test_table'");
    }

    @Test
    void testLinterFlagMissingPrimaryKey() {
        //ARRANGE - create a table without primary key
        jdbcTemplate.execute("CREATE TABLE test_table (data TEXT);");

        //ACT
        var metadata = inspectorService.inspectDatabase();
        var violations = linterService.lint(metadata);

        //ASSERT - linter should catch missing primary key
        boolean hasCriticalViolation = violations
                .stream()
                .anyMatch(v -> v.getRuleName().equals("MISSING_PRIMARY_KEY") && v.getSeverity().equals("CRITICAL"));

        assertTrue(hasCriticalViolation, "Linter should flag missing primary key as CRITICAL");
    }

    @Test
    void testFixerGeneratesStructuredPlan() {
        // Arrange: Table with missing PK
        jdbcTemplate.execute("CREATE TABLE test_table (data TEXT);");

        // Act
        var metadata = inspectorService.inspectDatabase();
        var violations = linterService.lint(metadata);
        SchemaFixerService.SchemaFixReport report = fixerService.generateFixes(violations);

        // Assert: Verify fix plan contains valid SQL step
        assertFalse(report.plans().isEmpty(), "Fix report should contain plans");
        assertEquals("MISSING_PRIMARY_KEY", report.plans().get(0).rule());
        assertFalse(report.plans().get(0).steps().isEmpty(), "Fix plan should have execution steps");
    }
}
