package radams.gracenote.webapi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import radams.gracenote.webapi.GracenoteMetadataOET;

public class GracenoteMetadata
{
    // Members
    private ArrayList<Map<String, Object>> _data = new ArrayList<Map<String, Object>>();

    // Construct from the XML response from GN API
    public GracenoteMetadata(GracenoteWebAPI api, Document xml)
    {
        Element root = xml.getDocumentElement();

        NodeList nl = root.getElementsByTagName("ALBUM");
        for (int i = 0; i < nl.getLength(); i++)
        {
            Element e = (Element)nl.item(i);

            // Album data map
            Map<String, Object> albumData = new HashMap<String, Object>();

            // If there's no GOET data, do a fetch to get the full information instead.
            if (this._getTextValue(e, "ARTIST_ORIGIN") == null)
            {
                xml = api.fetchAlbumWithoutParsing(this._getTextValue(e, "GN_ID"));
                e   = xml.getDocumentElement();
            }

            // Album metadata
            albumData.put("album_gnid",        this._getTextValue(e, "GN_ID"));
            albumData.put("album_artist_name", this._getTextValue(e, "ARTIST"));
            albumData.put("album_title",       this._getTextValue(e, "TITLE"));
            albumData.put("album_year",        this._getTextValue(e, "DATE"));
            albumData.put("genre",             this._getOETData(e, "GENRE"));
            albumData.put("album_coverart",    this._getAttribElement(e, "URL", "TYPE", "COVERART"));

            // Artist metadata
            albumData.put("artist_image_url",  this._getAttribElement(e, "URL", "TYPE", "ARTIST_IMAGE"));
            albumData.put("artist_bio_url",    this._getAttribElement(e, "URL", "TYPE", "ARTIST_BIOGRAPHY"));
            albumData.put("review_url",        this._getAttribElement(e, "URL", "TYPE", "ARTIST_REVIEW"));

            // Artist OET metadata
            albumData.put("artist_era",        this._getOETData(e, "ARTIST_ERA"));
            albumData.put("artist_type",       this._getOETData(e, "ARTIST_TYPE"));
            albumData.put("artist_origin",     this._getOETData(e, "ARTIST_ORIGIN"));

            // Parse track metadata if there is any.
            if (e.getElementsByTagName("TRACK").getLength() > 0)
            {
                Element trackElement = (Element)e.getElementsByTagName("TRACK").item(0);

                albumData.put("track_number",      this._getTextValue(trackElement, "TRACK_NUM"));
                albumData.put("track_gn_id",       this._getTextValue(trackElement, "GN_ID"));
                albumData.put("track_title",       this._getTextValue(trackElement, "TITLE"));
                albumData.put("track_artist_name", this._getTextValue(trackElement, "ARTIST"));

                albumData.put("mood",              this._getOETData(trackElement, "MOOD"));
                albumData.put("tempo",             this._getOETData(trackElement, "TEMPO"));

                // If track level GOET data exists, overwrite metadata from album.
                if (trackElement.getElementsByTagName("GENRE").getLength() > 0)         { albumData.put("genre", this._getOETData(trackElement, "GENRE")); }
                if (trackElement.getElementsByTagName("ARTIST_ERA").getLength() > 0)    { albumData.put("artist_era", this._getOETData(trackElement, "ARTIST_ERA")); }
                if (trackElement.getElementsByTagName("ARTIST_TYPE").getLength() > 0)   { albumData.put("artist_type", this._getOETData(trackElement, "ARTIST_TYPE")); }
                if (trackElement.getElementsByTagName("ARTIST_ORIGIN").getLength() > 0) { albumData.put("artist_origin", this._getOETData(trackElement, "ARTIST_ORIGIN")); }
            }

            this._data.add(albumData);
        }
    }

    // Getters
    public ArrayList<Map<String, Object>> getAlbums() { return this._data; }
    public Map<String, Object> getAlbum(int index) { return this._data.get(index); }
    public Object getAlbumData(int albumIndex, String dataKey) { return this.getAlbum(albumIndex).get(dataKey); }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Helpers

    private String _getAttribElement(Element root, String nodeName, String attribute, String value)
    {
        NodeList nl = root.getElementsByTagName(nodeName);
        for (int i = 0; i < nl.getLength(); i++)
        {
            Element e = (Element)nl.item(i);
            String attr = e.getAttribute(attribute);
            if (attr != null && attr.equals(value)) { return e.getFirstChild().getNodeValue(); }
        }

        return null;
    }

    private ArrayList<GracenoteMetadataOET> _getOETData(Element root, String name)
    {
        ArrayList<GracenoteMetadataOET> al = new ArrayList<GracenoteMetadataOET>();

        NodeList nl = root.getElementsByTagName(name);
        for (int i = 0; i < nl.getLength(); i++)
        {
            Element e = (Element)nl.item(i);
            GracenoteMetadataOET oet = new GracenoteMetadataOET(e.getAttribute("ID"), e.getFirstChild().getNodeValue());
            al.add(oet);
        }

        return al;
    }

    private String _getTextValue(Element ele, String tagName)
    {
        String textVal = null;
        NodeList nl = ele.getElementsByTagName(tagName);
        if(nl != null && nl.getLength() > 0)
        {
            Element el = (Element)nl.item(0);
            textVal = el.getFirstChild().getNodeValue();
        }

        return textVal;
    }

    // Dumps out the data in a raw format to the console
    @SuppressWarnings("unchecked")
    public void print()
    {
        System.out.println("** Metadata **");
        for (Map<String, Object> m: this._data)
        {
            System.out.println(" + ALBUM");
            for (String key: m.keySet())
            {
                Object o = m.get(key);

                // Most stuff is string, we can just dump that out.
                if (o instanceof String)
                    System.out.println("   + " + key + ": " + (String)o);

                if (o instanceof ArrayList)
                {
                    System.out.println("   + " + key + ":");
                    for (Object oo: (ArrayList<GracenoteMetadataOET>)o)
                    {
                        if (oo instanceof GracenoteMetadataOET)
                        {
                            GracenoteMetadataOET oet = (GracenoteMetadataOET)oo; 
                            oet.print();
                        }
                    }
                }
            }
        }
    }
}
