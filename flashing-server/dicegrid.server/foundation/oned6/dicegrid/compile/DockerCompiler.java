package foundation.oned6.dicegrid.compile;

import foundation.oned6.dicegrid.TempDirectory;
import foundation.oned6.dicegrid.TempFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DockerCompiler implements Compiler {
	private static final String DOCKER_IMAGE = "dicegrid-compiler";


	@Override
	public CompileResult compile(String code) throws InterruptedException {
		var output = "";

		try (var tempDir = TempDirectory.create("dicegrid")) {
			var binds = Map.of(tempDir.path(), Path.of("/build"));
			Files.writeString(tempDir.path().resolve("program.c"), code);

			output += runCommandInDocker(binds, "avr-gcc", "-Wall", "-Os", "-DF_CPU=8000000", "-mmcu=attiny85", "-c", "program.c", "-o", "program.o");
			output += runCommandInDocker(binds, "avr-gcc", "-Wall", "-Os", "-DF_CPU=8000000", "-mmcu=attiny85", "-o", "program.elf", "program.o");
			output += runCommandInDocker(binds, "avr-objcopy", "-j", ".text", "-j", ".data", "-O", "ihex", "program.elf", "program.hex");
			output += runCommandInDocker(binds, "avr-size","program.elf");

			return new CompileResult(Files.readAllBytes(tempDir.path().resolve("program.hex")), output);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (CompilerException e) {
			return new CompileResult(null, output + e.getMessage());
		}
	}

	private static String runCommandInDocker(Map<Path, Path> bindMounts, String... command) throws CompilerException {
		try (var output = TempFile.create()) {
			buildDockerImage();

			var pb = new ProcessBuilder("docker", "run", "--rm");
			for (var entry : bindMounts.entrySet()) {
				pb.command().add("-v");
				pb.command().add(entry.getKey() + ":" + entry.getValue());
			}
			pb.command().add(DOCKER_IMAGE);
			pb.command().addAll(List.of(command));
			pb.redirectOutput(ProcessBuilder.Redirect.appendTo(output.path().toFile()));
			pb.redirectError(ProcessBuilder.Redirect.appendTo(output.path().toFile()));

			var process = pb.start();
			if (!process.waitFor(1, TimeUnit.SECONDS)) {
				process.destroyForcibly();
				throw new CompilerException("Timed out");
			}

			if (process.exitValue() != 0) {
				throw new CompilerException("> " + String.join(" ", command) + "\n" + Files.readString(output.path()));
			}

			return "> " + String.join(" ", command) + "\n" + Files.readString(output.path());
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return null;
	}

	private static class CompilerException extends Exception {
		public CompilerException(Exception e) {
			super(e);
		}

		public CompilerException() {
			super();
		}

		public CompilerException(String message) {
			super(message);
		}
	}
	private static volatile boolean dockerImageBuilt = false;
	private static void buildDockerImage() throws CompilerException {
		if (dockerImageBuilt) {
			return;
		}
		try {
			var pb = new ProcessBuilder("docker", "build", "-t", DOCKER_IMAGE, "-");

			var process = pb.start();
			process.getOutputStream().write("""
				FROM alpine:3.20
				RUN apk add make avr-libc gcc-avr binutils-avr
				WORKDIR /build""".getBytes());
			process.getOutputStream().close();

			if (process.waitFor() != 0) {
				throw new RuntimeException("Failed to build Docker image");
			}

			dockerImageBuilt = true;
		} catch (IOException e) {
			e.printStackTrace();
			throw new CompilerException(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
