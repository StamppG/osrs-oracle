package com.osrsoracle;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
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

private static final long FAILURE_LOG_INTERVAL_NANOS =
TimeUnit.MINUTES.toNanos(1);

enum Outcome
{
SUCCESS,
HTTP_FAILURE,
NETWORK_FAILURE
}

interface OutcomeObserver
{
void onOutcome(
Outcome outcome,
String snapshotReason,
int status,
String errorType
);
}

interface SuccessfulResponseObserver
{
void onSuccessfulResponse(
String snapshotReason,
int status,
String responseBody
);
}

private static final class LoggingOutcomeObserver
implements OutcomeObserver
{
private final AtomicLong nextHttpFailureLogNanos =
new AtomicLong();

private final AtomicLong nextNetworkFailureLogNanos =
new AtomicLong();

@Override
public void onOutcome(
Outcome outcome,
String snapshotReason,
int status,
String errorType
)
{
switch (outcome)
{
case SUCCESS:
log.debug(
"Snapshot upload response: reason={}, status={}",
snapshotReason,
status
);
break;

case HTTP_FAILURE:
if (
shouldLogFailure(
nextHttpFailureLogNanos
)
)
{
log.warn(
"Snapshot upload HTTP failure: reason={}, status={}",
snapshotReason,
status
);
}
else
{
log.debug(
"Snapshot upload HTTP failure suppressed: reason={}, status={}",
snapshotReason,
status
);
}
break;

				case NETWORK_FAILURE:
					break;

default:
break;
}
}

private static boolean shouldLogFailure(
AtomicLong nextLogNanos
)
{
long now =
System.nanoTime();

while (true)
{
long next =
nextLogNanos.get();

if (now < next)
{
return false;
}

if (
nextLogNanos.compareAndSet(
next,
now + FAILURE_LOG_INTERVAL_NANOS
)
)
{
return true;
}
}
}
}

private final OkHttpClient httpClient;
private final OutcomeObserver outcomeObserver;

@Inject
SnapshotTransport(OkHttpClient httpClient)
{
this(
httpClient,
new LoggingOutcomeObserver()
);
}

SnapshotTransport(
OkHttpClient httpClient,
OutcomeObserver outcomeObserver
)
{
this.httpClient = httpClient;
this.outcomeObserver = outcomeObserver;
}

void send(
String backendUrl,
String writeToken,
String json,
String snapshotReason
)
{
send(
backendUrl,
writeToken,
json,
snapshotReason,
null
);
}

void send(
String backendUrl,
String writeToken,
String json,
String snapshotReason,
SuccessfulResponseObserver successfulResponseObserver
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

httpClient.newCall(request).enqueue(
new Callback()
{
@Override
public void onFailure(
Call call,
IOException e
)
{
log.error(
"Snapshot upload failed: reason={}",
snapshotReason,
e
);

outcomeObserver.onOutcome(
Outcome.NETWORK_FAILURE,
snapshotReason,
0,
e.getClass().getSimpleName()
);
}

@Override
public void onResponse(
Call call,
Response response
)
{
try (Response ignored = response)
{
String responseBody = null;

if (response.isSuccessful())
{
try
{
responseBody =
response.body() == null
? null
: response.body().string();
}
catch (IOException e)
{
log.warn(
"Failed to read successful snapshot response body: reason={}",
snapshotReason,
e
);
}
}

outcomeObserver.onOutcome(
response.isSuccessful()
? Outcome.SUCCESS
: Outcome.HTTP_FAILURE,
snapshotReason,
response.code(),
null
);

if (
response.isSuccessful() &&
successfulResponseObserver != null
)
{
successfulResponseObserver.onSuccessfulResponse(
snapshotReason,
response.code(),
responseBody
);
}
}
}
}
);
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
