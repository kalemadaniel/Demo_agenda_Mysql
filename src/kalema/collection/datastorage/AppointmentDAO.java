package kalema.collection.datastorage;

import kalema.collection.domain.Appointment;
import java.sql.Time;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;

/** Adapter retained for the original calendar panels. */
public class AppointmentDAO {
    private final AgendaRepository repository = new AgendaRepository();
    public ArrayList<Appointment> getAppointments(Date date) {
        ArrayList<Appointment> result = new ArrayList<>();
        if (date == null) return result;
        LocalDate day = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        try {
            for (AgendaRepository.Event e : repository.load(day, day))
                result.add(new Appointment(e.id,e.title,e.notes,e.location,date,Time.valueOf(e.start),Time.valueOf(e.end)));
            return result;
        } catch (Exception e) { throw new IllegalStateException("Lecture MySQL impossible",e); }
    }
    public Boolean addAppointment(Date date, String title, String description, String location, Time start, Time end) {
        try {
            AgendaRepository.Event e = new AgendaRepository.Event();
            e.date=date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            e.title=title; e.notes=description==null?"":description; e.location=location==null?"":location;
            e.start=start.toLocalTime(); e.end=end.toLocalTime(); repository.save(e); return true;
        } catch (Exception e) { throw new IllegalStateException("Enregistrement MySQL impossible",e); }
    }
    public boolean deleteAppointment(Integer id) {
        try { repository.delete(id); return true; }
        catch(Exception e) { throw new IllegalStateException("Suppression MySQL impossible",e); }
    }
}
