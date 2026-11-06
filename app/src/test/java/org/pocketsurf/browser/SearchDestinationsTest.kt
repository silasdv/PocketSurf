package org.pocketsurf.browser

import org.junit.Assert.*
import org.junit.Test

class SearchDestinationsTest {
 private val history=listOf(Visit("https://github.com/user/repo","My repository",10),Visit("https://github.com/user/repo","My repository",20),Visit("https://reddit.com/r/android","Android",30),Visit("https://github.com/","GitHub",5),Visit("https://github.com/other","Other",1))
 @Test fun frequencyPrecedesRecencyAndSubpagesStayExact(){val pages=SearchDestinations.rank(history);assertEquals("https://github.com/user/repo",pages.first().url);assertEquals(1,pages.count{it.url.contains("github.com")});assertEquals("https://reddit.com/r/android",pages[1].url)}
 @Test fun publicSuffixAndPrivateHostingIdentityKeepIndependentSitesSeparate(){
  assertEquals(SearchDestinations.websiteIdentity("https://www.github.com"),SearchDestinations.websiteIdentity("https://m.github.com/user"))
  assertEquals("example.co.uk",SearchDestinations.websiteIdentity("https://store.example.co.uk/page"))
  assertNotEquals(SearchDestinations.websiteIdentity("https://example.co.uk"),SearchDestinations.websiteIdentity("https://other.co.uk"))
  assertNotEquals(SearchDestinations.websiteIdentity("https://alice.github.io"),SearchDestinations.websiteIdentity("https://bob.github.io"))
  assertNotEquals(SearchDestinations.websiteIdentity("https://alice.blogspot.com"),SearchDestinations.websiteIdentity("https://bob.blogspot.com"))
  assertEquals("127.0.0.1",SearchDestinations.websiteIdentity("http://127.0.0.1/path"))
 }
 @Test fun mostUsedRepresentativeAndRecencySurviveDeduplicationDeletionAndBookmarkFiltering(){
  val repository="https://github.com/user/repository?tab=readme%20here"
  val video="https://m.youtube.com/watch?v=123"
  val visits=List(12){Visit("https://github.com","GitHub",1)}+List(8){Visit("https://github.com/user","User",2)}+
   List(35){Visit(repository,"Repository",3)}+List(18){Visit("https://youtube.com","YouTube",4)}+List(42){Visit(video,"Video",5)}
  val pages=SearchDestinations.rank(visits);assertEquals(2,pages.size);assertEquals(setOf(repository,video),pages.map{it.url}.toSet())
  val suggestions=SearchDestinations.bookmarkSuggestions(emptyList(),visits)
  assertEquals(repository,suggestions.first{it.title=="GitHub"}.url);assertEquals(video,suggestions.first{it.title=="YouTube"}.url)
  val saved=listOf(Visit("https://www.github.com/other","Saved",9));assertFalse(SearchDestinations.bookmarkSuggestions(saved,visits).any{it.title=="GitHub"})
  assertEquals("https://github.com",SearchDestinations.rank(visits.filter{it.url!=repository}).first{SearchDestinations.websiteIdentity(it.url)=="github.com"}.url)
  assertEquals("https://example.com/new",SearchDestinations.rank(listOf(Visit("https://example.com/old","Old",1),Visit("https://example.com/new","New",2))).single().url)
 }
 @Test fun noHistoryProducesNoInventedBubbles(){assertTrue(SearchDestinations.rank(emptyList()).isEmpty())}
 @Test fun capacityHasNoArbitraryEightItemLimit(){assertEquals(0,SearchDestinations.capacity(39,40));assertEquals(3,SearchDestinations.capacity(240,76,6));assertEquals(2,SearchDestinations.capacity(239,76,6));assertEquals(20,SearchDestinations.capacity(1600,76,4))}
 @Test fun identityPreservesEncodedSubpagesAndNormalizesOrigins(){assertEquals(SearchDestinations.identity("https://WWW.Example.com:443"),SearchDestinations.identity("https://example.com/#fragment"));assertNotEquals(SearchDestinations.identity("https://example.com/a"),SearchDestinations.identity("https://example.com/b"));assertNotEquals(SearchDestinations.identity("https://example.com/a%2Fb"),SearchDestinations.identity("https://example.com/a/b"))}
 @Test fun bookmarkCandidatesRankUsageAndRetainDefaultsAsFallback(){
  val defaults=SearchDestinations.bookmarkSuggestions(emptyList(),emptyList());assertEquals(StartPageStore.popular,defaults)
  val ranked=SearchDestinations.bookmarkSuggestions(emptyList(),history);assertEquals("GitHub",ranked.first().title);assertEquals("https://github.com/user/repo",ranked.first().url);assertEquals("Reddit",ranked[1].title)
  val saved=listOf(Visit("https://www.github.com/user/repo","Repository",0));assertTrue(SearchDestinations.bookmarkSuggestions(saved,history).none{it.title=="GitHub"})
  assertEquals(ranked,SearchDestinations.bookmarkSuggestions(emptyList(),history))
  val wiki=List(12){Visit("https://en.wikipedia.org/wiki/Android","Android - Wikipedia",it.toLong())}
  val sites=SearchDestinations.bookmarkSuggestions(emptyList(),wiki);assertEquals("Wikipedia",sites.first().title);assertEquals(1,sites.count{it.title=="Wikipedia"})
  assertTrue(SearchDestinations.bookmarkSuggestions(listOf(wiki.first()),wiki).none{it.title=="Wikipedia"})
 }
 @Test fun onlyBookmarkSuggestionsAreLimitedToEight(){
  val many=(1..25).flatMap{i->List(i){Visit("https://site$i.test/subpage","Site $i",i.toLong())}}
  assertEquals(25,SearchDestinations.rank(many).size);val candidates=SearchDestinations.bookmarkSuggestions(emptyList(),many)
  assertEquals(8,candidates.size);assertEquals("https://site25.test/subpage",candidates.first().url)
  assertTrue(SearchDestinations.bookmarkSuggestions(candidates.map{Visit(it.url,it.title,0)},many).none{c->candidates.any{it.url==c.url}})
 }
}
