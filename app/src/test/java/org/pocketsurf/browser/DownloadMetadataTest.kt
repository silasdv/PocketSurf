package org.pocketsurf.browser
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class DownloadMetadataTest {
 @Test fun apkSuffixSurvivesGenericResponse(){assertEquals("PocketSurf.apk",DownloadMetadata.name("https://files.test/PocketSurf.apk",null,"application/octet-stream"));assertEquals("application/vnd.android.package-archive",DownloadMetadata.mime("PocketSurf.apk","application/octet-stream"))}
 @Test fun dispositionWinsOverOpaqueUrl(){assertEquals("PocketSurf-1.2.5.apk",DownloadMetadata.name("https://files.test/opaque?sig=1","attachment; filename=\"PocketSurf-1.2.5.apk\"","application/octet-stream"))}
 @Test fun internationalNamesRetainTheirSuffixAndPlus(){assertEquals("Pokémon+Test.zip",DownloadMetadata.name("https://files.test/opaque","attachment; filename*=UTF-8''Pok%C3%A9mon+Test.zip","application/octet-stream"))}
 @Test fun romAndArchiveSuffixesAreNotReplaced(){for(name in listOf("game.zip","game.7z","game.nes","game.bin"))assertEquals(name,DownloadMetadata.name("https://files.test/$name",null,"application/octet-stream"))}
 @Test fun extensionlessEndpointsStillUseSpecificResponseTypes(){assertEquals("123.pdf",DownloadMetadata.name("https://files.test/123",null,"application/pdf"))}
 @Test fun apkOpeningDoesNotUseTheZipContainerType(){assertEquals("application/vnd.android.package-archive",DownloadMetadata.mime("app.apk","application/zip"));assertEquals("application/zip",DownloadMetadata.mime("game.zip","application/octet-stream"))}
}
