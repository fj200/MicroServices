package com.eecs3311.profilemicroservice;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eecs3311.profilemicroservice.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping
public class ProfileController {
	public static final String KEY_USER_NAME = "userName";
	public static final String KEY_USER_FULLNAME = "fullName";
	public static final String KEY_USER_PASSWORD = "password";
	public static final String KEY_FRIEND_USER_NAME = "friendUserName";
	public static final String KEY_SONG_ID = "songId";

	@Autowired
	private final ProfileDriverImpl profileDriver;

	@Autowired
	private final PlaylistDriverImpl playlistDriver;

	OkHttpClient client = new OkHttpClient();

	public ProfileController(ProfileDriverImpl profileDriver, PlaylistDriverImpl playlistDriver) {
		this.profileDriver = profileDriver;
		this.playlistDriver = playlistDriver;
	}

	@RequestMapping(value = "/profile", method = RequestMethod.POST)
	public ResponseEntity<Map<String, Object>> addProfile(@RequestBody Map<String, String> params, HttpServletRequest request) {
		Map<String, Object> response = new HashMap<>();
		try {
			response.put("path", String.format("POST %s", Utils.getUrl(request)));
			DbQueryStatus dbQueryStatus = profileDriver.createUserProfile(params.get("userName"), params.get("fullName"), params.get("password"));
			response.put("message", dbQueryStatus.getMessage());
			return Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		} catch (Exception e) {
			response.put("error", "An error occurred while processing the request.");
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping(value = "/followFriend", method = RequestMethod.PUT)
	public ResponseEntity<Map<String, Object>> followFriend(@RequestBody Map<String, String> params, HttpServletRequest request) {
		Map<String, Object> response = new HashMap<>();
		try {
			response.put("path", String.format("PUT %s", Utils.getUrl(request)));
			DbQueryStatus dbQueryStatus = profileDriver.followFriend(params.get("userName"), params.get("friendUserName"));
			response.put("message", dbQueryStatus.getMessage());
			return Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		} catch (Exception e) {
			// Handle the exception here
			response.put("error", "An error occurred while processing the request.");
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}


	@RequestMapping(value = "/getAllFriendFavouriteSongTitles/{userName}", method = RequestMethod.GET)
	public ResponseEntity<Map<String, Object>> getAllFriendFavouriteSongTitles(@PathVariable("userName") String userName, HttpServletRequest request) {
		Map<String, Object> response = new HashMap<>();
		response.put("path", String.format("GET %s", Utils.getUrl(request)));
		try {
			DbQueryStatus dbQueryStatus = profileDriver.getAllSongFriendsLike(userName);
            HashMap<String, List<String>> res = new HashMap<>();
            HashMap<String, List<String>> songs = (HashMap<String, List<String>>) dbQueryStatus.getData();

            for(String friendUserName : songs.keySet()){
                int i = 0;
                for(String songId: songs.get(friendUserName)){
                    Request req = new Request.Builder()
                            .url("http://localhost:3001/getSongTitleById/"+songId)
                            .build();
                    Call call = client.newCall(req);
                    String resp = call.execute().body().string();
                    // Parse the JSON response
                    JSONObject jsonResponse = new JSONObject(resp);
                    songs.get(friendUserName).set(i, jsonResponse.getString("data"));
                }
            }
			response.put("message", res);
			return Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		} catch (Exception e) {
			return  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	@RequestMapping(value = "/unfollowFriend", method = RequestMethod.PUT)
	public ResponseEntity<Map<String, Object>> unfollowFriend(@RequestBody Map<String, String> params, HttpServletRequest request) {
		Map<String, Object> response = new HashMap<>();
		try {
			response.put("path", String.format("PUT %s", Utils.getUrl(request)));
			DbQueryStatus dbQueryStatus = profileDriver.unfollowFriend(params.get("userName"), params.get("friendUserName"));
			response.put("message", dbQueryStatus.getMessage());
			return Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		} catch (Exception e) {
			response.put("error", "An error occurred while processing the request.");
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping(value = "/likeSong", method = RequestMethod.PUT)
	public ResponseEntity<Map<String, Object>> likeSong(@RequestBody Map<String, String> params, HttpServletRequest request) {
		Map<String, Object> response = new HashMap<>();
		try {
			response.put("path", String.format("PUT %s", Utils.getUrl(request)));
			DbQueryStatus dbQueryStatus = playlistDriver.likeSong(params.get("userName"), params.get("songId"));
			response.put("message", dbQueryStatus.getMessage());
			return Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		} catch (Exception e) {
			response.put("error", "An error occurred while processing the request.");
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

    @RequestMapping(value = "/getMostPopularSong/{userName}", method = RequestMethod.GET)
    public ResponseEntity<Map<String, Object>> getMostPopularSong(@PathVariable("userName") String userName, HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("path", String.format("GET %s", Utils.getUrl(request)));
        try {
            List<String> songs = (List<String>) profileDriver.getSongsInPlayList(userName).getData();
            if(songs.isEmpty()){
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }
            else {
                JSONArray json = new JSONArray(songs);
                okhttp3.RequestBody body = okhttp3.RequestBody.create(MediaType.parse("application/json"), json.toString());
                Request req = new Request.Builder()
                        .url("http://localhost:3001/getMostFavoritesSong/")
                        .post(body)
                        .build();

                Call call = client.newCall(req);
                String res = call.execute().body().string();
                JSONObject jsonResponse = new JSONObject(res);
                response.put("data", jsonResponse.get("data"));
                return new ResponseEntity<>(response, HttpStatus.OK);
            }

        } catch (Exception e) {
            response.put("error", "An error occurred while processing the request.");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

	@RequestMapping(value = "/unlikeSong", method = RequestMethod.PUT)
	public ResponseEntity<Map<String, Object>> unlikeSong(@RequestBody Map<String, String> params, HttpServletRequest request) {
		Map<String, Object> response = new HashMap<>();
		try {
			response.put("path", String.format("PUT %s", Utils.getUrl(request)));
			DbQueryStatus dbQueryStatus = playlistDriver.unlikeSong(params.get("userName"), params.get("songId"));
			response.put("message", dbQueryStatus.getMessage());
			return Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		} catch (Exception e) {
			response.put("error", "An error occurred while processing the request.");
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

}
