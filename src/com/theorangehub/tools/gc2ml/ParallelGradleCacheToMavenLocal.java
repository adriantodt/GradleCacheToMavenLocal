package com.theorangehub.tools.gc2ml;

import javafx.util.Pair;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;

public class ParallelGradleCacheToMavenLocal {
    public static File createMavenFolder() {
        File repo = new File(".m2/repository");

        if (!repo.exists()) {
            repo.mkdirs();
            return repo;
        }

        if (repo.isFile()) {
            if (!repo.delete()) {
                System.out.println("Maven Repository is Fucked up.");
                System.exit(0);
                throw new IllegalStateException();
            }

            repo.mkdirs();
        }

        return repo;
    }

    public static void main(String[] args) throws IOException {
        parallelWork();
    }

    public static void parallelWork() {
        Future<File> mavenFolder = new CompletableThread<>(ParallelGradleCacheToMavenLocal::createMavenFolder);
        Supplier<File> repository = () -> wrap(mavenFolder);

        File gradleCaches = new File(".gradle/caches");
        if (!gradleCaches.exists() || !gradleCaches.isDirectory()) {
            System.out.println("No Gradle Caches.");
            System.exit(0);
            return;
        }

        //region counters
        AtomicLong skippedFileCounter = new AtomicLong(), createdFileCounter = new AtomicLong(), exceptionCounter = new AtomicLong();
        //endregion

        //region findGradleCaches()
        Arrays.stream(gradleCaches.listFiles(folder -> folder.getName().startsWith("modules") && folder.isDirectory())).parallel()
            .filter(File::isDirectory).map(f -> f.listFiles(folder -> folder.getName().startsWith("files") && folder.isDirectory()))
            .filter(Objects::nonNull).flatMap(Arrays::stream).filter(File::isDirectory).map(File::listFiles).filter(Objects::nonNull)
            .flatMap(Arrays::stream).filter(File::isDirectory)
            //endregion
            //region convertCaches()
            .flatMap(cachedDepsGroups -> {
                File mavenDepGroup = new File(repository.get(), cachedDepsGroups.getName().replace('.', '/'));
                return Arrays.stream(cachedDepsGroups.listFiles()).parallel()
                    .map(cachedDepsModules -> new Pair<>(cachedDepsModules, new File(mavenDepGroup, cachedDepsModules.getName())));
            })
            .flatMap(cachedAndMavenDepModules -> {
                File mavenDepModule = cachedAndMavenDepModules.getValue();
                return Arrays.stream(cachedAndMavenDepModules.getKey().listFiles()).parallel()
                    .map(cachedDepsVersions -> new Pair<>(cachedDepsVersions, new File(mavenDepModule, cachedDepsVersions.getName())));
            })
            .flatMap(cachedAndMavenDepVersions -> {
                File mavenDepVersion = cachedAndMavenDepVersions.getValue();
                mavenDepVersion.mkdirs();
                return Arrays.stream(cachedAndMavenDepVersions.getKey().listFiles())
                    .parallel()
                    .flatMap(hashedShit -> Arrays.stream(hashedShit.listFiles()).parallel())
                    .map(cachedFileOfVersion -> new Pair<>(cachedFileOfVersion, new File(mavenDepVersion, cachedFileOfVersion.getName())));
            })
            .forEach(cachedAndMavenFileOfVersion -> {
                File cachedFileOfVersion = cachedAndMavenFileOfVersion.getKey();
                File mavenFileOfVersion = cachedAndMavenFileOfVersion.getValue();
                if (mavenFileOfVersion.exists()) {
                    skippedFileCounter.incrementAndGet();
                    return;
                }

                try {
                    Files.copy(cachedFileOfVersion.toPath(), mavenFileOfVersion.toPath(), COPY_ATTRIBUTES);
                } catch (IOException e) {
                    e.printStackTrace();
                    exceptionCounter.incrementAndGet();
                    return;
                }

                createdFileCounter.incrementAndGet();
            });
        //endregion

        //report the stats
        System.out.printf("%d files copied, %d files skipped, %d exceptions.\n", createdFileCounter.get(), skippedFileCounter.get(), exceptionCounter.get());
    }

    private static <T> T wrap(Future<T> future) {
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
