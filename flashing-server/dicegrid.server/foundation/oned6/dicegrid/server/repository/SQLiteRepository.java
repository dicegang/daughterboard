package foundation.oned6.dicegrid.server.repository;

import foundation.oned6.dicegrid.protocol.NodeType;
import foundation.oned6.dicegrid.server.Code;
import foundation.oned6.dicegrid.server.auth.TeamPrincipal;

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;

public class SQLiteRepository implements GridRepository {
	private final Connection connection;

	public SQLiteRepository(Connection connection) {
		this.connection = connection;
	}

	public void createTables() throws SQLException {
		try (var statement = connection.createStatement()) {
			statement.execute("DROP TABLE IF EXISTS teams");
			statement.execute("DROP TABLE IF EXISTS schematics");
			statement.execute("DROP TABLE IF EXISTS flashEvents");

			statement.execute("CREATE TABLE IF NOT EXISTS teams (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL)");
			statement.execute("CREATE UNIQUE INDEX IF NOT EXISTS teamNameIndex ON teams(name)");
			statement.execute("CREATE UNIQUE INDEX IF NOT EXISTS teamNameIndex ON teams(id)");
			// int index, int teamID, NodeType target, MemorySegment pdfData, Instant uploaded
			statement.execute(STR."""
				CREATE TABLE IF NOT EXISTS schematics (
						id INTEGER PRIMARY KEY AUTOINCREMENT,
						idx INTEGER NOT NULL,
						teamID INTEGER NOT NULL,
						target TEXT CHECK(target IN (\{toSQLList(NodeType.class)})),
						pdfData BLOB NOT NULL,
						uploaded TEXT NOT NULL,
						FOREIGN KEY(teamID) REFERENCES teams(id)
				)""");
			statement.execute("CREATE UNIQUE INDEX IF NOT EXISTS schematicIndex ON schematics(teamID, idx)");
			statement.execute("CREATE UNIQUE INDEX IF NOT EXISTS schematicIndex ON schematics(id)");

			// int index, int teamID, NodeType target, Code code, Instant lastUpdate, String compileLog, byte[] hex, Status status
			statement.execute(STR."""
				CREATE TABLE IF NOT EXISTS flashEvents (
					id INTEGER PRIMARY KEY,
					idx INTEGER NOT NULL,
					teamID INTEGER NOT NULL,
					target TEXT CHECK(target IN (\{toSQLList(NodeType.class)})),
					code TEXT NOT NULL,
					lastUpdate TEXT NOT NULL,
					compileLog TEXT NULL,
					hex BLOB NULL,
					status TEXT CHECK(status IN(\{toSQLList(FlashEvent.Status.class)})),
					FOREIGN KEY(teamID) REFERENCES teams(id)
				                                 )""");
			statement.execute("CREATE UNIQUE INDEX IF NOT EXISTS flashEventIndex ON flashEvents(id)");
			statement.execute("CREATE UNIQUE INDEX IF NOT EXISTS flashEventIndex ON flashEvents(teamID, idx)");
		}
	}

	private static String toSQLList(Class<? extends Enum<?>> enumClass) {
		return Arrays.stream(enumClass.getEnumConstants())
			.map(Enum::name)
			.map(name -> "'" + name + "'")
			.collect(Collectors.joining(", "));
	}

