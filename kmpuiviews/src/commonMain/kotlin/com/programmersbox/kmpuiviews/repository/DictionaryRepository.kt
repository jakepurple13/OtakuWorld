package com.programmersbox.kmpuiviews.repository


import com.programmersbox.favoritesdatabase.DictionaryDao
import com.programmersbox.favoritesdatabase.DictionaryEntry
import kotlinx.coroutines.flow.Flow

enum class DictionarySort { Term, DateAdded, Category }

class DictionaryRepository(
    private val dao: DictionaryDao,
    private val translationService: TranslationService,
) {
    fun getById(id: Long): Flow<DictionaryEntry?> = dao.getById(id)

    fun getAll(sort: DictionarySort): Flow<List<DictionaryEntry>> = when (sort) {
        DictionarySort.Term -> dao.getAllByTerm()
        DictionarySort.DateAdded -> dao.getAllByDateAdded()
        DictionarySort.Category -> dao.getAllByCategory()
    }

    fun search(query: String): Flow<List<DictionaryEntry>> = dao.search(query)

    suspend fun save(entry: DictionaryEntry): Long =
        if (entry.id == 0L) {
            dao.insert(entry)
        } else {
            dao.update(entry)
            entry.id
        }

    suspend fun delete(entry: DictionaryEntry) = dao.delete(entry)

    suspend fun translateTerm(
        term: String,
        sourceLanguage: String,
        targetLanguage: String,
    ): TranslationResult = translationService.translateTerm(term, sourceLanguage, targetLanguage)
}
