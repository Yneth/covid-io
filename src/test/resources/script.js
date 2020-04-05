(function () {
  'use strict';

  const
    WS_PROTOCOL = location.protocol === 'https:' ? 'wss' : 'ws',
    WS_PORT = 8080,
    WS_URL = WS_PROTOCOL + '://' + location.hostname + ':' + WS_PORT + '/ws?realmId=realm-0',
    canvas = document.getElementById('canvas'),
    buffer = document.createElement('canvas');

  let
    users = [],
    bullets = [],
    walls = [];

  canvas.width = document.body.clientWidth;
  buffer.width = document.body.clientWidth;

  canvas.height = document.body.clientHeight;
  buffer.height = document.body.clientHeight;

  const canvasWidth = canvas.width,
    canvasHeight = canvas.height,
    cameraScale = Math.min(canvasWidth, canvasHeight),
    boundingRect = canvas.getBoundingClientRect();

  const ctx = canvas.getContext('2d');


  canvas.addEventListener('click', function (event) {
    // http://www.html5canvastutorials.com/advanced/html5-canvas-mouse-coordinates/
    var rect = boundingRect;
    var x = Math.round((event.clientX - rect.left) / (rect.right - rect.left) * canvasWidth),
      y = Math.round((event.clientY - rect.top) / (rect.bottom - rect.top) * canvasHeight);
    var worldPos = toWorld(x, y);
    sendPosition(worldPos.x, worldPos.y);
  }, false);

  const socket = new WebSocket(WS_URL);
  socket.binaryType = 'arraybuffer';
  socket.onopen = function (e) {
    const userNumber = (Math.random() * 100) | 0;
    joinGame('Test' + userNumber);
  };
  socket.onmessage = function (e) {
    var data = new Uint8Array(e.data);
    if (data[0] !== 0) {
      return;
    }
    users = [];
    bullets = [];
    walls = [];
    var i = 3;
    var playerCount = data[2];
    while (playerCount-- > 0) {
      var x = (data[i] << 8) | data[i + 1];
      var y = (data[i + 2] << 8) | data[i + 3];

      var player = toViewport(x, y);

      player.rotation = {};
      player.rotation.x = data[i + 4];
      player.rotation.y = data[i + 5];
      if (player.rotation.x > 10) {
        player.rotation.x = -(player.rotation.x & ~(1 << 5));
      }
      if (player.rotation.y > 10) {
        player.rotation.y = -(player.rotation.y & ~(1 << 5));
      }
      player.rotation.x /= 10;
      player.rotation.y /= 10;

      users.push(player);
      i += 6;
    }
    var bulletCount = sh2int(data[i], data[i + 1]);
    i += 2;
    while (bulletCount-- > 0) {
      var x = sh2int(data[i], data[i + 1]);
      var y = sh2int(data[i + 2], data[i + 3]);

      bullets.push(toViewport(x, y));
      i += 4;
    }
    for (; i < data.length - 7; i += 8) {
      var x = sh2int(data[i], data[i + 1]);
      var y = sh2int(data[i + 2], data[i + 3]);
      var width = sh2int(data[i + 4], data[i + 5]);
      var height = sh2int(data[i + 6], data[i + 7]);

      var wall = toViewport(x, y);
      var bounds = toViewport(width, height);
      wall.width = bounds.x;
      wall.height = bounds.y;
      walls.push(wall);
    }
  };

  function joinGame(username) {
    const packet = new Uint8Array(16);

    let packetIndex = 0;
    packet[packetIndex++] = 100;
    packet[packetIndex++] = username.length;
    console.log(username);
    for (let i = 0; i < username.length; i++) {
      packet[packetIndex++] = username.charCodeAt(i);
    }

    socket.send(packet);
  }

  function leaveGame() {
    socket.send('leave:');
  }

  function sendPosition(x, y) {
    socket.send(new Uint8Array([0, x >> 8, x & 0xFF, y >> 8, y & 0xFF]));
  }

  function shoot() {
    socket.send(new Uint8Array([1]));
  }

  function draw() {
    ctx.clearRect(0, 0, canvasWidth, canvasHeight);
    for (var i = 0; i < users.length; i++) {
      ctx.beginPath();
      ctx.arc(users[i].x, users[i].y, 20, 0, 2 * Math.PI);
      ctx.stroke();

      ctx.beginPath();
      var headX = users[i].x + users[i].rotation.x * 20;
      var headY = users[i].y + users[i].rotation.y * 20;

      ctx.moveTo(headX, headY);
      ctx.lineTo(users[i].x, users[i].y);
      ctx.stroke();
    }
    for (var i = 0; i < bullets.length; i++) {
      ctx.beginPath();
      ctx.arc(bullets[i].x, bullets[i].y, 5, 0, 2 * Math.PI);
      ctx.stroke();
    }
    for (var i = 0; i < walls.length; i++) {
      var wall = walls[i];
      ctx.rect(wall.x - wall.width / 2, wall.y - wall.height / 2, wall.width, wall.height);
      ctx.stroke();
    }
    window.requestAnimationFrame(draw);
  }

  window.requestAnimationFrame(draw);

  window.addEventListener('keypress', function () {
    shoot();
  });

  function toWorld(x, y) {
    x = x + (cameraScale / 2) - (cameraScale / 2); // add camera pos AND subtract viewport offset
    x = x / (cameraScale * 2); // divide by viewport scale IE normalize
    x = Math.round(x * 1000); // multiply to server coords

    y = y + (cameraScale / 2) - (cameraScale / 2);
    y = y / (cameraScale * 2);
    y = Math.round(y * 1000);
    return { 'x': x, 'y': y };
  }

  function toViewport(x, y) {
    x = x / 10000; // normalize
    x = x * 2 * cameraScale; // to world viewport scale
    x = x - (cameraScale / 2); // to camera pos
    x = x + (cameraScale / 2); // add viewport offset

    y = y / 10000; // normalize
    y = y * 2 * cameraScale; // to world viewport scale
    y = y - (cameraScale / 2); // to camera pos
    y = y + (cameraScale / 2); // add viewport offset
    return { 'x': x, 'y': y };
  }

  function sh2int(b0, b1) {
    return b0 << 8 | b1;
  }
})();
