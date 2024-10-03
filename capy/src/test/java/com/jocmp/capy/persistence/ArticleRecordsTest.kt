package com.jocmp.capy.persistence

import com.jocmp.capy.Article
import com.jocmp.capy.ArticleStatus
import com.jocmp.capy.InMemoryDatabaseProvider
import com.jocmp.capy.RandomUUID
import com.jocmp.capy.articles.UnreadSortOrder
import com.jocmp.capy.common.TimeHelpers.nowUTC
import com.jocmp.capy.db.Database
import com.jocmp.capy.fixtures.ArticleFixture
import com.jocmp.capy.fixtures.FeedFixture
import com.jocmp.capy.reload
import com.jocmp.capy.repeated
import org.junit.Before
import org.junit.Test
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ArticleRecordsTest {
    private lateinit var database: Database
    private lateinit var articleFixture: ArticleFixture

    @Before
    fun setup() {
        database = InMemoryDatabaseProvider.build("777")
        articleFixture = ArticleFixture(database)
    }

    @Test
    fun allByStatus_returnsAll() {
        val startTime = nowUTC().minusMonths(1)

        val articles = 3.repeated { i ->
            articleFixture.create(
                publishedAt = startTime.minusDays(i.toLong()).toEpochSecond()
            )
        }

        val articleRecords = ArticleRecords(database)

        val results = articleRecords
            .byStatus
            .all(
                ArticleStatus.ALL,
                limit = 3,
                offset = 0,
                unreadSort = UnreadSortOrder.NEWEST_FIRST,
            )
            .executeAsList()

        val count = articleRecords
            .byStatus
            .count(ArticleStatus.ALL)
            .executeAsOne()

        val expected = articles.map { it.id }
        val actual = results.map { it.id }

        assertTrue(actual.isNotEmpty())
        assertEquals(
            actual = actual,
            expected = expected,
            message = sortedMessage(articles, results)
        )
        assertEquals(expected = 3, actual = count)
    }

    @Test
    fun allByStatus_returnsUnread() {
        val startTime = nowUTC().minusMonths(1)

        val articles = 3.repeated { i ->
            articleFixture.create(
                publishedAt = startTime.minusDays(i.toLong()).toEpochSecond()
            )
        }

        val articleRecords = ArticleRecords(database)

        val unread = articles.take(2)
        val unreadIDs = unread.map { it.id }

        unreadIDs.forEach {
            articleRecords.markUnread(it)
        }

        val results = articleRecords
            .byStatus
            .all(
                ArticleStatus.UNREAD,
                limit = 3,
                offset = 0,
                unreadSort = UnreadSortOrder.NEWEST_FIRST,
            )
            .executeAsList()

        val count = articleRecords
            .byStatus
            .count(status = ArticleStatus.UNREAD)
            .executeAsOne()

        val expected = unreadIDs
        val actual = results.map { it.id }

        assertEquals(expected = 2, actual = count)
        assertEquals(actual = actual, expected = expected, message = sortedMessage(unread, results))
    }

    @Test
    fun allByStatus_returnsUnread_oldestFirst() {
        val startTime = nowUTC().minusMonths(1)

        val articles = 3.repeated { i ->
            articleFixture.create(
                publishedAt = startTime.minusDays(i.toLong()).toEpochSecond()
            )
        }
        .reversed()

        val articleRecords = ArticleRecords(database)

        val unread = articles.take(2)
        val unreadIDs = unread.map { it.id }

        unreadIDs.forEach {
            articleRecords.markUnread(it)
        }

        val results = articleRecords
            .byStatus
            .all(
                ArticleStatus.UNREAD,
                limit = 3,
                offset = 0,
                unreadSort = UnreadSortOrder.OLDEST_FIRST,
            )
            .executeAsList()

        val count = articleRecords
            .byStatus
            .count(status = ArticleStatus.UNREAD)
            .executeAsOne()

        val expected = unreadIDs
        val actual = results.map { it.id }

        assertEquals(expected = 2, actual = count)
        assertEquals(actual = actual, expected = expected, message = sortedMessage(unread, results))
    }

    @Test
    fun allByStatus_searchQuery() {
        2.repeated { i ->
            articleFixture.create(
                title = "Test Title $i",
                summary = "my summary $i",
            )
        }
        val articleResult = articleFixture.create(
            title = "The Pixel 9 is great — and a problem",
            summary = "On The Vergecast: AI photos, Chick-fil-A’s foray into streaming, headphone screens, and more.",
        )
        val articleRecords = ArticleRecords(database)
        val query = "problem"

        val results = articleRecords
            .byStatus
            .all(
                status = ArticleStatus.ALL,
                query = query,
                limit = 3,
                offset = 0,
                unreadSort = UnreadSortOrder.NEWEST_FIRST,
            )
            .executeAsList()

        val count = articleRecords
            .byStatus
            .count(
                status = ArticleStatus.ALL,
                query = query,
            )
            .executeAsOne()

        val actualID = results.map { it.id }.first()

        assertEquals(expected = 1, actual = count)
        assertEquals(actual = actualID, expected = articleResult.id)
    }

    @Test
    fun allByStatus_searchQueryInSummary() {
        2.repeated { i ->
            articleFixture.create(
                title = "Test Title $i",
                summary = "my summary $i",
            )
        }
        val articleResult = articleFixture.create(
            title = "The Pixel 9 is great — and a problem",
            summary = "On The Vergecast: AI photos, Chick-fil-A’s foray into streaming, headphone screens, and more.",
        )
        val articleRecords = ArticleRecords(database)
        val query = "Chick-fil-A"

        val results = articleRecords
            .byStatus
            .all(
                status = ArticleStatus.ALL,
                query = query,
                limit = 3,
                offset = 0,
                unreadSort = UnreadSortOrder.NEWEST_FIRST,
            )
            .executeAsList()

        val count = articleRecords
            .byStatus
            .count(
                status = ArticleStatus.ALL,
                query = query,
            )
            .executeAsOne()

        val actualID = results.map { it.id }.first()

        assertEquals(expected = 1, actual = count)
        assertEquals(actual = actualID, expected = articleResult.id)
    }

    @Test
    fun allByFeed_searchQuery() {
        2.repeated { i ->
            articleFixture.create(
                title = "article $i  - i'm from a random feed!",
                summary = "Chick-fil-A $i",
            )
        }

        val vergeFeed = FeedFixture(database).create()

        2.repeated { i ->
            articleFixture.create(
                title = "Test Title $i",
                summary = "my summary $i",
                feed = vergeFeed,
            )
        }
        val articleResult = articleFixture.create(
            title = "The Pixel 9 is great — and a problem",
            summary = "On The Vergecast: AI photos, Chick-fil-A’s foray into streaming, headphone screens, and more.",
            feed = vergeFeed,
        )
        val articleRecords = ArticleRecords(database)
        val query = "Chick-fil-A"

        val since = OffsetDateTime.now().minusDays(1)
        val results = articleRecords
            .byFeed
            .all(
                status = ArticleStatus.ALL,
                query = query,
                feedIDs = listOf(vergeFeed.id),
                since = since,
                limit = 10,
                offset = 0,
                unreadSort = UnreadSortOrder.NEWEST_FIRST,
            )
            .executeAsList()

        val count = articleRecords
            .byFeed
            .count(
                status = ArticleStatus.ALL,
                query = query,
                feedIDs = listOf(vergeFeed.id),
                since = since,
            )
            .executeAsOne()

        val actualID = results.map { it.id }.first()

        assertEquals(expected = 1, actual = count)
        assertEquals(actual = actualID, expected = articleResult.id)
    }

    @Test
    fun markUnread() {
        val article = articleFixture.create()
        val articleRecords = ArticleRecords(database)

        articleRecords.markUnread(articleID = article.id)

        val reloaded = articleRecords.find(articleID = article.id)!!

        assertFalse(reloaded.read)
    }

    @Test
    fun addStar() {
        val article = articleFixture.create()
        val articleRecords = ArticleRecords(database)

        articleRecords.addStar(articleID = article.id)

        val reloaded = articleRecords.find(articleID = article.id)!!

        assertTrue(reloaded.starred)
    }

    @Test
    fun removeStar() {
        val article = articleFixture.create()
        val articleRecords = ArticleRecords(database)

        articleRecords.removeStar(articleID = article.id)

        val reloaded = articleRecords.find(articleID = article.id)!!

        assertFalse(reloaded.starred)
    }

    @Test
    fun markAllUnread() {
        val articleIDs = 3.repeated { RandomUUID.generate() }
        val articleRecords = ArticleRecords(database)

        articleRecords.markAllUnread(articleIDs)

        val articles = articleIDs.map { id ->
            articleFixture.create(id = id)
        }

        assertTrue(articles.none { it.read })
    }

    @Test
    fun deleteOldArticles() {
        val oldPublishedAt = nowUTC().minusMonths(4).toEpochSecond()
        val articleRecords = ArticleRecords(database)

        val oldArticle = articleFixture.create(publishedAt = oldPublishedAt)
        val oldUnreadArticle = articleFixture.create(publishedAt = oldPublishedAt, read = false)
        val newArticle = articleFixture.create()
        val oldStarredArticle = articleFixture.create(publishedAt = oldPublishedAt).run {
            articleRecords.markAllStarred(listOf(id))
            articleRecords.find(id)!!
        }

        assertNotNull(articleRecords.reload(newArticle))
        assertNotNull(articleRecords.reload(oldUnreadArticle))
        assertNotNull(articleRecords.reload(oldStarredArticle))
        assertNotNull(articleRecords.reload(oldArticle))

        articleRecords.deleteOldArticles(before = nowUTC())

        assertNotNull(articleRecords.reload(newArticle))
        assertNotNull(articleRecords.reload(oldUnreadArticle))
        assertNotNull(articleRecords.reload(oldStarredArticle))
        assertNull(articleRecords.reload(oldArticle))
    }

    @Test
    fun deleteAllArticles() {
        val oldPublishedAt = nowUTC().minusMonths(4).toEpochSecond()
        val articleRecords = ArticleRecords(database)

        val oldArticle = articleFixture.create(publishedAt = oldPublishedAt)
        val oldUnreadArticle = articleFixture.create(publishedAt = oldPublishedAt, read = false)
        val newArticle = articleFixture.create()
        val oldStarredArticle = articleFixture.create(publishedAt = oldPublishedAt).run {
            articleRecords.markAllStarred(listOf(id))
            articleRecords.find(id)!!
        }

        assertNotNull(articleRecords.reload(newArticle))
        assertNotNull(articleRecords.reload(oldUnreadArticle))
        assertNotNull(articleRecords.reload(oldStarredArticle))
        assertNotNull(articleRecords.reload(oldArticle))

        articleRecords.deleteAllArticles()

        assertNull(articleRecords.reload(newArticle))
        assertNull(articleRecords.reload(oldUnreadArticle))
        assertNull(articleRecords.reload(oldStarredArticle))
        assertNull(articleRecords.reload(oldArticle))
    }
}

fun sortedMessage(expected: List<Article>, actual: List<Article>): String {
    return "Expected order ${expected.joinToString(", ") { it.title }}; got ${actual.joinToString(", ") { it.title }}"
}
