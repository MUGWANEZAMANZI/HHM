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
