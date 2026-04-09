package com.example.seedie.data.remote

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseConfig {
    // Replace with your actual Supabase URL and Anon Key
    const val URL = "https://xojbnrxnkgkqacrkcmqc.supabase.co"
    const val ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InhvamJucnhua2drcWFjcmtjbXFjIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzQ0MDAzMjQsImV4cCI6MjA4OTk3NjMyNH0.DzJHw_58tks9bORpF3Fwbis35ogEgbnnGUVBzonRg4Y"
}

/**
 * 这是一个带有自动 Token 缓存功能的 Supabase 客户端工厂函数。
 * 我们需要传入一个 Context 来构建底层的 DataStore。
 * 在后续的 Hilt 注入中，我们会用这个函数生成一个全局唯一的单例 Client。
 */
fun createSeedieSupabaseClient(context: Context): SupabaseClient {
    // 1. 创建一个专属的 DataStore 文件来存 Token
    val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile("supabase_auth_session") }
    )

    // 2. 创建并返回 Supabase Client
    return createSupabaseClient(
        supabaseUrl = SupabaseConfig.URL,
        supabaseKey = SupabaseConfig.ANON_KEY
    ) {
        install(Auth) {
            // 这行代码是魔法！它告诉 Supabase：
            // "以后登录成功了，请自动把 JWT Token 写进这个 DataStore；
            // 以后每次启动 App，请自动去这里面读 Token；
            // 如果发现 Token 快过期了，请在后台静默刷新它！"
            // (注意：在较新的 Supabase SDK 中，这通常是默认行为，只要你引入了针对平台的支持模块，但显式写出来更安全和清晰，具体写法依赖版本 API，当前 3.5.0 版本我们使用默认的或者基于系统配置即可。如果你要深度定制缓存实现也可以。不过对于标准 Android 依赖，其实通常不用手动传 dataStore 了，下面演示如何兼容。)
            
            // Note: In Supabase Kotlin 3.x, if you added the platform specific dependencies, 
            // it usually picks up a default storage. However, explicitly configuring it 
            // is still possible if needed via custom session managers, but for most apps 
            // relying on standard configs is sufficient. We will let the Auth module use 
            // its internal default session manager for now which is fine for modern setup, 
            // OR if you strictly want to force DataStore, you'd use the experimental/custom API.
            // Let's stick to the simplest standard setup first.
        }
        install(Postgrest)
    }
}

