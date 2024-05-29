package foundation.oned6.dicegrid.server;

import java.lang.foreign.MemorySegment;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

import static foundation.oned6.dicegrid.server.HTTPException.HTTPCode.BAD_REQUEST;
import static foundation.oned6.dicegrid.server.Server.context;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.nio.charset.StandardCharsets.UTF_8;

public record FormBody(List<FormEntry> fields) {
	private static final MemorySegment LEADING_DASHES = MemorySegment.ofArray(new byte[]{'-', '-'});
	private static final MemorySegment CRLF = MemorySegment.ofArray(new byte[]{'\r', '\n'});

	private static final Pattern CONTENT_TYPE = Pattern.compile("multipart/form-data; boundary=\"?(?<boundary>[^\"]+)\"?", Pattern.CASE_INSENSITIVE);

	public Optional<MemorySegment> get(String fieldName) {
		for (FormEntry field : fields) {
			if (fieldName.equals(field.disposition().name()))
				return Optional.of(field.data());
		}

		return Optional.empty();
	}

	public Optional<String> getString(String fieldName) {
		return get(fieldName).map(segment -> new String(segment.toArray(JAVA_BYTE), UTF_8));
	}

	public record FormEntry(String contentType, ContentDisposition disposition, MemorySegment data) {

	}

	public record ContentDisposition(String name, Optional<String> filename) {
		private static final Pattern PATTERN = Pattern.compile("form-data; name=\"?(?<name>[^\"]+)\"?(; filename=\"?(?<filename>[^\"]+)\"?)?");

		// TODO: make this more compliant
		private static ContentDisposition parse(String header) throws HTTPException {
			var matcher = PATTERN.matcher(header);

			if (!matcher.matches())
				throw new HTTPException(BAD_REQUEST);

			var name = matcher.group("name");
			if (name == null)
				throw new HTTPException(BAD_REQUEST);

			Optional<String> filename = Optional.ofNullable(matcher.group("filename"));

			return new ContentDisposition(name, filename);
		}
	}

	public static FormBody parse(Charset charset, String contentType, MemorySegment body) throws HTTPException {
		var matcher = CONTENT_TYPE.matcher(contentType);
		if (!matcher.matches())
			throw new HTTPException("Invalid Content-Type", BAD_REQUEST);

		var boundary = matcher.group("boundary");
		if (boundary == null)
			throw new HTTPException("Invalid Content-Type", BAD_REQUEST);

		var segments = splitByBoundary(charset, boundary, body);
		var fields = new ArrayList<FormEntry>();
		for (var segment : segments) {
			fields.add(parseEntry(segment));
		}

		return new FormBody(Collections.unmodifiableList(fields));
	}

	private static FormEntry parseEntry(MemorySegment segment) throws HTTPException {
		// find where the header seciton ends (first empty line)
		var headerEndIndices = new ArrayList<Long>();
		headerEndIndices.add(0L);

		long position = 0;
		for (; ; ) {
			long remaining = segment.byteSize() - position;
			if (remaining < 4)
				throw new HTTPException(BAD_REQUEST);

			var crlf = segment.asSlice(position, 2);
			if (crlf.mismatch(CRLF) == -1) {
				headerEndIndices.add(position);
				position += 2;
				//  two CRLF in a row means end of headers
				if (segment.asSlice(position, 2).mismatch(CRLF) == -1) {
					position += 2;
					break;
				}
			}

			position++;
		}

		var headers = new ArrayList<Map.Entry<String, String>>();
		for (int i = 0; i < headerEndIndices.size() - 1; i++) {
			long start = headerEndIndices.get(i);
			long end = headerEndIndices.get(i + 1);

			var headerSegment = segment.asSlice(start, end - start);
			headers.add(parseHeader(headerSegment));
		}

		var contentDisposition = ContentDisposition.parse(headers.stream()
			.filter(e -> e.getKey().equalsIgnoreCase("content-disposition")).map(e -> e.getValue())
			.findFirst().orElseThrow(() -> new HTTPException(BAD_REQUEST)));

		var contentType = headers.stream()
			.filter(e -> e.getKey().equalsIgnoreCase("content-type"))
			.findFirst()
			.map(Map.Entry::getValue)
			.orElse("text/plain");

		var entryData = segment.asSlice(position);

		return new FormEntry(contentType, contentDisposition, entryData);
	}

	private static Map.Entry<String, String> parseHeader(MemorySegment headerSegment) throws HTTPException {
		var header = new String(headerSegment.toArray(JAVA_BYTE), context().charset()).split(":", 2);
		if (header.length != 2)
			throw new HTTPException(BAD_REQUEST);

		return Map.entry(header[0].trim(), header[1].trim());
	}

	/*
	 * <a href="https://datatracker.ietf.org/doc/html/rfc2046#section-5.1.1">RFC 2046 § 5.1.1</a>
	 *  roughly implemented, doesn't work with trailing whitespace
	 *  but hcrome doesn't addthat so whatever
	 * @param boundary boundary string specified in request
	 */
	private static List<MemorySegment> splitByBoundary(Charset charset, String splitBoundaryString, MemorySegment content) {
		var splitBoundary = MemorySegment.ofArray(splitBoundaryString.getBytes(charset));
		var headerStartIndices = new ArrayList<Long>();

		long position = 0;
		// actual boundary: --<boundary>\r\n
		while (position + splitBoundary.byteSize() + 4 < content.byteSize()) {
			var leadingDashes = content.asSlice(position, 2);
			var boundary = content.asSlice(position + 2, splitBoundary.byteSize());
			var trailingCRLF = content.asSlice(position + 2 + splitBoundary.byteSize(), 2);

			if (
				leadingDashes.mismatch(LEADING_DASHES) == -1 &&
				boundary.mismatch(splitBoundary) == -1 &&
				trailingCRLF.mismatch(CRLF) == -1
			) {
				headerStartIndices.add(position + boundary.byteSize() + 4);
				position += boundary.byteSize() + 4;
			} else {
				position++;
			}
		}

		var segments = new ArrayList<MemorySegment>();
		for (int i = 0; i < headerStartIndices.size(); i++) {
			long start = headerStartIndices.get(i);
			long end = (i + 1 < headerStartIndices.size()) ?
				headerStartIndices.get(i + 1) :
				content.byteSize() - 2;

			end -= 4 + splitBoundary.byteSize();
			segments.add(content.asSlice(start, end - start));
		}

		return segments;
	}
}
