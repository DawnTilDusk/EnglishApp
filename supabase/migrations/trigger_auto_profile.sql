-- ==============================================================================
-- Business-Driven Sync Triggers (Users -> Students -> Profiles)
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 1. users -> profiles (占坑) & students (业务实体初始化)
-- ------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS trigger AS $$
DECLARE
    v_default_name TEXT;
BEGIN
    -- 获取默认姓名 (从邮箱前缀提取)
    v_default_name := split_part(new.email, '@', 1);
    IF v_default_name IS NULL OR v_default_name = '' THEN
        v_default_name := 'New Student';
    END IF;

    -- 步骤 A：纯粹为了满足物理外键约束，在 profiles 占坑 (业务数据全是空的/默认的)
    INSERT INTO public.profiles (id, role, status, display_name, agency_id)
    VALUES (
        new.id, 
        'student'::public.user_role, 
        'active',
        NULL, -- 故意留空，等待 students 表同步过来
        '1d13f462-d263-4ce6-90c8-f867ef6a2207'::UUID -- 分配到默认的"根机构"缓冲池
    );

    -- 步骤 B：(你期望的 users -> students 流向) 
    -- 在业务层初始化一个学生实体。
    BEGIN
        INSERT INTO public.students (id, name, agency_id)
        VALUES (new.id, v_default_name, '1d13f462-d263-4ce6-90c8-f867ef6a2207'::UUID);
    EXCEPTION WHEN OTHERS THEN
        -- 如果因为外键/非空约束报错，则静默忽略，由后台管理员手动插入 students
        NULL;
    END;

    RETURN new;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE PROCEDURE public.handle_new_user();


-- ------------------------------------------------------------------------------
-- 2. students -> profiles (业务实体驱动权限档案)
-- 只要 students 发生新增或修改，立刻反向覆盖 profiles 的权限信息
-- ------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.sync_student_to_profile()
RETURNS trigger AS $$
BEGIN
    -- 业务层 (students) 的变动，直接决定了权限层 (profiles) 的数据
    UPDATE public.profiles
    SET 
        agency_id = NEW.agency_id,
        display_name = NEW.name,
        role = 'student'::public.user_role
    WHERE id = NEW.id;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_student_inserted_updated ON public.students;
CREATE TRIGGER on_student_inserted_updated
  -- 无论是刚建号时 INSERT 的 student，还是后续 UPDATE 的 student，都会触发同步
  AFTER INSERT OR UPDATE OF agency_id, name ON public.students
  FOR EACH ROW EXECUTE PROCEDURE public.sync_student_to_profile();


-- ------------------------------------------------------------------------------
-- 3. (可选) agencies -> profiles
-- 通常 agencies 的字段(如名字)不需要同步给 profiles，因为 profiles 只存 UUID。
-- 这里的逻辑仅作占位：如果机构被删除或禁用，可以把该机构下所有 profiles 踢出。
-- ------------------------------------------------------------------------------
-- CREATE OR REPLACE FUNCTION public.handle_agency_deletion()
-- RETURNS trigger AS $$
-- BEGIN
--     UPDATE public.profiles SET agency_id = NULL, status = 'banned' WHERE agency_id = OLD.id;
--     RETURN OLD;
-- END;
-- $$ LANGUAGE plpgsql SECURITY DEFINER;
