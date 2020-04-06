(function () {
  'use strict';

  const
    WS_PROTOCOL = location.protocol === 'https:' ? 'wss' : 'ws',
    WS_PORT = 8080,
    WS_URL = WS_PROTOCOL + '://' + location.hostname + ':' + WS_PORT + '/ws?realmId=realm-0',
    canvas = document.getElementById('canvas'),
    buffer = document.createElement('canvas');

  let users = [], bullets = [];

  canvas.width = document.body.clientWidth;
  buffer.width = document.body.clientWidth;

  canvas.height = document.body.clientHeight;
  buffer.height = document.body.clientHeight;

  const canvasWidth = canvas.width,
    canvasHeight = canvas.height,
    cameraScale = Math.min(canvasWidth, canvasHeight),
    boundingRect = canvas.getBoundingClientRect();


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
    const
      STATE_CODE = 0;
    const
      PLAYER_CODE = 0,
      BULLET_CODE = 1;

    const data = new Uint8Array(e.data);

    const messageCode = data[0];
    switch (messageCode) {
      case STATE_CODE: {
        users = [];
        bullets = [];

        let i = 1;
        let gameObjectCount =
          (data[i++] << 24)
          | (data[i++] << 16)
          | (data[i++] << 8)
          | (data[i++]);

        while (gameObjectCount-- > 0) {
          const gameObjectType = data[i++];
          const
            xPos = sh2int(data[i++], data[i++]),
            yPos = sh2int(data[i++], data[i++]);
          console.log(xPos, yPos);

          const gameObject = toViewport(xPos, yPos);
          if (gameObjectType === PLAYER_CODE) {
            users.push(gameObject);
          } else if (gameObjectType === BULLET_CODE) {
            bullets.push(gameObject);
          }
        }
        break;
      }
      default: break;
    }
  };

  function joinGame(username) {
    const packet = new Uint8Array(16);

    let packetIndex = 0;
    packet[packetIndex++] = 100;
    packet[packetIndex++] = username.length;

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

  const ctx = canvas.getContext('2d');
  function draw() {
    ctx.clearRect(0, 0, canvasWidth, canvasHeight);
    for (let i = 0; i < users.length; i++) {
      ctx.beginPath();
      ctx.arc(users[i].x, users[i].y, 20, 0, 2 * Math.PI);
      ctx.stroke();

      // draw directional line
      // ctx.beginPath();
      // const headX = users[i].x + users[i].rotation.x * 20;
      // const headY = users[i].y + users[i].rotation.y * 20;
      //
      // ctx.moveTo(headX, headY);
      // ctx.lineTo(users[i].x, users[i].y);
      // ctx.stroke();
    }
    for (let i = 0; i < bullets.length; i++) {
      ctx.beginPath();
      ctx.arc(bullets[i].x, bullets[i].y, 5, 0, 2 * Math.PI);
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
