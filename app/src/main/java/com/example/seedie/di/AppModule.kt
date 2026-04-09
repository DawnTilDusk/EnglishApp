package com.example.seedie.di

import android.content.Context
import com.example.seedie.data.remote.AuthService
import com.example.seedie.data.remote.createSeedieSupabaseClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(@ApplicationContext context: Context): SupabaseClient {
        // Hilt 会自动注入 Application Context，我们用它来创建并返回单例 Client
        return createSeedieSupabaseClient(context)
    }

    @Provides
    @Singleton
    fun provideAuthService(supabaseClient: SupabaseClient): AuthService {
        // Hilt 发现 AuthService 需要 SupabaseClient，它会自动把上面那个传进来
        return AuthService(supabaseClient)
    }
}
