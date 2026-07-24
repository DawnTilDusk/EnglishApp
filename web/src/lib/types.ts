export type UserRole = "agency_admin" | "teacher" | "student" | "company_admin";

export type Profile = {
  id: string;
  role: UserRole;
  agency_id: string | null;
  display_name: string | null;
  email: string | null;
};

export type TeacherRow = {
  id: string;
  display_name: string;
  agency_id: string;
};

export type StudentRow = {
  id: string;
  name: string;
  agency_id: string;
  teacher_id: string | null;
  student_no: string | null;
  class_id: string | null;
};

export type ShopProduct = {
  id: string;
  agency_id: string;
  name: string;
  description: string | null;
  price_tokens: number;
  stock: number;
  is_active: boolean;
  created_at?: string;
};

export type ShopOrder = {
  id: string;
  agency_id: string;
  student_id: string;
  product_id: string;
  tokens_amount: number;
  status: string;
  created_at: string | null;
};
