-- 011: Agency shop (one shop per agency); instant purchase; no approval flow

-- 1) Schema: add agency_id, backfill from teachers, drop teacher_id
ALTER TABLE public.shop_products
    ADD COLUMN IF NOT EXISTS agency_id UUID REFERENCES public.agencies(id) ON DELETE CASCADE;

ALTER TABLE public.shop_orders
    ADD COLUMN IF NOT EXISTS agency_id UUID REFERENCES public.agencies(id) ON DELETE CASCADE;

-- Backfill products
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'shop_products' AND column_name = 'teacher_id'
    ) THEN
        UPDATE public.shop_products sp
        SET agency_id = t.agency_id
        FROM public.teachers t
        WHERE sp.teacher_id = t.id
          AND sp.agency_id IS NULL
          AND t.agency_id IS NOT NULL;
    END IF;
END $$;

-- Backfill orders
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'shop_orders' AND column_name = 'teacher_id'
    ) THEN
        UPDATE public.shop_orders so
        SET agency_id = t.agency_id
        FROM public.teachers t
        WHERE so.teacher_id = t.id
          AND so.agency_id IS NULL
          AND t.agency_id IS NOT NULL;
    END IF;

    -- Also backfill from student's agency when teacher path missing
    UPDATE public.shop_orders so
    SET agency_id = s.agency_id
    FROM public.students s
    WHERE so.student_id = s.id
      AND so.agency_id IS NULL;
END $$;

-- Drop old policies that reference teacher_id
DROP POLICY IF EXISTS "Teachers manage own products" ON public.shop_products;
DROP POLICY IF EXISTS "Students view teacher active products" ON public.shop_products;
DROP POLICY IF EXISTS "Company admins manage all shop products" ON public.shop_products;
DROP POLICY IF EXISTS "Teachers view own orders" ON public.shop_orders;
DROP POLICY IF EXISTS "Students view own orders" ON public.shop_orders;
DROP POLICY IF EXISTS "Company admins manage all shop orders" ON public.shop_orders;

-- Drop teacher_id columns when present
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'shop_products' AND column_name = 'teacher_id'
    ) THEN
        ALTER TABLE public.shop_products DROP COLUMN teacher_id;
    END IF;
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'shop_orders' AND column_name = 'teacher_id'
    ) THEN
        ALTER TABLE public.shop_orders DROP COLUMN teacher_id;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS shop_products_agency_id_idx ON public.shop_products (agency_id);
CREATE INDEX IF NOT EXISTS shop_orders_agency_id_idx ON public.shop_orders (agency_id);

-- 2) New RLS
DROP POLICY IF EXISTS "Agency admins manage agency products" ON public.shop_products;
CREATE POLICY "Agency admins manage agency products" ON public.shop_products
    FOR ALL
    USING (public.is_agency_admin_of(agency_id))
    WITH CHECK (public.is_agency_admin_of(agency_id));

DROP POLICY IF EXISTS "Teachers view agency products" ON public.shop_products;
CREATE POLICY "Teachers view agency products" ON public.shop_products
    FOR SELECT
    USING (public.is_teacher_in_agency(agency_id));

DROP POLICY IF EXISTS "Students view agency active products" ON public.shop_products;
CREATE POLICY "Students view agency active products" ON public.shop_products
    FOR SELECT
    USING (
        is_active = true
        AND EXISTS (
            SELECT 1 FROM public.students s
            WHERE s.id = auth.uid()
              AND s.agency_id = shop_products.agency_id
        )
    );

DROP POLICY IF EXISTS "Students view own orders" ON public.shop_orders;
CREATE POLICY "Students view own orders" ON public.shop_orders
    FOR SELECT USING (student_id = auth.uid());

DROP POLICY IF EXISTS "Agency admins view agency orders" ON public.shop_orders;
CREATE POLICY "Agency admins view agency orders" ON public.shop_orders
    FOR SELECT USING (public.is_agency_admin_of(agency_id));

DROP POLICY IF EXISTS "Teachers view agency orders" ON public.shop_orders;
CREATE POLICY "Teachers view agency orders" ON public.shop_orders
    FOR SELECT USING (public.is_teacher_in_agency(agency_id));

-- 3) Instant purchase RPC (replaces pending submit + approve/reject)
CREATE OR REPLACE FUNCTION public.submit_shop_order(p_product_id UUID)
RETURNS UUID AS $$
DECLARE
    v_student_id UUID;
    v_agency_id UUID;
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

    SELECT s.agency_id INTO v_agency_id
    FROM public.students s
    WHERE s.id = v_student_id;

    IF v_agency_id IS NULL THEN
        RAISE EXCEPTION 'Student has no agency';
    END IF;

    SELECT p.price_tokens, p.stock, p.name
    INTO v_price, v_stock, v_product_name
    FROM public.shop_products p
    WHERE p.id = p_product_id
      AND p.agency_id = v_agency_id
      AND p.is_active = true
    FOR UPDATE;

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

    UPDATE public.shop_products
    SET stock = CASE WHEN stock = -1 THEN -1 ELSE stock - 1 END
    WHERE id = p_product_id
      AND (stock = -1 OR stock > 0);

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Product out of stock';
    END IF;

    v_order_id := gen_random_uuid();

    INSERT INTO public.user_economy_transactions (id, user_id, amount, reason, ref_id)
    VALUES (
        gen_random_uuid(),
        v_student_id,
        -v_price,
        'Shop purchase: ' || v_product_name,
        v_order_id::TEXT
    );

    INSERT INTO public.shop_orders (
        id, student_id, agency_id, product_id, tokens_amount, status, resolved_at
    ) VALUES (
        v_order_id,
        v_student_id,
        v_agency_id,
        p_product_id,
        v_price,
        'completed'::public.redemption_status,
        NOW()
    );

    RETURN v_order_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

-- Alias name from plan
CREATE OR REPLACE FUNCTION public.purchase_shop_product(p_product_id UUID)
RETURNS UUID AS $$
    SELECT public.submit_shop_order(p_product_id);
$$ LANGUAGE sql SECURITY DEFINER SET search_path = public;

-- Remove approval workflow
DROP FUNCTION IF EXISTS public.approve_shop_order(UUID);
DROP FUNCTION IF EXISTS public.reject_shop_order(UUID);

GRANT EXECUTE ON FUNCTION public.submit_shop_order(UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION public.purchase_shop_product(UUID) TO authenticated;
