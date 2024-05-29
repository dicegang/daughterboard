package foundation.oned6.dicegrid.compile;

public interface Compiler {
	CompileResult compile(String src) throws InterruptedException;

	record CompileResult(byte[] hex, String compileLog) {}
}
