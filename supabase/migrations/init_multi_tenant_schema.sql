-- Initialize Enums
DO $$ BEGIN
    CREATE TYPE public.user_role AS ENUM ('company_admin', 'agency_admin', 'student');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE public.redemption_status AS ENUM ('pending', 'approved', 'rejected', 'completed');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- 1. Agencies Table
CREATE TABLE IF NOT EXISTS public.agencies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    quota_students INT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2. Profiles Table (extends auth.users)
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    role public.user_role NOT NULL DEFAULT 'student'::public.user_role,
    agency_id UUID REFERENCES public.agencies(id) ON DELETE SET NULL,
    display_name TEXT,
    email TEXT,
    phone TEXT,
    phone_verified BOOLEAN NOT NULL DEFAULT false,
    phone_updated_at TIMESTAMPTZ,
    status TEXT DEFAULT 'inactive',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Ensure the default is applied even if the table already exists
ALTER TABLE public.profiles ALTER COLUMN status SET DEFAULT 'inactive';

CREATE INDEX IF NOT EXISTS profiles_email_idx ON public.profiles (email);
CREATE INDEX IF NOT EXISTS profiles_phone_idx ON public.profiles (phone);

-- 3. Students Table (extends profiles for students)
CREATE TABLE IF NOT EXISTS public.students (
    id UUID PRIMARY KEY REFERENCES public.profiles(id) ON DELETE CASCADE,
    agency_id UUID NOT NULL REFERENCES public.agencies(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    student_no TEXT,
    class_id TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 4. Content Items Table (Global)
CREATE TABLE IF NOT EXISTS public.content_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type TEXT NOT NULL,
    title TEXT NOT NULL,
    body TEXT,
    asset_refs JSONB,
    version INT DEFAULT 1,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 5. Content Assignments Table
CREATE TABLE IF NOT EXISTS public.content_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agency_id UUID NOT NULL REFERENCES public.agencies(id) ON DELETE CASCADE,
    student_id UUID REFERENCES public.students(id) ON DELETE CASCADE,
    content_item_id UUID NOT NULL REFERENCES public.content_items(id) ON DELETE CASCADE,
    enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 6. Study Events Table
CREATE TABLE IF NOT EXISTS public.study_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agency_id UUID NOT NULL REFERENCES public.agencies(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES public.students(id) ON DELETE CASCADE,
    event_type TEXT NOT NULL,
    payload JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 7. Points Ledger Table
CREATE TABLE IF NOT EXISTS public.points_ledger (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agency_id UUID NOT NULL REFERENCES public.agencies(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES public.students(id) ON DELETE CASCADE,
    delta INT NOT NULL,
    reason TEXT,
    ref_id TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 8. Rewards Table
CREATE TABLE IF NOT EXISTS public.rewards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agency_id UUID NOT NULL REFERENCES public.agencies(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    points_required INT NOT NULL,
    stock INT DEFAULT -1,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 9. Redemptions Table
CREATE TABLE IF NOT EXISTS public.redemptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agency_id UUID NOT NULL REFERENCES public.agencies(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES public.students(id) ON DELETE CASCADE,
    reward_id UUID NOT NULL REFERENCES public.rewards(id) ON DELETE CASCADE,
    status public.redemption_status DEFAULT 'pending'::public.redemption_status,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Enable RLS
ALTER TABLE public.agencies ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.students ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.content_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.content_assignments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.study_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.points_ledger ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.rewards ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.redemptions ENABLE ROW LEVEL SECURITY;

-- Helper Functions for RLS
CREATE OR REPLACE FUNCTION public.get_auth_role() RETURNS public.user_role AS $$
  SELECT role FROM public.profiles WHERE id = auth.uid() LIMIT 1;
$$ LANGUAGE sql SECURITY DEFINER;

CREATE OR REPLACE FUNCTION public.get_auth_agency_id() RETURNS UUID AS $$
  SELECT agency_id FROM public.profiles WHERE id = auth.uid() LIMIT 1;
$$ LANGUAGE sql SECURITY DEFINER;

CREATE OR REPLACE FUNCTION public.set_my_phone(p_phone TEXT)
RETURNS VOID AS $$
BEGIN
  IF auth.uid() IS NULL THEN
    RAISE EXCEPTION 'Not authenticated';
  END IF;

  UPDATE public.profiles
  SET
    phone = p_phone,
    phone_verified = false,
    phone_updated_at = NOW()
  WHERE id = auth.uid();
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Clear existing policies (to allow re-running this script without errors)
DO $$ DECLARE
    r RECORD;
BEGIN
    FOR r IN (SELECT policyname, tablename FROM pg_policies WHERE schemaname = 'public') LOOP
        EXECUTE format('DROP POLICY IF EXISTS %I ON public.%I', r.policyname, r.tablename);
    END LOOP;
END $$;

-- RLS Policies for agencies
CREATE POLICY "Company admins manage agencies" ON public.agencies
  FOR ALL USING (public.get_auth_role() = 'company_admin');
CREATE POLICY "Agency admins view own agency" ON public.agencies
  FOR SELECT USING (id = public.get_auth_agency_id() AND public.get_auth_role() = 'agency_admin');
CREATE POLICY "Students view own agency" ON public.agencies
  FOR SELECT USING (id = public.get_auth_agency_id() AND public.get_auth_role() = 'student');

-- RLS Policies for profiles
CREATE POLICY "Company admins manage all profiles" ON public.profiles
  FOR ALL USING (public.get_auth_role() = 'company_admin');
CREATE POLICY "Agency admins manage own agency profiles" ON public.profiles
  FOR ALL USING (agency_id = public.get_auth_agency_id() AND public.get_auth_role() = 'agency_admin');
CREATE POLICY "Users view own profile" ON public.profiles
  FOR SELECT USING (id = auth.uid());

-- RLS Policies for students
CREATE POLICY "Company admins manage all students" ON public.students
  FOR ALL USING (public.get_auth_role() = 'company_admin');
CREATE POLICY "Agency admins manage own agency students" ON public.students
  FOR ALL USING (agency_id = public.get_auth_agency_id() AND public.get_auth_role() = 'agency_admin');
CREATE POLICY "Students view own record" ON public.students
  FOR SELECT USING (id = auth.uid());

-- RLS Policies for content_items
CREATE POLICY "Company admins manage content items" ON public.content_items
  FOR ALL USING (public.get_auth_role() = 'company_admin');
CREATE POLICY "Anyone can view content items" ON public.content_items
  FOR SELECT USING (true);

-- RLS Policies for content_assignments
CREATE POLICY "Company admins manage all assignments" ON public.content_assignments
  FOR ALL USING (public.get_auth_role() = 'company_admin');
CREATE POLICY "Agency admins manage own agency assignments" ON public.content_assignments
  FOR ALL USING (agency_id = public.get_auth_agency_id() AND public.get_auth_role() = 'agency_admin');
CREATE POLICY "Students view own assignments" ON public.content_assignments
  FOR SELECT USING (student_id = auth.uid() OR (student_id IS NULL AND agency_id = public.get_auth_agency_id()));

-- RLS Policies for study_events
CREATE POLICY "Company admins view all events" ON public.study_events
  FOR SELECT USING (public.get_auth_role() = 'company_admin');
CREATE POLICY "Agency admins view own agency events" ON public.study_events
  FOR SELECT USING (agency_id = public.get_auth_agency_id() AND public.get_auth_role() = 'agency_admin');
CREATE POLICY "Students view own events" ON public.study_events
  FOR SELECT USING (student_id = auth.uid());
CREATE POLICY "Students insert own events" ON public.study_events
  FOR INSERT WITH CHECK (student_id = auth.uid() AND agency_id = public.get_auth_agency_id());

-- RLS Policies for points_ledger
CREATE POLICY "Company admins view all ledger entries" ON public.points_ledger
  FOR SELECT USING (public.get_auth_role() = 'company_admin');
CREATE POLICY "Agency admins view own agency ledger entries" ON public.points_ledger
  FOR SELECT USING (agency_id = public.get_auth_agency_id() AND public.get_auth_role() = 'agency_admin');
CREATE POLICY "Students view own ledger entries" ON public.points_ledger
  FOR SELECT USING (student_id = auth.uid());
-- Note: Points should only be inserted via RPC / Edge Function (service role) to prevent cheating.

-- RLS Policies for rewards
CREATE POLICY "Company admins manage all rewards" ON public.rewards
  FOR ALL USING (public.get_auth_role() = 'company_admin');
CREATE POLICY "Agency admins manage own agency rewards" ON public.rewards
  FOR ALL USING (agency_id = public.get_auth_agency_id() AND public.get_auth_role() = 'agency_admin');
CREATE POLICY "Students view own agency rewards" ON public.rewards
  FOR SELECT USING (agency_id = public.get_auth_agency_id());

-- RLS Policies for redemptions
CREATE POLICY "Company admins view all redemptions" ON public.redemptions
  FOR SELECT USING (public.get_auth_role() = 'company_admin');
CREATE POLICY "Agency admins manage own agency redemptions" ON public.redemptions
  FOR ALL USING (agency_id = public.get_auth_agency_id() AND public.get_auth_role() = 'agency_admin');
CREATE POLICY "Students view own redemptions" ON public.redemptions
  FOR SELECT USING (student_id = auth.uid());
CREATE POLICY "Students insert own redemptions" ON public.redemptions
  FOR INSERT WITH CHECK (student_id = auth.uid() AND agency_id = public.get_auth_agency_id());
