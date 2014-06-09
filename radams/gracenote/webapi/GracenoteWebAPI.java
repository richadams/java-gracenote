package radams.gracenote.webapi;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

// You will need a Gracenote Client ID to use this. Visit https://developer.gracenote.com/ for info.

public class GracenoteWebAPI
{
    // Members
    private String _clientID  = "";
    private String _clientTag = "";
    private String _userID    = "";
    private String _apiURL    = "https://[[CLID]].web.cddbp.net/webapi/xml/1.0/";

    // Constructor
    public GracenoteWebAPI(String clientID, String clientTag) throws GracenoteException
    {
        this(clientID, clientTag, "");
    }

    public GracenoteWebAPI(String clientID, String clientTag, String userID) throws GracenoteException
    {
        // Sanity checks
        if (clientID.equals(""))  { throw new GracenoteException("Invalid input specified: clientID."); }
        if (clientTag.equals("")) { throw new GracenoteException("Invalid input specified: clientTag."); }

        this._clientID  = clientID;
        this._clientTag = clientTag;
        this._userID    = userID;
        this._apiURL    = this._apiURL.replace("[[CLID]]", clientID);
    }

    // Will register your clientID and Tag in order to get a userID. The userID should be stored
    // in a persistent form (filesystem, db, etc) otherwise you will hit your user limit.
    public String register()
    {
        return this.register(this._clientID + "-" + this._clientTag);
    }

    public String register(String clientID)
    {
        // Make sure user doesn't try to register again if they already have a userID in the ctor.
        if (!this._userID.equals(""))
        {
            System.out.println("Warning: You already have a userID, no need to register another. Using current ID.");
            return this._userID;
        }

        // Do the register request
        String request = "<QUERIES>"
                           + "<QUERY CMD=\"REGISTER\">"
                              + "<CLIENT>" + clientID + "</CLIENT>"
                           + "</QUERY>"
                       + "</QUERIES>";

        String response = this._httpPostRequest(this._apiURL, request);
        Document xml = this._checkResponse(response);

        // Cache it locally then return to user.
        this._userID = xml.getDocumentElement().getElementsByTagName("USER").item(0).getFirstChild().getNodeValue();
        return this._userID;
    }

    // Queries the Gracenote service for a track
    public GracenoteMetadata searchTrack(String artistName, String albumTitle, String trackTitle)
    {
        // Sanity check
        if (this._userID.equals("")) { this.register(); }

        String body = this._constructQueryBody(artistName, albumTitle, trackTitle);
        String data = this._constructQueryRequest(body);
        return this._execute(data);
    }

    // Queries the Gracenote service for an artist.
    public GracenoteMetadata searchArtist(String artistName) 
    {
        return this.searchTrack(artistName,  "", "");
    }

    // Queries the Gracenote service for an album.
    public GracenoteMetadata searchAlbum(String artistName, String albumTitle)
    {
        return this.searchTrack(artistName, albumTitle, "");
    }

    // This looks up an album directly using it's Gracenote identifier. Will return all the
    // additional GOET data.
    public GracenoteMetadata fetchAlbum(String gn_id)
    {
        // Sanity check
        if (this._userID.equals("")) { this.register(); }

        String body = this._constructQueryBody("", "", "", gn_id, "ALBUM_FETCH");
        String data = this._constructQueryRequest(body, "ALBUM_FETCH");
        return this._execute(data);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    // This looks up an album directly using it's Gracenote identifier. Returns the document, without
    // parsing the data first.
    public Document fetchAlbumWithoutParsing(String gn_id)
    {
        // Sanity check
        if (this._userID.equals("")) { this.register(); }

        String body = this._constructQueryBody("", "", "", gn_id, "ALBUM_FETCH");
        String data = this._constructQueryRequest(body, "ALBUM_FETCH");
        String response = this._httpPostRequest(this._apiURL, data);
        return this._checkResponse(response);
    }

    // Simply executes the query to Gracenote WebAPI
    protected GracenoteMetadata _execute(String data)
    {
        String response = this._httpPostRequest(this._apiURL, data);
        return this._parseResponse(response);
    }

    // Performs a HTTP POST request and returns the response as a string.
    protected String _httpPostRequest(String url, String data)
    {
        try
        {
            URL u = new URL(url); 
            HttpURLConnection connection = (HttpURLConnection) u.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "text/plain");
            connection.setRequestProperty("Charset", "utf-8");
            connection.setRequestProperty("Content-Length", "" + Integer.toString(data.getBytes().length));
            connection.setUseCaches (false);

            // Write the POST data
            BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(), "UTF-8"));
            wr.write(data);
            wr.flush(); wr.close();

