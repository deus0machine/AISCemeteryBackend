# API Поиска Мемориалов

## Обзор

Система поиска мемориалов предоставляет несколько эндпоинтов для различных типов поиска:

1. **Простой поиск** - базовый поиск по ФИО и локации
2. **Расширенный поиск** - детальный поиск с множеством фильтров
3. **Быстрый поиск** - для автодополнения в UI
4. **Поиск по годовщинам** - поиск дней рождения/смерти
5. **Поиск с фильтрами** - через POST с DTO объектом

## Эндпоинты

### 1. Простой поиск
```http
GET /api/memorials/search
```

**Параметры:**
- `query` (optional) - поисковый запрос по ФИО
- `location` (optional) - местоположение
- `startDate` (optional) - дата рождения от (YYYY-MM-DD)
- `endDate` (optional) - дата смерти до (YYYY-MM-DD)
- `isPublic` (optional) - публичность (true/false)
- `page` (default: 0) - номер страницы
- `size` (default: 10) - размер страницы

**Пример запроса:**
```http
GET /api/memorials/search?query=Иванов&location=Москва&page=0&size=20
```

### 2. Расширенный поиск
```http
GET /api/memorials/search/advanced
```

**Параметры:**
- `query` (optional) - общий поисковый запрос
- `firstName` (optional) - имя
- `lastName` (optional) - фамилия
- `middleName` (optional) - отчество
- `birthDateFrom` (optional) - дата рождения от
- `birthDateTo` (optional) - дата рождения до
- `deathDateFrom` (optional) - дата смерти от
- `deathDateTo` (optional) - дата смерти до
- `location` (optional) - местоположение
- `isPublic` (optional) - публичность
- `sortBy` (default: "lastName") - поле сортировки
- `sortDirection` (default: "asc") - направление сортировки
- `page` (default: 0) - номер страницы
- `size` (default: 10) - размер страницы

**Доступные поля для сортировки:**
- `lastName` - по фамилии
- `firstName` - по имени
- `birthDate` - по дате рождения
- `deathDate` - по дате смерти
- `createdAt` - по дате создания

**Пример запроса:**
```http
GET /api/memorials/search/advanced?firstName=Иван&lastName=Петров&birthDateFrom=1950-01-01&birthDateTo=1980-12-31&sortBy=birthDate&sortDirection=desc
```

### 3. Быстрый поиск (автодополнение)
```http
GET /api/memorials/search/quick
```

**Параметры:**
- `query` (required) - поисковый запрос
- `limit` (default: 10) - максимальное количество результатов

**Пример запроса:**
```http
GET /api/memorials/search/quick?query=Ив&limit=5
```

### 4. Поиск по годовщинам
```http
GET /api/memorials/search/anniversaries
```

**Параметры:**
- `type` (required) - тип годовщины ("birth" или "death")
- `month` (optional) - месяц (1-12)
- `day` (optional) - день (1-31)
- `page` (default: 0) - номер страницы
- `size` (default: 10) - размер страницы

**Примеры запросов:**
```http
# Все дни рождения в декабре
GET /api/memorials/search/anniversaries?type=birth&month=12

# Дни рождения 25 декабря
GET /api/memorials/search/anniversaries?type=birth&month=12&day=25

# Все дни смерти в текущем месяце
GET /api/memorials/search/anniversaries?type=death&month=6
```

### 5. Поиск с фильтрами (POST)
```http
POST /api/memorials/search/filter
```

**Тело запроса (JSON):**
```json
{
  "query": "Иванов",
  "firstName": "Иван",
  "lastName": "Петров",
  "middleName": "Сергеевич",
  "birthDateFrom": "1950-01-01",
  "birthDateTo": "1980-12-31",
  "deathDateFrom": "2000-01-01",
  "deathDateTo": "2023-12-31",
  "location": "Москва",
  "isPublic": true,
  "sortBy": "lastName",
  "sortDirection": "asc",
  "page": 0,
  "size": 20
}
```

### 6. Статистика поиска
```http
GET /api/memorials/search/stats
```

**Ответ:**
```json
{
  "totalMemorials": 1500,
  "publicMemorials": 1200,
  "publishedMemorials": 1100,
  "memorialsThisYear": 150,
  "memorialsLastYear": 200,
  "popularLocations": [
    "Москва",
    "Санкт-Петербург",
    "Новосибирск",
    "Екатеринбург",
    "Казань"
  ]
}
```

## Формат ответа

