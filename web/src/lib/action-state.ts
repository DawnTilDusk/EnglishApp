export type ActionState = {
  error: string | null;
  ok: boolean;
};

export const initialActionState: ActionState = { error: null, ok: false };

/** Map Auth unique-email failures to a stable Chinese message. */
export function mapCreateAccountError(message: string): string {
  const lower = message.toLowerCase();
  if (
    lower.includes("users_email_partial_key") ||
    message.includes("该邮箱已被使用")
  ) {
    return "该邮箱已被使用。";
  }
  return message;
}
