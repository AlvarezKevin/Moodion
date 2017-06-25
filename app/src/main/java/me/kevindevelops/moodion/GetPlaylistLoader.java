package me.kevindevelops.moodion;

import android.content.Context;
import android.net.Uri;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import cz.msebera.android.httpclient.util.EntityUtils;
import me.kevindevelops.moodion.models.MoodPlaylist;
import me.kevindevelops.moodion.models.Track;

/**
 * Created by Kevin on 6/19/2017.
 */

public class GetPlaylistLoader extends AsyncTaskLoader<MoodPlaylist> {

    private static final String LOG_TAG = GetPlaylistLoader.class.getSimpleName();
    private String ACCESS_TOKEN;
    private String searchTerm;

    public GetPlaylistLoader(Context context, String accessToken, String searchTerm) {
        super(context);
        ACCESS_TOKEN = accessToken;
        this.searchTerm = searchTerm;
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        forceLoad();
    }

    @Override
    public MoodPlaylist loadInBackground() {
        String json = getJsonFromUrl(buildUrl());

        if (json == null) {
            return null;
        }

        MoodPlaylist playlist =  buildPlaylist(json);

        if(playlist == null) {
            Log.v(LOG_TAG,"Playlist is null, some sort of error");
        }
        return playlist;
    }

    private MoodPlaylist buildPlaylist(String json) {
        String name;
        String ownerId;
        String id;
        String url;
        String apiUrl;
        String imageUrl;
        String uri;

        try {
            JSONObject jsonObject = new JSONObject(json);
            JSONArray jsonArray = jsonObject.getJSONArray("items");
            JSONObject resultsJsonObject = jsonArray.getJSONObject(new Random().nextInt(10));

            name = resultsJsonObject.getString("name");
            ownerId = resultsJsonObject.getJSONObject("owner").getString("id");
            id = resultsJsonObject.getString("id");
            url = resultsJsonObject.getJSONObject("external_urls").getString("spotify");
            apiUrl = resultsJsonObject.getString("href");
            imageUrl = resultsJsonObject.getJSONArray("images").getJSONObject(0).getString("url");
            uri = resultsJsonObject.getString("uri");

            MoodPlaylist playlist = new MoodPlaylist();
            playlist.setEmotion(searchTerm);
            playlist.setName(name);
            playlist.setOwnerId(ownerId);
            playlist.setId(id);
            playlist.setUrl(url);
            playlist.setApiUrl(apiUrl);
            playlist.setImageUrl(imageUrl);
            playlist.setUri(uri);

            String playlistJson = getJsonFromUrl(apiUrl);

            JSONObject tracksJsonObject = new JSONObject(playlistJson).getJSONObject("tracks");
            JSONArray tracksJsonArray = tracksJsonObject.getJSONArray("items");

            List<Track> tempTrackList = new ArrayList<>();
            for (int i = 0; i < tracksJsonArray.length(); i++) {
                JSONObject tempTrackJsonObject = tracksJsonArray.getJSONObject(i);

                String trackImageUrl = tempTrackJsonObject.getJSONArray("images").getJSONObject(1).getString("url");
                String trackName = tempTrackJsonObject.getString("name");
                String songUri = tempTrackJsonObject.getString("uri");
                String songId = tempTrackJsonObject.getString("id");
                String songURL = tempTrackJsonObject.getJSONObject("external_urls").getString("spotify");

                List<String> tempArtists = new ArrayList<>();
                JSONArray artistsArray = tempTrackJsonObject.getJSONArray("artists");
                for (int j = 0; j < artistsArray.length(); j++) {
                    tempArtists.add(artistsArray.getJSONObject(j).getString("name"));
                }

                Track tempTrack = new Track();
                tempTrack.setImageUrl(trackImageUrl);
                tempTrack.setTrackName(trackName);
                tempTrack.setSongUri(songUri);
                tempTrack.setSongId(songId);
                tempTrack.setSongURL(songURL);
                tempTrack.setArtistName(tempArtists);

                playlist.addTrack(tempTrack);

            }

            return playlist;
        } catch (JSONException e) {
            Log.v(LOG_TAG, "Error building playlist");
        }

        return null;
    }

    private String getJsonFromUrl(String url) {
        HttpClient httpClient = new DefaultHttpClient();
        String json;
        try {
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("Accept", "application/json");
            httpGet.setHeader("Authorization", "Bearer " + ACCESS_TOKEN);


            HttpResponse httpResponse = httpClient.execute(httpGet);
            json = EntityUtils.toString(httpResponse.getEntity());
            Log.v(LOG_TAG, json);

            return json;
        } catch (Exception e) {
            Log.v(LOG_TAG, "Couldn't get playlist", e);
        }
        return null;
    }

    private String buildUrl() {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme("https")
                .authority("api.spotify.com")
                .appendPath("v1")
                .appendPath("search")
                .appendQueryParameter("query", searchTerm)
                .appendQueryParameter("type", "playlist")
                .appendQueryParameter("market", "US")
                .appendQueryParameter("offset", "0")
                .appendQueryParameter("limit", "10");
        return builder.build().toString();
    }
}