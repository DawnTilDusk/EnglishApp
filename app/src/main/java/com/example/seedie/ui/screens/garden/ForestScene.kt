package com.example.seedie.ui.screens.garden

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.example.seedie.data.local.entity.GardenPlantEntity
import com.example.seedie.domain.model.GardenSpeciesCatalog
import com.example.seedie.domain.usecase.ForestGridCell
import com.example.seedie.domain.usecase.ForestLayout
import com.example.seedie.domain.usecase.PlacedTree
import kotlin.math.hypot

@Composable
fun ForestScene(
    cells: List<ForestGridCell>,
    selectedPlantId: String?,
    onTreeClick: (PlacedTree) -> Unit,
    modifier: Modifier = Modifier
) {
    val cellList = remember(cells) { cells }
    val gridSize = ForestLayout.GRID_SIZE

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(cellList, gridSize) {
                    detectTapGestures { offset ->
                        val metrics = IsoMetrics.fromCanvas(size.width.toFloat(), size.height.toFloat(), gridSize)
                        val hit = cellList
                            .sortedByDescending { it.depth }
                            .firstOrNull { cell ->
                                val tree = cell.tree ?: return@firstOrNull false
                                pointInDiamond(offset, cell, metrics)
                            }
                            ?.tree
                        if (hit != null) onTreeClick(hit)
                    }
                }
        ) {
            val metrics = IsoMetrics.fromCanvas(size.width, size.height, gridSize)
            drawSky()
            drawGardenBoard(metrics)

            val ordered = cellList.sortedWith(
                compareBy<ForestGridCell> { it.row + it.col }.thenBy { it.col }
            )
            ordered.forEach { cell ->
                drawSoilDiamond(cell, metrics)
            }
            ordered.forEach { cell ->
                val tree = cell.tree ?: return@forEach
                val center = metrics.cellCenter(cell.col, cell.row)
                drawTreeSprite(
                    center = center,
                    scale = tree.scale,
                    tileW = metrics.tileW,
                    speciesId = tree.speciesId,
                    withered = tree.status == GardenPlantEntity.STATUS_WITHERED,
                    selected = tree.plantId == selectedPlantId
                )
            }
        }
    }
}

private data class IsoMetrics(
    val originX: Float,
    val originY: Float,
    val tileW: Float,
    val tileH: Float,
    val gridSize: Int
) {
    fun project(col: Float, row: Float): Offset {
        // Classic isometric (pseudo-3D diamonds). All cells share the same tileW/tileH.
        val x0 = (col - row) * (tileW / 2f)
        val y0 = (col + row) * (tileH / 2f)
        return Offset(
            x = originX + x0,
            y = originY + y0
        )
    }

    fun cellCenter(col: Int, row: Int): Offset =
        project(col + 0.5f, row + 0.5f)

    fun cellPath(col: Int, row: Int): Path {
        val p0 = project(col.toFloat(), row.toFloat())
        val p1 = project(col + 1f, row.toFloat())
        val p2 = project(col + 1f, row + 1f)
        val p3 = project(col.toFloat(), row + 1f)
        return Path().apply {
            moveTo(p0.x, p0.y)
            lineTo(p1.x, p1.y)
            lineTo(p2.x, p2.y)
            lineTo(p3.x, p3.y)
            close()
        }
    }

    companion object {
        fun fromCanvas(width: Float, height: Float, gridSize: Int): IsoMetrics {
            val padX = width * 0.08f
            val padY = height * 0.10f
            val usableW = width - padX * 2f
            val usableH = height - padY * 2f
            val n = gridSize.toFloat()
            // Isometric aspect: height diagonal ~ half width → flattened diamonds (pseudo-3D).
            // Fit board (width = n*tileW, height = n*tileH) inside usable area.
            val tileWByWidth = usableW / n
            val tileWByHeight = (usableH / n) / 0.5f
            val tileW = minOf(tileWByWidth, tileWByHeight)
            val tileH = tileW * 0.5f
            val originX = width / 2f
            val boardH = n * tileH
            val originY = padY + (usableH - boardH) / 2f
            return IsoMetrics(originX, originY, tileW, tileH, gridSize)
        }
    }
}

private fun pointInDiamond(point: Offset, cell: ForestGridCell, metrics: IsoMetrics): Boolean {
    val pathPts = listOf(
        metrics.project(cell.col.toFloat(), cell.row.toFloat()),
        metrics.project(cell.col + 1f, cell.row.toFloat()),
        metrics.project(cell.col + 1f, cell.row + 1f),
        metrics.project(cell.col.toFloat(), cell.row + 1f)
    )
    // Ray casting
    var inside = false
    var j = pathPts.lastIndex
    for (i in pathPts.indices) {
        val pi = pathPts[i]
        val pj = pathPts[j]
        val intersect = ((pi.y > point.y) != (pj.y > point.y)) &&
            (point.x < (pj.x - pi.x) * (point.y - pi.y) / (pj.y - pi.y + 1e-6f) + pi.x)
        if (intersect) inside = !inside
        j = i
    }
    if (inside) return true
    // Fallback near center for easier taps
    val c = metrics.cellCenter(cell.col, cell.row)
    val reach = hypot(metrics.tileW, metrics.tileH) * 0.22f * cell.scale
    return hypot(point.x - c.x, point.y - c.y) <= reach
}

