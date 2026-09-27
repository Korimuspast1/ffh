package com.korimuspast1.lexora.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.korimuspast1.lexora.core.database.model.CourseEntity
import com.korimuspast1.lexora.core.database.model.ExerciseEntity
import com.korimuspast1.lexora.core.database.model.GrammarRuleEntity
import com.korimuspast1.lexora.core.database.model.LanguageEntity
import com.korimuspast1.lexora.core.database.model.LessonEntity
import com.korimuspast1.lexora.core.database.model.StoryEntity
import com.korimuspast1.lexora.core.database.model.UnitEntity
import com.korimuspast1.lexora.core.database.model.WordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Upsert suspend fun upsertLanguages(items: List<LanguageEntity>)
    @Upsert suspend fun upsertCourses(items: List<CourseEntity>)
    @Upsert suspend fun upsertUnits(items: List<UnitEntity>)
    @Upsert suspend fun upsertLessons(items: List<LessonEntity>)
    @Upsert suspend fun upsertExercises(items: List<ExerciseEntity>)
    @Upsert suspend fun upsertWords(items: List<WordEntity>)
    @Upsert suspend fun upsertGrammarRules(items: List<GrammarRuleEntity>)
    @Upsert suspend fun upsertStories(items: List<StoryEntity>)

    @Query("SELECT * FROM languages ORDER BY sortOrder ASC")
    fun observeLanguages(): Flow<List<LanguageEntity>>

    @Query("SELECT * FROM courses WHERE id = :courseId LIMIT 1")
    fun observeCourse(courseId: String): Flow<CourseEntity?>

    @Query("SELECT * FROM courses ORDER BY title ASC")
    fun observeCourses(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM units WHERE courseId = :courseId ORDER BY orderIndex ASC")
    fun observeUnits(courseId: String): Flow<List<UnitEntity>>

    @Query("SELECT * FROM lessons WHERE unitId = :unitId ORDER BY orderIndex ASC")
    fun observeLessons(unitId: String): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons WHERE id = :lessonId LIMIT 1")
    fun observeLesson(lessonId: String): Flow<LessonEntity?>

    @Query("SELECT * FROM exercises WHERE lessonId = :lessonId ORDER BY orderIndex ASC")
    fun observeExercises(lessonId: String): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM words WHERE languageId = :languageId AND (word LIKE '%' || :query || '%' OR translation LIKE '%' || :query || '%') ORDER BY word LIMIT :limit")
    fun searchWords(languageId: String, query: String, limit: Int): Flow<List<WordEntity>>

    @Query("SELECT * FROM grammar_rules WHERE languageId = :languageId ORDER BY unitId, orderIndex")
    fun observeGrammar(languageId: String): Flow<List<GrammarRuleEntity>>

    @Query("SELECT * FROM stories WHERE languageId = :languageId ORDER BY unitId, orderIndex")
    fun observeStories(languageId: String): Flow<List<StoryEntity>>

    @Transaction
    suspend fun replaceCourseContent(
        languages: List<LanguageEntity>,
        courses: List<CourseEntity>,
        units: List<UnitEntity>,
        lessons: List<LessonEntity>,
        exercises: List<ExerciseEntity>,
        words: List<WordEntity>,
        grammarRules: List<GrammarRuleEntity>,
        stories: List<StoryEntity>,
    ) {
        upsertLanguages(languages)
        upsertCourses(courses)
        upsertUnits(units)
        upsertLessons(lessons)
        upsertExercises(exercises)
        upsertWords(words)
        upsertGrammarRules(grammarRules)
        upsertStories(stories)
    }
}
