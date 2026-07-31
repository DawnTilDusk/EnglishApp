"use client";

import { useCallback, useEffect, useRef, useState, type CSSProperties } from "react";

type Tool = "pen" | "eraser";
type Point = { x: number; y: number };
type Stroke = {
  tool: Tool;
  color: string;
  width: number;
  points: Point[];
};

const COLORS = [
  { id: "red", value: "#dc2626", label: "红" },
  { id: "blue", value: "#2563eb", label: "蓝" },
  { id: "green", value: "#16a34a", label: "绿" },
] as const;

const WIDTHS = [
  { id: "thin", value: 3, label: "细" },
  { id: "mid", value: 6, label: "中" },
  { id: "thick", value: 12, label: "粗" },
] as const;

type Props = {
  imageUrl: string;
  onCancel: () => void;
  onComplete: (blob: Blob) => Promise<void>;
};

function paintStrokes(ctx: CanvasRenderingContext2D, strokes: Stroke[]) {
  for (const stroke of strokes) {
    if (stroke.points.length < 2) continue;
    ctx.save();
    ctx.lineCap = "round";
    ctx.lineJoin = "round";
    ctx.lineWidth = stroke.width;
    if (stroke.tool === "eraser") {
      ctx.globalCompositeOperation = "destination-out";
      ctx.strokeStyle = "rgba(0,0,0,1)";
    } else {
      ctx.globalCompositeOperation = "source-over";
      ctx.strokeStyle = stroke.color;
    }
    ctx.beginPath();
    ctx.moveTo(stroke.points[0].x, stroke.points[0].y);
    for (let i = 1; i < stroke.points.length; i++) {
      ctx.lineTo(stroke.points[i].x, stroke.points[i].y);
    }
    ctx.stroke();
    ctx.restore();
  }
}

