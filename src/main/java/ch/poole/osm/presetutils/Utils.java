package ch.poole.osm.presetutils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPInputStream;

import org.jetbrains.annotations.NotNull;

public class Utils {

    private static final int TIMEOUT = 20;

    /**
     * Given an URL open the connection and return the InputStream
     * 
     * @param url the URL
     * @return the InputStream
     * @throws IOException
     */
    static InputStream openConnection(@NotNull URL url) throws IOException {
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        boolean isServerGzipEnabled;

        // Log.d(DEBUG_TAG, "get input stream for " + url.toString());

        // --Start: header not yet send
        con.setReadTimeout(TIMEOUT * 1000);
        con.setConnectTimeout(TIMEOUT * 1000);
        con.setRequestProperty("Accept-Encoding", "gzip");
        con.setRequestProperty("User-Agent", "PresetUtils (+https://github.com/simonpoole/preset-utils");
        con.setInstanceFollowRedirects(true);

        // --Start: got response header
        isServerGzipEnabled = "gzip".equals(con.getHeaderField("Content-encoding"));

        if (con.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new IOException("Got " + con.getResponseMessage());
        }

        if (isServerGzipEnabled) {
            return new GZIPInputStream(con.getInputStream());
        } else {
            return con.getInputStream();
        }
    }
}