private fun DrawScope.drawSky() {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFBFDFF0), Color(0xFFE8F5E9), Color(0xFFC8E6C9)),
            startY = 0f,
            endY = size.height
        )
    )
}

private fun DrawScope.drawGardenBoard(metrics: IsoMetrics) {
    val n = metrics.gridSize.toFloat()
    val tipFar = metrics.project(0f, 0f)
    val tipRight = metrics.project(n, 0f)
    val tipNear = metrics.project(n, n)
    val tipLeft = metrics.project(0f, n)
    val board = Path().apply {
        moveTo(tipFar.x, tipFar.y)
        lineTo(tipRight.x, tipRight.y)
        lineTo(tipNear.x, tipNear.y)
        lineTo(tipLeft.x, tipLeft.y)
        close()
    }
    // Soft shadow under board
    val shadow = Path().apply {
        val dy = metrics.tileH * 0.35f
        moveTo(tipFar.x, tipFar.y + dy * 0.2f)
        lineTo(tipRight.x + metrics.tileW * 0.05f, tipRight.y + dy * 0.4f)
        lineTo(tipNear.x, tipNear.y + dy)
        lineTo(tipLeft.x - metrics.tileW * 0.05f, tipLeft.y + dy * 0.4f)
        close()
    }
    drawPath(shadow, Color(0x33000000))
    drawPath(
        board,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF81C784), Color(0xFF66BB6A), Color(0xFF4CAF50))
        )
    )
    drawPath(board, Color(0xFF2E7D32).copy(alpha = 0.35f), style = Stroke(width = 2.5f))
}

private fun DrawScope.drawSoilDiamond(cell: ForestGridCell, metrics: IsoMetrics) {
    val path = metrics.cellPath(cell.col, cell.row)
    // Same-size isometric diamonds; mild depth tint only (no size warp).
    val farSoil = Color(0xFFBCAAA4)
    val nearSoil = Color(0xFF8D6E63)
    val soil = lerpColor(farSoil, nearSoil, cell.depth * 0.55f)
    val stroke = lerpColor(Color(0xFF8D6E63), Color(0xFF5D4037), cell.depth * 0.55f)
    drawPath(path, soil.copy(alpha = 0.92f))
    drawPath(path, stroke.copy(alpha = 0.6f), style = Stroke(width = 1.6f))
    drawPath(path, Color.White.copy(alpha = 0.08f), style = Stroke(width = 1f))
}

private fun DrawScope.drawTreeSprite(
    center: Offset,
    scale: Float,
    tileW: Float,
    speciesId: String,
    withered: Boolean,
    selected: Boolean
) {
    val species = GardenSpeciesCatalog.requireById(speciesId)
    val accent = Color(species.accentColorArgb)
    val canopy = if (withered) Color(0xFF9E9E9E) else accent
    val trunk = if (withered) Color(0xFF795548) else Color(0xFF5D4037)
    // Size relative to soil tile, then apply depth scale (far≈0.48 → near≈1.28).
    val baseScale = tileW * 0.48f * scale

    drawOval(
        color = Color(0x33000000),
        topLeft = Offset(center.x - baseScale * 0.42f, center.y - baseScale * 0.08f),
        size = Size(baseScale * 0.84f, baseScale * 0.24f)
    )
    drawRect(
        color = trunk,
        topLeft = Offset(center.x - baseScale * 0.07f, center.y - baseScale * 0.52f),
        size = Size(baseScale * 0.14f, baseScale * 0.52f)
    )
    when (speciesId) {
        "sunny_flower" -> {
            drawCircle(color = canopy, radius = baseScale * 0.26f, center = Offset(center.x, center.y - baseScale * 0.68f))
            drawCircle(color = Color(0xFFFFF176), radius = baseScale * 0.11f, center = Offset(center.x, center.y - baseScale * 0.68f))
        }
        "bamboo" -> {
            drawRect(
                color = canopy,
                topLeft = Offset(center.x - baseScale * 0.09f, center.y - baseScale * 1.05f),
                size = Size(baseScale * 0.18f, baseScale * 1.05f)
            )
            drawCircle(
                color = canopy.copy(alpha = 0.85f),
                radius = baseScale * 0.2f,
                center = Offset(center.x, center.y - baseScale * 1.0f)
            )
        }
        else -> {
            drawCircle(color = canopy, radius = baseScale * 0.4f, center = Offset(center.x, center.y - baseScale * 0.72f))
            drawCircle(
                color = canopy.copy(alpha = 0.88f),
                radius = baseScale * 0.3f,
                center = Offset(center.x - baseScale * 0.16f, center.y - baseScale * 0.52f)
            )
            drawCircle(
                color = canopy.copy(alpha = 0.88f),
                radius = baseScale * 0.3f,
                center = Offset(center.x + baseScale * 0.16f, center.y - baseScale * 0.52f)
            )
        }
    }
    if (selected) {
        drawCircle(
            color = Color(0x66FFFFFF),
            radius = baseScale * 0.52f,
            center = Offset(center.x, center.y - baseScale * 0.62f)
        )
    }
}

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

private fun lerpColor(a: Color, b: Color, t: Float): Color = Color(
    red = lerp(a.red, b.red, t),
    green = lerp(a.green, b.green, t),
    blue = lerp(a.blue, b.blue, t),
    alpha = lerp(a.alpha, b.alpha, t)
)
