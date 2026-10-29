package com.hhm.graphql;

import com.hhm.model.Appointment;
import com.hhm.model.AppointmentView;
import com.hhm.service.AppointmentService;
import com.hhm.service.ServiceException;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.List;

@Controller
public class AppointmentResolver {

    private final AppointmentService appointmentService;

    public AppointmentResolver(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @QueryMapping
    public List<AppointmentView> appointments(@Argument String search, @Argument String status) {
        List<AppointmentView> all = appointmentService.getAll();

        if (status != null && !status.isBlank()) {
            all = all.stream()
                    .filter(a -> status.equalsIgnoreCase(a.getStatus()))
                    .toList();
        }

        if (search != null && !search.isBlank()) {
            String lower = search.toLowerCase();
            all = all.stream()
                    .filter(a -> (a.getPatientName() != null && a.getPatientName().toLowerCase().contains(lower))
                            || (a.getDoctorName() != null && a.getDoctorName().toLowerCase().contains(lower)))
                    .toList();
        }

        return all;
    }

    @QueryMapping
    public AppointmentView appointment(@Argument Long id) {
        return appointmentService.getAll().stream()
                .filter(a -> id != null && a.getId() == id.longValue())
                .findFirst()
                .orElse(null);
    }

    @MutationMapping
    public String bookAppointment(
            @Argument Long patientId,
            @Argument Long doctorId,
            @Argument String appointmentDate) {
        try {
            LocalDateTime date = LocalDateTime.parse(appointmentDate);
            Appointment a = new Appointment(patientId, doctorId, date, "Scheduled");
            appointmentService.book(a);
            return "Appointment booked successfully";
        } catch (ServiceException e) {
            throw new RuntimeException(e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Invalid date format. Use ISO 8601: 2026-11-15T09:30:00");
        }
    }

    @MutationMapping
    public boolean cancelAppointment(@Argument Long id) {
        try {
            appointmentService.cancel(id);
            return true;
        } catch (ServiceException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @MutationMapping
    public boolean deleteAppointment(@Argument Long id) {
        try {
            appointmentService.remove(id);
            return true;
        } catch (ServiceException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
