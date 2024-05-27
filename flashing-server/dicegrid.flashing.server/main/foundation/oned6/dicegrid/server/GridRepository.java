package foundation.oned6.dicegrid.server;

import java.lang.foreign.MemorySegment;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface GridRepository {
	int findTeamID(String teamName);
	String findTeamName(int teamID);

	boolean isValidTeam(int teamID);
	Schematic[] schematicHistory(int teamID);
	Optional<Schematic> getSchematic(int teamID, int index);

	FlashEvent[] flashingHistory(int teamID);

	FlashEvent getFlashEvent(long id);
	Schematic updateSchematic(int teamID, TargetDevice target, MemorySegment pdfData, Instant uploaded);
	int enqueueFlash(int teamID, TargetDevice target, Code code, Instant submitted);
	void postFlashingInProgress(int eventID, Instant startTime);
	void postFlashingCompletion(int eventID, Instant completionTime);

	Optional<FlashEvent> getFlashEvent(int teamID, int index);

	record Schematic(long id, int index, int teamID, TargetDevice target, MemorySegment pdfData, Instant uploaded) {
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null || getClass() != obj.getClass()) return false;
			Schematic schematic = (Schematic) obj;
			return id == schematic.id;
		}
	}

	record FlashEvent(long id, int teamID, int index, TargetDevice target, Code code, Instant submitted, Instant start, Instant completed, String compileLog, byte[] hex) {
		public FlashEvent startedAt(Instant start) {
			return new FlashEvent(id, teamID, index, target, code, submitted, start, completed, compileLog, hex);
		}

		public FlashEvent completedAt(Instant completed) {
			return new FlashEvent(id, teamID, index, target, code, submitted, start, completed, compileLog, hex);
		}
	}

	enum TargetDevice {
		SOURCE, LOAD
	}
}
