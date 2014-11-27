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

import java.util.List;

import retrofit.http.GET;

public class OpenCultureDataSearchRequest {
    final string query;
    final string filters;
    final string size;

    OpenCultureDataSearchRequest(String query, String size) {
        this.query = query;
        this.size = size;
        this.filters = "{\"media_content_type\": {\"terms\": [\"image/jpeg\"]}}";
    }
}

interface OpenCultureDataService {
    @POST("/v0/search")
    HitsResponse searchAllCollections(@Body OpenCultureDataSearchRequest body);

    static class HitsResponse {
        Hits hits;
    }

    static class Hits {
        List<Item> hits;
    }

    static class Item {
        String _id;
        List<String> authors;
        String date;
        int date_granularity;
        List<MediaUrl> media_urls;
        Meta meta;
        String title;
    }

    static class MediaUrl {
        String content_type;
        int height;
        String url;
        int width;
    }

    static class Meta {
        String collection;
        String ocd_url;
        String original_object_id;
        String processing_finished;
        String processing_started;
        String rights;
        String source_id;
    }
}
