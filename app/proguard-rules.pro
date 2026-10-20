# CineList ProGuard / R8 Production Rules

# Rastreio de stacktrace seguro
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod

# Preserva anotações do Gson
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.** { *; }

# Preserva modelos de dados do CineList (usados pelo Gson, Room e Firestore)
-keep class com.example.cinelist.Midia { *; }
-keep class com.example.cinelist.NotificacaoEntity { *; }
-keep class com.example.cinelist.GrupoEntity { *; }
-keep class com.example.cinelist.StatusMidia { *; }
-keep class com.example.cinelist.AvaliacaoMembro { *; }
-keep class com.example.cinelist.MensagemChat { *; }
-keep class com.example.cinelist.MensagemGrupo { *; }
-keep class com.example.cinelist.ComentarioSocial { *; }
-keep class com.example.cinelist.RespostaSocial { *; }
-keep class com.example.cinelist.AmigoPerfil { *; }
-keep class com.example.cinelist.MembroGrupo { *; }
-keep class com.example.cinelist.InfoAtualizacao { *; }
-keep class com.example.cinelist.ResultadoAtualizacao { *; }
-keep class com.example.cinelist.Tmdb* { *; }
-keep class com.example.cinelist.Match* { *; }
-keep class com.example.cinelist.Solicitacao* { *; }

# Retrofit 2
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# OkHttp e Okio
-dontwarn okhttp3.**
-dontwarn okio.**

# Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Firebase
-dontwarn com.google.firebase.**
-keep class com.google.firebase.** { *; }

# Dagger / Hilt
-dontwarn dagger.hilt.**
