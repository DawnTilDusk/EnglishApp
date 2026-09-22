-- 043: Student storage SELECT used unqualified `name`, which bound to students.name
-- inside the JOIN and made signed URL creation return not_found.

CREATE OR REPLACE FUNCTION private.student_can_read_agency_video_object(p_name text)
RETURNS boolean
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path TO public, private
AS $$
  SELECT EXISTS (
    SELECT 1
    FROM public.video_lessons v
    JOIN public.students s
      ON s.agency_id = v.agency_id
     AND s.id = auth.uid()
    WHERE v.id = private.video_storage_lesson_id(p_name)
      AND v.agency_id = private.video_storage_agency_id(p_name)
      AND v.is_published = true
      AND v.storage_path IS NOT NULL
  );
$$;

REVOKE ALL ON FUNCTION private.student_can_read_agency_video_object(text) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION private.student_can_read_agency_video_object(text) TO authenticated, service_role;

DROP POLICY IF EXISTS "Students read published video lessons" ON storage.objects;
CREATE POLICY "Students read published video lessons"
    ON storage.objects FOR SELECT TO authenticated
    USING (
        bucket_id = 'agency-videos'
        AND private.student_can_read_agency_video_object(name)
    );
