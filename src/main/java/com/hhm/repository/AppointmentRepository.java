package com.hhm.repository;

import com.hhm.model.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // Derived queries
    List<Appointment> findByPatientId(long patientId);
    List<Appointment> findByDoctorId(long doctorId);
    List<Appointment> findByStatus(String status);

    // JPQL — appointments by status sorted by date descending
    @Query("SELECT a FROM Appointment a WHERE a.status = :status ORDER BY a.appointmentDate DESC")
    List<Appointment> findByStatusSorted(@Param("status") String status);

    // Native SQL — full JOIN view with patient, doctor, department names
    @Query(value = """
            SELECT a.id, a.patient_id, a.doctor_id, a.appointment_date, a.status,
                   p.name  AS patient_name,  p.phone AS patient_phone,
                   d.name  AS doctor_name,
                   dept.name AS department_name
            FROM appointment a
            JOIN patient     p    ON a.patient_id    = p.id
            JOIN doctor      d    ON a.doctor_id     = d.id
            JOIN department  dept ON d.department_id = dept.id
            """, nativeQuery = true)
    List<Object[]> findAllWithDetails();

    // Pagination
    Page<Appointment> findAll(Pageable pageable);
    Page<Appointment> findByPatientId(long patientId, Pageable pageable);
}
