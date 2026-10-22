# NoSQL Design — Patient Notes and Medical Logs

## Why NoSQL for This Use Case

The HHM relational schema stores structured data — appointments, prescriptions,
inventory — where every record shares the same fixed columns. This works well
because the data is uniform and heavily joined (e.g. appointments link patients,
doctors, and departments).

Patient notes and medical logs are different. A doctor's note after a cardiology
consultation looks nothing like a note after a psychiatric session. Notes may
contain free-form text, embedded images, voice transcriptions, lists of symptoms,
or custom fields added per department. Forcing this into a fixed relational column
(e.g. `TEXT notes`) loses structure; creating a new column for every possible field
breaks normalization and creates a sparse table.

A **document store (MongoDB)** suits this better because:

- Each document can have a different shape — no schema migration needed when a
  new department adds a custom field
- Nested objects model the data naturally (doctor info embedded in the note)
- Full-text search across note content is a first-class feature in MongoDB
- Horizontal scaling is straightforward if note volume grows significantly

---

## Proposed Document Schema (MongoDB)

Each patient note or medical log is stored as one document in a
`patient_logs` collection.

```json
{
  "_id": "ObjectId('64f3a1b2c9d4e5f6a7b8c9d0')",
  "patientId": 42,
  "patientName": "Uwimana Chantal",
  "visitDate": "2024-06-01T09:45:00Z",
  "type": "consultation",
  "department": "Cardiology",
  "doctor": {
    "id": 1,
    "name": "Dr. Nkurunziza Samuel",
    "licenseNumber": "RW-MD-10001"
  },
  "chiefComplaint": "Chest pain and shortness of breath on exertion",
  "vitalSigns": {
    "bloodPressure": "138/88",
    "heartRate": 92,
    "temperature": 36.8,
    "oxygenSaturation": 97
  },
  "clinicalNotes": "Patient presents with stable angina. ECG shows minor ST changes. Recommended stress test and medication review.",
  "attachments": [
    { "type": "ECG", "filename": "ecg_2024_06_01.pdf", "url": "/files/p42/ecg_2024_06_01.pdf" }
  ],
  "followUpDate": "2024-07-01",
  "tags": ["cardiology", "angina", "follow-up-required"],
  "createdAt": "2024-06-01T10:15:00Z",
  "updatedAt": "2024-06-01T10:15:00Z"
}
```

A psychiatric log for the same patient might add completely different fields:

```json
{
  "_id": "ObjectId('64f3a1b2c9d4e5f6a7b8c9d1')",
  "patientId": 42,
  "visitDate": "2024-06-22T12:00:00Z",
  "type": "psychiatric_session",
  "department": "Psychiatry",
  "doctor": { "id": 9, "name": "Dr. Niyonzima Pascal" },
  "sessionDuration": 50,
  "moodAssessment": "PHQ-9 score: 11 (moderate depression)",
  "sessionNotes": "Patient reports improved sleep but continued low motivation. Adjusted SSRI dose. Next session in 4 weeks.",
  "medicationChanges": [
    { "drug": "Sertraline", "previousDose": "50mg", "newDose": "100mg" }
  ],
  "riskLevel": "low",
  "createdAt": "2024-06-22T13:00:00Z"
}
```

Neither document breaks the other's schema — MongoDB stores both without
modification.

---

## Relationship With the Relational Database

The two databases complement each other rather than replace one another.

| Data | Storage | Reason |
|---|---|---|
| Patient identity, contact info | MySQL `Patient` table | Structured, frequently joined |
| Appointments, prescriptions | MySQL | Transactional, FK integrity needed |
| Doctor notes, session logs | MongoDB `patient_logs` | Variable structure, full-text search |
| Imaging / attachments | File storage (path in MongoDB doc) | Binary, not suitable for either DB |

The link between the two systems is `patientId` — the MySQL primary key is stored
as a plain integer field in each MongoDB document, allowing the application to
fetch structured data from MySQL and unstructured notes from MongoDB using the
same patient ID.

---

## Query Examples (MongoDB)

```javascript
// All logs for patient 42, newest first
db.patient_logs.find({ patientId: 42 }).sort({ visitDate: -1 })

// All cardiology logs in June 2024
db.patient_logs.find({
  department: "Cardiology",
  visitDate: { $gte: ISODate("2024-06-01"), $lt: ISODate("2024-07-01") }
})

// Full-text search across all clinical notes
db.patient_logs.createIndex({ clinicalNotes: "text" })
db.patient_logs.find({ $text: { $search: "angina chest pain" } })
```

---

## Summary

Patient notes are the clearest case in this system where NoSQL adds value.
The relational schema already has a `notes TEXT` column on `Prescriptions`
and a `Feedback TEXT` column on `PatientFeedback` — both are flat strings
with no queryable internal structure. Moving free-form clinical logs to
MongoDB allows per-department custom fields, embedded vital signs, attachment
references, and full-text search, without schema changes or NULL-heavy rows.
