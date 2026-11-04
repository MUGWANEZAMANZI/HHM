package com.hhm.util;

import com.hhm.model.*;
import com.hhm.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Seeds the database with realistic demo data on first boot.
 * Skips everything if any user already exists.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository       userRepository;
    private final AdminRepository      adminRepository;
    private final DepartmentRepository departmentRepository;
    private final DoctorRepository     doctorRepository;
    private final PatientRepository    patientRepository;
    private final PatientAddressRepository     addressRepository;
    private final AppointmentRepository        appointmentRepository;
    private final PatientFeedbackRepository    feedbackRepository;
    private final PrescriptionRepository       prescriptionRepository;
    private final PrescriptionItemRepository   prescriptionItemRepository;
    private final MedicalInventoryRepository   inventoryRepository;
    private final MedicalItemRepository        medicalItemRepository;
    private final PasswordEncoder              passwordEncoder;

    public DataSeeder(
            UserRepository userRepository,
            AdminRepository adminRepository,
            DepartmentRepository departmentRepository,
            DoctorRepository doctorRepository,
            PatientRepository patientRepository,
            PatientAddressRepository addressRepository,
            AppointmentRepository appointmentRepository,
            PatientFeedbackRepository feedbackRepository,
            PrescriptionRepository prescriptionRepository,
            PrescriptionItemRepository prescriptionItemRepository,
            MedicalInventoryRepository inventoryRepository,
            MedicalItemRepository medicalItemRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository            = userRepository;
        this.adminRepository           = adminRepository;
        this.departmentRepository      = departmentRepository;
        this.doctorRepository          = doctorRepository;
        this.patientRepository         = patientRepository;
        this.addressRepository         = addressRepository;
        this.appointmentRepository     = appointmentRepository;
        this.feedbackRepository        = feedbackRepository;
        this.prescriptionRepository    = prescriptionRepository;
        this.prescriptionItemRepository = prescriptionItemRepository;
        this.inventoryRepository       = inventoryRepository;
        this.medicalItemRepository     = medicalItemRepository;
        this.passwordEncoder           = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) return;   // already seeded

        List<Department>       depts    = seedDepartments();
        List<Doctor>           doctors  = seedDoctors(depts);
        seedNurses();
        seedReceptionists();
        List<Patient>          patients = seedPatients();
        List<MedicalInventory> meds     = seedInventory();
        seedAdmin();
        List<Appointment>      appointments = seedAppointments(patients, doctors);
        seedFeedback(patients);
        seedPrescriptions(appointments, doctors, meds);
    }

    // ─── Departments ────────────────────────────────────────────────────────────

    private List<Department> seedDepartments() {
        String[] names = {
            "Cardiology", "Neurology", "Orthopedics", "Pediatrics", "Oncology",
            "Dermatology", "Psychiatry", "Emergency Medicine", "Radiology", "General Medicine"
        };
        List<Department> saved = new ArrayList<>();
        for (String name : names) saved.add(departmentRepository.save(new Department(name)));
        return saved;
    }

    // ─── Admin ──────────────────────────────────────────────────────────────────

    private void seedAdmin() {
        Admin admin = new Admin();
        admin.setName("System Admin");
        admin.setEmail("admin@hhm.rw");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRegisteredAt(LocalDateTime.now().minusMonths(6));
        adminRepository.save(admin);
    }

    // ─── Doctors ────────────────────────────────────────────────────────────────

    private static final String[][] DOCTOR_DATA = {
        {"Dr. Emmanuel Habimana",     "LIC-CARD-001"},
        {"Dr. Amina Uwimana",         "LIC-NEUR-001"},
        {"Dr. Patrick Niyonzima",     "LIC-ORTH-001"},
        {"Dr. Grace Mukamana",        "LIC-PED-001"},
        {"Dr. Jean-Pierre Habineza",  "LIC-ONC-001"},
        {"Dr. Diane Ingabire",        "LIC-DERM-001"},
        {"Dr. Charles Nzabonimana",   "LIC-PSY-001"},
        {"Dr. Solange Mukandutiye",   "LIC-EMER-001"},
        {"Dr. Bernard Niyomugabo",    "LIC-RAD-001"},
        {"Dr. Marie Uwase",           "LIC-GEN-001"},
    };

    private List<Doctor> seedDoctors(List<Department> depts) {
        List<Doctor> saved = new ArrayList<>();
        for (int i = 0; i < DOCTOR_DATA.length; i++) {
            Doctor d = new Doctor();
            d.setName(DOCTOR_DATA[i][0]);
            d.setLicenseNumber(DOCTOR_DATA[i][1]);
            d.setDepartment(depts.get(i));
            d.setEmail("doctor" + (i + 1) + "@hhm.rw");
            d.setPassword(passwordEncoder.encode("doctor123"));
            d.setRegisteredAt(LocalDateTime.now().minusMonths(5));
            saved.add(doctorRepository.save(d));
        }
        return saved;
    }

    // ─── Nurses ─────────────────────────────────────────────────────────────────

    private static final String[] NURSE_NAMES = {
        "Alice Mukansanga", "Betty Uwimana", "Clarisse Nzeyimana", "Doris Habimana",
        "Esther Mukandayisenga", "Florence Ingabire", "Goretti Uwamariya", "Henriette Nyirahabimana",
        "Immaculée Mukamana", "Josephine Uwase", "Keza Mukarugwiro", "Laetitia Nduwimana",
        "Marceline Uwimana", "Nadine Muhimpundu", "Odette Nyiramahoro", "Pascasie Mukabera",
        "Quirine Uwineza", "Rose Mukamwezi", "Sylvie Nsengimana", "Therese Mukamazera",
        "Umulisa Umuhoza", "Valerie Murerwa", "Winnie Umutesi", "Xavera Nyiranshimiyimana",
        "Yvette Habiyaremye", "Zawadi Mukamurera", "Angele Umubyeyi", "Beatrice Mukamugema",
        "Celestine Nyiraneza", "Delphine Uwimana"
    };

    private void seedNurses() {
        for (int i = 0; i < NURSE_NAMES.length; i++) {
            Nurse n = new Nurse();
            n.setNurseName(NURSE_NAMES[i]);
            n.setEmail("nurse" + (i + 1) + "@hhm.rw");
            n.setPassword(passwordEncoder.encode("nurse123"));
            n.setRegisteredAt(LocalDateTime.now().minusMonths(4));
            userRepository.save(n);
        }
    }

    // ─── Receptionists ──────────────────────────────────────────────────────────

    private void seedReceptionists() {
        String[][] data = {
            {"Marie Claire Uwimana", "receptionist1@hhm.rw"},
            {"Jean Paul Habimana",   "receptionist2@hhm.rw"},
        };
        for (String[] row : data) {
            Receptionist r = new Receptionist();
            r.setReceptionistName(row[0]);
            r.setEmail(row[1]);
            r.setPassword(passwordEncoder.encode("recep123"));
            r.setRegisteredAt(LocalDateTime.now().minusMonths(3));
            userRepository.save(r);
        }
    }

    // ─── Patients ────────────────────────────────────────────────────────────────

    private static final String[] PATIENT_FIRST = {
        "Alain","Beatrice","Celestin","Diane","Eric","Francine","Gilles","Honorine",
        "Ivan","Joselyne","Kevin","Laetitia","Marc","Nadine","Olivier","Pascale",
        "Quentin","Rosine","Samuel","Therese","Urbain","Valerie","William","Xavera",
        "Yves","Zozo","Andre","Brigitte","Claude","Denise","Emile","Felicite",
        "Gaston","Helene","Isidor","Jeanne","Karl","Lucie","Martin","Noel",
        "Oscar","Patricia","Remy","Solange","Tito","Umulisa","Vincent","Wanda",
        "Xavier","Yvonne","Alexis","Bernadette","Christophe","Delphine","Eugene","Fabrice",
        "Gertrude","Hyacinth","Irene","Jules","Killian","Leonie","Mathieu","Nadia",
        "Orphe","Pauline","Raoul","Sylvie","Thomas","Ursuline","Victor","Winnie",
        "Xanthi","Yasmine","Zacharie","Adele","Bruno","Carine","Didier","Edna",
        "Fabian","Gisele","Herve","Isabelle","Jerome","Ketsia","Laurent","Margot",
        "Nicolas","Ophelia","Pierre","Rachelle","Serge","Tania","Ulrich","Vanessa",
        "Walter","Xenia","Yannick","Zita"
    };

    private static final String[] SURNAMES = {
        "Habimana","Uwimana","Niyonzima","Mukamana","Habineza","Ingabire","Nzabonimana",
        "Mukandutiye","Niyomugabo","Uwase","Nsengimana","Mukamazera","Muhimpundu",
        "Nyiramahoro","Mukabera","Uwineza","Mukamwezi","Murerwa","Umutesi","Habiyaremye",
        "Mukamurera","Umubyeyi","Mukamugema","Nyiraneza","Munyeshuri","Nduwimana",
        "Uwimana","Mukarugwiro","Uwamariya","Nyirahabimana","Kayitesi","Nshimiyimana",
        "Rurangwa","Bizimana","Hakizimana","Ntakirutimana","Nkurunziza","Kabera",
        "Uwera","Karangwa","Gasana","Mugiraneza","Nzeyimana","Hirwa","Ndabaga",
        "Kampire","Gahima","Sebera","Twagira","Ndagijimana"
    };

    private static final String[] DISTRICTS = {
        "Gasabo", "Kicukiro", "Nyarugenge", "Burera", "Gicumbi", "Gakenke", "Rulindo",
        "Musanze", "Nyabihu", "Rubavu", "Nyamasheke", "Rusizi", "Karongi", "Ngororero",
        "Rutsiro", "Ruhango", "Muhanga", "Nyanza", "Kamonyi", "Nyamagabe"
    };

    private static final String[] SECTORS = {
        "Remera", "Kacyiru", "Kimironko", "Gisozi", "Ndera", "Jabana",
        "Nyarugunga", "Gahanga", "Niboye", "Kanombe"
    };

    private List<Patient> seedPatients() {
        List<Patient> saved = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            String first   = PATIENT_FIRST[i % PATIENT_FIRST.length];
            String surname = SURNAMES[i % SURNAMES.length];
            String email   = "patient" + (i + 1) + "@hhm.rw";
            String phone   = String.format("078%07d", i + 1);

            Patient p = new Patient();
            p.setName(first + " " + surname);
            p.setPhone(phone);
            p.setEmail(email);
            p.setPassword(passwordEncoder.encode("patient123"));
            p.setRegisteredAt(LocalDateTime.now().minusDays(180 - i));
            Patient sp = patientRepository.save(p);

            // Give every patient an address
            PatientAddress addr = new PatientAddress(
                sp,
                DISTRICTS[i % DISTRICTS.length],
                SECTORS[i % SECTORS.length],
                "Cell " + (i % 5 + 1),
                "Village " + (i % 8 + 1)
            );
            addressRepository.save(addr);
            saved.add(sp);
        }
        return saved;
    }

    // ─── Medical Inventory ───────────────────────────────────────────────────────

    private static final String[][] INVENTORY_DATA = {
        {"Amoxicillin 500mg",      "MED-AMX-500", "Antibiotics"},
        {"Paracetamol 500mg",      "MED-PCM-500", "Analgesics"},
        {"Ibuprofen 400mg",        "MED-IBU-400", "Anti-inflammatories"},
        {"Metformin 850mg",        "MED-MET-850", "Diabetes"},
        {"Amlodipine 5mg",         "MED-AML-005", "Cardiovascular"},
        {"Omeprazole 20mg",        "MED-OMP-020", "Gastrointestinal"},
        {"Salbutamol Inhaler",     "MED-SAL-INH", "Respiratory"},
        {"Chloroquine 250mg",      "MED-CLQ-250", "Antimalarials"},
        {"Diclofenac 50mg",        "MED-DCL-050", "Analgesics"},
        {"Cotrimoxazole 960mg",    "MED-CTX-960", "Antibiotics"},
    };

    private static final String[][] ITEM_CATEGORIES = {
        {"Tablets", "Capsules", "Syrup"},
        {"Tablets", "Suspension"},
        {"Tablets", "Gel", "Capsules"},
        {"Tablets", "Extended-release"},
        {"Tablets", "Injection"},
        {"Capsules", "Powder for suspension"},
        {"Inhaler", "Nebuliser solution"},
        {"Tablets", "Syrup", "Injection"},
        {"Tablets", "Gel", "Injection"},
        {"Tablets", "Suspension"},
    };

    private List<MedicalInventory> seedInventory() {
        List<MedicalInventory> saved = new ArrayList<>();
        for (int i = 0; i < INVENTORY_DATA.length; i++) {
            MedicalInventory inv = new MedicalInventory(
                INVENTORY_DATA[i][0],
                INVENTORY_DATA[i][1],
                200 + i * 30,  // quantityOnHand
                50             // reorderLevel
            );
            inv = inventoryRepository.save(inv);
            saved.add(inv);

            String[] itemCategories = ITEM_CATEGORIES[i];
            for (String cat : itemCategories) {
                medicalItemRepository.save(new MedicalItem(inv, 50 + i * 10, cat));
            }
        }
        return saved;
    }

    // ─── Appointments ────────────────────────────────────────────────────────────

    private List<Appointment> seedAppointments(List<Patient> patients, List<Doctor> doctors) {
        List<Appointment> saved = new ArrayList<>();
        LocalDateTime base = LocalDateTime.now();
        int patCount = patients.size();
        int docCount = doctors.size();

        // 60 completed (past)
        for (int i = 0; i < 60; i++) {
            Appointment a = new Appointment(
                patients.get(i % patCount),
                doctors.get(i % docCount),
                base.minusDays(90 - i),
                "Completed"
            );
            saved.add(appointmentRepository.save(a));
        }

        // 40 cancelled (past)
        for (int i = 0; i < 40; i++) {
            Appointment a = new Appointment(
                patients.get((i + 10) % patCount),
                doctors.get((i + 3) % docCount),
                base.minusDays(60 - i),
                "Cancelled"
            );
            saved.add(appointmentRepository.save(a));
        }

        // 50 scheduled (future)
        for (int i = 0; i < 50; i++) {
            Appointment a = new Appointment(
                patients.get((i + 50) % patCount),
                doctors.get(i % docCount),
                base.plusDays(i + 1),
                "Scheduled"
            );
            saved.add(appointmentRepository.save(a));
        }

        return saved;
    }

    // ─── Feedback ────────────────────────────────────────────────────────────────

    private static final String[] FEEDBACK_TEXTS = {
        "The doctor was very attentive and explained everything clearly.",
        "Great service, I felt comfortable throughout my visit.",
        "Waiting time was a bit long but the care was excellent.",
        "Very professional staff. The doctor answered all my questions.",
        "I am satisfied with the treatment I received.",
        "The nurse was kind and gentle during the procedure.",
        "Excellent diagnosis. I am recovering well thanks to the team.",
        "The hospital is clean and well organized.",
        "Doctor took time to listen to my concerns. Very grateful.",
        "Good experience overall. Would recommend this facility.",
        "The prescription was explained thoroughly. Thank you.",
        "Staff was friendly and made me feel at ease.",
        "Fast and efficient service despite how busy the clinic was.",
        "I appreciated the follow-up call after my appointment.",
        "The treatment plan is working. I feel much better now.",
    };

    private void seedFeedback(List<Patient> patients) {
        LocalDateTime base = LocalDateTime.now();
        for (int i = 0; i < 60; i++) {
            String text  = FEEDBACK_TEXTS[i % FEEDBACK_TEXTS.length];
            String notes = (i % 3 == 0) ? "Patient expressed high satisfaction. No concerns." : null;
            PatientFeedback fb = new PatientFeedback(
                patients.get(i % patients.size()),
                text,
                notes,
                base.minusDays(80 - i)
            );
            feedbackRepository.save(fb);
        }
    }

    // ─── Prescriptions + Items ────────────────────────────────────────────────────

    private static final String[]   DOSAGES    = {"250mg", "500mg", "10mg", "20mg", "5mg", "850mg", "400mg"};
    private static final String[]   FREQUENCIES = {"Once daily", "Twice daily", "Three times daily", "Every 8 hours", "As needed"};
    private static final String[]   STATUSES   = {"Active", "Active", "Active", "Completed", "Cancelled"};

    private void seedPrescriptions(List<Appointment> appointments,
                                   List<Doctor> doctors,
                                   List<MedicalInventory> meds) {
        // Only issue prescriptions for the completed appointments (first 60)
        List<Appointment> completed = appointments.stream()
            .filter(a -> "Completed".equals(a.getStatus()))
            .toList();

        LocalDateTime base = LocalDateTime.now();
        for (int i = 0; i < completed.size(); i++) {
            Appointment appt = completed.get(i);
            Prescription rx = new Prescription(
                appt.getPatient(),
                doctors.get(i % doctors.size()),
                base.minusDays(80 - i),
                STATUSES[i % STATUSES.length],
                (i % 4 == 0) ? "Take with food. Avoid alcohol." : null
            );
            rx = prescriptionRepository.save(rx);

            // 1–3 items per prescription
            int itemCount = (i % 3) + 1;
            for (int j = 0; j < itemCount; j++) {
                MedicalInventory med = meds.get((i + j) % meds.size());
                prescriptionItemRepository.save(new PrescriptionItem(
                    rx,
                    med,
                    DOSAGES[j % DOSAGES.length],
                    FREQUENCIES[j % FREQUENCIES.length],
                    (j + 1) * 10,
                    j % 3
                ));
            }
        }
    }
}
