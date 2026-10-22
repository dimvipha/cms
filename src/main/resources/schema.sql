CREATE SEQUENCE IF NOT EXISTS patient_mrn_seq START 1;

CREATE SEQUENCE IF NOT EXISTS invoice_number_seq START 1;

-- a visit can be billed again only after its previous invoice was voided
CREATE UNIQUE INDEX IF NOT EXISTS ux_invoice_active_visit ON invoice (visit_id) WHERE status <> 'VOID';
