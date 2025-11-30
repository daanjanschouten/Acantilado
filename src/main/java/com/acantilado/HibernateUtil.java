package com.acantilado;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;
import org.hibernate.tool.schema.SourceType;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.internal.ExceptionHandlerLoggedImpl;
import org.hibernate.tool.schema.spi.*;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class HibernateUtil {
    public static void generateSchema(Set<Class<?>> annotatedClasses) {
        StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                .applySetting(Environment.URL, "jdbc:h2:mem:schema")
                .applySetting(Environment.FORMAT_SQL, "true")
                .build();

        MetadataSources metadataSources = new MetadataSources(serviceRegistry);
        annotatedClasses.forEach(metadataSources::addAnnotatedClass);

        Metadata metadata = metadataSources.buildMetadata();

        SchemaManagementTool tool = serviceRegistry.getService(SchemaManagementTool.class);

        ExecutionOptions executionOptions = new ExecutionOptions() {
            @Override
            public boolean shouldManageNamespaces() {
                return false;
            }

            @Override
            public Map<String, Object> getConfigurationValues() {
                return new HashMap<>();
            }

            @Override
            public ExceptionHandler getExceptionHandler() {
                return new ExceptionHandlerLoggedImpl();
            }

            @Override
            public SchemaFilter getSchemaFilter() {
                return SchemaFilter.ALL;
            }
        };

        ContributableMatcher contributableMatcher = (contributed) -> true;

        SourceDescriptor sourceDescriptor = new SourceDescriptor() {
            @Override
            public SourceType getSourceType() {
                return SourceType.METADATA;
            }

            @Override
            public ScriptSourceInput getScriptSourceInput() {
                return null;
            }
        };

        try (Writer writer = new FileWriter(new File("create.sql"))) {
            TargetDescriptor targetDescriptor = new TargetDescriptor() {
                @Override
                public EnumSet<TargetType> getTargetTypes() {
                    return EnumSet.of(TargetType.SCRIPT);
                }

                @Override
                public ScriptTargetOutput getScriptTargetOutput() {
                    return new ScriptTargetOutput() {
                        @Override
                        public void accept(String command) {
                            try {
                                writer.write(command);
                                writer.write(";\n\n");
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }

                        @Override
                        public void prepare() {}

                        @Override
                        public void release() {}
                    };
                }
            };

            tool.getSchemaCreator(null).doCreation(
                    metadata,
                    executionOptions,
                    contributableMatcher,
                    sourceDescriptor,
                    targetDescriptor
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate schema", e);
        } finally {
            serviceRegistry.close();
        }
    }
}