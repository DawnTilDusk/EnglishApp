export type VideoLesson = {
  id: string;
  agency_id: string;
  title: string;
  description: string | null;
  storage_path: string | null;
  duration_seconds: number | null;
  sort_order: number;
  is_published: boolean;
  created_at?: string;
  updated_at?: string;
};
