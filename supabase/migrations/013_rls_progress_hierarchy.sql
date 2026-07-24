-- 013: Hierarchy RLS for profiles/students/progress/economy; drop company_admin dependency on shop-related paths

-- Profiles: agency admins can read profiles in their agency
DROP POLICY IF EXISTS "Agency admins view agency profiles" ON public.profiles;
CREATE POLICY "Agency admins view agency profiles" ON public.profiles
    FOR SELECT
    USING (
        public.get_auth_role() = 'agency_admin'::public.user_role
        AND agency_id IS NOT DISTINCT FROM public.get_auth_agency_id()
    );

-- Students: agency admins manage students in agency
DROP POLICY IF EXISTS "Agency admins manage agency students" ON public.students;
CREATE POLICY "Agency admins manage agency students" ON public.students
    FOR ALL
    USING (public.is_agency_admin_of(agency_id))
    WITH CHECK (public.is_agency_admin_of(agency_id));

-- Economy: agency read
DROP POLICY IF EXISTS "Agency admins view agency economy transactions" ON public.user_economy_transactions;
CREATE POLICY "Agency admins view agency economy transactions" ON public.user_economy_transactions
    FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM public.students s
            WHERE s.id::text = user_economy_transactions.user_id::text
              AND public.is_agency_admin_of(s.agency_id)
        )
    );

-- Progress tables: teacher (assigned) + agency read (cast ids — some tables use TEXT user_id)
DROP POLICY IF EXISTS "Teachers view assigned check_ins" ON public.user_check_ins;
CREATE POLICY "Teachers view assigned check_ins" ON public.user_check_ins
    FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM public.students s
            WHERE s.id::text = user_check_ins.user_id::text
              AND s.teacher_id = auth.uid()
        )
    );

DROP POLICY IF EXISTS "Agency admins view agency check_ins" ON public.user_check_ins;
CREATE POLICY "Agency admins view agency check_ins" ON public.user_check_ins
    FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM public.students s
            WHERE s.id::text = user_check_ins.user_id::text
              AND public.is_agency_admin_of(s.agency_id)
        )
    );

DROP POLICY IF EXISTS "Teachers view assigned word progress" ON public.user_vocabulary_word_learning_progress;
CREATE POLICY "Teachers view assigned word progress" ON public.user_vocabulary_word_learning_progress
    FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM public.students s
            WHERE s.id::text = user_vocabulary_word_learning_progress.user_id::text
              AND s.teacher_id = auth.uid()
        )
    );

DROP POLICY IF EXISTS "Agency admins view agency word progress" ON public.user_vocabulary_word_learning_progress;
CREATE POLICY "Agency admins view agency word progress" ON public.user_vocabulary_word_learning_progress
    FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM public.students s
            WHERE s.id::text = user_vocabulary_word_learning_progress.user_id::text
              AND public.is_agency_admin_of(s.agency_id)
        )
    );

DROP POLICY IF EXISTS "Teachers view assigned study rounds" ON public.user_vocabulary_study_rounds;
CREATE POLICY "Teachers view assigned study rounds" ON public.user_vocabulary_study_rounds
    FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM public.students s
            WHERE s.id::text = user_vocabulary_study_rounds.user_id::text
              AND s.teacher_id = auth.uid()
        )
    );

DROP POLICY IF EXISTS "Agency admins view agency study rounds" ON public.user_vocabulary_study_rounds;
CREATE POLICY "Agency admins view agency study rounds" ON public.user_vocabulary_study_rounds
    FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM public.students s
            WHERE s.id::text = user_vocabulary_study_rounds.user_id::text
              AND public.is_agency_admin_of(s.agency_id)
        )
    );

DROP POLICY IF EXISTS "Teachers view assigned book progress" ON public.user_vocabulary_book_progress;
CREATE POLICY "Teachers view assigned book progress" ON public.user_vocabulary_book_progress
    FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM public.students s
            WHERE s.id::text = user_vocabulary_book_progress.user_id::text
              AND s.teacher_id = auth.uid()
        )
    );

DROP POLICY IF EXISTS "Agency admins view agency book progress" ON public.user_vocabulary_book_progress;
CREATE POLICY "Agency admins view agency book progress" ON public.user_vocabulary_book_progress
    FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM public.students s
            WHERE s.id::text = user_vocabulary_book_progress.user_id::text
              AND public.is_agency_admin_of(s.agency_id)
        )
    );
