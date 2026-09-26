package com.osrsoracle;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SnapshotTransportTest
{
@Test
public void buildsCompatibleAuthorizedJsonPost() throws Exception
{
SnapshotTransport transport =
new SnapshotTransport(
new OkHttpClient()
);

String json =
"{\"snapshotReason\":\"HEARTBEAT\"}";

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

assertEquals(
"POST",
request.method()
);

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

@Test
public void reportsSuccessfulHttpResponse()
throws Exception
{
RecordingOutcomeObserver observer =
new RecordingOutcomeObserver();

SnapshotTransport transport =
new SnapshotTransport(
clientReturning(200),
observer
);

sendTestSnapshot(transport);

assertTrue(observer.await());
assertEquals(
SnapshotTransport.Outcome.SUCCESS,
observer.outcome
);
assertEquals(200, observer.status);
}

@Test
public void reportsNonSuccessfulHttpResponse()
throws Exception
{
RecordingOutcomeObserver observer =
new RecordingOutcomeObserver();

SnapshotTransport transport =
new SnapshotTransport(
clientReturning(500),
observer
);

sendTestSnapshot(transport);

assertTrue(observer.await());
assertEquals(
SnapshotTransport.Outcome.HTTP_FAILURE,
observer.outcome
);
assertEquals(500, observer.status);
}

@Test
public void reportsNetworkFailure()
throws Exception
{
RecordingOutcomeObserver observer =
new RecordingOutcomeObserver();

OkHttpClient client =
new OkHttpClient.Builder()
.addInterceptor(
chain ->
{
throw new IOException(
"simulated failure"
);
}
)
.build();

SnapshotTransport transport =
new SnapshotTransport(
client,
observer
);

sendTestSnapshot(transport);

assertTrue(observer.await());
assertEquals(
SnapshotTransport.Outcome.NETWORK_FAILURE,
observer.outcome
);
assertEquals(0, observer.status);
assertEquals(
"IOException",
observer.errorType
);
}

private static OkHttpClient clientReturning(
int status
)
{
return new OkHttpClient.Builder()
.addInterceptor(
chain ->
new Response.Builder()
.request(chain.request())
.protocol(Protocol.HTTP_1_1)
.code(status)
.message("test")
.body(
ResponseBody.create(
null,
new byte[0]
)
)
.build()
)
.build();
}

private static void sendTestSnapshot(
SnapshotTransport transport
)
{
transport.send(
"https://example.test",
"test-token",
"{\"snapshotReason\":\"HEARTBEAT\"}",
"HEARTBEAT"
);
}

private static final class RecordingOutcomeObserver
implements SnapshotTransport.OutcomeObserver
{
private final CountDownLatch latch =
new CountDownLatch(1);

private volatile SnapshotTransport.Outcome outcome;
private volatile int status;
private volatile String errorType;

@Override
public void onOutcome(
SnapshotTransport.Outcome outcome,
String snapshotReason,
int status,
String errorType
)
{
this.outcome = outcome;
this.status = status;
this.errorType = errorType;
latch.countDown();
}

boolean await()
throws InterruptedException
{
return latch.await(
2,
TimeUnit.SECONDS
);
}
}
}