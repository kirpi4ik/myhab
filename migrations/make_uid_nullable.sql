-- Migration script to make 'uid' column nullable in all tables
-- This removes the need for custom uid setting logic in BaseEntity.afterInsert()
--
-- This script:
-- 1. Finds all tables with a 'uid' column
-- 2. Makes the column nullable
-- 3. Handles partitioned tables (like port_values) by altering parent and partitions

-- Step 1: Make uid nullable in all regular tables
-- This dynamically finds all tables with a uid column and makes it nullable

DO $$
DECLARE
    table_record RECORD;
    partition_record RECORD;
    sql_stmt TEXT;
BEGIN
    -- Loop through all tables in the public schema that have a 'uid' column
    FOR table_record IN
        SELECT DISTINCT
            t.table_schema,
            t.table_name
        FROM information_schema.tables t
        INNER JOIN information_schema.columns c
            ON t.table_schema = c.table_schema
            AND t.table_name = c.table_name
        WHERE t.table_schema = 'public'
            AND t.table_type = 'BASE TABLE'
            AND c.column_name = 'uid'
            AND c.is_nullable = 'NO'  -- Only alter if currently NOT NULL
        ORDER BY t.table_name
    LOOP
        -- Check if this is a partitioned table
        IF EXISTS (
            SELECT 1
            FROM pg_inherits
            WHERE inhrelid = (
                SELECT oid
                FROM pg_class
                WHERE relname = table_record.table_name
                AND relnamespace = (
                    SELECT oid
                    FROM pg_namespace
                    WHERE nspname = table_record.table_schema
                )
            )
        ) THEN
            -- This is a partition, skip it (we'll handle partitions separately)
            CONTINUE;
        END IF;
        
        -- Check if this is a parent partitioned table
        IF EXISTS (
            SELECT 1
            FROM pg_inherits
            WHERE inhparent = (
                SELECT oid
                FROM pg_class
                WHERE relname = table_record.table_name
                AND relnamespace = (
                    SELECT oid
                    FROM pg_namespace
                    WHERE nspname = table_record.table_schema
                )
            )
        ) THEN
            -- This is a parent partitioned table
            -- First, alter the parent table
            sql_stmt := format('ALTER TABLE %I.%I ALTER COLUMN uid DROP NOT NULL', 
                table_record.table_schema, 
                table_record.table_name);
            
            BEGIN
                EXECUTE sql_stmt;
                RAISE NOTICE 'Made uid nullable in parent partitioned table: %.%', 
                    table_record.table_schema, table_record.table_name;
            EXCEPTION WHEN OTHERS THEN
                RAISE WARNING 'Failed to alter parent table %.%: %', 
                    table_record.table_schema, table_record.table_name, SQLERRM;
            END;
            
            -- Then, alter all partitions
            FOR partition_record IN
                SELECT 
                    n.nspname AS partition_schema,
                    c.relname AS partition_name
                FROM pg_inherits i
                JOIN pg_class c ON i.inhrelid = c.oid
                JOIN pg_namespace n ON c.relnamespace = n.oid
                WHERE i.inhparent = (
                    SELECT oid
                    FROM pg_class
                    WHERE relname = table_record.table_name
                    AND relnamespace = (
                        SELECT oid
                        FROM pg_namespace
                        WHERE nspname = table_record.table_schema
                    )
                )
            LOOP
                -- Check if partition has uid column and it's NOT NULL
                IF EXISTS (
                    SELECT 1
                    FROM information_schema.columns
                    WHERE table_schema = partition_record.partition_schema
                        AND table_name = partition_record.partition_name
                        AND column_name = 'uid'
                        AND is_nullable = 'NO'
                ) THEN
                    sql_stmt := format('ALTER TABLE %I.%I ALTER COLUMN uid DROP NOT NULL', 
                        partition_record.partition_schema, 
                        partition_record.partition_name);
                    
                    BEGIN
                        EXECUTE sql_stmt;
                        RAISE NOTICE 'Made uid nullable in partition: %.%', 
                            partition_record.partition_schema, partition_record.partition_name;
                    EXCEPTION WHEN OTHERS THEN
                        RAISE WARNING 'Failed to alter partition %.%: %', 
                            partition_record.partition_schema, partition_record.partition_name, SQLERRM;
                    END;
                END IF;
            END LOOP;
        ELSE
            -- Regular table (not partitioned)
            sql_stmt := format('ALTER TABLE %I.%I ALTER COLUMN uid DROP NOT NULL', 
                table_record.table_schema, 
                table_record.table_name);
            
            BEGIN
                EXECUTE sql_stmt;
                RAISE NOTICE 'Made uid nullable in table: %.%', 
                    table_record.table_schema, table_record.table_name;
            EXCEPTION WHEN OTHERS THEN
                RAISE WARNING 'Failed to alter table %.%: %', 
                    table_record.table_schema, table_record.table_name, SQLERRM;
            END;
        END IF;
    END LOOP;
    
    RAISE NOTICE '=== Migration completed ===';
END $$;

-- Verification query: Check which tables still have NOT NULL uid columns
SELECT 
    t.table_schema,
    t.table_name,
    c.column_name,
    c.is_nullable,
    CASE 
        WHEN EXISTS (
            SELECT 1 FROM pg_inherits 
            WHERE inhparent = (
                SELECT oid FROM pg_class 
                WHERE relname = t.table_name 
                AND relnamespace = (SELECT oid FROM pg_namespace WHERE nspname = t.table_schema)
            )
        ) THEN 'Parent Partitioned Table'
        WHEN EXISTS (
            SELECT 1 FROM pg_inherits 
            WHERE inhrelid = (
                SELECT oid FROM pg_class 
                WHERE relname = t.table_name 
                AND relnamespace = (SELECT oid FROM pg_namespace WHERE nspname = t.table_schema)
            )
        ) THEN 'Partition'
        ELSE 'Regular Table'
    END AS table_type
FROM information_schema.tables t
INNER JOIN information_schema.columns c
    ON t.table_schema = c.table_schema
    AND t.table_name = c.table_name
WHERE t.table_schema = 'public'
    AND t.table_type = 'BASE TABLE'
    AND c.column_name = 'uid'
ORDER BY 
    CASE 
        WHEN EXISTS (
            SELECT 1 FROM pg_inherits 
            WHERE inhparent = (
                SELECT oid FROM pg_class 
                WHERE relname = t.table_name 
                AND relnamespace = (SELECT oid FROM pg_namespace WHERE nspname = t.table_schema)
            )
        ) THEN 1
        WHEN EXISTS (
            SELECT 1 FROM pg_inherits 
            WHERE inhrelid = (
                SELECT oid FROM pg_class 
                WHERE relname = t.table_name 
                AND relnamespace = (SELECT oid FROM pg_namespace WHERE nspname = t.table_schema)
            )
        ) THEN 2
        ELSE 3
    END,
    t.table_name;

