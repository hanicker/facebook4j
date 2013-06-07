/*
 * Copyright 2012 Ryuji Yamashita
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package facebook4j.internal.http;

import facebook4j.FacebookException;
import facebook4j.internal.logging.Logger;
import facebook4j.internal.org.json.JSONException;
import facebook4j.internal.org.json.JSONObject;

import java.io.*;

/**
 * This class mock HTTP request & response, and returns pre-registered JSON response.
 * If pre-registered JSON file not found, it uses real HTTP request
 * and register JSON response to src/test/resources/mock_json directory.
 */
public final class MockHttpClientWrapper extends HttpClientWrapper {
    private static final long serialVersionUID = 6371174139273022969L;

    private static final Logger logger = Logger.getLogger(MockHttpClientWrapper.class);

    private String mockJSONResourceName;

    public MockHttpClientWrapper(HttpClientWrapperConfiguration wrapperConf) {
        super(wrapperConf);
    }

    /**
     * Sets the resource name of pre-registered JSON response.
     * <blockquote><pre>
     *     ex) "mock_json/user.json"
     * </pre></blockquote>
     * @param resourceName The resource name
     */
    public void setMockJSON(String resourceName) {
        mockJSONResourceName = resourceName;
    }

    @Override
    protected HttpResponse request(HttpRequest req) throws FacebookException {
        String url = req.getURL();
        logger.info(url);

        String json = readMockJSON();
        if (json == null) {
            HttpResponse res = super.request(req);
            try {
                JSONObject jsonObject = res.asJSONObject();
                json = jsonObject.toString(4);
                registerMockJSON(json);
            } catch (IOException e) {
                throw new FacebookException("write mock_json failed.", e);
            } catch (JSONException e) {
                throw new FacebookException(e);
            }
        }
        return new MockHttpResponseImpl(json);
    }

    private String readMockJSON() throws FacebookException {
        try {
            InputStream is = ClassLoader.getSystemResourceAsStream(mockJSONResourceName);
            if (null == is) return null;
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while (null != (line = reader.readLine())) {
                sb.append(line);
            }
            return sb.toString();
        } catch (IOException e) {
            throw new FacebookException("cannot read mock json", e);
        }
    }

    private void registerMockJSON(String json) throws IOException {
        String baseDir = System.getProperty("user.dir");
        if (!baseDir.endsWith("/facebook4j-core")) {
            baseDir += "/facebook4j-core";
        }
        File file = new File(baseDir + "/src/test/resources/" + mockJSONResourceName);
        new File(file.getParent()).mkdirs();
        FileWriter writer = new FileWriter(file);
        writer.write(json);
        writer.close();
    }

}
