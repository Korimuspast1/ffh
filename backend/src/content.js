const languageSeed = [
  ["en", "English", "English", false, "flag_en", ["hello", "thanks", "family", "work", "travel", "food", "city", "study", "music", "today"]],
  ["es", "Spanish", "Español", false, "flag_es", ["hola", "gracias", "familia", "trabajo", "viaje", "comida", "ciudad", "estudio", "música", "hoy"]],
  ["de", "German", "Deutsch", false, "flag_de", ["hallo", "danke", "familie", "arbeit", "reise", "essen", "stadt", "lernen", "musik", "heute"]],
  ["fr", "French", "Français", false, "flag_fr", ["bonjour", "merci", "famille", "travail", "voyage", "repas", "ville", "étude", "musique", "aujourd'hui"]],
  ["it", "Italian", "Italiano", false, "flag_it", ["ciao", "grazie", "famiglia", "lavoro", "viaggio", "cibo", "città", "studio", "musica", "oggi"]],
  ["pt", "Portuguese", "Português", false, "flag_pt", ["olá", "obrigado", "família", "trabalho", "viagem", "comida", "cidade", "estudo", "música", "hoje"]],
  ["ja", "Japanese", "日本語", false, "flag_ja", ["こんにちは", "ありがとう", "家族", "仕事", "旅行", "食べ物", "都市", "勉強", "音楽", "今日"]],
  ["ko", "Korean", "한국어", false, "flag_ko", ["안녕하세요", "감사합니다", "가족", "일", "여행", "음식", "도시", "공부", "음악", "오늘"]],
  ["zh", "Chinese", "中文", false, "flag_zh", ["你好", "谢谢", "家庭", "工作", "旅行", "食物", "城市", "学习", "音乐", "今天"]],
  ["ar", "Arabic", "العربية", true, "flag_ar", ["مرحبا", "شكرا", "عائلة", "عمل", "سفر", "طعام", "مدينة", "دراسة", "موسيقى", "اليوم"]],
  ["tr", "Turkish", "Türkçe", false, "flag_tr", ["merhaba", "teşekkürler", "aile", "iş", "seyahat", "yemek", "şehir", "çalışma", "müzik", "bugün"]],
  ["pl", "Polish", "Polski", false, "flag_pl", ["cześć", "dziękuję", "rodzina", "praca", "podróż", "jedzenie", "miasto", "nauka", "muzyka", "dzisiaj"]]
];

const ruMeanings = ["привет", "спасибо", "семья", "работа", "путешествие", "еда", "город", "учёба", "музыка", "сегодня"];
const topics = ["Basics", "People", "Cafe", "Travel", "Stories"];
const lessonKinds = ["Vocabulary", "Grammar", "Listening", "Speaking", "Review"];

