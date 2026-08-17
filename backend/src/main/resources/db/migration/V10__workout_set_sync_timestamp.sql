-- V10: Offline sync (UC-21 / 009 FR-009) — LWW theo client_timestamp
ALTER TABLE workout_sets ADD COLUMN client_timestamp TIMESTAMP WITH TIME ZONE;
