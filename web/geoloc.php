<?php
$file = __DIR__ . '/position.txt';

function respond($code, $message) {
    http_response_code($code);
    header('Content-Type: text/plain; charset=utf-8');
    echo $message;
    exit;
}

if ($_SERVER['REQUEST_METHOD'] !== 'GET' && $_SERVER['REQUEST_METHOD'] !== 'POST') {
    respond(405, 'Method not allowed');
}

$lat = isset($_GET['lat']) ? $_GET['lat'] : (isset($_POST['lat']) ? $_POST['lat'] : null);
$lon = isset($_GET['lon']) ? $_GET['lon'] : (isset($_POST['lon']) ? $_POST['lon'] : null);

if ($lat === null || $lon === null) {
    respond(400, 'Missing lat or lon parameter');
}

$lat = filter_var($lat, FILTER_VALIDATE_FLOAT);
$lon = filter_var($lon, FILTER_VALIDATE_FLOAT);
if ($lat === false || $lon === false || $lat < -90 || $lat > 90 || $lon < -180 || $lon > 180) {
    respond(400, 'Invalid coordinates');
}

$line = number_format($lat, 6, '.', '') . ' ' . number_format($lon, 6, '.', '') . ' ' . time() . PHP_EOL;
if (file_put_contents($file, $line, LOCK_EX) === false) {
    respond(500, 'Unable to store position');
}

respond(200, 'OK');