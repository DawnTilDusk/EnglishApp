package com.example.seedie.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseConfig {
    // Replace with your actual Supabase URL and Anon Key
    const val URL = "https://xojbnrxnkgkqacrkcmqc.supabase.co"
    const val ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InhvamJucnhua2drcWFjcmtjbXFjIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzQ0MDAzMjQsImV4cCI6MjA4OTk3NjMyNH0.DzJHw_58tks9bORpF3Fwbis35ogEgbnnGUVBzonRg4Y"
}

val supabaseClient: SupabaseClient = createSupabaseClient(
    supabaseUrl = SupabaseConfig.URL,
    supabaseKey = SupabaseConfig.ANON_KEY
) {
    install(Auth)
    install(Postgrest)
}
