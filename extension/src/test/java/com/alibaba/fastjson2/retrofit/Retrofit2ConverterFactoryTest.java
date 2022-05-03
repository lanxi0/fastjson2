package com.alibaba.fastjson2.retrofit;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.support.config.FastJsonConfig;
import com.alibaba.fastjson2.support.retrofit.Retrofit2ConverterFactory;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.internal.http.RealResponseBody;
import okio.Buffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class Retrofit2ConverterFactoryTest {

    @Test
    public void test_for_coverage() throws Exception {
        assertThrows(NullPointerException.class, () -> Retrofit2ConverterFactory.create(null));

        Retrofit2ConverterFactory f = new Retrofit2ConverterFactory();
        f.getFastJsonConfig();
        f.setFastJsonConfig(new FastJsonConfig());
        f.requestBodyConverter(Model.class, null, null, null);
        f.responseBodyConverter(Model.class, null, null);

        final Model model = new Model().setId(1).setName("test");
        final String json = JSON.toJSONString(model);
        final ResponseBody body = new RealResponseBody("application/json; charset=UTF-8",
                json.length(), new Buffer().writeUtf8(json));

        RequestBody requestBody = Retrofit2ConverterFactory.create()
                .requestBodyConverter(Model.class, null, null, null)
                .convert(model);

        assertNotEquals(requestBody.contentLength(), 0);

        Model mode2 = (Model) Retrofit2ConverterFactory.create()
                .responseBodyConverter(Model.class, null, null)
                .convert(body);

        assertEquals(JSON.toJSONString(mode2), json);

        assertThrows(NullPointerException.class, () -> Retrofit2ConverterFactory.create()
                .responseBodyConverter(null, null, null)
                .convert(null));

    }

    public static class Model {

        private int id;
        private String name;

        public int getId() {
            return id;
        }

        public Model setId(int id) {
            this.id = id;
            return this;
        }

        public String getName() {
            return name;
        }

        public Model setName(String name) {
            this.name = name;
            return this;
        }
    }
}
