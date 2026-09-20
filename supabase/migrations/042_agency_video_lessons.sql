-- 042: Agency-scoped video lessons (MP4) + private Storage bucket.

CREATE TABLE IF NOT EXISTS public.video_lessons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agency_id UUID NOT NULL REFERENCES public.agencies(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    description TEXT,
    storage_path TEXT,
    duration_seconds INT,
    sort_order INT NOT NULL DEFAULT 0,
    is_published BOOLEAN NOT NULL DEFAULT false,
    created_by UUID REFERENCES public.profiles(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT video_lessons_storage_path_chk CHECK (
        storage_path IS NULL
        OR storage_path = agency_id::text || '/' || id::text || '/source.mp4'
    ),
    CONSTRAINT video_lessons_published_has_file_chk CHECK (
        is_published = false OR storage_path IS NOT NULL
    )
);

CREATE INDEX IF NOT EXISTS video_lessons_agency_published_sort_idx
    ON public.video_lessons (agency_id, is_published, sort_order, created_at DESC);

ALTER TABLE public.video_lessons ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Agency admins manage video lessons" ON public.video_lessons;
CREATE POLICY "Agency admins manage video lessons"
    ON public.video_lessons FOR ALL
    USING (private.is_agency_admin_of(agency_id))
    WITH CHECK (private.is_agency_admin_of(agency_id));

DROP POLICY IF EXISTS "Teachers view agency video lessons" ON public.video_lessons;
CREATE POLICY "Teachers view agency video lessons"
    ON public.video_lessons FOR SELECT
    USING (private.is_teacher_in_agency(agency_id));

DROP POLICY IF EXISTS "Students view published agency video lessons" ON public.video_lessons;
CREATE POLICY "Students view published agency video lessons"
    ON public.video_lessons FOR SELECT
    USING (
        is_published = true
        AND storage_path IS NOT NULL
        AND EXISTS (
            SELECT 1
            FROM public.students s
            WHERE s.id = auth.uid()
              AND s.agency_id = video_lessons.agency_id
        )
    );

GRANT SELECT, INSERT, UPDATE, DELETE ON public.video_lessons TO authenticated;
GRANT ALL ON public.video_lessons TO service_role;

-- ---------------------------------------------------------------------------
-- Storage bucket agency-videos (private, MP4 only, 200MB)
-- ---------------------------------------------------------------------------
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
    'agency-videos',
    'agency-videos',
    false,
    209715200,
    ARRAY['video/mp4']
)
ON CONFLICT (id) DO UPDATE SET
    public = false,
    file_size_limit = EXCLUDED.file_size_limit,
    allowed_mime_types = EXCLUDED.allowed_mime_types;

-- Path helpers: {agency_id}/{lesson_id}/source.mp4
CREATE OR REPLACE FUNCTION private.video_storage_agency_id(p_name text)
RETURNS uuid
LANGUAGE plpgsql
IMMUTABLE
AS $$
BEGIN
  RETURN NULLIF(split_part(p_name, '/', 1), '')::uuid;
EXCEPTION WHEN others THEN
  RETURN NULL;
END;
$$;

CREATE OR REPLACE FUNCTION private.video_storage_lesson_id(p_name text)
RETURNS uuid
LANGUAGE plpgsql
IMMUTABLE
AS $$
BEGIN
  RETURN NULLIF(split_part(p_name, '/', 2), '')::uuid;
EXCEPTION WHEN others THEN
  RETURN NULL;
END;
$$;

CREATE OR REPLACE FUNCTION private.video_storage_basename(p_name text)
RETURNS text
LANGUAGE sql
IMMUTABLE
AS $$
  SELECT split_part(p_name, '/', 3);
$$;

REVOKE ALL ON FUNCTION private.video_storage_agency_id(text) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION private.video_storage_agency_id(text) TO authenticated, service_role;
REVOKE ALL ON FUNCTION private.video_storage_lesson_id(text) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION private.video_storage_lesson_id(text) TO authenticated, service_role;
REVOKE ALL ON FUNCTION private.video_storage_basename(text) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION private.video_storage_basename(text) TO authenticated, service_role;

DROP POLICY IF EXISTS "Agency admins upload video lessons" ON storage.objects;
CREATE POLICY "Agency admins upload video lessons"
    ON storage.objects FOR INSERT TO authenticated
    WITH CHECK (
        bucket_id = 'agency-videos'
        AND private.video_storage_basename(name) = 'source.mp4'
        AND private.is_agency_admin_of(private.video_storage_agency_id(name))
        AND EXISTS (
            SELECT 1
            FROM public.video_lessons v
            WHERE v.id = private.video_storage_lesson_id(name)
              AND v.agency_id = private.video_storage_agency_id(name)
        )
    );

DROP POLICY IF EXISTS "Agency admins update video lessons" ON storage.objects;
CREATE POLICY "Agency admins update video lessons"
    ON storage.objects FOR UPDATE TO authenticated
    USING (
        bucket_id = 'agency-videos'
        AND private.is_agency_admin_of(private.video_storage_agency_id(name))
    )
    WITH CHECK (
        bucket_id = 'agency-videos'
        AND private.video_storage_basename(name) = 'source.mp4'
        AND private.is_agency_admin_of(private.video_storage_agency_id(name))
    );

DROP POLICY IF EXISTS "Agency admins delete video lessons" ON storage.objects;
CREATE POLICY "Agency admins delete video lessons"
    ON storage.objects FOR DELETE TO authenticated
    USING (
        bucket_id = 'agency-videos'
        AND private.is_agency_admin_of(private.video_storage_agency_id(name))
    );

DROP POLICY IF EXISTS "Agency admins read video lessons" ON storage.objects;
CREATE POLICY "Agency admins read video lessons"
    ON storage.objects FOR SELECT TO authenticated
    USING (
        bucket_id = 'agency-videos'
        AND private.is_agency_admin_of(private.video_storage_agency_id(name))
    );

DROP POLICY IF EXISTS "Teachers read agency video lessons" ON storage.objects;
CREATE POLICY "Teachers read agency video lessons"
    ON storage.objects FOR SELECT TO authenticated
    USING (
        bucket_id = 'agency-videos'
        AND private.is_teacher_in_agency(private.video_storage_agency_id(name))
    );

DROP POLICY IF EXISTS "Students read published video lessons" ON storage.objects;
CREATE POLICY "Students read published video lessons"
    ON storage.objects FOR SELECT TO authenticated
    USING (
        bucket_id = 'agency-videos'
        AND EXISTS (
            SELECT 1
            FROM public.video_lessons v
            JOIN public.students s
              ON s.agency_id = v.agency_id
             AND s.id = auth.uid()
            -- Must qualify storage.objects.name: students also has `name`.
            WHERE v.id = private.video_storage_lesson_id(storage.objects.name)
              AND v.agency_id = private.video_storage_agency_id(storage.objects.name)
              AND v.is_published = true
              AND v.storage_path IS NOT NULL
        )
    );
