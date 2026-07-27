export type ActionState = {
  error: string | null;
  ok: boolean;
};

export const initialActionState: ActionState = { error: null, ok: false };
