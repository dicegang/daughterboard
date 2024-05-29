package foundation.oned6.dicegrid;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public record TempDirectory(Path path) implements AutoCloseable {
	public static TempDirectory create(String prefix) throws IOException {
		return new TempDirectory(Files.createTempDirectory(prefix));
	}

	@Override
	public void close() {
		try {
			recursiveDeleteDirectory(path);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String toString() {
		return path.toString();
	}

	private static void recursiveDeleteDirectory(Path buildDir) throws IOException {
		Files.walkFileTree(buildDir, new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}
}
