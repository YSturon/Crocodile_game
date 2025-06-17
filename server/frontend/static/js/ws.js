import { Client } from 'https://cdn.jsdelivr.net/npm/@stomp/stompjs@7/+esm';

/**
 * Подключение STOMP-клиента и отправка кадра.
 * В sendLandmarks передаём уже закодированную строку Base-64.
 */
export function connectWS(onResult, onConnect = () => {}) {

  const client = new Client({
    brokerURL     : `ws://${location.host}/ws`,
    reconnectDelay: 2000
  });

  client.onConnect = () => {
    client.subscribe('/user/queue/result',
      m => onResult(JSON.parse(m.body)));
    onConnect();
  };
  client.activate();

  return {
    /** b64String — результат btoa(...) */
    sendLandmarks(b64String){
      client.publish({
        destination : '/app/landmarks',
        headers     : { 'content-type':'text/plain;charset=UTF-8' },
        body        : b64String
      });
    }
  };
}