### Пагинированный ответ (PagedResponse)
```json
{
  "content": [
    {
      "id": 1,
      "firstName": "Иван",
      "lastName": "Петров",
      "middleName": "Сергеевич",
      "birthDate": "1955-03-15",
      "deathDate": "2020-11-20",
      "biography": "Краткая биография...",
      "photoUrl": "https://example.com/photo.jpg",
      "isPublic": true,
      "mainLocation": {
        "latitude": 55.7558,
        "longitude": 37.6176,
        "address": "Москва, Россия"
      },
      "burialLocation": {
        "latitude": 55.7558,
        "longitude": 37.6176,
        "address": "Ваганьковское кладбище"
      }
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 150,
  "totalPages": 15,
  "first": true,
  "last": false
}
```

### Быстрый поиск (List)
```json
[
  {
    "id": 1,
    "firstName": "Иван",
    "lastName": "Иванов",
    "middleName": "Петрович",
    "birthDate": "1960-05-10",
    "deathDate": "2021-03-15"
  },
  {
    "id": 2,
    "firstName": "Ирина",
    "lastName": "Иванова",
    "middleName": "Сергеевна",
    "birthDate": "1965-08-22",
    "deathDate": null
  }
]
```

## Примеры использования в Android

### Kotlin/Retrofit интерфейс
```kotlin
interface MemorialSearchApi {
    
    @GET("memorials/search")
    suspend fun searchMemorials(
        @Query("query") query: String?,
        @Query("location") location: String?,
        @Query("startDate") startDate: String?,
        @Query("endDate") endDate: String?,
        @Query("isPublic") isPublic: Boolean?,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10
    ): PagedResponse<MemorialDTO>
    
    @GET("memorials/search/advanced")
    suspend fun advancedSearch(
        @Query("query") query: String?,
        @Query("firstName") firstName: String?,
        @Query("lastName") lastName: String?,
        @Query("middleName") middleName: String?,
        @Query("birthDateFrom") birthDateFrom: String?,
        @Query("birthDateTo") birthDateTo: String?,
        @Query("deathDateFrom") deathDateFrom: String?,
        @Query("deathDateTo") deathDateTo: String?,
        @Query("location") location: String?,
        @Query("isPublic") isPublic: Boolean?,
        @Query("sortBy") sortBy: String = "lastName",
        @Query("sortDirection") sortDirection: String = "asc",
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10
    ): PagedResponse<MemorialDTO>
    
    @GET("memorials/search/quick")
    suspend fun quickSearch(
        @Query("query") query: String,
        @Query("limit") limit: Int = 10
    ): List<MemorialDTO>
    
    @GET("memorials/search/anniversaries")
    suspend fun searchAnniversaries(
        @Query("type") type: String,
        @Query("month") month: Int?,
        @Query("day") day: Int?,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10
    ): PagedResponse<MemorialDTO>
    
    @POST("memorials/search/filter")
    suspend fun searchWithFilter(
        @Body searchFilter: MemorialSearchDTO
    ): PagedResponse<MemorialDTO>
    
    @GET("memorials/search/stats")
    suspend fun getSearchStats(): SearchStatsDTO
}
```

### Пример использования
```kotlin
class MemorialSearchRepository(private val api: MemorialSearchApi) {
    
    suspend fun searchMemorials(query: String): PagedResponse<MemorialDTO> {
        return api.searchMemorials(
            query = query,
            isPublic = true,
            page = 0,
            size = 20
        )
    }
    
    suspend fun quickSearch(query: String): List<MemorialDTO> {
        return api.quickSearch(query, limit = 5)
    }
    
    suspend fun searchBirthdays(month: Int): PagedResponse<MemorialDTO> {
        return api.searchAnniversaries(
            type = "birth",
            month = month,
            day = null
        )
    }
}
```

## Особенности поиска

1. **Поиск нечувствителен к регистру**
2. **Поддерживает частичное совпадение** (LIKE '%query%')
3. **Автоматически ищет по всем вариантам ФИО** (Фамилия Имя, Имя Фамилия, полное ФИО)
4. **Фильтрует только опубликованные и незаблокированные мемориалы**
5. **Быстрый поиск приоритизирует точные совпадения**
6. **Поддерживает сортировку по различным полям**

## Рекомендации для UI

1. **Используйте быстрый поиск** для автодополнения в поисковой строке
2. **Реализуйте дебаунсинг** для избежания частых запросов
3. **Показывайте индикатор загрузки** при выполнении поиска
4. **Кэшируйте результаты** для улучшения производительности
5. **Предоставьте фильтры** для расширенного поиска
6. **Показывайте статистику** для лучшего UX 