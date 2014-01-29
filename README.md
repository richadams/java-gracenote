A simple Java client for the [Gracenote Web API](https://developer.gracenote.com/web-api), which allows you to look up artists, albums, and tracks in the Gracenote database, and returns a number of metadata fields, including:

* Basic metadata; e.g. Artist Name, Album Title, Track Title.
* Descriptors; e.g. Genre, Origin, Mood, Tempo.
* Related content; e.g. Album Art, Artist Image, Biographies.

:exclamation: **_This is just example code to get you started on your own projects using Gracenote's API, and is not meant as an exhaustive wrapper of the full API._**

### Installation

Just copy the `radams` directory into your project, then import `radams.gracenote.webapi.GracenoteWebAPI` into it.

### Prerequisites

You will need a Gracenote Client ID from the [Gracenote Developer Portal](https://developer.gracenote.com/) to use the API.

Each installed application also needs to have a User ID, which may be obtained by registering your Client ID with the Gracenote API. To do this, do:

    GracenoteWebAPI api = new GracenoteWebAPI(clientID, clientTag); // If you have a userID, you can specify it as the third parameter to constructor.
    String userID = api.register();

**This registration should be done only once per application to avoid hitting your API quota** (i.e. definitely do NOT do this before every query). The userID can be stored in persistent storage (e.g. on the filesystem) and used for all subsequent pygn function calls.

Once you have your Client ID and User ID, you can start making queries.

### Usage

First, initialize the object using your credentials and UserID.

    GracenoteWebAPI api = new GracenoteWebAPI(clientID, clientTag, userID);

Then, to search for the Moby track "Porcelin" from his album "Play",

    GracenoteMetadata results = api.searchTrack("Moby", "Play", "Porcelin");

The results are a GracenoteMetadata objext containing the metadata information,

    ** Metadata **
     + ALBUM
       + genre:
         + OET id:35470, text:Electronica
         + OET id:25364, text:Electronica Mainstream
         + OET id:25665, text:Pop Electronica
       + album_artist_name: Moby
       + tempo:
       + mood:
       + album_gnid: 97474325-8C600076B380712C6D1C5DC5DC5674F1
       + artist_type:
         + OET id:29422, text:Male
         + OET id:29426, text:Male
       + track_number: 1
       + album_coverart: http://web.content.cddbp.net/cds/2.0?id=2FC2D6DD64870A70&client=15071488&class=cover&origin=front&size=medium&type=image/jpeg&tag=02KsU06lSPoLEKmCtLC7oOSfMf1zXdygxsd.Cle0Wdvq2pCkNIEzHdqA
       + artist_era:
         + OET id:29484, text:1990's
       + artist_bio_url: https://web.content.cddbp.net/cgi-bin/content-thin?id=1274BA55D9C33B8E&client=8311808&class=biography&type=text/plain&tag=022AV-RNVbltMJc.EFJbGc5S2gIPLshDXUSUnGxxiCA5oNVzSIk88Iig
       + track_gn_id: 97474326-B1F15B5A5852DF660C94268D737B6C36
       + artist_image_url: https://web.content.cddbp.net/cgi-bin/content-thin?id=C018FAD072939E99&client=8311808&class=image&size=medium&type=image/jpeg&tag=02ZI.drB7Mat.LPaSF0wPW1qBoaoWjc7FsitYyztLaNNMHL.KDFhYKUg
       + track_title: Porcelin
       + album_title: Play
       + artist_origin:
         + OET id:29889, text:North America
         + OET id:29908, text:United States
         + OET id:30199, text:New York
         + OET id:30634, text:New York City

_Note that URLs to related content (e.g. Album Art, Artist Image, etc) are not valid forever, so your application should download the content you want relatively soon after the lookup and cache it locally._

If you don't know which album a track is on (or don't care which album version you get), you can simply leave that parameter blank,

    GracenoteMetadata results = api.searchTrack("Moby", "", "Porcelin");

There are also convenience functions to look up just an Artist...,

    GracenoteMetadata results = api.searchArtist("CSS");

(_This will return the same result array with metadata for the top album by CSS (which happens to be "Cansei De Ser Sexy" at time of writing), with track-specific fields being blank_)

...or to look up just an Album,

    GracenoteMetadata results = api.searchAlbum("Jaga Jazzist", "What We Must");

(_This will return a array with metadata for Jaga Jazzist's "What We Must" album, again with track-specific fields empty._)
