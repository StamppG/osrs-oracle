package com.osrsoracle;

import java.io.IOException;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Slf4j
final class SnapshotTransport
{
private static final MediaType JSON =
MediaType.parse("application/json; charset=utf-8");

private final OkHttpClient httpClient;

@Inject
SnapshotTransport(OkHttpClient httpClient)
{
this.httpClient = httpClient;
}

void send(
String backendUrl,
String writeToken,
String json,
String snapshotReason
)
{
try
{
Request request =
buildRequest(
backendUrl,
writeToken,
json
);

log.debug(
"Queueing snapshot upload: reason={}, bytes={}",
snapshotReason,
json.length()
);

httpClient.newCall(request).enqueue(new Callback()
{
@Override
public void onFailure(Call call, IOException e)
{
log.error(
"Snapshot upload failed: reason={}",
snapshotReason,
e
);
}

@Override
public void onResponse(Call call, Response response)
{
try (Response ignored = response)
{
log.debug(
"Snapshot upload response: reason={}, status={}",
snapshotReason,
response.code()
);
}
}
});
}
catch (Exception e)
{
log.error(
"Failed to queue snapshot upload: reason={}",
snapshotReason,
e
);
}
}

Request buildRequest(
String backendUrl,
String writeToken,
String json
)
{
return new Request.Builder()
.url(backendUrl + "/update")
.header(
"Authorization",
"Bearer " + writeToken
)
.post(
RequestBody.create(
JSON,
json
)
)
.build();
}
}