	@Override
	public void addTeam(String teamName) {
		try (var insert = sqlKeys."INSERT INTO teams (name) VALUES (\{teamName})") {
			insert.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public OptionalInt findTeamID(String teamName) {
		try (var query = sql."SELECT id FROM teams WHERE name = \{teamName}";
		     var result = query.executeQuery()) {
			if (result.next()) {
				return OptionalInt.of(result.getInt(1));
			} else {
				return OptionalInt.empty();
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Optional<String> findTeamName(int teamID) {
		try (var query = sql."SELECT name FROM teams WHERE id = \{teamID}";
		     var result = query.executeQuery()) {
			if (result.next()) {
				return Optional.of(result.getString(1));
			} else {
				return Optional.empty();
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<TeamPrincipal> getAllTeams() {
		try (var query = connection.createStatement();
		     var result = query.executeQuery("SELECT id, name FROM teams")) {
			var teams = new ArrayList<TeamPrincipal>();
			while (result.next()) {
				teams.add(new TeamPrincipal(result.getString(2), result.getInt(1)));
			}

			return Collections.unmodifiableList(teams);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Schematic[] schematicHistory(int teamID) {
		try (var query = sql."SELECT id, idx, target, pdfData, uploaded FROM schematics WHERE teamID = \{teamID} ORDER BY idx ASC";
		     var result = query.executeQuery()) {
			var schematics = new ArrayList<Schematic>();
			while (result.next()) {
				schematics.add(new Schematic(
					result.getInt(1),
					result.getInt(2),
					teamID,
					NodeType.valueOf(result.getString(3)),
					MemorySegment.ofArray(result.getBinaryStream(4).readAllBytes()),
					Instant.parse(result.getString(5))
				));
			}

			return schematics.toArray(Schematic[]::new);
		} catch (IOException | SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Optional<Schematic> getSchematic(int teamID, int index) {
		try (var query = sql."SELECT id, target, pdfData, uploaded FROM schematics WHERE teamID = \{teamID} AND idx = \{index}";
		     var result = query.executeQuery()) {
			if (result.next()) {
				return Optional.of(new Schematic(
					result.getInt(1),
					index,
					teamID,
					NodeType.valueOf(result.getString(2)),
					MemorySegment.ofArray(result.getBinaryStream(3).readAllBytes()),
					Instant.parse(result.getString(4))
				));
			} else {
				return Optional.empty();
			}
		} catch (IOException | SQLException e) {
			throw new RuntimeException(e);
		}
	}

	private Optional<Schematic> getSchematic(int id) {
		try (var query = sql."SELECT idx, teamID, target, pdfData, uploaded FROM schematics WHERE id = \{id}";
		     var result = query.executeQuery()) {
			if (result.next()) {
				return Optional.of(new Schematic(
					id,
					result.getInt(1),
					result.getInt(2),
					NodeType.valueOf(result.getString(3)),
					MemorySegment.ofArray(result.getBinaryStream(4).readAllBytes()),
					Instant.parse(result.getString(5))
				));
			} else {
				return Optional.empty();
			}
		} catch (IOException | SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public FlashEvent[] flashingHistory(int teamID) {
		try (var query = sql."SELECT id, idx, target, code, lastUpdate, compileLog, hex, status FROM flashEvents WHERE teamID = \{teamID} ORDER BY idx ASC";
		     var result = query.executeQuery()) {
			var events = new ArrayList<FlashEvent>();
			while (result.next()) {
				events.add(new FlashEvent(
					result.getInt(1),
					result.getInt(2),
					teamID,
					NodeType.valueOf(result.getString(3)),
					new Code(result.getString(4)),
					Instant.parse(result.getString(5)),
					result.getString(6),
					result.getBytes(7),
					FlashEvent.Status.valueOf(result.getString(8))
				));
			}

			return events.toArray(FlashEvent[]::new);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Optional<FlashEvent> getFlashEvent(long id) {
		try (var query = sql."SELECT idx, teamID, target, code, lastUpdate, compileLog, hex, status FROM flashEvents WHERE id = \{id}";
		     var result = query.executeQuery()) {
			if (result.next()) {
				return Optional.of(new FlashEvent(
					id,
					result.getInt(1),
					result.getInt(2),
					NodeType.valueOf(result.getString(3)),
					new Code(result.getString(4)),
					Instant.parse(result.getString(5)),
					result.getString(6),
					result.getBytes(7),
					FlashEvent.Status.valueOf(result.getString(8))
				));
			} else {
				return Optional.empty();
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Schematic updateSchematic(int teamID, NodeType target, MemorySegment pdfData, Instant uploaded) {
		try (var insert = sqlKeys."""
				INSERT INTO schematics (idx, teamID, target, pdfData, uploaded) VALUES (
								(SELECT COUNT(*) FROM schematics WHERE teamID = \{teamID}),
								\{teamID},
								\{target.name()},
								\{pdfData.toArray(JAVA_BYTE)},
								\{uploaded.toString()}
				)""") {
			insert.executeUpdate();
			try (var keys = insert.getGeneratedKeys()) {
				keys.next();
				int id = keys.getInt(1);
				return getSchematic(id).orElseThrow();
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public long beginFlashing(int teamID, NodeType target, Code code) {
		try (var insert = sqlKeys."""
				INSERT INTO flashEvents (idx, teamID, target, code, lastUpdate, status) VALUES (
								(SELECT COUNT(*) FROM flashEvents WHERE teamID = \{teamID}),
								\{teamID},
								\{target.name()},
								\{code.toString()},
								\{Instant.now().toString()},
								\{FlashEvent.Status.BUILDING.name()}
				)""") {
			insert.executeUpdate();
			try (var keys = insert.getGeneratedKeys()) {
				keys.next();
				return keys.getInt(1);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void updateFlashStatus(long id, FlashEvent.Status status) {
		try (var update = sql."UPDATE flashEvents SET status = \{status.name()} WHERE id = \{id}") {
			update.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void setFlashCompilerOutput(long id, String compileLog, byte[] hex) {
		try (var update = sql."UPDATE flashEvents SET compileLog = \{compileLog}, hex = \{hex} WHERE id = \{id}") {
			update.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	private final StringTemplate.Processor<PreparedStatement, SQLException> sql = tpl -> sql(tpl, false);
	private final StringTemplate.Processor<PreparedStatement, SQLException> sqlKeys = tpl -> sql(tpl, true);

	private PreparedStatement sql(StringTemplate template, boolean keys) throws SQLException {
		var sb = new StringBuilder();
		var fragments = template.fragments();
		for (int i = 0; i < fragments.size(); i++) {
			sb.append(fragments.get(i));
			if (i < fragments.size() - 1)
				sb.append("?");
		}

		var values = template.values();
		PreparedStatement statement;
		if (keys)
			statement = connection.prepareStatement(sb.toString(), PreparedStatement.RETURN_GENERATED_KEYS);
		else
			statement = connection.prepareStatement(sb.toString());

		for (int i = 0; i < values.size(); i++) {
			statement.setObject(i + 1, values.get(i));
		}

		return statement;
	}
}
