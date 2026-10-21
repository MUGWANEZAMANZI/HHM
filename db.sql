use hhm;

-- Create user database
CREATE TABLE `Users` (
    `Id` BIGINT AUTO_INCREMENT PRIMARY KEY NOT NULL,
    `email` VARCHAR(255) UNIQUE NOT NULL,
    `password` VARCHAR(255) NOT NULL,
    `registeredAt` DATETIME NOT NULL
);

-- Creating Departments table
CREATE TABLE `Departments` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY NOT NULL,
    `name` VARCHAR(120) UNIQUE NOT NULL
);

-- Creating Patients table
CREATE TABLE `Patient` (
    `Id` BIGINT AUTO_INCREMENT PRIMARY KEY NOT NULL,
    `Name` VARCHAR(160) NOT NULL,
    `Address` VARCHAR(255),
    `Phone` VARCHAR(40),
    `email` VARCHAR(255) UNIQUE
);

-- Creating Patient addresses table
CREATE TABLE `Patient Address` (
    `Id` BIGINT AUTO_INCREMENT NOT NULL,
    `patientId` BIGINT NOT NULL,
    `District` VARCHAR(120) NOT NULL,
    `Sector` VARCHAR(120) NOT NULL,
    `Cell` VARCHAR(120) NOT NULL,
    `Village` VARCHAR(120) NOT NULL,
    PRIMARY KEY (`Id`)
);

-- Creating Doctors table
CREATE TABLE `Doctors` (
    `Id` BIGINT AUTO_INCREMENT PRIMARY KEY NOT NULL,
    `Name` VARCHAR(200) NOT NULL,
    `departmentId` BIGINT NOT NULL,
    `licenseNumber` VARCHAR(80) UNIQUE NOT NULL
);

-- Creating Medical Inventory table
CREATE TABLE `MedicalInventory` (
    `Id` BIGINT AUTO_INCREMENT PRIMARY KEY NOT NULL,
    `name` VARCHAR(160) NOT NULL,
    `sku` VARCHAR(80) UNIQUE NOT NULL,
    `quantityOnHand` INT NOT NULL,
    `reorderLevel` INT NOT NULL
);

-- Creating Appointments table
CREATE TABLE `Appointments` (
    `Id` BIGINT AUTO_INCREMENT PRIMARY KEY NOT NULL,
    `patientId` BIGINT NOT NULL,
    `doctorId` BIGINT NOT NULL,
    `appointmentDate` DATETIME NOT NULL,
    `status` VARCHAR(30) NOT NULL
);

-- Creating Prescriptions table
CREATE TABLE `Prescriptions` (
    `Id` BIGINT AUTO_INCREMENT PRIMARY KEY NOT NULL,
    `patientId` BIGINT NOT NULL,
    `doctorId` BIGINT NOT NULL,
    `issuedAt` DATETIME NOT NULL,
    `status` VARCHAR(20) NOT NULL,
    `notes` TEXT
);

-- Creating Patient Feedback table
CREATE TABLE `PatientFeedback` (
    `Id` BIGINT AUTO_INCREMENT PRIMARY KEY NOT NULL,
    `patientId` BIGINT NOT NULL,
    `Feedback` TEXT NOT NULL,
    `passwordNotes` TEXT,
    `issuedAt` DATETIME NOT NULL
);

-- Creating Medical Items table
CREATE TABLE `MedicalItems` (
    `Id` BIGINT AUTO_INCREMENT PRIMARY KEY NOT NULL,
    `inventoryId` BIGINT NOT NULL,
    `quantity` INT NOT NULL,
    `category` VARCHAR(100)
);

-- Creating Prescription Items table
CREATE TABLE `PrescriptionItems` (
    `Id` BIGINT AUTO_INCREMENT PRIMARY KEY NOT NULL,
    `prescriptionId` BIGINT NOT NULL,
    `medicationId` BIGINT NOT NULL,
    `dosage` VARCHAR(50) NOT NULL,
    `frequency` VARCHAR(80) NOT NULL,
    `quantity` INT NOT NULL,
    `refillsAllowed` INT NOT NULL
);

-- Adding Foreign Key Relationships
ALTER TABLE `Doctors`
    ADD CONSTRAINT `fk_doctors_department`
        FOREIGN KEY (`departmentId`) REFERENCES `Departments` (`id`);

ALTER TABLE `Patient Address`
    ADD CONSTRAINT `fk_patientaddress_patient`
        FOREIGN KEY (`patientId`) REFERENCES `Patient` (`Id`);

