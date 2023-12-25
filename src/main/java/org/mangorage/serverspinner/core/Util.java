package org.mangorage.serverspinner.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class Util {

    public static List<String> readLinesFromFile(File file) {
        try (var is = new FileInputStream(file)) {
            return readLinesFromInputStream(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return List.of();
    }

    public static UUID getUUID(String str) {
        try {
            return UUID.fromString(str);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static List<String> readLinesFromInputStream(InputStream inputStream) {
        List<String> lines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                lines.add(line);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        return lines;
    }

    public static void copyDirectory(Path sourceDir, Path targetDir) throws IOException {
        // Copy the entire directory and its contents recursively
        Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                // Calculate the target path for the file in the new directory
                Path targetFile = targetDir.resolve(sourceDir.relativize(file));

                // Copy the file
                Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                // Calculate the target path for the directory in the new directory
                Path targetDirPath = targetDir.resolve(sourceDir.relativize(dir));

                // Create the target directory
                Files.createDirectories(targetDirPath);

                return FileVisitResult.CONTINUE;
            }
        });
    }

}
