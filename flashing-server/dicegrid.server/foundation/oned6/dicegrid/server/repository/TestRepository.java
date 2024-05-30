package foundation.oned6.dicegrid.server.repository;

import foundation.oned6.dicegrid.protocol.NodeType;
import foundation.oned6.dicegrid.server.Code;
import foundation.oned6.dicegrid.server.auth.TeamPrincipal;

import java.lang.foreign.MemorySegment;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static foundation.oned6.dicegrid.server.repository.GridRepository.FlashEvent.Status.BUILDING;

public class TestRepository implements GridRepository {
	private final AtomicInteger teamCounter = new AtomicInteger();
	private final AtomicLong idCounter = new AtomicLong();

	private final Map<Long, FlashEvent> eventsByID = new HashMap<>();
	private final Map<Long, List<FlashEvent>> events = new HashMap<>();
	private final Map<Long, List<Schematic>> schematics = new HashMap<>();
	private final Map<Integer, String> teamNames = new HashMap<>();

	public TestRepository() {

	}

	@Override
	public synchronized void addTeam(String teamName) {
		teamNames.put(teamCounter.getAndIncrement(), teamName);
	}

	@Override
	public synchronized OptionalInt findTeamID(String teamName) {
		return teamNames.entrySet().stream()
				.filter(entry -> entry.getValue().equals(teamName))
				.mapToInt(Map.Entry::getKey)
				.findFirst();
	}

	@Override
	public Optional<String> findTeamName(int teamID) {
		return Optional.ofNullable(teamNames.get(teamID));
	}

	@Override
	public List<TeamPrincipal> getAllTeams() {
		return teamNames.entrySet().stream()
				.map(entry -> new TeamPrincipal(entry.getValue(), entry.getKey()))
				.toList();
	}

	@Override
	public synchronized Schematic[] schematicHistory(int teamID) {
		return teamSchematics(teamID).toArray(Schematic[]::new);
	}

	@Override
	public Optional<Schematic> getSchematic(int teamID, int index) {
		if (index < 0 || index >= teamSchematics(teamID).size()) {
			return Optional.empty();
		}

		return Optional.of(teamSchematics(teamID).get(index));
	}

	@Override
	public FlashEvent[] flashingHistory(int teamID) {
		return teamFlashingEvents(teamID).toArray(FlashEvent[]::new);
	}

	@Override
	public Optional<FlashEvent> getFlashEvent(long id) {
		return Optional.ofNullable(eventsByID.get(id));
	}

	@Override
	public synchronized Schematic updateSchematic(int teamID, NodeType target, MemorySegment pdfData, Instant uploaded) {
		var result = new Schematic(idCounter.getAndIncrement(), schematicHistory(teamID).length, teamID, target, pdfData, uploaded);
		teamSchematics(teamID).add(result);
		return result;
	}

	@Override
	public synchronized long beginFlashing(int teamID, NodeType target, Code code) {
		long id = idCounter.getAndIncrement();
		var event = new FlashEvent(id, teamFlashingEvents(teamID).size(), teamID, target, code, Instant.now(), null, null, BUILDING);
		eventsByID.put(id, event);
		teamFlashingEvents(teamID).add(event);
		return id;
	}

	@Override
	public synchronized void updateFlashStatus(long id, FlashEvent.Status status) {
		var oldEvent = eventsByID.get(id);
		var newEvent = oldEvent.updateStatus(status);

		var teamEvents = teamFlashingEvents(newEvent.teamID());

		eventsByID.put(id, newEvent);
		teamEvents.set(teamEvents.indexOf(oldEvent), newEvent);
	}

	@Override
	public void setFlashCompilerOutput(long id, String compileLog, byte[] hex) {
		var oldEvent = eventsByID.get(id);
		var newEvent = oldEvent. updateOutput(compileLog, hex);

		var teamEvents = teamFlashingEvents(newEvent.teamID());

		eventsByID.put(id, newEvent);
		teamEvents.set(teamEvents.indexOf(oldEvent), newEvent);
	}

	private List<Schematic> teamSchematics(long teamID) {
		return schematics.computeIfAbsent(teamID, _ -> Collections.synchronizedList(new ArrayList<>()));
	}

	private List<FlashEvent> teamFlashingEvents(long teamID) {
		return events.computeIfAbsent(teamID, _ -> Collections.synchronizedList(new ArrayList<>()));
	}
}
