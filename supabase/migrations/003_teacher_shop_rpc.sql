-- Teacher account RPC, shop order RPCs, student stats, and extended RLS for progress tables

-- Helper: cloud token balance for a user
CREATE OR REPLACE FUNCTION public.get_user_token_balance(p_user_id UUID)
RETURNS INT AS $$
    SELECT COALESCE(SUM(amount), 0)::INT
    FROM public.user_economy_transactions
    WHERE user_id = p_user_id;
$$ LANGUAGE sql STABLE SECURITY DEFINER;

-- Create teacher account (SQL Editor / service role; no caller role check for dev convenience)
CREATE OR REPLACE FUNCTION public.create_teacher_account(
    p_email TEXT,
    p_password TEXT,
    p_display_name TEXT DEFAULT NULL
) RETURNS UUID AS $$
DECLARE
    v_user_id UUID;
BEGIN
    v_user_id := gen_random_uuid();

    INSERT INTO auth.users (
        id, instance_id, aud, role, email, encrypted_password,
        email_confirmed_at, recovery_sent_at, last_sign_in_at, raw_app_meta_data,
        raw_user_meta_data, created_at, updated_at, confirmation_token, email_change,
        email_change_token_new, recovery_token
    ) VALUES (
        v_user_id, '00000000-0000-0000-0000-000000000000', 'authenticated', 'authenticated', p_email,
        crypt(p_password, gen_salt('bf')),
        NOW(), NULL, NULL, '{"provider":"email","providers":["email"]}',
        '{}', NOW(), NOW(), '', '', '', ''
    );

    INSERT INTO public.profiles (id, role, display_name, status, email)
    VALUES (
        v_user_id,
        'teacher'::public.user_role,
        COALESCE(p_display_name, p_email),
        'active',
        p_email
    );

    INSERT INTO public.teachers (id, display_name)
    VALUES (v_user_id, COALESCE(p_display_name, p_email));

    RETURN v_user_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Student submits a shop order (deduct tokens, create pending order)
CREATE OR REPLACE FUNCTION public.submit_shop_order(p_product_id UUID)
RETURNS UUID AS $$
DECLARE
    v_student_id UUID;
    v_teacher_id UUID;
    v_price INT;
    v_stock INT;
    v_balance INT;
    v_order_id UUID;
    v_product_name TEXT;
BEGIN
    v_student_id := auth.uid();
    IF v_student_id IS NULL THEN
        RAISE EXCEPTION 'Not authenticated';
    END IF;

    SELECT s.teacher_id INTO v_teacher_id
    FROM public.students s
    WHERE s.id = v_student_id;

    IF v_teacher_id IS NULL THEN
        RAISE EXCEPTION 'Student is not assigned to a teacher';
    END IF;

    SELECT p.price_tokens, p.stock, p.name
    INTO v_price, v_stock, v_product_name
    FROM public.shop_products p
    WHERE p.id = p_product_id
      AND p.teacher_id = v_teacher_id
      AND p.is_active = true;

    IF v_price IS NULL THEN
        RAISE EXCEPTION 'Product not found or not available';
    END IF;

    IF v_stock = 0 THEN
        RAISE EXCEPTION 'Product out of stock';
    END IF;

    v_balance := public.get_user_token_balance(v_student_id);
    IF v_balance < v_price THEN
        RAISE EXCEPTION 'Insufficient tokens';
    END IF;

    v_order_id := gen_random_uuid();

    INSERT INTO public.user_economy_transactions (id, user_id, amount, reason, ref_id)
    VALUES (
        gen_random_uuid(),
        v_student_id,
        -v_price,
        'Shop order pending: ' || v_product_name,
        v_order_id::TEXT
    );

    INSERT INTO public.shop_orders (
        id, student_id, teacher_id, product_id, tokens_amount, status
    ) VALUES (
        v_order_id, v_student_id, v_teacher_id, p_product_id, v_price, 'pending'::public.redemption_status
    );

    RETURN v_order_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Teacher approves order
CREATE OR REPLACE FUNCTION public.approve_shop_order(p_order_id UUID)
RETURNS VOID AS $$
DECLARE
    v_order public.shop_orders%ROWTYPE;
