# Cannot connect websocket when proxying through Kong

The purpose of this project is to demonstrate that you cannot proxy websockets
through Kong when using Chrome or Firefox.

## Running

Start a simple Spring Boot application with an embedded Tomcat and a Kong instance
by executing

```bash
$ docker-compose build
$ docker-compose up
```

Open https://www.piesocket.com/websocket-tester, enter ws://localhost:8000/hello and press connect.

**Expected behaviour**
The client should be connected to the server.

**Actual behaviour**
Connection failed

The request and response data from the upstream service can be found at http://localhost:8080/actuator/httptrace

## Investigation
If we instead connect to ws://localhost:8080/hello which will bypass Kong and go directly
to the upstream service you can see that it successfully connect to the websocket.

The difference between the two responses are that the one that goes directly to
the upstream service has the `Upgrade: websocket` header in the response, but it
is missing for the request that goes through Kong.

If we take a look at the request handler in Kong we can see that the upstream
`Connection` header is set to `keep-alive, Upgrade`

https://github.com/Kong/kong/blob/master/kong/runloop/handler.lua#L1244-L1247
```lua
-- Keep-Alive and WebSocket Protocol Upgrade Headers
if var.http_upgrade and lower(var.http_upgrade) == "websocket" then
  var.upstream_connection = "keep-alive, Upgrade"
  var.upstream_upgrade    = "websocket"
```

It was introduced in https://github.com/Kong/kong/pull/5495 and was before set
to only `"Upgrade"`

If we continue our investigation and see why Kong drops the `Upgrade` header
we find the following lines where the response headers are filtered:
https://github.com/Kong/kong/blob/master/kong/runloop/handler.lua#L1398-L1401
```lua
-- clear hop-by-hop response headers:
for _, header_name in csv(var.upstream_http_connection) do
  header[header_name] = nil
end
```

The csv function is called and all headers returned will be removed in the response.
https://github.com/Kong/kong/blob/master/kong/runloop/handler.lua#L159-L170

```lua
local function csv(s)
  if type(s) ~= "string" or s == "" then
    return csv_iterator, s, -1
  end

  s = lower(s)
  if s == "close" or s == "upgrade" or s == "keep-alive" then
    return csv_iterator, s, -1
  end

  return csv_iterator, s, 1
end
```
As we can see above, the `csv` function will return an empty list in case `s`
is equal to `upgrade`. However, since our `Connection` header is `keep-alive, Upgrade`,
which is not equal to `upgrade`, the method will now return an array with
two elements; `keep-alive` and `Upgrade`. The headers `keep-alive` and `upgrade`
will be removed from the response, giving us our actual behaviour.


This can also be confirmed by the posted workaround in https://github.com/Kong/kong/issues/5714#issuecomment-668061996
where the `Connection` header is explicitly overridden and set to the single value `Upgrade`.
Since the header value now is equal to `upgrade` the `csv` function will return an empty array
and no headers will be dropped from the response, making the client successfully connect to the server.