ALTER TABLE `Appointments`
    ADD CONSTRAINT `fk_appointments_patient`
        FOREIGN KEY (`patientId`) REFERENCES `Patient` (`Id`),
    ADD CONSTRAINT `fk_appointments_doctor`
        FOREIGN KEY (`doctorId`) REFERENCES `Doctors` (`Id`);

ALTER TABLE `Prescriptions`
    ADD CONSTRAINT `fk_prescriptions_patient`
        FOREIGN KEY (`patientId`) REFERENCES `Patient` (`Id`),
    ADD CONSTRAINT `fk_prescriptions_doctor`
        FOREIGN KEY (`doctorId`) REFERENCES `Doctors` (`Id`);

ALTER TABLE `PatientFeedback`
    ADD CONSTRAINT `fk_patientfeedback_patient`
        FOREIGN KEY (`patientId`) REFERENCES `Patient` (`Id`);

ALTER TABLE `MedicalItems`
    ADD CONSTRAINT `fk_medicalitems_inventory`
        FOREIGN KEY (`inventoryId`) REFERENCES `MedicalInventory` (`Id`);

ALTER TABLE `PrescriptionItems`
    ADD CONSTRAINT `fk_prescriptionitems_prescription`
        FOREIGN KEY (`prescriptionId`) REFERENCES `Prescriptions` (`Id`),
    ADD CONSTRAINT `fk_prescriptionitems_medication`
        FOREIGN KEY (`medicationId`) REFERENCES `MedicalInventory` (`Id`);

-- Creating Indexes on high-frequency columns
CREATE INDEX `idx_patient_name`          ON `Patient`      (`Name`);
CREATE INDEX `idx_doctors_department`    ON `Doctors`      (`departmentId`);
CREATE INDEX `idx_appointments_date`     ON `Appointments` (`appointmentDate`);
CREATE INDEX `idx_appointments_patient`  ON `Appointments` (`patientId`);
CREATE INDEX `idx_appointments_doctor`   ON `Appointments` (`doctorId`);
CREATE INDEX `idx_prescriptions_patient` ON `Prescriptions`(`patientId`);



-- Seed data for Departments
INSERT INTO `Departments` (`name`) VALUES
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

SELECT * FROM `Departments`;

-- Seed data for Users
INSERT INTO `Users` (`email`, `password`, `registeredAt`) VALUES
('admin@hhm.rw',       '$2a$10$abcdefghij1234567890ab', '2024-01-10 08:00:00'),
('reception@hhm.rw',   '$2a$10$abcdefghij1234567890ac', '2024-01-11 08:30:00'),
('nurse1@hhm.rw',      '$2a$10$abcdefghij1234567890ad', '2024-02-01 09:00:00'),
('pharmacist@hhm.rw',  '$2a$10$abcdefghij1234567890ae', '2024-02-15 10:00:00'),
('manager@hhm.rw',     '$2a$10$abcdefghij1234567890af', '2024-03-01 07:45:00');

-- Seed data for Patient
INSERT INTO `Patient` (`Name`, `Address`, `Phone`, `email`) VALUES
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

-- Seed data for Patient Address (Rwandan administrative divisions)
INSERT INTO `Patient Address` (`patientId`, `District`, `Sector`, `Cell`, `Village`) VALUES
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

-- Seed data for Doctors (departmentId references Departments 1-10)
INSERT INTO `Doctors` (`Name`, `departmentId`, `licenseNumber`) VALUES
('Dr. Nkurunziza Samuel',   1, 'RW-MD-10001'),   -- Cardiology
('Dr. Uwimana Marie',       2, 'RW-MD-10002'),   -- Neurology
('Dr. Habimana Bernard',    3, 'RW-MD-10003'),   -- Pediatrics
('Dr. Mukagasana Lise',     4, 'RW-MD-10004'),   -- Oncology
('Dr. Karekezi Theophile',  5, 'RW-MD-10005'),   -- Orthopedics
('Dr. Nsanzimana Eric',     6, 'RW-MD-10006'),   -- Dermatology
('Dr. Munyakazi Francois',  7, 'RW-MD-10007'),   -- Gastroenterology
('Dr. Gahongayire Joelle',  8, 'RW-MD-10008'),   -- Endocrinology
('Dr. Niyonzima Pascal',    9, 'RW-MD-10009'),   -- Psychiatry
('Dr. Ingabire Solange',   10, 'RW-MD-10010');   -- Ophthalmology

