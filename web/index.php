<?php
header('Cache-Control: no-store, no-cache, must-revalidate');
$dataFile = __DIR__ . '/position.txt';

function loadPosition() {
    global $dataFile;
    if (!file_exists($dataFile)) {
        return null;
    }
    $content = trim(file_get_contents($dataFile));
    if ($content === '') {
        return null;
    }
    $parts = preg_split('/\s+/', $content);
    if (count($parts) < 2) {
        return null;
    }
    $lat = filter_var($parts[0], FILTER_VALIDATE_FLOAT);
    $lon = filter_var($parts[1], FILTER_VALIDATE_FLOAT);
    if ($lat === false || $lon === false) {
        return null;
    }
    $time = isset($parts[2]) ? (int) $parts[2] : null;
    return array('lat' => $lat, 'lon' => $lon, 'time' => $time);
}

if (isset($_GET['json']) && $_GET['json'] === '1') {
    header('Content-Type: application/json; charset=utf-8');
    $position = loadPosition();
    if ($position === null) {
        echo json_encode(array('ok' => false));
    } else {
        echo json_encode(array('ok' => true) + $position);
    }
    exit;
}

$position = loadPosition();
$initialJson = $position === null ? 'null' : json_encode($position);
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>AutoGeoloc — Automatic Geolocation System</title>
    <link rel="icon" type="image/svg+xml" href="favicon.svg?v=5">
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"
          integrity="sha256-p4NxAoJBhIIN+hmNHrzRCf9tD/miZyoHS5obTRR9BMY=" crossorigin="">
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"
            integrity="sha256-20nQCchB9co0qIjJZRGuk2/Z9VM+kNiyxNV1lvTlZBo=" crossorigin=""></script>
    <style>
        html, body { margin: 0; padding: 0; height: 100%; }
        #map { height: calc(100% - 96px); }
        #header {
            height: 32px;
            line-height: 32px;
            padding: 0 8px;
            font-family: sans-serif;
            font-size: 14px;
            font-weight: bold;
            color: #fff;
            background: #1A237E;
        }
        #header img {
            vertical-align: middle;
            margin-right: 8px;
        }
        #status {
            height: 32px;
            line-height: 32px;
            padding: 0 8px;
            font-family: sans-serif;
            font-size: 14px;
            color: #333;
            background: #fff;
            border-bottom: 1px solid #ccc;
        }
        #footer {
            position: relative;
            z-index: 1000;
            height: 32px;
            line-height: 32px;
            padding: 0 8px;
            font-family: sans-serif;
            font-size: 14px;
            color: #fff;
            background: #1A237E;
        }
        #footer a {
            color: #fff;
            text-decoration: none;
        }
        #footer a:hover {
            text-decoration: underline;
        }
        #gear {
            float: right;
            background: none;
            border: none;
            color: #fff;
            font-size: 18px;
            cursor: pointer;
        }
        #gear:hover {
            color: #26A69A;
        }
        #panel {
            display: none;
            position: relative;
            z-index: 1000;
            padding: 8px 12px;
            font-family: sans-serif;
            font-size: 13px;
            color: #333;
            background: #eef;
            border-bottom: 1px solid #99a;
        }
        #panel code {
            display: inline-block;
            background: #fff;
            border: 1px solid #ccc;
            padding: 2px 6px;
            margin: 2px 0;
        }
        #panel button {
            margin-left: 6px;
            cursor: pointer;
        }
    </style>
