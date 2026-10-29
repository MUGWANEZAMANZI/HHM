-- ============================================================
-- data.sql — Seed data for HHM system
-- Table/column names match Hibernate SpringPhysicalNamingStrategy
-- (lowercase snake_case). Run AFTER Hibernate creates tables.
-- ============================================================

-- Departments
INSERT IGNORE INTO department (name) VALUES
('Cardiology'),
('Neurology'),
('Pediatrics'),
('Oncology'),
('Orthopedics'),
('Dermatology'),
('Gastroenterology'),
('Endocrinology'),
('Psychiatry'),
('Ophthalmology');

-- Users (backtick-quoted because 'user' is a MySQL reserved word)
INSERT IGNORE INTO `user` (email, password, registered_at) VALUES
('admin@hhm.rw',       '$2a$10$abcdefghij1234567890ab', '2024-01-10 08:00:00'),
('reception@hhm.rw',   '$2a$10$abcdefghij1234567890ac', '2024-01-11 08:30:00'),
('nurse1@hhm.rw',      '$2a$10$abcdefghij1234567890ad', '2024-02-01 09:00:00'),
('pharmacist@hhm.rw',  '$2a$10$abcdefghij1234567890ae', '2024-02-15 10:00:00'),
('manager@hhm.rw',     '$2a$10$abcdefghij1234567890af', '2024-03-01 07:45:00');

-- Patients
INSERT IGNORE INTO patient (name, address, phone, email) VALUES
('Uwimana Chantal',      'Kigali, Gasabo',       '0788100001', 'chantal.uwimana@gmail.com'),
('Hakizimana Jean',      'Kigali, Kicukiro',     '0788100002', 'jean.hakizimana@gmail.com'),
('Mukamana Esperance',   'Huye, Ngoma',          '0788100003', 'esperance.mukamana@gmail.com'),
('Niyonzima Emmanuel',   'Musanze, Muhoza',      '0788100004', 'emmanuel.niyonzima@gmail.com'),
('Ingabire Alice',       'Rubavu, Gisenyi',      '0788100005', 'alice.ingabire@gmail.com'),
('Kamanzi Patrick',      'Nyanza, Nyanza',       '0788100006', 'patrick.kamanzi@gmail.com'),
('Mukamurenzi Rose',     'Rwamagana, Kigabiro',  '0788100007', 'rose.mukamurenzi@gmail.com'),
('Bizimana Claude',      'Kigali, Nyarugenge',   '0788100008', 'claude.bizimana@gmail.com'),
('Ntirenganya Olivier',  'Gicumbi, Byumba',      '0788100009', 'olivier.ntirenganya@gmail.com'),
('Uwase Beatrice',       'Bugesera, Ntarama',    '0788100010', 'beatrice.uwase@gmail.com');

-- Patient Addresses (patient_address — no space, snake_case)
INSERT IGNORE INTO patient_address (patient_id, district, sector, cell, village) VALUES
(1,  'Gasabo',     'Remera',    'Rukiri I',    'Amahoro'),
(2,  'Kicukiro',   'Gatenga',   'Gatenga',     'Inyana'),
(3,  'Huye',       'Ngoma',     'Cyarwa',      'Kabuye'),
(4,  'Musanze',    'Muhoza',    'Cyabararika', 'Kabusunzu'),
(5,  'Rubavu',     'Gisenyi',   'Umukamba',    'Kigali'),
(6,  'Nyanza',     'Nyanza',    'Cyabakamyi',  'Gitega'),
(7,  'Rwamagana',  'Kigabiro',  'Musha',       'Nyakarambi'),
(8,  'Nyarugenge', 'Nyamirambo','Cyivugiza',   'Agatego'),
(9,  'Gicumbi',    'Byumba',    'Mulindi',     'Karwana'),
(10, 'Bugesera',   'Ntarama',   'Ntarama',     'Kimisagara');

-- Doctors (department_id, license_number — snake_case)
INSERT IGNORE INTO doctor (name, department_id, license_number) VALUES
('Dr. Nkurunziza Samuel',   1, 'RW-MD-10001'),
('Dr. Uwimana Marie',       2, 'RW-MD-10002'),
('Dr. Habimana Bernard',    3, 'RW-MD-10003'),
('Dr. Mukagasana Lise',     4, 'RW-MD-10004'),
('Dr. Karekezi Theophile',  5, 'RW-MD-10005'),
('Dr. Nsanzimana Eric',     6, 'RW-MD-10006'),
('Dr. Munyakazi Francois',  7, 'RW-MD-10007'),
('Dr. Gahongayire Joelle',  8, 'RW-MD-10008'),
('Dr. Niyonzima Pascal',    9, 'RW-MD-10009'),
('Dr. Ingabire Solange',   10, 'RW-MD-10010');