BEGIN
    IF auth.uid() IS NULL THEN
        RAISE EXCEPTION 'Not authenticated';
    END IF;

    SELECT * INTO v_order
    FROM public.shop_orders
    WHERE id = p_order_id AND teacher_id = auth.uid()
    FOR UPDATE;

    IF v_order.id IS NULL THEN
        RAISE EXCEPTION 'Order not found';
    END IF;

    IF v_order.status <> 'pending'::public.redemption_status THEN
        RAISE EXCEPTION 'Order is not pending';
    END IF;

    UPDATE public.shop_products
    SET stock = CASE WHEN stock = -1 THEN -1 ELSE stock - 1 END
    WHERE id = v_order.product_id
      AND (stock = -1 OR stock > 0);

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Product out of stock';
    END IF;

    UPDATE public.shop_orders
    SET status = 'completed'::public.redemption_status,
        resolved_at = NOW(),
        resolved_by = auth.uid()
    WHERE id = p_order_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Teacher rejects order and refunds tokens
CREATE OR REPLACE FUNCTION public.reject_shop_order(p_order_id UUID)
RETURNS VOID AS $$
DECLARE
    v_order public.shop_orders%ROWTYPE;
BEGIN
    IF auth.uid() IS NULL THEN
        RAISE EXCEPTION 'Not authenticated';
    END IF;

    SELECT * INTO v_order
    FROM public.shop_orders
    WHERE id = p_order_id AND teacher_id = auth.uid()
    FOR UPDATE;

    IF v_order.id IS NULL THEN
        RAISE EXCEPTION 'Order not found';
    END IF;

    IF v_order.status <> 'pending'::public.redemption_status THEN
        RAISE EXCEPTION 'Order is not pending';
    END IF;

    INSERT INTO public.user_economy_transactions (user_id, amount, reason, ref_id)
    VALUES (
        v_order.student_id,
        v_order.tokens_amount,
        'Shop order rejected refund',
        p_order_id::TEXT
    );

    UPDATE public.shop_orders
    SET status = 'rejected'::public.redemption_status,
        resolved_at = NOW(),
        resolved_by = auth.uid()
    WHERE id = p_order_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Teacher views student learning stats summary
CREATE OR REPLACE FUNCTION public.get_teacher_student_stats(p_student_id UUID)
RETURNS JSONB AS $$
DECLARE
    v_result JSONB;
BEGIN
    IF auth.uid() IS NULL THEN
        RAISE EXCEPTION 'Not authenticated';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM public.students s
        WHERE s.id = p_student_id AND s.teacher_id = auth.uid()
    ) THEN
        RAISE EXCEPTION 'Access denied';
    END IF;

    SELECT jsonb_build_object(
        'student_id', p_student_id,
        'name', s.name,
        'student_no', s.student_no,
        'class_id', s.class_id,
        'total_check_ins', (
            SELECT COUNT(*) FROM public.user_check_ins c
            WHERE c.user_id = p_student_id AND c.is_checked_in = true
        ),
        'total_study_minutes', (
            SELECT COALESCE(SUM(c.study_time_minutes), 0)
            FROM public.user_check_ins c WHERE c.user_id = p_student_id
        ),
        'learned_word_count', (
            SELECT COALESCE(SUM(bp.learned_word_count), 0)
            FROM public.user_vocabulary_book_progress bp
            WHERE bp.user_id::text = p_student_id::text
        ),
        'token_balance', public.get_user_token_balance(p_student_id)
    ) INTO v_result
    FROM public.students s
    WHERE s.id = p_student_id;

    RETURN v_result;
END;
$$ LANGUAGE plpgsql STABLE SECURITY DEFINER;

-- Allow teachers to read their students' progress tables
DO $$ DECLARE r RECORD; BEGIN
    FOR r IN (
        SELECT unnest(ARRAY[
            'user_check_ins',
            'user_vocabulary_word_learning_progress',
            'user_vocabulary_study_rounds',
            'user_vocabulary_book_progress'
        ]) AS tbl
    ) LOOP
        EXECUTE format(
            'DROP POLICY IF EXISTS "Teachers view assigned student progress" ON public.%I',
            r.tbl
        );
        EXECUTE format(
            'CREATE POLICY "Teachers view assigned student progress" ON public.%I
             FOR SELECT USING (
               EXISTS (
                 SELECT 1 FROM public.students s
                 WHERE s.id::text = %I.user_id::text AND s.teacher_id = auth.uid()
               )
             )',
            r.tbl, r.tbl
        );
    END LOOP;
END $$;

-- Allow teachers to list their assigned students
DROP POLICY IF EXISTS "Teachers view assigned students" ON public.students;
CREATE POLICY "Teachers view assigned students" ON public.students
    FOR SELECT USING (teacher_id = auth.uid());

-- Allow teachers to read profiles of assigned students
DROP POLICY IF EXISTS "Teachers view assigned student profiles" ON public.profiles;
CREATE POLICY "Teachers view assigned student profiles" ON public.profiles
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM public.students s
            WHERE s.id = profiles.id AND s.teacher_id = auth.uid()
        )
    );
