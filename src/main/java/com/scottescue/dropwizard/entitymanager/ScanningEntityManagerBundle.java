package com.scottescue.dropwizard.entitymanager;

import com.google.common.collect.ImmutableList;
import io.dropwizard.Configuration;
import org.glassfish.jersey.server.internal.scanning.AnnotationAcceptingListener;
import org.glassfish.jersey.server.internal.scanning.PackageNamesScanner;

import javax.persistence.Entity;
import java.io.IOException;
import java.io.InputStream;

/**
 * Extension of EntityManagerBundle that scans given package for entities instead of giving them by hand.
 */
public abstract class ScanningEntityManagerBundle<T extends Configuration> extends EntityManagerBundle<T> {
    /**
     * @param pckg string with package containing JPA entities (classes annotated with {@code @Entity}
     *             annotation) e. g. {@code com.my.application.directory.entities}
     */
    protected ScanningEntityManagerBundle(String pckg) {
        this(pckg, new EntityManagerFactoryFactory());
    }

    protected ScanningEntityManagerBundle(String pckg, EntityManagerFactoryFactory entityManagerFactoryFactory) {
        super(findEntityClassesFromDirectory(pckg), entityManagerFactoryFactory);
    }

    /**
     * Method scanning given directory for classes containing JPA @Entity annotation
     *
     * @param pckg string with package containing JPA entities (classes annotated with @Entity annotation)
     * @return ImmutableList with classes from given directory annotated with JPA @Entity annotation
     */
    public static ImmutableList<Class<?>> findEntityClassesFromDirectory(String pckg) {
        @SuppressWarnings("unchecked")
        final AnnotationAcceptingListener asl = new AnnotationAcceptingListener(Entity.class);
        final PackageNamesScanner scanner = new PackageNamesScanner(new String[]{pckg}, true);

        while (scanner.hasNext()) {
            final String next = scanner.next();
            if (asl.accept(next)) {
                try (final InputStream in = scanner.open()) {
                    asl.process(next, in);
                } catch (IOException e) {
                    throw new RuntimeException("AnnotationAcceptingListener failed to process scanned resource: " + next);
                }
            }
        }

        final ImmutableList.Builder<Class<?>> builder = ImmutableList.builder();
        for (Class<?> clazz : asl.getAnnotatedClasses()) {
            builder.add(clazz);
        }

        return builder.build();
    }
}
