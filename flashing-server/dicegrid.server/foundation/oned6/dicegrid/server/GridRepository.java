package foundation.oned6.dicegrid.server;

import foundation.oned6.dicegrid.protocol.NodeType;
import foundation.oned6.dicegrid.server.auth.TeamPrincipal;

import java.lang.foreign.MemorySegment;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface GridRepository {
	int findTeamID(String teamName);
	String findTeamName(int teamID);

	List<TeamPrincipal> getAllTeams();

	boolean isValidTeam(int teamID);
	Schematic[] schematicHistory(int teamID);
	Optional<Schematic> getSchematic(int teamID, int index);

	FlashEvent[] flashingHistory(int teamID);

	Optional<FlashEvent> getFlashEvent(long id);
	Schematic updateSchematic(int teamID, NodeType target, MemorySegment pdfData, Instant uploaded);
	long beginFlashing(int teamID, NodeType target, Code code);
	void updateFlashStatus(long id, FlashEvent.Status status);
	void setFlashCompilerOutput(long id, String compileLog, byte[] hex);

	Optional<FlashEvent> getFlashEvent(int teamID, int index);

	record Schematic(long id, int index, int teamID, NodeType target, MemorySegment pdfData, Instant uploaded) {
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null || getClass() != obj.getClass()) return false;
			Schematic schematic = (Schematic) obj;
			return id == schematic.id;
		}
	}

	record FlashEvent(long id, int index, int teamID, NodeType target, Code code, Instant lastUpdate, String compileLog, byte[] hex, Status status) {
		public FlashEvent updateStatus(Status status) {
			return new FlashEvent(id, index, teamID, target, code, Instant.now(), compileLog, hex, status);
		}

		public FlashEvent updateOutput(String compileLog, byte[] hex) {
			return new FlashEvent(id, index, teamID, target, code, Instant.now(), compileLog, hex, status);
		}

		public enum Status {
			 BUILDING,BUILD_FAILED,
			QUEUED,
			FLASH_FAILED, FLASHING,
			SUCCESS;

			public boolean isComplete() {
				return this == SUCCESS || this == BUILD_FAILED || this == FLASH_FAILED;
			}
		}
	}
}
