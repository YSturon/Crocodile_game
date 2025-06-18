import { Client } from 'https://cdn.jsdelivr.net/npm/@stomp/stompjs@7/+esm';

/**
 * Подключается к STOMP-брокеру и возвращает объект с одной функцией
 *   sendLandmarks(buffer:ArrayBuffer)
 * Фреймы режутся на чанки по 16 К (splitLargeFrames).
 * Heart-beat 10 с, авто-переподключение 2 с.
 */

function getUid () {
  const m = document.cookie.match(/(?:^|;\s*)zoo_uid=([^;]+)/);
  return m ? m[1] : '';
}

export function connectWS (onResult) {
  return new Promise(resolve => {
    const url = `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}/ws`;

    const client = new Client({
      brokerURL            : url,
      connectHeaders       : { uid: getUid() },         // ← передаём UID всегда
      reconnectDelay       : 2000,
      heartbeatIncoming    : 10000,
      heartbeatOutgoing    : 10000,
      splitLargeFrames     : true,
      maxWebSocketChunkSize: 16 * 1024,
      debug                : msg => console.log('[STOMP]', msg)
    });

    client.onConnect = () => {
      console.log('WS connected →', url);

      client.subscribe('/user/queue/result',
          m => onResult(JSON.parse(m.body)));

      resolve({
        /** отправляет 30×225 float-значений в JSON */
        sendLandmarks (ab) {
          if (!client.connected) return;
          client.publish({
            destination: '/app/landmarks',
            headers    : { 'content-type': 'application/json' },
            body       : JSON.stringify([...new Float32Array(ab)])
          });
        }
      });
    };

    client.onStompError     = f => console.error('STOMP error:', f.headers.message, f.body);
    client.onWebSocketClose = e => console.warn('WebSocket closed', e.code, e.reason);

    client.activate();
  });
}
