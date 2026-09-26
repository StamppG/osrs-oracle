package com.osrsoracle;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SnapshotTransportTest
{
@Test
public void buildsCompatibleAuthorizedJsonPost() throws Exception
{
SnapshotTransport transport =
new SnapshotTransport(new OkHttpClient());

String json = "{\"snapshotReason\":\"HEARTBEAT\"}";

Request request =
transport.buildRequest(
"https://example.test",
"test-token",
json
);

assertEquals(
"https://example.test/update",
request.url().toString()
);
assertEquals("POST", request.method());
assertEquals(
"Bearer test-token",
request.header("Authorization")
);
assertNotNull(request.body());
assertNotNull(request.body().contentType());
assertEquals(
"application/json; charset=utf-8",
request.body().contentType().toString()
);
assertEquals(
json.getBytes("UTF-8").length,
request.body().contentLength()
);
}
}