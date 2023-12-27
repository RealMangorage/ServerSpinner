package org.mangorage.serverspinner.core;

import java.nio.file.Path;

public class Constants {
    private static final Path TEMPLATE = Path.of("plugins/template");
    private static final Path INSTANCES = Path.of("plugins/instances");

    public static Path getTemplate() {
        return TEMPLATE.toAbsolutePath();
    }

    public static Path getInstances() {
        return INSTANCES.toAbsolutePath();
    }
}
