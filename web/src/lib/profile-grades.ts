/** Same allowed set as profiles.grade / ProfileGradeOptions. */
export const PROFILE_GRADE_OPTIONS = [
  "一年级",
  "二年级",
  "三年级",
  "四年级",
  "五年级",
  "六年级",
  "初一",
  "初二",
  "初三",
  "高一",
  "高二",
  "高三",
  "其他",
] as const;

export type ProfileGrade = (typeof PROFILE_GRADE_OPTIONS)[number];
