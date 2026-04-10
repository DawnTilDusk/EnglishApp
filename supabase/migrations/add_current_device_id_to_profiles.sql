-- Add current_device_id to profiles for tracking active session (kick-out mechanism)
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS current_device_id TEXT;
