package com.graphql;

import com.model.Appointment;
import com.model.AppointmentView;
import com.service.AppointmentService;
import com.service.ServiceException;
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
        if (status != null && !status.isBlank())
            return appointmentService.findByStatus(status);
        if (search != null && !search.isBlank())
            return appointmentService.search(search);
        return appointmentService.getAll();
    }

    @QueryMapping
    public AppointmentView appointment(@Argument Long id) {
        return appointmentService.getAll().stream()
                .filter(a -> a.getId() == id)
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