            // Read the output
            StringBuffer output = new StringBuffer();
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));

            String line;
            while ((line = reader.readLine()) != null) { output.append(line); }

            reader.close();
            connection.disconnect();

            return output.toString();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }

    // This will construct the Gracenote query, adding in the authentication header, etc.
    protected String _constructQueryRequest(String body) { return this._constructQueryRequest(body, "ALBUM_SEARCH"); }
    protected String _constructQueryRequest(String body, String command)
    {
        return "<QUERIES>"
                   + "<AUTH>"
                       + "<CLIENT>" + this._clientID + "-" + this._clientTag + "</CLIENT>"
                       + "<USER>" + this._userID + "</USER>"
                   + "</AUTH>"
                   + "<QUERY CMD=\"" + command + "\">"
                       + body
                   + "</QUERY>"
               + "</QUERIES>";
    }

    // Constructs the main request body, including some default options for metadata, etc.
    protected String _constructQueryBody(String artist, String album, String track) { return this._constructQueryBody(artist, album, track, "", "ALBUM_SEARCH"); }
    protected String _constructQueryBody(String artist, String album, String track, String gn_id, String command)
    {
        String body = "";

        // If a fetch scenario, user the Gracenote ID.
        if (command.equals("ALBUM_FETCH"))
        {
            body += "<GN_ID>" + gn_id + "</GN_ID>";

            // Include extended data.
            body += "<OPTION>"
                         + "<PARAMETER>SELECT_EXTENDED</PARAMETER>"
                         + "<VALUE>COVER,REVIEW,ARTIST_BIOGRAPHY,ARTIST_IMAGE,ARTIST_OET,MOOD,TEMPO</VALUE>"
                    + "</OPTION>";

            // Include more detailed responses.
            body += "<OPTION>"
                         + "<PARAMETER>SELECT_DETAIL</PARAMETER>"
                         + "<VALUE>GENRE:3LEVEL,MOOD:2LEVEL,TEMPO:3LEVEL,ARTIST_ORIGIN:4LEVEL,ARTIST_ERA:2LEVEL,ARTIST_TYPE:2LEVEL</VALUE>"
                     + "</OPTION>";

            // Only want the thumbnail cover art for now (LARGE,XLARGE,SMALL,MEDIUM,THUMBNAIL)
            body += "<OPTION>"
                         + "<PARAMETER>COVER_SIZE</PARAMETER>"
                         + "<VALUE>MEDIUM</VALUE>"
                     + "</OPTION>";
        }
        // Otherwise, just do a search.
        else
        {
            // Only want the single best match.
            body += "<MODE>SINGLE_BEST</MODE>";

            // If a search scenario, then need the text input
            if (!artist.equals("")) { body += "<TEXT TYPE=\"ARTIST\">" + artist + "</TEXT>"; }
            if (!track.equals(""))  { body += "<TEXT TYPE=\"TRACK_TITLE\">" + track + "</TEXT>"; }
            if (!album.equals(""))  { body += "<TEXT TYPE=\"ALBUM_TITLE\">" + album + "</TEXT>"; }
        }

        return body;
    }

    // Checks the response for any Gracenote API errors, and converts to an XML document.
    private Document _checkResponse(String response)
    {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try
        {
            // Get and parse into a document
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(new StringReader(response)));

            // Navigate to the status code and read it.
            Element root = doc.getDocumentElement();
            NodeList nl = root.getElementsByTagName("RESPONSE");
            String status = "ERROR";
            if (nl != null && nl.getLength() > 0)
            {
                status = nl.item(0).getAttributes().getNamedItem("STATUS").getNodeValue();
            }

            // Handle error codes accordingly
            if (status.equals("ERROR"))    { throw new GracenoteException("API response error."); }
            if (status.equals("NO_MATCH")) { throw new GracenoteException("No match response."); }
            if (!status.equals("OK"))      { throw new GracenoteException("Non-OK API response."); }

            return doc;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }

    // This parses the API response into a GracenoteMetadata object
    protected GracenoteMetadata _parseResponse(String response)
    {
        Document xml = this._checkResponse(response);
        return new GracenoteMetadata(this, xml);
    }
}
