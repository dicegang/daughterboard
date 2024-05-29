package foundation.oned6.dicegrid;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public record TempFile(Path path) implements AutoCloseable {
	public static TempFile create(String prefix, String suffix) throws IOException {
		var path = Files.createTempFile(prefix, suffix);
		Files.delete(path);
		return new TempFile(path);
	}

	public static TempFile create() throws IOException {
		return create("", "");
	}

	public static TempFile withContents(String prefix, String suffix, byte[] contents) throws IOException {
		var path = Files.createTempFile(prefix, suffix);
		Files.write(path, contents);
		return new TempFile(path);
	}

	public static TempFile withContents(String prefix, String suffix, String contents) throws IOException {
		var path = Files.createTempFile(prefix, suffix);
		Files.writeString(path, contents);
		return new TempFile(path);
	}

	@Override
	public void close() {
		try {
			if (Files.exists(path))
				Files.delete(path);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String toString() {
		return path.toString();

	}
}
