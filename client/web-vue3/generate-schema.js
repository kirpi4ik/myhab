const fs = require('fs');
const { buildClientSchema, printSchema } = require('graphql');

try {
  // Read the introspection result
  const introspectionText = fs.readFileSync('introspection-result.json', 'utf8');
  const introspection = JSON.parse(introspectionText);
  
  // Clean up any problematic default values in the schema
  if (introspection.data && introspection.data.__schema) {
    const types = introspection.data.__schema.types;
    
    types.forEach(type => {
      if (type.inputFields) {
        type.inputFields.forEach(field => {
          // Replace empty string default values with null
          if (field.defaultValue === '' || field.defaultValue === '<EOF>') {
            field.defaultValue = null;
          }
        });
      }
      
      if (type.fields) {
        type.fields.forEach(field => {
          if (field.args) {
            field.args.forEach(arg => {
              // Replace empty string default values with null
              if (arg.defaultValue === '' || arg.defaultValue === '<EOF>') {
                arg.defaultValue = null;
              }
            });
          }
        });
      }
    });
  }
  
  // Build the schema from introspection
  const schema = buildClientSchema(introspection.data);
  
  // Generate the schema SDL
  const schemaSDL = printSchema(schema);
  
  // Write to file
  fs.writeFileSync('src/graphql/schema.graphql', '# This file was generated. Do not edit manually.\n\n' + schemaSDL);
  
  console.log('✓ Schema generated successfully!');
  console.log('  Output: src/graphql/schema.graphql');
  
} catch (error) {
  console.error('Error generating schema:', error.message);
  console.error(error.stack);
  process.exit(1);
}

