-- Shop tables and cloud economy ledger

CREATE TABLE IF NOT EXISTS public.shop_products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    teacher_id UUID NOT NULL REFERENCES public.teachers(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    description TEXT,
    price_tokens INT NOT NULL CHECK (price_tokens > 0),
    stock INT NOT NULL DEFAULT -1,
    image_url TEXT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.shop_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL REFERENCES public.students(id) ON DELETE CASCADE,
    teacher_id UUID NOT NULL REFERENCES public.teachers(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES public.shop_products(id) ON DELETE RESTRICT,
    tokens_amount INT NOT NULL CHECK (tokens_amount > 0),
    status public.redemption_status NOT NULL DEFAULT 'pending'::public.redemption_status,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    resolved_at TIMESTAMPTZ,
    resolved_by UUID REFERENCES public.profiles(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS shop_products_teacher_id_idx ON public.shop_products (teacher_id);
CREATE INDEX IF NOT EXISTS shop_orders_teacher_id_idx ON public.shop_orders (teacher_id);
CREATE INDEX IF NOT EXISTS shop_orders_student_id_idx ON public.shop_orders (student_id);
CREATE INDEX IF NOT EXISTS shop_orders_status_idx ON public.shop_orders (status);

CREATE TABLE IF NOT EXISTS public.user_economy_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    amount INT NOT NULL,
    reason TEXT NOT NULL,
    ref_id TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS user_economy_transactions_user_id_idx
    ON public.user_economy_transactions (user_id);

ALTER TABLE public.shop_products ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.shop_orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_economy_transactions ENABLE ROW LEVEL SECURITY;

-- shop_products policies
DROP POLICY IF EXISTS "Teachers manage own products" ON public.shop_products;
CREATE POLICY "Teachers manage own products" ON public.shop_products
    FOR ALL USING (teacher_id = auth.uid())
    WITH CHECK (teacher_id = auth.uid());

DROP POLICY IF EXISTS "Students view teacher active products" ON public.shop_products;
CREATE POLICY "Students view teacher active products" ON public.shop_products
    FOR SELECT USING (
        is_active = true
        AND EXISTS (
            SELECT 1 FROM public.students s
            WHERE s.id = auth.uid() AND s.teacher_id = shop_products.teacher_id
        )
    );

DROP POLICY IF EXISTS "Company admins manage all shop products" ON public.shop_products;
CREATE POLICY "Company admins manage all shop products" ON public.shop_products
    FOR ALL USING (public.get_auth_role() = 'company_admin'::public.user_role);

-- shop_orders policies (insert/update via RPC only; direct insert blocked)
DROP POLICY IF EXISTS "Students view own orders" ON public.shop_orders;
CREATE POLICY "Students view own orders" ON public.shop_orders
    FOR SELECT USING (student_id = auth.uid());

DROP POLICY IF EXISTS "Teachers view own orders" ON public.shop_orders;
CREATE POLICY "Teachers view own orders" ON public.shop_orders
    FOR SELECT USING (teacher_id = auth.uid());

DROP POLICY IF EXISTS "Company admins manage all shop orders" ON public.shop_orders;
CREATE POLICY "Company admins manage all shop orders" ON public.shop_orders
    FOR ALL USING (public.get_auth_role() = 'company_admin'::public.user_role);

-- user_economy_transactions policies
DROP POLICY IF EXISTS "Users view own economy transactions" ON public.user_economy_transactions;
CREATE POLICY "Users view own economy transactions" ON public.user_economy_transactions
    FOR SELECT USING (user_id = auth.uid());

DROP POLICY IF EXISTS "Teachers view student economy transactions" ON public.user_economy_transactions;
CREATE POLICY "Teachers view student economy transactions" ON public.user_economy_transactions
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM public.students s
            WHERE s.id = user_economy_transactions.user_id
              AND s.teacher_id = auth.uid()
        )
    );