-- Medical Inventory (quantity_on_hand, reorder_level — snake_case)
INSERT IGNORE INTO medical_inventory (name, sku, quantity_on_hand, reorder_level) VALUES
('Amoxicillin 500mg',        'MED-AMX-500',  200, 50),
('Paracetamol 1000mg',       'MED-PCT-100',  500, 100),
('Ibuprofen 400mg',          'MED-IBU-400',  300, 60),
('Metformin 850mg',          'MED-MET-850',  150, 40),
('Atenolol 50mg',            'MED-ATN-050',  120, 30),
('Omeprazole 20mg',          'MED-OMP-020',   80, 25),
('Surgical Gloves (box)',     'SUP-GLV-BOX',   40, 10),
('IV Normal Saline 1L',      'FLU-NS1-000',   60, 15),
('Bandage Roll 10cm',        'SUP-BND-010',   90, 20),
('Insulin Glargine 100U/mL', 'MED-INS-100',   35, 10);

-- Appointments (patient_id, doctor_id, appointment_date — snake_case)
INSERT IGNORE INTO appointment (patient_id, doctor_id, appointment_date, status) VALUES
(1,  1,  '2024-06-01 09:00:00', 'Completed'),
(2,  3,  '2024-06-03 10:30:00', 'Completed'),
(3,  5,  '2024-06-05 11:00:00', 'Cancelled'),
(4,  2,  '2024-06-10 08:30:00', 'Completed'),
(5,  4,  '2024-06-12 14:00:00', 'Rescheduled'),
(6,  7,  '2024-06-15 09:30:00', 'Completed'),
(7,  6,  '2024-06-18 13:00:00', 'Scheduled'),
(8,  8,  '2024-06-20 10:00:00', 'Scheduled'),
(9,  9,  '2024-06-22 11:30:00', 'Scheduled'),
(10, 10, '2024-06-25 15:00:00', 'Scheduled'),
(1,  1,  '2024-07-01 09:00:00', 'Scheduled'),
(2,  2,  '2024-07-05 10:00:00', 'Scheduled');

-- Prescriptions (patient_id, doctor_id, issued_at — snake_case)
INSERT IGNORE INTO prescription (patient_id, doctor_id, issued_at, status, notes) VALUES
(1,  1, '2024-06-01 09:45:00', 'Completed', 'Hypertension follow-up. Monitor blood pressure weekly.'),
(2,  3, '2024-06-03 11:00:00', 'Active',    'Childhood respiratory infection. Complete full course.'),
(4,  2, '2024-06-10 09:00:00', 'Active',    'Migraine management. Avoid triggers.'),
(6,  7, '2024-06-15 10:15:00', 'Active',    'Gastric ulcer treatment. Take omeprazole before meals.'),
(8,  8, '2024-06-20 10:30:00', 'Active',    'Type 2 diabetes. Metformin with meals. Monitor glucose daily.'),
(3,  5, '2024-05-20 11:00:00', 'Cancelled', 'Knee inflammation. Cancelled — patient requested second opinion.'),
(5,  4, '2024-05-28 14:30:00', 'Completed', 'Post-chemotherapy supportive care.'),
(9,  9, '2024-06-22 12:00:00', 'Active',    'Anxiety disorder. Counselling sessions scheduled.');

-- Patient Feedback
-- NOTE: column was 'passwordNotes' in old db.sql — renamed to 'private_notes' in entity
INSERT IGNORE INTO patient_feedback (patient_id, feedback, private_notes, issued_at) VALUES
(1,  'Excellent service from the cardiology team. Dr. Nkurunziza was very thorough.',  NULL,                           '2024-06-02 10:00:00'),
(2,  'The pediatrics ward is clean and the staff were very kind to my child.',           NULL,                           '2024-06-04 09:00:00'),
(4,  'Very professional. I got a clear explanation of my diagnosis.',                    'Patient prefers morning slots.','2024-06-11 08:00:00'),
(6,  'Wait time was long but the doctor was excellent.',                                 NULL,                           '2024-06-16 14:00:00'),
(8,  'Happy with the diabetes management plan. Easy to understand instructions.',        'Prefers SMS reminders.',        '2024-06-21 11:00:00'),
(10, 'Great ophthalmology consultation. Very helpful advice on eye care.',               NULL,                           '2024-06-26 16:00:00');

-- Medical Items (inventory_id — snake_case)
INSERT IGNORE INTO medical_item (inventory_id, quantity, category) VALUES
(1,  200, 'Antibiotic'),
(2,  500, 'Analgesic'),
(3,  300, 'Anti-inflammatory'),
(4,  150, 'Antidiabetic'),
(5,  120, 'Antihypertensive'),
(6,   80, 'Proton Pump Inhibitor'),
(7,   40, 'Medical Supply'),
(8,   60, 'IV Fluid'),
(9,   90, 'Medical Supply'),
(10,  35, 'Antidiabetic');

-- Prescription Items (prescription_id, medication_id, refills_allowed — snake_case)
INSERT IGNORE INTO prescription_item (prescription_id, medication_id, dosage, frequency, quantity, refills_allowed) VALUES
(1, 5, '50mg',    'Once daily',        30, 2),
(2, 1, '500mg',   'Three times daily', 21, 0),
(2, 2, '1000mg',  'Twice daily',       14, 0),
(3, 2, '1000mg',  'As needed',         10, 1),
(4, 6, '20mg',    'Once daily',        30, 2),
(5, 4, '850mg',   'Twice daily',       60, 3),
(5, 10,'100U/mL', 'Once daily',        30, 2),
(7, 3, '400mg',   'Twice daily',       14, 0),
(8, 2, '1000mg',  'Once daily',        30, 1);