export function WritingAnnotator({ imageUrl, onCancel, onComplete }: Props) {
  const containerRef = useRef<HTMLDivElement>(null);
  const baseRef = useRef<HTMLCanvasElement>(null);
  const inkRef = useRef<HTMLCanvasElement>(null);
  const imageRef = useRef<HTMLImageElement | null>(null);
  const strokesRef = useRef<Stroke[]>([]);
  const drawingRef = useRef(false);
  const currentStrokeRef = useRef<Stroke | null>(null);

  const [ready, setReady] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [tool, setTool] = useState<Tool>("pen");
  const [color, setColor] = useState<string>(COLORS[0].value);
  const [width, setWidth] = useState<number>(WIDTHS[1].value);
  const [strokeCount, setStrokeCount] = useState(0);
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [displaySize, setDisplaySize] = useState({ w: 0, h: 0 });

  const redrawInk = useCallback(() => {
    const ink = inkRef.current;
    if (!ink) return;
    const ctx = ink.getContext("2d");
    if (!ctx) return;
    ctx.clearRect(0, 0, ink.width, ink.height);
    paintStrokes(ctx, strokesRef.current);
  }, []);

  useEffect(() => {
    let cancelled = false;
    let objectUrl: string | null = null;

    async function load() {
      setLoadError(null);
      setReady(false);
      try {
        const res = await fetch(imageUrl);
        if (!res.ok) throw new Error(`加载原图失败（${res.status}）`);
        const blob = await res.blob();
        objectUrl = URL.createObjectURL(blob);
        const img = new Image();
        await new Promise<void>((resolve, reject) => {
          img.onload = () => resolve();
          img.onerror = () => reject(new Error("原图解码失败"));
          img.src = objectUrl!;
        });
        if (cancelled) return;
        imageRef.current = img;
        strokesRef.current = [];
        setStrokeCount(0);

        const container = containerRef.current;
        const maxW = Math.min(container?.clientWidth || 800, 1200);
        const scale = Math.min(1, maxW / img.naturalWidth);
        const w = Math.max(1, Math.round(img.naturalWidth * scale));
        const h = Math.max(1, Math.round(img.naturalHeight * scale));
        setDisplaySize({ w, h });

        for (const canvas of [baseRef.current, inkRef.current]) {
          if (!canvas) continue;
          canvas.width = img.naturalWidth;
          canvas.height = img.naturalHeight;
          canvas.style.width = `${w}px`;
          canvas.style.height = `${h}px`;
        }

        const base = baseRef.current;
        const baseCtx = base?.getContext("2d");
        if (base && baseCtx) {
          baseCtx.clearRect(0, 0, base.width, base.height);
          baseCtx.drawImage(img, 0, 0);
        }
        setReady(true);
        requestAnimationFrame(() => redrawInk());
      } catch (e) {
        if (!cancelled) {
          setLoadError(e instanceof Error ? e.message : "加载失败");
        }
      }
    }

    void load();
    return () => {
      cancelled = true;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [imageUrl, redrawInk]);

  useEffect(() => {
    if (!ready) return;
    redrawInk();
  }, [ready, strokeCount, redrawInk]);

  function canvasPoint(e: React.PointerEvent<HTMLCanvasElement>): Point {
    const canvas = inkRef.current!;
    const rect = canvas.getBoundingClientRect();
    const x = ((e.clientX - rect.left) / rect.width) * canvas.width;
    const y = ((e.clientY - rect.top) / rect.height) * canvas.height;
    return { x, y };
  }

  function onPointerDown(e: React.PointerEvent<HTMLCanvasElement>) {
    if (!ready) return;
    e.currentTarget.setPointerCapture(e.pointerId);
    drawingRef.current = true;
    const stroke: Stroke = {
      tool,
      color,
      width: tool === "eraser" ? width * 2.5 : width,
      points: [canvasPoint(e)],
    };
    currentStrokeRef.current = stroke;
    strokesRef.current = [...strokesRef.current, stroke];
    setStrokeCount(strokesRef.current.length);
  }

  function onPointerMove(e: React.PointerEvent<HTMLCanvasElement>) {
    if (!drawingRef.current || !currentStrokeRef.current) return;
    currentStrokeRef.current.points.push(canvasPoint(e));
    redrawInk();
  }

  function onPointerUp() {
    drawingRef.current = false;
    currentStrokeRef.current = null;
    setStrokeCount(strokesRef.current.length);
  }

  function undo() {
    strokesRef.current = strokesRef.current.slice(0, -1);
    setStrokeCount(strokesRef.current.length);
  }

  function clearAll() {
    strokesRef.current = [];
    setStrokeCount(0);
  }

  async function finish() {
    const base = baseRef.current;
    const ink = inkRef.current;
    if (!base || !ink) return;
    setSaving(true);
    setSaveError(null);
    try {
      // Phone photos as PNG often exceed the 10MB Storage limit — export JPEG
      // and downscale if needed.
      const MAX_EDGE = 2400;
      const MAX_BYTES = 9 * 1024 * 1024;
      let outW = base.width;
      let outH = base.height;
      const longEdge = Math.max(outW, outH);
      if (longEdge > MAX_EDGE) {
        const scale = MAX_EDGE / longEdge;
        outW = Math.max(1, Math.round(outW * scale));
        outH = Math.max(1, Math.round(outH * scale));
      }

      const exportCanvas = document.createElement("canvas");
      exportCanvas.width = outW;
      exportCanvas.height = outH;
      const ctx = exportCanvas.getContext("2d");
      if (!ctx) throw new Error("无法创建导出画布");
      ctx.fillStyle = "#ffffff";
      ctx.fillRect(0, 0, outW, outH);
      ctx.drawImage(base, 0, 0, outW, outH);
      ctx.drawImage(ink, 0, 0, outW, outH);

      async function toJpeg(quality: number): Promise<Blob> {
        return new Promise((resolve, reject) => {
          exportCanvas.toBlob(
            (b) => (b ? resolve(b) : reject(new Error("导出图片失败"))),
            "image/jpeg",
            quality
          );
        });
      }

      let quality = 0.82;
      let blob = await toJpeg(quality);
      while (blob.size > MAX_BYTES && quality > 0.45) {
        quality -= 0.12;
        blob = await toJpeg(quality);
      }
      if (blob.size > MAX_BYTES) {
        // Last resort: shrink further
        const shrink = 0.7;
        exportCanvas.width = Math.max(1, Math.round(outW * shrink));
        exportCanvas.height = Math.max(1, Math.round(outH * shrink));
        const ctx2 = exportCanvas.getContext("2d");
        if (!ctx2) throw new Error("无法创建导出画布");
        ctx2.fillStyle = "#ffffff";
        ctx2.fillRect(0, 0, exportCanvas.width, exportCanvas.height);
        ctx2.drawImage(base, 0, 0, exportCanvas.width, exportCanvas.height);
        ctx2.drawImage(ink, 0, 0, exportCanvas.width, exportCanvas.height);
        blob = await toJpeg(0.7);
      }
      if (blob.size > MAX_BYTES) {
        throw new Error(
          `批改图过大（${Math.round(blob.size / 1024 / 1024)}MB），请缩小原图后重试`
        );
      }
      await onComplete(blob);
    } catch (e) {
      setSaveError(e instanceof Error ? e.message : "保存失败");
    } finally {
      setSaving(false);
    }
  }

  const canvasStyle: CSSProperties = {
    display: ready ? "block" : "none",
    maxWidth: "100%",
    width: displaySize.w || undefined,
    height: displaySize.h || undefined,
    borderRadius: 8,
  };

  return (
    <div className="stack">
      <div className="row" style={{ flexWrap: "wrap", gap: 8 }}>
        <button
          className="btn secondary"
          type="button"
          onClick={() => setTool("pen")}
          style={tool === "pen" ? { outline: "2px solid var(--accent)" } : undefined}
        >
          画笔
        </button>
        <button
          className="btn secondary"
          type="button"
          onClick={() => setTool("eraser")}
          style={tool === "eraser" ? { outline: "2px solid var(--accent)" } : undefined}
        >
          橡皮
        </button>
        {COLORS.map((c) => (
          <button
            key={c.id}
            className="btn secondary"
            type="button"
            onClick={() => {
              setColor(c.value);
              setTool("pen");
            }}
            title={c.label}
            style={{
              width: 36,
              padding: 0,
              background: c.value,
              color: "#fff",
              outline: color === c.value && tool === "pen" ? "2px solid #111" : undefined,
            }}
          >
            {c.label}
          </button>
        ))}
        {WIDTHS.map((w) => (
          <button
            key={w.id}
            className="btn secondary"
            type="button"
            onClick={() => setWidth(w.value)}
            style={width === w.value ? { outline: "2px solid var(--accent)" } : undefined}
          >
            {w.label}
          </button>
        ))}
        <button className="btn secondary" type="button" onClick={undo} disabled={strokeCount === 0}>
          撤销
        </button>
        <button
          className="btn secondary"
          type="button"
          onClick={clearAll}
          disabled={strokeCount === 0}
        >
          清空
        </button>
      </div>

      {loadError && <p className="error">{loadError}</p>}
      {saveError && <p className="error">{saveError}</p>}

      <div ref={containerRef} style={{ width: "100%", overflow: "auto" }}>
        {!ready && !loadError ? <p className="muted">正在加载画布…</p> : null}
        <div
          style={{
            position: "relative",
            width: displaySize.w || undefined,
            height: displaySize.h || undefined,
            maxWidth: "100%",
          }}
        >
          <canvas
            ref={baseRef}
            style={{
              ...canvasStyle,
              border: "1px solid var(--line)",
              background: "#f8f8f8",
            }}
          />
          <canvas
            ref={inkRef}
            onPointerDown={onPointerDown}
            onPointerMove={onPointerMove}
            onPointerUp={onPointerUp}
            onPointerCancel={onPointerUp}
            style={{
              ...canvasStyle,
              position: "absolute",
              left: 0,
              top: 0,
              touchAction: "none",
              cursor: tool === "eraser" ? "cell" : "crosshair",
            }}
          />
        </div>
      </div>

      <div className="row" style={{ gap: 8 }}>
        <button className="btn secondary" type="button" onClick={onCancel} disabled={saving}>
          退出批改
        </button>
        <button className="btn" type="button" onClick={() => void finish()} disabled={!ready || saving}>
          {saving ? "上传中…" : "完成圈画并上传"}
        </button>
      </div>
    </div>
  );
}
