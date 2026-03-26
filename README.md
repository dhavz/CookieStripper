# Cookie Stripper Burp Extension

This extension learns individual cookies from the HTTP `Cookie` request header, shows each cookie name as a checkbox row, and removes checked cookies by rewriting the Proxy request itself.

## What It Does

- Learns cookie names from live HTTP request traffic.
- Shows unique cookie names in a checkbox table with seen count and last-seen tool.
- Rewrites the Proxy request itself instead of creating an `Original` versus `Edited` split during intercept.
- Removes only the checked cookies from the `Cookie` header.
- Rebuilds remaining cookies with the exact format `name=value; name2=value2`.
- Removes the entire `Cookie` header if no cookies remain after stripping.

## Practical Features Implemented

- Separate learning toggles for Proxy and non-Proxy requests.
- Filter box for large cookie inventories.
- Quick selection of common session cookie names.
- Case-insensitive cookie-name matching when stripping.
- Preserves original cookie order for the cookies that remain.

## Build

```bash
./build.sh
```

If your Burp API JAR is elsewhere:

```bash
BURP_API_JAR=/path/to/burp-extender-api.jar ./build.sh
```

The build output is:

```text
dist/cookie-stripper-burp-extension.jar
```

## Load In Burp

1. Open `Extender`.
2. Choose `Extensions`.
3. Click `Add`.
4. Set extension type to `Java`.
5. Select `dist/cookie-stripper-burp-extension.jar`.

Burp should load `io.github.burpextensions.cookiestripper.CookieStripperBurpExtender`.

## Verification Helper

Place a sample cookie header in `test.txt`. You can verify parsing or stripping locally with:

```bash
java -cp build/classes io.github.burpextensions.cookiestripper.CookieHeaderStripperCli test.txt
java -cp build/classes io.github.burpextensions.cookiestripper.CookieHeaderStripperCli test.txt JSESSIONID
```

## Next Features Worth Adding

- Save and load cookie-strip presets for different test phases.
- Scope stripping to only selected hosts or paths.
- Regex or prefix matching for cookie names.
- Optional tab showing the last rewritten Cookie header for auditability.