function buildCourses() {
  const languages = [];
  const courses = [];
  const units = [];
  const lessons = [];
  const exercises = [];
  const words = [];
  const grammar = [];
  const stories = [];

  languageSeed.forEach(([code, name, nativeName, isRtl, flagVectorKey, baseWords], languageIndex) => {
    const languageId = `lang_${code}`;
    const courseId = `course_${code}_ru`;
    languages.push({ id: languageId, code, name, nativeName, isRtl, flagVectorKey, sortOrder: languageIndex });
    courses.push({ id: courseId, language: languages[languages.length - 1], code: `${code}-ru`, title: `${name} for Russian speakers`, description: `Практический курс ${nativeName}: слова, фразы, аудирование и повторение.`, version: 1 });

    for (let i = 0; i < 520; i += 1) {
      const root = baseWords[i % baseWords.length];
      const meaning = ruMeanings[i % ruMeanings.length];
      words.push({
        id: `word_${code}_${i + 1}`,
        languageId,
        word: i < baseWords.length ? root : `${root}-${Math.floor(i / baseWords.length) + 1}`,
        translation: i < ruMeanings.length ? meaning : `${meaning} ${Math.floor(i / ruMeanings.length) + 1}`,
        transcription: `[${root}]`,
        partOfSpeech: i % 3 === 0 ? "noun" : i % 3 === 1 ? "verb" : "phrase",
        audioUrl: `/api/v1/tts/${code}/${i + 1}`,
        status: "New"
      });
    }

    for (let unitIndex = 0; unitIndex < 5; unitIndex += 1) {
      const unitId = `unit_${code}_${unitIndex + 1}`;
      units.push({ id: unitId, courseId, orderIndex: unitIndex + 1, title: topics[unitIndex], description: `Unit ${unitIndex + 1}: ${topics[unitIndex].toLowerCase()} practice`, colorToken: ["primary", "secondary", "tertiary", "warning", "legendary"][unitIndex] });
      grammar.push({ id: `grammar_${code}_${unitIndex + 1}`, languageId, unitId, title: `${topics[unitIndex]} grammar`, body: `Rule ${unitIndex + 1}: build short useful phrases and repeat them aloud.`, examples: [`${baseWords[unitIndex]} — ${ruMeanings[unitIndex]}`, `${baseWords[(unitIndex + 1) % baseWords.length]}?`] });
      stories.push({ id: `story_${code}_${unitIndex + 1}`, languageId, unitId, title: `${topics[unitIndex]} story`, body: `${baseWords[0]}! ${baseWords[5]} ${baseWords[6]}.`, glossary: baseWords.slice(0, 5).map((word, index) => ({ word, translation: ruMeanings[index] })) });

      for (let lessonIndex = 0; lessonIndex < 5; lessonIndex += 1) {
        const lessonId = `lesson_${code}_${unitIndex + 1}_${lessonIndex + 1}`;
        lessons.push({ id: lessonId, unitId, orderIndex: lessonIndex + 1, title: `${topics[unitIndex]} ${lessonIndex + 1}`, type: lessonKinds[lessonIndex], xpReward: 10 + lessonIndex * 2, estimatedMinutes: 5 + lessonIndex });
        for (let exIndex = 0; exIndex < 8; exIndex += 1) {
          const wordIndex = (unitIndex * 5 + lessonIndex + exIndex) % baseWords.length;
          const answer = baseWords[wordIndex];
          exercises.push({
            id: `exercise_${code}_${unitIndex + 1}_${lessonIndex + 1}_${exIndex + 1}`,
            lessonId,
            orderIndex: exIndex + 1,
            type: ["WordChoice", "SentenceBuilder", "ListeningInput", "GapChoice", "MatchingPairs", "Pronunciation", "TextInsert", "Dictation"][exIndex],
            prompt: `Выбери перевод: ${ruMeanings[wordIndex]}`,
            payload: {
              answer,
              options: [answer, baseWords[(wordIndex + 1) % baseWords.length], baseWords[(wordIndex + 2) % baseWords.length], baseWords[(wordIndex + 3) % baseWords.length]],
              pairs: baseWords.slice(0, 4).map((word, index) => ({ left: word, right: ruMeanings[index] }))
            },
            explanation: `${answer} означает «${ruMeanings[wordIndex]}».`,
            timeLimitSeconds: exIndex % 3 === 0 ? 30 : null
          });
        }
      }
    }
  });

  return { languages, courses, units, lessons, exercises, words, grammar, stories };
}

export const content = buildCourses();

export const achievements = [
  ["wildfire_7", "Wildfire I", "7 days streak", "flame", 7, 20],
  ["scholar_100", "Scholar I", "Complete 100 lessons", "book", 100, 30],
  ["sage_1000", "Sage I", "Earn 1000 XP", "star", 1000, 40],
  ["explorer_5", "Explorer I", "Complete 5 units", "flag", 5, 25],
  ["champion", "Champion", "Reach rank 1 in a league", "trophy", 1, 100],
  ["sharpshooter", "Sharpshooter", "Finish a perfect lesson", "target", 1, 25]
].map(([id, title, description, iconVectorKey, target, rewardGems]) => ({ id, code: id, title, description, iconVectorKey, progress: 0, target, rewardGems }));

export const shopItems = [
  { id: "hearts_5", type: "Hearts", title: "5 hearts", description: "Restore all hearts", priceGems: 50, payload: { hearts: 5 } },
  { id: "xp_boost_15", type: "XpBoost", title: "2x XP 15 minutes", description: "Double XP for a short session", priceGems: 120, payload: { multiplier: 2, minutes: 15 } },
  { id: "streak_freeze", type: "StreakFreeze", title: "Streak freeze", description: "Protect one missed day", priceGems: 200, payload: { freezes: 1 } },
  { id: "theme_ocean", type: "Theme", title: "Ocean theme", description: "Blue-green interface theme", priceGems: 300, payload: { theme: "ocean" } },
  { id: "nori_scarf", type: "MascotCostume", title: "Nori scarf", description: "A hand-drawn scarf for Nori", priceGems: 180, payload: { costume: "scarf" } }
];
