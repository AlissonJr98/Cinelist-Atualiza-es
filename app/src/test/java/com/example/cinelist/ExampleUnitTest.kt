package com.example.cinelist

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import org.junit.Assert.*
import java.io.File

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun appDatabase_builderDoesNotThrowConflictException() {
        val mockContext = mockk<Context>(relaxed = true)
        every { mockContext.applicationContext } returns mockContext
        every { mockContext.getDatabasePath(any()) } returns File("non_existent_test_db")

        val db = AppDatabase.getDatabase(mockContext)
        assertNotNull(db)
        assertEquals(21, AppDatabase.TODAS_AS_MIGRACOES.size)
    }
}