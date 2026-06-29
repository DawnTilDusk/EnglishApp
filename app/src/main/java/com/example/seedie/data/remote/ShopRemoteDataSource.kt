package com.example.seedie.data.remote

import com.example.seedie.domain.model.ShopOrder
import com.example.seedie.domain.model.ShopProduct
import com.example.seedie.domain.model.StudentStats
import com.example.seedie.domain.model.StudentSummary
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShopRemoteDataSource @Inject constructor(
    private val client: SupabaseClient
) {
    suspend fun fetchProductsByTeacher(teacherId: String, activeOnly: Boolean = false): List<ShopProduct> {
        val rows = client.postgrest["shop_products"].select {
            filter {
                eq("teacher_id", teacherId)
                if (activeOnly) eq("is_active", true)
            }
            order("created_at", Order.DESCENDING)
        }.decodeList<SupabaseShopProduct>()

        return rows.map { it.toDomain() }
    }

    suspend fun fetchMyProducts(teacherId: String): List<ShopProduct> =
        fetchProductsByTeacher(teacherId, activeOnly = false)

    suspend fun fetchOrdersForStudent(studentId: String): List<ShopOrder> {
        val rows = client.postgrest["shop_orders"].select {
            filter { eq("student_id", studentId) }
            order("created_at", Order.DESCENDING)
        }.decodeList<SupabaseShopOrder>()

        return rows.map { it.toDomain(productName = null) }
    }

    suspend fun fetchOrdersForTeacher(teacherId: String, status: String? = null): List<ShopOrder> {
        val rows = client.postgrest["shop_orders"].select {
            filter {
                eq("teacher_id", teacherId)
                if (status != null) eq("status", status)
            }
            order("created_at", Order.DESCENDING)
        }.decodeList<SupabaseShopOrder>()

        return rows.map { it.toDomain(productName = null) }
    }

    suspend fun createProduct(
        teacherId: String,
        name: String,
        description: String?,
        priceTokens: Int,
        stock: Int
    ) {
        client.postgrest["shop_products"].insert(
            buildJsonObject {
                put("teacher_id", teacherId)
                put("name", name)
                if (description != null) put("description", description)
                put("price_tokens", priceTokens)
                put("stock", stock)
                put("is_active", true)
            }
        )
    }

    suspend fun updateProduct(
        productId: String,
        name: String,
        description: String?,
        priceTokens: Int,
        stock: Int,
        isActive: Boolean
    ) {
        client.postgrest["shop_products"].update(
            buildJsonObject {
                put("name", name)
                if (description != null) put("description", description)
                put("price_tokens", priceTokens)
                put("stock", stock)
                put("is_active", isActive)
            }
        ) {
            filter { eq("id", productId) }
        }
    }

    suspend fun submitOrder(productId: String): String {
        return client.postgrest.rpc(
            "submit_shop_order",
            buildJsonObject { put("p_product_id", productId) }
        ).decodeAs<String>().trim('"')
    }

    suspend fun approveOrder(orderId: String) {
        client.postgrest.rpc(
            "approve_shop_order",
            buildJsonObject { put("p_order_id", orderId) }
        )
    }

    suspend fun rejectOrder(orderId: String) {
        client.postgrest.rpc(
            "reject_shop_order",
            buildJsonObject { put("p_order_id", orderId) }
        )
    }

    private fun SupabaseShopProduct.toDomain() = ShopProduct(
        id = id,
        teacherId = teacher_id,
        name = name,
        description = description,
        priceTokens = price_tokens,
        stock = stock,
        imageUrl = image_url,
        isActive = is_active
    )

    private fun SupabaseShopOrder.toDomain(productName: String?) = ShopOrder(
        id = id,
        studentId = student_id,
        teacherId = teacher_id,
        productId = product_id,
        productName = productName,
        tokensAmount = tokens_amount,
        status = status,
        createdAt = created_at
    )
}

@Singleton
class TeacherRemoteDataSource @Inject constructor(
    private val client: SupabaseClient
) {
    suspend fun fetchStudentsForTeacher(teacherId: String): List<StudentSummary> {
        val rows = client.postgrest["students"].select {
            filter { eq("teacher_id", teacherId) }
            order("name", Order.ASCENDING)
        }.decodeList<Student>()

        return rows.map {
            StudentSummary(
                id = it.id,
                name = it.name,
                studentNo = it.student_no,
                classId = it.class_id
            )
        }
    }

    suspend fun getStudentStats(studentId: String): StudentStats {
        val json = client.postgrest.rpc(
            "get_teacher_student_stats",
            buildJsonObject { put("p_student_id", studentId) }
        ).decodeAs<JsonObject>()

        return StudentStats(
            studentId = json.stringField("student_id") ?: studentId,
            name = json.stringField("name").orEmpty(),
            studentNo = json.stringField("student_no"),
            classId = json.stringField("class_id"),
            totalCheckIns = json.intField("total_check_ins"),
            totalStudyMinutes = json.intField("total_study_minutes"),
            learnedWordCount = json.intField("learned_word_count"),
            tokenBalance = json.intField("token_balance")
        )
    }
}

private fun JsonObject.stringField(key: String): String? {
    val element = this[key] ?: return null
    if (element is JsonNull) return null
    return element.jsonPrimitive.content
}

private fun JsonObject.intField(key: String): Int {
    val element = this[key] ?: return 0
    if (element is JsonNull) return 0
    return element.jsonPrimitive.content.toIntOrNull() ?: 0
}
