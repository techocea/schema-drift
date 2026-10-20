# 🛡️ SchemaDrift

> **An automated database migration sentinel, CI/CD linter, and living documentation engine for modern engineering teams.**

[![Java 17](https://img.shields.io/badge/Java-17%2B-orange?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%25Boot-3.2.5-brightgreen?style=flat-square&logo=springboot)](https://spring.io/)
[![Testcontainers](https://img.shields.io/badge/Testcontainers-JUnit5-blue?style=flat-square&logo=docker)](https://www.testcontainers.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

---

## 📖 1. The Scenario & Problem Statement

In modern fast-paced software development, application code goes through rigorous code reviews, automated unit tests, and style linting. Yet, **database migrations are often treated like the Wild West.**

### The Production Nightmare
It’s Friday afternoon. A feature branch is merged, and your CI/CD pipeline executes a database migration via Flyway or Liquibase. Suddenly—**production locks up or crashes**.

Why? Because a developer added a foreign key constraint or a `NOT NULL` column without a default value on a table with 5 million rows, causing a heavy table lock and silent deadlock.

### Why SchemaDrift?
Reviewers spot bugs in application code easily, but spotting subtle database performance bottlenecks, missing primary keys, or unindexed foreign keys in raw SQL migration scripts is tedious and prone to human error.

**SchemaDrift** solves this by acting as an automated database sentinel. It intercepts migrations before they touch staging or production, runs them inside an isolated container to verify structural integrity, lints them against database anti-patterns, and auto-generates living architectural documentation.

---

## 🏗️ 2. System Architecture & Data Flow

SchemaDrift connects directly to your database connection pool, inspects relational metadata via Java's JDBC API, evaluates architectural rules, and outputs clean developer insights.

```text
 ┌────────────────────────┐
 │ SQL Migration Scripts  │
 │ (Flyway / Liquibase)   │
 └───────────┬────────────┘
             │
             ▼
 ┌────────────────────────┐      JDBC Metadata API      ┌───────────────────────┐
 │ Ephemeral Testcontainer│ ──────────────────────────> │ SchemaInspectorService│
 │ (PostgreSQL / MySQL)   │                             └───────────┬───────────┘
 └────────────────────────┘                                         │
                                                            [Parsed Table Models]
                                                                    │
                                            ┌───────────────────────┴───────────────────────┐
                                            ▼                                               ▼
                                 ┌─────────────────────┐                         ┌──────────────────────────┐
                                 │ SchemaLinterService │                         │ DocumentationGenerator   │
                                 │ (Anti-Pattern Rules)│                         │ (Markdown & Mermaid ERD) │
                                 └─────────────────────┘                         └──────────────────────────┘