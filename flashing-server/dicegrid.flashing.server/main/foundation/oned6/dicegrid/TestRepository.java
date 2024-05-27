package foundation.oned6.dicegrid;

import foundation.oned6.dicegrid.server.Code;
import foundation.oned6.dicegrid.server.GridRepository;

import java.lang.foreign.MemorySegment;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TestRepository implements GridRepository {
	private final AtomicInteger idCounter = new AtomicInteger();

	private final Map<Integer, FlashEvent> eventsByID = new HashMap<>();
	private final Map<Integer, List<FlashEvent>> events = new HashMap<>();
	private final Map<Integer, List<Schematic>> schematics = new HashMap<>();

	public TestRepository() {

	}

	@Override
	public synchronized int findTeamID(String teamName) {
		return 0;
	}

	@Override
	public String findTeamName(int teamID) {
		return teamID == 0 ? "Test Team" : "Unknown Team";
	}

	@Override
	public synchronized boolean isValidTeam(int teamID) {
		return teamID == 0;
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
	public FlashEvent getFlashEvent(long id) {
		return eventsByID.get((int)id);
	}

	@Override
	public synchronized Schematic updateSchematic(int teamID, TargetDevice target, MemorySegment pdfData, Instant uploaded) {
		var result = new Schematic(idCounter.getAndIncrement(), schematicHistory(teamID).length, teamID, target, pdfData, uploaded);
		teamSchematics(teamID).add(result);
		return result;
	}

	@Override
	public synchronized int enqueueFlash(int teamID, TargetDevice target, Code code, Instant submitted) {
		int id = idCounter.getAndIncrement();
		var event = new FlashEvent(id, teamID, teamFlashingEvents(teamID).size(), target, code, submitted, null, null, null, null);
		eventsByID.put(id, event);
		teamFlashingEvents(teamID).add(event);
		return id;
	}

	@Override
	public void postFlashingInProgress(int eventID, Instant startTime) {
		var oldEvent = eventsByID.get(eventID);
		var newEvent = oldEvent.startedAt(startTime);

		var teamEvents = teamFlashingEvents(newEvent.teamID());

		eventsByID.put(eventID, newEvent);
		teamEvents.set(teamEvents.indexOf(oldEvent), newEvent);
	}

	@Override
	public synchronized void postFlashingCompletion(int eventID, Instant completionTime) {
		var oldEvent = eventsByID.get(eventID);
		var newEvent = oldEvent.completedAt(completionTime);

		var teamEvents = teamFlashingEvents(newEvent.teamID());

		eventsByID.put(eventID, newEvent);
		teamEvents.set(teamEvents.indexOf(oldEvent), newEvent);
	}

	@Override
	public Optional<FlashEvent> getFlashEvent(int teamID, int index) {
		var events = teamFlashingEvents(teamID);
		if (index < 0 || index >= events.size()) {
			return Optional.empty();
		}

		return Optional.of(events.get(index));
	}

	private List<Schematic> teamSchematics(int teamID) {
		return schematics.computeIfAbsent(teamID, _ -> Collections.synchronizedList(new ArrayList<>()));
	}

	private List<FlashEvent> teamFlashingEvents(int teamID) {
		return events.computeIfAbsent(teamID, _ -> Collections.synchronizedList(new ArrayList<>()));
	}
}
