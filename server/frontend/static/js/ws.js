import { Client } from 'https://cdn.jsdelivr.net/npm/@stomp/stompjs@7/+esm';

/**
 * Читает zoo_uid из cookie и передаёт его в каждый STOMP-SEND.
 */
function readUid() {
  const m = document.cookie.match(/(?:^|;\s*)zoo_uid=([^;]+)/);
  return m ? m[1] : '';
}

export function connectWS(onResult) {
  return new Promise(resolve => {
    const scheme = location.protocol === 'https:' ? 'wss' : 'ws';
    const url    = `${scheme}://${location.host}/ws`;
    const uid    = readUid();

    const client = new Client({
      brokerURL            : url,
      reconnectDelay       : 2000,
      heartbeatIncoming    : 10000,
      heartbeatOutgoing    : 10000,
      splitLargeFrames     : true,
      maxWebSocketChunkSize: 16 * 1024,
      debug                : msg => console.log('[STOMP]', msg),
    });

    client.onConnect = () => {
      console.log('WS connected →', url);

      client.subscribe('/user/queue/result', msg =>
          onResult(JSON.parse(msg.body))
      );

      resolve({
        sendLandmarks(buffer) {
          if (!client.connected) return;
          client.publish({
            destination: '/app/landmarks',
            headers: {
              'content-type': 'application/json',
              uid              // <<< ключевое изменение: uid в заголовке
            },
            body: JSON.stringify(Array.from(new Float32Array(buffer)))
          });
        }
      });
    };

    client.onStompError     = frame => console.error('STOMP error:', frame.headers['message'], frame.body);
    client.onWebSocketClose = e     => console.warn('WebSocket closed', e.code, e.reason);

    client.activate();
  });
}
