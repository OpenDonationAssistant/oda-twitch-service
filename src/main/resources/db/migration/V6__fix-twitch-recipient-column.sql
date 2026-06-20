ALTER TABLE reward ALTER COLUMN recipient_id TYPE varchar(100) USING recipient_id::text;
ALTER TABLE reward ADD COLUMN widget_id UUID not null;
