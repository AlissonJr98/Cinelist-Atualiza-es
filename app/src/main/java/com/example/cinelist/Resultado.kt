package com.example.cinelist

/**
 * Classe selada para representar resultados de operações.
 * 
 * @param T Tipo do dado em caso de sucesso
 */
sealed class Resultado<out T> {
    /** Operação realizada com sucesso */
    data class Sucesso<T>(val dados: T) : Resultado<T>()
    
    /** Operação falhou */
    data class Erro(
        val mensagem: String,
        val causa: Throwable? = null,
        val codigo: Int? = null
    ) : Resultado<Nothing>()
    
    /** Operação em andamento */
    object Carregando : Resultado<Nothing>()
}

/**
 * Extensão para facilitar o uso de Resultado em Flows
 */
fun <T> Resultado<T>.getOrThrow(): T = when (this) {
    is Resultado.Sucesso -> dados
    is Resultado.Erro -> throw causa ?: Exception(mensagem)
    is Resultado.Carregando -> throw IllegalStateException("Resultado ainda está carregando")
}

/**
 * Extensão para transformar dados de um Resultado
 */
inline fun <T, R> Resultado<T>.map(transform: (T) -> R): Resultado<R> = when (this) {
    is Resultado.Sucesso -> Resultado.Sucesso(transform(dados))
    is Resultado.Erro -> this
    is Resultado.Carregando -> this
}

/**
 * Extensão para executar ação em caso de sucesso
 */
inline fun <T> Resultado<T>.onSuccess(action: (T) -> Unit): Resultado<T> {
    if (this is Resultado.Sucesso) action(dados)
    return this
}

/**
 * Extensão para executar ação em caso de erro
 */
inline fun <T> Resultado<T>.onError(action: (Resultado.Erro) -> Unit): Resultado<T> {
    if (this is Resultado.Erro) action(this)
    return this
}
