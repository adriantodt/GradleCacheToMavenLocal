package br.net.brjdevs.adrian.tools.gc2ml;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;

public class GC2ML {
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

	public static Set<File> findGradleCaches() {
		//return Set<File>(".gradle/caches/modules-*/files-*/");
		return Stream.of(new File(".gradle/caches"))
            .parallel()
			.filter(File::exists)
			.filter(File::isDirectory)
			.map(f -> f.listFiles(folder -> folder.getName().startsWith("modules") && folder.isDirectory()))
			.filter(Objects::nonNull)
			.flatMap(Arrays::stream)
			.filter(File::isDirectory)
			.map(f -> f.listFiles(folder -> folder.getName().startsWith("files") && folder.isDirectory()))
			.filter(Objects::nonNull)
			.flatMap(Arrays::stream)
			.filter(File::isDirectory)
			.map(File::listFiles)
			.filter(Objects::nonNull)
			.flatMap(Arrays::stream)
			.filter(File::isDirectory)
			.collect(Collectors.toCollection(TreeSet::new));
	}

	public static void main(String[] args) throws IOException {
		Set<File> gradleCaches = findGradleCaches();

		if (gradleCaches.isEmpty()) {
			System.out.println("No Gradle Caches.");
			System.exit(0);
			return;
		}

		File repository = createMavenFolder();

		int amount = 0;
		for (File cachedDepsGroups : gradleCaches) {
			File mavenDepGroup = new File(repository, cachedDepsGroups.getName().replace('.', '/'));

			System.out.println(mavenDepGroup);

			for (File cachedDepsModules : cachedDepsGroups.listFiles()) {
				File mavenDepModule = new File(mavenDepGroup, cachedDepsModules.getName());
				System.out.println("\t" + mavenDepModule);

				for (File cachedDepsVersions : cachedDepsModules.listFiles()) {
					File mavenDepVersion = new File(mavenDepModule, cachedDepsVersions.getName());
					System.out.println("\t\t" + mavenDepVersion);
					mavenDepVersion.mkdirs();

					for (File hashedShit : cachedDepsVersions.listFiles()) {
						for (File cachedFileOfVersion : hashedShit.listFiles()) {
							File mavenFileOfVersion = new File(mavenDepVersion, cachedFileOfVersion.getName());
							System.out.print("\t\t\t" + mavenFileOfVersion);
							if (mavenFileOfVersion.exists()) {
								System.out.println(" (SKIPPED)");
								continue;
							}

                            Files.copy(cachedFileOfVersion.toPath(), mavenFileOfVersion.toPath(), COPY_ATTRIBUTES);

							System.out.println(" (CREATED)");
						}
					}
				}
			}
		}
	}
}