</head>
<body>
    <div id="header">
        <img src="favicon.svg?v=5" width="24" height="24" alt="AutoGeoloc">
        <span>AutoGeoloc — Automatic Geolocation System</span>
    </div>
    <div id="status">Loading…</div>
    <div id="map"></div>
    <div id="panel">
        <b>Phone app configuration</b><br>
        In the app settings, add this publication URL (it contains the required {lat} and {lon}):<br>
        <code id="pub_url"></code>
        <button id="copy_url">Copy</button><br>
        <a href="https://sebasr.github.io/autogeoloc/AutoGeoloc.apk" target="_blank" rel="noopener">Download the Android app (APK)</a>
    </div>
    <div id="footer">
        <span>AutoGeoloc &copy; 2026 — <a href="https://sebasr.github.io/autogeoloc/" target="_blank" rel="noopener">Project website on GitHub Pages</a></span>
        <button id="gear" title="Phone app configuration">⚙</button>
    </div>
    <script>
        var initial = <?php echo $initialJson; ?>;
        var defaultPosition = [48.8566, 2.3522];
        var map = null;
        var marker = null;
        var track = [];
        var polyline = null;

        init();

        function init() {
            map = L.map('map');
            L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                maxZoom: 19,
                attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            }).addTo(map);

            if (initial !== null) {
                placeMarker(initial.lat, initial.lon);
                map.setView([initial.lat, initial.lon], 15);
            } else {
                map.setView(defaultPosition, 5);
                setStatus('No position stored yet');
            }
            refresh();
            setInterval(refresh, 5000);
            document.getElementById('gear').addEventListener('click', togglePanel);
            document.getElementById('copy_url').addEventListener('click', copyUrl);
        }

        function placeMarker(lat, lon) {
            if (marker === null) {
                marker = L.marker([lat, lon]).addTo(map);
            } else {
                marker.setLatLng([lat, lon]);
            }
            marker.bindPopup('Lat ' + lat.toFixed(6) + ', Lon ' + lon.toFixed(6));
        }

        function addToHistory(lat, lon) {
            var last = track[track.length - 1];
            if (last !== undefined && last[0] === lat && last[1] === lon) {
                return;
            }
            track.push([lat, lon]);
            if (polyline === null) {
                polyline = L.polyline(track, { color: '#26A69A', weight: 4, opacity: 0.85 });
                polyline.addTo(map);
            } else {
                polyline.setLatLngs(track);
            }
        }

        function updateTrajectoryStatus() {
            var text = document.getElementById('status').textContent;
            var count = track.length;
            if (count > 0) {
                document.getElementById('status').textContent =
                    text + ' — Trajectory: ' + count + ' point' + (count > 1 ? 's' : '');
            }
        }

        function setStatus(text) {
            document.getElementById('status').textContent = text;
        }

        function publicationUrl() {
            return location.origin + location.pathname.replace(/[^/]*$/, '') +
                'geoloc.php?lat={lat}&lon={lon}';
        }

        function togglePanel() {
            var panel = document.getElementById('panel');
            var show = panel.style.display === 'none' || panel.style.display === '';
            panel.style.display = show ? 'block' : 'none';
            if (show) {
                document.getElementById('pub_url').textContent = publicationUrl();
            }
        }

        function copyUrl() {
            var text = document.getElementById('pub_url').textContent;
            var done = function () {
                var button = document.getElementById('copy_url');
                button.textContent = 'Copied!';
                setTimeout(function () { button.textContent = 'Copy'; }, 1500);
            };
            if (navigator.clipboard && navigator.clipboard.writeText) {
                navigator.clipboard.writeText(text).then(done, done);
            } else {
                var input = document.createElement('textarea');
                input.value = text;
                document.body.appendChild(input);
                input.select();
                document.execCommand('copy');
                document.body.removeChild(input);
                done();
            }
        }

        function refresh() {
            fetch('index.php?json=1')
                .then(function (response) {
                    return response.json();
                })
                .then(function (data) {
                    if (data.ok !== true) {
                        setStatus('No position recorded yet');
                        return;
                    }
                    var when = data.time ? new Date(data.time * 1000).toLocaleString() : 'unknown';
                    setStatus('Last update: ' + when);
                    var first = marker === null;
                    placeMarker(data.lat, data.lon);
                    try {
                        addToHistory(data.lat, data.lon);
                        updateTrajectoryStatus();
                    } catch (e) {
                        console.error('Trajectory update failed:', e);
                    }
                    if (first) {
                        map.setView([data.lat, data.lon], 15);
                    }
                })
                .catch(function () {
                    setStatus('Unable to reach the server');
                });
        }
    </script>
</body>
</html>