@echo off
REM ============================================================================
REM PostgreSQL Schema Dump Script (Windows Batch)
REM ============================================================================
REM This script exports the complete database schema using pg_dump
REM ============================================================================

SETLOCAL EnableDelayedExpansion

REM Configuration
SET DB_USER=postgres
SET DB_NAME=madhouse
SET OUTPUT_DIR=schema-dumps
SET TIMESTAMP=%date:~-4%%date:~3,2%%date:~0,2%_%time:~0,2%%time:~3,2%%time:~6,2%
SET TIMESTAMP=%TIMESTAMP: =0%

REM Create output directory
IF NOT EXIST "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"

echo ============================================================================
echo PostgreSQL Schema Dump - MyHab Database
echo ============================================================================
echo.

REM ============================================================================
REM 1. Complete Schema (Structure only, no data)
REM ============================================================================

echo [1/6] Dumping complete schema...
pg_dump -U %DB_USER% -d %DB_NAME% --schema-only --no-owner --no-acl --no-privileges -f "%OUTPUT_DIR%\schema_full_%TIMESTAMP%.sql"

IF %ERRORLEVEL% EQU 0 (
    echo [OK] Complete schema saved to: %OUTPUT_DIR%\schema_full_%TIMESTAMP%.sql
) ELSE (
    echo [FAIL] Failed to dump complete schema
    exit /b 1
)
echo.

REM ============================================================================
REM 2. Table Definitions Only
REM ============================================================================

echo [2/6] Dumping table definitions only...
pg_dump -U %DB_USER% -d %DB_NAME% --schema-only --no-owner --no-acl --section=pre-data -f "%OUTPUT_DIR%\schema_tables_%TIMESTAMP%.sql"

IF %ERRORLEVEL% EQU 0 (
    echo [OK] Table definitions saved to: %OUTPUT_DIR%\schema_tables_%TIMESTAMP%.sql
) ELSE (
    echo [FAIL] Failed to dump table definitions
)
echo.

REM ============================================================================
REM 3. Key Tables Only (Device-related)
REM ============================================================================

echo [3/6] Dumping device-related tables...
pg_dump -U %DB_USER% -d %DB_NAME% --schema-only --no-owner --no-acl -t device_categories -t device_controllers -t device_ports -t configurations -t device_peripherals -t port_values -f "%OUTPUT_DIR%\schema_device_tables_%TIMESTAMP%.sql"

IF %ERRORLEVEL% EQU 0 (
    echo [OK] Device tables saved to: %OUTPUT_DIR%\schema_device_tables_%TIMESTAMP%.sql
) ELSE (
    echo [FAIL] Failed to dump device tables
)
echo.

REM ============================================================================
REM 4. Table List
REM ============================================================================

echo [4/6] Generating table list...
psql -U %DB_USER% -d %DB_NAME% -t -c "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE' ORDER BY table_name;" > "%OUTPUT_DIR%\table_list_%TIMESTAMP%.txt"

IF %ERRORLEVEL% EQU 0 (
    echo [OK] Table list saved to: %OUTPUT_DIR%\table_list_%TIMESTAMP%.txt
) ELSE (
    echo [FAIL] Failed to generate table list
)
echo.

REM ============================================================================
REM 5. Database Structure Analysis
REM ============================================================================

echo [5/6] Extracting database structure...
psql -U %DB_USER% -d %DB_NAME% -f dump-database-structure.sql > "%OUTPUT_DIR%\database_structure_%TIMESTAMP%.txt" 2>&1

IF %ERRORLEVEL% EQU 0 (
    echo [OK] Database structure saved to: %OUTPUT_DIR%\database_structure_%TIMESTAMP%.txt
) ELSE (
    echo [FAIL] Failed to extract database structure
)
echo.

REM ============================================================================
REM 6. Quick Reference
REM ============================================================================

echo [6/6] Generating quick reference...
psql -U %DB_USER% -d %DB_NAME% -c "SELECT table_name, column_name, data_type, character_maximum_length, is_nullable, column_default FROM information_schema.columns WHERE table_schema = 'public' ORDER BY table_name, ordinal_position;" > "%OUTPUT_DIR%\columns_reference_%TIMESTAMP%.txt"

IF %ERRORLEVEL% EQU 0 (
    echo [OK] Columns reference saved to: %OUTPUT_DIR%\columns_reference_%TIMESTAMP%.txt
) ELSE (
    echo [FAIL] Failed to generate columns reference
)
echo.

REM ============================================================================
REM Summary
REM ============================================================================

echo ============================================================================
echo Schema Dump Complete!
echo ============================================================================
echo.
echo Output files in: %OUTPUT_DIR%\
echo.
echo Files created:
echo   1. schema_full_%TIMESTAMP%.sql - Complete schema
echo   2. schema_tables_%TIMESTAMP%.sql - Table definitions only
echo   3. schema_device_tables_%TIMESTAMP%.sql - Device-related tables
echo   4. table_list_%TIMESTAMP%.txt - List of all tables
echo   5. database_structure_%TIMESTAMP%.txt - Detailed structure
echo   6. columns_reference_%TIMESTAMP%.txt - Column details
echo.
echo To view files:
echo   cd %OUTPUT_DIR%
echo   notepad schema_full_%TIMESTAMP%.sql
echo.

ENDLOCAL

