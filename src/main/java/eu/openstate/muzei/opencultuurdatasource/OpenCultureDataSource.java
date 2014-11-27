/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.openstate.muzei.opencultuurdatasource;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.apps.muzei.api.Artwork;
import com.google.android.apps.muzei.api.RemoteMuzeiArtSource;
import com.google.android.apps.muzei.api.UserCommand;

import java.util.Random;

import retrofit.ErrorHandler;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;

import static eu.openstate.muzei.opencultuurdatasource.OpenCultureDataService.Item;
import static eu.openstate.muzei.opencultuurdatasource.OpenCultureDataService.HitsResponse;

public class OpenCultureDataSource extends RemoteMuzeiArtSource {
    private static final String TAG = "OpenCultureData";
    private static final String SOURCE_NAME = "OpenCultureDataSource";

    private static final int ROTATE_TIME_MILLIS = 3 * 60 * 60 * 1000; // rotate every 3 hours

    public OpenCultureDataSource() {
        super(SOURCE_NAME);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        List<UserCommand> commands = new ArrayList<UserCommand>();
        commands.add(new UserCommand(BUILTIN_COMMAND_ID_NEXT_ARTWORK));
        setUserCommands(commands);
    }

    @Override
    protected void onTryUpdate(int reason) throws RetryException {
        String currentToken = (getCurrentArtwork() != null) ? getCurrentArtwork().getToken() : null;

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(Config.OCD_API_BASE)
                // .setRequestInterceptor(new RequestInterceptor() {
                //     @Override
                //     public void intercept(RequestFacade request) {
                //         request.addQueryParam("consumer_key", Config.CONSUMER_KEY);
                //     }
                // })
                .setErrorHandler(new ErrorHandler() {
                    @Override
                    public Throwable handleError(RetrofitError retrofitError) {
                        int statusCode = retrofitError.getResponse().getStatus();
                        if (retrofitError.getKind() == RetrofitError.Kind.NETWORK
                                || (500 <= statusCode && statusCode < 600)) {
                            return new RetryException();
                        }
                        scheduleUpdate(System.currentTimeMillis() + ROTATE_TIME_MILLIS);
                        return retrofitError;
                    }
                })
                .build();

        OpenCultureDataService service = restAdapter.create(OpenCultureDataService.class);
        OpenCultureDataSearchRequest sr = new OpenCultureDataSearchRequest("en", "10");
        HitsResponse response = service.searchAllCollections(
            sr
        );

        if (response == null || response.hits == null) {
            throw new RetryException();
        }

        if (response.hits.hits.size() == 0) {
            Log.w(TAG, "No photos returned from API.");
            scheduleUpdate(System.currentTimeMillis() + ROTATE_TIME_MILLIS);
            return;
        }

        Random random = new Random();
        Item photo;
        String token;
        while (true) {
            photo = response.hits.hits.get(random.nextInt(response.hits.hits.size()));
            token = photo._id;
            if (response.hits.hits.size() <= 1 || !TextUtils.equals(token, currentToken)) {
                break;
            }
        }

        publishArtwork(new Artwork.Builder()
                .title(photo.title)
                .byline(photo.authors.get(0))
                .imageUri(Uri.parse(photo.media_urls.get(0).url))
                .token(token)
                .viewIntent(new Intent(Intent.ACTION_VIEW,
                        Uri.parse(photo.meta.ocd_url)))
                .build());

        scheduleUpdate(System.currentTimeMillis() + ROTATE_TIME_MILLIS);
    }
}
