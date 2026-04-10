-- Create RPC function to allow users to securely update their own current_device_id
CREATE OR REPLACE FUNCTION public.set_my_device_id(p_device_id TEXT)
RETURNS VOID AS $$
BEGIN
  IF auth.uid() IS NULL THEN
    RAISE EXCEPTION 'Not authenticated';
  END IF;

  UPDATE public.profiles
  SET
    current_device_id = p_device_id
  WHERE id = auth.uid();
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
