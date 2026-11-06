package org.pocketsurf.browser

import android.net.Uri
import android.content.ComponentName
import android.os.Bundle
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class Surf110DataTest {
 private fun encoded(v:JSONObject)=JSONObject().put("format","PocketSurf backup").put("version",1).put("values",v).toString()
 private fun rejects(v:JSONObject){assertThrows(IllegalArgumentException::class.java){BackupCodec.validate(encoded(v))}}
 @Test fun backupRoundTripPreservesSettingsAndExcludesBrowsingData(){val p=MemoryPreferences(mapOf("theme" to "oled","accent" to "red","favorites_bar" to true,"default_text_zoom" to 125,"history" to "secret","sessions_v2" to "secret","cookies" to "secret"));val raw=BackupCodec.export(p);assertFalse(raw.contains("secret"));val target=MemoryPreferences();BackupCodec.apply(target,BackupCodec.validate(raw));assertEquals("oled",target.getString("theme",null));assertEquals("red",target.getString("accent",null));assertTrue(target.getBoolean("favorites_bar",false));assertEquals(125,target.getInt("default_text_zoom",0));Control.entries.forEach{assertEquals(it.defaultKey,target.getInt("key_${it.name}",0))}}
 @Test fun backupRejectsUnknownFieldsAndWrongTypes(){rejects(JSONObject().put("history","[]"));rejects(JSONObject().put("favorites_bar","true"));rejects(JSONObject().put("default_text_zoom",125.5));rejects(JSONObject().put("reader_size",33))}
 @Test fun backupRejectsCodeUrlsAndConflictingButtons(){rejects(JSONObject().put("home_url","javascript:alert(1)"));rejects(JSONObject().put("bookmarks","[{\"url\":\"file:///private\",\"title\":\"x\"}]"));rejects(JSONObject().put("key_${Control.entries[0].name}",Control.entries[1].defaultKey))}
 @Test fun backupRejectsOversizedInputWithoutChangingPreferences(){val p=MemoryPreferences(mapOf("theme" to "dark"));assertThrows(IllegalArgumentException::class.java){BackupCodec.apply(p,BackupCodec.validate(" ".repeat(BackupCodec.MAX_BYTES+1)))};assertEquals("dark",p.getString("theme",null))}
 @Test fun backupAllowsOnlyNormalizedSiteOriginsForTextSizes(){rejects(JSONObject().put("site_text_zoom","{\"https://example.org/path\":125}"));val clean=BackupCodec.validate(encoded(JSONObject().put("site_text_zoom","{\"https://example.org\":125}")));assertEquals(125,JSONObject(clean.getString("site_text_zoom")).getInt("https://example.org"))}
 @Test fun visibleHostCannotBeSpoofedByUserInfoOrPath(){assertEquals("https://trusted.test@attacker.test/trusted.test",PageIdentity.label("https://trusted.test@attacker.test/trusted.test"));assertEquals("https://attacker.test",PageIdentity.origin("https://trusted.test@attacker.test/a?secret=1"));assertNull(PageIdentity.origin("javascript:alert(1)"))}
 @Test fun originsNormalizeIdnSchemeAndPorts(){assertEquals("https://xn--bcher-kva.de",PageIdentity.origin("HTTPS://bücher.de:443/a"));assertEquals("http://[::1]:8080",PageIdentity.origin("http://[::1]:8080/a"));assertEquals("http://example.org",PageIdentity.origin("http://EXAMPLE.ORG:80/a"))}
 @Test fun memoryPreferencesDoNotModifyTheSourceOrNormalPreferences(){val input=mutableMapOf<String,Any?>("theme" to "oled");val a=MemoryPreferences(input);val b=MemoryPreferences(input);a.edit().putString("theme","light").putString("history","private").commit();assertEquals("oled",input["theme"]);assertEquals("oled",b.getString("theme",null));assertFalse(b.contains("history"));a.edit().clear().commit();assertTrue(a.all.isEmpty())}
 @Test fun privateActivityAndBookmarkProviderAreNotExported(){val app=RuntimeEnvironment.getApplication();val pm=app.packageManager;val activity=pm.getActivityInfo(ComponentName(app,PrivateBrowserActivity::class.java),0);assertFalse(activity.exported);assertEquals(app.packageName+":private",activity.processName);val provider=pm.resolveContentProvider(app.packageName+".bookmarks",0)!!;assertFalse(provider.exported);assertFalse(provider.processName.endsWith(":private"))}
 @Test fun explicitBookmarkProviderUsesNormalProcessPreferences(){val app=RuntimeEnvironment.getApplication();val result=app.contentResolver.call(Uri.parse("content://${app.packageName}.bookmarks"),"save_bookmark",null,Bundle().apply{putString("url","https://explicit.test/");putString("title","Explicit save")});assertTrue(result!!.getBoolean("saved"));assertTrue(BrowserData(app.getSharedPreferences("browser",0)).entries("bookmarks").any{it.url=="https://explicit.test/"})}
 @Test fun generatedDownloadsRetainProgressAndCancellation(){val app=RuntimeEnvironment.getApplication();app.getSharedPreferences("downloads",0).edit().clear().commit();val d=SurfDownloads(app);val item=d.generated("data:text/plain;base64,c2VjcmV0","test.txt","text/plain",Uri.parse("content://files/test"));assertEquals("data:",item.url);d.generatedProgress(item.id,400);assertEquals(400L,d.items().first{it.id==item.id}.received);d.cancel(item.id);d.generatedProgress(item.id,800,true);assertEquals(SurfDownloads.CANCELLED,d.items().first{it.id==item.id}.status)}
 @Test fun downloadReferrersKeepSameOriginPathsButRemoveCrossOriginQueries(){assertEquals("https://example.org/private/file?token=a",PageIdentity.referer("https://u:p@example.org/private/file?token=a#hidden","https://example.org/download"));assertEquals("https://example.org/",PageIdentity.referer("https://example.org/private?secret=a","https://cdn.example.net/file"));assertNull(PageIdentity.referer("https://example.org/","http://example.org/file"))}

 @Test fun oversizedExportCannotProduceAnUnimportableBackup(){assertThrows(IllegalArgumentException::class.java){BackupCodec.export(MemoryPreferences(mapOf("search_name" to "x".repeat(BackupCodec.MAX_BYTES))))}}
 @Test fun invalidWebsiteHostsCannotBecomeSiteOrigins(){assertNull(PageIdentity.origin("https://invalid host/path"));assertNull(PageIdentity.origin("file:///local"))}

 @Test fun legacyPinsAreIncludedInAllBookmarksWithoutDuplicates(){val p=MemoryPreferences(mapOf("github_explore_v1" to true,"home_shortcuts" to "[{\"id\":\"one\",\"title\":\"One\",\"url\":\"https://one.test/\"},{\"id\":\"two\",\"title\":\"Two\",\"url\":\"https://two.test/\"}]","bookmarks" to "[{\"url\":\"https://three.test/\",\"title\":\"Three\",\"time\":123}]"));val store=StartPageStore(p);assertEquals(3,BrowserData(p).entries("bookmarks").size);assertEquals(2,store.shortcuts().size);store.remove("one");assertEquals(3,BrowserData(p).entries("bookmarks").size);assertEquals(1,store.shortcuts().size)}
 @Test fun moreThanSixBookmarksStaySavedAndUnpinningDoesNotDeleteThem(){val p=MemoryPreferences();val data=BrowserData(p);val pins=StartPageStore(p);repeat(12){data.record("https://site$it.test/","Site $it","bookmarks")};repeat(6){pins.put("Site $it","https://site$it.test/")};assertThrows(IllegalArgumentException::class.java){pins.put("Site 6","https://site6.test/")};assertEquals(12,data.entries("bookmarks").size);data.write("bookmarks",data.entries("bookmarks").filterNot{it.url=="https://site2.test/"});assertEquals(5,pins.shortcuts().size);assertEquals(11,data.entries("bookmarks").size);data.write("bookmarks",emptyList());assertTrue(pins.shortcuts().isEmpty())}

}
