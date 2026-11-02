package kalema.collection.datastorage;

import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;

/** MySQL persistence. Errors propagate to the UI; no silent fallback. */
public final class AgendaRepository {
    public static final class Event {
        public int id;
        public String title, location, notes;
        public LocalDate date;
        public LocalTime start, end;
    }
    private Connection connect() throws Exception {
        Properties p = new Properties();
        Path path = Paths.get(System.getProperty("agenda.config", "database.properties"));
        if (Files.exists(path)) {
            try (InputStream in = Files.newInputStream(path)) { p.load(in); }
        }
        String url = setting(p, "url", "AGENDA_DB_URL", "jdbc:mysql://localhost:3306/java_calendar?connectTimeout=4000&socketTimeout=8000&useUnicode=true&characterEncoding=UTF-8");
        return DriverManager.getConnection(url, setting(p,"user","AGENDA_DB_USER","root"), setting(p,"password","AGENDA_DB_PASSWORD",""));
    }
    private String setting(Properties p, String key, String env, String fallback) {
        String value = System.getenv(env);
        return value == null ? p.getProperty(key, fallback) : value;
    }
    public java.util.List<Event> load(LocalDate first, LocalDate last) throws Exception {
        java.util.List<Event> events = new ArrayList<>();
        try (Connection c = connect(); PreparedStatement s = c.prepareStatement(
                "SELECT id,title,description,location,date,startTime,endTime FROM appointment WHERE date >= ? AND date <= ? ORDER BY date,startTime,id")) {
            s.setDate(1, java.sql.Date.valueOf(first)); s.setDate(2, java.sql.Date.valueOf(last));
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) {
                    Event e = new Event(); e.id = r.getInt("id"); e.title = r.getString("title");
                    e.notes = r.getString("description"); e.location = r.getString("location");
                    e.date = r.getDate("date").toLocalDate();
                    e.start = r.getTime("startTime").toLocalTime(); e.end = r.getTime("endTime").toLocalTime();
                    events.add(e);
                }
            }
        }
        return events;
    }
    public void save(Event e) throws Exception {
        if (e.title == null || e.title.trim().isEmpty() || e.title.length() > 200 || e.location.length() > 255
                || e.notes.length() > 10000 || e.date == null || !e.end.isAfter(e.start))
            throw new IllegalArgumentException("Vérifiez le titre, la date et les horaires.");
        boolean insert = e.id == 0;
        String sql = insert ? "INSERT INTO appointment (title,description,location,date,startTime,endTime) VALUES (?,?,?,?,?,?)"
                : "UPDATE appointment SET title=?,description=?,location=?,date=?,startTime=?,endTime=? WHERE id=?";
        try (Connection c = connect(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, e.title.trim()); s.setString(2, e.notes); s.setString(3, e.location);
            s.setDate(4, java.sql.Date.valueOf(e.date)); s.setTime(5, Time.valueOf(e.start)); s.setTime(6, Time.valueOf(e.end));
            if (!insert) s.setInt(7, e.id);
            if (s.executeUpdate() != 1) throw new SQLException("Le rendez-vous n'a pas été enregistré (ou n'existe plus).");
        }
    }
    public void delete(int id) throws Exception {
        try (Connection c = connect(); PreparedStatement s = c.prepareStatement("DELETE FROM appointment WHERE id=?")) {
            s.setInt(1, id);
            if (s.executeUpdate() != 1) throw new SQLException("Ce rendez-vous n'existe plus. Actualisez l'agenda.");
        }
    }
}
