package com.korimuspast1.lexora.core.database

import androidx.room.TypeConverter
import com.korimuspast1.lexora.core.database.model.ExerciseKind
import com.korimuspast1.lexora.core.database.model.LeagueId
import com.korimuspast1.lexora.core.database.model.LessonKind
import com.korimuspast1.lexora.core.database.model.ProgressStatus
import com.korimuspast1.lexora.core.database.model.QuestKind
import com.korimuspast1.lexora.core.database.model.QuestStatus
import com.korimuspast1.lexora.core.database.model.ShopItemType
import com.korimuspast1.lexora.core.database.model.WordStatus
import java.time.Instant

class LexoraTypeConverters {
    @TypeConverter fun instantToLong(value: Instant?): Long? = value?.toEpochMilli()
    @TypeConverter fun longToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter fun lessonKindToString(value: LessonKind): String = value.name
    @TypeConverter fun stringToLessonKind(value: String): LessonKind = LessonKind.valueOf(value)

    @TypeConverter fun exerciseKindToString(value: ExerciseKind): String = value.name
    @TypeConverter fun stringToExerciseKind(value: String): ExerciseKind = ExerciseKind.valueOf(value)

    @TypeConverter fun progressStatusToString(value: ProgressStatus): String = value.name
    @TypeConverter fun stringToProgressStatus(value: String): ProgressStatus = ProgressStatus.valueOf(value)

    @TypeConverter fun wordStatusToString(value: WordStatus): String = value.name
    @TypeConverter fun stringToWordStatus(value: String): WordStatus = WordStatus.valueOf(value)

    @TypeConverter fun questKindToString(value: QuestKind): String = value.name
    @TypeConverter fun stringToQuestKind(value: String): QuestKind = QuestKind.valueOf(value)

    @TypeConverter fun questStatusToString(value: QuestStatus): String = value.name
    @TypeConverter fun stringToQuestStatus(value: String): QuestStatus = QuestStatus.valueOf(value)

    @TypeConverter fun shopItemTypeToString(value: ShopItemType): String = value.name
    @TypeConverter fun stringToShopItemType(value: String): ShopItemType = ShopItemType.valueOf(value)

    @TypeConverter fun leagueIdToString(value: LeagueId): String = value.name
    @TypeConverter fun stringToLeagueId(value: String): LeagueId = LeagueId.valueOf(value)
}
