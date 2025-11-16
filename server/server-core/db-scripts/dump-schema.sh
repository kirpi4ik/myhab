#!/bin/bash
# ============================================================================
# PostgreSQL Schema Dump Script (Shell wrapper for pg_dump)
# ============================================================================
# This script exports the complete database schema using pg_dump
# ============================================================================

# Configuration
DB_USER="${DB_USER:-postgres}"
DB_NAME="${DB_NAME:-myhab}"
OUTPUT_DIR="./schema-dumps"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Create output directory
mkdir -p "$OUTPUT_DIR"

echo -e "${BLUE}============================================================================${NC}"
echo -e "${BLUE}PostgreSQL Schema Dump - MyHab Database${NC}"
echo -e "${BLUE}============================================================================${NC}"
echo ""

# ============================================================================
# 1. Complete Schema (Structure only, no data)
# ============================================================================

echo -e "${YELLOW}[1/6] Dumping complete schema...${NC}"
pg_dump -U "$DB_USER" -d "$DB_NAME" \
    --schema-only \
    --no-owner \
    --no-acl \
    --no-privileges \
    -f "$OUTPUT_DIR/schema_full_${TIMESTAMP}.sql"

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Complete schema saved to: $OUTPUT_DIR/schema_full_${TIMESTAMP}.sql${NC}"
else
    echo "✗ Failed to dump complete schema"
    exit 1
fi
echo ""

# ============================================================================
# 2. Table Definitions Only (no constraints, indexes)
# ============================================================================

echo -e "${YELLOW}[2/6] Dumping table definitions only...${NC}"
pg_dump -U "$DB_USER" -d "$DB_NAME" \
    --schema-only \
    --no-owner \
    --no-acl \
    --section=pre-data \
    -f "$OUTPUT_DIR/schema_tables_${TIMESTAMP}.sql"

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Table definitions saved to: $OUTPUT_DIR/schema_tables_${TIMESTAMP}.sql${NC}"
else
    echo "✗ Failed to dump table definitions"
fi
echo ""

# ============================================================================
# 3. Key Tables Only (Device-related)
# ============================================================================

echo -e "${YELLOW}[3/6] Dumping device-related tables...${NC}"
pg_dump -U "$DB_USER" -d "$DB_NAME" \
    --schema-only \
    --no-owner \
    --no-acl \
    -t device_categories \
    -t device_controllers \
    -t device_ports \
    -t configurations \
    -t device_peripherals \
    -t port_values \
    -f "$OUTPUT_DIR/schema_device_tables_${TIMESTAMP}.sql"

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Device tables saved to: $OUTPUT_DIR/schema_device_tables_${TIMESTAMP}.sql${NC}"
else
    echo "✗ Failed to dump device tables"
fi
echo ""

# ============================================================================
# 4. Table List
# ============================================================================

echo -e "${YELLOW}[4/6] Generating table list...${NC}"
psql -U "$DB_USER" -d "$DB_NAME" -t -c "
    SELECT table_name 
    FROM information_schema.tables 
    WHERE table_schema = 'public' 
    AND table_type = 'BASE TABLE'
    ORDER BY table_name;
" > "$OUTPUT_DIR/table_list_${TIMESTAMP}.txt"

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Table list saved to: $OUTPUT_DIR/table_list_${TIMESTAMP}.txt${NC}"
    echo -e "  Tables found: $(wc -l < $OUTPUT_DIR/table_list_${TIMESTAMP}.txt | tr -d ' ')"
else
    echo "✗ Failed to generate table list"
fi
echo ""

# ============================================================================
# 5. ER Diagram Data (relationships)
# ============================================================================

echo -e "${YELLOW}[5/6] Extracting foreign key relationships...${NC}"
psql -U "$DB_USER" -d "$DB_NAME" -f "$(dirname "$0")/dump-database-structure.sql" \
    > "$OUTPUT_DIR/database_structure_${TIMESTAMP}.txt" 2>&1

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Database structure saved to: $OUTPUT_DIR/database_structure_${TIMESTAMP}.txt${NC}"
else
    echo "✗ Failed to extract database structure (make sure dump-database-structure.sql exists)"
fi
echo ""

# ============================================================================
# 6. Quick Reference (column info)
# ============================================================================

echo -e "${YELLOW}[6/6] Generating quick reference...${NC}"
psql -U "$DB_USER" -d "$DB_NAME" -c "
    SELECT 
        table_name,
        column_name,
        data_type,
        character_maximum_length,
        is_nullable,
        column_default
    FROM information_schema.columns
    WHERE table_schema = 'public'
    ORDER BY table_name, ordinal_position;
" > "$OUTPUT_DIR/columns_reference_${TIMESTAMP}.txt"

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Columns reference saved to: $OUTPUT_DIR/columns_reference_${TIMESTAMP}.txt${NC}"
else
    echo "✗ Failed to generate columns reference"
fi
echo ""

# ============================================================================
# Summary
# ============================================================================

echo -e "${BLUE}============================================================================${NC}"
echo -e "${GREEN}Schema Dump Complete!${NC}"
echo -e "${BLUE}============================================================================${NC}"
echo ""
echo "Output files in: $OUTPUT_DIR/"
echo ""
echo "Files created:"
echo "  1. schema_full_${TIMESTAMP}.sql - Complete schema with all constraints"
echo "  2. schema_tables_${TIMESTAMP}.sql - Table definitions only"
echo "  3. schema_device_tables_${TIMESTAMP}.sql - Device-related tables"
echo "  4. table_list_${TIMESTAMP}.txt - List of all tables"
echo "  5. database_structure_${TIMESTAMP}.txt - Detailed structure analysis"
echo "  6. columns_reference_${TIMESTAMP}.txt - Column details"
echo ""
echo "Create a symlink to latest dump:"
echo "  ln -sf schema_full_${TIMESTAMP}.sql $OUTPUT_DIR/schema_latest.sql"
echo ""
echo "Compare with previous dump:"
echo "  diff $OUTPUT_DIR/schema_latest.sql $OUTPUT_DIR/schema_full_${TIMESTAMP}.sql"
echo ""