-- Seed data for MedicalInventory
INSERT INTO `MedicalInventory` (`name`, `sku`, `quantityOnHand`, `reorderLevel`) VALUES
('Amoxicillin 500mg',        'MED-AMX-500',  200, 50),
('Paracetamol 1000mg',       'MED-PCT-100',  500, 100),
('Ibuprofen 400mg',          'MED-IBU-400',  300, 60),
('Metformin 850mg',          'MED-MET-850',  150, 40),
('Atenolol 50mg',            'MED-ATN-050',  120, 30),
('Omeprazole 20mg',          'MED-OMP-020',   80, 25),
('Surgical Gloves (box)',     'SUP-GLV-BOX',   40,  10),
('IV Normal Saline 1L',      'FLU-NS1-000',   60,  15),
('Bandage Roll 10cm',         'SUP-BND-010',   90,  20),
('Insulin Glargine 100U/mL', 'MED-INS-100',   35,  10);

-- Seed data for Appointments
INSERT INTO `Appointments` (`patientId`, `doctorId`, `appointmentDate`, `status`) VALUES
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

-- Seed data for Prescriptions
INSERT INTO `Prescriptions` (`patientId`, `doctorId`, `issuedAt`, `status`, `notes`) VALUES
(1,  1, '2024-06-01 09:45:00', 'Completed', 'Hypertension follow-up. Monitor blood pressure weekly.'),
(2,  3, '2024-06-03 11:00:00', 'Active',    'Childhood respiratory infection. Complete full course.'),
(4,  2, '2024-06-10 09:00:00', 'Active',    'Migraine management. Avoid triggers. Return if no improvement.'),
(6,  7, '2024-06-15 10:15:00', 'Active',    'Gastric ulcer treatment. Take omeprazole before meals.'),
(8,  8, '2024-06-20 10:30:00', 'Active',    'Type 2 diabetes. Metformin with meals. Monitor glucose daily.'),
(3,  5, '2024-05-20 11:00:00', 'Cancelled', 'Knee inflammation. Prescription cancelled — patient requested second opinion.'),
(5,  4, '2024-05-28 14:30:00', 'Completed', 'Post-chemotherapy supportive care.'),
(9,  9, '2024-06-22 12:00:00', 'Active',    'Anxiety disorder. Counselling sessions scheduled.');

-- Seed data for PatientFeedback
INSERT INTO `PatientFeedback` (`patientId`, `Feedback`, `passwordNotes`, `issuedAt`) VALUES
(1,  'Excellent service from the cardiology team. Dr. Nkurunziza was very thorough.',          NULL,                           '2024-06-02 10:00:00'),
(2,  'The pediatrics ward is clean and the staff were very kind to my child.',                  NULL,                           '2024-06-04 09:00:00'),
(4,  'Very professional. I got a clear explanation of my diagnosis.',                           'Patient prefers morning slots.','2024-06-11 08:00:00'),
(6,  'Wait time was long but the doctor was excellent.',                                        NULL,                           '2024-06-16 14:00:00'),
(8,  'Happy with the diabetes management plan. Easy to understand instructions.',               'Prefers SMS reminders.',        '2024-06-21 11:00:00'),
(10, 'Great ophthalmology consultation. Very helpful advice on eye care.',                      NULL,                           '2024-06-26 16:00:00');

-- Seed data for MedicalItems (links inventory items to a category)
INSERT INTO `MedicalItems` (`inventoryId`, `quantity`, `category`) VALUES
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

-- Seed data for PrescriptionItems (prescriptionId refs Prescriptions, medicationId refs MedicalInventory)
INSERT INTO `PrescriptionItems` (`prescriptionId`, `medicationId`, `dosage`, `frequency`, `quantity`, `refillsAllowed`) VALUES
(1, 5, '50mg',       'Once daily',       30, 2),   -- Prescription 1: Atenolol for hypertension
(2, 1, '500mg',      'Three times daily', 21, 0),   -- Prescription 2: Amoxicillin for infection
(2, 2, '1000mg',     'Twice daily',       14, 0),   -- Prescription 2: Paracetamol for fever
(3, 2, '1000mg',     'As needed',         10, 1),   -- Prescription 3: Paracetamol for migraine
(4, 6, '20mg',       'Once daily',        30, 2),   -- Prescription 4: Omeprazole for gastric ulcer
(5, 4, '850mg',      'Twice daily',       60, 3),   -- Prescription 5: Metformin for diabetes
(5, 10,'100U/mL',    'Once daily',        30, 2),   -- Prescription 5: Insulin for diabetes
(7, 3, '400mg',      'Twice daily',       14, 0),   -- Prescription 7: Ibuprofen post-chemo pain
(8, 2, '1000mg',     'Once daily',        30, 1);   -- Prescription 8: Paracetamol support
